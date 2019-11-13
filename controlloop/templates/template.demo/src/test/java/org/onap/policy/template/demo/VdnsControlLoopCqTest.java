/*-
 * ============LICENSE_START=======================================================
 * demo
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.TopicListener;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.ControlLoopTargetType;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.so.SoRequest;

public class VdnsControlLoopCqTest extends ControlLoopBase implements TopicListener {

    /**
     * Setup the simulator.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        ControlLoopBase.setUpBeforeClass(
            "../archetype-cl-amsterdam/src/main/resources/archetype-resources/"
                + "src/main/resources/__closedLoopControlName__.drl",
            "src/test/resources/yaml/policy_ControlLoop_SO_Cq-test.yaml", "type=operational",
            "CL_vDNS", "v2.0");
        SupportUtil.setCustomQuery("true");
    }

    @Test
    public void successTest() {

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
         * Simulate an onset event the policy engine will receive from DCAE to kick off processing
         * through the rules
         */
        sendEvent(pair.first, requestId, ControlLoopEventStatus.ONSET);

        kieSession.fireUntilHalt();

        // allow object clean-up
        kieSession.fireAllRules();

        /*
         * The only fact in memory should be Params
         */
        assertEquals(1, kieSession.getFactCount());

        /*
         * Print what's left in memory
         */
        dumpFacts(kieSession);
    }

    @Test
    public void aaiGetFailTest() {

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
         * Simulate an onset event the policy engine will receive from DCAE to kick off processing
         * through the rules
         */
        sendEvent(pair.first, requestId, ControlLoopEventStatus.ONSET, "getFail");

        try {
            kieSession.fireUntilHalt();

            // allow object clean-up
            kieSession.fireAllRules();

        } catch (Exception e) {
            e.printStackTrace();
            logger.warn(e.toString());
            fail(e.getMessage());
        }

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
    public void onTopicEvent(CommInfrastructure commType, String topic, String event) {
        /*
         * Pull the object that was sent out to DMAAP and make sure it is a ControlLoopNoticiation
         * of type active
         */
        Object obj = null;
        if ("POLICY-CL-MGT".equals(topic)) {
            obj = org.onap.policy.controlloop.util.Serialization.gsonJunit.fromJson(event,
                org.onap.policy.controlloop.VirtualControlLoopNotification.class);
        }
        assertNotNull(obj);
        if (obj instanceof VirtualControlLoopNotification) {
            VirtualControlLoopNotification notification = (VirtualControlLoopNotification) obj;
            String policyName = notification.getPolicyName();
            if (policyName.endsWith("EVENT")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                assertTrue(
                    ControlLoopNotificationType.ACTIVE.equals(notification.getNotification()));
            } else if (policyName.endsWith("GUARD_NOT_YET_QUERIED")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                assertTrue(
                    ControlLoopNotificationType.OPERATION.equals(notification.getNotification()));
                assertNotNull(notification.getMessage());
                assertTrue(notification.getMessage().startsWith("Sending guard query"));
            } else if (policyName.endsWith("GUARD.RESPONSE")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                assertTrue(
                    ControlLoopNotificationType.OPERATION.equals(notification.getNotification()));
                assertNotNull(notification.getMessage());
                assertTrue(notification.getMessage().toLowerCase().endsWith("permit"));
            } else if (policyName.endsWith("GUARD_PERMITTED")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                assertTrue(
                    ControlLoopNotificationType.OPERATION.equals(notification.getNotification()));
                assertNotNull(notification.getMessage());
                assertTrue(notification.getMessage().startsWith("actor=SO"));
            } else if (policyName.endsWith("OPERATION.TIMEOUT")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                kieSession.halt();
                logger.debug("The operation timed out");
                fail("Operation Timed Out");
            } else if (policyName.endsWith("SO.RESPONSE")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                assertTrue(ControlLoopNotificationType.OPERATION_SUCCESS
                    .equals(notification.getNotification()));
                assertNotNull(notification.getMessage());
                assertTrue(notification.getMessage().startsWith("actor=SO"));
            } else if (policyName.endsWith("EVENT.MANAGER")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                if ("error".equals(notification.getAai().get("vserver.vserver-name"))) {
                    assertEquals(ControlLoopNotificationType.FINAL_FAILURE,
                        notification.getNotification());
                } else if ("getFail".equals(notification.getAai().get("vserver.vserver-name"))) {
                    assertEquals(ControlLoopNotificationType.FINAL_FAILURE,
                        notification.getNotification());
                } else {
                    assertTrue(ControlLoopNotificationType.FINAL_SUCCESS
                        .equals(notification.getNotification()));
                }
                kieSession.halt();
            } else if (policyName.endsWith("EVENT.MANAGER.TIMEOUT")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                kieSession.halt();
                logger.debug("The control loop timed out");
                fail("Control Loop Timed Out");
            }
        } else if (obj instanceof SoRequest) {
            logger.debug("\n============ SO received the request!!! ===========\n");
        }
    }

    /**
     * This method is used to simulate event messages from DCAE that start the control loop (onset
     * message) or end the control loop (abatement message).
     *
     * @param policy the controlLoopName comes from the policy
     * @param requestId the requestId for this event
     * @param status could be onset or abated
     */
    protected void sendEvent(ControlLoopPolicy policy, UUID requestId,
        ControlLoopEventStatus status) {
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(policy.getControlLoop().getControlLoopName());
        event.setRequestId(requestId);
        event.setTarget("vserver.vserver-name");
        event.setTargetType(ControlLoopTargetType.VNF);
        event.setClosedLoopAlarmStart(Instant.now());
        event.setAai(new HashMap<>());
        event.getAai().put("vserver.vserver-name", "OzVServer");
        event.getAai().put("vserver.is-closed-loop-disabled", "false");
        event.getAai().put("vserver.prov-status", "ACTIVE");
        event.setClosedLoopEventStatus(status);
        kieSession.insert(event);
    }

    protected void sendEvent(ControlLoopPolicy policy, UUID requestId,
        ControlLoopEventStatus status, String vserverName) {
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(policy.getControlLoop().getControlLoopName());
        event.setRequestId(requestId);
        event.setTarget("vserver.vserver-name");
        event.setTargetType(ControlLoopTargetType.VNF);
        event.setClosedLoopAlarmStart(Instant.now());
        event.setAai(new HashMap<>());
        event.getAai().put("vserver.vserver-name", vserverName);
        event.setClosedLoopEventStatus(status);
        kieSession.insert(event);
    }
}
