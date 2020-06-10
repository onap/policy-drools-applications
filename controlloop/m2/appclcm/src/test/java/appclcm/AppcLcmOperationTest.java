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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
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
import org.onap.policy.m2.appclcm.AppcLcmOperation;
import org.onap.policy.m2.base.Transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppcLcmOperationTest {
    private static Logger logger = LoggerFactory.getLogger(AppcLcmOperationTest.class);

    public static Policy policy;
    public static VirtualControlLoopEvent event;
    public static Transaction transaction;
    public static AppcLcmOperation operation;

    /**
     * Class-level setup.
     */
    @BeforeClass
    public static void start() {
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
    public void testGetVmRestartRequest() throws ControlLoopException {

        policy.setRecipe("RESTART");
        policy.getTarget().setType(TargetType.VM);
        operation = new AppcLcmOperation(transaction, policy, event, 1);

        Object request = operation.getRequest();
        assertTrue(request instanceof AppcLcmDmaapWrapper);

        AppcLcmDmaapWrapper dmaapRequest = (AppcLcmDmaapWrapper) request;
        assertEquals("request", dmaapRequest.getType());
        assertEquals("restart", dmaapRequest.getRpcName());
        assertNotNull(dmaapRequest.getBody());

        AppcLcmInput appcRequest = dmaapRequest.getBody().getInput();
        assertNotNull(appcRequest.getCommonHeader());
        assertEquals("2.00", appcRequest.getCommonHeader().getApiVer());
        assertEquals("POLICY", appcRequest.getCommonHeader().getOriginatorId());
        assertNotNull(appcRequest.getAction());
        assertEquals("Restart", appcRequest.getAction());
        assertNotNull(appcRequest.getActionIdentifiers());
        assertEquals(event.getAai().get("generic-vnf.vnf-id"), appcRequest.getActionIdentifiers().get("vnf-id"));
        assertEquals(event.getAai().get("vserver.vserver-id"), appcRequest.getActionIdentifiers().get("vserver-id"));
        assertNotNull(appcRequest.getPayload());

        logger.info("vm restart request: {}", Serialization.gson.toJson(request, AppcLcmDmaapWrapper.class));

    }

    @Test
    public void testGetVnfRestartRequest() throws ControlLoopException {

        policy.setRecipe("RESTART");
        policy.getTarget().setType(TargetType.VNF);
        operation = new AppcLcmOperation(transaction, policy, event, 1);

        Object request = operation.getRequest();
        assertTrue(request instanceof AppcLcmDmaapWrapper);

        AppcLcmDmaapWrapper dmaapRequest = (AppcLcmDmaapWrapper) request;
        assertEquals("request", dmaapRequest.getType());
        assertEquals("restart", dmaapRequest.getRpcName());
        assertNotNull(dmaapRequest.getBody());

        AppcLcmInput appcRequest = dmaapRequest.getBody().getInput();
        assertNotNull(appcRequest.getCommonHeader());
        assertEquals("2.00", appcRequest.getCommonHeader().getApiVer());
        assertEquals("POLICY", appcRequest.getCommonHeader().getOriginatorId());
        assertNotNull(appcRequest.getAction());
        assertEquals("Restart", appcRequest.getAction());
        assertNotNull(appcRequest.getActionIdentifiers());
        assertEquals(event.getAai().get("generic-vnf.vnf-id"), appcRequest.getActionIdentifiers().get("vnf-id"));
        assertNull(appcRequest.getActionIdentifiers().get("vserver-id"));
        assertNull(appcRequest.getPayload());

        logger.info("vnf restart request: {}", Serialization.gson.toJson(request, AppcLcmDmaapWrapper.class));

    }

    @Test
    public void testGetVmRebuildRequest() throws ControlLoopException {

        policy.setRecipe("REBUILD");
        policy.getTarget().setType(TargetType.VM);
        operation = new AppcLcmOperation(transaction, policy, event, 1);

        Object request = operation.getRequest();
        assertTrue(request instanceof AppcLcmDmaapWrapper);

        AppcLcmDmaapWrapper dmaapRequest = (AppcLcmDmaapWrapper) request;
        assertEquals("request", dmaapRequest.getType());
        assertEquals("rebuild", dmaapRequest.getRpcName());
        assertNotNull(dmaapRequest.getBody());

        AppcLcmInput appcRequest = dmaapRequest.getBody().getInput();
        assertNotNull(appcRequest.getCommonHeader());
        assertEquals("2.00", appcRequest.getCommonHeader().getApiVer());
        assertEquals("POLICY", appcRequest.getCommonHeader().getOriginatorId());
        assertNotNull(appcRequest.getAction());
        assertEquals("Rebuild", appcRequest.getAction());
        assertNotNull(appcRequest.getActionIdentifiers());
        assertEquals(event.getAai().get("generic-vnf.vnf-id"), appcRequest.getActionIdentifiers().get("vnf-id"));
        assertEquals(event.getAai().get("vserver.vserver-id"), appcRequest.getActionIdentifiers().get("vserver-id"));
        assertNotNull(appcRequest.getPayload());

        logger.info("vm rebuild request: {}", Serialization.gson.toJson(request, AppcLcmDmaapWrapper.class));

    }

    @Test
    public void testGetVnfRebuildRequest() throws ControlLoopException {

        policy.setRecipe("REBUILD");
        policy.getTarget().setType(TargetType.VNF);
        operation = new AppcLcmOperation(transaction, policy, event, 1);

        Object request = operation.getRequest();
        assertTrue(request instanceof AppcLcmDmaapWrapper);

        AppcLcmDmaapWrapper dmaapRequest = (AppcLcmDmaapWrapper) request;
        assertEquals("request", dmaapRequest.getType());
        assertEquals("rebuild", dmaapRequest.getRpcName());
        assertNotNull(dmaapRequest.getBody());

        AppcLcmInput appcRequest = dmaapRequest.getBody().getInput();
        assertNotNull(appcRequest.getCommonHeader());
        assertEquals("2.00", appcRequest.getCommonHeader().getApiVer());
        assertEquals("POLICY", appcRequest.getCommonHeader().getOriginatorId());
        assertNotNull(appcRequest.getAction());
        assertEquals("Rebuild", appcRequest.getAction());
        assertNotNull(appcRequest.getActionIdentifiers());
        assertEquals(event.getAai().get("generic-vnf.vnf-id"), appcRequest.getActionIdentifiers().get("vnf-id"));
        assertNull(appcRequest.getActionIdentifiers().get("vserver-id"));
        assertNull(appcRequest.getPayload());

        logger.info("vnf rebuild request: {}", Serialization.gson.toJson(request, AppcLcmDmaapWrapper.class));

    }

    @Test
    public void testGetVmMigrateRequest() throws ControlLoopException {

        policy.setRecipe("MIGRATE");
        policy.getTarget().setType(TargetType.VM);
        operation = new AppcLcmOperation(transaction, policy, event, 1);

        Object request = operation.getRequest();
        assertTrue(request instanceof AppcLcmDmaapWrapper);

        AppcLcmDmaapWrapper dmaapRequest = (AppcLcmDmaapWrapper) request;
        assertEquals("request", dmaapRequest.getType());
        assertEquals("migrate", dmaapRequest.getRpcName());
        assertNotNull(dmaapRequest.getBody());

        AppcLcmInput appcRequest = dmaapRequest.getBody().getInput();
        assertNotNull(appcRequest.getCommonHeader());
        assertEquals("2.00", appcRequest.getCommonHeader().getApiVer());
        assertEquals("POLICY", appcRequest.getCommonHeader().getOriginatorId());
        assertNotNull(appcRequest.getAction());
        assertEquals("Migrate", appcRequest.getAction());
        assertNotNull(appcRequest.getActionIdentifiers());
        assertEquals(event.getAai().get("generic-vnf.vnf-id"), appcRequest.getActionIdentifiers().get("vnf-id"));
        assertEquals(event.getAai().get("vserver.vserver-id"), appcRequest.getActionIdentifiers().get("vserver-id"));
        assertNotNull(appcRequest.getPayload());

        logger.info("vm migrate request: {}", Serialization.gson.toJson(request, AppcLcmDmaapWrapper.class));

    }

    @Test
    public void testGetVnfMigrateRequest() throws ControlLoopException {

        policy.setRecipe("MIGRATE");
        policy.getTarget().setType(TargetType.VNF);
        operation = new AppcLcmOperation(transaction, policy, event, 1);

        Object request = operation.getRequest();
        assertTrue(request instanceof AppcLcmDmaapWrapper);

        AppcLcmDmaapWrapper dmaapRequest = (AppcLcmDmaapWrapper) request;
        assertEquals("request", dmaapRequest.getType());
        assertEquals("migrate", dmaapRequest.getRpcName());
        assertNotNull(dmaapRequest.getBody());

        AppcLcmInput appcRequest = dmaapRequest.getBody().getInput();
        assertNotNull(appcRequest.getCommonHeader());
        assertEquals("2.00", appcRequest.getCommonHeader().getApiVer());
        assertEquals("POLICY", appcRequest.getCommonHeader().getOriginatorId());
        assertNotNull(appcRequest.getAction());
        assertEquals("Migrate", appcRequest.getAction());
        assertNotNull(appcRequest.getActionIdentifiers());
        assertEquals(event.getAai().get("generic-vnf.vnf-id"), appcRequest.getActionIdentifiers().get("vnf-id"));
        assertNull(appcRequest.getActionIdentifiers().get("vserver-id"));
        assertNull(appcRequest.getPayload());

        logger.info("vnf migrate request: {}", Serialization.gson.toJson(request, AppcLcmDmaapWrapper.class));

    }

    @Test
    public void testGetVmEvacuateRequest() throws ControlLoopException {

        policy.setRecipe("EVACUATE");
        policy.getTarget().setType(TargetType.VM);
        operation = new AppcLcmOperation(transaction, policy, event, 1);

        Object request = operation.getRequest();
        assertTrue(request instanceof AppcLcmDmaapWrapper);

        AppcLcmDmaapWrapper dmaapRequest = (AppcLcmDmaapWrapper) request;
        assertEquals("request", dmaapRequest.getType());
        assertEquals("evacuate", dmaapRequest.getRpcName());
        assertNotNull(dmaapRequest.getBody());

        AppcLcmInput appcRequest = dmaapRequest.getBody().getInput();
        assertNotNull(appcRequest.getCommonHeader());
        assertEquals("2.00", appcRequest.getCommonHeader().getApiVer());
        assertEquals("POLICY", appcRequest.getCommonHeader().getOriginatorId());
        assertNotNull(appcRequest.getAction());
        assertEquals("Evacuate", appcRequest.getAction());
        assertNotNull(appcRequest.getActionIdentifiers());
        assertEquals(event.getAai().get("generic-vnf.vnf-id"), appcRequest.getActionIdentifiers().get("vnf-id"));
        assertEquals(event.getAai().get("vserver.vserver-id"), appcRequest.getActionIdentifiers().get("vserver-id"));
        assertNotNull(appcRequest.getPayload());

        logger.info("vm evacuate request: {}", Serialization.gson.toJson(request, AppcLcmDmaapWrapper.class));

    }

    @Test
    public void testGetVnfEvacuateRequest() throws ControlLoopException {

        policy.setRecipe("EVACUATE");
        policy.getTarget().setType(TargetType.VNF);
        operation = new AppcLcmOperation(transaction, policy, event, 1);

        Object request = operation.getRequest();
        assertTrue(request instanceof AppcLcmDmaapWrapper);

        AppcLcmDmaapWrapper dmaapRequest = (AppcLcmDmaapWrapper) request;
        assertEquals("request", dmaapRequest.getType());
        assertEquals("evacuate", dmaapRequest.getRpcName());
        assertNotNull(dmaapRequest.getBody());

        AppcLcmInput appcRequest = dmaapRequest.getBody().getInput();
        assertNotNull(appcRequest.getCommonHeader());
        assertEquals("2.00", appcRequest.getCommonHeader().getApiVer());
        assertEquals("POLICY", appcRequest.getCommonHeader().getOriginatorId());
        assertNotNull(appcRequest.getAction());
        assertEquals("Evacuate", appcRequest.getAction());
        assertNotNull(appcRequest.getActionIdentifiers());
        assertEquals(event.getAai().get("generic-vnf.vnf-id"), appcRequest.getActionIdentifiers().get("vnf-id"));
        assertNull(appcRequest.getActionIdentifiers().get("vserver-id"));
        assertNull(appcRequest.getPayload());

        logger.info("vnf evacuate request: {}", Serialization.gson.toJson(request, AppcLcmDmaapWrapper.class));

    }

    @Test
    public void testGetVmRebootRequest() throws ControlLoopException {

        policy.setRecipe("REBOOT");
        policy.getTarget().setType(TargetType.VM);
        policy.setPayload(new HashMap<String, String>());
        policy.getPayload().put("type", "HARD");
        operation = new AppcLcmOperation(transaction, policy, event, 1);

        Object request = operation.getRequest();
        assertTrue(request instanceof AppcLcmDmaapWrapper);

        AppcLcmDmaapWrapper dmaapRequest = (AppcLcmDmaapWrapper) request;
        assertEquals("request", dmaapRequest.getType());
        assertEquals("reboot", dmaapRequest.getRpcName());
        assertNotNull(dmaapRequest.getBody());

        AppcLcmInput appcRequest = dmaapRequest.getBody().getInput();
        assertNotNull(appcRequest.getCommonHeader());
        assertEquals("2.00", appcRequest.getCommonHeader().getApiVer());
        assertEquals("POLICY", appcRequest.getCommonHeader().getOriginatorId());
        assertNotNull(appcRequest.getAction());
        assertEquals("Reboot", appcRequest.getAction());
        assertNotNull(appcRequest.getActionIdentifiers());
        assertEquals(event.getAai().get("generic-vnf.vnf-id"), appcRequest.getActionIdentifiers().get("vnf-id"));
        assertEquals(event.getAai().get("vserver.vserver-id"), appcRequest.getActionIdentifiers().get("vserver-id"));
        assertNotNull(appcRequest.getPayload());

        logger.info("vm reboot request: {}", Serialization.gson.toJson(request, AppcLcmDmaapWrapper.class));

    }

    @Test
    public void testGetVnfRebootRequest() throws ControlLoopException {

        policy.setRecipe("REBOOT");
        policy.getTarget().setType(TargetType.VNF);
        policy.setPayload(new HashMap<String, String>());
        policy.getPayload().put("type", "HARD");
        operation = new AppcLcmOperation(transaction, policy, event, 1);

        Object request = operation.getRequest();
        assertTrue(request instanceof AppcLcmDmaapWrapper);

        AppcLcmDmaapWrapper dmaapRequest = (AppcLcmDmaapWrapper) request;
        assertEquals("request", dmaapRequest.getType());
        assertEquals("reboot", dmaapRequest.getRpcName());
        assertNotNull(dmaapRequest.getBody());

        AppcLcmInput appcRequest = dmaapRequest.getBody().getInput();
        assertNotNull(appcRequest.getCommonHeader());
        assertEquals("2.00", appcRequest.getCommonHeader().getApiVer());
        assertEquals("POLICY", appcRequest.getCommonHeader().getOriginatorId());
        assertNotNull(appcRequest.getAction());
        assertEquals("Reboot", appcRequest.getAction());
        assertNotNull(appcRequest.getActionIdentifiers());
        assertEquals(event.getAai().get("generic-vnf.vnf-id"), appcRequest.getActionIdentifiers().get("vnf-id"));
        assertNull(appcRequest.getActionIdentifiers().get("vserver-id"));
        assertNotNull(appcRequest.getPayload());

        logger.info("vnf reboot request: {}", Serialization.gson.toJson(request, AppcLcmDmaapWrapper.class));

    }

    @Test
    public void testGetVnfStartRequest() throws ControlLoopException {

        policy.setRecipe("START");
        policy.getTarget().setType(TargetType.VNF);
        operation = new AppcLcmOperation(transaction, policy, event, 1);

        Object request = operation.getRequest();
        assertTrue(request instanceof AppcLcmDmaapWrapper);

        AppcLcmDmaapWrapper dmaapRequest = (AppcLcmDmaapWrapper) request;
        assertEquals("request", dmaapRequest.getType());
        assertEquals("start", dmaapRequest.getRpcName());
        assertNotNull(dmaapRequest.getBody());

        AppcLcmInput appcRequest = dmaapRequest.getBody().getInput();
        assertNotNull(appcRequest.getCommonHeader());
        assertEquals("2.00", appcRequest.getCommonHeader().getApiVer());
        assertEquals("POLICY", appcRequest.getCommonHeader().getOriginatorId());
        assertNotNull(appcRequest.getAction());
        assertEquals("Start", appcRequest.getAction());
        assertNotNull(appcRequest.getActionIdentifiers());
        assertEquals(event.getAai().get("generic-vnf.vnf-id"), appcRequest.getActionIdentifiers().get("vnf-id"));
        assertEquals(appcRequest.getActionIdentifiers().get("vserver-id"), null);
        assertNull(appcRequest.getPayload());

        logger.info("vnf start request: {}", Serialization.gson.toJson(request, AppcLcmDmaapWrapper.class));

    }

    @Test
    public void testGetVmStartRequest() throws ControlLoopException {

        policy.setRecipe("START");
        policy.getTarget().setType(TargetType.VM);
        operation = new AppcLcmOperation(transaction, policy, event, 1);

        Object request = operation.getRequest();
        assertTrue(request instanceof AppcLcmDmaapWrapper);

        AppcLcmDmaapWrapper dmaapRequest = (AppcLcmDmaapWrapper) request;
        assertEquals("request", dmaapRequest.getType());
        assertEquals("start", dmaapRequest.getRpcName());
        assertNotNull(dmaapRequest.getBody());

        AppcLcmInput appcRequest = dmaapRequest.getBody().getInput();
        assertNotNull(appcRequest.getCommonHeader());
        assertEquals("2.00", appcRequest.getCommonHeader().getApiVer());
        assertEquals("POLICY", appcRequest.getCommonHeader().getOriginatorId());
        assertNotNull(appcRequest.getAction());
        assertEquals("Start", appcRequest.getAction());
        assertNotNull(appcRequest.getActionIdentifiers());
        assertEquals(event.getAai().get("generic-vnf.vnf-id"), appcRequest.getActionIdentifiers().get("vnf-id"));
        assertEquals(event.getAai().get("vserver.vserver-id"), appcRequest.getActionIdentifiers().get("vserver-id"));
        assertNotNull(appcRequest.getPayload());

        logger.info("vm start request: {}", Serialization.gson.toJson(request, AppcLcmDmaapWrapper.class));

    }

    @Test
    public void testGetVnfStopRequest() throws ControlLoopException {

        policy.setRecipe("STOP");
        policy.getTarget().setType(TargetType.VNF);
        operation = new AppcLcmOperation(transaction, policy, event, 1);

        Object request = operation.getRequest();
        assertTrue(request instanceof AppcLcmDmaapWrapper);

        AppcLcmDmaapWrapper dmaapRequest = (AppcLcmDmaapWrapper) request;
        assertEquals("request", dmaapRequest.getType());
        assertEquals("stop", dmaapRequest.getRpcName());
        assertNotNull(dmaapRequest.getBody());

        AppcLcmInput appcRequest = dmaapRequest.getBody().getInput();
        assertNotNull(appcRequest.getCommonHeader());
        assertEquals("2.00", appcRequest.getCommonHeader().getApiVer());
        assertEquals("POLICY", appcRequest.getCommonHeader().getOriginatorId());
        assertNotNull(appcRequest.getAction());
        assertEquals("Stop", appcRequest.getAction());
        assertNotNull(appcRequest.getActionIdentifiers());
        assertEquals(event.getAai().get("generic-vnf.vnf-id"), appcRequest.getActionIdentifiers().get("vnf-id"));
        assertEquals(appcRequest.getActionIdentifiers().get("vserver-id"), null);
        assertNull(appcRequest.getPayload());

        logger.info("vnf stop request: {}", Serialization.gson.toJson(request, AppcLcmDmaapWrapper.class));

    }

    @Test
    public void testGetVmStopRequest() throws ControlLoopException {

        policy.setRecipe("STOP");
        policy.getTarget().setType(TargetType.VM);
        operation = new AppcLcmOperation(transaction, policy, event, 1);

        Object request = operation.getRequest();
        assertTrue(request instanceof AppcLcmDmaapWrapper);

        AppcLcmDmaapWrapper dmaapRequest = (AppcLcmDmaapWrapper) request;
        assertEquals("request", dmaapRequest.getType());

        assertEquals("stop", dmaapRequest.getRpcName());
        assertNotNull(dmaapRequest.getBody());

        AppcLcmInput appcRequest = dmaapRequest.getBody().getInput();
        assertNotNull(appcRequest.getCommonHeader());
        assertEquals("2.00", appcRequest.getCommonHeader().getApiVer());
        assertEquals("POLICY", appcRequest.getCommonHeader().getOriginatorId());
        assertNotNull(appcRequest.getAction());
        assertEquals("Stop", appcRequest.getAction());
        assertNotNull(appcRequest.getActionIdentifiers());
        assertEquals(event.getAai().get("generic-vnf.vnf-id"), appcRequest.getActionIdentifiers().get("vnf-id"));
        assertEquals(event.getAai().get("vserver.vserver-id"), appcRequest.getActionIdentifiers().get("vserver-id"));
        assertNotNull(appcRequest.getPayload());

        logger.info("vm stop request: {}", Serialization.gson.toJson(request, AppcLcmDmaapWrapper.class));

    }

    /* ===================================================================== */

    /*
     * these tests are for ensuring the incoming response messages process
     * properly and translate to the expected policy result
     */

    @Test
    public void testIncomingVmSuccessMessage() {
        policy.setRecipe("RESTART");
        policy.getTarget().setType(TargetType.VM);
        operation = new AppcLcmOperation(transaction, policy, event, 1);

        String lcmRespJson = "{\"version\": \"2.0\",\"rpc-name\": \"Restart\",\"correlation-id\": \"baf5ba32-6b8c-430c-a91b-02d2c0ba3064-1\",\"type\": \"response\",\"body\": {\"output\": {\"status\": {\"code\": 400,\"message\": \"Restart Successful\"},\"common-header\": {\"timestamp\": \"2017-07-18T16:52:06.186Z\",\"api-ver\": \"2.01\",\"request-id\": \"baf5ba32-6b8c-430c-a91b-02d2c0ba3064\",\"sub-request-id\": \"1\",\"flags\": {\"ttl\": 600}},\"payload\": \"{\\\"vm-id\\\":\\\"http://135.25.246.131:8774/v2/81fc2bc61f974de1b5a49e8c2ec090bb/servers/75dce20c-97f9-454d-abcc-aa904a33df5a\\\",\\\"tenant-id\\\":\\\"test2\\\"}\"}}}";
        AppcLcmDmaapWrapper restartResponse = Serialization.gson.fromJson(lcmRespJson, AppcLcmDmaapWrapper.class);

        operation.incomingMessage(restartResponse);
        assertEquals(operation.getResult(), PolicyResult.SUCCESS);
    }

    @Test
    public void testIncomingVnfSuccessMessage() {
        policy.setRecipe("RESTART");
        policy.getTarget().setType(TargetType.VNF);
        operation = new AppcLcmOperation(transaction, policy, event, 1);

        String lcmRespJson = "{\"version\": \"2.0\",\"rpc-name\": \"Restart\",\"correlation-id\": \"baf5ba32-6b8c-430c-a91b-02d2c0ba3064-1\",\"type\": \"response\",\"body\": {\"output\": {\"status\": {\"code\": 500,\"message\": \"Restart Successful\"},\"common-header\": {\"timestamp\": \"2017-07-18T16:52:06.186Z\",\"api-ver\": \"2.01\",\"request-id\": \"baf5ba32-6b8c-430c-a91b-02d2c0ba3064\",\"sub-request-id\": \"1\",\"flags\": {\"ttl\": 600}},\"payload\": \"{\\\"vm-id\\\":\\\"http://135.25.246.131:8774/v2/81fc2bc61f974de1b5a49e8c2ec090bb/servers/75dce20c-97f9-454d-abcc-aa904a33df5a\\\",\\\"tenant-id\\\":\\\"test2\\\"}\"}}}";
        AppcLcmDmaapWrapper restartResponse = Serialization.gson.fromJson(lcmRespJson, AppcLcmDmaapWrapper.class);

        /* Send in several partial success messages */
        for (int i = 0; i < 5; i++) {
            operation.incomingMessage(restartResponse);
            assertEquals(operation.getResult(), null);
        }

        /* Send in an operation success */
        restartResponse.getBody().getOutput().getStatus().setCode(400);
        operation.incomingMessage(restartResponse);
        assertEquals(operation.getResult(), PolicyResult.SUCCESS);
    }

    @Test
    public void testIncomingVmFailureMessage() {
        policy.setRecipe("RESTART");
        policy.getTarget().setType(TargetType.VM);
        operation = new AppcLcmOperation(transaction, policy, event, 1);

        String lcmRespJson = "{\"version\": \"2.0\",\"rpc-name\": \"Restart\",\"correlation-id\": \"baf5ba32-6b8c-430c-a91b-02d2c0ba3064-1\",\"type\": \"response\",\"body\": {\"output\": {\"status\": {\"code\": 401,\"message\": \"Restart Successful\"},\"common-header\": {\"timestamp\": \"2017-07-18T16:52:06.186Z\",\"api-ver\": \"2.01\",\"request-id\": \"baf5ba32-6b8c-430c-a91b-02d2c0ba3064\",\"sub-request-id\": \"1\",\"flags\": {\"ttl\": 600}},\"payload\": \"{\\\"vm-id\\\":\\\"http://135.25.246.131:8774/v2/81fc2bc61f974de1b5a49e8c2ec090bb/servers/75dce20c-97f9-454d-abcc-aa904a33df5a\\\",\\\"tenant-id\\\":\\\"test2\\\"}\"}}}";
        AppcLcmDmaapWrapper restartResponse = Serialization.gson.fromJson(lcmRespJson, AppcLcmDmaapWrapper.class);

        operation.incomingMessage(restartResponse);
        assertEquals(operation.getResult(), PolicyResult.FAILURE);
    }

    @Test
    public void testIncomingAllVnfFailureMessage() {
        policy.setRecipe("RESTART");
        policy.getTarget().setType(TargetType.VNF);
        operation = new AppcLcmOperation(transaction, policy, event, 1);

        String lcmRespJson = "{\"version\": \"2.0\",\"rpc-name\": \"Restart\",\"correlation-id\": \"baf5ba32-6b8c-430c-a91b-02d2c0ba3064-1\",\"type\": \"response\",\"body\": {\"output\": {\"status\": {\"code\": 501,\"message\": \"Restart Successful\"},\"common-header\": {\"timestamp\": \"2017-07-18T16:52:06.186Z\",\"api-ver\": \"2.01\",\"request-id\": \"baf5ba32-6b8c-430c-a91b-02d2c0ba3064\",\"sub-request-id\": \"1\",\"flags\": {\"ttl\": 600}},\"payload\": \"{\\\"vm-id\\\":\\\"http://135.25.246.131:8774/v2/81fc2bc61f974de1b5a49e8c2ec090bb/servers/75dce20c-97f9-454d-abcc-aa904a33df5a\\\",\\\"tenant-id\\\":\\\"test2\\\"}\"}}}";
        AppcLcmDmaapWrapper restartResponse = Serialization.gson.fromJson(lcmRespJson, AppcLcmDmaapWrapper.class);

        /* Send in ALL failure messages */
        for (int i = 0; i < 5; i++) {
            operation.incomingMessage(restartResponse);
            assertEquals(operation.getResult(), null);
        }

        /* Send in an operation failure */
        restartResponse.getBody().getOutput().getStatus().setCode(401);
        operation.incomingMessage(restartResponse);

        /* Because every VM failed in the VNF, it should be failure result */
        assertEquals(operation.getResult(), PolicyResult.FAILURE);
    }

    @Test
    public void testIncomingPartialVnfFailureMessage() {
        policy.setRecipe("RESTART");
        policy.getTarget().setType(TargetType.VNF);
        operation = new AppcLcmOperation(transaction, policy, event, 1);

        String lcmRespJson = "{\"version\": \"2.0\",\"rpc-name\": \"Restart\",\"correlation-id\": \"baf5ba32-6b8c-430c-a91b-02d2c0ba3064-1\",\"type\": \"response\",\"body\": {\"output\": {\"status\": {\"code\": 500,\"message\": \"Restart Successful\"},\"common-header\": {\"timestamp\": \"2017-07-18T16:52:06.186Z\",\"api-ver\": \"2.01\",\"request-id\": \"baf5ba32-6b8c-430c-a91b-02d2c0ba3064\",\"sub-request-id\": \"1\",\"flags\": {\"ttl\": 600}},\"payload\": \"{\\\"vm-id\\\":\\\"http://135.25.246.131:8774/v2/81fc2bc61f974de1b5a49e8c2ec090bb/servers/75dce20c-97f9-454d-abcc-aa904a33df5a\\\",\\\"tenant-id\\\":\\\"test2\\\"}\"}}}";
        AppcLcmDmaapWrapper restartResponse = Serialization.gson.fromJson(lcmRespJson, AppcLcmDmaapWrapper.class);

        /* Send in several partial success messages */
        for (int i = 0; i < 5; i++) {
            operation.incomingMessage(restartResponse);
            assertEquals(operation.getResult(), null);
        }

        /* Change status to partial failure */
        restartResponse.getBody().getOutput().getStatus().setCode(501);

        /* Send in several partial failures messages */
        for (int i = 0; i < 5; i++) {
            operation.incomingMessage(restartResponse);
            assertEquals(operation.getResult(), null);
        }

        /* Send in an operation failure */
        restartResponse.getBody().getOutput().getStatus().setCode(401);
        operation.incomingMessage(restartResponse);

        /*
         * Only a subset of VMs failed in the VNF so the
         * result will be failure_exception
         */
        assertEquals(operation.getResult(), PolicyResult.FAILURE_EXCEPTION);
    }

    /* ===================================================================== */

    /*
     * these tests are for validating the A&AI subtag and target in an onset
     */

    @Test
    public void testValidAaiSubtag() {
        transaction.setNotificationMessage(null);
        VirtualControlLoopEvent validEvent = new VirtualControlLoopEvent();
        validEvent.setTarget("vserver.vserver-name");
        validEvent.getAai().put(AppcLcmOperation.DCAE_CLOSEDLOOP_DISABLED_FIELD, "false");
        validEvent.getAai().put(validEvent.getTarget(), "VM001");
        assertTrue(AppcLcmOperation.isAaiValid(transaction, validEvent));
        assertNull(transaction.getNotificationMessage());
    }

    @Test
    public void testNoAaiSubtag() {
        transaction.setNotificationMessage(null);
        VirtualControlLoopEvent noAaiTag = new VirtualControlLoopEvent();
        noAaiTag.setAai(null);
        assertFalse(AppcLcmOperation.isAaiValid(transaction, noAaiTag));
        assertEquals(transaction.getNotificationMessage(), "No A&AI Subtag");
    }

    @Test
    public void testNoClosedLoopDisabledInAai() {
        transaction.setNotificationMessage(null);
        VirtualControlLoopEvent invalidEvent = new VirtualControlLoopEvent();
        assertFalse(AppcLcmOperation.isAaiValid(transaction, invalidEvent));
        assertEquals(AppcLcmOperation.DCAE_CLOSEDLOOP_DISABLED_FIELD
                     + " information missing", transaction.getNotificationMessage());
    }

    @Test
    public void testClosedLoopDisabledInAai() {
        transaction.setNotificationMessage(null);
        VirtualControlLoopEvent invalidEvent = new VirtualControlLoopEvent();
        invalidEvent.getAai().put(AppcLcmOperation.DCAE_CLOSEDLOOP_DISABLED_FIELD, "true");
        assertFalse(AppcLcmOperation.isAaiValid(transaction, invalidEvent));
        assertEquals(AppcLcmOperation.DCAE_CLOSEDLOOP_DISABLED_FIELD
                     + " is set to true", transaction.getNotificationMessage());
    }

    @Test
    public void testTargetMismatchInAai() {
        transaction.setNotificationMessage(null);
        VirtualControlLoopEvent validEvent = new VirtualControlLoopEvent();
        validEvent.setTarget("vserver.vserver-name");
        validEvent.getAai().put(AppcLcmOperation.DCAE_CLOSEDLOOP_DISABLED_FIELD, "false");
        assertFalse(AppcLcmOperation.isAaiValid(transaction, validEvent));
        assertEquals("target field invalid - must have corresponding AAI value",
                     transaction.getNotificationMessage());
    }

    @Test
    public void testTimeout() {
        policy.setRecipe("RESTART");
        policy.getTarget().setType(TargetType.VNF);
        operation = new AppcLcmOperation(transaction, policy, event, 1);
        operation.timeout();
        assertEquals(PolicyResult.FAILURE_TIMEOUT, operation.getResult());
    }

    @Test
    public void testIncomingAbatedEvent() {
        transaction.setNotificationMessage(null);
        VirtualControlLoopEvent validEvent = new VirtualControlLoopEvent();
        validEvent.setTarget("vserver.vserver-name");
        validEvent.getAai().put(AppcLcmOperation.DCAE_CLOSEDLOOP_DISABLED_FIELD, "false");
        validEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ABATED);
        operation = new AppcLcmOperation(transaction, policy, event, 1);
        operation.incomingMessage(validEvent);
        assertEquals(PolicyResult.SUCCESS, operation.getResult());
    }
}
