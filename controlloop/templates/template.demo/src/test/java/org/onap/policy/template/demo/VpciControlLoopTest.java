/*-
 * ============LICENSE_START=======================================================
 * demo
 * ================================================================================
 * Copyright (C) 2018 Wipro Limited Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2018-2020 AT&T Intellectual Property. All rights reserved.
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
import org.onap.policy.sdnr.PciRequest;
import org.onap.policy.sdnr.PciRequestWrapper;
import org.onap.policy.sdnr.PciResponse;
import org.onap.policy.sdnr.PciResponseWrapper;

public class VpciControlLoopTest extends ControlLoopBase implements TopicListener {

    /**
     * Setup the simulator.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        ControlLoopBase.setUpBeforeClass(
            "../archetype-cl-amsterdam/src/main/resources/archetype-resources"
                    + "/src/main/resources/__closedLoopControlName__.drl",
            "src/test/resources/yaml/policy_ControlLoop_vPCI.yaml",
            "type=operational",
            "CL_vPCI",
            "v3.0.0");
    }

    @Test
    public void testSuccess() {

        /*
         * Allows the PolicyEngine to callback to this object to notify that there is an
         * event ready to be pulled from the queue
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
         * Simulate an onset event the policy engine will receive from DCAE to kick off
         * processing through the rules
         */
        sendEvent(pair.first, requestId, ControlLoopEventStatus.ONSET, true);

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
    public void testAaiGetFail() {

        /*
         * Allows the PolicyEngine to callback to this object to notify that there is an
         * event ready to be pulled from the queue
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
         * Simulate an onset event the policy engine will receive from DCAE to kick off
         * processing through the rules
         */
        sendEvent(pair.first, requestId, ControlLoopEventStatus.ONSET, false);

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

    /*
     * (non-Javadoc)
     *
     * @see
     * org.onap.policy.drools.PolicyEngineListener#newEventNotification(java.lang.
     * String)
     */
    @Override
    public void onTopicEvent(CommInfrastructure commType, String topic, String event) {
        logger.debug("\n============ onTopicEvent!!! ===========\n");
        logger.debug("topic: {}, event: {}", topic, event);
        /*
         * Pull the object that was sent out to DMAAP and make sure it is a
         * ControlLoopNoticiation of type active
         */
        Object obj = null;
        if ("POLICY-CL-MGT".equals(topic)) {
            obj = org.onap.policy.controlloop.util.Serialization.gsonJunit.fromJson(event,
                    org.onap.policy.controlloop.VirtualControlLoopNotification.class);
        } else if ("SDNR-CL".equals(topic)) {
            obj = org.onap.policy.sdnr.util.Serialization.gsonJunit.fromJson(event,
                    org.onap.policy.sdnr.PciRequestWrapper.class);
        }
        assertNotNull(obj);
        if (obj instanceof VirtualControlLoopNotification) {
            VirtualControlLoopNotification notification = (VirtualControlLoopNotification) obj;
            String policyName = notification.getPolicyName();
            logger.debug("Rule Fired: {}", policyName);
            if (policyName.endsWith("EVENT")) {
                assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());
            } else if (policyName.endsWith("GUARD_NOT_YET_QUERIED")) {
                assertEquals(ControlLoopNotificationType.OPERATION, notification.getNotification());
                assertNotNull(notification.getMessage());
                assertTrue(notification.getMessage().startsWith("Sending guard query"));
            } else if (policyName.endsWith("GUARD.RESPONSE")) {
                assertEquals(ControlLoopNotificationType.OPERATION, notification.getNotification());
                assertNotNull(notification.getMessage());
                assertTrue(notification.getMessage().toLowerCase().endsWith("permit"));
            } else if (policyName.endsWith("GUARD_PERMITTED")) {
                assertEquals(ControlLoopNotificationType.OPERATION, notification.getNotification());
                assertNotNull(notification.getMessage());
                assertTrue(notification.getMessage().startsWith("actor=SDNR"));
            } else if (policyName.endsWith("OPERATION.TIMEOUT")) {
                kieSession.halt();
                logger.debug("The operation timed out");
                fail("Operation Timed Out");
            } else if (policyName.endsWith("SDNR.RESPONSE")) {
                assertEquals(ControlLoopNotificationType.OPERATION_SUCCESS, notification.getNotification());
                assertNotNull(notification.getMessage());
                assertTrue(notification.getMessage().startsWith("actor=SDNR"));
            } else if (policyName.endsWith("EVENT.MANAGER")) {
                if ("getFail".equals(notification.getAai().get("generic-vnf.vnf-id"))) {
                    assertEquals(ControlLoopNotificationType.FINAL_FAILURE, notification.getNotification());
                    kieSession.halt();
                } else {
                    assertEquals(ControlLoopNotificationType.FINAL_SUCCESS, notification.getNotification());
                    kieSession.halt();
                }
            } else if (policyName.endsWith("EVENT.MANAGER.TIMEOUT")) {
                kieSession.halt();
                logger.debug("The control loop timed out");
                fail("Control Loop Timed Out");
            }
        } else if (obj instanceof PciRequestWrapper) {
            /*
             * The request should be of type PciRequestWrapper and the subrequestid should
             * be 1
             */
            PciRequestWrapper dmaapRequest = (PciRequestWrapper) obj;
            PciRequest pciRequest = dmaapRequest.getBody();
            assertEquals("1", pciRequest.getCommonHeader().getSubRequestId());

            logger.debug("\n============ SDNR received the request!!! ===========\n");
            logger.debug("\n============ dmaapRequest ===========\n {} ", dmaapRequest);
            logger.debug("\n============ pciRequest ===========\n {}", pciRequest);

            /*
             * Simulate a success response from SDNR and insert the response into the
             * working memory
             */
            PciResponse pciResponse = new PciResponse(pciRequest);
            pciResponse.getStatus().setCode(200);
            pciResponse.getStatus().setValue("SUCCESS");
            StringBuilder sb = new StringBuilder();
            sb.append("{ \"Configurations\":[ { \"Status\": { \"Code\": 200, \"Value\":"
                    + " \"SUCCESS\" }, \"data\":{ \"FAPService\":{ \"alias\":"
                    + "\"Network1\", \"X0005b9Lte\" : { \"PnfName\" : \"cu1\" }, \"CellConfig\":"
                    + "{ \"LTE\":{ \"RAN\":{ \"Common\":{ \"CellIdentity\":" + "\"1\" } } } } } } } ] }");

            pciResponse.setPayload(sb.toString());
            PciResponseWrapper dmaapResponse = new PciResponseWrapper();
            dmaapResponse.setBody(pciResponse);
            dmaapResponse.setType("response");
            logger.debug("\n============ SDNR sending response!!! ===========\n");
            logger.debug("\n============ dmaapResponse ===========\n {}", dmaapResponse);
            logger.debug("\n============ pciResponse ===========\n {}", pciResponse);
            kieSession.insert(dmaapResponse);
        }
    }

    /**
     * This method is used to simulate event messages from DCAE that start the
     * control loop (onset message).
     *
     * @param policy
     *            the controlLoopName comes from the policy
     * @param requestId
     *            the requestId for this event
     * @param status
     *            could be onset
     */
    protected void sendEvent(ControlLoopPolicy policy, UUID requestId, ControlLoopEventStatus status,
            boolean isEnriched) {
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(policy.getControlLoop().getControlLoopName());
        event.setRequestId(requestId);
        event.setTarget("generic-vnf.vnf-id");
        event.setTargetType(ControlLoopTargetType.VNF);
        event.setClosedLoopAlarmStart(Instant.now());
        event.setAai(new HashMap<>());
        if (isEnriched) {
            event.getAai().put("generic-vnf.is-closed-loop-disabled", "false");
            event.getAai().put("generic-vnf.prov-status", "ACTIVE");
            event.getAai().put("generic-vnf.vnf-id", "notused");
            event.getAai().put("vserver.vserver-name", "OzVServer");
        } else {
            event.getAai().put("generic-vnf.vnf-id", "getFail");
        }
        event.setClosedLoopEventStatus(status);
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"Configurations\":[ { \"data\":{ \"FAPService\":"
                + " { \"alias\":\"Cell1\", \"X0005b9Lte\" : { \"PhyCellIdInUse\" :"
                + " \"35\", \"PnfName\" : \"cu1\" }, \"CellConfig\":{ \"LTE\":{ \"RAN\":"
                + "{ \"Common\":{ \"CellIdentity\":\"1\" } } } } } } } ] }");

        event.setPayload(sb.toString());
        logger.debug("\n============ Policy receiving ONSET event !!! ===========\n");
        logger.debug("\n============ event ===========\n {}", event);
        kieSession.insert(event);
    }
}
