/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.controlloop.eventmanager;

import java.util.Map;
import java.util.Properties;
import lombok.Getter;
import org.onap.policy.common.parameters.ValidationResult;
import org.onap.policy.common.utils.properties.PropertyObjectUtils;
import org.onap.policy.controlloop.actor.xacml.DecisionConfig;
import org.onap.policy.controlloop.actor.xacml.DecisionOperator;
import org.onap.policy.controlloop.actor.xacml.GuardOperation;
import org.onap.policy.controlloop.actor.xacml.XacmlActor;
import org.onap.policy.controlloop.actorserviceprovider.ActorService;
import org.onap.policy.controlloop.actorserviceprovider.Util;
import org.onap.policy.controlloop.ophistory.OperationHistoryDataManager;
import org.onap.policy.controlloop.ophistory.OperationHistoryDataManagerImpl;
import org.onap.policy.controlloop.ophistory.OperationHistoryDataManagerParams;
import org.onap.policy.controlloop.ophistory.OperationHistoryDataManagerStub;
import org.onap.policy.drools.persistence.SystemPersistenceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Services used by the ControlLoopEventManager.
 */
@Getter
public class EventManagerServices {
    public static final Logger logger = LoggerFactory.getLogger(EventManagerServices.class);
    public static final String ACTOR_SERVICE_PROPERTIES = "actor.service";
    public static final String DATA_MANAGER_PROPERTIES = "operation.history";

    public final ActorService actorService = new ActorService();

    // assume we're using a stub until proven otherwise
    public final OperationHistoryDataManager dataManager;

    /**
     * Constructs the object. Configures and starts the actor service. Initializes
     * {@link #dataManager}, to a "real" data manager, if guards are enabled.
     *
     * @param configFileName configuration file name
     */
    public EventManagerServices(String configFileName) {
        // configure and start actor services
        Properties props = startActorService(configFileName);

        if (isGuardEnabled()) {
            // guards are enabled - use a real data manager
            dataManager = makeDataManager(props);
        } else {
            // guards are disabled - use a stub data manager
            dataManager = new OperationHistoryDataManagerStub();
        }
    }

    /**
     * Configures and starts the actor service.
     *
     * @param configFileName configuration file name
     * @return the properties that were loaded from the configuration file
     */
    public Properties startActorService(String configFileName) {
        try {
            var props = SystemPersistenceConstants.getManager().getProperties(configFileName);

            Map<String, Object> parameters = PropertyObjectUtils.toObject(props, ACTOR_SERVICE_PROPERTIES);
            PropertyObjectUtils.compressLists(parameters);

            actorService.configure(parameters);
            actorService.start();

            return props;

        } catch (RuntimeException e) {
            logger.error("cannot configure/start actor service");
            throw new IllegalStateException(e);
        }
    }

    /**
     * Determines if guards are enabled.
     *
     * @return {@code true} if guards are enabled, {@code false} otherwise
     */
    public boolean isGuardEnabled() {
        try {
            DecisionOperator guard = (DecisionOperator) getActorService().getActor(XacmlActor.NAME)
                            .getOperator(GuardOperation.NAME);
            if (!guard.isConfigured()) {
                logger.warn("cannot check 'disabled' property in GUARD actor - assuming disabled");
                return false;
            }

            DecisionConfig config = (DecisionConfig) guard.getCurrentConfig();
            if (config.isDisabled()) {
                logger.warn("guard disabled");
                return false;
            }

            if (!guard.isAlive()) {
                logger.warn("guard actor is not running");
                return false;
            }

            return true;

        } catch (RuntimeException e) {
            logger.warn("cannot check 'disabled' property in GUARD actor - assuming disabled", e);
            return false;
        }
    }

    /**
     * Makes and starts the data manager.
     *
     * @param props properties with which to configure the data manager
     * @return a new data manager
     */
    public OperationHistoryDataManagerImpl makeDataManager(Properties props) {
        try {
            Map<String, Object> parameters = PropertyObjectUtils.toObject(props, DATA_MANAGER_PROPERTIES);
            OperationHistoryDataManagerParams params = Util.translate(DATA_MANAGER_PROPERTIES, parameters,
                            OperationHistoryDataManagerParams.class);
            ValidationResult result = params.validate(DATA_MANAGER_PROPERTIES);
            if (!result.isValid()) {
                throw new IllegalArgumentException("invalid data manager properties:\n" + result.getResult());
            }

            var mgr = new OperationHistoryDataManagerImpl(params);
            mgr.start();

            return mgr;

        } catch (RuntimeException e) {
            logger.error("cannot start operation history data manager");
            actorService.stop();
            throw e;
        }
    }
}
