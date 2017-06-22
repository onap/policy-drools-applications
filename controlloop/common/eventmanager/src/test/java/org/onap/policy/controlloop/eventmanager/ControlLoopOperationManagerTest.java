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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import org.junit.Test;
import org.onap.policy.appc.Request;
import org.onap.policy.appc.Response;
import org.onap.policy.appc.ResponseCode;
import org.onap.policy.appc.ResponseValue;
import org.onap.policy.controlloop.ControlLoopEventStatus;

import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.Util;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.controlloop.processor.ControlLoopProcessor;

public class ControlLoopOperationManagerTest {
	
	private static VirtualControlLoopEvent onset;
	static {
		onset = new VirtualControlLoopEvent();
		onset.requestID = UUID.randomUUID();
		onset.target = "vserver.selflink";
		onset.closedLoopAlarmStart = Instant.now();
		onset.AAI = new HashMap<String, String>();
		onset.AAI.put("cloud-region.identity-url", "foo");
		onset.AAI.put("vserver.selflink", "bar");
		onset.AAI.put("vserver.is-closed-loop-disabled", "false");
		onset.AAI.put("generic-vnf.vnf-name", "testTriggerSource");
		onset.closedLoopEventStatus = ControlLoopEventStatus.ONSET;
	}

	@Test
	public void testRetriesFail() {
		//
		// Load up the policy
		//
		final Util.Pair<ControlLoopPolicy, String> pair = Util.loadYaml("src/test/resources/test.yaml");
		onset.closedLoopControlName = pair.a.controlLoop.controlLoopName;
		try {
			//
			// Create a processor
			//
			ControlLoopProcessor processor = new ControlLoopProcessor(pair.b);
			//
			// create the manager
			//
			ControlLoopEventManager eventManager = new ControlLoopEventManager(onset.closedLoopControlName, onset.requestID);

			ControlLoopOperationManager manager = new ControlLoopOperationManager(onset, processor.getCurrentPolicy(), eventManager);
			System.out.println(manager);
			//
			//
			//
			assertFalse(manager.isOperationComplete());
			assertFalse(manager.isOperationRunning());
			//
			// Start
			//
			Object request = manager.startOperation(onset);
			System.out.println(manager);
			assertNotNull(request);
			assertTrue(request instanceof Request);
			assertTrue(((Request)request).CommonHeader.SubRequestID.contentEquals("1"));
			assertFalse(manager.isOperationComplete());
			assertTrue(manager.isOperationRunning());
			//
			// Accept
			//
			Response response = new Response((Request) request);
			response.Status.Code = ResponseCode.ACCEPT.getValue();
			response.Status.Value = ResponseValue.ACCEPT.toString();
			//
			//
			//
			PolicyResult result = manager.onResponse(response);
			System.out.println(manager);
			assertTrue(result == null);
			assertFalse(manager.isOperationComplete());
			assertTrue(manager.isOperationRunning());
			//
			// Now we are going to Fail it
			//
			response = new Response((Request) request);
			response.Status.Code = ResponseCode.FAILURE.getValue();
			response.Status.Value = ResponseValue.FAILURE.toString();
			response.Status.Description = "AppC failed for some reason";
			result = manager.onResponse(response);
			System.out.println(manager);
			assertTrue(result.equals(PolicyResult.FAILURE));
			assertFalse(manager.isOperationComplete());
			assertFalse(manager.isOperationRunning());
			//
			// Retry it
			//
			request = manager.startOperation(onset);
			System.out.println(manager);
			assertNotNull(request);
			assertTrue(request instanceof Request);
			assertTrue(((Request)request).CommonHeader.SubRequestID.contentEquals("2"));
			assertFalse(manager.isOperationComplete());
			assertTrue(manager.isOperationRunning());
			//
			// 
			//
			response = new Response((Request) request);
			System.out.println(manager);
			response.Status.Code = ResponseCode.ACCEPT.getValue();
			response.Status.Value = ResponseValue.ACCEPT.toString();
			//
			//
			//
			result = manager.onResponse(response);
			System.out.println(manager);
			assertTrue(result == null);
			assertFalse(manager.isOperationComplete());
			assertTrue(manager.isOperationRunning());
			//
			// Now we are going to Fail it
			//
			response = new Response((Request) request);
			response.Status.Code = ResponseCode.FAILURE.getValue();
			response.Status.Value = ResponseValue.FAILURE.toString();
			response.Status.Description = "AppC failed for some reason";
			result = manager.onResponse(response);
			System.out.println(manager);
			assertTrue(result.equals(PolicyResult.FAILURE));
			//
			// Should be complete now
			//
			assertTrue(manager.isOperationComplete());
			assertFalse(manager.isOperationRunning());
			assertNotNull(manager.getOperationResult());
			assertTrue(manager.getOperationResult().equals(PolicyResult.FAILURE_RETRIES));
			assertTrue(manager.getHistory().size() == 2);
		} catch (ControlLoopException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testTimeout() {
		//
		// Load up the policy
		//
		final Util.Pair<ControlLoopPolicy, String> pair = Util.loadYaml("src/test/resources/test.yaml");
		onset.closedLoopControlName = pair.a.controlLoop.controlLoopName;
		try {
			//
			// Create a processor
			//
			ControlLoopProcessor processor = new ControlLoopProcessor(pair.b);
			//
			// create the manager
			//
			ControlLoopEventManager eventManager = new ControlLoopEventManager(onset.closedLoopControlName, onset.requestID);

			ControlLoopOperationManager manager = new ControlLoopOperationManager(onset, processor.getCurrentPolicy(), eventManager);
			//
			//
			//
			System.out.println(manager);
			assertFalse(manager.isOperationComplete());
			assertFalse(manager.isOperationRunning());
			//
			// Start
			//
			Object request = manager.startOperation(onset);
			System.out.println(manager);
			assertNotNull(request);
			assertTrue(request instanceof Request);
			assertTrue(((Request)request).CommonHeader.SubRequestID.contentEquals("1"));
			assertFalse(manager.isOperationComplete());
			assertTrue(manager.isOperationRunning());
			//
			// Accept
			//
			Response response = new Response((Request) request);
			response.Status.Code = ResponseCode.ACCEPT.getValue();
			response.Status.Value = ResponseValue.ACCEPT.toString();
			//
			//
			//
			PolicyResult result = manager.onResponse(response);
			System.out.println(manager);
			assertTrue(result == null);
			assertFalse(manager.isOperationComplete());
			assertTrue(manager.isOperationRunning());
			//
			// Now we are going to simulate Timeout
			//
			manager.setOperationHasTimedOut();
			System.out.println(manager);
			assertTrue(manager.isOperationComplete());
			assertFalse(manager.isOperationRunning());
			assertTrue(manager.getHistory().size() == 1);
			assertTrue(manager.getOperationResult().equals(PolicyResult.FAILURE_TIMEOUT));
			//
			// Now we are going to Fail the previous request
			//
			response = new Response((Request) request);
			response.Status.Code = ResponseCode.FAILURE.getValue();
			response.Status.Value = ResponseValue.FAILURE.toString();
			response.Status.Description = "AppC failed for some reason";
			result = manager.onResponse(response);
			System.out.println(manager);
			//
			//
			//
			assertTrue(manager.isOperationComplete());
			assertFalse(manager.isOperationRunning());
			assertTrue(manager.getHistory().size() == 1);
			assertTrue(manager.getOperationResult().equals(PolicyResult.FAILURE_TIMEOUT));
		} catch (ControlLoopException e) {
			fail(e.getMessage());
		}
	}

}
