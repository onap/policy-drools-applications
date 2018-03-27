/*-
 * ============LICENSE_START=======================================================
 * simulators
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

package org.onap.policy.simulators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.drools.http.server.HttpServletServer;
import org.onap.policy.drools.utils.logging.LoggerUtil;
import org.onap.policy.rest.RESTManager;
import org.onap.policy.rest.RESTManager.Pair;

public class GuardSimulatorTest {

    /**
     * Set up test class.
     */
    @BeforeClass
    public static void setupSimulator() {
        LoggerUtil.setLevel("ROOT", "INFO");
        LoggerUtil.setLevel("org.eclipse.jetty", "WARN");
        try {
            org.onap.policy.simulators.Util.buildGuardSim();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @AfterClass
    public static void tearDownSimulator() {
        HttpServletServer.factory.destroy();
    }

    @Test
    public void testGuard() {
        String request = makeRequest("test_actor_id", "test_op_id", "test_target", "test_clName");
        String url = "http://localhost:" + Util.GUARDSIM_SERVER_PORT + "/pdp/api/getDecision";
        Pair<Integer, String> response =
                new RESTManager().post(url, "testUname", "testPass", null, "application/json", request);
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        assertEquals("{\"decision\": \"PERMIT\", \"details\": \"Decision Permit. OK!\"}", response.b);

        request = makeRequest("test_actor_id", "test_op_id", "test_target", "denyGuard");
        response = new RESTManager().post(url, "testUname", "testPass", null, "application/json", request);
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        assertEquals("{\"decision\": \"DENY\", \"details\": \"Decision Deny. You asked for it\"}", response.b);
    }

    private static String makeRequest(String actor, String recipe, String target, String clName) {
        return "{\"decisionAttributes\": {\"actor\": \"" + actor + "\", \"recipe\": \"" + recipe + "\""
                + ", \"target\": \"" + target + "\", \"clname\": \"" + clName + "\"}, \"onapName\": \"PDPD\"}";
    }
}
