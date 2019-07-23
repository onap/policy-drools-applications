/*-
 * ============LICENSE_START=======================================================
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


package org.onap.policy.drools.apps.controlloop.feature.usecases;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.Results;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.event.comm.TopicListener;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.http.server.HttpServletServer;
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.controlloop.actorserviceprovider.spi.Actor;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.drools.utils.logging.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This test will be used to test the Frankfurt Drools Rules. It will also
 * serve as the JUnit test for pairwise testing.  Each individual use case JUnit
 * will be removed once this work is finished.
 *
 *
 * @author pameladragosh
 *
 */
public class UsecaseTest implements TopicListener {
    private static final String OPSHISTPUPROP = "OperationsHistoryPU";

    private static final Logger logger = LoggerFactory.getLogger(UsecaseTest.class);

    private static List<? extends TopicSink> noopTopics;

    private static KieServices kieServices;
    private static KieContainer keyContainer;
    private static KieSession kieSession;

    static {
        //
        // Set environment properties
        //
        UsecaseTest.setAaiProps();
        UsecaseTest.setGuardProps();
        UsecaseTest.setPuProp();
        UsecaseTest.setSdncProps();
        UsecaseTest.setSoProps();
        UsecaseTest.setVfcProps();
        LoggerUtil.setLevel(LoggerUtil.ROOT_LOGGER, "INFO");
    }

