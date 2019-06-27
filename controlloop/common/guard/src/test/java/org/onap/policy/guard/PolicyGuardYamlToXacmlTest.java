/*-
 * ============LICENSE_START=======================================================
 * guard
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

package org.onap.policy.guard;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.onap.policy.controlloop.policy.guard.Constraint;
import org.onap.policy.controlloop.policy.guard.ControlLoopGuard;
import org.onap.policy.controlloop.policy.guard.GuardPolicy;
import org.onap.policy.controlloop.policy.guard.MatchParameters;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class PolicyGuardYamlToXacmlTest {
    private static final String SOME_START_TIME = "someStartTime";
    private static final String SOME_END_TIME = "someEndTime";
    private static final String HOURS = "hours";
    private static final String TARGET2 = "WickedWitchOfTheWest";
    private static final String TARGET1 = "Wizard";
    private static final String ONAPPF_FILE = "ONAPPF";
    private static final String RECIPE = "GoToOz";
    private static final String TEXT1 = "WestWitches";
    private static final String TEXT2 = "EastWitches";
    private static final String OUT_XACML = ".out.xacml";
    private ControlLoopGuard clGuard;

    /**
     * Set up test cases.
     */
    @Before
    public void createControlLoopGuard() {
        clGuard = new ControlLoopGuard();
        MatchParameters matchParameters = new MatchParameters();
        matchParameters.setControlLoopName("WizardOfOz");
        matchParameters.setActor("Dorothy");
        matchParameters.setRecipe(RECIPE);
        List<String> targets = new ArrayList<>();
        targets.add(TARGET1);
        targets.add(TARGET2);
        matchParameters.setTargets(targets);
        GuardPolicy guardPolicy = new GuardPolicy();
        guardPolicy.setMatch_parameters(matchParameters);
        Constraint limitConstraint = new Constraint();
        limitConstraint.setFreq_limit_per_target(5);
        Map<String, String> timeWindow = new HashMap<>();
        timeWindow.put("value", "10");
        timeWindow.put("units", HOURS);
        limitConstraint.setTime_window(timeWindow);
        Map<String, String> activeTimeRange = new HashMap<>();
        activeTimeRange.put("start", SOME_START_TIME);
        activeTimeRange.put("end", SOME_END_TIME);
        limitConstraint.setActive_time_range(activeTimeRange);
        LinkedList<Constraint> limitConstraints = new LinkedList<>();
        limitConstraints.add(limitConstraint);
        guardPolicy.setLimit_constraints(limitConstraints);
        LinkedList<GuardPolicy> guardList = new LinkedList<>();
        guardList.add(guardPolicy);
        clGuard.setGuards(guardList);
    }

    @Test
    public void testGenerateXacmlGuardFull() throws IOException {
        File tempYamlFile = File.createTempFile(ONAPPF_FILE, "yaml");
        tempYamlFile.deleteOnExit();

        File tempXacmlTemplateFile = new File("src/test/resources/frequency_limiter_template.xml");

        File tempXacmlOutputFile = File.createTempFile(ONAPPF_FILE, OUT_XACML);
        tempXacmlOutputFile.deleteOnExit();

        Yaml clYaml = new Yaml(new Constructor(ControlLoopGuard.class));
        String clYamlString = clYaml.dump(clGuard);

        SupportTextFileUtils.putStringAsFile(clYamlString, tempYamlFile);
        PolicyGuardYamlToXacml.fromYamlToXacml(tempYamlFile.getCanonicalPath(),
                tempXacmlTemplateFile.getCanonicalPath(), tempXacmlOutputFile.getCanonicalPath());

        String result = SupportTextFileUtils.getTextFileAsString(tempXacmlOutputFile.getCanonicalPath());

        // Assert no mote "${}" are left
        assertFalse(result.contains("${"));
        assertFalse(result.contains("}"));
        // Assert all substitutions are made
        assertTrue(result.contains("cl"));
        assertTrue(result.contains("actor"));
        assertTrue(result.contains(RECIPE));
        assertTrue(result.contains(TARGET1));
        assertTrue(result.contains(TARGET2));
        assertTrue(result.contains("10"));
        assertTrue(result.contains(HOURS));
        assertTrue(result.contains(SOME_START_TIME));
        assertTrue(result.contains(SOME_END_TIME));
    }

    @Test
    public void testGenerateXacmlGuardPartial() throws IOException {
        final File tempYamlFile = File.createTempFile(ONAPPF_FILE, "yaml");
        tempYamlFile.deleteOnExit();

        final File tempXacmlTemplateFile = new File("src/test/resources/frequency_limiter_template.xml");

        final File tempXacmlOutputFile = File.createTempFile(ONAPPF_FILE, OUT_XACML);
        tempXacmlOutputFile.deleteOnExit();

        MatchParameters matchParameters = clGuard.getGuards().get(0).getMatch_parameters();
        matchParameters.setControlLoopName(null);
        matchParameters.setActor(null);
        matchParameters.setRecipe(null);
        matchParameters.setTargets(null);

        Yaml clYaml = new Yaml(new Constructor(ControlLoopGuard.class));
        String clYamlString = clYaml.dump(clGuard);

        SupportTextFileUtils.putStringAsFile(clYamlString, tempYamlFile);
        PolicyGuardYamlToXacml.fromYamlToXacml(tempYamlFile.getCanonicalPath(),
                tempXacmlTemplateFile.getCanonicalPath(), tempXacmlOutputFile.getCanonicalPath());

        String result = SupportTextFileUtils.getTextFileAsString(tempXacmlOutputFile.getCanonicalPath());

        // Assert no mote "${}" are left
        assertFalse(result.contains("${"));
        assertFalse(result.contains("}"));
        // Assert all substitutions are made
        assertTrue(result.contains("cl"));
        assertTrue(result.contains("actor"));
        assertFalse(result.contains(RECIPE));
        assertFalse(result.contains(TARGET1));
        assertFalse(result.contains(TARGET2));
        assertTrue(result.contains("10"));
        assertTrue(result.contains(HOURS));
        assertTrue(result.contains(SOME_START_TIME));
        assertTrue(result.contains(SOME_END_TIME));
    }

    @Test
    public void testIsNullOrEmpty() {
        assertTrue(PolicyGuardYamlToXacml.isNullOrEmpty(""));
        assertTrue(PolicyGuardYamlToXacml.isNullOrEmpty(null));
        assertFalse(PolicyGuardYamlToXacml.isNullOrEmpty("hello"));
    }

    @Test
    public void testIsNullOrEmptyList() {
        List<String> list = new ArrayList<>();
        assertTrue(PolicyGuardYamlToXacml.isNullOrEmptyList(null));
        assertTrue(PolicyGuardYamlToXacml.isNullOrEmptyList(list));

        list.add("hello");
        assertFalse(PolicyGuardYamlToXacml.isNullOrEmptyList(list));
    }

    @Test
    public void testFromYamlToXacmlBlacklist() {
        // fail("Not yet implemented");
    }

    @Test
    public void testGenerateXacmlGuardBlacklist() throws IOException {
        final File tempYamlFile = File.createTempFile(ONAPPF_FILE, "yaml");
        tempYamlFile.deleteOnExit();

        final File tempXacmlTemplateFile = new File("src/test/resources/blacklist_template.xml");

        final File tempXacmlOutputFile = File.createTempFile(ONAPPF_FILE, OUT_XACML);
        tempXacmlOutputFile.deleteOnExit();

        List<String> blacklist = new ArrayList<>();
        blacklist.add(TEXT1);
        blacklist.add(TEXT2);
        clGuard.getGuards().get(0).getLimit_constraints().get(0).setBlacklist(blacklist);

        Yaml clYaml = new Yaml(new Constructor(ControlLoopGuard.class));
        String clYamlString = clYaml.dump(clGuard);

        SupportTextFileUtils.putStringAsFile(clYamlString, tempYamlFile);
        PolicyGuardYamlToXacml.fromYamlToXacmlBlacklist(tempYamlFile.getCanonicalPath(),
                tempXacmlTemplateFile.getCanonicalPath(), tempXacmlOutputFile.getCanonicalPath());

        String result = SupportTextFileUtils.getTextFileAsString(tempXacmlOutputFile.getCanonicalPath());
        // Assert no mote "${}" are left
        assertFalse(result.contains("${"));
        assertFalse(result.contains("}"));
        // Assert all substitutions are made
        assertTrue(result.contains(TEXT1));
        assertTrue(result.contains(TEXT2));
    }

    @Test
    public void testGenerateXacmlGuardBlacklistPartial() throws IOException {
        final File tempYamlFile = File.createTempFile(ONAPPF_FILE, "yaml");
        tempYamlFile.deleteOnExit();

        final File tempXacmlTemplateFile = new File("src/test/resources/blacklist_template.xml");

        final File tempXacmlOutputFile = File.createTempFile(ONAPPF_FILE, OUT_XACML);
        tempXacmlOutputFile.deleteOnExit();

        List<String> blacklist = new ArrayList<>();
        blacklist.add(TEXT1);
        blacklist.add(TEXT2);
        GuardPolicy guardPolicy = clGuard.getGuards().get(0);
        guardPolicy.getLimit_constraints().get(0).setBlacklist(blacklist);

        MatchParameters matchParameters = guardPolicy.getMatch_parameters();
        matchParameters.setControlLoopName(null);
        matchParameters.setActor(null);
        matchParameters.setRecipe(null);
        matchParameters.setTargets(null);

        Yaml clYaml = new Yaml(new Constructor(ControlLoopGuard.class));
        String clYamlString = clYaml.dump(clGuard);

        SupportTextFileUtils.putStringAsFile(clYamlString, tempYamlFile);
        PolicyGuardYamlToXacml.fromYamlToXacmlBlacklist(tempYamlFile.getCanonicalPath(),
                tempXacmlTemplateFile.getCanonicalPath(), tempXacmlOutputFile.getCanonicalPath());

        String result = SupportTextFileUtils.getTextFileAsString(tempXacmlOutputFile.getCanonicalPath());
        // Assert no mote "${}" are left
        assertFalse(result.contains("${"));
        assertFalse(result.contains("}"));
        // Assert all substitutions are made
        assertTrue(result.contains(TEXT1));
        assertTrue(result.contains(TEXT2));
    }
}
