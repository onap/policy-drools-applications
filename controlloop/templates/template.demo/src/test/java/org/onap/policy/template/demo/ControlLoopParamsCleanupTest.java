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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.drools.utils.logging.LoggerUtil;
import org.onap.policy.template.demo.SupportUtil.Pair;
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

    private static final String POLICY_SCOPE = "type=operational";

    private static final String CONTROL_LOOP_NAME = "ControlLoop-Params-Cleanup-Test";

    private static final String DROOLS_TEMPLATE = "../archetype-cl-amsterdam/src/main/resources/archetype-resources/"
                    + "src/main/resources/__closedLoopControlName__.drl";

    // values specific to the second copy of the rules

    private static final String YAML_B = "src/test/resources/yaml/policy_ControlLoop_ParamsCleanup-test-B.yaml";
    private static final String POLICY_NAME_B = "CL_CleanupTest_B";
    private static final String CONTROL_LOOP_NAME_B = "ControlLoop-Params-Cleanup-Test-B";

    private static KieSession kieSession;
    private static SupportUtil.RuleSpec[] specifications;

    /**
     * Setup the simulator.
     */
    @BeforeClass
    public static void setUpSimulator() {
        LoggerUtil.setLevel(LoggerUtil.ROOT_LOGGER, "INFO");

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
    }

    @Test
    public void test() throws IOException {

        /*
         * Let rules create Params objects. There should be one object for each set of
         * rules.
         */
        kieSession.fireAllRules();
        List<Object> facts = getSessionObjects();
        assertEquals(specifications.length, facts.size());
        Iterator<Object> iter = facts.iterator();

        final Object fact1 = iter.next();
        assertTrue(fact1.toString().contains(loadYaml(YAML)));

        final Object fact1b = iter.next();
        assertTrue(fact1b.toString().contains(loadYaml(YAML_B)));

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
        iter = facts.iterator();

        final Object fact2 = iter.next();
        assertTrue(fact2 != fact1);
        assertTrue(fact2 != fact1b);
        assertTrue(fact2.toString().contains(loadYaml(YAML2)));

        assertTrue(iter.next() == fact1b);

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
        iter = facts.iterator();

        final Object fact3 = iter.next();
        assertTrue(fact3.toString().contains(loadYaml(YAML)));
        assertTrue(fact3 != fact2);
        assertTrue(fact3 != fact1b);

        assertTrue(iter.next() == fact1b);

        logger.info("UPDATING VERSION TO v4.0 (i.e., unchanged)");
        updatePolicy(YAML, "v4.0");

        /*
         * Let rules update Params objects. As the version (and YAML) are unchanged for
         * either rule set, both Params objects should be unchanged.
         */
        kieSession.fireAllRules();
        facts = getSessionObjects();
        assertEquals(specifications.length, facts.size());
        iter = facts.iterator();
        assertTrue(iter.next() == fact3);
        assertTrue(iter.next() == fact1b);
        
        /*
         * Now we'll delete the first rule set.  That won't actually have any immediate
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
        assertEquals(specs.length, facts.size());
        iter = facts.iterator();
        assertTrue(iter.next().toString().contains(CONTROL_LOOP_NAME_B));
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

        return lst;
    }
}
