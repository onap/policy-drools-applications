/*-
 * ============LICENSE_START=======================================================
 * m2/appclcm
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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

package appclcm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;

import java.util.Properties;
import java.util.UUID;

import org.drools.core.WorkingMemory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.onap.policy.appclcm.AppcLcmDmaapWrapper;
import org.onap.policy.appclcm.AppcLcmInput;
import org.onap.policy.appclcm.util.Serialization;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopTargetType;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.controlloop.policy.Target;
import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.guard.PolicyGuardResponse;
import org.onap.policy.m2.appclcm.AppcLcmActor;
import org.onap.policy.m2.appclcm.AppcLcmHealthCheckOperation;
import org.onap.policy.m2.base.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppcLcmHealthCheckOperationTest {
    private static Logger logger = LoggerFactory.getLogger(AppcLcmHealthCheckOperationTest.class);

    public static Policy policy;
    public static VirtualControlLoopEvent event;
    public static Transaction transaction;
    public static AppcLcmHealthCheckOperation operation;

    /**
     * Class-level setup.
     */
    @BeforeClass
    public static void setup() {
        PolicyEngineConstants.getManager().configure(new Properties());
        PolicyEngineConstants.getManager().start();

        policy = new Policy();
        policy.setActor("APPCLCM");
        policy.setTarget(new Target(TargetType.VM));

        event = new VirtualControlLoopEvent();
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event.setRequestId(UUID.randomUUID());
        event.setTarget("vserver.vserver-name");
        event.setTargetType(ControlLoopTargetType.VM);
        event.getAai().put("vserver.is-closed-loop-disabled", "false");
        event.getAai().put("complex.state", "NJ");
        event.getAai().put("vserver.l-interface.interface-name", "89ee9ee6-1e96-4063-b690-aa5ca9f73b32");
        event.getAai().put("vserver.l-interface.l3-interface-ipv4-address-list.l3-inteface-ipv4-address",
            "135.144.3.49");
        event.getAai().put("vserver.l-interface.l3-interface-ipv6-address-list.l3-inteface-ipv6-address", null);
        event.getAai().put("vserver.in-maint", "N");
        event.getAai().put("complex.city", "AAIDefault");
        event.getAai().put("vserver.vserver-id", "aa7a24f9-8791-491f-b31a-c8ba5ad9e2aa");
        event.getAai().put("vserver.l-interface.network-name", "vUSP_DPA3_OAM_3750");
        event.getAai().put("vserver.vserver-name", "ctsf0002vm013");
        event.getAai().put("generic-vnf.vnf-name", "ctsf0002v");
        event.getAai().put("generic-vnf.vnf-id", "0f551f1b-e4e5-4ce2-84da-eda916e06e1c");
        event.getAai().put("generic-vnf.service-id", "e433710f-9217-458d-a79d-1c7aff376d89");
        event.getAai().put("vserver.selflink", "https://compute-aic.dpa3.cci.att.com:8774/v2/d0719b845a804b368f8ac0bba39e188b/servers/aa7a24f9-8791-491f-b31a-c8ba5ad9e2aa");
        event.getAai().put("generic-vnf.vnf-type", "vUSP - vCTS");
        event.getAai().put("tenant.tenant-id", "d0719b845a804b368f8ac0bba39e188b");
        event.getAai().put("cloud-region.identity-url", "https://compute-aic.dpa3.cci.att.com:8774/");
        event.getAai().put("vserver.prov-status", "PROV");
        event.getAai().put("complex.physical-location-id", "LSLEILAA");

        WorkingMemory wm = mock(WorkingMemory.class);
        transaction = new Transaction(wm, "clvusptest", event.getRequestId(), null);

    }

    @AfterClass
    public static void cleanup() {
        transaction.cleanup();
        PolicyEngineConstants.getManager().stop();
    }

    @Test
    public void testGetVnfHealthCheckRequest() throws ControlLoopException {

        policy.setRecipe("HEALTHCHECK");
        policy.getTarget().setType(TargetType.VNF);
        AppcLcmActor actor = new AppcLcmActor();
        operation = (AppcLcmHealthCheckOperation) actor.createOperation(transaction, policy, event, 1);

        Object request = operation.getRequest();
        assertTrue(request instanceof AppcLcmDmaapWrapper);

        AppcLcmDmaapWrapper dmaapRequest = (AppcLcmDmaapWrapper) request;
        assertEquals("request", dmaapRequest.getType());
        assertEquals("health-check", dmaapRequest.getRpcName());
        assertNotNull(dmaapRequest.getBody());

        AppcLcmInput appcRequest = dmaapRequest.getBody().getInput();
        assertNotNull(appcRequest.getCommonHeader());
        assertEquals("2.00", appcRequest.getCommonHeader().getApiVer());
        assertEquals("POLICY", appcRequest.getCommonHeader().getOriginatorId());
        assertNotNull(appcRequest.getAction());
        assertEquals("HealthCheck", appcRequest.getAction());
        assertNotNull(appcRequest.getActionIdentifiers());
        assertEquals(event.getAai().get("generic-vnf.vnf-id"), appcRequest.getActionIdentifiers().get("vnf-id"));
        assertNotNull(appcRequest.getPayload());
        assertTrue(appcRequest.getPayload().contains("host-ip-address"));

        logger.info("health-check request: {}", Serialization.gson.toJson(request, AppcLcmDmaapWrapper.class));

    }

    @Test
    public void testIncomingHealthCheckMessageHealthyState() {
        policy.setRecipe("HEALTHCHECK");
        policy.getTarget().setType(TargetType.VNF);
        operation = new AppcLcmHealthCheckOperation(transaction, policy, event, 1);

        String lcmRespJson = "{\"body\":{\"output\":{\"common-header\":{\"timestamp\":\"2017-08-25T21:06:23.037Z\","
            + "\"api-ver\":\"5.00\",\"originator-id\":\"POLICY\","
            + "\"request-id\":\"664be3d2-6c12-4f4b-a3e7-c349acced200\",\"sub-request-id\":\"1\",\"flags\":{}},"
            + "\"status\":{\"code\":400,\"message\":\"HealthCheckSuccessful\"},"
            + "\"payload\":\"{\\\"identifier\\\":\\\"scoperepresented\\\",\\\"state\\\":\\\"healthy\\\","
            + "\\\"time\\\":\\\"01-01-1000:0000\\\"}\"}},\"version\":\"2.0\",\"rpc-name\":\"health-check\","
            + "\"correlation-id\":\"664be3d2-6c12-4f4b-a3e7-c349acced200-1\",\"type\":\"response\"}";
        AppcLcmDmaapWrapper healthCheckResp = Serialization.gson.fromJson(lcmRespJson, AppcLcmDmaapWrapper.class);

        operation.incomingMessage(healthCheckResp);
        assertEquals(PolicyResult.SUCCESS, operation.getResult());
    }

    @Test
    public void testIncomingHealthCheckMessageUnhealthyState() {
        policy.setRecipe("HEALTHCHECK");
        policy.getTarget().setType(TargetType.VNF);
        operation = new AppcLcmHealthCheckOperation(transaction, policy, event, 1);

        String lcmRespJson = "{\"body\":{\"output\":{\"common-header\":{\"timestamp\":\"2017-08-25T21:06:23.037Z\","
            + "\"api-ver\":\"5.00\",\"originator-id\":\"POLICY\","
            + "\"request-id\":\"664be3d2-6c12-4f4b-a3e7-c349acced200\",\"sub-request-id\":\"1\",\"flags\":{}},"
            + "\"status\":{\"code\":400,\"message\":\"VNF is unhealthy\"},"
            + "\"payload\":\"{\\\"identifier\\\":\\\"scoperepresented\\\",\\\"state\\\":\\\"unhealthy\\\","
            + "\\\"info\\\":\\\"Systemthresholdexceededdetails\\\",\\\"fault\\\":{\\\"cpuOverall\\\":0.80,"
            + "\\\"cpuThreshold\\\":0.45},\\\"time\\\":\\\"01-01-1000:0000\\\"}\"}},\"version\":\"2.0\","
            + "\"rpc-name\":\"health-check\",\"correlation-id\":\"664be3d2-6c12-4f4b-a3e7-c349acced200-1\","
            + "\"type\":\"response\"}";
        AppcLcmDmaapWrapper healthCheckResp = Serialization.gson.fromJson(lcmRespJson, AppcLcmDmaapWrapper.class);

        operation.incomingMessage(healthCheckResp);
        assertEquals(PolicyResult.FAILURE, operation.getResult());
    }

    @Test
    public void testIncomingHealthCheckMessageUnknownState() {
        policy.setRecipe("HEALTHCHECK");
        policy.getTarget().setType(TargetType.VNF);
        operation = new AppcLcmHealthCheckOperation(transaction, policy, event, 1);

        String lcmRespJson = "{\"body\":{\"output\":{\"common-header\":{\"timestamp\":\"2017-08-25T21:06:23.037Z\","
            + "\"api-ver\":\"5.00\",\"originator-id\":\"POLICY\","
            + "\"request-id\":\"664be3d2-6c12-4f4b-a3e7-c349acced200\",\"sub-request-id\":\"1\",\"flags\":{}},"
            + "\"status\":{\"code\":400,\"message\":\"VNF is unhealthy\"},"
            + "\"payload\":\"{\\\"identifier\\\":\\\"scoperepresented\\\",\\\"state\\\":\\\"unknown\\\","
            + "\\\"info\\\":\\\"Systemthresholdexceededdetails\\\",\\\"fault\\\":{\\\"cpuOverall\\\":0.80,"
            + "\\\"cpuThreshold\\\":0.45},\\\"time\\\":\\\"01-01-1000:0000\\\"}\"}},\"version\":\"2.0\","
            + "\"rpc-name\":\"health-check\",\"correlation-id\":\"664be3d2-6c12-4f4b-a3e7-c349acced200-1\","
            + "\"type\":\"response\"}";
        AppcLcmDmaapWrapper healthCheckResp = Serialization.gson.fromJson(lcmRespJson, AppcLcmDmaapWrapper.class);

        operation.incomingMessage(healthCheckResp);
        assertEquals(PolicyResult.FAILURE_EXCEPTION, operation.getResult());
    }

    @Test
    public void testIncomingHealthCheckMessageNoState() {
        policy.setRecipe("HEALTHCHECK");
        policy.getTarget().setType(TargetType.VNF);
        operation = new AppcLcmHealthCheckOperation(transaction, policy, event, 1);

        String lcmRespJson = "{\"body\":{\"output\":{\"common-header\":{\"timestamp\":\"2017-08-25T21:06:23.037Z\","
            + "\"api-ver\":\"5.00\",\"originator-id\":\"POLICY\","
            + "\"request-id\":\"664be3d2-6c12-4f4b-a3e7-c349acced200\",\"sub-request-id\":\"1\",\"flags\":{}},"
            + "\"status\":{\"code\":400,\"message\":\"VNF is unhealthy\"},"
            + "\"payload\":\"{\\\"identifier\\\":\\\"scoperepresented\\\","
            + "\\\"info\\\":\\\"Systemthresholdexceededdetails\\\",\\\"fault\\\":{\\\"cpuOverall\\\":0.80,"
            + "\\\"cpuThreshold\\\":0.45},\\\"time\\\":\\\"01-01-1000:0000\\\"}\"}},\"version\":\"2.0\","
            + "\"rpc-name\":\"health-check\",\"correlation-id\":\"664be3d2-6c12-4f4b-a3e7-c349acced200-1\","
            + "\"type\":\"response\"}";
        AppcLcmDmaapWrapper healthCheckResp = Serialization.gson.fromJson(lcmRespJson, AppcLcmDmaapWrapper.class);

        operation.incomingMessage(healthCheckResp);
        assertEquals(PolicyResult.FAILURE_EXCEPTION, operation.getResult());
    }

    @Test
    public void testIncomingHealthCheckMessageUnsuccessful() {
        policy.setRecipe("HEALTHCHECK");
        policy.getTarget().setType(TargetType.VNF);
        operation = new AppcLcmHealthCheckOperation(transaction, policy, event, 1);

        String lcmRespJson = "{\"body\":{\"output\":{\"common-header\":{\"timestamp\":\"2017-08-25T21:06:23.037Z\","
            + "\"api-ver\":\"5.00\",\"originator-id\":\"POLICY\","
            + "\"request-id\":\"664be3d2-6c12-4f4b-a3e7-c349acced200\",\"sub-request-id\":\"1\",\"flags\":{}},"
            + "\"status\":{\"code\":401,\"message\":\"Could not complete HealthCheck\"},"
            + "\"payload\":\"{\\\"identifier\\\":\\\"scoperepresented\\\","
            + "\\\"info\\\":\\\"Systemthresholdexceededdetails\\\",\\\"fault\\\":{\\\"cpuOverall\\\":0.80,"
            + "\\\"cpuThreshold\\\":0.45},\\\"time\\\":\\\"01-01-1000:0000\\\"}\"}},\"version\":\"2.0\","
            + "\"rpc-name\":\"health-check\",\"correlation-id\":\"664be3d2-6c12-4f4b-a3e7-c349acced200-1\","
            + "\"type\":\"response\"}";
        AppcLcmDmaapWrapper healthCheckResp = Serialization.gson.fromJson(lcmRespJson, AppcLcmDmaapWrapper.class);

        operation.incomingMessage(healthCheckResp);
        assertEquals(PolicyResult.FAILURE, operation.getResult());
    }

    @Test
    public void testIncomingHealthCheckMessageNoPayload() {
        policy.setRecipe("HEALTHCHECK");
        policy.getTarget().setType(TargetType.VNF);
        operation = new AppcLcmHealthCheckOperation(transaction, policy, event, 1);

        String lcmRespJson = "{\"body\":{\"output\":{\"common-header\":{\"timestamp\":\"2017-08-25T21:06:23.037Z\","
            + "\"api-ver\":\"5.00\",\"originator-id\":\"POLICY\","
            + "\"request-id\":\"664be3d2-6c12-4f4b-a3e7-c349acced200\",\"sub-request-id\":\"1\",\"flags\":{}},"
            + "\"status\":{\"code\":400,\"message\":\"VNF is unhealthy\"}}},\"version\":\"2.0\","
            + "\"rpc-name\":\"health-check\",\"correlation-id\":\"664be3d2-6c12-4f4b-a3e7-c349acced200-1\","
            + "\"type\":\"response\"}";
        AppcLcmDmaapWrapper healthCheckResp = Serialization.gson.fromJson(lcmRespJson, AppcLcmDmaapWrapper.class);

        operation.incomingMessage(healthCheckResp);
        assertEquals(PolicyResult.FAILURE_EXCEPTION, operation.getResult());
    }

    @Test
    public void testIncomingHealthCheckMessageEmptyPayload() {
        policy.setRecipe("HEALTHCHECK");
        policy.getTarget().setType(TargetType.VNF);
        operation = new AppcLcmHealthCheckOperation(transaction, policy, event, 1);

        String lcmRespJson = "{\"body\":{\"output\":{\"common-header\":{\"timestamp\":\"2017-08-25T21:06:23.037Z\","
            + "\"api-ver\":\"5.00\",\"originator-id\":\"POLICY\","
            + "\"request-id\":\"664be3d2-6c12-4f4b-a3e7-c349acced200\",\"sub-request-id\":\"1\",\"flags\":{}},"
            + "\"status\":{\"code\":400,\"message\":\"VNF is unhealthy\"},\"payload\":\"\"}},\"version\":\"2.0\","
            + "\"rpc-name\":\"health-check\",\"correlation-id\":\"664be3d2-6c12-4f4b-a3e7-c349acced200-1\","
            + "\"type\":\"response\"}";
        AppcLcmDmaapWrapper healthCheckResp = Serialization.gson.fromJson(lcmRespJson, AppcLcmDmaapWrapper.class);

        operation.incomingMessage(healthCheckResp);
        assertEquals(PolicyResult.FAILURE_EXCEPTION, operation.getResult());
    }

    @Test
    public void testIncomingMessage() {
        policy.setRecipe("HEALTHCHECK");
        policy.getTarget().setType(TargetType.VNF);
        operation = new AppcLcmHealthCheckOperation(transaction, policy, event, 1);

        //Submitting Policy Guard Response instead of AppcLcmDmaapWrapper
        PolicyGuardResponse response = new PolicyGuardResponse("", UUID.randomUUID(), "");
        operation.incomingMessage(response);

        //Checking for Failure Code
        String lcmRespJson = "{\"body\":{\"output\":{\"common-header\":{\"timestamp\":\"2017-08-25T21:06:23.037Z\","
                + "\"api-ver\":\"5.00\",\"originator-id\":\"POLICY\","
                + "\"request-id\":\"664be3d2-6c12-4f4b-a3e7-c349acced200\",\"sub-request-id\":\"1\",\"flags\":{}},"
                + "\"status\":{\"code\":404,\"message\":\"VNF is unhealthy\"},\"payload\":\"\"}},\"version\":\"2.0\","
                + "\"rpc-name\":\"health-check\",\"correlation-id\":\"664be3d2-6c12-4f4b-a3e7-c349acced200-1\","
                + "\"type\":\"response\"}";
        AppcLcmDmaapWrapper healthCheckResp = Serialization.gson.fromJson(lcmRespJson, AppcLcmDmaapWrapper.class);

        operation = new AppcLcmHealthCheckOperation(transaction, policy, event, 1);
        operation.incomingMessage(healthCheckResp);
        assertEquals(PolicyResult.FAILURE, operation.getResult());

        //Checking code 300 Failure_Exception
        lcmRespJson = "{\"body\":{\"output\":{\"common-header\":{\"timestamp\":\"2017-08-25T21:06:23.037Z\","
                + "\"api-ver\":\"5.00\",\"originator-id\":\"POLICY\","
                + "\"request-id\":\"664be3d2-6c12-4f4b-a3e7-c349acced200\",\"sub-request-id\":\"1\",\"flags\":{}},"
                + "\"status\":{\"code\":300,\"message\":\"VNF is unhealthy\"},\"payload\":\"\"}},\"version\":\"2.0\","
                + "\"rpc-name\":\"health-check\",\"correlation-id\":\"664be3d2-6c12-4f4b-a3e7-c349acced200-1\","
                + "\"type\":\"response\"}";
        healthCheckResp = Serialization.gson.fromJson(lcmRespJson, AppcLcmDmaapWrapper.class);

        operation = new AppcLcmHealthCheckOperation(transaction, policy, event, 1);
        operation.incomingMessage(healthCheckResp);
        assertEquals(PolicyResult.FAILURE_EXCEPTION, operation.getResult());

        //Checking code 100 accepted does nothing to result
        //Leaving the operation result as the initialized value of null
        lcmRespJson = "{\"body\":{\"output\":{\"common-header\":{\"timestamp\":\"2017-08-25T21:06:23.037Z\","
                + "\"api-ver\":\"5.00\",\"originator-id\":\"POLICY\","
                + "\"request-id\":\"664be3d2-6c12-4f4b-a3e7-c349acced200\",\"sub-request-id\":\"1\",\"flags\":{}},"
                + "\"status\":{\"code\":100,\"message\":\"VNF is unhealthy\"},\"payload\":\"\"}},\"version\":\"2.0\","
                + "\"rpc-name\":\"health-check\",\"correlation-id\":\"664be3d2-6c12-4f4b-a3e7-c349acced200-1\","
                + "\"type\":\"response\"}";
        healthCheckResp = Serialization.gson.fromJson(lcmRespJson, AppcLcmDmaapWrapper.class);

        operation = new AppcLcmHealthCheckOperation(transaction, policy, event, 1);
        operation.incomingMessage(healthCheckResp);
        assertEquals(null, operation.getResult());
    }
}
