/*-
 * ============LICENSE_START=======================================================
 * unit test
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

package org.onap.policy.controlloop.eventmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.aai.util.AAIException;
import org.onap.policy.appclcm.LCMRequest;
import org.onap.policy.appclcm.LCMRequestWrapper;
import org.onap.policy.appclcm.LCMResponse;
import org.onap.policy.appclcm.LCMResponseWrapper;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopTargetType;
import org.onap.policy.controlloop.Util;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.controlloop.processor.ControlLoopProcessor;
import org.onap.policy.drools.http.server.HttpServletServer;
import org.onap.policy.drools.system.PolicyEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlLoopOperationManagerTest {
	private static final Logger logger = LoggerFactory.getLogger(ControlLoopOperationManagerTest.class);
	private static VirtualControlLoopEvent onset;
	static {
		onset = new VirtualControlLoopEvent();
		onset.requestID = UUID.randomUUID();
		onset.target = "generic-vnf.vnf-name";
		onset.target_type = ControlLoopTargetType.VNF;
		onset.closedLoopAlarmStart = Instant.now();
		onset.AAI = new HashMap<>();
		onset.AAI.put("generic-vnf.vnf-name", "testTriggerSource");
		onset.closedLoopEventStatus = ControlLoopEventStatus.ONSET;
		
		/* Set environment properties */
        PolicyEngine.manager.setEnvironmentProperty("aai.url", "http://localhost:6666");
        PolicyEngine.manager.setEnvironmentProperty("aai.username", "AAI");
        PolicyEngine.manager.setEnvironmentProperty("aai.password", "AAI");
	}

	@BeforeClass
    public static void setUpSimulator() {
        try {
            org.onap.policy.simulators.Util.buildAaiSim();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @AfterClass
    public static void tearDownSimulator() {
        HttpServletServer.factory.destroy();
    }
	
	@Test
	public void testRetriesFail() {
		//
		// Load up the policy
		//
		final Util.Pair<ControlLoopPolicy, String> pair = Util.loadYaml("src/test/resources/test.yaml");
		onset.closedLoopControlName = pair.a.getControlLoop().getControlLoopName();
		try {
			//
			// Create a processor
			//
			ControlLoopProcessor processor = new ControlLoopProcessor(pair.b);
			//
			// create the manager
			//
			ControlLoopEventManager eventManager = new ControlLoopEventManager(onset.closedLoopControlName, onset.requestID);
            VirtualControlLoopNotification notification = eventManager.activate(onset);
			
			assertNotNull(notification);
			assertEquals(ControlLoopNotificationType.ACTIVE, notification.notification);
			
			ControlLoopEventManager.NEW_EVENT_STATUS status = null;
	        try {
	            status = eventManager.onNewEvent(onset);
	        } catch (AAIException e) {
	            logger.warn(e.toString());
	            fail("A&AI Query Failed");
	        }
	        assertNotNull(status);
	        assertEquals(ControlLoopEventManager.NEW_EVENT_STATUS.FIRST_ONSET, status);
			
			ControlLoopOperationManager manager = new ControlLoopOperationManager(onset, processor.getCurrentPolicy(), eventManager);
			logger.debug("{}",manager);
			//
			//
			//
			assertFalse(manager.isOperationComplete());
			assertFalse(manager.isOperationRunning());
			//
			// Start
			//
			Object request = manager.startOperation(onset);
			logger.debug("{}",manager);
			assertNotNull(request);
			assertTrue(request instanceof LCMRequestWrapper);
			LCMRequestWrapper dmaapRequest = (LCMRequestWrapper) request;
			LCMRequest appcRequest = dmaapRequest.getBody();
			assertTrue(appcRequest.getCommonHeader().getSubRequestId().contentEquals("1"));
			assertFalse(manager.isOperationComplete());
			assertTrue(manager.isOperationRunning());
			//
			// Accept
			//
			LCMResponseWrapper dmaapResponse = new LCMResponseWrapper();
			LCMResponse appcResponse = new LCMResponse((LCMRequest) appcRequest);
			appcResponse.getStatus().setCode(100);
			appcResponse.getStatus().setMessage("ACCEPT");
			dmaapResponse.setBody(appcResponse);
			//
			//
			//
			PolicyResult result = manager.onResponse(dmaapResponse);
			logger.debug("{}",manager);
			assertTrue(result == null);
			assertFalse(manager.isOperationComplete());
			assertTrue(manager.isOperationRunning());
			//
			// Now we are going to Fail it
			//
			appcResponse = new LCMResponse(appcRequest);
			appcResponse.getStatus().setCode(401);
			appcResponse.getStatus().setMessage("AppC failed for some reason");
			dmaapResponse.setBody(appcResponse);
			result = manager.onResponse(dmaapResponse);
			logger.debug("{}",manager);
			assertTrue(result.equals(PolicyResult.FAILURE));
			assertFalse(manager.isOperationComplete());
			assertFalse(manager.isOperationRunning());
			//
			// Retry it
			//
			request = manager.startOperation(onset);
			logger.debug("{}",manager);
			assertNotNull(request);
			assertTrue(request instanceof LCMRequestWrapper);
			dmaapRequest = (LCMRequestWrapper) request;
			appcRequest = dmaapRequest.getBody();
			assertTrue(appcRequest.getCommonHeader().getSubRequestId().contentEquals("2"));
			assertFalse(manager.isOperationComplete());
			assertTrue(manager.isOperationRunning());
			//
			// 
			//
			appcResponse = new LCMResponse((LCMRequest) appcRequest);
			logger.debug("{}",manager);
			appcResponse.getStatus().setCode(100);
			appcResponse.getStatus().setMessage("ACCEPT");
			dmaapResponse.setBody(appcResponse);
			//
			//
			//
			result = manager.onResponse(dmaapResponse);
			logger.debug("{}",manager);
			assertTrue(result == null);
			assertFalse(manager.isOperationComplete());
			assertTrue(manager.isOperationRunning());
			//
			// Now we are going to Fail it
			//
			appcResponse = new LCMResponse((LCMRequest) appcRequest);
			appcResponse.getStatus().setCode(401);
			appcResponse.getStatus().setMessage("AppC failed for some reason");
			dmaapResponse.setBody(appcResponse);
			result = manager.onResponse(dmaapResponse);
			logger.debug("{}",manager);
			assertTrue(result.equals(PolicyResult.FAILURE));
			//
			// Should be complete now
			//
			assertTrue(manager.isOperationComplete());
			assertFalse(manager.isOperationRunning());
			assertNotNull(manager.getOperationResult());
			assertTrue(manager.getOperationResult().equals(PolicyResult.FAILURE_RETRIES));
			assertTrue(manager.getHistory().size() == 2);
		} catch (ControlLoopException | AAIException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testTimeout() {
		//
		// Load up the policy
		//
		final Util.Pair<ControlLoopPolicy, String> pair = Util.loadYaml("src/test/resources/test.yaml");
		onset.closedLoopControlName = pair.a.getControlLoop().getControlLoopName();
		try {
			//
			// Create a processor
			//
			ControlLoopProcessor processor = new ControlLoopProcessor(pair.b);
			//
			// create the manager
			//
			ControlLoopEventManager eventManager = new ControlLoopEventManager(onset.closedLoopControlName, onset.requestID);
			VirtualControlLoopNotification notification = eventManager.activate(onset);
			
			assertNotNull(notification);
			assertEquals(ControlLoopNotificationType.ACTIVE, notification.notification);

			ControlLoopEventManager.NEW_EVENT_STATUS status = null;
            try {
                status = eventManager.onNewEvent(onset);
            } catch (AAIException e) {
                logger.warn(e.toString());
                fail("A&AI Query Failed");
            }
            assertNotNull(status);
            assertEquals(ControlLoopEventManager.NEW_EVENT_STATUS.FIRST_ONSET, status);
			
			ControlLoopOperationManager manager = new ControlLoopOperationManager(onset, processor.getCurrentPolicy(), eventManager);
			//
			//
			//
			logger.debug("{}",manager);
			assertFalse(manager.isOperationComplete());
			assertFalse(manager.isOperationRunning());
			//
			// Start
			//
			Object request = manager.startOperation(onset);
			logger.debug("{}",manager);
			assertNotNull(request);
			assertTrue((request) instanceof LCMRequestWrapper);
			LCMRequestWrapper dmaapRequest = (LCMRequestWrapper) request;
			LCMRequest appcRequest = dmaapRequest.getBody();
			assertTrue((appcRequest).getCommonHeader().getSubRequestId().contentEquals("1"));
			assertFalse(manager.isOperationComplete());
			assertTrue(manager.isOperationRunning());
			//
			// Accept
			//
			LCMResponseWrapper dmaapResponse = new LCMResponseWrapper();
			LCMResponse appcResponse = new LCMResponse(appcRequest);
	        dmaapResponse.setBody(appcResponse);
			appcResponse.getStatus().setCode(100);
			appcResponse.getStatus().setMessage("ACCEPT");
			//
			//
			//
			PolicyResult result = manager.onResponse(dmaapResponse);
			logger.debug("{}",manager);
			assertTrue(result == null);
			assertFalse(manager.isOperationComplete());
			assertTrue(manager.isOperationRunning());
			//
			// Now we are going to simulate Timeout
			//
			manager.setOperationHasTimedOut();
			logger.debug("{}",manager);
			assertTrue(manager.isOperationComplete());
			assertFalse(manager.isOperationRunning());
			assertTrue(manager.getHistory().size() == 1);
			assertTrue(manager.getOperationResult().equals(PolicyResult.FAILURE_TIMEOUT));
			//
			// Now we are going to Fail the previous request
			//
			appcResponse = new LCMResponse(appcRequest);
			appcResponse.getStatus().setCode(401);
			appcResponse.getStatus().setMessage("AppC failed for some reason");
			dmaapResponse.setBody(appcResponse);
			result = manager.onResponse(dmaapResponse);
			logger.debug("{}",manager);
			//
			//
			//
			assertTrue(manager.isOperationComplete());
			assertFalse(manager.isOperationRunning());
			assertTrue(manager.getHistory().size() == 1);
			assertTrue(manager.getOperationResult().equals(PolicyResult.FAILURE_TIMEOUT));
		} catch (ControlLoopException | AAIException e) {
			fail(e.getMessage());
		}
	}

}
