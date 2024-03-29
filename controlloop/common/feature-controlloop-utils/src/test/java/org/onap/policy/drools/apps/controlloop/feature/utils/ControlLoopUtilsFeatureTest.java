/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2017-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023 Nordix Foundation.
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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.common.utils.logging.LoggerUtils;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.simulators.Util;

/**
 * ControlLoopUtilsFeature JUnit Tests.
 */
class ControlLoopUtilsFeatureTest {

    @Test
    void testSimulate() {
        LoggerUtils.setLevel("ROOT", "INFO");
        LoggerUtils.setLevel("org.eclipse.jetty", "WARN");
        final var feature = new ControlLoopUtilsFeature();
        feature.afterStart(PolicyEngineConstants.getManager());
        assertNotNull(HttpServletServerFactoryInstance.getServerFactory().get(Util.AAISIM_SERVER_PORT));
        assertNotNull(HttpServletServerFactoryInstance.getServerFactory().get(Util.SOSIM_SERVER_PORT));
        assertNotNull(HttpServletServerFactoryInstance.getServerFactory().get(Util.SOSIM_SERVER_PORT));
        assertNotNull(HttpServletServerFactoryInstance.getServerFactory().get(Util.XACMLSIM_SERVER_PORT));
        assertNotNull(HttpServletServerFactoryInstance.getServerFactory().get(Util.SDNCSIM_SERVER_PORT));
    }

}
