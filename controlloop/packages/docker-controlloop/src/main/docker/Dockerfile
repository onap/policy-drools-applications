FROM onap/policy-drools:1.4-SNAPSHOT-latest

ARG BUILD_VERSION_APP_CL=${BUILD_VERSION_APP_CL}
ENV BUILD_VERSION_APP_CL=${BUILD_VERSION_APP_CL}

SHELL ["/bin/bash", "-l", "-c"]

COPY apps-controlloop.zip ./
RUN unzip apps-controlloop.zip && \
    mkdir -p ${POLICY_HOME}/logs ${POLICY_HOME}/config ${HOME}/.m2 && \
    tar -C ${POLICY_HOME} -xvf base-${BUILD_VERSION}.tar.gz --no-same-owner && \
    tar -C ${POLICY_HOME} -xvf basex-controlloop-${BUILD_VERSION_APP_CL}.tar.gz --no-same-owner && \
    unzip policy-management-${BUILD_VERSION}.zip -d ${POLICY_HOME} && \
    echo "source ${POLICY_HOME}/etc/profile.d/env.sh" >> "${HOME}/.bashrc" && \
    sed -i -e 's!${{POLICY_HOME}}!'"${POLICY_HOME}!g" ${POLICY_HOME}/etc/profile.d/env.sh && \
    chmod 700 ${POLICY_HOME}/bin/* && \
    chmod 600 ${POLICY_HOME}/config/* && \
    rm -f ${POLICY_INSTALL}/*.conf

RUN source ${POLICY_HOME}/etc/profile.d/env.sh && \
    mvn org.apache.maven.plugins:maven-dependency-plugin:3.1.1:get \
        --settings=${POLICY_HOME}/etc/m2/standalone-settings.xml \
        -DartifactId=feature-controlloop-management \
        -DgroupId=org.onap.policy.drools-applications.controlloop.common -Dversion=${BUILD_VERSION_APP_CL} && \
    ${POLICY_HOME}/bin/features install healthcheck distributed-locking && \
    ${POLICY_HOME}/bin/features install controlloop-utils controlloop-trans controlloop-management && \
    ${POLICY_HOME}/bin/features install controlloop-amsterdam controlloop-casablanca && \
    ${POLICY_HOME}/bin/features enable healthcheck distributed-locking && \
    ${POLICY_HOME}/bin/features enable controlloop-utils controlloop-trans controlloop-management && \
    ${POLICY_HOME}/bin/features enable controlloop-amsterdam controlloop-casablanca && \
    rm ${POLICY_INSTALL}/*.zip ${POLICY_INSTALL}/*.gz 2> /dev/null

CMD bash -l
