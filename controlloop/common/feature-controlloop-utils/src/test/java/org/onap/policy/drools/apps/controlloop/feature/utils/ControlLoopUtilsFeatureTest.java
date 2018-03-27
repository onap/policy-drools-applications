/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.onap.policy.drools.http.server.HttpServletServer;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.drools.utils.logging.LoggerUtil;
import org.onap.policy.simulators.Util;

/**
 * ControlLoopUtilsFeature JUnit Tests.
 */
public class ControlLoopUtilsFeatureTest {

    @Test
    public void simulate() {
        LoggerUtil.setLevel("ROOT", "INFO");
        LoggerUtil.setLevel("org.eclipse.jetty", "WARN");
        final ControlLoopUtilsFeature feature = new ControlLoopUtilsFeature();
        feature.afterStart(PolicyEngine.manager);
        assertNotNull(HttpServletServer.factory.get(Util.AAISIM_SERVER_PORT));
        assertNotNull(HttpServletServer.factory.get(Util.SOSIM_SERVER_PORT));
        assertNotNull(HttpServletServer.factory.get(Util.SOSIM_SERVER_PORT));
        assertNotNull(HttpServletServer.factory.get(Util.GUARDSIM_SERVER_PORT));
    }

}
