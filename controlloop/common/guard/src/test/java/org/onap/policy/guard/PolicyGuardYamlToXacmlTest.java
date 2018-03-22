/*-
 * ============LICENSE_START=======================================================
 * guard
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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
        matchParameters.setRecipe("GoToOz");
        List<String> targets = new ArrayList<>();
        targets.add("Wizard");
        targets.add("WickedWitchOfTheWest");
        matchParameters.setTargets(targets);
        GuardPolicy guardPolicy = new GuardPolicy();
        guardPolicy.setMatch_parameters(matchParameters);
        Constraint limitConstraint = new Constraint();
        limitConstraint.setFreq_limit_per_target(5);
        Map<String, String> timeWindow = new HashMap<>();
        timeWindow.put("value", "10");
        timeWindow.put("units", "hours");
        limitConstraint.setTime_window(timeWindow);
        Map<String, String> activeTimeRange = new HashMap<>();
        activeTimeRange.put("start", "someStartTime");
        activeTimeRange.put("end", "someEndTime");
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
        File tempYamlFile = File.createTempFile("ONAPPF", "yaml");
        File tempXacmlTemplateFile = new File("src/test/resources/frequency_limiter_template.xml");
        File tempXacmlOutputFile = File.createTempFile("ONAPPF", ".out.xacml");

        Yaml clYaml = new Yaml(new Constructor(ControlLoopGuard.class));
        String clYamlString = clYaml.dump(clGuard);

        TextFileUtils.putStringAsFile(clYamlString, tempYamlFile);
        PolicyGuardYamlToXacml.fromYamlToXacml(tempYamlFile.getCanonicalPath(),
                tempXacmlTemplateFile.getCanonicalPath(), tempXacmlOutputFile.getCanonicalPath());

        String result = TextFileUtils.getTextFileAsString(tempXacmlOutputFile.getCanonicalPath());

        // Assert no mote "${}" are left
        assertFalse(result.contains("${"));
        assertFalse(result.contains("}"));
        // Assert all substitutions are made
        assertTrue(result.contains("cl"));
        assertTrue(result.contains("actor"));
        assertTrue(result.contains("GoToOz"));
        assertTrue(result.contains("Wizard"));
        assertTrue(result.contains("WickedWitchOfTheWest"));
        assertTrue(result.contains("10"));
        assertTrue(result.contains("hours"));
        assertTrue(result.contains("someStartTime"));
        assertTrue(result.contains("someEndTime"));

        tempYamlFile.delete();
        tempXacmlOutputFile.delete();
    }

    @Test
    public void testGenerateXacmlGuardPartial() throws IOException {
        final File tempYamlFile = File.createTempFile("ONAPPF", "yaml");
        final File tempXacmlTemplateFile = new File("src/test/resources/frequency_limiter_template.xml");
        final File tempXacmlOutputFile = File.createTempFile("ONAPPF", ".out.xacml");

        clGuard.getGuards().getFirst().getMatch_parameters().setControlLoopName(null);
        clGuard.getGuards().getFirst().getMatch_parameters().setActor(null);
        clGuard.getGuards().getFirst().getMatch_parameters().setRecipe(null);
        clGuard.getGuards().getFirst().getMatch_parameters().setTargets(null);

        Yaml clYaml = new Yaml(new Constructor(ControlLoopGuard.class));
        String clYamlString = clYaml.dump(clGuard);

        TextFileUtils.putStringAsFile(clYamlString, tempYamlFile);
        PolicyGuardYamlToXacml.fromYamlToXacml(tempYamlFile.getCanonicalPath(),
                tempXacmlTemplateFile.getCanonicalPath(), tempXacmlOutputFile.getCanonicalPath());

        String result = TextFileUtils.getTextFileAsString(tempXacmlOutputFile.getCanonicalPath());

        // Assert no mote "${}" are left
        assertFalse(result.contains("${"));
        assertFalse(result.contains("}"));
        // Assert all substitutions are made
        assertTrue(result.contains("cl"));
        assertTrue(result.contains("actor"));
        assertFalse(result.contains("GoToOz"));
        assertFalse(result.contains("Wizard"));
        assertFalse(result.contains("WickedWitchOfTheWest"));
        assertTrue(result.contains("10"));
        assertTrue(result.contains("hours"));
        assertTrue(result.contains("someStartTime"));
        assertTrue(result.contains("someEndTime"));

        tempYamlFile.delete();
        tempXacmlOutputFile.delete();
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
        final File tempYamlFile = File.createTempFile("ONAPPF", "yaml");
        final File tempXacmlTemplateFile = new File("src/test/resources/blacklist_template.xml");
        final File tempXacmlOutputFile = File.createTempFile("ONAPPF", ".out.xacml");

        List<String> blacklist = new ArrayList<>();
        blacklist.add("WestWitches");
        blacklist.add("EastWitches");
        clGuard.getGuards().getFirst().getLimit_constraints().getFirst().setBlacklist(blacklist);

        Yaml clYaml = new Yaml(new Constructor(ControlLoopGuard.class));
        String clYamlString = clYaml.dump(clGuard);

        TextFileUtils.putStringAsFile(clYamlString, tempYamlFile);
        PolicyGuardYamlToXacml.fromYamlToXacmlBlacklist(tempYamlFile.getCanonicalPath(),
                tempXacmlTemplateFile.getCanonicalPath(), tempXacmlOutputFile.getCanonicalPath());

        String result = TextFileUtils.getTextFileAsString(tempXacmlOutputFile.getCanonicalPath());
        System.err.println(result);
        // Assert no mote "${}" are left
        assertFalse(result.contains("${"));
        assertFalse(result.contains("}"));
        // Assert all substitutions are made
        assertTrue(result.contains("WestWitches"));
        assertTrue(result.contains("EastWitches"));

        tempYamlFile.delete();
        tempXacmlOutputFile.delete();
    }

    @Test
    public void testGenerateXacmlGuardBlacklistPartial() throws IOException {
        final File tempYamlFile = File.createTempFile("ONAPPF", "yaml");
        final File tempXacmlTemplateFile = new File("src/test/resources/blacklist_template.xml");
        final File tempXacmlOutputFile = File.createTempFile("ONAPPF", ".out.xacml");

        List<String> blacklist = new ArrayList<>();
        blacklist.add("WestWitches");
        blacklist.add("EastWitches");
        clGuard.getGuards().getFirst().getLimit_constraints().getFirst().setBlacklist(blacklist);

        clGuard.getGuards().getFirst().getMatch_parameters().setControlLoopName(null);
        clGuard.getGuards().getFirst().getMatch_parameters().setActor(null);
        clGuard.getGuards().getFirst().getMatch_parameters().setRecipe(null);
        clGuard.getGuards().getFirst().getMatch_parameters().setTargets(null);

        Yaml clYaml = new Yaml(new Constructor(ControlLoopGuard.class));
        String clYamlString = clYaml.dump(clGuard);

        TextFileUtils.putStringAsFile(clYamlString, tempYamlFile);
        PolicyGuardYamlToXacml.fromYamlToXacmlBlacklist(tempYamlFile.getCanonicalPath(),
                tempXacmlTemplateFile.getCanonicalPath(), tempXacmlOutputFile.getCanonicalPath());

        String result = TextFileUtils.getTextFileAsString(tempXacmlOutputFile.getCanonicalPath());
        System.err.println(result);
        // Assert no mote "${}" are left
        assertFalse(result.contains("${"));
        assertFalse(result.contains("}"));
        // Assert all substitutions are made
        assertTrue(result.contains("WestWitches"));
        assertTrue(result.contains("EastWitches"));

        tempYamlFile.delete();
        tempXacmlOutputFile.delete();
    }
}
