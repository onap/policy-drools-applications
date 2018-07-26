#!/bin/bash

#
# Copyright (C) 2018 AT&T. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# The aim of this script is to collect performance metrics for UEB requests
# associated with policies running in PDP-D.
#
# Pre-requisites:
#
# Run the JMeter Performance test plan (see below link) on the PDP-D.
#
# https://gerrit.onap.org/r/gitweb?p=policy/drools-applications.git;a=blob;f=testsuites/performance/src/main/resources/amsterdam/policyMultiUseCase.jmx;hb=refs/heads/master
#
# How to run:
# 1: Copy this script to drools container
# 2: Pass following parameters to run the script
#    - log-dir : the complete directory location of audit.log file.
#
# Sample command for running the script:
#	./uebtime.sh -l /var/log/onap/policy/pdpd
# Note: -h or --help can be used to display details about input parameters.
#
# How it works
# The script will parse the network.log file at the specified location and
# match up outgoing requests to APPC with incoming responses and then
# calculate the average time taken for APPC requests to complete.

usage()
{
_msg_="$@"
scriptname=$(basename $0)

cat<<-USAGE

Command Arguments:

-l
 Mandatory argument. Directory location of network logs.

-h
 Optional argument.  Display this usage.

USAGE

}

while getopts "hl:" OPT "$@"
do
	case $OPT in
	h)
		usage
		exit 0
		;;
	l)
		LOGDIR="$OPTARG"
		;;
	*)
		usage
		exit 1
		;;
	esac
done

if [ -z "$LOGDIR" ]; then
	usage
	exit 1
fi


python - <<PYTHON

import glob
import re
import time
import calendar
import logging

logging.basicConfig(format="%(message)s")
logger = logging.getLogger("diff")
logger.setLevel(logging.INFO)

outpat = re.compile("" +
	"(^[[]([^|]+).*(?:OUT.UEB.*POLICY-CL))|" +
	"(^[[]([^|]+).*(?:IN.UEB.*APPC))|" +
	'(.*"(?:requestId|request-id|RequestID)"[": ]+([^"]+))|' +
	'(.*"policyName"(?:.*)[.]' +
		"(EVENT.MANAGER.OPERATION.LOCKED.GUARD_PERMITTED))|" +
	"(^[[]([^|]+).*(?:(OUT|IN).UEB))" +
	"")
datepat = re.compile("^(20[^|]*)[.](\\d+)([^|]+)$")

diff_total = 0
diff_count = 0


def date_to_ms(dtstr):
	result = datepat.match(dtstr)
	tm = result.group(1) + result.group(3)
	tm = time.strptime(tm, "%Y-%m-%dT%H:%M:%S+00:00")
	tm = calendar.timegm(tm)
	tm = tm * 1000 + int(result.group(2))
	return tm

def gen_time_differences(file):
	global diff_total, diff_count

	tm = None
	state = None
	reqid = None
	req2tm = {}

	with open(file) as f:
		for line in f:
			result = outpat.match(line)
			if result:
				if result.group(1):
					# matched OUT line, which starts with a timestamp
					tm = date_to_ms(result.group(2))
					state = "out"
					logger.debug("out %s", tm)
				elif result.group(3):
					# matched IN line, which starts with a timestamp
					tm = date_to_ms(result.group(4))
					state = "in"
					logger.debug("in %s", tm)
				elif result.group(5):
					if state == "out":
						# matched request id in OUT message
						reqid = result.group(6)
						logger.debug("reqid: %s", reqid)
					elif state == "in":
						# matched request id in IN message
						reqid = result.group(6)
						logger.debug("reqid #2: %s", reqid)
						if reqid in req2tm:
							logger.debug("matched %s", reqid)
							diff = tm - req2tm[reqid]
							diff_total += diff
							diff_count += 1
							logger.debug("diff %s", diff)
							del req2tm[reqid]
						else:
							logger.debug("unmatched %s", reqid)
				elif result.group(7):
					if reqid != None:
						# matched GUARD_PERMITTED in OUT message
						req2tm[reqid] = tm
						logger.debug("permit")
					else:
						logger.debug("unknown permit")
				elif result.group(9):
					# matched irrelevant IN/OUT message
					logger.debug("clear")
					state = None
					reqid = None
					tm = None

files = glob.glob("$LOGDIR/network*.log*")
for f in files:
	gen_time_differences(f)

print diff_total / diff_count

PYTHON
