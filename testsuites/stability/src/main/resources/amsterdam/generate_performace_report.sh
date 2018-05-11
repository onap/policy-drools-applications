#!/bin/bash

# Copyright (C) 2018 Ericsson. All rights reserved.
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

# The aim of this script is to collect performance metric for policies running in PDP-D.
#
# Pre-requisites:
#
# Run the JMeter Stability test plan (see below link) on the PDP-D for atleast few hours so that enough samples are collected and used for performance calculation.
#
# Recommendation:
# Run for 72 hours
#
# https://gerrit.onap.org/r/gitweb?p=policy/drools-applications.git;a=blob;f=testsuites/stability/src/main/resources/amsterdam/droolsPdpStabilityTestPlan.jmx;h=8a327622acc38b4615e000bfab3f778d1997e6e7;hb=refs/heads/master
#
# How to run:
# 1: Copy this script to drools container
# 2: Pass following parameters to run the script
#    - log-dir : the complete directory location of audit.log file.
#    - wait : the wait time configured in JMeter test plan.
#
# Sample command for running the script: ./generate_performance_report -l /var/log/onap/policy/pdpd -w 500
# Note: -h or --help can be used to display details about input parameters.
#
# How it works
# The script will parse the audit.log file at the specified location and fetch the running time of each policy.
# Take enough samples and then calculate the average time taken for policies to complete.

usage()
{
_msg_="$@"
scriptname=$(basename $0)

cat<<-EOF

Command Arguments:

-l, --log-dir
 Mandatory argument. Directory location of audit logs.

-w, --wait
 Mandatory argument.  Wait time between onset and appc for vCPE and vFW (in milliseconds)

-h, --help
 Optional argument.  Display this usage.

EOF
exit 1

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
 # Multiplying by 2 because stability test waits after onset and abatement
 average=$((($vcpeSum / $vcpeTotal)-(2*$WAIT)))
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
 # Substracting wait as stability test waits after onset
 average=$((($vfwSum / $vfwTotal)-$WAIT))
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

# Called when script is executed with invalid arguments
invalid_arguments() {
echo "Missing or invalid option(s):"
echo "$@"
echo "Try -help for more information"
   exit 1
}

# Process the arguments passed to the script
process_arguments() {
short_args="hl:w:"
long_args="help,log-dir:wait:"

args=$(getopt -o $short_args -l $long_args -n "$0"  -- "$@"  2>&1 )
[[ $? -ne 0 ]] && invalid_arguments $( echo " $args"| head -1 )
[[ $# -eq 0 ]] && invalid_arguments "No options provided"
eval set -- "$args"
cmd_arg="$0"

while true; do
    case "$1" in
        -l|--log-dir)
          LOG_DIR=$2
          shift 2 ;;
        -w|--wait)
          WAIT=$2
          shift 2 ;;
         -h|--help)
          usage
          exit 0
          ;;
         --)
          shift
          break ;;
         *)
          echo BAD ARGUMENTS # perhaps error
          break ;;
      esac
done

if ! [[ -d $LOG_DIR ]]; then
	echo "$LOG_DIR does not exists" >&2; exit 1
fi

re='^[0-9]+$'
if ! [[ $WAIT =~ $re ]] ; then
   echo "error: WAIT must be number " >&2; exit 1
fi

}


# main body
process_arguments $@
process_vCPE
process_vFW
process_vDNS
process_VOLTE


