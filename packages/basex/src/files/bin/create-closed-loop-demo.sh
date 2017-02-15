#! /bin/bash

###
# ============LICENSE_START=======================================================
# PDP-D APPS Base Package
# ================================================================================
# Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ============LICENSE_END=========================================================
###

# Interactive script to generate a closed loop demo rules artifact
# for testing purposes of standalone PDP-D

echo "Closed Loop Demo Creator for standalone PDP-D"
echo "----------------------------------------------"
echo

GROUPID="org.openecomp.policy.demo.rules"
ARTIFACTID="closed-loop-demo-rules"
VERSION="1.0.0-SNAPSHOT"
PACKAGE="org.openecomp.policy.demo.rules"
CLOSEDLOOPCONTROLNAME="CL-FRWL-LOW-TRAFFIC-SIG-d925ed73-8231-4d02-9545-db4e101f88f8"
POLICYSCOPE="service=test;resource=FRWL;type=configuration"
POLICYNAME="FirewallDemo"
POLICYVERSION="v0.0.1"
ACTOR="APPC"
APPCTOPIC="APPC-CL"
APPCSERVERS="vm1.mr.simpledemo.openecomp.org"
APPCAPIKEY=
APPCAPISECRET=
NOTIFICATIONTOPIC="POLICY-CL-MGT"
NOTIFICATIONSERVERS="vm1.mr.simpledemo.openecomp.org"
NOTIFICATIONAPIKEY=
NOTIFICATIONAPISECRET=
DCAETOPIC="DCAE-CL-EVENT"
DCAESERVERS="vm1.mr.simpledemo.openecomp.org"
DCAEAPIKEY=
DCAEAPISECRET=
AAIURL="http://localhost:7676/aai/test"
AAIUSERNAME="policy"
AAIPASSWORD="policy"
AAINAMEDQUERYUUID=d925ed73-8231-4d02-9545-db4e101fffff
AAIPATTERNMATCH=true
MSOURL="http://localhost:7677/mso/test"
MSOUSERNAME="policy"
MSOPASSWORD="policy"

read -e -i "${GROUPID}" -p "Closed Loop Rules Maven Group Id> " GROUP_ID
read -e -i "${ARTIFACTID}" -p "Closed Loop Rules Maven Coordinates Artifact Id> " ARTIFACTID
read -e -i "${VERSION}" -p "Closed Loop Rules Maven Coordinates Version> " VERSION
read -e -i "${PACKAGE}" -p "Closed Loop Rules Package> " PACKAGE
read -e -i "${CLOSEDLOOPCONTROLNAME}" -p "Closed Loop Template Control Name> " CLOSEDLOOPCONTROLNAME
read -e -i "${POLICYSCOPE}" -p "Closed Loop Policy Scope> " POLICYSCOPE
read -e -i "${POLICYNAME}" -p "Closed Loop Policy Name> " POLICYNAME
read -e -i "${POLICYVERSION}" -p "Closed Loop Policy Version> " POLICYVERSION
read -e -i "${ACTOR}" -p "Closed Loop Actor ('APPC' or 'MSO')> " ACTOR
read -e -i "${APPCTOPIC}" -p "Closed Loop APP-C Recipe Topic> " APPCTOPIC
read -e -i "${APPCSERVERS}" -p "Closed Loop APP-C UEB Servers for ${APPCTOPIC} topic> " APPCSERVERS
read -e -i "${APPCAPIKEY}" -p "Closed Loop APP-C UEB API Key for ${APPCTOPIC} topic> " APPCAPIKEY
read -e -i "${APPCAPISECRET}" -p "Closed Loop APP-C UEB API Secret for ${APPCTOPIC} topic> " APPCAPISECRET
read -e -i "${NOTIFICATIONTOPIC}" -p "Closed Loop Ruby Notification Topic> " NOTIFICATIONTOPIC
read -e -i "${NOTIFICATIONSERVERS}" -p "Closed Loop Ruby UEB Servers for ${NOTIFICATIONTOPIC} topic> " NOTIFICATIONSERVERS
read -e -i "${NOTIFICATIONAPIKEY}" -p "Closed Loop Ruby UEB API Key ${NOTIFICATIONTOPIC} topic> " NOTIFICATIONAPIKEY
read -e -i "${NOTIFICATIONAPISECRET}" -p "Closed Loop Ruby UEB API Secret ${NOTIFICATIONTOPIC} topic> " NOTIFICATIONAPISECRET
read -e -i "${DCAETOPIC}" -p "Closed Loop DCAE Topic> " DCAETOPIC
read -e -i "${DCAESERVERS}" -p "Closed Loop DCAE UEB Servers> " DCAESERVERS
read -e -i "${DCAEAPIKEY}" -p "Closed Loop DCAE UEB API Key for ${DCAETOPIC} topic> " DCAEAPIKEY
read -e -i "${DCAEAPISECRET}" -p "Closed Loop DCAE UEB API Secret for ${DCAETOPIC} topic> " DCAEAPISECRET
read -e -i "${AAIURL}" -p "Closed Loop AAI URL> " AAIURL
read -e -i "${AAIUSERNAME}" -p "Closed Loop AAI Username> " AAIUSERNAME
read -e -i "${AAIPASSWORD}" -p "Closed Loop AAI Password> " AAIPASSWORD
read -e -i "${AAINAMEDQUERYUUID}" -p "Closed Loop AAI Named Query UUID> " AAINAMEDQUERYUUID
read -e -i "${AAIPATTERNMATCH}" -p "Closed Loop AAI Pattern Match ('true' or 'false')> " AAINAMEDQUERYUUID
read -e -i "${MSOURL}" -p "Closed Loop MSO URL> " MSOURL
read -e -i "${MSOUSERNAME}" -p "Closed Loop MSO Username> " MSOUSERNAME
read -e -i "${MSOPASSWORD}" -p "Closed Loop MSO Password> " MSOPASSWORD

