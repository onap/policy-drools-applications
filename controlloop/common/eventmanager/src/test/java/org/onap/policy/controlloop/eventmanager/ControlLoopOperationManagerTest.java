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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
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
import org.onap.policy.common.endpoints.http.server.HttpServletServer;
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
    private static final Logger logger = LoggerFactory.getLogger(ControlLoopOperationManagerTest.class);


    private static VirtualControlLoopEvent onset;

    static {
        onset = new VirtualControlLoopEvent();
        onset.setRequestId(UUID.randomUUID());
        onset.setTarget("generic-vnf.vnf-name");
        onset.setTargetType(ControlLoopTargetType.VNF);
        onset.setClosedLoopAlarmStart(Instant.now());
        onset.setAai(new HashMap<>());
        onset.getAai().put("generic-vnf.vnf-name", "testTriggerSource");
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
        String sql = "select count(*) as count from operationshistory10";
        Query nq = em.createNativeQuery(sql);

        int numEvents = -1;
        try {
            numEvents = ((Number) nq.getSingleResult()).intValue();
        } catch (NoResultException | NonUniqueResultException ex) {
            logger.error("getCountFromDb threw: ", ex);
            fail(ex.getMessage());
        }
        return numEvents;
    }

    
    /**
     * Set up test class.
     */
    @BeforeClass
    public static void setUp() {

        try {
            org.onap.policy.simulators.Util.buildAaiSim();
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        // Set PU
        System.setProperty("OperationsHistoryPU", "TestOperationsHistoryPU");

        // Enter dummy props to avoid nullPointerException
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.ONAP_KEY_URL, "a");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.ONAP_KEY_USER, "b");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.ONAP_KEY_PASS, "c");

        // Connect to in-mem db
        emf = Persistence.createEntityManagerFactory("TestOperationsHistoryPU");
        em = emf.createEntityManager();
    }


    /**
     * Clean up test class.
     */
    @AfterClass
    public static void tearDown() {
        em.close();
        emf.close();
        HttpServletServer.factory.destroy();
    }
    
    @Test
    public void testRetriesFail() {
        //
        // Load up the policy
        //
        final SupportUtil.Pair<ControlLoopPolicy, String> pair = SupportUtil.loadYaml("src/test/resources/test.yaml");
        onset.setClosedLoopControlName(pair.key.getControlLoop().getControlLoopName());
        try {
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

            ControlLoopEventManager.NewEventStatus status = null;
            try {
                status = eventManager.onNewEvent(onset);
            } catch (AaiException e) {
                logger.warn(e.toString());
                fail("A&AI Query Failed");
            }
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
            appcResponse.getStatus().setMessage("ACCEPT");
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
            appcResponse.getStatus().setMessage("AppC failed for some reason");
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
            appcResponse.getStatus().setMessage("ACCEPT");
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
            appcResponse.getStatus().setMessage("AppC failed for some reason");
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
        } catch (ControlLoopException | AaiException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testTimeout() {
        //
        // Load up the policy
        //
        final SupportUtil.Pair<ControlLoopPolicy, String> pair = SupportUtil.loadYaml("src/test/resources/test.yaml");
        onset.setClosedLoopControlName(pair.key.getControlLoop().getControlLoopName());
        try {
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

            ControlLoopEventManager.NewEventStatus status = null;
            try {
                status = eventManager.onNewEvent(onset);
            } catch (AaiException e) {
                logger.warn(e.toString());
                fail("A&AI Query Failed");
            }
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
            appcResponse.getStatus().setMessage("ACCEPT");
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
            appcResponse.getStatus().setMessage("AppC failed for some reason");
            dmaapResponse.setBody(appcResponse);
            result = manager.onResponse(dmaapResponse);
            logger.debug("{}", manager);
            //
            //
            //
            assertTrue(manager.isOperationComplete());
            assertFalse(manager.isOperationRunning());
            assertTrue(manager.getHistory().size() == 1);
            assertTrue(manager.getOperationResult().equals(PolicyResult.FAILURE_TIMEOUT));
        } catch (ControlLoopException | AaiException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testMethods() throws IOException, ControlLoopException, AaiException {
        InputStream is = new FileInputStream(new File("src/test/resources/testSOactor.yaml"));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
        onsetEvent.setClosedLoopControlName("TwoOnsetTest");
        onsetEvent.setRequestId(requestId);
        onsetEvent.setTarget("generic-vnf.vnf-id");
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put("generic-vnf.vnf-name", "onsetOne");

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
        try {
            clom.getTarget(policy);
            fail("test should throw an exception here");
        } catch (Exception e) {
            assertEquals("The target is null", e.getMessage());
        }

        policy.setTarget(new Target());
        try {
            clom.getTarget(policy);
            fail("test should throw an exception here");
        } catch (Exception e) {
            assertEquals("The target type is null", e.getMessage());
        }

        policy.setTarget(savedTarget);

        policy.getTarget().setType(TargetType.PNF);
        try {
            clom.getTarget(policy);
            fail("test should throw an exception here");
        } catch (Exception e) {
            assertEquals("PNF target is not supported", e.getMessage());
        }

        onsetEvent.setTarget("Oz");
        onsetEvent.getAai().remove("generic-vnf.vnf-name");
        onsetEvent.getAai().remove("generic-vnf.vnf-id");
        onsetEvent.getAai().remove("vserver.vserver-name");

        policy.getTarget().setType(TargetType.VNF);
        try {
            clom.getTarget(policy);
            fail("test should throw an exception here");
        } catch (Exception e) {
            assertEquals("Target does not match target type", e.getMessage());
        }

        onsetEvent.setTarget("vserver.vserver-name");
        onsetEvent.getAai().put("vserver.vserver-name", "OzVServer");
        assertEquals("OzVServer", clom.getTarget(policy));

        onsetEvent.getAai().remove("vserver.vserver-name");
        onsetEvent.setTarget("generic-vnf.vnf-id");
        onsetEvent.getAai().put("generic-vnf.vnf-id", "OzVNF");
        assertEquals("OzVNF", clom.getTarget(policy));

        onsetEvent.setTarget("generic-vnf.vnf-name");
        assertEquals("OzVNF", clom.getTarget(policy));

        manager.onNewEvent(onsetEvent);

        onsetEvent.getAai().remove("generic-vnf.vnf-id");
        manager.getVnfResponse();
        clom.getEventManager().getVnfResponse().setVnfId("generic-vnf.vnf-id");
        assertEquals("generic-vnf.vnf-id", clom.getTarget(policy));

        policy.getTarget().setType(TargetType.VFC);
        try {
            clom.getTarget(policy);
            fail("test should throw an exception here");
        } catch (Exception e) {
            assertEquals("The target type is not supported", e.getMessage());
        }

        assertEquals(Integer.valueOf(20), clom.getOperationTimeout());

        assertEquals("20s", clom.getOperationTimeoutString(100));

        assertEquals(null, clom.getOperationMessage());
        assertEquals(null, clom.getOperationMessage("The Wizard Escaped"));

        clom.startOperation(onsetEvent);

        assertEquals("actor=SO,operation=Restart,target=Target [type=VFC, resourceId=null],subRequestId=1",
                clom.getOperationMessage());
        assertEquals(
                "actor=SO,operation=Restart,target=Target [type=VFC, resourceId=null],subRequestId=1, Guard result: "
                        + "The Wizard Escaped",
                clom.getOperationMessage("The Wizard Escaped"));

        assertEquals("actor=SO,operation=Restart,tar", clom.getOperationHistory().substring(0, 30));

        clom.setOperationHasException("The Wizard is gone");
        clom.setOperationHasGuardDeny();
    }

    @Test
    public void testConstructor() throws IOException, ControlLoopException, AaiException {
        InputStream is = new FileInputStream(new File("src/test/resources/test.yaml"));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
        onsetEvent.setClosedLoopControlName("TwoOnsetTest");
        onsetEvent.setRequestId(requestId);
        onsetEvent.setTarget("generic-vnf.vnf-id");
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put("generic-vnf.vnf-name", "onsetOne");

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
        try {
            new ControlLoopOperationManager(onsetEvent, policy, manager);
            fail("test should throw an exception here");
        } catch (Exception e) {
            assertEquals("Target vnf-id could not be found", e.getMessage());
        }

        policy.getTarget().setResourceID("82194af1-3c2c-485a-8f44-420e22a9eaa4");
        clom = new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);

        policy.setActor("SO");
        clom = new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);

        policy.setActor("VFC");
        clom = new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);

        policy.setActor("Dorothy");
        try {
            new ControlLoopOperationManager(onsetEvent, policy, manager);
            fail("test should throw an exception here");
        } catch (Exception e) {
            assertEquals("ControlLoopEventManager: policy has an unknown actor.", e.getMessage());
        }
    }

    @Test
    public void testStartOperation() throws IOException, ControlLoopException, AaiException {
        InputStream is = new FileInputStream(new File("src/test/resources/test.yaml"));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
        onsetEvent.setClosedLoopControlName("TwoOnsetTest");
        onsetEvent.setRequestId(requestId);
        onsetEvent.setTarget("generic-vnf.vnf-id");
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put("generic-vnf.vnf-name", "onsetOne");

        ControlLoopEventManager manager =
                new ControlLoopEventManager(onsetEvent.getClosedLoopControlName(), onsetEvent.getRequestId());
        VirtualControlLoopNotification notification = manager.activate(yamlString, onsetEvent);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        Policy policy = manager.getProcessor().getCurrentPolicy();
        ControlLoopOperationManager clom = new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);

        clom.startOperation(onsetEvent);

        try {
            clom.startOperation(onsetEvent);
            fail("test should throw an exception here");
        } catch (Exception e) {
            assertEquals("current operation is not null (an operation is already running)", e.getMessage());
        }

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
        try {
            clom.startOperation(onsetEvent);
            fail("test should throw an exception here");
        } catch (Exception e) {
            assertEquals("current operation failed and retries are not allowed", e.getMessage());
        }

        policy.setRetry(0);
        clom = new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);
        clom.startOperation(onsetEvent);
        clom.setOperationHasTimedOut();
        assertTrue(clom.isOperationComplete());
        try {
            clom.startOperation(onsetEvent);
            fail("test should throw an exception here");
        } catch (Exception e) {
            assertEquals("current operation failed and retries are not allowed", e.getMessage());
        }

        policy.setRetry(1);
        clom = new ControlLoopOperationManager(onsetEvent, policy, manager);
        assertNotNull(clom);
        clom.startOperation(onsetEvent);
        clom.setOperationHasTimedOut();
        clom.startOperation(onsetEvent);
        clom.setOperationHasTimedOut();
        assertTrue(clom.isOperationComplete());
        try {
            clom.startOperation(onsetEvent);
            fail("test should throw an exception here");
        } catch (Exception e) {
            assertEquals("current oepration has failed after 2 retries", e.getMessage());
        }

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
        try {
            clom.startOperation(onsetEvent);
            fail("test should throw an exception here");
        } catch (Exception e) {
            assertEquals("invalid actor Oz on policy", e.getMessage());
        }
    }

    @Test
    public void testOnResponse() throws IOException, ControlLoopException, AaiException {
        InputStream is = new FileInputStream(new File("src/test/resources/test.yaml"));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
        onsetEvent.setClosedLoopControlName("TwoOnsetTest");
        onsetEvent.setRequestId(requestId);
        onsetEvent.setTarget("generic-vnf.vnf-id");
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put("generic-vnf.vnf-name", "onsetOne");

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
        InputStream is = new FileInputStream(new File("src/test/resources/test.yaml"));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
        onsetEvent.setClosedLoopControlName("TwoOnsetTest");
        onsetEvent.setRequestId(requestId);
        onsetEvent.setTarget("generic-vnf.vnf-id");
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put("generic-vnf.vnf-name", "onsetOne");

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
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.ONAP_KEY_USER, "Dorothy");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.ONAP_KEY_PASS, "Toto");

        assertEquals(PolicyResult.FAILURE, clom.onResponse(soRw));

        System.setProperty("OperationsHistoryPU", "TestOperationsHistoryPU");
        assertEquals(PolicyResult.FAILURE, clom.onResponse(soRw));
    }    

    @Test
    public void testCommitAbatement() throws ControlLoopException, AaiException, IOException {

        String yamlString = null;
        try ( InputStream is = new FileInputStream(new File("src/test/resources/test.yaml")) ) {
            yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
        onsetEvent.setClosedLoopControlName("TwoOnsetTest");
        onsetEvent.setRequestId(requestId);
        onsetEvent.setTarget("generic-vnf.vnf-id");
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put("generic-vnf.vnf-name", "onsetOne");

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
        
        clom.commitAbatement("Test message","TEST_RESULT");

        int numEventsAfter = getCount();
        logger.info("numEventsAfter={}", numEventsAfter); 
        
        assertEquals(1, numEventsAfter - numEventsBefore);        
    }    

    @Test
    public void testSerialization() throws Exception {
        InputStream is = new FileInputStream(new File("src/test/resources/test.yaml"));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
        onsetEvent.setClosedLoopControlName("TwoOnsetTest");
        onsetEvent.setRequestId(requestId);
        onsetEvent.setTarget("generic-vnf.vnf-id");
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put("generic-vnf.vnf-name", "onsetOne");

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
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.ONAP_KEY_USER, "Dorothy");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.ONAP_KEY_PASS, "Toto");

        assertEquals(PolicyResult.FAILURE, clom.onResponse(soRw));
        assertFalse(clom.isOperationRunning());
        assertEquals(1, clom.getHistory().size());
        
        clom = Serializer.roundTrip(clom);
        assertNotNull(clom);
        assertFalse(clom.isOperationRunning());
        assertEquals(1, clom.getHistory().size());

        System.setProperty("OperationsHistoryPU", "TestOperationsHistoryPU");
        assertEquals(PolicyResult.FAILURE, clom.onResponse(soRw));
        
        clom = Serializer.roundTrip(clom);
        assertNotNull(clom);
        assertFalse(clom.isOperationRunning());
        assertEquals(1, clom.getHistory().size());
    }
}
