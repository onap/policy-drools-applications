/*-
 * ============LICENSE_START=======================================================
 * m2/base
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.m2.base;

import org.onap.policy.drools.core.PolicyContainer;
import org.onap.policy.drools.core.PolicySession;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.drools.system.PolicyControllerConstants;
import org.onap.policy.drools.system.PolicyEngineConstants;

/**
 * This class contains static utility methods.
 */
public class Util {
    // Add a private Util to hide the implicit public one.
    private Util() {
        //not called
    }

    /**
     * Find the PolicyController associated with the specified PolicySession.
     *
     * @param session the current PolicySession
     * @return the associated PolicyController (or 'null' if not found)
     */
    public static PolicyController getPolicyController(PolicySession session) {
        PolicyContainer container = session.getPolicyContainer();
        return PolicyControllerConstants.getFactory().get(
                   container.getGroupId(), container.getArtifactId());
    }

    /**
     * Send a UEB/DMAAP message to the specified topic, using the specified
     * PolicyController.
     *
     * @param topic UEB/DMAAP topic
     * @param event the message to encode, and send
     * @return 'true' if successful, 'false' if delivery failed
     * @throws IllegalStateException if the topic can't be found,
     *     or there are multiple topics with the same name
     */
    public static boolean deliver(String topic, Object event) {
        return PolicyEngineConstants.getManager().deliver(topic, event);
    }
}