echo
echo

if [ -z "${GROUPID}" ]; then echo "Aborting: Closed Loop Rules Maven Group Id not provided"; exit 1; fi
if [ -z "${ARTIFACTID}" ]; then echo "Aborting: Closed Loop Rules Maven Coordinates Artifact Id not provided"; exit 1; fi
if [ -z "${VERSION}" ]; then echo "Aborting: Closed Loop Rules Maven Coordinates Version not provided"; exit 1; fi
if [ -z "${PACKAGE}" ]; then echo "Aborting: Closed Loop Rules Package not provided"; exit 1; fi
if [ -z "${CLOSEDLOOPCONTROLNAME}" ]; then echo "Aborting: Closed Loop Template Control Name not provided"; exit 1; fi
if [ -z "${POLICYSCOPE}" ]; then echo "Aborting: Closed Loop Template Policy Scope not provided"; exit 1; fi
if [ -z "${POLICYNAME}" ]; then echo "Aborting: Closed Loop Template Policy Name not provided"; exit 1; fi
if [ -z "${POLICYVERSION}" ]; then echo "Aborting: Closed Loop Template Policy Version not provided"; exit 1; fi
if [ -z "${ACTOR}" ]; then echo "Aborting: Closed Loop Template Actor not provided"; exit 1; fi
if [ -z "${APPCTOPIC}" ]; then echo "Aborting: Closed Loop Template APP-C Recipe Topic not provided"; exit 1; fi
if [ -z "${APPCSERVERS}" ]; then echo "Aborting: Closed Loop Template APP-C UEB Servers not provided"; exit 1; fi
if [ -z "${NOTIFICATIONTOPIC}" ]; then echo "Aborting: Closed Loop Template Ruby Notification Topic not provided"; exit 1; fi
if [ -z "${NOTIFICATIONSERVERS}" ]; then echo "Aborting: Closed Loop Template Ruby UEB Servers not provided"; exit 1; fi
if [ -z "${DCAETOPIC}" ]; then echo "Aborting: Closed Loop Template DCAE DMAAP Topic not provided"; exit 1; fi
if [ -z "${DCAESERVERS}" ]; then echo "Aborting: Closed Loop Template DCAE DMAAP Servers not provided"; exit 1; fi
if [ -z "${AAIURL}" ]; then echo "Aborting: Closed Loop Template AAI URL not provided"; exit 1; fi
if [ -z "${AAIUSERNAME}" ]; then echo "Aborting: Closed Loop Template AAI Username not provided"; exit 1; fi
if [ -z "${AAIPASSWORD}" ]; then echo "Aborting: Closed Loop Template AAI Password not provided"; exit 1; fi
if [ -z "${AAINAMEDQUERYUUID}" ]; then echo "Aborting: Closed Loop Template AAI Named Query UUID not provided"; exit 1; fi
if [ -z "${AAIPATTERNMATCH}" ]; then echo "Aborting: Closed Loop Template AAPI Pattern Match not provided"; exit 1; fi
if [ -z "${MSOURL}" ]; then echo "Aborting: Closed Loop Template MSO URL not provided"; exit 1; fi
if [ -z "${MSOUSERNAME}" ]; then echo "Aborting: Closed Loop Template MSO Username not provided"; exit 1; fi
if [ -z "${MSOPASSWORD}" ]; then echo "Aborting: Closed Loop Template MSO Password not provided"; exit 1; fi

