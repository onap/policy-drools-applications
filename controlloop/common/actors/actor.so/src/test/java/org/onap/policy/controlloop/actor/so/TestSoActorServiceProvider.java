/*-
 * ============LICENSE_START=======================================================
 * TestSOActorServiceProvider
 * ================================================================================
 * Copyright (C) 2018 Ericsson. All rights reserved.
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.onap.policy.aai.AaiNqResponse;
import org.onap.policy.aai.AaiNqResponseWrapper;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.so.SORequest;
import org.onap.policy.so.SORequestParameters;
import org.onap.policy.so.util.Serialization;

public class TestSoActorServiceProvider {

    @Test
    public void testConstructRequest() throws Exception {
        VirtualControlLoopEvent onset = new VirtualControlLoopEvent();
        final ControlLoopOperation operation = new ControlLoopOperation();
        final AaiNqResponseWrapper aaiNqResp = loadAaiResponse(onset, "aai/AaiNqResponse-Full.json");

        final UUID requestId = UUID.randomUUID();
        onset.setRequestId(requestId);

        Policy policy = new Policy();
        policy.setActor("Dorothy");
        policy.setRecipe("GoToOz");
        assertNull(new SOActorServiceProvider().constructRequest(onset, operation, policy, aaiNqResp));

        policy.setActor("SO");
        assertNull(new SOActorServiceProvider().constructRequest(onset, operation, policy, aaiNqResp));

        policy.setRecipe("VF Module Create");

        // empty policy payload
        SORequest request = new SOActorServiceProvider().constructRequest(onset, operation, policy, aaiNqResp);
        assertNotNull(request);

        assertEquals("my_module_3", request.getRequestDetails().getRequestInfo().getInstanceName());
        assertEquals("policy", request.getRequestDetails().getRequestInfo().getRequestorId());
        assertEquals("RegionOne", request.getRequestDetails().getCloudConfiguration().getLcpCloudRegionId());

        // non-empty policy payload
        policy.setPayload(makePayload());
        request = new SOActorServiceProvider().constructRequest(onset, operation, policy, aaiNqResp);
        assertNotNull(request);
        assertEquals(true, request.getRequestDetails().getRequestParameters().isUsePreload());
        assertEquals("avalue", request.getRequestDetails().getRequestParameters().getUserParams().get(0).get("akey"));
        assertEquals(1, request.getRequestDetails().getConfigurationParameters().size());
        assertEquals("cvalue", request.getRequestDetails().getConfigurationParameters().get(0).get("ckey"));

        // null response
        assertNull(new SOActorServiceProvider().constructRequest(onset, operation, policy, null));

        // response has no base VF module
        assertNull(new SOActorServiceProvider().constructRequest(onset, operation, policy,
                        loadAaiResponse(onset, "aai/AaiNqResponse-NoBase.json")));

        // response has no non-base VF modules (other than the "dummy")
        assertNull(new SOActorServiceProvider().constructRequest(onset, operation, policy,
                        loadAaiResponse(onset, "aai/AaiNqResponse-NoNonBase.json")));
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
     * Creates a policy payload containing request & configuration parameters.
     *
     * @return the payload
     */
    private Map<String, String> makePayload() {
        Map<String, String> payload = new TreeMap<>();

        payload.put(SOActorServiceProvider.REQ_PARAM_NM, makeReqParams());
        payload.put(SOActorServiceProvider.CONFIG_PARAM_NM, makeConfigParams());

        return payload;
    }

    /**
     * Creates request parameters.
     *
     * @return request parameters, encoded as JSON
     */
    private String makeReqParams() {
        SORequestParameters params = new SORequestParameters();

        params.setUsePreload(true);

        Map<String, String> map = new TreeMap<>();
        map.put("akey", "avalue");

        List<Map<String, String>> lst = new LinkedList<>();
        lst.add(map);

        params.setUserParams(lst);

        return Serialization.gsonPretty.toJson(params);
    }

    /**
     * Creates configuration parameters.
     *
     * @return configuration parameters, encoded as JSON
     */
    private String makeConfigParams() {
        Map<String, String> map = new TreeMap<>();
        map.put("ckey", "cvalue");

        List<Map<String, String>> lst = new LinkedList<>();
        lst.add(map);

        return Serialization.gsonPretty.toJson(lst);
    }

    /**
     * Reads an AAI vserver named-query response from a file.
     *
     * @param onset the ONSET event
     * @param fileName name of the file containing the JSON response
     * @return output from the AAI vserver named-query
     * @throws IOException if the file cannot be read
     */
    private AaiNqResponseWrapper loadAaiResponse(VirtualControlLoopEvent onset, String fileName) throws IOException {
        String resp = IOUtils.toString(getClass().getResource(fileName), StandardCharsets.UTF_8);
        AaiNqResponse aaiNqResponse = Serialization.gsonPretty.fromJson(resp, AaiNqResponse.class);

        return new AaiNqResponseWrapper(onset.getRequestId(), aaiNqResponse);
    }
}
