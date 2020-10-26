#!/bin/bash

# Copyright (C) 2020 AT&T. All rights reserved.
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
# Run the JMeter Stability test plan on the PDP-D for at least few hours so that enough samples
# are collected and used for performance calculation.
#
# Recommendation:
# Run for 72 hours
#
# How to run:
# 1: Copy this script to drools container
# 2: Pass following parameters to run the script
#    - log-dir : the complete directory location of audit.log file.
#    - wait : the wait time configured in JMeter test plan. (in Guilin Release is no wait time)
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

process_vCPE_FAIL() {
 # vCPE use case
 vcpe0_perf_list=($(ls -lrth $LOG_DIR/audit.* | awk '{print $9}'| xargs -n1 zgrep ControlLoop-vCPE-Fail | grep "FINAL.FAILURE.ACCEPTED" | awk -F'|' '{print $7 }'| tail -200000))

 vcpe0Total=0
 vcpe0Sum=0
 vcpe0Max=0
 vcpe0Min=10000
 for count in "${vcpe0_perf_list[@]}"
    do
      if [ "$count" -gt "$vcpe0Max" ]; then
              vcpe0Max=$count
      fi
      if [ "$count" -lt "$vcpe0Min" ]; then
              vcpe0Min=$count
      fi
      vcpe0Sum=$(($vcpe0Sum + $count))
      vcpe0Total=$(($vcpe0Total + 1))
    done
 # Multiplying by 2 because stability test waits after onset and abatement
 average=$((($vcpe0Sum / $vcpe0Total)-(2*$WAIT)))
 echo "vCPE Failure cuse case ==> Max: $vcpe0Max, Min: $vcpe0Min, Average: $average ms [samples taken for average: $vcpe0Total]"
}

process_vCPE_OK() {
 # vCPE use case
 vcpe_perf_list=($(ls -lrth $LOG_DIR/audit.* | awk '{print $9}'| xargs -n1 zgrep ControlLoop-vCPE-48f0c2c3-a172-4192-9ae3-052274181b6e | grep COMPLETE | grep FINAL | awk -F'|' '{print $7 }' | tail -200000))

 vcpeTotal=0
 vcpeSum=0
 vcpeMax=0
 vcpeMin=10000
 for count in "${vcpe_perf_list[@]}"
    do
      if [ "$count" -gt "$vcpeMax" ]; then
              vcpeMax=$count
      fi
      if [ "$count" -lt "$vcpeMin" ]; then
              vcpeMin=$count
      fi
      vcpeSum=$(($vcpeSum + $count))
      vcpeTotal=$(($vcpeTotal + 1))
    done
 # Multiplying by 2 because stability test waits after onset and abatement
 average=$((($vcpeSum / $vcpeTotal)-(2*$WAIT)))
 echo "vCPE Success cuse case ==> Max: $vcpeMax, Min: $vcpeMin, Average: $average ms [samples taken for average: $vcpeTotal]"
}

process_vFW() {
 # vFirewall use case
 vfw_perf_list=($(ls -lrth $LOG_DIR/audit.* | awk '{print $9}'| xargs -n1 zgrep ControlLoop-vFirewall-d0a1dfc6-94f5-4fd4-a5b5-4630b438850a | grep COMPLETE | grep FINAL | awk -F'|' '{print $7 }' | tail -200000))

 vfwTotal=0
 vfwSum=0
 vfwMax=0
 vfwMin=10000
 for count in "${vfw_perf_list[@]}"
    do
      if [ "$count" -gt "$vfwMax" ]; then
	      vfwMax=$count
      fi
      if [ "$count" -lt "$vfwMin" ]; then
	      vfwMin=$count
      fi
      vfwSum=$(($vfwSum + $count))
      vfwTotal=$(($vfwTotal + 1))
    done
 # Substracting wait as stability test waits after onset
 average=$((($vfwSum / $vfwTotal)-$WAIT))
 echo "vFirewall Success use case => Max: $vfwMax, Min: $vfwMin, Average: $average ms [samples taken for average: $vfwTotal]"
}

process_vDNS_OK() {
 # vDNS use case
 vdns_perf_list=($(ls -lrth $LOG_DIR/audit.* | awk '{print $9}'| xargs -n1 zgrep ControlLoop-vDNS-6f37f56d-a87d-4b85-b6a9-cc953cf779b3 | grep COMPLETE | grep FINAL | awk -F'|' '{print $7 }' | tail -200000))

 vdnsTotal=0
 vdnsSum=0
 vdnsMax=0
 vdnsMin=10000
 for count in "${vdns_perf_list[@]}"
     do
        if [ "$count" -gt "$vdnsMax" ]; then
              vdnsMax=$count
       fi
       if [ "$count" -lt "$vdnsMin" ]; then
              vdnsMin=$count
       fi
       vdnsSum=$(($vdnsSum + $count))
       vdnsTotal=$(($vdnsTotal + 1))
     done
 average=$(($vdnsSum / $vdnsTotal))
 echo "vDNS Success use case => Max: $vdnsMax, Min: $vdnsMin, Average: $average ms [samples taken for average: $vdnsTotal]"
}

process_vDNS_FAIL() {
 # vDNS use case
 vdns_perf_list=($(ls -lrth $LOG_DIR/audit.* | awk '{print $9}'| xargs -n1 zgrep ControlLoop-vDNS-Fail | grep "failed to execute the next step" | awk -F'|' '{print $7 }' | tail -200000))

 vdns0Total=0
 vdns0Sum=0
 vdns0Max=0
 vdns0Min=10000
 for count in "${vdns_perf_list[@]}"
     do
        if [ "$count" -gt "$vdns0Max" ]; then
              vdns0Max=$count
       fi
       if [ "$count" -lt "$vdns0Min" ]; then
              vdns0Min=$count
       fi
       vdns0Sum=$(($vdns0Sum + $count))
       vdns0Total=$(($vdns0Total + 1))
     done
 average=$(($vdns0Sum / $vdns0Total))
 echo "vDNS Failure use case => Max: $vdns0Max, Min: $vdns0Min, Average: $average ms [samples taken for average: $vdns0Total]"
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
process_vCPE_OK
process_vCPE_FAIL
process_vDNS_OK
process_vDNS_FAIL
process_vFW

