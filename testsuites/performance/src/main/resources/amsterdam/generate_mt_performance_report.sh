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

# The aim of this script is to collect performance metric for policies
# running in PDP-D.
#
# Pre-requisites:
#
# Get the JMeter Multi-threaded Performance test plan (see below link).
#
# https://gerrit.onap.org/r/gitweb?p=policy/drools-applications.git;a=blob;f=testsuites/performance/src/main/resources/amsterdam/policyMTPerformanceTestPlan.jmx;hb=refs/heads/master
#
# How to run:
# 1: Copy this script to drools container
# 2: Remove (or save) existing network*zip files found in logging directory
# 3: Truncate (and save?) existing audit.log file found in logging directory
# 4: Run the performance jmeter script to completion
# 5: Pass following parameters to run this script
#    - log-dir : the complete directory location of network.log and audit.log
#
# Sample command for running the script:
#   ./generate_mt_performance_report.sh -l /var/log/onap/policy/pdpd
# Note: -h or --help can be used to display details about input parameters.
#
# How it works
# The script will parse the network log files at the specified location
# and match up outgoing requests to APPC with incoming responses and then
# calculate the time taken for each APPC request to complete.  The total
# time is recorded for each request.
# Once that completes, it then scans the audit log files, matches request IDs
# with those found in the network files, and reports the average elapsed
# time for each ONSET type, after subtracting the network time.

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

import subprocess
import re
import time
import calendar
import logging

logging.basicConfig(format="%(message)s")
logger = logging.getLogger("diff")
logger.setLevel(logging.INFO)

# Pattern for network.log files
# Single regular expression that matches different lines of interest.
# Each pattern as an outer "()" that captures the match, and a single
# nested capture group, so that group(2*n+1) can be used to determine
# if a line matched a given pattern and then group(2*n+2) can be used
# to extract the relevant item.
netpat = re.compile("" +
    "(^[[]([^|]+).*(?:OUT.UEB.*POLICY-CL))|" +
    "(^[[]([^|]+).*(?:IN.UEB.*APPC))|" +
    "(^[[]([^|]+).*(?:IN.UEB.*DCAE_CL_OUTPUT))|" +
    '(.*"closedLoopEventStatus"(?:.*)"(ABATED)")|' +
    '(.*"(?:requestId|request-id|RequestID|requestID)"[": ]+([^"]+))|' +
    '(.*"policyName"(?:.*)[.](GUARD_PERMITTED))|' +
    '(.*"policyName"(?:.*)[.](APPC.LCM.RESPONSE))|' +
    "(^[[]([^|]+).*(?:(?:OUT|IN).UEB))|" +
    ".")

# pattern to match a date and extract the millisecond portion
datepat = re.compile("^(20[^|]*)[.](\\d+)([^|]+)$")

# maps a request id to the start when the notification was received
req2begin = {}

# maps a request id to the total time from notification to response.
# some requests (e.g., vCPE) may include more than one notification/response
# pair in this total
req2total = {}


#
# converts a date string to linux milliseconds
#
def date_to_ms(dtstr):
    result = datepat.match(dtstr)
    tm = result.group(1) + result.group(3)
    tm = time.strptime(tm, "%Y-%m-%dT%H:%M:%S+00:00")
    tm = calendar.timegm(tm)
    tm = tm * 1000 + int(result.group(2))
    return tm

#
# Updates req2total and req2begin, based on the total time used by the
# given request
#
def update_diff(reqtype, reqid, tend):
    global req2total, req2begin

    logger.debug("reqid #2: %s", reqid)

    if reqid in req2begin:
        logger.debug("matched %s", reqid)
        diff = tend - req2begin[reqid]

        if reqid in req2total:
            req2total[reqid] += diff
        else:
            req2total[reqid] = diff

        logger.debug("%s %s total %s", reqtype, diff, req2total[reqid])
        del req2begin[reqid]

    else:
        logger.debug("unmatched %s", reqid)

