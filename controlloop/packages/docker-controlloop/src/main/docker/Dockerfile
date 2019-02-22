FROM onap/policy-drools

ARG BUILD_VERSION_APP_CL=${BUILD_VERSION_APP_CL}
ENV BUILD_VERSION_APP_CL=${BUILD_VERSION_APP_CL}

ADD apps-controlloop.zip ./
RUN unzip apps-controlloop.zip
