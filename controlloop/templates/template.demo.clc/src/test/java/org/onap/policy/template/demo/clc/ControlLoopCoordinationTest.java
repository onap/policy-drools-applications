/*-
 * ============LICENSE_START=======================================================
 * demo
 * ================================================================================
 * Copyright (C) 2018-2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.template.demo.clc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.StringBuilder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
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

import org.onap.policy.appclcm.LcmRequest;
import org.onap.policy.appclcm.LcmRequestWrapper;
import org.onap.policy.appclcm.LcmResponse;
import org.onap.policy.appclcm.LcmResponseWrapper;
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
import org.onap.policy.coordination.CoordinationDirective;
import org.onap.policy.coordination.Util;
import org.onap.policy.drools.protocol.coders.EventProtocolCoder;
import org.onap.policy.drools.protocol.coders.EventProtocolParams;
import org.onap.policy.drools.protocol.coders.JsonProtocolFilter;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.drools.system.PolicyEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlLoopCoordinationTest implements TopicListener {

    private static final Logger logger = LoggerFactory.getLogger(ControlLoopCoordinationTest.class);

    private static List<? extends TopicSink> noopTopics;

    private static KieSession kieSession1;
    private static KieSession kieSession2;
    private static StringBuilder controlLoopOneName = new StringBuilder();
    private static StringBuilder controlLoopTwoName = new StringBuilder();
    private static String expectedDecision;

    static {
        /* Set environment properties */
        SupportUtil.setAaiProps();
        SupportUtil.setGuardPropsEmbedded();
        SupportUtil.setPuProp();
    }

    /**
     * Setup simulator.
     *
     * @throws IOException when thrown by buildAaiSim
     */
    @BeforeClass
    public static void setUpSimulator()  throws Exception {
        PolicyEngine.manager.configure(new Properties());
        assertTrue(PolicyEngine.manager.start());
        Properties noopSinkProperties = new Properties();
        noopSinkProperties.put(PolicyEndPointProperties.PROPERTY_NOOP_SINK_TOPICS, "APPC-LCM-READ,POLICY-CL-MGT");
        noopSinkProperties.put("noop.sink.topics.APPC-LCM-READ.events", "org.onap.policy.appclcm.LcmRequestWrapper");
        noopSinkProperties.put("noop.sink.topics.APPC-LCM-READ.events.custom.gson",
                "org.onap.policy.appclcm.util.Serialization,gson");
        noopSinkProperties.put("noop.sink.topics.POLICY-CL-MGT.events",
                "org.onap.policy.controlloop.VirtualControlLoopNotification");
        noopSinkProperties.put("noop.sink.topics.POLICY-CL-MGT.events.custom.gson",
                "org.onap.policy.controlloop.util.Serialization,gsonPretty");
        noopTopics = TopicEndpoint.manager.addTopicSinks(noopSinkProperties);

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
                .topic("APPC-LCM-READ")
                .eventClass("org.onap.policy.appclcm.LcmRequestWrapper")
                .protocolFilter(new JsonProtocolFilter())
                .modelClassLoaderHash(1111));
        SupportUtil.buildAaiSim();
        /*
         * Apply the coordination directive
         */
        final String coordinationDir = "src/test/resources/coordination";
        final String coordinationProtoDir = "src/main/resources/coordination/prototype";
        final String coordinationDirectiveName = "synthetic_control_loop_one_blocks_synthetic_control_loop_two";
        final String propertiesDir = "src/test/resources/properties";
        final String propertiesProtoDir = "src/test/resources/properties/prototype";
        final String xacmlDir      = "src/test/resources/xacml";
        final String yamlDir = "src/test/resources/yaml";

        String coordinationDirectiveFile = coordinationDir + File.separator + coordinationDirectiveName + ".yaml";
        /*
         * Load the coordination directive from the Yaml encoding
         */
        CoordinationDirective cd = Util.loadCoordinationDirectiveFromFile(coordinationDirectiveFile);
        logger.info("CoordinationDirective={}", cd.toString());
        /*
         * Generate the Xacml policy from the coordination directive
         */
        String xacmlPolicy = Util.generateXacmlFromCoordinationDirective(cd, coordinationProtoDir);
        /*
         * Create directory for Xacml output file, if necessary
         */
        Files.createDirectories(Paths.get(xacmlDir));
        /*
         * Write Xacml policy to file
         */
        String xacmlFilename      = xacmlDir + File.separator
            + cd.getCoordinationFunction()
            + cd.getControlLoop(1)
            + cd.getControlLoop(2)
            + ".xml";
        File xacmlFile = new File(xacmlFilename);
        PrintWriter xacmlFileWriter = new PrintWriter(xacmlFile);
        xacmlFileWriter.println(xacmlPolicy);
        xacmlFileWriter.flush();
        /*
         * Remove Xacml file after test
         */
        xacmlFile.deleteOnExit();
        /*
         * Insert the Xacml policy file into the PDP
         */
        SupportUtil.insertXacmlPolicy(xacmlFilename, propertiesProtoDir, propertiesDir);
        /*
         * Start the kie sessions
         */
        kieSession1 = startSession(
                    controlLoopOneName,
                    "src/main/resources/__closedLoopControlName__.drl",
                    yamlDir + "/policy_ControlLoop_SyntheticOne.yaml",
                    "service=ServiceDemo;resource=Res1Demo;type=operational",
                    "SyntheticControlLoopOnePolicy",
                    "org.onap.closed_loop.ServiceDemo:VNFS:1.0.0");
        kieSession2 = startSession(
                    controlLoopTwoName,
                    "src/main/resources/__closedLoopControlName__.drl",
                    yamlDir + "/policy_ControlLoop_SyntheticTwo.yaml",
                    "service=ServiceDemo;resource=Res1Demo;type=operational",
                    "SyntheticControlLoopTwoPolicy",
                    "org.onap.closed_loop.ServiceDemo:VNFS:1.0.0");
    }

    /**
     * Tear down simulator.
     */
    @AfterClass
    public static void tearDownSimulator() {
        /*
         * Gracefully shut down the kie session
         */
        kieSession1.dispose();
        kieSession2.dispose();

        PolicyEngine.manager.stop();
        HttpServletServer.factory.destroy();
        PolicyController.factory.shutdown();
        TopicEndpoint.manager.shutdown();
    }

    /**
     * Set expected decision.
     *
     * @param ed the expected decision ("PERMIT" or "DENY")
     */
    public void expectedDecisionIs(String ed) {
        expectedDecision = ed;
        logger.info("Expected decision is {}", ed);
    }

    /**
     * This method is used to simulate event messages from DCAE
     * that start the control loop (onset message) or end the
     * control loop (abatement message).
     *
     * @param controlLoopName the control loop name
     * @param requestId the requestId for this event
     * @param status could be onset or abated
     * @param target the target name
     * @param kieSession the kieSession to which this event is being sent
     */
    protected void sendEvent(String controlLoopName,
                             UUID requestId,
                             ControlLoopEventStatus status,
                             String target,
                             KieSession kieSession) {
        logger.debug("sendEvent controlLoopName={}", controlLoopName);
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(controlLoopName);
        event.setRequestId(requestId);
        event.setTarget("generic-vnf.vnf-name");
        event.setTargetType(ControlLoopTargetType.VNF);
        event.setClosedLoopAlarmStart(Instant.now());
        event.setAai(new HashMap<>());
        event.getAai().put("generic-vnf.vnf-name", target);
        event.setClosedLoopEventStatus(status);

        Gson gson = new Gson();
        String json = gson.toJson(event);
        logger.debug("sendEvent {}", json);

        kieSession.insert(event);
    }


    /**
     * Simulate an event by inserting into kieSession and firing rules as needed.
     *
     * @param cles the ControlLoopEventStatus
     * @param rid the request ID
     * @param controlLoopName the control loop name
     * @param kieSession the kieSession to which this event is being sent
     * @param expectedDecision the expected decision
     */
    protected void simulateEvent(ControlLoopEventStatus cles,
                                 UUID rid,
                                 String controlLoopName,
                                 String target,
                                 KieSession kieSession,
                                 String expectedDecision) {
        //
        // if onset, set expected decision
        //
        if (cles == ControlLoopEventStatus.ONSET) {
            expectedDecisionIs(expectedDecision);
        }
        //
        // simulate sending event
        //
        sendEvent(controlLoopName, rid, cles, target, kieSession);
        kieSession.fireUntilHalt();
        //
        // get dump of database entries and log
        //
        List<?> entries = SupportUtil.dumpDb();
        assertNotNull(entries);
        logger.debug("dumpDB, {} entries", entries.size());
        for (Object entry : entries) {
            logger.debug("{}", entry);
        }
        //
        // we are done
        //
        logger.info("simulateEvent: done");
    }

    /**
     * Simulate an onset event.
     *
     * @param rid the request ID
     * @param controlLoopName the control loop name
     * @param kieSession the kieSession to which this event is being sent
     * @param expectedDecision the expected decision
     */
    public void simulateOnset(UUID rid,
                              String controlLoopName,
                              String target,
                              KieSession kieSession,
                              String expectedDecision) {
        simulateEvent(ControlLoopEventStatus.ONSET, rid, controlLoopName, target, kieSession, expectedDecision);
    }

    /**
     * Simulate an abated event.
     *
     * @param rid the request ID
     * @param controlLoopName the control loop name
     * @param kieSession the kieSession to which this event is being sent
     */
    public void simulateAbatement(UUID rid,
                                  String controlLoopName,
                                  String target,
                                  KieSession kieSession) {
        simulateEvent(ControlLoopEventStatus.ABATED, rid, controlLoopName, target, kieSession, null);
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
     * @throws IOException throws IO exception
     */
    private static KieSession startSession(StringBuilder controlLoopName,
                                           String droolsTemplate,
                                           String yamlFile,
                                           String policyScope,
                                           String policyName,
                                           String policyVersion) throws IOException {

        /*
         * Load policies from yaml
         */
        SupportUtil.Pair<ControlLoopPolicy, String> pair = SupportUtil.loadYaml(yamlFile);
        assertNotNull(pair);
        assertNotNull(pair.first);
        assertNotNull(pair.first.getControlLoop());
        assertNotNull(pair.first.getControlLoop().getControlLoopName());
        assertTrue(!pair.first.getControlLoop().getControlLoopName().isEmpty());

        controlLoopName.append(pair.first.getControlLoop().getControlLoopName());
        String yamlContents = pair.second;

        /*
         * Construct a kie session
         */
        final KieSession kieSession = SupportUtil.buildContainer(droolsTemplate,
                                                          controlLoopName.toString(),
                                                          policyScope,
                                                          policyName,
                                                          policyVersion,
                                                          URLEncoder.encode(yamlContents, "UTF-8"));

        /*
         * Retrieve the Policy Engine
         */

        logger.debug("============");
        logger.debug(URLEncoder.encode(yamlContents, "UTF-8"));
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
                    org.onap.policy.controlloop.VirtualControlLoopNotification.class);
        } else if ("APPC-LCM-READ".equals(topic)) {
            obj = org.onap.policy.appclcm.util.Serialization.gsonJunit.fromJson(event,
                    org.onap.policy.appclcm.LcmRequestWrapper.class);
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
                // THESE ARE THE MOST CRITICAL ASSERTS
                // TEST IF GUARD.RESPONSE IS CORRECT
                logger.debug("Testing whether decision was {} as expected", expectedDecision);
                assertTrue(notification.getMessage().toUpperCase().endsWith(expectedDecision));
            } else if (policyName.endsWith("GUARD_PERMITTED")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                assertEquals(ControlLoopNotificationType.OPERATION,notification.getNotification());
                assertNotNull(notification.getMessage());
                assertTrue(notification.getMessage().startsWith("actor=APPC"));
            } else if (policyName.endsWith("OPERATION.TIMEOUT")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                kieSession1.halt();
                kieSession2.halt();
                logger.debug("The operation timed out");
                fail("Operation Timed Out");
            } else if (policyName.endsWith("APPC.LCM.RESPONSE")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                assertTrue(ControlLoopNotificationType.OPERATION_SUCCESS.equals(notification.getNotification()));
                assertNotNull(notification.getMessage());
                assertTrue(notification.getMessage().startsWith("actor=APPC"));
            } else if (policyName.endsWith("EVENT.MANAGER")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                if (notification.getMessage().endsWith("Closing the control loop.")
                    || notification.getMessage().equals("Waiting for abatement")) {
                    if (policyName.startsWith(controlLoopOneName.toString())) {
                        logger.debug("Halting kieSession1");
                        kieSession1.halt();
                    } else if (policyName.startsWith(controlLoopTwoName.toString())) {
                        logger.debug("Halting kieSession2");
                        kieSession2.halt();
                    } else {
                        fail("Unknown ControlLoop");
                    }
                }
            } else if (policyName.endsWith("EVENT.MANAGER.TIMEOUT")) {
                logger.debug("Rule Fired: " + notification.getPolicyName());
                kieSession1.halt();
                kieSession2.halt();
                logger.debug("The control loop timed out");
                fail("Control Loop Timed Out");
            }
        } else if (obj instanceof LcmRequestWrapper) {
            /*
             * The request should be of type LCMRequestWrapper and the subrequestid should be 1
             */
            LcmRequestWrapper dmaapRequest = (LcmRequestWrapper) obj;
            LcmRequest appcRequest = dmaapRequest.getBody();
            assertEquals(appcRequest.getCommonHeader().getSubRequestId(),"1");

            logger.debug("\n============ APPC received the request!!! ===========\n");

            /*
             * Simulate a success response from APPC and insert the response into the working memory
             */
            LcmResponseWrapper dmaapResponse = new LcmResponseWrapper();
            LcmResponse appcResponse = new LcmResponse(appcRequest);
            appcResponse.getStatus().setCode(400);
            appcResponse.getStatus().setMessage("AppC success");
            dmaapResponse.setBody(appcResponse);
            kieSession1.insert(dmaapResponse);
            kieSession2.insert(dmaapResponse);
        }
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

    /**
     * Test that SyntheticControlLoopOne blocks SyntheticControlLoopTwo
     * is enforced correctly.
     */
    @Test
    public void testSyntheticControlLoopOneBlocksSyntheticControlLoopTwo() throws InterruptedException {
        logger.info("Beginning testSyntheticControlLoopOneBlocksSyntheticControlLoopTwo");
        /*
         * Allows the PolicyEngine to callback to this object to
         * notify that there is an event ready to be pulled
         * from the queue
         */
        for (TopicSink sink : noopTopics) {
            assertTrue(sink.start());
            sink.register(this);
        }

        /*
         * Create unique requestIds
         */
        final UUID requestId1 = UUID.randomUUID();
        final UUID requestId2 = UUID.randomUUID();
        final UUID requestId3 = UUID.randomUUID();
        final UUID requestId4 = UUID.randomUUID();
        final UUID requestId5 = UUID.randomUUID();
        final String cl1 = controlLoopOneName.toString();
        final String cl2 = controlLoopTwoName.toString();
        final String t1 = "TARGET_1";
        final String t2 = "TARGET_2";

        logger.info("@@@@@@@@@@ cl2 ONSET t1 (Success) @@@@@@@@@@");
        simulateOnset(requestId1, cl2, t1, kieSession2,"PERMIT");

        logger.info("@@@@@@@@@@ cl1 ONSET t1 @@@@@@@@@@");
        simulateOnset(requestId2, cl1, t1, kieSession1,"PERMIT");

        logger.info("@@@@@@@@@@ cl2 ABATED t1 @@@@@@@@@@");
        simulateAbatement(requestId1, cl2, t1, kieSession2);

        logger.info("@@@@@@@@@@ cl2 ONSET t1 (Fail) @@@@@@@@@@");
        simulateOnset(requestId3, cl2, t1, kieSession2,"DENY");

        logger.info("@@@@@@@@@@ cl2 ONSET t2 (Success) @@@@@@@@@@");
        simulateOnset(requestId4, cl2, t2, kieSession2,"PERMIT");

        logger.info("@@@@@@@@@@ cl2 ABATED t2 @@@@@@@@@@");
        simulateAbatement(requestId4, cl2, t2, kieSession2);

        logger.info("@@@@@@@@@@ cl1 ABATED t1  @@@@@@@@@@");
        simulateAbatement(requestId2, cl1, t1, kieSession1);

        logger.info("@@@@@@@@@@ cl2 ONSET t1 (Success) @@@@@@@@@@");
        simulateOnset(requestId5, cl2, t1, kieSession2,"PERMIT");

        logger.info("@@@@@@@@@@ cl2 ABATED t1 @@@@@@@@@@");
        simulateAbatement(requestId5, cl2, t1, kieSession2);

        /*
         * Print what's left in memory
         */
        dumpFacts(kieSession1);
        dumpFacts(kieSession2);
    }
}
