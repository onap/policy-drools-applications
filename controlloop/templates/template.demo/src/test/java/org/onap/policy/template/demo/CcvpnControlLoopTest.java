/*-
 * ============LICENSE_START=======================================================
 * demo
 * ================================================================================
 * Copyright (C) 2017-2018 Intel Corp. All rights reserved.
 * Modifications Copyright (C) 2018 AT&T Intellectual Property. All rights reserved.
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
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.TopicEndpoint;
import org.onap.policy.common.endpoints.event.comm.TopicListener;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.http.server.HttpServletServer;
import org.onap.policy.common.endpoints.properties.PolicyEndPointProperties;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.ControlLoopTargetType;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.drools.protocol.coders.EventProtocolCoder;
import org.onap.policy.drools.protocol.coders.JsonProtocolFilter;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.drools.utils.logging.LoggerUtil;
import org.onap.policy.sdnc.SdncRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CcvpnControlLoopTest implements TopicListener {

    private static final Logger logger = LoggerFactory.getLogger(CcvpnControlLoopTest.class);

    private static List<? extends TopicSink> noopTopics;

    private static KieSession kieSession;
    private static Util.Pair<ControlLoopPolicy, String> pair;

    static {
        /* Set environment properties */
        Util.setAaiProps();
        Util.setSdncProps();
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
        noopSinkProperties.put(PolicyEndPointProperties.PROPERTY_NOOP_SINK_TOPICS, "POLICY-CL-MGT");
        noopSinkProperties.put("noop.sink.topics.POLICY-CL-MGT.events",
                "org.onap.policy.controlloop.VirtualControlLoopNotification");
        noopSinkProperties.put("noop.sink.topics.POLICY-CL-MGT.events.custom.gson",
                "org.onap.policy.controlloop.util.Serialization,gsonPretty");
        noopTopics = TopicEndpoint.manager.addTopicSinks(noopSinkProperties);

        EventProtocolCoder.manager.addEncoder("junit.groupId", "junit.artifactId", "POLICY-CL-MGT",
                "org.onap.policy.controlloop.VirtualControlLoopNotification", new JsonProtocolFilter(),
                                              null, null,1111);

        try {
            Util.buildAaiSim();
            Util.buildSdncSim();
        } catch (Exception e) {
            fail(e.getMessage());
        }

        /*
         *
         * Start the kie session
         */
        try {
            kieSession = startSession(
                    "../archetype-cl-amsterdam/src/main/resources/archetype-resources/"
                    + "src/main/resources/__closedLoopControlName__.drl",
                    "src/test/resources/yaml/policy_ControlLoop_CCVPN.yaml", "type=operational", "Connectivity Reroute",
                    "2.0.0");
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
        HttpServletServer.factory.destroy();
        PolicyController.factory.shutdown();
        TopicEndpoint.manager.shutdown();
    }

    @Test
    public void successTest() throws IOException {

        /*
         * Allows the PolicyEngine to callback to this object to notify that there is an event ready
         * to be pulled from the queue
         */
        for (TopicSink sink : noopTopics) {
            assertTrue(sink.start());
            sink.register(this);
        }

        /*
         * Simulate an onset event the policy engine will receive from DCAE to kick off processing
         * through the rules
         */
        sendEvent(pair.first);

        kieSession.fireUntilHalt();

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
    public void nullRequestTest() throws IOException {

        /*
         * Allows the PolicyEngine to callback to this object to notify that there is an event ready
         * to be pulled from the queue
         */
        for (TopicSink sink : noopTopics) {
            assertTrue(sink.start());
            sink.register(this);
        }

        /*
         * Simulate an onset event the policy engine will receive from DCAE to kick off processing
         * through the rules
         */

        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(pair.first.getControlLoop().getControlLoopName());
        event.setRequestId(UUID.randomUUID());
        event.setClosedLoopEventClient("DCAE.HolmesInstance");
        event.setTargetType(ControlLoopTargetType.VM);
        event.setTarget("vserver.vserver-name");
        event.setFrom("DCAE");
        event.setClosedLoopAlarmStart(Instant.now());
        event.setAai(new HashMap<String, String>());
        event.getAai().put("vserver.vserver-name", "nullRequest");
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        kieSession.insert(event);

        kieSession.fireUntilHalt();

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
        pair = Util.loadYaml(yamlFile);
        assertNotNull(pair);
        assertNotNull(pair.first);
        assertNotNull(pair.first.getControlLoop());
        assertNotNull(pair.first.getControlLoop().getControlLoopName());
        assertTrue(pair.first.getControlLoop().getControlLoopName().length() > 0);

        /*
         * Construct a kie session
         */
        final KieSession kieSession = Util.buildContainer(droolsTemplate,
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
                    VirtualControlLoopNotification.class);
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
                assertTrue(notification.getMessage().startsWith("actor=SDNC"));
            } else if (policyName.endsWith("OPERATION.TIMEOUT")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                kieSession.halt();
                logger.debug("The operation timed out");
                fail("Operation Timed Out");
            } else if (policyName.endsWith("SDNC.RESPONSE")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                assertTrue(ControlLoopNotificationType.OPERATION_SUCCESS.equals(notification.getNotification()));
                assertNotNull(notification.getMessage());
                assertTrue(notification.getMessage().startsWith("actor=SDNC"));
            } else if (policyName.endsWith("EVENT.MANAGER")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                if ("nullRequest".equals(notification.getAai().get("vserver.vserver-name"))) {
                    assertEquals(ControlLoopNotificationType.FINAL_FAILURE, notification.getNotification());
                } else {
                    assertEquals(ControlLoopNotificationType.FINAL_SUCCESS, notification.getNotification());
                }
                kieSession.halt();
            } else if (policyName.endsWith("EVENT.MANAGER.TIMEOUT")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                kieSession.halt();
                logger.debug("The control loop timed out");
                fail("Control Loop Timed Out");
            }
        } else if (obj instanceof SdncRequest) {
            logger.debug("\n============ SDNC received the request!!! ===========\n");
        }
    }

    /**
     * This method is used to simulate event messages from DCAE that start the control loop (onset
     * message) or end the control loop (abatement message).
     * 
     * @param policy the controlLoopName comes from the policy
     */
    protected void sendEvent(ControlLoopPolicy policy) {
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(policy.getControlLoop().getControlLoopName());
        event.setRequestId(UUID.randomUUID());
        event.setClosedLoopEventClient("DCAE.HolmesInstance");
        event.setTargetType(ControlLoopTargetType.VM);
        event.setTarget("vserver.vserver-name");
        event.setFrom("DCAE");
        event.setClosedLoopAlarmStart(Instant.now());
        event.setAai(new HashMap<String, String>());
        event.getAai().put("vserver.vserver-name", "TBD");
        event.getAai().put("globalSubscriberId", "e151059a-d924-4629-845f-264db19e50b4");
        event.getAai().put("serviceType", "SOTN");
        event.getAai().put("service-instance.service-instance-id", "service-instance-id-example-1");
        event.getAai().put("network-information.network-id", "id");
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        kieSession.insert(event);
    }

    /**
     * Dumps the kie session facts.
     * 
     * @param kieSession input session
     */
    public static void dumpFacts(KieSession kieSession) {
        logger.debug("Fact Count: " + kieSession.getFactCount());
        for (FactHandle handle : kieSession.getFactHandles()) {
            logger.debug("FACT: " + handle);
        }
    }
}

