/*-
 * ============LICENSE_START=======================================================
 * TestSOActorServiceProvider
 * ================================================================================
 * Copyright (C) 2018 Ericsson. All rights reserved.
 * ================================================================================
 * Modifications Copyright (C) 2018 AT&T. All rights reserved.
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.Test;
import org.onap.policy.aai.AaiNqInstanceFilters;
import org.onap.policy.aai.AaiNqRequest;
import org.onap.policy.aai.AaiNqResponse;
import org.onap.policy.aai.AaiNqResponseWrapper;
import org.onap.policy.common.endpoints.http.server.HttpServletServer;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.simulators.AaiSimulatorJaxRs;
import org.onap.policy.so.SORequest;
import org.onap.policy.so.util.Serialization;

public class TestSOActorServiceProvider {

    /**
     * Tear down after test class.
     */
    @AfterClass
    public static void tearDownSimulator() {
        HttpServletServer.factory.destroy();
    }

    @Test
    public void testConstructRequest() {
        VirtualControlLoopEvent onset = new VirtualControlLoopEvent();
        final ControlLoopOperation operation = new ControlLoopOperation();
        final AaiNqResponseWrapper aaiNqResp = getNqVserverFromAai(onset);

        final UUID requestId = UUID.randomUUID();
        onset.setRequestId(requestId);

        Policy policy = new Policy();
        policy.setActor("Dorothy");
        policy.setRecipe("GoToOz");
        assertNull(new SOActorServiceProvider().constructRequest(onset, operation, policy, aaiNqResp));

        policy.setActor("SO");
        assertNull(new SOActorServiceProvider().constructRequest(onset, operation, policy, aaiNqResp));

        policy.setRecipe("VF Module Create");
        SORequest request = new SOActorServiceProvider().constructRequest(onset, operation, policy, aaiNqResp);
        assertNotNull(request);

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

    /**
     * Queries the AAI simulator directly (i.e., bypassing the REST API) to get the
     * vserver named-query response.
     * 
     * @param onset the ONSET event
     * @return output from the AAI vserver named-query
     */
    private AaiNqResponseWrapper getNqVserverFromAai(VirtualControlLoopEvent onset) {
        AaiNqRequest aaiNqRequest = new AaiNqRequest();
        final AaiNqInstanceFilters aaiNqInstanceFilter = new AaiNqInstanceFilters();

        Map<String, Map<String, String>> aaiNqInstanceFilterMap = new HashMap<>();
        Map<String, String> aaiNqInstanceFilterMapItem = new HashMap<>();
        aaiNqInstanceFilterMapItem.put("vserver-name", "my-vserver-name");
        aaiNqInstanceFilterMap.put("vserver", aaiNqInstanceFilterMapItem);
        aaiNqInstanceFilter.getInstanceFilter().add(aaiNqInstanceFilterMap);
        aaiNqRequest.setInstanceFilters(aaiNqInstanceFilter);

        String req = Serialization.gsonPretty.toJson(aaiNqRequest);
        String resp = new AaiSimulatorJaxRs().aaiPostQuery(req);
        AaiNqResponse aaiNqResponse = Serialization.gsonPretty.fromJson(resp, AaiNqResponse.class);

        return new AaiNqResponseWrapper(onset.getRequestId(), aaiNqResponse);
    }
}