    /**
     * Setup the simulator.
     *
     * @throws IOException exception
     * @throws FileNotFoundException exception
     */
    @BeforeClass
    public static void setUpSimulator() throws FileNotFoundException, IOException {
        //
        // Setup our manager and properties for the controller
        //
        PolicyEngine.manager.configure(new Properties());
        assertTrue(PolicyEngine.manager.start());

        Properties controllerProperties = new Properties();
        try (InputStream propertiesFile = new FileInputStream(
                "src/test/resources/config/usecases-controller.properties")) {
            controllerProperties.load(propertiesFile);
        };

        noopTopics = TopicEndpointManager.getManager().addTopicSinks(controllerProperties);

        TopicEndpointManager.getManager().addTopicSources(controllerProperties);

        //
        // Build our client-side simulators
        //
        try {
            UsecaseTest.buildAaiSim();
            UsecaseTest.buildGuardSim();
            UsecaseTest.buildSdncSim();
            UsecaseTest.buildSoSim();
            UsecaseTest.buildVfcSim();
        } catch (Exception e) {
            fail(e.getMessage());
        }
        //
        // Load the drools rule
        //
        byte[] rules = Files.readAllBytes(Paths.get("src/main/resources/archetype-resources",
                "/src/main/resources/__closedLoopControlName__frankfurt.drl"));
        /*
         * Start the kie session
         */
        try {
            kieSession = startSession(rules);
        } catch (IOException e) {
            logger.debug("Could not create kieSession {}", e);
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
    public void test() {
        ServiceLoader<Actor> serviceLoader = ServiceLoader.load(Actor.class);
        Iterator<Actor> iterator = serviceLoader.iterator();
        while (iterator.hasNext()) {
            Actor actor = iterator.next();
            assertNotNull(actor.actor());
            logger.info("Found actor {}", actor.actor());
        }
    }

    /**
     *  Set the A&AI properties.
     */
    private static void setAaiProps() {
        PolicyEngine.manager.setEnvironmentProperty("aai.url", "http://localhost:6666");
        PolicyEngine.manager.setEnvironmentProperty("aai.username", "AAI");
        PolicyEngine.manager.setEnvironmentProperty("aai.password", "AAI");
    }

    /**
     *  Set the SO properties.
     */
    private static void setSoProps() {
        PolicyEngine.manager.setEnvironmentProperty("so.url", "http://localhost:6667");
        PolicyEngine.manager.setEnvironmentProperty("so.username", "SO");
        PolicyEngine.manager.setEnvironmentProperty("so.password", "SO");
    }

    /**
     *  Set the SDNC properties.
     */
    private static void setSdncProps() {
        PolicyEngine.manager.setEnvironmentProperty("sdnc.url", "http://localhost:6670/restconf/operations");
        PolicyEngine.manager.setEnvironmentProperty("sdnc.username", "sdnc");
        PolicyEngine.manager.setEnvironmentProperty("sdnc.password", "sdnc");
    }

    /**
     *  Set the Guard properties.
     */
    private static void setGuardProps() {
        /*
         * Guard PDP-x connection Properties
         */
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_URL,         "http://localhost:6669/policy/pdpx/v1/decision");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_USER,        "python");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_PASS,        "test");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_DISABLED,    "false");
    }

    /**
     *  Set the VFC properties.
     */
    private static void setVfcProps() {
        PolicyEngine.manager.setEnvironmentProperty("vfc.url", "http://localhost:6668/api/nslcm/v1");
        PolicyEngine.manager.setEnvironmentProperty("vfc.username", "VFC");
        PolicyEngine.manager.setEnvironmentProperty("vfc.password", "VFC");
    }

    /**
     *  Set the operation history properties.
     */
    private static void setPuProp() {
        System.setProperty(OPSHISTPUPROP, "OperationsHistoryPUTest");
    }

    private static HttpServletServer buildAaiSim() throws InterruptedException, IOException {
        return org.onap.policy.simulators.Util.buildAaiSim();
    }

    private static HttpServletServer buildSoSim() throws InterruptedException, IOException {
        return org.onap.policy.simulators.Util.buildSoSim();
    }

    private static HttpServletServer buildVfcSim() throws InterruptedException, IOException {
        return org.onap.policy.simulators.Util.buildVfcSim();
    }

    private static HttpServletServer buildGuardSim() throws InterruptedException, IOException {
        return org.onap.policy.simulators.Util.buildGuardSim();
    }

    private static HttpServletServer buildSdncSim() throws InterruptedException, IOException {
        return org.onap.policy.simulators.Util.buildSdncSim();
    }

    /**
     * This method will start a kie session and instantiate the Policy Engine.
     *
     * @param rules the DRL rules file
     * @return the kieSession to be used to insert facts
     * @throws IOException IO Exception
     */
    private static KieSession startSession(byte[] rules) throws IOException {

        //
        // Get our Drools Kie factory
        //
        kieServices = KieServices.Factory.get();

        KieFileSystem kfs = kieServices.newKieFileSystem();
        ReleaseId releaseId = kieServices.getRepository().getDefaultReleaseId();
        releaseId = kieServices.newReleaseId(releaseId.getGroupId(), releaseId.getArtifactId(), releaseId.getVersion());

        kfs.generateAndWritePomXML(releaseId);

        kfs.write("src/main/resources/usecases.drl",
                kieServices.getResources().newByteArrayResource(rules));

        //
        // Compile the rule
        //
        KieBuilder builder = kieServices.newKieBuilder(kfs).buildAll();
        Results results = builder.getResults();
        if (results.hasMessages(Message.Level.ERROR)) {
            for (Message msg : results.getMessages()) {
                logger.error(msg.toString());
            }
            throw new RuntimeException("Drools Rule has Errors");
        }
        for (Message msg : results.getMessages()) {
            logger.debug(msg.toString());
        }
        logger.debug(releaseId.toString());

        //
        // Create our kie Session and container
        //
        keyContainer = kieServices.newKieContainer(releaseId);
        /*
         * Construct a kie session
         */
        final KieSession kieSession = keyContainer.newKieSession();

        return kieSession;
    }

    /**
     * This method will dump all the facts in the working memory.
     *
     * @param kieSession the session containing the facts
     */
    private long dumpFacts(KieSession kieSession) {
        long countFacts = kieSession.getFactCount();
        logger.debug("Fact Count: {}", countFacts);
        for (FactHandle handle : kieSession.getFactHandles()) {
            logger.debug("FACT: {}", handle);
        }
        return countFacts;
    }

    @Override
    public void onTopicEvent(CommInfrastructure commType, String topic, String event) {
        // TODO Auto-generated method stub

    }
}
