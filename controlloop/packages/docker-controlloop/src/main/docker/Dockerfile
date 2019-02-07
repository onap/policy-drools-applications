FROM onap/policy-drools:1.4-SNAPSHOT-latest

ARG BUILD_VERSION_APP_CL=${BUILD_VERSION_APP_CL}
ENV BUILD_VERSION_APP_CL=${BUILD_VERSION_APP_CL}

ADD apps-controlloop.zip ./
RUN unzip apps-controlloop.zip
