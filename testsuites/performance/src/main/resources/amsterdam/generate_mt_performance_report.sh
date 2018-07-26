#!/bin/bash

# Copyright (C) 2018 Ericsson. All rights reserved.
# Modifications Copyright (C) 2018 AT&T. All rights reserved.
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
# Run the JMeter Multi-threaded Performance test plan (see below link) on
# the PDP-D until it completes.
#
# https://gerrit.onap.org/r/gitweb?p=policy/drools-applications.git;a=blob;f=testsuites/performance/src/main/resources/amsterdam/policyMultiThread.jmx;hb=refs/heads/master
#
# How to run:
# 1: Copy this script to drools container
# 2: Pass following parameters to run the script
#    - log-dir : the complete directory location of audit.log file.
#
# Sample command for running the script:
#   ./generate_mt_performance_report -l /var/log/onap/policy/pdpd
# Note: -h or --help can be used to display details about input parameters.
#
# How it works
# The script will parse the audit.log file at the specified location and
# fetch the running time of each policy.  Take enough samples and then
# calculate the average time taken for policies to complete.

usage()
{
_msg_="$@"
scriptname=$(basename $0)

cat<<-EOF

Command Arguments:

-l
 Mandatory argument. Directory location of audit logs.

-h
 Optional argument.  Display this usage.

EOF

}

process_vCPE() {
 # vCPE use case
 vcpe_perf_list=($(ls -lrth $LOG_DIR/audit.* | awk '{print $9}'| xargs -n1 zgrep vCPE | grep COMPLETE | grep generic-vnf.vnf-id | awk -F'|' '{print $7 }' | tail -10000))

 vcpeTotal=0
 vcpeSum=0
 for count in "${vcpe_perf_list[@]}"
    do
      vcpeSum=$(($vcpeSum + $count))
      vcpeTotal=$(($vcpeTotal + 1))
    done
  average=$(($vcpeSum / $vcpeTotal))
 echo "Average time taken to execute vCPE use case: $average ms [samples taken for average: $vcpeTotal]"
}

process_vFW() {
 # vFirewall use case
 vfw_perf_list=($(ls -lrth $LOG_DIR/audit.* | awk '{print $9}'| xargs -n1 zgrep vFirewall | grep COMPLETE | grep generic-vnf.vnf-id | awk -F'|' '{print $7 }' | tail -10000))

 vfwTotal=0
 vfwSum=0
 for count in "${vfw_perf_list[@]}"
    do
      vfwSum=$(($vfwSum + $count))
      vfwTotal=$(($vfwTotal + 1))
    done
 average=$(($vfwSum / $vfwTotal))
 echo "Average time taken to execute vFirewall use case: $average ms [samples taken for average: $vfwTotal]"
}

process_vDNS() {
 # vDNS use case
 vdns_perf_list=($(ls -lrth $LOG_DIR/audit.* | awk '{print $9}'| xargs -n1 zgrep vDNS | grep COMPLETE | grep vserver.vserver-name | awk -F'|' '{print $7 }' | tail -10000))

 vdnsTotal=0
 vdnsSum=0
 for count in "${vdns_perf_list[@]}"
     do
       vdnsSum=$(($vdnsSum + $count))
       vdnsTotal=$(($vdnsTotal + 1))
     done  
 average=$(($vdnsSum / $vdnsTotal))
 echo "Average time taken to execute vDNS use case: $average ms [samples taken for average: $vdnsTotal]"
}

process_VOLTE() {
 # VOLTE use case
 volte_perf_list=($(ls -lrth $LOG_DIR/audit.* | awk '{print $9}'| xargs -n1 zgrep VOLTE | grep COMPLETE | awk -F'|' '{print $7 }' | tail -10000))

 volteTotal=0
 volteSum=0
 for count in "${volte_perf_list[@]}"
    do
      volteSum=$(($volteSum + $count))
      volteTotal=$(($volteTotal + 1))
    done
 average=$(($volteSum / $volteTotal))
 echo "Average time taken to execute VOLTE use case: $average ms [samples taken for average: $volteTotal]"
}

# Process the arguments passed to the script
process_arguments() {

while getopts "hl:" opt "$@"
do
    case $opt in
    l)
        LOG_DIR=$OPTARG
        ;;
    h)
        usage
        exit 0
        ;;
    *)
        usage
        exit 1
        ;;
    esac
done

echo log dir: $LOG_DIR

if ! [[ -n "$LOG_DIR" || -d "$LOG_DIR" ]]; then
    echo "$LOG_DIR does not exists" >&2; exit 1
fi

}


# main body
process_arguments "$@"
process_vCPE
process_vFW
process_vDNS
process_VOLTE
