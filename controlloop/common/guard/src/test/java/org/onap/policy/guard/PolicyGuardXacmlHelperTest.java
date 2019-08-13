/*-
 * ============LICENSE_START=======================================================
 * guard
 * ================================================================================
 * Copyright (C) 2017-2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.guard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Properties;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.drools.utils.logging.LoggerUtil;

public class PolicyGuardXacmlHelperTest {

    private static final String TARGET = "target";
    private static final String REQUEST_ID = "requestId";
    private static final String RECIPE = "recipe";
    private static final String GUARD_URL = "guard.url";
    private static final String ACTOR = "actor";
    private static final Integer VF_COUNT = 100;

    /**
     * Set up test class.
     */
    @BeforeClass
    public static void setupSimulator() throws Exception {
        LoggerUtil.setLevel("ROOT", "INFO");
        LoggerUtil.setLevel("org.eclipse.jetty", "WARN");

        HttpServletServerFactoryInstance.getServerFactory().destroy();
        org.onap.policy.simulators.Util.buildGuardSim();

        //
        // Set guard properties
        //
        org.onap.policy.guard.Util.setGuardEnvProps("http://localhost:6669/policy/pdpx/v1/decision", "python", "test");
    }

    /**
     * Shuts down simulator and performs 1 more test for the case where the connection fails.
     */
    @AfterClass
    public static void tearDownSimulator() {
        HttpServletServerFactoryInstance.getServerFactory().destroy();

        // Null/ Bad Connection Case
        PolicyGuardXacmlRequestAttributes xacmlReq = new PolicyGuardXacmlRequestAttributes(
                        org.onap.policy.simulators.GuardSimulatorJaxRs.DENY_CLNAME, ACTOR, RECIPE, TARGET,
                        REQUEST_ID, VF_COUNT);
        String rawDecision = new PolicyGuardXacmlHelper().callPdp(xacmlReq);
        assertNotNull(rawDecision);
        assertEquals(Util.DENY, rawDecision);
    }

    @Test
    public void testSimulator() {
        PolicyGuardXacmlRequestAttributes request = new PolicyGuardXacmlRequestAttributes("clname_id", "actor_id",
                        "operation_id", "target_id", "request_id", VF_COUNT);
        String xacmlResponse = new PolicyGuardXacmlHelper().callPdp(request);
        assertNotNull(xacmlResponse);
    }

    @Test
    /**
     * Tests PolicyGuardXacmlHelper.callPdp method to determine if it returns DENY, PERMIT, or
     * INDETERMINATE as expected.
     */
    public void testCallPdp() {
        // Deny Case
        PolicyGuardXacmlRequestAttributes xacmlReq = new PolicyGuardXacmlRequestAttributes(
                        org.onap.policy.simulators.GuardSimulatorJaxRs.DENY_CLNAME, ACTOR, RECIPE, TARGET,
                        REQUEST_ID, VF_COUNT);
        String rawDecision = new PolicyGuardXacmlHelper().callPdp(xacmlReq);
        assertNotNull(rawDecision);
        assertEquals(Util.DENY, rawDecision);

        // Permit Case
        xacmlReq = new PolicyGuardXacmlRequestAttributes("clname", ACTOR, RECIPE, TARGET, REQUEST_ID, VF_COUNT);
        rawDecision = new PolicyGuardXacmlHelper().callPdp(xacmlReq);
        assertNotNull(rawDecision);
        assertEquals(Util.PERMIT, rawDecision);

        // Indeterminate case is in tearDown for efficiency
    }

    @Test
    public void testInit() {
        final Properties savedEnvironment = (Properties) PolicyEngineConstants.getManager().getEnvironment().clone();

        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngineConstants.getManager().getEnvironment().setProperty(GUARD_URL,
                "http://localhost:6669/pdp/api/getDecision,Dorothy");
        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngineConstants.getManager().getEnvironment().setProperty(GUARD_URL,
                "http://localhost:6669/pdp/api/getDecision,Dorothy,Toto");
        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngineConstants.getManager().getEnvironment().setProperty(GUARD_URL,
                "http://localhost:6969/policy/pdpx/v1/decision");

        PolicyEngineConstants.getManager().getEnvironment().setProperty("pdpx.timeout", "thisIsNotANumber");
        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngineConstants.getManager().getEnvironment().setProperty("pdpx.timeout", "1000");
        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngineConstants.getManager().getEnvironment().remove("pdpx.password");
        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngineConstants.getManager().getEnvironment().setProperty("pdpx.username", "python");
        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngineConstants.getManager().getEnvironment().setProperty(GUARD_URL, "///");
        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngineConstants.getManager().getEnvironment().setProperty("guard.disabled", "");
        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngineConstants.getManager().getEnvironment().setProperty("guard.disabled", "true");
        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngineConstants.getManager().getEnvironment().clear();
        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngineConstants.getManager().setEnvironment(savedEnvironment);
    }
}
