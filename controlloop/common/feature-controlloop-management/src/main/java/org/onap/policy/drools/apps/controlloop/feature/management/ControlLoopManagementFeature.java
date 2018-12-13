/*
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

package org.onap.policy.drools.apps.controlloop.feature.management;

import java.util.stream.Stream;
import org.onap.policy.controlloop.params.ControlLoopParams;
import org.onap.policy.drools.features.PolicyEngineFeatureAPI;
import org.onap.policy.drools.system.PolicyController;

/**
 * Control Loop Management Feature.
 */
public class ControlLoopManagementFeature implements PolicyEngineFeatureAPI {

    private static final String FEATURE_NAME = "controlloop-management";
    private static final int SEQNO = 1000;

    /**
     * retrieves control loops.
     *
     * @param controllerName controller name.
     * @param sessionName session name.
     * @return control loops.
     */
    public static Stream<ControlLoopParams> controlLoops(String controllerName, String sessionName) {
        PolicyController controller = PolicyController.factory.get(controllerName);
        if (controller == null) {
            throw new IllegalArgumentException("Invalid Controller Name");
        }

        if (controller.getDrools().getSessionNames().stream().filter(sessionName::equals).count() <= 0) {
            throw new IllegalArgumentException("Invalid Session Name");
        }

        return controller.getDrools()
            .facts(sessionName, ControlLoopParams.class.getCanonicalName(), false)
            .stream()
            .filter(c -> c instanceof ControlLoopParams)
            .map(c -> (ControlLoopParams) c);
    }

    /**
     * retrieves a control loop.
     *
     * @param controllerName controller name.
     * @param sessionName session name.
     * @param controlLoopName control loop name
     *
     * @return control loops.
     */
    public static ControlLoopParams controlLoop(String controllerName, String sessionName, String controlLoopName) {
        return controlLoops(controllerName, sessionName)
            .filter(c -> c.getClosedLoopControlName().equals(controlLoopName))
            .findFirst()
            .orElse(null);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public int getSequenceNumber() {
        return SEQNO;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public String getName() {
        return FEATURE_NAME;
    }

}
