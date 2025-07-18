/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2017-2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023,2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.drools.apps.controlloop.feature.utils;

import org.onap.policy.drools.features.PolicyEngineFeatureApi;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.simulators.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PDP-D feature for lab environments that provides Server simulation capabilities for AAI, SO, SDNC
 * and VFC.
 *
 */
public class ControlLoopUtilsFeature implements PolicyEngineFeatureApi {

    private static final Logger logger = LoggerFactory.getLogger(ControlLoopUtilsFeature.class);

    @Override
    public boolean afterStart(PolicyEngine engine) {
        try {
            Util.buildAaiSim();
            Util.buildSoSim();
            Util.buildXacmlSim();
            Util.buildSdncSim();
        } catch (final InterruptedException e) {
            logger.error("{}: initialization aborted", ControlLoopUtilsFeature.class.getName(), e);
            Thread.currentThread().interrupt();
        }
        return false;
    }

    @Override
    public int getSequenceNumber() {
        return 100000;
    }

}
