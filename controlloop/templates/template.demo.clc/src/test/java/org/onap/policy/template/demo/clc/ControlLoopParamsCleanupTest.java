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

package org.onap.policy.template.demo.clc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.drools.utils.logging.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies that Params objects are cleaned up when rules are updated. This loads
 * <b>two</b> copies of the rule set into a single policy to ensure that the two copies
 * interact appropriately with each other's Params objects.
 */
public class ControlLoopParamsCleanupTest {
    private static final Logger logger = LoggerFactory.getLogger(ControlLoopParamsCleanupTest.class);

    private static final String YAML = "src/test/resources/yaml/policy_ControlLoop_ParamsCleanup-test.yaml";

    /**
     * YAML to be used when the first rule set is updated.
     */
    private static final String YAML2 = "src/test/resources/yaml/policy_ControlLoop_ParamsCleanup-test2.yaml";

    private static final String POLICY_VERSION = "v2.0";

    private static final String POLICY_NAME = "CL_CleanupTest";
    private static final String POLICY_NAME_B = "CL_CleanupTest_B";

    private static final String POLICY_SCOPE = "type=operational";

    private static final String CONTROL_LOOP_NAME = "ControlLoop-Params-Cleanup-Test";

    private static final String DROOLS_TEMPLATE = "src/main/resources/__closedLoopControlName__.drl";

    // values specific to the second copy of the rules

    private static final String YAML_B = "src/test/resources/yaml/policy_ControlLoop_ParamsCleanup-test-B.yaml";
    private static final String CONTROL_LOOP_NAME_B = "ControlLoop-Params-Cleanup-Test-B";

    private static KieSession kieSession;
    private static Util.Pair<ControlLoopPolicy, String> pair;
    private static Util.RuleSpec[] specifications;

    /**
     * Setup the simulator.
     */
    @BeforeClass
    public static void setUpSimulator() {
        LoggerUtil.setLevel(LoggerUtil.ROOT_LOGGER, "INFO");

        try {
            specifications = new Util.RuleSpec[2];

            specifications[0] = new Util.RuleSpec(DROOLS_TEMPLATE, CONTROL_LOOP_NAME, POLICY_SCOPE, POLICY_NAME,
                            POLICY_VERSION, loadYaml(YAML));

            specifications[1] = new Util.RuleSpec(DROOLS_TEMPLATE, CONTROL_LOOP_NAME_B, POLICY_SCOPE, POLICY_NAME_B,
                            POLICY_VERSION, loadYaml(YAML_B));

            kieSession = Util.buildContainer(POLICY_VERSION, specifications);

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
    }

    @Test
    public void test() throws IOException {

        /*
         * Let rules create Params objects. There should be one object for each set of
         * rules.
         */
        kieSession.fireAllRules();
        Set<String> facts = getSessionObjects();
        assertEquals(specifications.length, facts.size());
        Iterator<String> iter = facts.iterator();
        final String fact1 = iter.next();
        final String fact1b = iter.next();

        logger.info("UPDATING VERSION TO v3.0");
        updatePolicy(YAML2, "v3.0");

        /*
         * Let rules update Params objects. The Params for the first set of rules should
         * now be deleted and replaced with a new one, while the Params for the second set
         * should be unchanged.
         */
        kieSession.fireAllRules();
        facts = getSessionObjects();
        assertEquals(specifications.length, facts.size());
        assertTrue(facts.remove(fact1b));
        assertFalse(facts.contains(fact1));
        final String fact2 = facts.iterator().next();

        logger.info("UPDATING VERSION TO v4.0");
        updatePolicy(YAML, "v4.0");

        /*
         * Let rules update Params objects. The Params for the first set of rules should
         * now be deleted and replaced with a new one, while the Params for the second set
         * should be unchanged.
         */
        kieSession.fireAllRules();
        facts = getSessionObjects();
        assertEquals(specifications.length, facts.size());
        assertTrue(facts.remove(fact1b));
        assertFalse(facts.contains(fact2));
        final String fact3 = facts.iterator().next();

        logger.info("UPDATING VERSION TO v4.0 (i.e., unchanged)");
        updatePolicy(YAML, "v4.0");

        /*
         * Let rules update Params objects. As the version (and YAML) are unchanged for
         * either rule set, both Params objects should be unchanged.
         */
        kieSession.fireAllRules();
        facts = getSessionObjects();
        assertEquals(specifications.length, facts.size());
        assertTrue(facts.remove(fact1b));
        assertTrue(facts.contains(fact3));
    }

    /**
     * Updates the policy, changing the YAML associated with the first rule set.
     *
     * @param yamlFile name of the YAML file
     * @param policyVersion policy version
     * @throws IOException if an error occurs
     */
    private static void updatePolicy(String yamlFile, String policyVersion) throws IOException {

        specifications[0] = new Util.RuleSpec(DROOLS_TEMPLATE, CONTROL_LOOP_NAME, POLICY_SCOPE, POLICY_NAME,
                        policyVersion, loadYaml(yamlFile));

        /*
         * Update the policy within the container.
         */
        Util.updateContainer(policyVersion, specifications);
    }

    /**
     * Loads a YAML file and URL-encodes it.
     *
     * @param yamlFile name of the YAML file
     * @return the contents of the specified file, URL-encoded
     * @throws UnsupportedEncodingException if an error occurs
     */
    private static String loadYaml(String yamlFile) throws UnsupportedEncodingException {
        pair = Util.loadYaml(yamlFile);
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
    private static Set<String> getSessionObjects() {
        // use a TreeSet so order is maintained
        Set<String> set = new TreeSet<>();

        for (Object obj : kieSession.getObjects()) {
            set.add(obj.toString());
        }

        return set;
    }
}
