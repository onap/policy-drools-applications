/*-
 * ============LICENSE_START=======================================================
 * AppcServiceProviderTest
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.controlloop.actor.appclcm;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.aai.util.AAIException;
import org.onap.policy.appclcm.LCMCommonHeader;
import org.onap.policy.appclcm.LCMRequest;
import org.onap.policy.appclcm.LCMRequestWrapper;
import org.onap.policy.appclcm.LCMResponse;
import org.onap.policy.appclcm.LCMResponseWrapper;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.ControlLoopTargetType;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.controlloop.policy.Target;
import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.drools.http.server.HttpServletServer;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.simulators.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppcLcmServiceProviderTest {
    
    private static final Logger logger = LoggerFactory.getLogger(AppcLcmServiceProviderTest.class);
    
    private static VirtualControlLoopEvent onsetEvent;
    private static ControlLoopOperation operation;
    private static Policy policy;
    private static LCMRequestWrapper dmaapRequest;
    private static LCMResponseWrapper dmaapResponse;

    static {
        /* 
         * Construct an onset with an AAI subtag containing
         * generic-vnf.vnf-id and a target type of VM.
         */
        onsetEvent = new VirtualControlLoopEvent();
        onsetEvent.closedLoopControlName = "closedLoopControlName-Test";
        onsetEvent.requestID = UUID.randomUUID();
        onsetEvent.closedLoopEventClient = "tca.instance00001";
        onsetEvent.target_type = ControlLoopTargetType.VM;
        onsetEvent.target = "generic-vnf.vnf-name";
        onsetEvent.from = "DCAE";
        onsetEvent.closedLoopAlarmStart = Instant.now();
        onsetEvent.AAI = new HashMap<>();
        onsetEvent.AAI.put("generic-vnf.vnf-name", "fw0001vm001fw001");
        onsetEvent.closedLoopEventStatus = ControlLoopEventStatus.ONSET;

        /* Construct an operation with an APPC actor and restart operation. */
        operation = new ControlLoopOperation();
        operation.actor = "APPC";
        operation.operation = "Restart";
        operation.target = "VM";
        operation.end = Instant.now();
        operation.subRequestId = "1";
        
        /* Construct a policy specifying to restart vm. */
        policy = new Policy();
        policy.setName("Restart the VM");
        policy.setDescription("Upon getting the trigger event, restart the VM");
        policy.setActor("APPC");
        policy.setTarget(new Target(TargetType.VNF));
        policy.setRecipe("Restart");
        policy.setPayload(null);
        policy.setRetry(2);
        policy.setTimeout(300);

        /* A sample DMAAP request wrapper. */
        dmaapRequest = new LCMRequestWrapper();
        dmaapRequest.setCorrelationId(onsetEvent.requestID.toString() + "-" + "1");
        dmaapRequest.setRpcName(policy.getRecipe().toLowerCase());
        dmaapRequest.setType("request");

        /* A sample DMAAP response wrapper */
        dmaapResponse = new LCMResponseWrapper();
        dmaapResponse.setCorrelationId(onsetEvent.requestID.toString() + "-" + "1");
        dmaapResponse.setRpcName(policy.getRecipe().toLowerCase());
        dmaapResponse.setType("response");
        
        /* Set environment properties */
        PolicyEngine.manager.setEnvironmentProperty("aai.url", "http://localhost:6666");
        PolicyEngine.manager.setEnvironmentProperty("aai.username", "AAI");
        PolicyEngine.manager.setEnvironmentProperty("aai.password", "AAI");
        
        /* A sample APPC LCM request. */
        LCMRequest appcRequest = new LCMRequest();
        
        /* The following code constructs a sample APPC LCM Request */
        appcRequest.setAction("restart");
        
        HashMap<String, String> actionIdentifiers = new HashMap<>();
        actionIdentifiers.put("vnf-id", "trial-vnf-003");
        
        appcRequest.setActionIdentifiers(actionIdentifiers);
        
        LCMCommonHeader commonHeader = new LCMCommonHeader();
        commonHeader.setRequestId(onsetEvent.requestID);
        commonHeader.setSubRequestId("1");
        commonHeader.setOriginatorId(onsetEvent.requestID.toString());
        
        appcRequest.setCommonHeader(commonHeader);
        
        appcRequest.setPayload(null);

        dmaapRequest.setBody(appcRequest);

        /* The following code constructs a sample APPC LCM Response */
        LCMResponse appcResponse = new LCMResponse(appcRequest);
        appcResponse.getStatus().setCode(400);
        appcResponse.getStatus().setMessage("Restart Successful");

        dmaapResponse.setBody(appcResponse);
    }
    
    @BeforeClass
    public static void setUpSimulator() {
        try {
            Util.buildAaiSim();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @AfterClass
    public static void tearDownSimulator() {
        HttpServletServer.factory.destroy();
    }
    
    /**
     * A test to construct an APPC LCM restart request.
     */
    @Test
    public void constructRestartRequestTest() {
        
        LCMRequestWrapper dmaapRequest = null;
        try {
            dmaapRequest = AppcLcmActorServiceProvider.constructRequest(onsetEvent, operation, policy, "vnf01");
        } catch (AAIException e) {
            logger.warn(e.toString());
            fail("no vnfid found");
        }

        /* The service provider must return a non null DMAAP request wrapper */
        assertNotNull(dmaapRequest);

        /* The DMAAP wrapper's type field must be request */
        assertEquals(dmaapRequest.getType(), "request");
        
        /* The DMAAP wrapper's body field cannot be null */
        assertNotNull(dmaapRequest.getBody());

        LCMRequest appcRequest = dmaapRequest.getBody();
        
        /* A common header is required and cannot be null */
        assertNotNull(appcRequest.getCommonHeader());
        assertEquals(appcRequest.getCommonHeader().getRequestId(), onsetEvent.requestID);

        /* An action is required and cannot be null */
        assertNotNull(appcRequest.getAction());
        assertEquals(appcRequest.getAction(), "Restart");

        /* Action Identifiers are required and cannot be null */
        assertNotNull(appcRequest.getActionIdentifiers());
        assertNotNull(appcRequest.getActionIdentifiers().get("vnf-id"));
        assertEquals(appcRequest.getActionIdentifiers().get("vnf-id"), "vnf01");
        
        logger.debug("APPC Request: \n" + appcRequest.toString());
    }

    /**
     * A test to process a successful APPC restart response.
     */
    @Test
    public void processRestartResponseSuccessTest() {
        AbstractMap.SimpleEntry<PolicyResult, String> result = AppcLcmActorServiceProvider
                .processResponse(dmaapResponse);
        assertEquals(result.getKey(), PolicyResult.SUCCESS);
        assertEquals(result.getValue(), "Restart Successful");
    }
    
    /**
     * A test to map APPC response results to corresponding Policy results
     */
    @Test
    public void appcToPolicyResultTest() {
        
        AbstractMap.SimpleEntry<PolicyResult, String> result;
        
        /* If APPC accepts, PolicyResult is null */
        dmaapResponse.getBody().getStatus().setCode(100);
        dmaapResponse.getBody().getStatus().setMessage("ACCEPTED");
        result = AppcLcmActorServiceProvider.processResponse(dmaapResponse);
        assertEquals(result.getKey(), null);
        
        /* If APPC is successful, PolicyResult is success */
        dmaapResponse.getBody().getStatus().setCode(400);
        dmaapResponse.getBody().getStatus().setMessage("SUCCESS");
        result = AppcLcmActorServiceProvider.processResponse(dmaapResponse);
        assertEquals(result.getKey(), PolicyResult.SUCCESS);
        
        /* If APPC returns an error, PolicyResult is failure exception */
        dmaapResponse.getBody().getStatus().setCode(200);
        dmaapResponse.getBody().getStatus().setMessage("ERROR");
        result = AppcLcmActorServiceProvider.processResponse(dmaapResponse);
        assertEquals(result.getKey(), PolicyResult.FAILURE_EXCEPTION);
        
        /* If APPC rejects, PolicyResult is failure exception */
        dmaapResponse.getBody().getStatus().setCode(300);
        dmaapResponse.getBody().getStatus().setMessage("REJECT");
        result = AppcLcmActorServiceProvider.processResponse(dmaapResponse);
        assertEquals(result.getKey(), PolicyResult.FAILURE_EXCEPTION);
        
        /* Test multiple reject codes */
        dmaapResponse.getBody().getStatus().setCode(306);
        dmaapResponse.getBody().getStatus().setMessage("REJECT");
        result = AppcLcmActorServiceProvider.processResponse(dmaapResponse);
        assertEquals(result.getKey(), PolicyResult.FAILURE_EXCEPTION);
        
        dmaapResponse.getBody().getStatus().setCode(313);
        dmaapResponse.getBody().getStatus().setMessage("REJECT");
        result = AppcLcmActorServiceProvider.processResponse(dmaapResponse);
        assertEquals(result.getKey(), PolicyResult.FAILURE_EXCEPTION);
        
        /* If APPC returns failure, PolicyResult is failure */
        dmaapResponse.getBody().getStatus().setCode(401);
        dmaapResponse.getBody().getStatus().setMessage("FAILURE");
        result = AppcLcmActorServiceProvider.processResponse(dmaapResponse);
        assertEquals(result.getKey(), PolicyResult.FAILURE);
        
        /* Test multiple failure codes */
        dmaapResponse.getBody().getStatus().setCode(406);
        dmaapResponse.getBody().getStatus().setMessage("FAILURE");
        result = AppcLcmActorServiceProvider.processResponse(dmaapResponse);
        assertEquals(result.getKey(), PolicyResult.FAILURE);
        
        dmaapResponse.getBody().getStatus().setCode(450);
        dmaapResponse.getBody().getStatus().setMessage("FAILURE");
        result = AppcLcmActorServiceProvider.processResponse(dmaapResponse);
        assertEquals(result.getKey(), PolicyResult.FAILURE);
        
        /* If APPC returns partial success, PolicyResult is failure exception */
        dmaapResponse.getBody().getStatus().setCode(500);
        dmaapResponse.getBody().getStatus().setMessage("PARTIAL SUCCESS");
        result = AppcLcmActorServiceProvider.processResponse(dmaapResponse);
        assertEquals(result.getKey(), PolicyResult.FAILURE_EXCEPTION);
        
        /* If APPC returns partial failure, PolicyResult is failure exception */
        dmaapResponse.getBody().getStatus().setCode(501);
        dmaapResponse.getBody().getStatus().setMessage("PARTIAL FAILURE");
        result = AppcLcmActorServiceProvider.processResponse(dmaapResponse);
        assertEquals(result.getKey(), PolicyResult.FAILURE_EXCEPTION);
        
        /* Test multiple partial failure codes */
        dmaapResponse.getBody().getStatus().setCode(599);
        dmaapResponse.getBody().getStatus().setMessage("PARTIAL FAILURE");
        result = AppcLcmActorServiceProvider.processResponse(dmaapResponse);
        assertEquals(result.getKey(), PolicyResult.FAILURE_EXCEPTION);
        
        dmaapResponse.getBody().getStatus().setCode(550);
        dmaapResponse.getBody().getStatus().setMessage("PARTIAL FAILURE");
        result = AppcLcmActorServiceProvider.processResponse(dmaapResponse);
        assertEquals(result.getKey(), PolicyResult.FAILURE_EXCEPTION);
        
        /* If APPC code is unknown to Policy, PolicyResult is failure exception */
        dmaapResponse.getBody().getStatus().setCode(700);
        dmaapResponse.getBody().getStatus().setMessage("UNKNOWN");
        result = AppcLcmActorServiceProvider.processResponse(dmaapResponse);
        assertEquals(result.getKey(), PolicyResult.FAILURE_EXCEPTION);
    }
    
    /**
     * This test ensures that that if the the source entity
     * is also the target entity, the source will be used for
     * the APPC request
     */
    @Test
    public void sourceIsTargetTest() {
        String resourceId = "82194af1-3c2c-485a-8f44-420e22a9eaa4";
        String targetVnfId = null;
        try {
            targetVnfId = AppcLcmActorServiceProvider.vnfNamedQuery(resourceId, "vnf01");
        } catch (AAIException e) {
            logger.warn(e.toString());
            fail("no vnf-id found");
        }
        assertNotNull(targetVnfId);
        assertEquals(targetVnfId, "vnf01");
    }
}