if [ -z "${DCAEAPIKEY}" ]; then DCAEAPIKEY="NULL"; fi
if [ -z "${DCAEAPISECRET}" ]; then DCAEAPISECRET="NULL"; fi
if [ -z "${APPCAPIKEY}" ]; then APPCAPIKEY="NULL"; fi
if [ -z "${APPCAPISECRET}" ]; then APPCAPISECRET="NULL"; fi
if [ -z "${NOTIFICATIONAPIKEY}" ]; then NOTIFICATIONAPIKEY="NULL"; fi
if [ -z "${NOTIFICATIONAPISECRET}" ]; then NOTIFICATIONAPISECRET="NULL"; fi

if [[ "$VERSION" == *-SNAPSHOT ]]; then
	DEPENDENCIES_VERSION="1.0.0-SNAPSHOT"
else
	DEPENDENCIES_VERSION="${VERSION}"
fi

read -e -i "${DEPENDENCIES_VERSION}" -p  "Closed Loop Model/PDP-D dependent version(s) (ie: 1.0.0-SNAPSHOT, 1607.31.1-1, or [1607.31.1,)) > " DEPENDENCIES_VERSION
if [ -z "${DEPENDENCIES_VERSION}" ]; then echo "Aborting: Closed Loop Model/PDP-D dependencies not provided"; exit 1; fi

