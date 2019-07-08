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

import java.io.IOException;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.onap.policy.appc.Request;
import org.onap.policy.appc.Response;
import org.onap.policy.appc.ResponseCode;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.event.comm.TopicListener;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.common.endpoints.properties.PolicyEndPointProperties;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.drools.protocol.coders.EventProtocolCoder;
import org.onap.policy.drools.protocol.coders.EventProtocolParams;
import org.onap.policy.drools.protocol.coders.JsonProtocolFilter;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.drools.utils.logging.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VfwControlLoopTest implements TopicListener {

    private static final Logger logger = LoggerFactory.getLogger(VfwControlLoopTest.class);

    private static List<? extends TopicSink> noopTopics;

    private static KieSession kieSession;
    private static SupportUtil.Pair<ControlLoopPolicy, String> pair;
    private UUID requestId;

    static {
        /* Set environment properties */
        SupportUtil.setAaiProps();
        SupportUtil.setGuardProps();
        SupportUtil.setPuProp();
        LoggerUtil.setLevel(LoggerUtil.ROOT_LOGGER, "INFO");
    }

    /**
     * Setup the simulator.
     */
    @BeforeClass
    public static void setUpSimulator() {
        PolicyEngine.manager.configure(new Properties());
        assertTrue(PolicyEngine.manager.start());
        Properties noopSinkProperties = new Properties();
        noopSinkProperties.put(PolicyEndPointProperties.PROPERTY_NOOP_SINK_TOPICS, "APPC-CL,POLICY-CL-MGT");
        noopSinkProperties.put("noop.sink.topics.APPC-CL.events", "org.onap.policy.appc.Response");
        noopSinkProperties.put("noop.sink.topics.APPC-CL.events.custom.gson",
                "org.onap.policy.appc.util.Serialization,gsonPretty");
        noopSinkProperties.put("noop.sink.topics.POLICY-CL-MGT.events",
                "org.onap.policy.controlloop.VirtualControlLoopNotification");
        noopSinkProperties.put("noop.sink.topics.POLICY-CL-MGT.events.custom.gson",
                "org.onap.policy.controlloop.util.Serialization,gsonPretty");
        noopTopics = TopicEndpointManager.getManager().addTopicSinks(noopSinkProperties);

        EventProtocolCoder.manager.addEncoder(EventProtocolParams.builder()
                .groupId("junit.groupId")
                .artifactId("junit.artifactId")
                .topic("POLICY-CL-MGT")
                .eventClass("org.onap.policy.controlloop.VirtualControlLoopNotification")
                .protocolFilter(new JsonProtocolFilter())
                .modelClassLoaderHash(1111));
        EventProtocolCoder.manager.addEncoder(EventProtocolParams.builder()
                .groupId("junit.groupId")
                .artifactId("junit.artifactId")
                .topic("APPC-CL")
                .eventClass("org.onap.policy.appc.Request")
                .protocolFilter(new JsonProtocolFilter())
                .modelClassLoaderHash(1111));

        try {
            SupportUtil.buildAaiSim();
            SupportUtil.buildGuardSim();
        } catch (Exception e) {
            fail(e.getMessage());
        }

        /*
         * Start the kie session
         */
        try {
            kieSession = startSession(
                    "../archetype-cl-amsterdam/src/main/resources/archetype-resources/src/"
                    + "main/resources/__closedLoopControlName__.drl",
                    "src/test/resources/yaml/policy_ControlLoop_vFW.yaml",
                    "service=ServiceDemo;resource=Res1Demo;type=operational", "CL_vFW",
                    "org.onap.closed_loop.ServiceDemo:VNFS:1.0.0");
        } catch (IOException e) {
            e.printStackTrace();
            logger.debug("Could not create kieSession");
            fail("Could not create kieSession");
        }
    }

    /**
     * Tear down the simulator.
     */
    @AfterClass
    public static void tearDownSimulator() {
        /*
         * Gracefully shut down the kie session
         */
        kieSession.dispose();

        PolicyEngine.manager.stop();
        HttpServletServerFactoryInstance.getServerFactory().destroy();
        PolicyController.factory.shutdown();
        TopicEndpointManager.getManager().shutdown();
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

        try {
            kieSession.fireUntilHalt();

            // allow object clean-up
            kieSession.fireAllRules();

        } catch (Exception e) {
            e.printStackTrace();
            logger.warn(e.toString());
            fail("fail");
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

    @Test
    public void aaiFailTests() {

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
        sendEvent(pair.first, requestId, ControlLoopEventStatus.ONSET, "error");
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

    /**
     * This method will start a kie session and instantiate the Policy Engine.
     *
     * @param droolsTemplate the DRL rules file
     * @param yamlFile the yaml file containing the policies
     * @param policyScope scope for policy
     * @param policyName name of the policy
     * @param policyVersion version of the policy
     * @return the kieSession to be used to insert facts
     * @throws IOException IO Exception
     */
    private static KieSession startSession(String droolsTemplate, String yamlFile, String policyScope,
            String policyName, String policyVersion) throws IOException {

        /*
         * Load policies from yaml
         */
        pair = SupportUtil.loadYaml(yamlFile);
        assertNotNull(pair);
        assertNotNull(pair.first);
        assertNotNull(pair.first.getControlLoop());
        assertNotNull(pair.first.getControlLoop().getControlLoopName());
        assertTrue(pair.first.getControlLoop().getControlLoopName().length() > 0);

        /*
         * Construct a kie session
         */
        final KieSession kieSession = SupportUtil.buildContainer(droolsTemplate,
                pair.first.getControlLoop().getControlLoopName(),
                policyScope, policyName, policyVersion, URLEncoder.encode(pair.second, "UTF-8"));

        /*
         * Retrieve the Policy Engine
         */

        logger.debug("============");
        logger.debug(URLEncoder.encode(pair.second, "UTF-8"));
        logger.debug("============");

        return kieSession;
    }

    /*
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
        } else if ("APPC-CL".equals(topic)) {
            obj = org.onap.policy.appc.util.Serialization.gsonPretty.fromJson(event,
                    org.onap.policy.appc.Request.class);
        }
        assertNotNull(obj);
        if (obj instanceof VirtualControlLoopNotification) {
            VirtualControlLoopNotification notification = (VirtualControlLoopNotification) obj;
            String policyName = notification.getPolicyName();
            if (policyName.endsWith("EVENT")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                assertTrue(ControlLoopNotificationType.ACTIVE.equals(notification.getNotification()));
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
            } else if (policyName.endsWith("APPC.RESPONSE")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                assertTrue(ControlLoopNotificationType.OPERATION_SUCCESS.equals(notification.getNotification()));
                assertNotNull(notification.getMessage());
                assertTrue(notification.getMessage().startsWith("actor=APPC"));
                sendEvent(pair.first, requestId, ControlLoopEventStatus.ABATED);
            } else if (policyName.endsWith("EVENT.MANAGER")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                if ("error".equals(notification.getAai().get("generic-vnf.vnf-name"))) {
                    assertEquals(ControlLoopNotificationType.FINAL_FAILURE, notification.getNotification());
                    assertEquals("Target vnf-id could not be found", notification.getMessage());
                } else if ("getFail".equals(notification.getAai().get("generic-vnf.vnf-name"))) {
                    assertEquals(ControlLoopNotificationType.FINAL_FAILURE, notification.getNotification());
                } else {
                    assertTrue(ControlLoopNotificationType.FINAL_SUCCESS.equals(notification.getNotification()));
                }
                kieSession.halt();
            } else if (policyName.endsWith("EVENT.MANAGER.TIMEOUT")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                kieSession.halt();
                logger.debug("The control loop timed out");
                fail("Control Loop Timed Out");
            }
        } else if (obj instanceof Request) {
            assertTrue(((Request) obj).getCommonHeader().getSubRequestId().equals("1"));
            assertNotNull(((Request) obj).getPayload().get("generic-vnf.vnf-id"));

            logger.debug("\n============ APPC received the request!!! ===========\n");

            /*
             * Simulate a success response from APPC and insert the response into the working memory
             */
            Response appcResponse = new Response((Request) obj);
            appcResponse.getStatus().setCode(ResponseCode.SUCCESS.getValue());
            appcResponse.getStatus().setValue("SUCCESS");
            kieSession.insert(appcResponse);
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
    protected void sendEvent(ControlLoopPolicy policy, UUID requestId, ControlLoopEventStatus status) {
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(policy.getControlLoop().getControlLoopName());
        event.setRequestId(requestId);
        event.setTarget("generic-vnf.vnf-name");
        event.setClosedLoopAlarmStart(Instant.now());
        event.setAai(new HashMap<>());
        event.getAai().put("generic-vnf.vnf-name", "testGenericVnfID");
        event.setClosedLoopEventStatus(status);
        kieSession.insert(event);
    }

    /**
     * This method is used to simulate event messages from DCAE that start the control loop (onset
     * message) or end the control loop (abatement message).
     *
     * @param policy the controlLoopName comes from the policy
     * @param requestId the requestId for this event
     * @param status could be onset or abated
     */
    protected void sendEvent(ControlLoopPolicy policy, UUID requestId, ControlLoopEventStatus status, String vnfId) {
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(policy.getControlLoop().getControlLoopName());
        event.setRequestId(requestId);
        event.setTarget("generic-vnf.vnf-name");
        event.setClosedLoopAlarmStart(Instant.now());
        event.setAai(new HashMap<>());
        event.getAai().put("generic-vnf.vnf-name", vnfId);
        event.setClosedLoopEventStatus(status);
        kieSession.insert(event);
    }

    /**
     * This method will dump all the facts in the working memory.
     *
     * @param kieSession the session containing the facts
     */
    public void dumpFacts(KieSession kieSession) {
        logger.debug("Fact Count: {}", kieSession.getFactCount());
        for (FactHandle handle : kieSession.getFactHandles()) {
            logger.debug("FACT: {}", handle);
        }
    }
}
