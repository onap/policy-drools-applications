FROM onap/policy-drools:1.6-SNAPSHOT-latest

ARG BUILD_VERSION_APP_CL=${BUILD_VERSION_APP_CL}
ENV BUILD_VERSION_APP_CL=${BUILD_VERSION_APP_CL}

COPY /maven/apps-controlloop.zip $POLICY_INSTALL/

RUN unzip apps-controlloop.zip && \
    mkdir -p $POLICY_HOME/logs $POLICY_HOME/config $HOME/.m2 && \
    tar -C $POLICY_HOME -xvf base-${BUILD_VERSION_DROOLS}.tar.gz --no-same-owner && \
    tar -C $POLICY_HOME -xvf basex-controlloop-$BUILD_VERSION_APP_CL.tar.gz --no-same-owner && \
    unzip policy-management-${BUILD_VERSION_DROOLS}.zip -d $POLICY_HOME && \
    echo "source $POLICY_HOME/etc/profile.d/env.sh" >> "$HOME/.bashrc" && \
    chmod 700 $POLICY_HOME/bin/* && \
    chmod 600 $POLICY_HOME/config/* && \
    chmod 700 $POLICY_INSTALL/*.sh && \
    rm -f $POLICY_INSTALL/*.conf

COPY /maven/docker-entrypoint.sh $POLICY_HOME/bin/

RUN . $POLICY_HOME/etc/profile.d/env.sh && \
    $POLICY_HOME/bin/features install healthcheck distributed-locking lifecycle && \
    $POLICY_HOME/bin/features install controlloop-utils controlloop-trans controlloop-management && \
    $POLICY_HOME/bin/features install controlloop-amsterdam controlloop-usecases && \
    $POLICY_HOME/bin/features enable healthcheck distributed-locking lifecycle && \
    $POLICY_HOME/bin/features enable controlloop-trans controlloop-management && \
    $POLICY_HOME/bin/features enable controlloop-amsterdam controlloop-usecases && \
    $POLICY_HOME/bin/deploy-artifact -l -d -s $POLICY_HOME/etc/m2/standalone-settings.xml \
        -a $POLICY_HOME/features/controlloop-management/lib/feature/feature-controlloop-management-$BUILD_VERSION_APP_CL.jar && \
    $POLICY_HOME/bin/deploy-artifact -l -d -s $POLICY_HOME/etc/m2/standalone-settings.xml \
        -a $POLICY_HOME/features/controlloop-usecases/artifacts/controller-usecases-$BUILD_VERSION_APP_CL.jar && \
    find $HOME/.m2/ -name _maven.repositories -exec rm -v {} \; && \
    find $HOME/.m2/ -name _remote.repositories -exec rm -v {} \; && \
    rm $POLICY_INSTALL/*.zip $POLICY_INSTALL/*.gz 2> /dev/null

ENTRYPOINT ["/opt/app/policy/bin/docker-entrypoint.sh"]
CMD ["boot"]
