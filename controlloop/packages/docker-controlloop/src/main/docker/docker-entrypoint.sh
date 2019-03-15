#!/bin/bash

# ########################################################################
# Copyright 2019 AT&T Intellectual Property. All rights reserved
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


function configurations {
	if [[ ${DEBUG} == y ]]; then
		echo "-- ${FUNCNAME[0]} --"
		set -x
	fi

    local confName

    for c in $(ls "${POLICY_INSTALL_INIT}"/*.conf 2> /dev/null); do
		echo "adding configuration file: ${c}"
		cp -f "${c}" "${POLICY_HOME}"/etc/profile.d/
		confName="$(basename "${c}")"
		sed -i -e "s/ *= */=/" -e "s/=\([^\"\']*$\)/='\1'/" "${POLICY_HOME}/etc/profile.d/${confName}"
    done

    source "${POLICY_HOME}"/etc/profile.d/env.sh
}

function features {
	if [[ ${DEBUG} == y ]]; then
		echo "-- ${FUNCNAME[0]} --"
		set -x
	fi

    source "${POLICY_HOME}"/etc/profile.d/env.sh

    for f in $(ls "${POLICY_INSTALL_INIT}"/features*.zip 2> /dev/null); do
		echo "installing feature: ${f}"
		"${POLICY_HOME}"/bin/features install "${f}"
    done
}

function scripts {
	if [[ ${DEBUG} == y ]]; then
		echo "-- ${FUNCNAME[0]} --"
		set -x
	fi

    source "${POLICY_HOME}"/etc/profile.d/env.sh

    for s in $(ls "${POLICY_INSTALL_INIT}"/*.sh 2> /dev/null); do
		echo "executing script: ${s}"
		source "${s}"
    done
}

function security {
	if [[ ${DEBUG} == y ]]; then
		echo "-- ${FUNCNAME[0]} --"
		set -x
	fi

    if [[ -f "${POLICY_INSTALL_INIT}"/policy-keystore ]]; then
        if ! cmp -s "${POLICY_INSTALL_INIT}"/policy-keystore "${POLICY_HOME}"/etc/ssl/policy-keystore; then
		    echo "overriding policy-keystore"policy-keystore
            cp -f "${POLICY_INSTALL_INIT}"/policy-keystore "${POLICY_HOME}"/etc/ssl
        fi
    fi

    if [[ -f ${POLICY_INSTALL_INIT}/policy-keystore ]]; then
        if ! cmp -s "${POLICY_INSTALL_INIT}"/policy-truststore "${POLICY_HOME}"/etc/ssl/policy-truststore; then
		    echo "overriding policy-truststore"
            cp -f "${POLICY_INSTALL_INIT}"/policy-truststore "${POLICY_HOME}"/etc/ssl
        fi
    fi

    if [[ -f "${POLICY_INSTALL_INIT}"/aaf-cadi.keyfile ]]; then
        if ! cmp -s "${POLICY_INSTALL_INIT}"/aaf-cadi.keyfile "${POLICY_HOME}"/config/aaf-cadi.keyfile; then
		    echo "overriding aaf-cadi.keyfile"
            cp -f "${POLICY_INSTALL_INIT}"/aaf-cadi.keyfile "${POLICY_HOME}"/config/aaf-cadi.keyfile
        fi
    fi
}

function properties {
	if [[ ${DEBUG} == y ]]; then
		echo "-- ${FUNCNAME[0]} --"
		set -x
	fi

    for p in $(ls "${POLICY_INSTALL_INIT}"/*.properties 2> /dev/null); do
		echo "configuration properties: ${p}"
		cp -f "${p}" "${POLICY_HOME}"/config
    done
}

function db {
	if [[ ${DEBUG} == y ]]; then
		echo "-- ${FUNCNAME[0]} --"
		set -x
	fi

    if [[ -z ${SQL_HOST} ]]; then
        return 0
    fi

	"${POLICY_HOME}"/bin/db-migrator -s ALL -o upgrade
}

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

function inspect {
	if [[ ${DEBUG} == y ]]; then
		echo "-- ${FUNCNAME[0]} --"
		set -x
	fi

    echo "ENV: "
    env
    echo
    echo

    source "${POLICY_HOME}"/etc/profile.d/env.sh
    policy status

    echo
    echo
}

function reload {
	if [[ ${DEBUG} == y ]]; then
		echo "-- ${FUNCNAME[0]} --"
		set -x
	fi

    configurations
    features
    security
    properties
    scripts
}

function start {
	if [[ ${DEBUG} == y ]]; then
		echo "-- ${FUNCNAME[0]} --"
		set -x
	fi

    source "${POLICY_HOME}"/etc/profile.d/env.sh
    policy start
}

function boot {
	if [[ ${DEBUG} == y ]]; then
		echo "-- ${FUNCNAME[0]} --"
		set -x
	fi

    reload
    db
    start

    tail -f /dev/null
}

set -e

if [[ ${DEBUG} == y ]]; then
	echo "-- $0 $* --"
	set -x
fi

operation="${1}"
case "${operation}" in
    inspect)    inspect
                ;;
    boot)       boot
                ;;
    *)          exec "$@"
                ;;
esac
