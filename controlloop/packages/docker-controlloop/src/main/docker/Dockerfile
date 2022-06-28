#-------------------------------------------------------------------------------
# Dockerfile
# ============LICENSE_START=======================================================
#  Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
#  Modifications Copyright (C) 2022 Nordix Foundation.
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
#
# SPDX-License-Identifier: Apache-2.0
# ============LICENSE_END=========================================================
#-------------------------------------------------------------------------------

FROM onap/policy-drools:@version.policy.drools-pdp@

LABEL maintainer="Policy Team"
LABEL org.opencontainers.image.title="Policy Drools Applications"
LABEL org.opencontainers.image.description="Policy Drools Applications image"
LABEL org.opencontainers.image.url="https://github.com/onap/policy-drools-applications"
LABEL org.opencontainers.image.vendor="ONAP Policy Team"
LABEL org.opencontainers.image.licenses="Apache-2.0"
LABEL org.opencontainers.image.created="${git.build.time}"
LABEL org.opencontainers.image.version="${git.build.version}"
LABEL org.opencontainers.image.revision="${git.commit.id.abbrev}"

ARG BUILD_VERSION_APP_CL=${BUILD_VERSION_APP_CL}
ENV BUILD_VERSION_APP_CL=${BUILD_VERSION_APP_CL}

COPY --chown=policy:policy /maven/apps-controlloop.zip $POLICY_INSTALL/
COPY --chown=policy:policy /maven/pdpd-cl-entrypoint.sh $POLICY_HOME/bin/

RUN unzip apps-controlloop.zip && \
    tar -C $POLICY_HOME -xvf basex-controlloop-$BUILD_VERSION_APP_CL.tar.gz --no-same-owner && \
    chmod 700 $POLICY_HOME/bin/* && \
    chmod 600 $POLICY_HOME/config/* && \
    . $POLICY_HOME/etc/profile.d/env.sh && \
    $POLICY_HOME/bin/features install controlloop-utils controlloop-trans controlloop-management && \
    $POLICY_HOME/bin/features install controlloop-usecases && \
    $POLICY_HOME/bin/features enable healthcheck distributed-locking lifecycle && \
    $POLICY_HOME/bin/features enable controlloop-trans controlloop-management && \
    $POLICY_HOME/bin/features enable controlloop-usecases && \
    $POLICY_HOME/bin/deploy-artifact -l -d -s $POLICY_HOME/etc/m2/standalone-settings.xml \
        -a $POLICY_HOME/features/controlloop-management/lib/feature/feature-controlloop-management-$BUILD_VERSION_APP_CL.jar && \
    $POLICY_HOME/bin/deploy-artifact -l -d -s $POLICY_HOME/etc/m2/standalone-settings.xml \
        -a $POLICY_HOME/features/controlloop-usecases/artifacts/controller-usecases-$BUILD_VERSION_APP_CL.jar && \
    find $HOME/.m2/ -name _maven.repositories -exec rm -v {} \; && \
    find $HOME/.m2/ -name _remote.repositories -exec rm -v {} \; && \
    rm $POLICY_INSTALL/apps-controlloop.zip $POLICY_INSTALL/basex-controlloop-$BUILD_VERSION_APP_CL.tar.gz 2> /dev/null

ENTRYPOINT ["/opt/app/policy/bin/pdpd-cl-entrypoint.sh"]
CMD ["boot"]
