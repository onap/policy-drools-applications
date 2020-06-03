FROM onap/policy-drools:@version.policy.drools-pdp@

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
    $POLICY_HOME/bin/features install controlloop-frankfurt && \
    $POLICY_HOME/bin/features enable healthcheck distributed-locking lifecycle && \
    $POLICY_HOME/bin/features enable controlloop-trans controlloop-management && \
    $POLICY_HOME/bin/features enable controlloop-frankfurt && \
    $POLICY_HOME/bin/deploy-artifact -l -d -s $POLICY_HOME/etc/m2/standalone-settings.xml \
        -a $POLICY_HOME/features/controlloop-management/lib/feature/feature-controlloop-management-$BUILD_VERSION_APP_CL.jar && \
    $POLICY_HOME/bin/deploy-artifact -l -d -s $POLICY_HOME/etc/m2/standalone-settings.xml \
        -a $POLICY_HOME/features/controlloop-frankfurt/artifacts/controller-frankfurt-$BUILD_VERSION_APP_CL.jar && \
    find $HOME/.m2/ -name _maven.repositories -exec rm -v {} \; && \
    find $HOME/.m2/ -name _remote.repositories -exec rm -v {} \; && \
    rm $POLICY_INSTALL/apps-controlloop.zip $POLICY_INSTALL/basex-controlloop-$BUILD_VERSION_APP_CL.tar.gz 2> /dev/null

ENTRYPOINT ["/opt/app/policy/bin/pdpd-cl-entrypoint.sh"]
CMD ["boot"]
