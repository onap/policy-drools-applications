/*-
 * ============LICENSE_START=======================================================
 * unit test
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
import org.onap.policy.aai.util.AaiException;
import org.onap.policy.appc.CommonHeader;
import org.onap.policy.appc.Response;
import org.onap.policy.appc.ResponseCode;
import org.onap.policy.appc.ResponseStatus;
import org.onap.policy.appclcm.LcmCommonHeader;
import org.onap.policy.appclcm.LcmRequest;
import org.onap.policy.appclcm.LcmRequestWrapper;
import org.onap.policy.appclcm.LcmResponse;
import org.onap.policy.appclcm.LcmResponseWrapper;
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
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.so.SoResponse;
import org.onap.policy.so.SoResponseWrapper;
import org.onap.policy.vfc.VfcResponse;
import org.onap.policy.vfc.VfcResponseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlLoopOperationManagerTest {
    private static final String VSERVER_NAME = "vserver.vserver-name";
    private static final String TEST_YAML = "src/test/resources/test.yaml";
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


    private static final Logger logger = LoggerFactory.getLogger(ControlLoopOperationManagerTest.class);


    private static VirtualControlLoopEvent onset;

    static {
        onset = new VirtualControlLoopEvent();
        onset.setRequestId(UUID.randomUUID());
        onset.setTarget(VNF_NAME);
        onset.setTargetType(ControlLoopTargetType.VNF);
        onset.setClosedLoopAlarmStart(Instant.now());
        onset.setAai(new HashMap<>());
        onset.getAai().put(VNF_NAME, "testTriggerSource");
        onset.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);

        /* Set environment properties */
        PolicyEngine.manager.setEnvironmentProperty("aai.url", "http://localhost:6666");
        PolicyEngine.manager.setEnvironmentProperty("aai.username", "AAI");
        PolicyEngine.manager.setEnvironmentProperty("aai.password", "AAI");
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
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.ONAP_KEY_URL, "a");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.ONAP_KEY_USER, "b");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.ONAP_KEY_PASS, "c");

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
        assertTrue(request instanceof LcmRequestWrapper);
        LcmRequestWrapper dmaapRequest = (LcmRequestWrapper) request;
        LcmRequest appcRequest = dmaapRequest.getBody();
        assertTrue(appcRequest.getCommonHeader().getSubRequestId().contentEquals("1"));
        assertFalse(manager.isOperationComplete());
        assertTrue(manager.isOperationRunning());
        //
        // Accept
        //
        LcmResponseWrapper dmaapResponse = new LcmResponseWrapper();
        LcmResponse appcResponse = new LcmResponse(appcRequest);
        appcResponse.getStatus().setCode(100);
        appcResponse.getStatus().setMessage(ACCEPT);
        dmaapResponse.setBody(appcResponse);
        //
        //
        //
        PolicyResult result = manager.onResponse(dmaapResponse);
        logger.debug("{}", manager);
        assertTrue(result == null);
        assertFalse(manager.isOperationComplete());
        assertTrue(manager.isOperationRunning());
        //
        // Now we are going to Fail it
        //
        appcResponse = new LcmResponse(appcRequest);
        appcResponse.getStatus().setCode(401);
        appcResponse.getStatus().setMessage(APPC_FAILURE_REASON);
        dmaapResponse.setBody(appcResponse);
        result = manager.onResponse(dmaapResponse);
        logger.debug("{}", manager);
        assertTrue(result.equals(PolicyResult.FAILURE));
        assertFalse(manager.isOperationComplete());
        assertFalse(manager.isOperationRunning());
        //
        // Retry it
        //
        request = manager.startOperation(onset);
        logger.debug("{}", manager);
        assertNotNull(request);
        assertTrue(request instanceof LcmRequestWrapper);
        dmaapRequest = (LcmRequestWrapper) request;
        appcRequest = dmaapRequest.getBody();
        assertTrue(appcRequest.getCommonHeader().getSubRequestId().contentEquals("2"));
        assertFalse(manager.isOperationComplete());
        assertTrue(manager.isOperationRunning());
        //
        //
        //
        appcResponse = new LcmResponse(appcRequest);
        logger.debug("{}", manager);
        appcResponse.getStatus().setCode(100);
        appcResponse.getStatus().setMessage(ACCEPT);
        dmaapResponse.setBody(appcResponse);
        //
        //
        //
        result = manager.onResponse(dmaapResponse);
        logger.debug("{}", manager);
        assertTrue(result == null);
        assertFalse(manager.isOperationComplete());
        assertTrue(manager.isOperationRunning());
        //
        // Now we are going to Fail it
        //
        appcResponse = new LcmResponse(appcRequest);
        appcResponse.getStatus().setCode(401);
        appcResponse.getStatus().setMessage(APPC_FAILURE_REASON);
        dmaapResponse.setBody(appcResponse);
        result = manager.onResponse(dmaapResponse);
        logger.debug("{}", manager);
        assertTrue(result.equals(PolicyResult.FAILURE));
        //
        // Should be complete now
        //
        assertTrue(manager.isOperationComplete());
        assertFalse(manager.isOperationRunning());
        assertNotNull(manager.getOperationResult());
        assertTrue(manager.getOperationResult().equals(PolicyResult.FAILURE_RETRIES));
        assertTrue(manager.getHistory().size() == 2);
    }

    @Test
    public void testTimeout() throws Exception {
        //
        // Load up the policy
        //
        final SupportUtil.Pair<ControlLoopPolicy, String> pair = SupportUtil.loadYaml(TEST_YAML);
        onset.setClosedLoopControlName(pair.key.getControlLoop().getControlLoopName());

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
        assertTrue((request) instanceof LcmRequestWrapper);
        LcmRequestWrapper dmaapRequest = (LcmRequestWrapper) request;
        LcmRequest appcRequest = dmaapRequest.getBody();
        assertTrue((appcRequest).getCommonHeader().getSubRequestId().contentEquals("1"));
        assertFalse(manager.isOperationComplete());
        assertTrue(manager.isOperationRunning());
        //
        // Accept
        //
        LcmResponseWrapper dmaapResponse = new LcmResponseWrapper();
        LcmResponse appcResponse = new LcmResponse(appcRequest);
        dmaapResponse.setBody(appcResponse);
        appcResponse.getStatus().setCode(100);
        appcResponse.getStatus().setMessage(ACCEPT);
        //
        //
        //
        PolicyResult result = manager.onResponse(dmaapResponse);
        logger.debug("{}", manager);
        assertTrue(result == null);
        assertFalse(manager.isOperationComplete());
        assertTrue(manager.isOperationRunning());
        //
        // Now we are going to simulate Timeout
        //
        manager.setOperationHasTimedOut();
        logger.debug("{}", manager);
        assertTrue(manager.isOperationComplete());
        assertFalse(manager.isOperationRunning());
        assertTrue(manager.getHistory().size() == 1);
        assertTrue(manager.getOperationResult().equals(PolicyResult.FAILURE_TIMEOUT));
        //
        // Now we are going to Fail the previous request
        //
        appcResponse = new LcmResponse(appcRequest);
        appcResponse.getStatus().setCode(401);
        appcResponse.getStatus().setMessage(APPC_FAILURE_REASON);
        dmaapResponse.setBody(appcResponse);
        manager.onResponse(dmaapResponse);
        logger.debug("{}", manager);
        //
        //
        //
        assertTrue(manager.isOperationComplete());
        assertFalse(manager.isOperationRunning());
        assertTrue(manager.getHistory().size() == 1);
        assertTrue(manager.getOperationResult().equals(PolicyResult.FAILURE_TIMEOUT));
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
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put(VNF_NAME, ONSET_ONE);

        ControlLoopEventManager manager =
                new ControlLoopEventManager(onsetEvent.getClosedLoopControlName(), onsetEvent.getRequestId());
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
        assertThatThrownBy(() -> clom.getTarget(policy)).hasMessage("PNF target is not supported");

        onsetEvent.setTarget("Oz");
        onsetEvent.getAai().remove(VNF_NAME);
        onsetEvent.getAai().remove(VNF_ID);
        onsetEvent.getAai().remove(VSERVER_NAME);

        policy.getTarget().setType(TargetType.VNF);
        assertThatThrownBy(() -> clom.getTarget(policy)).hasMessage("Target does not match target type");

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

        onsetEvent.getAai().remove(VNF_ID);
        manager.getVnfResponse();
        if (!Boolean.valueOf(PolicyEngine.manager.getEnvironmentProperty("aai.customQuery"))) {
            clom.getEventManager().getVnfResponse().setVnfId(VNF_ID);
            assertEquals(VNF_ID, clom.getTarget(policy));
        }


        policy.getTarget().setType(TargetType.VFC);
        assertThatThrownBy(() -> clom.getTarget(policy)).hasMessage("The target type is not supported");

        assertEquals(Integer.valueOf(20), clom.getOperationTimeout());

        assertEquals("20s", clom.getOperationTimeoutString(100));

        assertEquals(null, clom.getOperationMessage());
        assertEquals(null, clom.getOperationMessage(OPER_MSG));

        clom.startOperation(onsetEvent);

        assertEquals("actor=SO,operation=Restart,target=Target [type=VFC, resourceId=null],subRequestId=1",
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
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put(VNF_NAME, ONSET_ONE);

        ControlLoopEventManager manager =
                new ControlLoopEventManager(onsetEvent.getClosedLoopControlName(), onsetEvent.getRequestId());
        VirtualControlLoopNotification notification = manager.activate(yamlString, onsetEvent);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        Policy policy = manager.getProcessor().getCurrentPolicy();
        ControlLoopOperationManager clom = new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);

        policy.setRecipe("ModifyConfig");
        policy.getTarget().setResourceID(UUID.randomUUID().toString());
        assertThatThrownBy(() -> new ControlLoopOperationManager(onsetEvent, policy, manager))
                        .hasMessage("Target vnf-id could not be found");

        policy.getTarget().setResourceID("82194af1-3c2c-485a-8f44-420e22a9eaa4");
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
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put(VNF_NAME, ONSET_ONE);

        ControlLoopEventManager manager =
                new ControlLoopEventManager(onsetEvent.getClosedLoopControlName(), onsetEvent.getRequestId());
        VirtualControlLoopNotification notification = manager.activate(yamlString, onsetEvent);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        Policy policy = manager.getProcessor().getCurrentPolicy();
        ControlLoopOperationManager clom = new ControlLoopOperationManager(onsetEvent, policy, manager);
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
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put(VNF_NAME, ONSET_ONE);

        ControlLoopEventManager manager =
                new ControlLoopEventManager(onsetEvent.getClosedLoopControlName(), onsetEvent.getRequestId());
        VirtualControlLoopNotification notification = manager.activate(yamlString, onsetEvent);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        Policy policy = manager.getProcessor().getCurrentPolicy();
        ControlLoopOperationManager clom = new ControlLoopOperationManager(onsetEvent, policy, manager);
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

        LcmResponseWrapper lrw = new LcmResponseWrapper();
        LcmResponse body = new LcmResponse();
        LcmCommonHeader lcmCh = new LcmCommonHeader();
        body.setCommonHeader(lcmCh);
        lrw.setBody(body);

        lcmCh.setSubRequestId("NotANumber");
        assertEquals(PolicyResult.FAILURE_EXCEPTION, clom.onResponse(lrw));

        lcmCh.setSubRequestId("12345");
        assertEquals(PolicyResult.FAILURE_EXCEPTION, clom.onResponse(lrw));

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
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put(VNF_NAME, ONSET_ONE);

        ControlLoopEventManager manager =
                new ControlLoopEventManager(onsetEvent.getClosedLoopControlName(), onsetEvent.getRequestId());
        VirtualControlLoopNotification notification = manager.activate(yamlString, onsetEvent);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        Policy policy = manager.getProcessor().getCurrentPolicy();
        ControlLoopOperationManager clom = new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);

        clom.startOperation(onsetEvent);

        SoResponse soResponse = new SoResponse();
        final SoResponseWrapper soRw = new SoResponseWrapper(soResponse, null);

        PolicyEngine.manager.setEnvironmentProperty("guard.disabled", "false");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.ONAP_KEY_URL,
                "http://somewhere.over.the.rainbow");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.ONAP_KEY_USER, DOROTHY);
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.ONAP_KEY_PASS, "Toto");

        assertEquals(PolicyResult.FAILURE, clom.onResponse(soRw));

        System.setProperty(OPERATIONS_HISTORY_PU, OPERATIONS_HISTORY_PU_TEST);
        assertEquals(PolicyResult.FAILURE, clom.onResponse(soRw));
    }

    @Test
    public void testCommitAbatement() throws Exception {

        String yamlString = null;
        try (InputStream is = new FileInputStream(new File(TEST_YAML))) {
            yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);
        }

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
        onsetEvent.setClosedLoopControlName(TWO_ONSET_TEST);
        onsetEvent.setRequestId(requestId);
        onsetEvent.setTarget(VNF_ID);
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put(VNF_NAME, ONSET_ONE);

        ControlLoopEventManager manager =
                new ControlLoopEventManager(onsetEvent.getClosedLoopControlName(), onsetEvent.getRequestId());
        VirtualControlLoopNotification notification = manager.activate(yamlString, onsetEvent);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        Policy policy = manager.getProcessor().getCurrentPolicy();
        ControlLoopOperationManager clom = new ControlLoopOperationManager(onsetEvent, policy, manager);
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
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put(VNF_NAME, ONSET_ONE);

        ControlLoopEventManager manager =
                new ControlLoopEventManager(onsetEvent.getClosedLoopControlName(), onsetEvent.getRequestId());
        VirtualControlLoopNotification notification = manager.activate(yamlString, onsetEvent);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        Policy policy = manager.getProcessor().getCurrentPolicy();
        ControlLoopOperationManager clom = new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);

        clom.startOperation(onsetEvent);
        assertTrue(clom.isOperationRunning());

        clom = Serializer.roundTrip(clom);
        assertNotNull(clom);
        assertTrue(clom.isOperationRunning());

        SoResponse soResponse = new SoResponse();
        final SoResponseWrapper soRw = new SoResponseWrapper(soResponse, null);

        PolicyEngine.manager.setEnvironmentProperty("guard.disabled", "false");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.ONAP_KEY_URL,
                "http://somewhere.over.the.rainbow");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.ONAP_KEY_USER, DOROTHY);
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.ONAP_KEY_PASS, "Toto");

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
