/*-
 * ============LICENSE_START=======================================================
 * demo
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019 Bell Canada.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.junit.AfterClass;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.common.endpoints.properties.PolicyEndPointProperties;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.drools.protocol.coders.EventProtocolCoderConstants;
import org.onap.policy.drools.protocol.coders.EventProtocolParams;
import org.onap.policy.drools.protocol.coders.JsonProtocolFilter;
import org.onap.policy.drools.system.PolicyControllerConstants;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.drools.utils.logging.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common super class used by various Control Loop test classes. It manages the simulators
 * and the kie session.
 */
public class ControlLoopBase {

    private static final String JUNIT_ARTIFACT_ID = "junit.artifactId";

    private static final String JUNIT_GROUP_ID = "junit.groupId";

    protected static final Logger logger = LoggerFactory.getLogger(ControlLoopBase.class);

    protected static List<? extends TopicSink> noopTopics;

    protected static KieSession kieSession;
    protected static SupportUtil.Pair<ControlLoopPolicy, String> pair;

    protected UUID requestId;

    /**
     * Starts the simulator and the kie session.
     *
     * @param droolsTemplate the DRL rules file
     * @param yamlFile the yaml file containing the policies
     * @param policyScope scope for policy
     * @param policyName name of the policy
     * @param policyVersion version of the policy
     */
    public static void setUpBeforeClass(String droolsTemplate, String yamlFile, String policyScope,
                    String policyName, String policyVersion) {

        SupportUtil.setCustomQuery("false");

        /* Set environment properties */
        SupportUtil.setAaiProps();
        SupportUtil.setGuardProps();
        SupportUtil.setSdncProps();
        SupportUtil.setSoProps();
        SupportUtil.setVfcProps();
        SupportUtil.setPuProp();
        SupportUtil.setCdsProps();

        LoggerUtil.setLevel(LoggerUtil.ROOT_LOGGER, "INFO");

        PolicyEngineConstants.getManager().configure(new Properties());
        assertTrue(PolicyEngineConstants.getManager().start());
        Properties noopSinkProperties = new Properties();
        noopSinkProperties.put(PolicyEndPointProperties.PROPERTY_NOOP_SINK_TOPICS,
                        "APPC-LCM-READ,APPC-CL,SDNR-CL,POLICY-CL-MGT");
        noopSinkProperties.put("noop.sink.topics.APPC-LCM-READ.events", "org.onap.policy.appclcm.LcmRequestWrapper");
        noopSinkProperties.put("noop.sink.topics.APPC-LCM-READ.events.custom.gson",
                "org.onap.policy.appclcm.util.Serialization,gson");
        noopSinkProperties.put("noop.sink.topics.APPC-CL.events", "org.onap.policy.appc.Response");
        noopSinkProperties.put("noop.sink.topics.APPC-CL.events.custom.gson",
                "org.onap.policy.appc.util.Serialization,gsonPretty");
        noopSinkProperties.put("noop.sink.topics.POLICY-CL-MGT.events",
                "org.onap.policy.controlloop.VirtualControlLoopNotification");
        noopSinkProperties.put("noop.sink.topics.POLICY-CL-MGT.events.custom.gson",
                "org.onap.policy.controlloop.util.Serialization,gsonPretty");
        noopTopics = TopicEndpointManager.getManager().addTopicSinks(noopSinkProperties);

        EventProtocolCoderConstants.getManager().addEncoder(EventProtocolParams.builder()
                .groupId(JUNIT_GROUP_ID)
                .artifactId(JUNIT_ARTIFACT_ID)
                .topic("POLICY-CL-MGT")
                .eventClass("org.onap.policy.controlloop.VirtualControlLoopNotification")
                .protocolFilter(new JsonProtocolFilter())
                .modelClassLoaderHash(1111));
        EventProtocolCoderConstants.getManager().addEncoder(EventProtocolParams.builder()
                .groupId(JUNIT_GROUP_ID)
                .artifactId(JUNIT_ARTIFACT_ID)
                .topic("APPC-LCM-READ")
                .eventClass("org.onap.policy.appclcm.LcmRequestWrapper")
                .protocolFilter(new JsonProtocolFilter())
                .modelClassLoaderHash(1111));
        EventProtocolCoderConstants.getManager().addEncoder(EventProtocolParams.builder()
                .groupId(JUNIT_GROUP_ID)
                .artifactId(JUNIT_ARTIFACT_ID)
                .topic("APPC-CL")
                .eventClass("org.onap.policy.appc.Request")
                .protocolFilter(new JsonProtocolFilter())
                .modelClassLoaderHash(1111));
        EventProtocolCoderConstants.getManager().addEncoder(EventProtocolParams.builder()
                .groupId(JUNIT_GROUP_ID)
                .artifactId(JUNIT_ARTIFACT_ID)
                .topic("SDNR-CL")
                .eventClass("org.onap.policy.sdnr.PciRequestWrapper")
                .protocolFilter(new JsonProtocolFilter())
                .modelClassLoaderHash(1111));
        try {
            SupportUtil.buildAaiSim();
            SupportUtil.buildSdncSim();
            SupportUtil.buildSoSim();
            SupportUtil.buildVfcSim();
            SupportUtil.buildGuardSim();
        } catch (Exception e) {
            fail(e.getMessage());
        }


        /*
         * Start the kie session
         */
        try {
            kieSession = startSession(droolsTemplate, yamlFile, policyScope,
                            policyName, policyVersion);
        } catch (IOException e) {
            e.printStackTrace();
            logger.debug("Could not create kieSession");
            fail("Could not create kieSession");
        }
    }

    /**
     * Stops the simulators and the kie session.
     */
    @AfterClass
    public static void tearDownAfterClass() {

        SupportUtil.setCustomQuery("false");

        /*
         * Gracefully shut down the kie session
         */
        kieSession.dispose();

        PolicyEngineConstants.getManager().stop();
        HttpServletServerFactoryInstance.getServerFactory().destroy();
        PolicyControllerConstants.getFactory().shutdown();
        TopicEndpointManager.getManager().shutdown();
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
     * @throws IOException IO exception
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
