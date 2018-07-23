/*-
 * ============LICENSE_START=======================================================
 * TestSOActorServiceProvider
 * ================================================================================
 * Copyright (C) 2018 Ericsson. All rights reserved.
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

package org.onap.policy.controlloop.actor.so;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.common.endpoints.http.server.impl.IndexedHttpServletServerFactory;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.simulators.Util;
import org.onap.policy.so.SORequest;

public class TestSOActorServiceProvider {

    /**
     * Set up for test class.
     */
    @BeforeClass
    public static void setUpSimulator() {
        try {
            Util.buildAaiSim();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Tear down after test class.
     */
    @AfterClass
    public static void tearDownSimulator() {
        IndexedHttpServletServerFactory.getInstance().destroy();
    }

    @Test
    public void testConstructRequest() {
        VirtualControlLoopEvent onset = new VirtualControlLoopEvent();
        final ControlLoopOperation operation = new ControlLoopOperation();

        final UUID requestId = UUID.randomUUID();
        onset.setRequestId(requestId);

        PolicyEngine.manager.setEnvironmentProperty("aai.url", "http://localhost:6666");
        PolicyEngine.manager.setEnvironmentProperty("aai.username", "AAI");
        PolicyEngine.manager.setEnvironmentProperty("aai.password", "AAI");

        Policy policy = new Policy();
        policy.setActor("Dorothy");
        policy.setRecipe("GoToOz");
        assertNull(new SOActorServiceProvider().constructRequest(onset, operation, policy));

        policy.setActor("SO");
        assertNull(new SOActorServiceProvider().constructRequest(onset, operation, policy));

        policy.setRecipe("VF Module Create");
        assertNotNull(new SOActorServiceProvider().constructRequest(onset, operation, policy));

        PolicyEngine.manager.setEnvironmentProperty("aai.url", "http://localhost:999999");
        assertNull(new SOActorServiceProvider().constructRequest(onset, operation, policy));

        PolicyEngine.manager.setEnvironmentProperty("aai.url", "http://localhost:6666");
        assertNotNull(new SOActorServiceProvider().constructRequest(onset, operation, policy));

        SORequest request = new SOActorServiceProvider().constructRequest(onset, operation, policy);

        assertEquals("policy", request.getRequestDetails().getRequestInfo().getRequestorId());
        assertEquals("RegionOne", request.getRequestDetails().getCloudConfiguration().getLcpCloudRegionId());
    }

    @Test
    public void testSendRequest() {
        try {
            SOActorServiceProvider.sendRequest(UUID.randomUUID().toString(), null, null);
        } catch (Exception e) {
            fail("Test should not throw an exception");
        }
    }

    @Test
    public void testMethods() {
        SOActorServiceProvider sp = new SOActorServiceProvider();

        assertEquals("SO", sp.actor());
        assertEquals(1, sp.recipes().size());
        assertEquals("VF Module Create", sp.recipes().get(0));
        assertEquals(0, sp.recipePayloads("VF Module Create").size());
    }
}