echo "---------------------------------------------------------------------------------------"
echo "Please review the entered Closed Loop Maven Coordinates and Policy Template Parameters:"
echo
echo "Installation in Local Maven Repository"
echo
echo "Closed Loop Rules Maven Artifact Generation: Group Id: ${GROUP_ID}"
echo "Closed Loop Rules Maven Artifact Generation: Artifact Id: ${ARTIFACTID}"
echo "Closed Loop Rules Maven Artifact Generation: Version: ${VERSION}"
echo "Closed Loop Rules Maven Artifact Generation: Package: ${PACKAGE}"
echo
echo "Closed Loop Template Drools DRL Expansion: Closed Loop Control Name: ${CLOSEDLOOPCONTROLNAME}"
echo "Closed Loop Template Drools DRL Expansion: Closed Loop Policy Scope: ${POLICYSCOPE}"
echo "Closed Loop Template Drools DRL Expansion: Closed Loop Policy Name: ${POLICYNAME}"
echo "Closed Loop Template Drools DRL Expansion: Closed Loop Policy Version: ${POLICYVERSION}"
echo "Closed Loop Template Drools DRL Expansion: Closed Loop Actor: ${ACTOR}"
echo "Closed Loop Template Drools DRL Expansion: Closed Loop Recipe: ${APPC}"
echo "Closed Loop Template Drools DRL Expansion: Closed Loop Recipe Topic: ${APPCTOPIC}"
echo "Closed Loop Template Drools DRL Expansion: Closed Loop Notification Topic: ${NOTIFICATIONTOPIC}"
echo
echo "Closed Loop Controller Configuration: Rules: Group Id: ${GROUP_ID}"
echo "Closed Loop Controller Configuration: Rules: Artifact Id: ${ARTIFACTID}"
echo "Closed Loop Controller Configuration: Rules: Version: ${VERSION}"
echo
echo "Closed Loop Controller Configuration: DCAE UEB Topic: ${DCAETOPIC}"
echo "Closed Loop Controller Configuration: DCAE UEB Servers: ${DCAESERVERS}"
echo "Closed Loop Controller Configuration: DCAE UEB API Key: ${DCAEAPIKEY}"
echo "Closed Loop Controller Configuration: DCAE UEB API Secret: ${DCAEAPISECRET}"
echo
echo "Closed Loop Controller Configuration: APP-C UEB Topic: ${APPCTOPIC}"
echo "Closed Loop Controller Configuration: APP-C UEB Servers: ${APPCSERVERS}"
echo "Closed Loop Controller Configuration: APP-C UEB API Key: ${APPCAPIKEY}"
echo "Closed Loop Controller Configuration: APP-C UEB API Secret: ${APPCAPISECRET}"
echo
echo "Closed Loop Controller Configuration: NOTIFICATION Topic: ${NOTIFICATIONTOPIC}"
echo "Closed Loop Controller Configuration: NOTIFICATION UEB Servers: ${NOTIFICATIONSERVERS}"
echo "Closed Loop Controller Configuration: NOTIFICATION UEB API Key: ${NOTIFICATIONAPIKEY}"
echo "Closed Loop Controller Configuration: NOTIFICATION UEB API Secret: ${NOTIFICATIONAPISECRET}"
echo
echo "Closed Loop Controller Configuration: AAI URL: ${AAIURL}"
echo "Closed Loop Controller Configuration: AAI Username: ${AAIUSERNAME}"
echo "Closed Loop Controller Configuration: AAI Password: ${AAIPASSWORD}"
echo "Closed Loop Controller Configuration: AAI Named Query UUID: ${AAINAMEDQUERYUUID}"
echo "Closed Loop Controller Configuration: AAI Pattern Match: ${AAIPATTERNMATCH}"
echo
echo "Closed Loop Controller Configuration: MSO URL: ${MSOURL}"
echo "Closed Loop Controller Configuration: MSO Username: ${MSOUSERNAME}"
echo "Closed Loop Controller Configuration: MSO Password: ${MSOPASSWORD}"
echo
echo "Closed Loop Model/PDP-D dependent version(s): ${DEPENDENCIES_VERSION}"
echo "---------------------------------------------------------------------------------------"
echo

HAPPY="Y"
read -e -i "${HAPPY}" -p  "Are the previous parameters correct (Y/N)? " HAPPY
if [[ ${HAPPY} != "Y" ]]; then
	exit 1
fi

echo
DIR_TMP="/tmp"
echo "The Closed Loop Source Rules will be installed at ${DIR_TMP}"
read -e -i "${DIR_TMP}" -p  "Do you want to change the Source Rules install directory? " DIR_TMP

if [ ! -w "${DIR_TMP}" ]; then
	echo "Aborting.  ${DIR_TMP} is not writable"
	exit 1
fi

ARCHETYPE_GROUP_ID="org.openecomp.policy.drools-applications"
ARCHETYPE_ARTIFACT_ID="archetype-closedloop-demo-rules"

if [ -d "${DIR_TMP}/${ARTIFACTID}/" ]; then
	if [ "$(ls -A "${DIR_TMP}/${ARTIFACTID}"/)" ]; then
    		echo "${DIR_TMP} already contains a ${ARTIFACTID}/ directory, saving it to ${DIR_TMP}/${ARTIFACTID}.arch.bak/"
    		if [ -d "${DIR_TMP}/${ARTIFACTID}.arch.bak"/ ]; then
    			( 
    				echo "${DIR_TMP}/${ARTIFACTID}.arch.bak/ also exists, deleting it .."
    				cd "${DIR_TMP}"/
    				rm -fr "${ARTIFACTID}.arch.bak"
    			)
    		fi
		/bin/mv --force "${DIR_TMP}/${ARTIFACTID}/" "${DIR_TMP}/${ARTIFACTID}.arch.bak"
		if [ "${?}" -ne 0 ]; then
			echo
			echo
		    echo "Aborting: ${DIR_TMP}/${ARTIFACTID}/ cannot be moved"
		    exit 1
		fi
	else
    		( cd "${DIR_TMP}/" ; rmdir "${DIR_TMP}/${ARTIFACTID}/" )
	fi
