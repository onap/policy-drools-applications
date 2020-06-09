/*-
 * ============LICENSE_START=======================================================
 * unit test
 * ================================================================================
 * Copyright (C) 2017-2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019 Bell Canada.
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

package org.onap.policy.controlloop.eventmanager;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.ccsdk.cds.controllerblueprints.processing.api.ExecutionServiceInput;
import org.onap.policy.aai.util.AaiException;
import org.onap.policy.appc.CommonHeader;
import org.onap.policy.appc.Response;
import org.onap.policy.appc.ResponseCode;
import org.onap.policy.appc.ResponseStatus;
import org.onap.policy.appclcm.AppcLcmBody;
import org.onap.policy.appclcm.AppcLcmCommonHeader;
import org.onap.policy.appclcm.AppcLcmDmaapWrapper;
import org.onap.policy.appclcm.AppcLcmInput;
import org.onap.policy.appclcm.AppcLcmOutput;
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.common.utils.io.Serializer;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.ControlLoopTargetType;
import org.onap.policy.controlloop.SupportUtil;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.controlloop.policy.Target;
import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.controlloop.processor.ControlLoopProcessor;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.so.SoResponse;
import org.onap.policy.so.SoResponseWrapper;
import org.onap.policy.vfc.VfcResponse;
import org.onap.policy.vfc.VfcResponseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlLoopOperationManagerTest {
    private static final String VSERVER_NAME = "vserver.vserver-name";
    private static final String TEST_YAML = "src/test/resources/test.yaml";
    private static final String TEST_CDS_YAML = "src/test/resources/test-cds.yaml";
    private static final String ONSET_ONE = "onsetOne";
    private static final String VNF_NAME = "generic-vnf.vnf-name";
    private static final String VNF_ID = "generic-vnf.vnf-id";
    private static final String TWO_ONSET_TEST = "TwoOnsetTest";
    private static final String OPER_MSG = "The Wizard Escaped";
    private static final String OZ_VNF = "OzVNF";
    private static final String OPERATIONS_HISTORY_PU_TEST = "OperationsHistoryPUTest";
    private static final String OPERATIONS_HISTORY_PU = "OperationsHistoryPU";
    private static final String DOROTHY = "Dorothy";
    private static final String APPC_FAILURE_REASON = "AppC failed for some reason";
    private static final String ACCEPT = "ACCEPT";

    private static final Logger logger =
        LoggerFactory.getLogger(ControlLoopOperationManagerTest.class);

    private static VirtualControlLoopEvent onset;

    static {
        onset = new VirtualControlLoopEvent();
        onset.setRequestId(UUID.randomUUID());
        onset.setTarget(VNF_NAME);
        onset.setTargetType(ControlLoopTargetType.VNF);
        onset.setClosedLoopAlarmStart(Instant.now());
        onset.setAai(new HashMap<>());
        onset.getAai().put(VNF_NAME, "testTriggerSource");
        onset.getAai().put(VSERVER_NAME, "testVserverName");
        onset.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onset.setTargetType(ControlLoopTargetType.VNF);

        /* Set environment properties */
        PolicyEngineConstants.getManager().setEnvironmentProperty("aai.url",
            "http://localhost:6666");
        PolicyEngineConstants.getManager().setEnvironmentProperty("aai.username", "AAI");
        PolicyEngineConstants.getManager().setEnvironmentProperty("aai.password", "AAI");
        PolicyEngineConstants.getManager().setEnvironmentProperty("aai.customQuery", "false");
    }

    private static EntityManagerFactory emf;
    private static EntityManager em;

    private static int getCount() {
        // Create a query for number of items in DB
        String sql = "select count(*) as count from operationshistory";
        Query nq = em.createNativeQuery(sql);

        return ((Number) nq.getSingleResult()).intValue();
    }

    /**
     * Set up test class.
     */
    @BeforeClass
    public static void setUp() throws Exception {

        org.onap.policy.simulators.Util.buildAaiSim();

        // Set PU
        System.setProperty(OPERATIONS_HISTORY_PU, OPERATIONS_HISTORY_PU_TEST);

        // Enter dummy props to avoid nullPointerException
        PolicyEngineConstants.getManager()
            .setEnvironmentProperty(org.onap.policy.guard.Util.ONAP_KEY_URL, "a");
        PolicyEngineConstants.getManager()
            .setEnvironmentProperty(org.onap.policy.guard.Util.ONAP_KEY_USER, "b");
        PolicyEngineConstants.getManager()
            .setEnvironmentProperty(org.onap.policy.guard.Util.ONAP_KEY_PASS, "c");

        // Connect to in-mem db
        emf = Persistence.createEntityManagerFactory(OPERATIONS_HISTORY_PU_TEST);
        em = emf.createEntityManager();
    }

    /**
     * Clean up test class.
     */
    @AfterClass
    public static void tearDown() {
        em.close();
        emf.close();
        HttpServletServerFactoryInstance.getServerFactory().destroy();
    }

    @Test
    public void testRetriesFail() throws Exception {
        //
        // Load up the policy
        //
        final SupportUtil.Pair<ControlLoopPolicy, String> pair = SupportUtil.loadYaml(TEST_YAML);
        onset.setClosedLoopControlName(pair.key.getControlLoop().getControlLoopName());
        onset.getAai().put(VSERVER_NAME, "testVserverName");

        //
        // Create a processor
        //
        final ControlLoopProcessor processor = new ControlLoopProcessor(pair.value);
        //
        // create the manager
        //
        ControlLoopEventManager eventManager =
            new ControlLoopEventManager(onset.getClosedLoopControlName(), onset.getRequestId());
        VirtualControlLoopNotification notification = eventManager.activate(onset);

        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        ControlLoopEventManager.NewEventStatus status = eventManager.onNewEvent(onset);
        assertNotNull(status);
        assertEquals(ControlLoopEventManager.NewEventStatus.FIRST_ONSET, status);

        ControlLoopOperationManager manager =
            new ControlLoopOperationManager(onset, processor.getCurrentPolicy(), eventManager);
        logger.debug("{}", manager);
        //
        //
        //
        assertFalse(manager.isOperationComplete());
        assertFalse(manager.isOperationRunning());
        //
        // Start
        //
        Object request = manager.startOperation(onset);
        logger.debug("{}", manager);
        assertNotNull(request);
        assertTrue(request instanceof AppcLcmDmaapWrapper);
        AppcLcmDmaapWrapper dmaapRequest = (AppcLcmDmaapWrapper) request;
        AppcLcmInput appcRequest = dmaapRequest.getBody().getInput();
        assertTrue(appcRequest.getCommonHeader().getSubRequestId().contentEquals("1"));
        assertFalse(manager.isOperationComplete());
        assertTrue(manager.isOperationRunning());
        //
        // Accept
        //
        AppcLcmOutput appcResponse = new AppcLcmOutput(appcRequest);
        appcResponse.getStatus().setCode(100);
        appcResponse.getStatus().setMessage(ACCEPT);
        AppcLcmBody outputBody = new AppcLcmBody();
        outputBody.setOutput(appcResponse);
        AppcLcmDmaapWrapper dmaapResponse = new AppcLcmDmaapWrapper();
        dmaapResponse.setBody(outputBody);
        //
        //
        //
        PolicyResult result = manager.onResponse(dmaapResponse);
        logger.debug("{}", manager);
        assertNull(result);
        assertFalse(manager.isOperationComplete());
        assertTrue(manager.isOperationRunning());
        //
        // Now we are going to Fail it
        //
        appcResponse = new AppcLcmOutput(appcRequest);
        appcResponse.getStatus().setCode(401);
        appcResponse.getStatus().setMessage(APPC_FAILURE_REASON);
        outputBody.setOutput(appcResponse);
        dmaapResponse.setBody(outputBody);
        result = manager.onResponse(dmaapResponse);
        logger.debug("{}", manager);
        assertEquals(PolicyResult.FAILURE, result);
        assertFalse(manager.isOperationComplete());
        assertFalse(manager.isOperationRunning());
        //
        // Retry it
        //
        request = manager.startOperation(onset);
        logger.debug("{}", manager);
        assertNotNull(request);
        assertTrue(request instanceof AppcLcmDmaapWrapper);
        dmaapRequest = (AppcLcmDmaapWrapper) request;
        appcRequest = dmaapRequest.getBody().getInput();
        assertTrue(appcRequest.getCommonHeader().getSubRequestId().contentEquals("2"));
        assertFalse(manager.isOperationComplete());
        assertTrue(manager.isOperationRunning());
        //
        //
        //
        appcResponse = new AppcLcmOutput(appcRequest);
        logger.debug("{}", manager);
        appcResponse.getStatus().setCode(100);
        appcResponse.getStatus().setMessage(ACCEPT);
        outputBody.setOutput(appcResponse);
        dmaapResponse.setBody(outputBody);
        //
        //
        //
        result = manager.onResponse(dmaapResponse);
        logger.debug("{}", manager);
        assertNull(result);
        assertFalse(manager.isOperationComplete());
        assertTrue(manager.isOperationRunning());
        //
        // Now we are going to Fail it
        //
        appcResponse = new AppcLcmOutput(appcRequest);
        appcResponse.getStatus().setCode(401);
        appcResponse.getStatus().setMessage(APPC_FAILURE_REASON);
        outputBody.setOutput(appcResponse);
        dmaapResponse.setBody(outputBody);
        result = manager.onResponse(dmaapResponse);
        logger.debug("{}", manager);
        assertEquals(PolicyResult.FAILURE, result);
        //
        // Should be complete now
        //
        assertTrue(manager.isOperationComplete());
        assertFalse(manager.isOperationRunning());
        assertNotNull(manager.getOperationResult());
        assertEquals(PolicyResult.FAILURE_RETRIES, manager.getOperationResult());
        assertEquals(2, manager.getHistory().size());
    }

    @Test
    public void testTimeout() throws Exception {
        //
        // Load up the policy
        //
        final SupportUtil.Pair<ControlLoopPolicy, String> pair = SupportUtil.loadYaml(TEST_YAML);
        onset.setClosedLoopControlName(pair.key.getControlLoop().getControlLoopName());
        onset.getAai().put(VSERVER_NAME, "OzVServer");

        //
        // Create a processor
        //
        final ControlLoopProcessor processor = new ControlLoopProcessor(pair.value);
        //
        // create the manager
        //
        ControlLoopEventManager eventManager =
            new ControlLoopEventManager(onset.getClosedLoopControlName(), onset.getRequestId());
        VirtualControlLoopNotification notification = eventManager.activate(onset);

        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        ControlLoopEventManager.NewEventStatus status = eventManager.onNewEvent(onset);
        assertNotNull(status);
        assertEquals(ControlLoopEventManager.NewEventStatus.FIRST_ONSET, status);

        ControlLoopOperationManager manager =
            new ControlLoopOperationManager(onset, processor.getCurrentPolicy(), eventManager);
        //
        //
        //
        logger.debug("{}", manager);
        assertFalse(manager.isOperationComplete());
        assertFalse(manager.isOperationRunning());
        //
        // Start
        //
        Object request = manager.startOperation(onset);
        logger.debug("{}", manager);
        assertNotNull(request);
        assertTrue((request) instanceof AppcLcmDmaapWrapper);
        AppcLcmDmaapWrapper dmaapRequest = (AppcLcmDmaapWrapper) request;
        AppcLcmInput appcRequest = dmaapRequest.getBody().getInput();
        assertTrue((appcRequest).getCommonHeader().getSubRequestId().contentEquals("1"));
        assertFalse(manager.isOperationComplete());
        assertTrue(manager.isOperationRunning());
        //
        // Accept
        //
        AppcLcmDmaapWrapper dmaapResponse = new AppcLcmDmaapWrapper();
        AppcLcmOutput appcResponse = new AppcLcmOutput(appcRequest);
        AppcLcmBody outputBody = new AppcLcmBody();
        outputBody.setOutput(appcResponse);
        dmaapResponse.setBody(outputBody);
        appcResponse.getStatus().setCode(100);
        appcResponse.getStatus().setMessage(ACCEPT);
        //
        //
        //
        PolicyResult result = manager.onResponse(dmaapResponse);
        logger.debug("{}", manager);
        assertNull(result);
        assertFalse(manager.isOperationComplete());
        assertTrue(manager.isOperationRunning());
        //
        // Now we are going to simulate Timeout
        //
        manager.setOperationHasTimedOut();
        logger.debug("{}", manager);
        assertTrue(manager.isOperationComplete());
        assertFalse(manager.isOperationRunning());
        assertEquals(1, manager.getHistory().size());
        assertEquals(PolicyResult.FAILURE_TIMEOUT, manager.getOperationResult());
        //
        // Now we are going to Fail the previous request
        //
        appcResponse = new AppcLcmOutput(appcRequest);
        appcResponse.getStatus().setCode(401);
        appcResponse.getStatus().setMessage(APPC_FAILURE_REASON);
        outputBody.setOutput(appcResponse);
        dmaapResponse.setBody(outputBody);
        manager.onResponse(dmaapResponse);
        logger.debug("{}", manager);
        //
        //
        //
        assertTrue(manager.isOperationComplete());
        assertFalse(manager.isOperationRunning());
        assertEquals(1, manager.getHistory().size());
        assertEquals(PolicyResult.FAILURE_TIMEOUT, manager.getOperationResult());
    }

    @Test
    public void testMethods() throws IOException, ControlLoopException, AaiException {
        InputStream is = new FileInputStream(new File("src/test/resources/testSOactor.yaml"));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
        onsetEvent.setClosedLoopControlName(TWO_ONSET_TEST);
        onsetEvent.setRequestId(requestId);
        onsetEvent.setTarget(VNF_ID);
        onsetEvent.setTargetType(ControlLoopTargetType.VNF);
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put(VNF_NAME, ONSET_ONE);
        onsetEvent.getAai().put(VSERVER_NAME, "testVserverName");

        ControlLoopEventManager manager = new ControlLoopEventManager(
            onsetEvent.getClosedLoopControlName(), onsetEvent.getRequestId());
        VirtualControlLoopNotification notification = manager.activate(yamlString, onsetEvent);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        ControlLoopOperationManager clom = manager.processControlLoop();
        assertNotNull(clom);
        assertNull(clom.getOperationResult());

        clom.setEventManager(manager);
        assertEquals(manager, clom.getEventManager());

        assertNull(clom.getTargetEntity());

        clom.setGuardApprovalStatus("WizardOKedIt");
        assertEquals("WizardOKedIt", clom.getGuardApprovalStatus());

        assertNull(clom.getOperationResult());

        Policy policy = manager.getProcessor().getCurrentPolicy();
        clom.getTarget(policy);

        final Target savedTarget = policy.getTarget();
        policy.setTarget(null);
        assertThatThrownBy(() -> clom.getTarget(policy)).hasMessage("The target is null");

        policy.setTarget(new Target());
        assertThatThrownBy(() -> clom.getTarget(policy)).hasMessage("The target type is null");

        policy.setTarget(savedTarget);

        policy.getTarget().setType(TargetType.PNF);
        assertThatThrownBy(() -> clom.getTarget(policy)).hasMessage(
            "Target in the onset event is either null or does not match target key expected in AAI section.");

        onsetEvent.setTarget("Oz");
        onsetEvent.getAai().remove(VNF_NAME);
        onsetEvent.getAai().remove(VNF_ID);
        onsetEvent.getAai().remove(VSERVER_NAME);

        policy.getTarget().setType(TargetType.VNF);
        assertThatThrownBy(() -> clom.getTarget(policy))
            .hasMessage("Target does not match target type");

        onsetEvent.setTarget(VSERVER_NAME);
        onsetEvent.getAai().put(VSERVER_NAME, "OzVServer");
        assertEquals("OzVServer", clom.getTarget(policy));

        onsetEvent.getAai().remove(VSERVER_NAME);
        onsetEvent.setTarget(VNF_ID);
        onsetEvent.getAai().put(VNF_ID, OZ_VNF);
        assertEquals(OZ_VNF, clom.getTarget(policy));

        onsetEvent.setTarget(VNF_NAME);
        assertEquals(OZ_VNF, clom.getTarget(policy));

        manager.onNewEvent(onsetEvent);

        policy.getTarget().setType(TargetType.VFC);
        assertThatThrownBy(() -> clom.getTarget(policy))
            .hasMessage("The target type is not supported");

        assertEquals(Integer.valueOf(20), clom.getOperationTimeout());

        assertEquals("20s", clom.getOperationTimeoutString(100));

        assertEquals(null, clom.getOperationMessage());
        assertEquals(null, clom.getOperationMessage(OPER_MSG));

        clom.startOperation(onsetEvent);

        assertEquals(
            "actor=SO,operation=Restart,target=Target [type=VFC, resourceId=null],subRequestId=1",
            clom.getOperationMessage());
        assertEquals(
            "actor=SO,operation=Restart,target=Target [type=VFC, resourceId=null],subRequestId=1, Guard result: "
                + OPER_MSG,
            clom.getOperationMessage(OPER_MSG));

        assertEquals("actor=SO,operation=Restart,tar", clom.getOperationHistory().substring(0, 30));

        clom.setOperationHasException("The Wizard is gone");
        clom.setOperationHasGuardDeny();
    }

    @Test
    public void testConstructor() throws IOException, ControlLoopException, AaiException {
        InputStream is = new FileInputStream(new File(TEST_YAML));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
        onsetEvent.setClosedLoopControlName(TWO_ONSET_TEST);
        onsetEvent.setRequestId(requestId);
        onsetEvent.setTarget(VNF_ID);
        onsetEvent.setTargetType(ControlLoopTargetType.VNF);
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put(VNF_NAME, ONSET_ONE);
        onsetEvent.getAai().put(VSERVER_NAME, "OzVServer");

        ControlLoopEventManager manager = new ControlLoopEventManager(
            onsetEvent.getClosedLoopControlName(), onsetEvent.getRequestId());
        VirtualControlLoopNotification notification = manager.activate(yamlString, onsetEvent);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        Policy policy = manager.getProcessor().getCurrentPolicy();
        ControlLoopOperationManager clom =
            new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);

        policy.setRecipe("ModifyConfig");
        onsetEvent.getAai().put(VSERVER_NAME, "NonExistentVserver");
        policy.getTarget().setResourceID(UUID.randomUUID().toString());
        assertThatThrownBy(() -> new ControlLoopOperationManager(onsetEvent, policy, manager))
            .hasMessage("Target vnf-id could not be found");

        onsetEvent.getAai().put(VSERVER_NAME, "testVserverName");
        policy.getTarget().setResourceID("bbb3cefd-01c8-413c-9bdd-2b92f9ca3d38");
        clom = new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);

        policy.setActor("SO");
        clom = new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);

        policy.setActor("VFC");
        clom = new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);

        policy.setActor(DOROTHY);
        assertThatThrownBy(() -> new ControlLoopOperationManager(onsetEvent, policy, manager))
            .hasMessage("ControlLoopEventManager: policy has an unknown actor.");
    }

    @Test
    public void testStartOperation() throws IOException, ControlLoopException, AaiException {
        InputStream is = new FileInputStream(new File(TEST_YAML));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
        onsetEvent.setClosedLoopControlName(TWO_ONSET_TEST);
        onsetEvent.setRequestId(requestId);
        onsetEvent.setTarget(VNF_ID);
        onsetEvent.setTargetType(ControlLoopTargetType.VNF);
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put(VNF_NAME, ONSET_ONE);
        onsetEvent.getAai().put(VSERVER_NAME, "testVserverName");

        ControlLoopEventManager manager = new ControlLoopEventManager(
            onsetEvent.getClosedLoopControlName(), onsetEvent.getRequestId());
        VirtualControlLoopNotification notification = manager.activate(yamlString, onsetEvent);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        Policy policy = manager.getProcessor().getCurrentPolicy();
        ControlLoopOperationManager clom =
            new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);

        clom.startOperation(onsetEvent);
        ControlLoopOperationManager clom2 = clom;
        assertThatThrownBy(() -> clom2.startOperation(onsetEvent))
            .hasMessage("current operation is not null (an operation is already running)");

        clom = new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);
        final String savedRecipe = policy.getRecipe();
        policy.setRecipe("ModifyConfig");
        policy.getTarget().setResourceID(UUID.randomUUID().toString());
        clom.startOperation(onsetEvent);
        policy.setRecipe(savedRecipe);

        policy.setRetry(null);
        clom = new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);
        clom.startOperation(onsetEvent);
        clom.setOperationHasTimedOut();
        assertTrue(clom.isOperationComplete());
        ControlLoopOperationManager clom3 = clom;
        assertThatThrownBy(() -> clom3.startOperation(onsetEvent))
            .hasMessage("current operation failed and retries are not allowed");

        policy.setRetry(0);
        clom = new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);
        clom.startOperation(onsetEvent);
        clom.setOperationHasTimedOut();
        assertTrue(clom.isOperationComplete());
        ControlLoopOperationManager clom4 = clom;
        assertThatThrownBy(() -> clom4.startOperation(onsetEvent))
            .hasMessage("current operation failed and retries are not allowed");

        policy.setRetry(1);
        clom = new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);
        clom.startOperation(onsetEvent);
        clom.setOperationHasTimedOut();
        clom.startOperation(onsetEvent);
        clom.setOperationHasTimedOut();
        assertTrue(clom.isOperationComplete());
        ControlLoopOperationManager clom5 = clom;
        assertThatThrownBy(() -> clom5.startOperation(onsetEvent))
            .hasMessage("current oepration has failed after 2 retries");

        clom = new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);
        policy.setActor("SO");
        clom.startOperation(onsetEvent);

        clom = new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);
        policy.setActor("VFC");
        clom.startOperation(onsetEvent);

        clom = new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);
        policy.setActor("Oz");
        ControlLoopOperationManager clom6 = clom;
        assertThatThrownBy(() -> clom6.startOperation(onsetEvent))
            .hasMessage("invalid actor Oz on policy");
    }

    @Test
    public void testOnResponse() throws IOException, ControlLoopException, AaiException {
        InputStream is = new FileInputStream(new File(TEST_YAML));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
        onsetEvent.setClosedLoopControlName(TWO_ONSET_TEST);
        onsetEvent.setRequestId(requestId);
        onsetEvent.setTarget(VNF_ID);
        onsetEvent.setTargetType(ControlLoopTargetType.VNF);
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put(VNF_NAME, ONSET_ONE);
        onsetEvent.getAai().put(VSERVER_NAME, "testVserverName");

        ControlLoopEventManager manager = new ControlLoopEventManager(
            onsetEvent.getClosedLoopControlName(), onsetEvent.getRequestId());
        VirtualControlLoopNotification notification = manager.activate(yamlString, onsetEvent);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        Policy policy = manager.getProcessor().getCurrentPolicy();
        ControlLoopOperationManager clom =
            new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);

        assertNull(clom.onResponse(null));

        Response appcResponse = new Response();
        CommonHeader commonHeader = new CommonHeader();
        appcResponse.setCommonHeader(commonHeader);
        assertEquals(PolicyResult.FAILURE_EXCEPTION, clom.onResponse(appcResponse));

        commonHeader.setSubRequestId("12345");
        appcResponse.setStatus(null);
        assertEquals(PolicyResult.FAILURE_EXCEPTION, clom.onResponse(appcResponse));

        ResponseStatus responseStatus = new ResponseStatus();
        appcResponse.setStatus(responseStatus);
        assertEquals(PolicyResult.FAILURE_EXCEPTION, clom.onResponse(appcResponse));

        responseStatus.setCode(0);
        assertEquals(PolicyResult.FAILURE_EXCEPTION, clom.onResponse(appcResponse));

        responseStatus.setCode(ResponseCode.ACCEPT.getValue());
        assertEquals(null, clom.onResponse(appcResponse));

        responseStatus.setCode(ResponseCode.ERROR.getValue());
        assertEquals(PolicyResult.FAILURE_EXCEPTION, clom.onResponse(appcResponse));

        responseStatus.setCode(ResponseCode.FAILURE.getValue());
        assertEquals(PolicyResult.FAILURE, clom.onResponse(appcResponse));

        responseStatus.setCode(ResponseCode.REJECT.getValue());
        assertEquals(PolicyResult.FAILURE_EXCEPTION, clom.onResponse(appcResponse));

        responseStatus.setCode(ResponseCode.SUCCESS.getValue());
        assertEquals(PolicyResult.SUCCESS, clom.onResponse(appcResponse));

        AppcLcmDmaapWrapper dmaapWrapper = new AppcLcmDmaapWrapper();
        AppcLcmBody body = new AppcLcmBody();
        AppcLcmOutput output = new AppcLcmOutput();
        AppcLcmCommonHeader lcmCh = new AppcLcmCommonHeader();
        output.setCommonHeader(lcmCh);
        body.setOutput(output);
        dmaapWrapper.setBody(body);

        lcmCh.setSubRequestId("NotANumber");
        assertEquals(PolicyResult.FAILURE_EXCEPTION, clom.onResponse(dmaapWrapper));

        lcmCh.setSubRequestId("12345");
        assertEquals(PolicyResult.FAILURE_EXCEPTION, clom.onResponse(dmaapWrapper));

        SoResponse soResponse = new SoResponse();
        SoResponseWrapper soRw = new SoResponseWrapper(soResponse, null);

        soResponse.setHttpResponseCode(200);
        assertEquals(PolicyResult.SUCCESS, clom.onResponse(soRw));

        soResponse.setHttpResponseCode(202);
        assertEquals(PolicyResult.SUCCESS, clom.onResponse(soRw));

        soResponse.setHttpResponseCode(500);
        assertEquals(PolicyResult.FAILURE, clom.onResponse(soRw));

        VfcResponse vfcResponse = new VfcResponse();
        VfcResponseDescriptor responseDescriptor = new VfcResponseDescriptor();
        vfcResponse.setResponseDescriptor(responseDescriptor);

        responseDescriptor.setStatus("finished");
        assertEquals(PolicyResult.SUCCESS, clom.onResponse(vfcResponse));

        responseDescriptor.setStatus("unfinished");
        assertEquals(PolicyResult.FAILURE, clom.onResponse(vfcResponse));
    }

    @Test
    public void testCompleteOperation() throws ControlLoopException, AaiException, IOException {
        InputStream is = new FileInputStream(new File(TEST_YAML));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
        onsetEvent.setClosedLoopControlName(TWO_ONSET_TEST);
        onsetEvent.setRequestId(requestId);
        onsetEvent.setTarget(VNF_ID);
        onsetEvent.setTargetType(ControlLoopTargetType.VNF);
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put(VNF_NAME, ONSET_ONE);
        onsetEvent.getAai().put(VSERVER_NAME, "testVserverName");

        ControlLoopEventManager manager = new ControlLoopEventManager(
            onsetEvent.getClosedLoopControlName(), onsetEvent.getRequestId());
        VirtualControlLoopNotification notification = manager.activate(yamlString, onsetEvent);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        Policy policy = manager.getProcessor().getCurrentPolicy();
        ControlLoopOperationManager clom =
            new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);

        clom.startOperation(onsetEvent);

        SoResponse soResponse = new SoResponse();
        final SoResponseWrapper soRw = new SoResponseWrapper(soResponse, null);

        PolicyEngineConstants.getManager().setEnvironmentProperty("guard.disabled", "false");
        PolicyEngineConstants.getManager().setEnvironmentProperty(
            org.onap.policy.guard.Util.ONAP_KEY_URL, "http://somewhere.over.the.rainbow");
        PolicyEngineConstants.getManager()
            .setEnvironmentProperty(org.onap.policy.guard.Util.ONAP_KEY_USER, DOROTHY);
        PolicyEngineConstants.getManager()
            .setEnvironmentProperty(org.onap.policy.guard.Util.ONAP_KEY_PASS, "Toto");

        assertEquals(PolicyResult.FAILURE, clom.onResponse(soRw));

        System.setProperty(OPERATIONS_HISTORY_PU, OPERATIONS_HISTORY_PU_TEST);
        assertEquals(PolicyResult.FAILURE, clom.onResponse(soRw));
    }

    @Test
    public void testStartCdsOperation() throws ControlLoopException, IOException {

        // Prepare
        String yamlString;
        try (InputStream is = new FileInputStream(new File(TEST_CDS_YAML))) {
            yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);
        }

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(TWO_ONSET_TEST);
        event.setRequestId(requestId);
        event.setTarget(VNF_ID);
        event.setTargetType(ControlLoopTargetType.VNF);
        event.setClosedLoopAlarmStart(Instant.now());
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event.setAai(new HashMap<>());
        event.getAai().put(VNF_NAME, ONSET_ONE);
        event.getAai().put(VSERVER_NAME, "OzVServer");

        ControlLoopEventManager eventManager =
            new ControlLoopEventManager(event.getClosedLoopControlName(), event.getRequestId());
        VirtualControlLoopNotification notification = eventManager.activate(yamlString, event);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        Policy policy = eventManager.getProcessor().getCurrentPolicy();
        ControlLoopOperationManager operationManager =
            new ControlLoopOperationManager(event, policy, eventManager);

        // Run
        Object result = operationManager.startOperation(event);

        // Verify
        assertNotNull(result);
        assertTrue(result instanceof ExecutionServiceInput);
        ExecutionServiceInput request = (ExecutionServiceInput) result;
        logger.debug("request: " + request);

    }

    @Test
    public void testCommitAbatement() throws Exception {

        String yamlString;
        try (InputStream is = new FileInputStream(new File(TEST_YAML))) {
            yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);
        }

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
        onsetEvent.setClosedLoopControlName(TWO_ONSET_TEST);
        onsetEvent.setRequestId(requestId);
        onsetEvent.setTarget(VNF_ID);
        onsetEvent.setTargetType(ControlLoopTargetType.VNF);
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put(VNF_NAME, ONSET_ONE);
        onsetEvent.getAai().put(VSERVER_NAME, "testVserverName");

        ControlLoopEventManager manager = new ControlLoopEventManager(
            onsetEvent.getClosedLoopControlName(), onsetEvent.getRequestId());
        VirtualControlLoopNotification notification = manager.activate(yamlString, onsetEvent);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        Policy policy = manager.getProcessor().getCurrentPolicy();
        ControlLoopOperationManager clom =
            new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);

        clom.startOperation(onsetEvent);

        int numEventsBefore = getCount();
        logger.info("numEventsBefore={}", numEventsBefore);

        clom.commitAbatement("Test message", "TEST_RESULT");

        int numEventsAfter = getCount();
        logger.info("numEventsAfter={}", numEventsAfter);

        int diff = numEventsAfter - numEventsBefore;
        assertEquals(1, diff);
    }

    @Test
    public void testSerialization() throws Exception {
        InputStream is = new FileInputStream(new File(TEST_YAML));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
        onsetEvent.setClosedLoopControlName(TWO_ONSET_TEST);
        onsetEvent.setRequestId(requestId);
        onsetEvent.setTarget(VNF_ID);
        onsetEvent.setTargetType(ControlLoopTargetType.VNF);
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put(VNF_NAME, ONSET_ONE);
        onsetEvent.getAai().put(VSERVER_NAME, "testVserverName");

        ControlLoopEventManager manager = new ControlLoopEventManager(
            onsetEvent.getClosedLoopControlName(), onsetEvent.getRequestId());
        VirtualControlLoopNotification notification = manager.activate(yamlString, onsetEvent);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        Policy policy = manager.getProcessor().getCurrentPolicy();
        ControlLoopOperationManager clom =
            new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);

        clom.startOperation(onsetEvent);
        assertTrue(clom.isOperationRunning());

        clom = Serializer.roundTrip(clom);
        assertNotNull(clom);
        assertTrue(clom.isOperationRunning());

        SoResponse soResponse = new SoResponse();
        final SoResponseWrapper soRw = new SoResponseWrapper(soResponse, null);

        PolicyEngineConstants.getManager().setEnvironmentProperty("guard.disabled", "false");
        PolicyEngineConstants.getManager().setEnvironmentProperty(
            org.onap.policy.guard.Util.ONAP_KEY_URL, "http://somewhere.over.the.rainbow");
        PolicyEngineConstants.getManager()
            .setEnvironmentProperty(org.onap.policy.guard.Util.ONAP_KEY_USER, DOROTHY);
        PolicyEngineConstants.getManager()
            .setEnvironmentProperty(org.onap.policy.guard.Util.ONAP_KEY_PASS, "Toto");

        assertEquals(PolicyResult.FAILURE, clom.onResponse(soRw));
        assertFalse(clom.isOperationRunning());
        assertEquals(1, clom.getHistory().size());

        clom = Serializer.roundTrip(clom);
        assertNotNull(clom);
        assertFalse(clom.isOperationRunning());
        assertEquals(1, clom.getHistory().size());

        System.setProperty(OPERATIONS_HISTORY_PU, OPERATIONS_HISTORY_PU_TEST);
        assertEquals(PolicyResult.FAILURE, clom.onResponse(soRw));

        clom = Serializer.roundTrip(clom);
        assertNotNull(clom);
        assertFalse(clom.isOperationRunning());
        assertEquals(1, clom.getHistory().size());
    }
}
