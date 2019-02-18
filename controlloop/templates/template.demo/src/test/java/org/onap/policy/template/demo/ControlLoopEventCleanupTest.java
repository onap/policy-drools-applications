/*-
 * ============LICENSE_START=======================================================
 * demo
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights reserved.
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.onap.policy.common.endpoints.event.comm.TopicEndpoint;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.http.server.HttpServletServer;
import org.onap.policy.common.endpoints.properties.PolicyEndPointProperties;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.eventmanager.ControlLoopEventManager;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.drools.protocol.coders.EventProtocolCoder;
import org.onap.policy.drools.protocol.coders.EventProtocolParams;
import org.onap.policy.drools.protocol.coders.JsonProtocolFilter;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.drools.utils.logging.LoggerUtil;
import org.onap.policy.template.demo.SupportUtil.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies that event objects are cleaned up when rules are updated. This loads
 * <b>two</b> copies of the rule set into a single policy to ensure that the two copies
 * interact appropriately with each other's event objects.
 */
public class ControlLoopEventCleanupTest {
    private static final Logger logger = LoggerFactory.getLogger(ControlLoopEventCleanupTest.class);

    /**
     * Number of objects per control loop, including the Params object.
     */
    private static int CL_OBJECTS = 7;

    private static final String YAML = "src/test/resources/yaml/policy_ControlLoop_EventCleanup-test.yaml";

    /**
     * YAML to be used when the first rule set is updated.
     */
    private static final String YAML2 = "src/test/resources/yaml/policy_ControlLoop_EventCleanup-test2.yaml";

    private static final String POLICY_VERSION = "v2.0";

    private static final String POLICY_NAME = "CL_CleanupTest";

    private static final String POLICY_SCOPE = "type=operational";

    private static final String CONTROL_LOOP_NAME = "ControlLoop-Event-Cleanup-Test";

    private static final String DROOLS_TEMPLATE = "../archetype-cl-amsterdam/src/main/resources/archetype-resources/"
                    + "src/main/resources/__closedLoopControlName__.drl";

    // values specific to the second copy of the rules

    private static final String YAML_B = "src/test/resources/yaml/policy_ControlLoop_EventCleanup-test-B.yaml";
    private static final String POLICY_NAME_B = "CL_CleanupTest_B";
    private static final String CONTROL_LOOP_NAME_B = "ControlLoop-Event-Cleanup-Test-B";

    private static final String GUARD_DISABLED = "guard.disabled";

    private static String saveGuardFlag;

    private static KieSession kieSession;
    private static SupportUtil.RuleSpec[] specifications;

    /**
     * Setup the simulator.
     */
    @BeforeClass
    public static void setUpSimulator() {
        LoggerUtil.setLevel(LoggerUtil.ROOT_LOGGER, "INFO");

        saveGuardFlag = PolicyEngine.manager.getEnvironmentProperty(GUARD_DISABLED);
        PolicyEngine.manager.getEnvironment().setProperty(GUARD_DISABLED, "true");

        SupportUtil.setAaiProps();

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
        final List<TopicSink> noopTopics = TopicEndpoint.manager.addTopicSinks(noopSinkProperties);

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

        } catch (Exception e) {
            logger.error("Could not create simulator", e);
            fail("Could not create simulator");
        }

        for (TopicSink sink : noopTopics) {
            assertTrue(sink.start());
        }