fi

CREATEARTIFACT="Y"
read -e -i "${CREATEARTIFACT}" -p  "Create Maven Artifact (Y/N)? " CREATEARTIFACT
if [[ ${CREATEARTIFACT} != "Y" ]]; then
	exit 1
fi

(
cd "${DIR_TMP}"

"$M2_HOME"/bin/mvn archetype:generate \
    -B \
    -DarchetypeCatalog=local \
    -DarchetypeGroupId="${ARCHETYPE_GROUP_ID}" \
    -DarchetypeArtifactId="${ARCHETYPE_ARTIFACT_ID}" \
    -DarchetypeVersion="${VERSION}" \
    -DgroupId="${GROUP_ID}" \
    -DartifactId="${ARTIFACTID}" \
    -Dversion="${VERSION}" \
    -Dpackage="${PACKAGE}" \
    -DclosedLoopControlName="${CLOSEDLOOPCONTROLNAME}" \
    -DpolicyScope="${POLICYSCOPE}" \
    -DpolicyName="${POLICYNAME}" \
    -DpolicyVersion="${POLICYVERSION}" \
    -Dactor="${ACTOR}" \
    -DappcTopic="${APPCTOPIC}" \
    -DappcServers="${APPCSERVERS}" \
    -DappcApiKey="${APPCAPIKEY}" \
    -DappcApiSecret="${APPCAPISECRET}" \
    -DnotificationTopic="${NOTIFICATIONTOPIC}" \
    -DnotificationServers="${NOTIFICATIONSERVERS}" \
    -DnotificationApiKey="${NOTIFICATIONAPIKEY}" \
    -DnotificationApiSecret="${NOTIFICATIONAPISECRET}" \
    -DdcaeTopic="${DCAETOPIC}" \
    -DdcaeServers="${DCAESERVERS}" \
    -DdcaeApiKey="${DCAEAPIKEY}" \
    -DdcaeApiSecret="${DCAEAPISECRET}" \
    -DaaiURL="${AAIURL}" \
    -DaaiUsername="${AAIUSERNAME}" \
    -DaaiPassword="${AAIPASSWORD}" \
    -DaaiNamedQueryUUID="${AAINAMEDQUERYUUID}" \
    -DaaiPatternMatch="${AAIPATTERNMATCH}" \
    -DmsoURL="${MSOURL}" \
    -DmsoUsername="${MSOUSERNAME}" \
    -DmsoPassword="${MSOPASSWORD}" \
    -DdependenciesVersion="${DEPENDENCIES_VERSION}"
    
if [ "${?}" -ne 0 ]; then
	echo
	echo
    echo "Aborting: ${ARTIFACTID} has not been successfully generated"
    exit 1
fi

echo 

cd "${DIR_TMP}/${ARTIFACTID}"/

/bin/mv src/main/config/* .

/bin/sed -i -e "/apiKey=NULL$/d" *-controller.properties
/bin/sed -i -e "/apiSecret=NULL$/d" *-controller.properties

/bin/sed -i -e "/apiKey.*:.*\"NULL\",/d" *-controller.rest.json
/bin/sed -i -e "/apiSecret.*:.*\"NULL\",/d" *-controller.rest.json

echo "Closed Loop Rules from templates have been successfully created under ${DIR_TMP}/${ARTIFACTID}/"

INSTALLREPO="Y"
read -e -i "${INSTALLREPO}" -p  "Do you want to deploy ${ARTIFACTID} rules into maven repository (Y/N)? " INSTALLREPO
if [[ ${INSTALLREPO} != "Y" ]]; then
	exit 1
fi

echo
echo "generating deployable ${ARTIFACTID} maven artifact .."

"$M2_HOME"/bin/mvn install

if [ "${?}" -ne 0 ]; then
	echo
	echo
    echo "Aborting: ${ARTIFACTID} deployable jar cannot be generated"
    exit 1
fi


echo
echo "${ARTIFACTID} has been successfully installed in user's (${USER}) local repository"
echo "Find configuration files at ${DIR_TMP}/${ARTIFACTID}/"
)
