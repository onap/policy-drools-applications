/*-
 * ============LICENSE_START=======================================================
 * demo
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

package org.onap.policy.template.demo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.appclcm.AppcLcmBody;
import org.onap.policy.appclcm.AppcLcmDmaapWrapper;
import org.onap.policy.appclcm.AppcLcmInput;
import org.onap.policy.appclcm.AppcLcmOutput;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.TopicListener;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.ControlLoopTargetType;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;

public class ControlLoopFailureTest extends ControlLoopBase implements TopicListener {

    private UUID requestId2;
    private UUID requestId3;
    private int eventCount;
    private int nsuccess = 0;
    private int nreject = 0;

    /**
     * Setup simulator.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        ControlLoopBase.setUpBeforeClass(
            "../archetype-cl-amsterdam/src/main/resources/archetype-resources"
                            + "/src/main/resources/__closedLoopControlName__.drl",
            "src/test/resources/yaml/policy_ControlLoop_vCPE.yaml",
            "service=ServiceDemo;resource=Res1Demo;type=operational",
            "CL_vCPE",
            "org.onap.closed_loop.ServiceDemo:VNFS:1.0.0");
    }

    /**
     * This test case tests the scenario where 3 events occur and 2 of the requests refer to the
     * same target entity while the 3rd is for another entity. The expected result is that the event
     * with the duplicate target entity will have a final success result for one of the events, and
     * a rejected message for the one that was unable to obtain the lock. The event that is
     * referring to a different target entity should be able to obtain a lock since it is a
     * different target. After processing of all events there should only be the params object left
     * in memory.
     */
    @Test
    public void targetLockedTest() {

        /*
         * Allows the PolicyEngine to callback to this object to notify that there is an event ready
         * to be pulled from the queue
         */
        for (TopicSink sink : noopTopics) {
            assertTrue(sink.start());
            sink.register(this);
        }

        /*
         * Create a unique requestId
         */
        requestId = UUID.randomUUID();

        /*
         * This will be a unique request for another target entity
         */
        requestId2 = UUID.randomUUID();

        /*
         * This will be a request duplicating the target entity of the first request
         */
        requestId3 = UUID.randomUUID();

        /*
         * Simulate an onset event the policy engine will receive from DCAE to kick off processing
         * through the rules
         */
        sendEvent(pair.first, requestId, ControlLoopEventStatus.ONSET, "vnf01");

        /*
         * Send a second event for a different target to ensure there are no problems with obtaining
         * a lock
         */
        sendEvent(pair.first, requestId2, ControlLoopEventStatus.ONSET, "vnf02");

        /*
         * Send a third event requesting an action for a duplicate target entity
         */
        sendEvent(pair.first, requestId3, ControlLoopEventStatus.ONSET, "vnf01");

        kieSession.fireUntilHalt();

        // allow object clean-up
        kieSession.fireAllRules();

        // should be one success and one failure for vnf01
        assertEquals(1, nsuccess);
        assertEquals(1, nreject);

        /*
         * The only fact in memory should be Params
         */
        assertEquals(1, kieSession.getFactCount());

        /*
         * Print what's left in memory
         */
        dumpFacts(kieSession);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.onap.policy.drools.PolicyEngineListener#newEventNotification(java.lang.String)
     */
    @Override
    public synchronized void onTopicEvent(CommInfrastructure commType, String topic, String event) {
        /*
         * Pull the object that was sent out to DMAAP and make sure it is a ControlLoopNoticiation
         * of type active
         */
        Object obj = null;
        if ("POLICY-CL-MGT".equals(topic)) {
            obj = org.onap.policy.controlloop.util.Serialization.gsonJunit.fromJson(event,
                    org.onap.policy.controlloop.VirtualControlLoopNotification.class);
        } else if ("APPC-LCM-READ".equals(topic)) {
            obj = org.onap.policy.appclcm.util.Serialization.gsonJunit.fromJson(event,
                    org.onap.policy.appclcm.AppcLcmDmaapWrapper.class);
        }
        assertNotNull(obj);
        if (obj instanceof VirtualControlLoopNotification) {
            VirtualControlLoopNotification notification = (VirtualControlLoopNotification) obj;
            String policyName = notification.getPolicyName();
            if (policyName.endsWith("EVENT")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                assertTrue(ControlLoopNotificationType.ACTIVE.equals(notification.getNotification()));
            } else if (policyName.endsWith("DENIED")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                assertTrue(ControlLoopNotificationType.REJECTED.equals(notification.getNotification()));
                assertNotNull(notification.getMessage());
                assertTrue(notification.getMessage().contains("is already locked"));
                if (requestId.equals(notification.getRequestId()) || requestId3.equals(notification.getRequestId())) {
                    ++nreject;
                }
                if (++eventCount == 3) {
                    kieSession.halt();
                }
            } else if (policyName.endsWith("GUARD_NOT_YET_QUERIED")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                assertTrue(ControlLoopNotificationType.OPERATION.equals(notification.getNotification()));
                assertNotNull(notification.getMessage());
                assertTrue(notification.getMessage().startsWith("Sending guard query"));
            } else if (policyName.endsWith("GUARD.RESPONSE")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                assertTrue(ControlLoopNotificationType.OPERATION.equals(notification.getNotification()));
                assertNotNull(notification.getMessage());
                assertTrue(notification.getMessage().toLowerCase().endsWith("permit"));
            } else if (policyName.endsWith("GUARD_PERMITTED")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                assertTrue(ControlLoopNotificationType.OPERATION.equals(notification.getNotification()));
                assertNotNull(notification.getMessage());
                assertTrue(notification.getMessage().startsWith("actor=APPC"));
            } else if (policyName.endsWith("OPERATION.TIMEOUT")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                kieSession.halt();
                logger.debug("The operation timed out");
                fail("Operation Timed Out");
            } else if (policyName.endsWith("APPC.LCM.RESPONSE")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                assertTrue(ControlLoopNotificationType.OPERATION_SUCCESS.equals(notification.getNotification()));
                assertNotNull(notification.getMessage());
                assertTrue(notification.getMessage().startsWith("actor=APPC"));
                if (requestId.equals(notification.getRequestId()) || requestId3.equals(notification.getRequestId())) {
                    sendEvent(pair.first, notification.getRequestId(), ControlLoopEventStatus.ABATED, "vnf01");
                } else if (requestId2.equals(notification.getRequestId())) {
                    sendEvent(pair.first, requestId2, ControlLoopEventStatus.ABATED, "vnf02");
                }
            } else if (policyName.endsWith("EVENT.MANAGER")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                if (requestId.equals(notification.getRequestId()) || requestId3.equals(notification.getRequestId())) {
                    assertTrue(ControlLoopNotificationType.FINAL_SUCCESS.equals(notification.getNotification()));
                    ++nsuccess;
                } else {
                    assertTrue(ControlLoopNotificationType.FINAL_SUCCESS.equals(notification.getNotification()));
                }
                if (++eventCount == 3) {
                    kieSession.halt();
                }
            } else if (policyName.endsWith("EVENT.MANAGER.TIMEOUT")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                kieSession.halt();
                logger.debug("The control loop timed out");
                fail("Control Loop Timed Out");
            }
        } else if (obj instanceof AppcLcmDmaapWrapper) {
            /*
             * The request should be of type LCMRequestWrapper and the subrequestid should be 1
             */
            AppcLcmDmaapWrapper dmaapRequest = (AppcLcmDmaapWrapper) obj;
            AppcLcmInput appcRequest = dmaapRequest.getBody().getInput();
            assertTrue(appcRequest.getCommonHeader().getSubRequestId().equals("1"));

            logger.debug("\n============ APPC received the request!!! ===========\n");

            /*
             * Simulate a success response from APPC and insert the response into the working memory
             */
            AppcLcmDmaapWrapper dmaapResponse = new AppcLcmDmaapWrapper();
            AppcLcmOutput appcResponse = new AppcLcmOutput(appcRequest);
            appcResponse.getStatus().setCode(400);
            appcResponse.getStatus().setMessage("AppC success");
            dmaapResponse.setBody(new AppcLcmBody());
            dmaapResponse.getBody().setOutput(appcResponse);

            /*
             * Interrupting with a different request for the same target entity to check if lock
             * will be denied
             */
            if (requestId.equals(appcResponse.getCommonHeader().getRequestId())) {
                sendEvent(pair.first, requestId3, ControlLoopEventStatus.ONSET, "vnf01");
            }
            kieSession.insert(dmaapResponse);
        }
    }

    /**
     * This method is used to simulate event messages from DCAE that start the control loop (onset
     * message) or end the control loop (abatement message).
     *
     * @param policy the controlLoopName comes from the policy
     * @param requestId the requestId for this event
     * @param status could be onset or abated
     * @param target the target entity to take an action on
     */
    protected void sendEvent(ControlLoopPolicy policy, UUID requestId, ControlLoopEventStatus status, String target) {
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(policy.getControlLoop().getControlLoopName());
        event.setRequestId(requestId);
        event.setTarget("generic-vnf.vnf-id");
        event.setTargetType(ControlLoopTargetType.VNF);
        event.setClosedLoopAlarmStart(Instant.now());
        event.setAai(new HashMap<>());
        event.getAai().put("generic-vnf.vnf-id", target);
        event.getAai().put("vserver.vserver-name", "OzVServer");
        event.setClosedLoopEventStatus(status);
        kieSession.insert(event);
    }
}
