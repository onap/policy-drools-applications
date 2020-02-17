#!/bin/bash

# ########################################################################
# Copyright 2019-2020 AT&T Intellectual Property. All rights reserved
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
# ########################################################################

function nexus {
    if [[ ${DEBUG} == y ]]; then
        echo "-- ${FUNCNAME[0]} --"
        set -x
    fi

    if [[ -z ${RELEASE_REPOSITORY_URL} ]]; then
        return 0
    fi

    # amsterdam legacy

    echo
    echo "checking if there are amsterdam policies already deployed .."
    echo

    local amsterdamVersion=$(curl --silent --connect-timeout 20 -X GET \
        "http://nexus:8081/nexus/service/local/artifact/maven/resolve?r=releases&g=org.onap.policy-engine.drools.amsterdam&a=policy-amsterdam-rules&v=RELEASE" \
        | grep -Po "(?<=<version>).*(?=</version>)")

    if [[ -z ${amsterdamVersion} ]]; then
        echo "no amsterdam policies have been found .."
        exit 0
    fi

    echo
    echo "The latest deployed amsterdam artifact in nexus has version ${amsterdamVersion}"
    echo

    sed -i.INSTALL \
        -e "s/^rules.artifactId=.*/rules.artifactId=policy-amsterdam-rules/g" \
        -e "s/^rules.groupId=.*/rules.groupId=org.onap.policy-engine.drools.amsterdam/g" \
        -e "s/^rules.version=.*/rules.version=${amsterdamVersion}/g" "${POLICY_HOME}"/config/amsterdam-controller.properties

    echo
    echo "amsterdam controller will be started brained with maven coordinates:"
    echo

    grep "^rules" "${POLICY_HOME}"/config/amsterdam-controller.properties
    echo
    echo
}

if [[ ${DEBUG} == y ]]; then
    echo "-- $0 $* --"
    set -x
fi

operation="${1}"
case "${operation}" in
    nexus)      nexus
                ;;
    *)          ${POLICY_HOME}/bin/pdpd-entrypoint.sh "$@"
                ;;
esac
