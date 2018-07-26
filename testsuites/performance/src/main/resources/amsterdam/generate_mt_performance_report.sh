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
# and match outgoing requests with incoming responses and then calculate
# the time taken for each request to complete.  The total time is recorded
# for each request id.
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

class Data:
    """
    A little bit of data associated with a single request.  This is only
    used for some request types.
    """
    
    def __init__(self, reqid, nexpected):
        self.reqid = reqid
        
        # number of matches expected before this object is discarded
        self.nexpected = nexpected
        
        # time when the request began
        self.begin = None

    def complete(self):
        """
        Indicates that the request has completed.  Returns true if no more
        requests are expected
        """
        self.begin = None
        self.nexpected -= 1
        return self.nexpected <= 0
        
class NetworkLogParser:
    """
    Used to parse network.log.  Various requests types are identified
    within the network.log using pattern matching.

    AAI
        <begin-time-stamp> OUT.AAI
            "vnf-id": "vnf-<request-id>"; OR
            "vserver-name": "vserver-<request-id>"
        <end-time-stamp> IN.AAI
            (takes the first one after OUT.AAI)
     
    APPC-CL
        <begin-time-stamp> OUT.UEB.APPC-CL
            "RequestID": "<request-id>"
        <end-time-stamp> IN.UEB.APPC-CL
            "RequestID": "<matching-request-id>"
     
    APPC-LCM
        <begin-time-stamp> OUT.UEB.APPC-LCM-READ
            "request-id": "<request-id>"
        <end-time-stamp> IN.UEB.APPC-LCM-WRITE
            "request-id": "<matching-request-id>"
     
    SO
        OUT.UEB.POLICY-CL-MGT
            "requestId": "<request-id>"
            "message": "actor=SO,..."
        <begin-time-stamp> <thread-id> OUT.SO
            (takes the first one after OUT.UEB.POLICY-CL-MGT)
        <end-time-stamp> <matching-thread-id> IN.SO
     
    VFC
        <begin-time-stamp> <thread-id> OUT.VFC url contains vserver-name-ssi-<request-id>
        <end-time-stamp> <matching-thread-id> IN.VFC (vserver response)
        <begin-time-stamp> <matching-thread-id> OUT.VFC (jobs request)
        <end-time-stamp> <matching-thread-id> IN.VFC (jobs response)
    """

    def __init__(self):

        # maps a request id to the time when the notification was received
        self.req2begin = {}

        # maps a request id to the total time from notification to response.
        # some requests (e.g., vCPE) may include more than one notification/response
        # pair in this total
        self.req2total = {}

        # latest AAI request ids, added as requests are seen and popped
        # as requests are completed
        self.aai_reqid = []
        
        # latest actor data, added when actor=SO is seen and moved to
        # so_thread_data when OUT.SO is seen
        self.so_actor_data = []
        
        # maps a thread id of the form, "pool-63-thread-41", to its SO Data
        self.so_thread_data = {}
        
        # maps a thread id of the form, "Thread-11382", to its VFC Data
        self.vfc_thread_data = {}

        # time associated with last IN/OUT line
        self.tm = None
        
        # current parser state, if parsing the body of a message, None
        # otherwise
        self.state = None
        
        # request id associated with last IN/OUT line
        self.reqid = None
        
        # regular expression to match leading time stamp
        timestamp_re = '[[]([^|]+).*'
        
        # regular expression to match thread id appearing before IN/OUT
        thread_re = '\\|([^|]+)\\]\\['
        
        # list of [method, pattern]
        # when the pattern is matched, the given method is invoked.
        # during compilation, the number of capture groups + 1 is added to
        # the end of each list
        self.actions = [
            [NetworkLogParser.out_notify, timestamp_re + '(?:OUT.UEB.*POLICY-CL)'],
            [NetworkLogParser.out_appc, timestamp_re + '(?:OUT.UEB.*APPC)'],
            [NetworkLogParser.in_appc, timestamp_re + '(?:IN.UEB.*APPC)'],
            [NetworkLogParser.in_dcae, timestamp_re + '(?:IN.UEB.*DCAE_CL_OUTPUT)'],
            [NetworkLogParser.out_aai, timestamp_re + '(?:OUT.AAI)'],
            [NetworkLogParser.in_aai, timestamp_re + '(?:IN.AAI)'],
            [NetworkLogParser.out_vfc, timestamp_re + thread_re + '(?:OUT.VFC)(?:.*vserver-name-ssi-([^/]+))?'],
            [NetworkLogParser.in_vfc, timestamp_re + thread_re + '(?:IN.VFC)'],
            [NetworkLogParser.in_abated, '.*"closedLoopEventStatus"[: ]+"ABATED"'],
            [NetworkLogParser.request_id, '.*"(?:requestId|request-id|RequestID|requestID)"[: ]+"([^"]+)"'],
            [NetworkLogParser.guard_permitted, '.*"policyName"(?:.*)[.]GUARD_PERMITTED'],
            [NetworkLogParser.appc_lcm_response, '.*"policyName"(?:.*)[.]APPC.LCM.RESPONSE'],
            [NetworkLogParser.vnf_vserver, '.*"vnf-id"[: ]+"vnf-([^"]+)"'],
            [NetworkLogParser.vnf_vserver, '.*"vserver-name"[: ]+"vserver-([^"]+)"'],
            [NetworkLogParser.actor_so, '.*"actor=SO,'],
            [NetworkLogParser.out_so, timestamp_re + thread_re + '(?:OUT.SO)'],
            [NetworkLogParser.in_so, timestamp_re + thread_re + '(?:IN.SO)'],
            [NetworkLogParser.out_in_ueb, timestamp_re + '(?:(?:OUT|IN).UEB)'],
            ]
        
        # pattern to match a date and extract the millisecond portion
        self.datepat = re.compile("^(20[^|]*)[.](\\d+)([^|]+)$")
        
        # compile the actions into a single pattern
        netpat = ""
        for action in self.actions:
            actpat = action[1]
                        
            # append capture group count + 1 to the end of the action
            action.append(re.compile(actpat).groups + 1)
            
            # append the pattern to the combined pattern
            if netpat != "":
                netpat += "|"
            netpat += "(" + actpat + ")"
            
        # Pattern for network.log files
        self.netpat = re.compile(netpat)
    
    def total(self, reqid):
        """
        Returns the total network time for a request, or None, if no network
        time has been determined for the request
        """
        if reqid in self.req2total:
            return self.req2total[reqid]
            
        else:
            return None

    def detm_network_times(self):
        """
        Scans network log files, computing time differences between requests
        and their corresponding responses.
        Updates req2total, accordingly.
        """
    
        proc = subprocess.Popen(["bash", "-c",
                    "(ls -rth $LOGDIR/network*zip | xargs -n1 zcat) 2>/dev/null; " +
                        "cat $LOGDIR/network.log"],
                    stdout=subprocess.PIPE)
    
        netpat = self.netpat
        actions = self.actions
        
        for line in proc.stdout:
            result = netpat.match(line)
            if not result:
                continue
            
            # find the matching action and then apply its method
            i = 1
            for action in actions:
                if result.group(i):
                    method = action[0]
                    method(self, line, result, i)
                    break
                
                i += action[-1]

    def out_notify(self, line, result, i):
        """ matched OUT line, which starts with a timestamp """
        self.tm = self.date_to_ms(result.group(i+1))
        self.state = "out"
        logger.debug("out %s", self.tm)

    def out_appc(self, line, result, i):
        """ matched APPC OUT line, which starts with a timestamp """
        self.tm = self.date_to_ms(result.group(i+1))
        self.state = "out-appc"
        logger.debug("appc out %s", self.tm)

    def in_appc(self, line, result, i):
        """ matched APPC IN line, which starts with a timestamp """
        self.tm = self.date_to_ms(result.group(i+1))
        self.state = "in-appc"
        logger.debug("appc in %s", self.tm)

    def in_dcae(self, line, result, i):
        """ matched DCAE_CL_OUTPUT IN line, which starts with a timestamp """
        self.tm = self.date_to_ms(result.group(i+1))
        self.state = "in-dcae"
        logger.debug("dcae in %s", self.tm)

    def out_aai(self, line, result, i):
        """ matched AAI OUT line, which starts with a timestamp """
        self.tm = self.date_to_ms(result.group(i+1))
        self.state = "out-aai"
        logger.debug("aai out %s", self.tm)

    def in_aai(self, line, result, i):
        """ matched AAI IN line, which starts with a timestamp """
        tm = self.date_to_ms(result.group(i+1))
        self.state = None
        if len(self.aai_reqid) > 0:
            reqid = self.aai_reqid.pop()
            self.update_diff("aai", reqid, tm)
            logger.debug("aai in %s", tm)
        else:
            logger.debug("unmatched aai in")
    
    def out_vfc(self, line, result, i):
        """ matched VFC OUT line, which starts with a timestamp """
        tm = self.date_to_ms(result.group(i+1))
        tid = result.group(i+2)
        reqid = result.group(i+3)
        
        if reqid != None and reqid != "":
            self.add_out("vfc", [Data(reqid,2)], self.vfc_thread_data, tid, tm)            
        else:
            self.update_out("vfc", tid, self.vfc_thread_data, tm)
                
        self.state = None
    
    def in_vfc(self, line, result, i):
        """ matched VFC IN line, which starts with a timestamp """
        tm = self.date_to_ms(result.group(i+1))
        tid = result.group(i+2)
        self.finish_in("vfc", self.vfc_thread_data, tid, tm)
        self.state = None
    
    def in_abated(self, line, result, i):
        """
        matched ABATED line
        """
        if self.state == "in-dcae":
            self.state = "in-abatement"
            logger.debug("abatement in %s", self.tm)
    
    def request_id(self, line, result, i):
        """ matched a request id field """
        if self.state == "out":
            # matched request id in OUT message
            self.reqid = result.group(i+1)
            logger.debug("out reqid: %s", self.reqid)
            
        elif self.state == "out-appc":
            reqid = result.group(i+1)
            self.req2begin[reqid] = self.tm
            logger.debug("out appc reqid: %s", reqid)
            
        elif self.state == "in-appc":
            self.update_diff("appc", result.group(i+1), self.tm)
            
        elif self.state == "in-abatement":
            # matched request id in IN message
            self.update_diff("abatement", result.group(i+1), self.tm)
    
    def guard_permitted(self, line, result, i):
        """
        matched GUARD_PERMITTED in OUT message.
        this precedes an APPC request
        """
        if self.reqid != None:
            self.req2begin[self.reqid] = self.tm
            logger.debug("appc request")
        else:
            logger.debug("unknown appc request")
    
    def appc_lcm_response(self, line, result, i):
        """
        matched APPC.LCM.RESPONSE in OUT message.
        this precedes an ABATEMENT
        """
        if self.reqid != None:
            self.req2begin[self.reqid] = self.tm
            logger.debug("await abatement")
        else:
            logger.debug("unknown abatement")
    
    def vnf_vserver(self, line, result, i):
        """ matched vnf-id or vserver-name """
        if self.state == "out-aai":
            # matched within AAI OUT message
            reqid = result.group(i+1)
            self.req2begin[reqid] = self.tm
            self.aai_reqid.append(reqid)
            logger.debug("await aai vnf-id")
    
    def actor_so(self, line, result, i):
        """ matched actor=SO """
        self.add_req("so", self.so_actor_data, 1)
        self.state = None
    
    def out_so(self, line, result, i):
        """ matched OUT|SO """
        tm = self.date_to_ms(result.group(i+1))
        tid = result.group(i+2)
        self.add_out("so", self.so_actor_data, self.so_thread_data, tid, tm)
        self.state = None
    
    def in_so(self, line, result, i):
        """ matched IN|SO """
        tm = self.date_to_ms(result.group(i+1))
        tid = result.group(i+2)
        self.finish_in("so", self.so_thread_data, tid, tm)
        self.state = None
    
    def out_in_ueb(self, line, result, i):
        """ matched irrelevant IN/OUT message """
        logger.debug("clear")
        self.state = None
        self.reqid = None
        self.tm = None

    def date_to_ms(self, dtstr):
        """
        converts a date string to milliseconds
        """        
        result = self.datepat.match(dtstr)
        tm = result.group(1) + result.group(3)
        tm = time.strptime(tm, "%Y-%m-%dT%H:%M:%S+00:00")
        tm = calendar.timegm(tm)
        tm = tm * 1000 + int(result.group(2))
        return tm

    def update_diff(self, reqtype, reqid, tend):
        """
        Updates req2total and req2begin, based on the total time used by the
        given request
        """        
        logger.debug("reqid #2: %s", reqid)
    
        if reqid in self.req2begin:
            logger.debug("matched %s", reqid)
            diff = tend - self.req2begin[reqid]
    
            if reqid in self.req2total:
                self.req2total[reqid] += diff
            else:
                self.req2total[reqid] = diff
    
            logger.debug("%s %s total %s", reqtype, diff, self.req2total[reqid])
            del self.req2begin[reqid]
    
        else:
            logger.debug("unmatched %s", reqid)

    def add_req(self, actor, actor_data, nexpected):
        """
        Adds request data for a particular actor
        """        
        if self.state == "out" and self.reqid != None:
            # matched action=<actor> in POLICY OUT message
            actor_data.append(Data(self.reqid, nexpected))
            logger.debug("%s actor", actor)
        else:
            logger.debug("unmatched %s actor", actor)

    def add_out(self, actor, actor_data, thread_data, tid, tm):
        """
        Adds data associated with the OUT message for an actor
        """        
        if tid != None:
            if len(actor_data) > 0:
                d = actor_data.pop()
                d.begin = tm
                thread_data[tid] = d
                logger.debug("%s out %s %s begin %s", actor, d.reqid, tid, tm)
            else:
                logger.debug("unmatched %s out %s", actor, tid)
        else:
            logger.debug("unmatched %s out", actor)

    def update_out(self, actor, tid, thread_data, tm):
        """
        Updates data associated with the OUT message for an actor
        """        
        if tid != None:
            if tid in thread_data:
                d = thread_data[tid]
                d.begin = tm
                logger.debug("%s out %s begin %s (repeat) %s", actor, d.reqid, tid, tm)
            else:
                logger.debug("unmatched %s out (repeat) %s", actor, tid)
        else:
            logger.debug("unmatched %s out (repeat)", actor)

    def finish_in(self, actor, thread_data, tid, tm):
        """
        Finishes data associated with the IN message for an actor
        """    
        if tid != None:
            if tid in thread_data:
                d = thread_data[tid]
                if d.begin != None:
                    self.req2begin[d.reqid] = d.begin
                    self.update_diff(actor, d.reqid, tm)
                else:
                    logger.debug("unmatched %s in begin %s", actor, tid)
                    
                if d.complete():
                    del thread_data[tid]
                    logger.debug("removed %s in %s", actor, tid)
            else:
                logger.debug("unmatched %s in %s", actor, tid)
        else:
            logger.debug("unmatched %s in", actor)


def gen_audit_times(net_log, event_type, grep_cmd):
    """
    Scans audit log files and reports average elapsed time
    """    
    global logger

    # used to compute averages for requests having matching network times
    mat_total = 0
    mat_count = 0
    
    # used to compute averages for requests that are unmatched
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

        total = net_log.total(reqid)
        if total:
            logger.debug("audit reqid %s: %s %s", reqid, elapsed, total)
            mat_total += int(elapsed) - total
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


# scan all network log files, recording network times for each request id
nlp = NetworkLogParser()
nlp.detm_network_times()

# scan audit log files and report elapsed time for each ONSET type
gen_audit_times(nlp, "vCPE", '| grep generic-vnf.vnf-id')
gen_audit_times(nlp, "vFirewall", '| grep generic-vnf.vnf-id')
gen_audit_times(nlp, "vDNS", '| grep vserver.vserver-name')
gen_audit_times(nlp, "VOLTE", '')

PYTHON
