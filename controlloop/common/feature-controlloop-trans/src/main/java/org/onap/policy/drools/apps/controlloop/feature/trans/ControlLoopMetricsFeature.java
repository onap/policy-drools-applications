/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.drools.apps.controlloop.feature.trans;

import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.drools.features.PolicyControllerFeatureAPI;
import org.onap.policy.drools.system.PolicyController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Feature that tracks Transactions by observing Notification Patterns.
 */
public class ControlLoopMetricsFeature implements PolicyControllerFeatureAPI {

    /**
     * Feature Sequence Priority
     */
    public final static int FEATURE_SEQUENCE_PRIORITY = 100000;

    /**
     * Properties Configuration Name
     */
    public static final String CONFIGURATION_PROPERTIES_NAME = "feature-controlloop-trans";

    /**
     * maximum number of transaction cache entries
     */
    public static final String CL_CACHE_TRANS_SIZE_PROPERTY = "controlloop.cache.transactions.size";
    public static final int CL_CACHE_TRANS_SIZE_DEFAULT = 100;

    /**
     * transaction timeout in minutes
     */
    public static final String CL_CACHE_TRANS_TIMEOUT_SECONDS_PROPERTY =
            "controllop.cache.transactions.timeout.seconds";
    public static final long CL_CACHE_TRANS_TIMEOUT_SECONDS_DEFAULT = 1L * 60 * 60;

    @Override
    public boolean afterShutdown(PolicyController controller) {
        return false;
    }

    /**
     * Logger
     */
    private static Logger logger = LoggerFactory.getLogger(ControlLoopMetricsFeature.class);

    /**
     * Intercept Control Loop Notifications
     *
     * @param controller - controller
     * @param protocol - protocol
     * @param topic - topic
     * @param event - event object
     * @return
     */
    @Override
    public boolean beforeDeliver(PolicyController controller, CommInfrastructure protocol, String topic, Object event) {
        if (event instanceof VirtualControlLoopNotification) {
            ControlLoopMetrics.manager.transactionEvent(controller, (VirtualControlLoopNotification) event);
        }

        /* do not take ownership */
        return false;
    }

    @Override
    public int getSequenceNumber() {
        return FEATURE_SEQUENCE_PRIORITY;
    }

}
