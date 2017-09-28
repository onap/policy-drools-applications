/*-
 * ============LICENSE_START=======================================================
 * demo
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

package org.onap.policy.template.demo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.onap.policy.appclcm.LCMRequest;
import org.onap.policy.appclcm.LCMRequestWrapper;
import org.onap.policy.appclcm.LCMResponse;
import org.onap.policy.appclcm.LCMResponseWrapper;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.drools.http.server.HttpServletServer;
import org.onap.policy.drools.impl.PolicyEngineJUnitImpl;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.guard.PolicyGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class ControlLoopXacmlGuardTest {
    @BeforeClass
    public static void setUpSimulator() {
        try {
            Util.buildAaiSim();
            Util.buildGuardSim();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @AfterClass
    public static void tearDownSimulator() {
        HttpServletServer.factory.destroy();
    }

	private static final Logger logger = LoggerFactory.getLogger(ControlLoopXacmlGuardTest.class);
	private static final String OPSHISTPUPROP = "OperationsHistoryPU";
	@BeforeClass
	public static void setPUProp(){
		System.setProperty(OPSHISTPUPROP, "TestOperationsHistoryPU");
		Util.setGuardProps();
	}
	@AfterClass
	public static void restorePUProp(){
		System.setProperty(OPSHISTPUPROP, OPSHISTPUPROP);
	}


	@Test
	public void test() {
		try {
			this.runTest("src/main/resources/ControlLoop_Template_xacml_guard.drl",
					"src/test/resources/yaml/policy_ControlLoop_Service123.yaml",
					"service=Service123;resource=Res123;type=operational",
					"CL_SERV123_8888",
					"org.onap.closed_loop.Service123:VNFS:0.0.1");
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void runTest(String droolsTemplate,
			String yamlFile,
			String policyScope,
			String policyName,
			String policyVersion) throws IOException {
		//
		// Pull info from the yaml
		//
		final Util.Pair<ControlLoopPolicy, String> pair = Util.loadYaml(yamlFile);
		assertNotNull(pair);
		assertNotNull(pair.a);
		assertNotNull(pair.a.getControlLoop());
		assertNotNull(pair.a.getControlLoop().getControlLoopName());
		assertTrue(pair.a.getControlLoop().getControlLoopName().length() > 0);
		//
		// Build a container
		//
		final KieSession kieSession = Util.buildContainer(droolsTemplate,
				pair.a.getControlLoop().getControlLoopName(),
				policyScope,
				policyName,
				policyVersion,
				URLEncoder.encode(pair.b, "UTF-8"));



		logger.debug("============");
		logger.debug(URLEncoder.encode(pair.b, "UTF-8"));
		logger.debug("============");

		final PolicyEngineJUnitImpl engine = (PolicyEngineJUnitImpl) kieSession.getGlobal("Engine");

		//
		// Initial fire of rules
		//
		kieSession.fireAllRules();
		//
		// Kick a thread that starts testing
		//
		new Thread(new Runnable() {


			@Override
			public void run() {
				try {


					//
					// Let's use a unique ID for the request and
					// a unique trigger source.
					//
					UUID requestID = UUID.randomUUID();
					String triggerSourceName = "foobartriggersource36";

					Object obj = null;

					sendGoodEvents(kieSession, pair.a, requestID, triggerSourceName);
					obj = engine.subscribe("UEB", "POLICY-CL-MGT");
					assertNotNull(obj);
					assertTrue(obj instanceof VirtualControlLoopNotification);
					assertTrue(((VirtualControlLoopNotification)obj).notification.equals(ControlLoopNotificationType.ACTIVE));
					//
					// Give the control loop a little time to acquire the lock and publish the request
					//
					Thread.sleep(4000);


					// "About to query Guard" notification (Querying about Restart)
					obj = engine.subscribe("UEB", "POLICY-CL-MGT");
					assertNotNull(obj);
					logger.debug("\n\n####################### GOING TO QUERY GUARD about Restart!!!!!!");
					logger.debug("Rule: {} Message {}", ((VirtualControlLoopNotification)obj).policyName, ((VirtualControlLoopNotification)obj).message);
					assertTrue(obj instanceof VirtualControlLoopNotification);
					assertTrue(((VirtualControlLoopNotification)obj).notification.equals(ControlLoopNotificationType.OPERATION));

					Thread.sleep(2*4000);
					// "Response from Guard" notification
					obj = engine.subscribe("UEB", "POLICY-CL-MGT");
					assertNotNull(obj);
					logger.debug("Rule: {} Message {}", ((VirtualControlLoopNotification)obj).policyName, ((VirtualControlLoopNotification)obj).message);
					assertTrue(obj instanceof VirtualControlLoopNotification);
					assertTrue(((VirtualControlLoopNotification)obj).notification.equals(ControlLoopNotificationType.OPERATION));


					if(true == ((VirtualControlLoopNotification)obj).message.contains("Guard result: Deny")){

						// "About to query Guard" notification (Querying about Rebuild)
						obj = engine.subscribe("UEB", "POLICY-CL-MGT");
						assertNotNull(obj);
						logger.debug("\n\n####################### GOING TO QUERY GUARD about Rebuild!!!!!!");
						logger.debug("Rule: {} Message", ((VirtualControlLoopNotification)obj).policyName, ((VirtualControlLoopNotification)obj).message);
						assertTrue(obj instanceof VirtualControlLoopNotification);
						assertTrue(((VirtualControlLoopNotification)obj).notification.equals(ControlLoopNotificationType.OPERATION));

						Thread.sleep(4000);

						// "Response from Guard" notification
						obj = engine.subscribe("UEB", "POLICY-CL-MGT");
						assertNotNull(obj);
						logger.debug("Rule: {} Message {}", ((VirtualControlLoopNotification)obj).policyName, ((VirtualControlLoopNotification)obj).message);
						assertTrue(obj instanceof VirtualControlLoopNotification);
						assertTrue(((VirtualControlLoopNotification)obj).notification.equals(ControlLoopNotificationType.OPERATION));


						if(true == ((VirtualControlLoopNotification)obj).message.contains("Guard result: Deny")){

							// "About to query Guard" notification (Querying about Migrate)
							obj = engine.subscribe("UEB", "POLICY-CL-MGT");
							assertNotNull(obj);
							logger.debug("\n\n####################### GOING TO QUERY GUARD!!!!!!");
							logger.debug("Rule: {} Message {}", ((VirtualControlLoopNotification)obj).policyName, ((VirtualControlLoopNotification)obj).message);
							assertTrue(obj instanceof VirtualControlLoopNotification);
							assertTrue(((VirtualControlLoopNotification)obj).notification.equals(ControlLoopNotificationType.OPERATION));

							Thread.sleep(2*4000);

							// "Response from Guard" notification
							obj = engine.subscribe("UEB", "POLICY-CL-MGT");
							assertNotNull(obj);
							logger.debug("Rule: " + ((VirtualControlLoopNotification)obj).policyName +" Message: " + ((VirtualControlLoopNotification)obj).message);
							assertTrue(obj instanceof VirtualControlLoopNotification);
							assertTrue(((VirtualControlLoopNotification)obj).notification.equals(ControlLoopNotificationType.OPERATION));


							if(true == ((VirtualControlLoopNotification)obj).message.contains("Guard result: Deny")){
								//All the 3 operations were Denied by Guard
								Thread.sleep(60000);

							}
						}
					}

					//
					// In case one of the operations was permitted by Guard
					//
					if(true == ((VirtualControlLoopNotification)obj).message.contains("Guard result: Permit")){
						obj = engine.subscribe("UEB", "POLICY-CL-MGT");
						assertNotNull(obj);
						logger.debug("Rule: {} Message {}", ((VirtualControlLoopNotification)obj).policyName, ((VirtualControlLoopNotification)obj).message);
						assertTrue(obj instanceof VirtualControlLoopNotification);
						assertTrue(((VirtualControlLoopNotification)obj).notification.equals(ControlLoopNotificationType.OPERATION));

						Thread.sleep(2*1000);

						obj = engine.subscribe("UEB", "APPC-LCM-READ");
						assertNotNull(obj);
						assertTrue(obj instanceof LCMRequestWrapper);
						LCMRequestWrapper dmaapRequest = (LCMRequestWrapper) obj;
						LCMRequest appcRequest = dmaapRequest.getBody();
						assertTrue(appcRequest.getCommonHeader().getSubRequestId().equals("1"));

						logger.debug("\n============ APP-C Got request!!! ===========\n");
						//
						// Ok - let's simulate ACCEPT
						//

						//
						// now wait for it to finish
						//
						Thread.sleep(1000);

						//
						// Now we are going to success it
						//
						LCMResponseWrapper dmaapResponse = new LCMResponseWrapper();
						LCMResponse appcResponse = new LCMResponse(appcRequest);
						appcResponse.getStatus().setCode(400);
						appcResponse.getStatus().setMessage("AppC success");
						dmaapResponse.setBody(appcResponse);
						kieSession.insert(dmaapResponse);
						//
						// Give it some time to process
						//
						Thread.sleep(4000);
						//
						// Insert the abatement event
						//
						sendAbatement(kieSession, pair.a, requestID, triggerSourceName);
						//
						// now wait for it to finish
						//
						Thread.sleep(2*15000);
						//
						// Ensure they released the lock
						//
						assertFalse(PolicyGuard.isLocked(TargetType.VM, triggerSourceName, requestID));

					}



				} catch (InterruptedException e) {
					logger.error("Test thread got InterruptedException ", e.getLocalizedMessage());
				} catch (AssertionError e) {
					logger.error("Test thread got AssertionError ", e.getLocalizedMessage());
					e.printStackTrace();
				} catch (Exception e) {
					logger.error("Test thread got Exception ", e.getLocalizedMessage());
					e.printStackTrace();
				}
				kieSession.halt();
			}

		}).start();
		//
		// Start firing rules
		//
		kieSession.fireUntilHalt();
		//
		// Dump working memory
		//
		dumpFacts(kieSession);
		//
		// See if there is anything left in memory
		//
	    // assertEquals(1, kieSession.getFactCount());
	    if (kieSession.getFactCount() != 1L) {
	      logger.error("FACT count mismatch: 1 expected but there are {}", kieSession.getFactCount());
	    }

	    for (final FactHandle handle : kieSession.getFactHandles()) {
	      final Object fact = kieSession.getObject(handle);
	      // assertEquals("", "org.onap.policy.controlloop.Params", fact.getClass().getName());
	      logger.info("Working Memory FACT: {}", fact.getClass().getName());
	    }
		kieSession.dispose();
	}




	public static void dumpFacts(KieSession kieSession) {
		logger.debug("Fact Count: {}", kieSession.getFactCount());
		for (FactHandle handle : kieSession.getFactHandles()) {
			logger.debug("FACT: {}", handle);
		}
	}

	protected void sendAbatement(KieSession kieSession, ControlLoopPolicy policy, UUID requestID, String triggerSourceName) throws InterruptedException {
		VirtualControlLoopEvent event = new VirtualControlLoopEvent();
		event.closedLoopControlName = policy.getControlLoop().getControlLoopName();
		event.requestID = requestID;
		event.target = "vserver.vserver-name";
		event.closedLoopAlarmStart = Instant.now().minusSeconds(5);
		event.closedLoopAlarmEnd = Instant.now();
		event.AAI = new HashMap<>();
		event.AAI.put("cloud-region.identity-url", "foo");
		event.AAI.put("vserver.selflink", "bar");
		event.AAI.put("vserver.is-closed-loop-disabled", "false");
		event.AAI.put("generic-vnf.vnf-name", "testGenericVnfName");
		event.closedLoopEventStatus = ControlLoopEventStatus.ABATED;
		kieSession.insert(event);
	}

	protected void sendGoodEvents(KieSession kieSession, ControlLoopPolicy policy, UUID requestID, String triggerSourceName) throws InterruptedException {
		VirtualControlLoopEvent event = new VirtualControlLoopEvent();
		event.closedLoopControlName = policy.getControlLoop().getControlLoopName();
		event.requestID = requestID;
		event.target = "vserver.vserver-name";
		event.closedLoopAlarmStart = Instant.now();
		event.AAI = new HashMap<>();
		event.AAI.put("cloud-region.identity-url", "foo");
		event.AAI.put("vserver.selflink", "bar");
		event.AAI.put("vserver.is-closed-loop-disabled", "false");
		event.AAI.put("vserver.vserver-name", "testGenericVnfName");
		event.closedLoopEventStatus = ControlLoopEventStatus.ONSET;
		kieSession.insert(event);
		Thread.sleep(2000);

	}

	protected void sendBadEvents(KieSession kieSession, ControlLoopPolicy policy, UUID requestID, String triggerSourceName) throws InterruptedException {
		//
		// Insert a bad Event
		//
		VirtualControlLoopEvent event = new VirtualControlLoopEvent();
		event.closedLoopControlName = policy.getControlLoop().getControlLoopName();
		kieSession.insert(event);
		Thread.sleep(500);
		//
		// add the request id
		//
		event.requestID = requestID;
		kieSession.insert(event);
		Thread.sleep(500);
		//
		// add some aai
		//
		event.AAI = new HashMap<>();
		event.AAI.put("cloud-region.identity-url", "foo");
		event.AAI.put("vserver.selflink", "bar");
		event.AAI.put("vserver.vserver-name", "vmfoo");
		kieSession.insert(event);
		Thread.sleep(500);
		//
		// set a valid status
		//
		event.closedLoopEventStatus = ControlLoopEventStatus.ONSET;
		kieSession.insert(event);
		Thread.sleep(500);
		//
		// add a trigger sourcename
		//
		kieSession.insert(event);
		Thread.sleep(500);
		//
		// add is closed-loop-disabled
		//
		event.AAI.put("vserver.is-closed-loop-disabled", "true");
		kieSession.insert(event);
		Thread.sleep(500);
		//
		// now enable
		//
		event.AAI.put("vserver.is-closed-loop-disabled", "false");
		kieSession.insert(event);
		Thread.sleep(500);
		//
		// Add target, but bad.
		//
		event.target = "VM_BLAH";
		kieSession.insert(event);
		Thread.sleep(500);
	}
}