        try {
            specifications = new SupportUtil.RuleSpec[2];

            specifications[0] = new SupportUtil.RuleSpec(DROOLS_TEMPLATE, CONTROL_LOOP_NAME, POLICY_SCOPE, POLICY_NAME,
                            POLICY_VERSION, loadYaml(YAML));

            specifications[1] = new SupportUtil.RuleSpec(DROOLS_TEMPLATE, CONTROL_LOOP_NAME_B, POLICY_SCOPE,
                            POLICY_NAME_B, POLICY_VERSION, loadYaml(YAML_B));

            kieSession = SupportUtil.buildContainer(POLICY_VERSION, specifications);

        } catch (IOException e) {
            logger.error("Could not create kieSession", e);
            fail("Could not create kieSession");
        }
    }

    /**
     * Tear down.
     */
    @AfterClass
    public static void tearDown() {
        kieSession.dispose();

        PolicyEngine.manager.stop();
        HttpServletServer.factory.destroy();
        PolicyController.factory.shutdown();
        TopicEndpoint.manager.shutdown();

        if (saveGuardFlag == null) {
            PolicyEngine.manager.getEnvironment().remove(GUARD_DISABLED);

        } else {
            PolicyEngine.manager.getEnvironment().setProperty(GUARD_DISABLED, saveGuardFlag);
        }
    }

    @Test
    public void test() throws IOException {

        /*
         * Let rules create Params objects.
         */
        kieSession.fireAllRules();

        injectEvent(CONTROL_LOOP_NAME);
        injectEvent(CONTROL_LOOP_NAME_B);

        kieSession.fireAllRules();
        List<Object> facts = getSessionObjects();
        
        // should have events for both control loops
        assertEquals(2 * CL_OBJECTS, facts.size());
        assertTrue(hasEvent(facts, CONTROL_LOOP_NAME));
        assertTrue(hasEvent(facts, CONTROL_LOOP_NAME_B));

        logger.info("UPDATING VERSION TO v3.0");
        updatePolicy(YAML2, "v3.0");

        /*
         * Let rules update Params objects. The Params for the first set of rules should
         * now be deleted and replaced with a new one, while the Params for the second set
         * should be unchanged.
         */
        kieSession.fireAllRules();
        facts = getSessionObjects();

        // should only have event for second control loop + 1 Params for first control loop
        assertEquals(CL_OBJECTS + 1, facts.size());
        assertTrue(hasEvent(facts, CONTROL_LOOP_NAME_B));

        // add event for first control loop again
        injectEvent(CONTROL_LOOP_NAME);
        kieSession.fireAllRules();

        logger.info("UPDATING VERSION TO v4.0");
        updatePolicy(YAML, "v4.0");

        /*
         * Let rules update Params objects. The Params for the first set of rules should
         * now be deleted and replaced with a new one, while the Params for the second set
         * should be unchanged.
         */
        kieSession.fireAllRules();
        facts = getSessionObjects();

        // should only have event for second control loop + 1 Params for first control loop
        assertEquals(CL_OBJECTS + 1, facts.size());
        assertTrue(hasEvent(facts, CONTROL_LOOP_NAME_B));

        // add event for first control loop again
        injectEvent(CONTROL_LOOP_NAME);
        kieSession.fireAllRules();

        logger.info("UPDATING VERSION TO v4.0 (i.e., unchanged)");
        updatePolicy(YAML, "v4.0");

        /*
         * Let rules update Params objects. As the version (and YAML) are unchanged for
         * either rule set, both Params objects should be unchanged.
         */
        kieSession.fireAllRules();
        facts = getSessionObjects();

        // should have events for both control loops
        assertEquals(2 * CL_OBJECTS, facts.size());
        assertTrue(hasEvent(facts, CONTROL_LOOP_NAME));
        assertTrue(hasEvent(facts, CONTROL_LOOP_NAME_B));

        /*
         * Now we'll delete the first rule set. That won't actually have any immediate
         * effect, so then we'll update the second rule set, which should trigger a
         * clean-up of both.
         */
        SupportUtil.RuleSpec[] specs = new SupportUtil.RuleSpec[1];
        specs[0] = specifications[1];

        logger.info("UPDATING VERSION TO v5.0 - DELETED RULE SET");
        SupportUtil.updateContainer("v5.0", specs);

        specs[0] = new SupportUtil.RuleSpec(DROOLS_TEMPLATE, CONTROL_LOOP_NAME_B, POLICY_SCOPE, POLICY_NAME_B,
                POLICY_VERSION, loadYaml(YAML));

        logger.info("UPDATING VERSION TO v6.0 - UPDATED SECOND RULE SET");
        SupportUtil.updateContainer("v6.0", specs);

        kieSession.fireAllRules();
        facts = getSessionObjects();

        // only 1 Params should remain, for second rule set, but events should be gone
        assertEquals(1, facts.size());
        assertTrue(facts.stream().anyMatch(obj -> obj.toString().startsWith("Params( ")));
    }

    /**
     * Updates the policy, changing the YAML associated with the first rule set.
     *
     * @param yamlFile name of the YAML file
     * @param policyVersion policy version
     * @throws IOException if an error occurs
     */
    private static void updatePolicy(String yamlFile, String policyVersion) throws IOException {

        specifications[0] = new SupportUtil.RuleSpec(DROOLS_TEMPLATE, CONTROL_LOOP_NAME, POLICY_SCOPE, POLICY_NAME,
                        policyVersion, loadYaml(yamlFile));

        /*
         * Update the policy within the container.
         */
        SupportUtil.updateContainer(policyVersion, specifications);
    }

    /**
     * Loads a YAML file and URL-encodes it.
     *
     * @param yamlFile name of the YAML file
     * @return the contents of the specified file, URL-encoded
     * @throws UnsupportedEncodingException if an error occurs
     */
    private static String loadYaml(String yamlFile) throws UnsupportedEncodingException {
        Pair<ControlLoopPolicy, String> pair = SupportUtil.loadYaml(yamlFile);
        assertNotNull(pair);
        assertNotNull(pair.first);
        assertNotNull(pair.first.getControlLoop());
        assertNotNull(pair.first.getControlLoop().getControlLoopName());
        assertTrue(pair.first.getControlLoop().getControlLoopName().length() > 0);

        return URLEncoder.encode(pair.second, "UTF-8");
    }

    /**
     * Gets the session objects.
     *
     * @return the session objects
     */
    private static List<Object> getSessionObjects() {
        // sort the objects so we know the order
        LinkedList<Object> lst = new LinkedList<>(kieSession.getObjects());
        lst.sort((left, right) -> left.toString().compareTo(right.toString()));

        lst.forEach(obj -> logger.info("obj={}", obj));

        return lst;
    }

    /**
     * Injects an ONSET event into the rule engine.
     *
     * @param controlLoopName the control loop name
     */
    private void injectEvent(String controlLoopName) {
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();

        event.setClosedLoopControlName(controlLoopName);

        UUID reqid = UUID.randomUUID();
        event.setRequestId(reqid);

        event.setTarget("generic-vnf.vnf-id");
        event.setClosedLoopAlarmStart(Instant.now());
        event.setAai(new HashMap<>());
        event.getAai().put("generic-vnf.vnf-id", "vnf-" + reqid.toString());
        event.getAai().put(ControlLoopEventManager.GENERIC_VNF_IS_CLOSED_LOOP_DISABLED, "false");
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);

        kieSession.insert(event);
    }

    /**
     * Determines if the facts contain an event for the given control loop.
     * 
     * @param facts session facts to be checked
     * @param controlLoopName name of the control loop of interest
     * @return {@code true} if the facts contain an event for the given control loop,
     *         {@code false} otherwise
     */
    private boolean hasEvent(List<Object> facts, String controlLoopName) {
        return (facts.stream().anyMatch(obj -> obj instanceof VirtualControlLoopEvent
                        && controlLoopName.equals(((VirtualControlLoopEvent) obj).getClosedLoopControlName())));
    }
}
