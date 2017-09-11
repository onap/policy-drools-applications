/*-
 * ============LICENSE_START=======================================================
 * demo
 * ================================================================================
 * Copyright (C) 2017 Intel Corp. All rights reserved.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.Test;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.onap.policy.appclcm.LCMRequest;
import org.onap.policy.appclcm.LCMRequestWrapper;
import org.onap.policy.appclcm.LCMResponse;
import org.onap.policy.appclcm.LCMResponseWrapper;
import org.onap.policy.controlloop.*;
import org.onap.policy.controlloop.impl.ControlLoopLoggerStdOutImpl;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.drools.impl.PolicyEngineJUnitImpl;
import org.onap.policy.guard.PolicyGuard;
import org.onap.policy.vfc.util.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VFCControlLoopTest {

    private static final Logger logger = LoggerFactory.getLogger(VFCControlLoopTest.class);

    private KieSession kieSession;
    private Util.Pair<ControlLoopPolicy, String> pair;
    private PolicyEngineJUnitImpl engine;

    @Test
    public void successTest() {
        System.out.println("VFC successTest");
	
	@BeforeClass
        public static void setUpSimulator() {
            try {
                    Util.buildVfcSim();
            } catch (InterruptedException e) {
                    fail(e.getMessage());
            }
        }

        @AfterClass
        public static void tearDownSimulator() {
            HttpServletServer.factory.destroy();
        }

        /*
         * Start the kie session
         */
        try {
            kieSession = startSession("src/main/resources/ControlLoop_Template_xacml_guard.drl",
                    "src/test/resources/yaml/policy_ControlLoop_VFC.yaml",
                    "service=ServiceTest;resource=ResourceTest;type=operational",
                    "CL_VFC",
                    "org.onap.closed_loop.ServiceTest:VNFS:1.0.0");
        } catch (IOException e) {
            e.printStackTrace();
            logger.debug("Could not create kieSession");
            fail("Could not create kieSession");
        }

        /*
         * Create a thread to continuously fire rules
         * until main thread calls halt
         */
        new Thread(new Runnable() {
            @Override
            public void run() {
                kieSession.fireUntilHalt();
            }
        }).start();

        /*
         * Create a unique requestId and a unique trigger source
         */
        UUID requestID = UUID.randomUUID();

        /*
         * This will be the object returned from the PolicyEngine
         */
        Object obj = null;

        /*
         * Simulate an onset event the policy engine will
         * receive from DCAE to kick off processing through
         * the rules.
         */
        try {
            System.out.println("VFC Send Onset Message");
            sendOnset(pair.a, requestID);
        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.debug("Unable to send onset event");
            fail("Unable to send onset event");
        }

        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.debug("An interrupt Exception was thrown");
            fail("An interrupt Exception was thrown");
        }

        /*
         * This will stop the thread that is firing the rules
         */
        kieSession.halt();

        /*
         * The only fact in memory should be Params
         */
        assertEquals(1, kieSession.getFactCount());

        /*
         * Print what's left in memory
         */
        dumpFacts(kieSession);

        /*
         * Gracefully shut down the kie session
         */
        kieSession.dispose();
    }

    /**
     * This method will start a kie session and instantiate
     * the Policy Engine.
     *
     * @param droolsTemplate the DRL rules file
     * @param yamlFile       the yaml file containing the policies
     * @param policyScope    scope for policy
     * @param policyName     name of the policy
     * @param policyVersion  version of the policy
     * @return the kieSession to be used to insert facts
     * @throws IOException
     */
    private KieSession startSession(String droolsTemplate,
                                    String yamlFile,
                                    String policyScope,
                                    String policyName,
                                    String policyVersion) throws IOException {

        /*
         * Load policies from yaml
         */
        pair = Util.loadYaml(yamlFile);
        assertNotNull(pair);
        assertNotNull(pair.a);
        assertNotNull(pair.a.getControlLoop());
        assertNotNull(pair.a.getControlLoop().getControlLoopName());
        assertTrue(pair.a.getControlLoop().getControlLoopName().length() > 0);

        /*
         * Construct a kie session
         */
        final KieSession kieSession = Util.buildContainer(droolsTemplate,
                pair.a.getControlLoop().getControlLoopName(),
                policyScope,
                policyName,
                policyVersion,
                URLEncoder.encode(pair.b, "UTF-8"));

        /*
         * Retrieve the Policy Engine
         */
        engine = (PolicyEngineJUnitImpl) kieSession.getGlobal("Engine");

        logger.debug("============");
        logger.debug(URLEncoder.encode(pair.b, "UTF-8"));
        logger.debug("============");

        return kieSession;
    }

    /**
     * This method is used to simulate event messages from DCAE
     * that start the control loop (onset message).
     *
     * @param policy    the controlLoopName comes from the policy
     * @param requestID the requestId for this event
     * @throws InterruptedException
     */
    protected void sendOnset(ControlLoopPolicy policy, UUID requestID) throws InterruptedException {
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.closedLoopControlName = policy.getControlLoop().getControlLoopName();
        event.requestID = requestID;
        event.target_type = ControlLoopTargetType.VNF;
        event.target = "vserver.vserver-name";
        event.closedLoopAlarmStart = Instant.now();
        event.AAI = new HashMap<>();
        event.AAI.put("vserver.is-closed-loop-disabled", "false");
        event.AAI.put("vserver.vserver-name", "testGenericName");
        event.AAI.put("vserver.vserver-id", "testGenericId");
        event.AAI.put("generic-vnf.vnf-id", "testGenericVnfId");
        event.AAI.put("service-instance.service-instance-id", "testServiceInstanceId");
        event.closedLoopEventStatus = ControlLoopEventStatus.ONSET;
        kieSession.insert(event);
        Thread.sleep(1000);
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