#
# Scans network log files, computing time differences between notification
# requests and their corresponding responses.
# Updates req2total and req2begin, accordingly.
#
def gen_time_differences():
    global req2total, req2begin

    tm = None
    state = None
    reqid = None

    proc = subprocess.Popen(["bash", "-c",
                "ls -rth $LOGDIR/network*zip | xargs -n1 zcat; " +
                    "cat $LOGDIR/network.log"],
                stdout=subprocess.PIPE)

    for line in proc.stdout:
        result = netpat.match(line)
        if not result:
            continue

        i = -1

        i += 2
        if result.group(i):
            # matched OUT line, which starts with a timestamp
            tm = date_to_ms(result.group(i+1))
            state = "out"
            logger.debug("out %s", tm)
            continue

        i += 2
        if result.group(i):
            # matched APPC IN line, which starts with a timestamp
            tm = date_to_ms(result.group(i+1))
            state = "in-appc"
            logger.debug("appc in %s", tm)
            continue

        i += 2
        if result.group(i):
            # matched DCAE_CL_OUTPUT IN line, which starts with a timestamp
            tm = date_to_ms(result.group(i+1))
            state = "in-dcae"
            logger.debug("dcae in %s", tm)
            continue

        i += 2
        if result.group(i):
            if state == "in-dcae":
                # ABATEMENT line
                state = "in-abatement"
                logger.debug("abatement in %s", tm)
            continue

        i += 2
        if result.group(i):
            if state == "out":
                # matched request id in OUT message
                reqid = result.group(i+1)
                logger.debug("reqid: %s", reqid)
            elif state == "in-appc":
                # matched request id in IN message
                update_diff("appc", result.group(i+1), tm)
            elif state == "in-abatement":
                # matched request id in IN message
                update_diff("abatement", result.group(i+1), tm)
            continue

        i += 2
        if result.group(i):
            if reqid != None:
                # matched GUARD_PERMITTED in OUT message.
                # this precedes an APPC request
                req2begin[reqid] = tm
                logger.debug("appc request")
            else:
                logger.debug("unknown appc request")
            continue

        i += 2
        if result.group(i):
            if reqid != None:
                # matched APPC.LCM.RESPONSE in OUT message.
                # this precedes an ABATEMENT
                req2begin[reqid] = tm
                logger.debug("await abatement")
            else:
                logger.debug("unknown abatement")
            continue

        i += 2
        if result.group(i):
            # matched irrelevant IN/OUT message
            logger.debug("clear")
            state = None
            reqid = None
            tm = None
            continue

#
# scan audit log files and report average elapsed time
#
def gen_audit_times(event_type, grep_cmd):
    global logger

    mat_total = 0
    mat_count = 0
    un_total = 0
    un_count = 0

    proc = subprocess.Popen(["bash", "-c",
                "((ls -rth $LOGDIR/audit*zip | xargs -n1 zgrep " + event_type +
                    ") 2>/dev/null; " +
                    "grep " + event_type + " $LOGDIR/audit.log) " +
                    "| grep COMPLETE " +
                    grep_cmd +
                    "| awk -F'|' '{ print \$1, \$7}' " +
                    "| tail -10000"],
                stdout=subprocess.PIPE)

    for line in proc.stdout:
        (reqid,elapsed) = line.split(" ")

        if reqid in req2total:
            logger.debug("audit reqid %s: %s %s", reqid, elapsed, req2total[reqid])
            mat_total += int(elapsed) - req2total[reqid]
            mat_count += 1

        else:
            logger.debug("audit unmatched reqid %s: %s", reqid, elapsed)
            un_total += int(elapsed)
            un_count += 1

    print "Elapsed time for", event_type, ":"
    if mat_count > 0:
        print "  matched", mat_count, "samples, average", mat_total/mat_count, "ms"
    if un_count > 0:
        print "  unmatched", un_count, "samples, average", un_total/un_count, "ms"

	print


# scan all network log files, recording network times for requests in req2total
gen_time_differences()

# scan audit log files and report elapsed time for each ONSET type
gen_audit_times("vCPE", '| grep generic-vnf.vnf-id')
gen_audit_times("vFirewall", '| grep generic-vnf.vnf-id')
gen_audit_times("vDNS", '| grep vserver.vserver-name')
gen_audit_times("VOLTE", '')

PYTHON
