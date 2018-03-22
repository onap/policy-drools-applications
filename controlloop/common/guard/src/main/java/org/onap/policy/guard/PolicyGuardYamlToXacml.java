/*-
 * ============LICENSE_START=======================================================
 * guard
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.onap.policy.controlloop.policy.guard.Constraint;
import org.onap.policy.controlloop.policy.guard.ControlLoopGuard;
import org.onap.policy.controlloop.policy.guard.MatchParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyGuardYamlToXacml {
    private static final Logger logger = LoggerFactory.getLogger(PolicyGuardYamlToXacml.class);

    private PolicyGuardYamlToXacml() {
        // Construction of this static class is not allowed
    }

    /**
     * Convert from Yaml to Xacml.
     * 
     * @param yamlFile the Yaml file
     * @param xacmlTemplate the Xacml template
     * @param xacmlPolicyOutput the Xacml output
     */
    public static void fromYamlToXacml(String yamlFile, String xacmlTemplate, String xacmlPolicyOutput) {
        ControlLoopGuard yamlGuardObject = Util.loadYamlGuard(yamlFile);
        logger.debug("clname: {}", yamlGuardObject.getGuards().getFirst().getMatch_parameters().getControlLoopName());
        logger.debug("actor: {}", yamlGuardObject.getGuards().getFirst().getMatch_parameters().getActor());
        logger.debug("recipe: {}", yamlGuardObject.getGuards().getFirst().getMatch_parameters().getRecipe());
        logger.debug("num: {}",
                yamlGuardObject.getGuards().getFirst().getLimit_constraints().getFirst().getFreq_limit_per_target());
        logger.debug("duration: {}",
                yamlGuardObject.getGuards().getFirst().getLimit_constraints().getFirst().getTime_window());
        logger.debug("time_in_range: {}",
                yamlGuardObject.getGuards().getFirst().getLimit_constraints().getFirst().getActive_time_range());

        Path xacmlTemplatePath = Paths.get(xacmlTemplate);
        String xacmlTemplateContent;

        try {
            xacmlTemplateContent = new String(Files.readAllBytes(xacmlTemplatePath));

            String xacmlPolicyContent = generateXacmlGuard(xacmlTemplateContent,
                    yamlGuardObject.getGuards().getFirst().getMatch_parameters(),
                    yamlGuardObject.getGuards().getFirst().getLimit_constraints().getFirst());

            Files.write(Paths.get(xacmlPolicyOutput), xacmlPolicyContent.getBytes());

        } catch (IOException e) {
            logger.error("fromYamlToXacml threw: ", e);
        }
    }

    /**
     * Generate a Xacml guard.
     * 
     * @param xacmlTemplateContent the Xacml template content
     * @param matchParameters the paremeters to use
     * @param constraint the constraint to use
     * @return the guard
     */
    private static String generateXacmlGuard(String xacmlTemplateContent, MatchParameters matchParameters,
            Constraint constraint) {
        Pattern pattern = Pattern.compile("\\$\\{clname\\}");
        Matcher matcher = pattern.matcher(xacmlTemplateContent);
        if (isNullOrEmpty(matchParameters.getControlLoopName())) {
            matchParameters.setControlLoopName(".*");
        }
        xacmlTemplateContent = matcher.replaceAll(matchParameters.getControlLoopName());

        pattern = Pattern.compile("\\$\\{actor\\}");
        matcher = pattern.matcher(xacmlTemplateContent);
        if (isNullOrEmpty(matchParameters.getActor())) {
            matchParameters.setActor(".*");
        }
        xacmlTemplateContent = matcher.replaceAll(matchParameters.getActor());

        pattern = Pattern.compile("\\$\\{recipe\\}");
        matcher = pattern.matcher(xacmlTemplateContent);
        if (isNullOrEmpty(matchParameters.getRecipe())) {
            matchParameters.setRecipe(".*");
        }
        xacmlTemplateContent = matcher.replaceAll(matchParameters.getRecipe());

        pattern = Pattern.compile("\\$\\{targets\\}");
        matcher = pattern.matcher(xacmlTemplateContent);
        String targetsRegex = "";
        if (isNullOrEmptyList(matchParameters.getTargets())) {
            targetsRegex = ".*";
        } else {
            StringBuilder targetsRegexSb = new StringBuilder();
            boolean addBarChar = false;
            for (String t : matchParameters.getTargets()) {
                targetsRegexSb.append(t);
                if (addBarChar) {
                    targetsRegexSb.append("|");
                } else {
                    addBarChar = true;
                }
            }
            targetsRegex = targetsRegexSb.toString();
        }
        xacmlTemplateContent = matcher.replaceAll(targetsRegex);

        pattern = Pattern.compile("\\$\\{limit\\}");
        matcher = pattern.matcher(xacmlTemplateContent);
        xacmlTemplateContent = matcher.replaceAll(constraint.getFreq_limit_per_target().toString());

        pattern = Pattern.compile("\\$\\{twValue\\}");
        matcher = pattern.matcher(xacmlTemplateContent);
        xacmlTemplateContent = matcher.replaceAll(constraint.getTime_window().get("value"));

        pattern = Pattern.compile("\\$\\{twUnits\\}");
        matcher = pattern.matcher(xacmlTemplateContent);
        xacmlTemplateContent = matcher.replaceAll(constraint.getTime_window().get("units"));


        pattern = Pattern.compile("\\$\\{guardActiveStart\\}");
        matcher = pattern.matcher(xacmlTemplateContent);
        xacmlTemplateContent = matcher.replaceAll(constraint.getActive_time_range().get("start"));

        pattern = Pattern.compile("\\$\\{guardActiveEnd\\}");
        matcher = pattern.matcher(xacmlTemplateContent);
        xacmlTemplateContent = matcher.replaceAll(constraint.getActive_time_range().get("end"));
        logger.debug(xacmlTemplateContent);

        return xacmlTemplateContent;
    }

    public static boolean isNullOrEmpty(String string) {
        return string == null || string.trim().isEmpty();
    }

    public static boolean isNullOrEmptyList(List<String> list) {
        return list == null || list.isEmpty();
    }

    /**
     * Convert from Yaml to Xacml blacklist.
     * 
     * @param yamlFile the Yaml file
     * @param xacmlTemplate the Xacml template
     * @param xacmlPolicyOutput the Xacml output
     */
    public static void fromYamlToXacmlBlacklist(String yamlFile, String xacmlTemplate, String xacmlPolicyOutput) {
        ControlLoopGuard yamlGuardObject = Util.loadYamlGuard(yamlFile);
        logger.debug("actor: {}", yamlGuardObject.getGuards().getFirst().getMatch_parameters().getActor());
        logger.debug("recipe: {}", yamlGuardObject.getGuards().getFirst().getMatch_parameters().getRecipe());
        logger.debug("freq_limit_per_target: {}",
                yamlGuardObject.getGuards().getFirst().getLimit_constraints().getFirst().getFreq_limit_per_target());
        logger.debug("time_window: {}",
                yamlGuardObject.getGuards().getFirst().getLimit_constraints().getFirst().getTime_window());
        logger.debug("active_time_range: {}",
                yamlGuardObject.getGuards().getFirst().getLimit_constraints().getFirst().getActive_time_range());

        Path xacmlTemplatePath = Paths.get(xacmlTemplate);
        String xacmlTemplateContent;

        try {
            xacmlTemplateContent = new String(Files.readAllBytes(xacmlTemplatePath));
            String xacmlPolicyContent = generateXacmlGuardBlacklist(xacmlTemplateContent,
                    yamlGuardObject.getGuards().getFirst().getMatch_parameters(),
                    yamlGuardObject.getGuards().getFirst().getLimit_constraints().getFirst());

            Files.write(Paths.get(xacmlPolicyOutput), xacmlPolicyContent.getBytes());

        } catch (IOException e) {
            logger.error("fromYamlToXacmlBlacklist threw: ", e);
        }
    }

    private static String generateXacmlGuardBlacklist(String xacmlTemplateContent, MatchParameters matchParameters,
            Constraint constraint) {
        Pattern pattern = Pattern.compile("\\$\\{clname\\}");
        Matcher matcher = pattern.matcher(xacmlTemplateContent);
        if (isNullOrEmpty(matchParameters.getControlLoopName())) {
            matchParameters.setControlLoopName(".*");
        }
        xacmlTemplateContent = matcher.replaceAll(matchParameters.getControlLoopName());

        pattern = Pattern.compile("\\$\\{actor\\}");
        matcher = pattern.matcher(xacmlTemplateContent);
        if (isNullOrEmpty(matchParameters.getActor())) {
            matchParameters.setActor(".*");
        }
        xacmlTemplateContent = matcher.replaceAll(matchParameters.getActor());

        pattern = Pattern.compile("\\$\\{recipe\\}");
        matcher = pattern.matcher(xacmlTemplateContent);
        if (isNullOrEmpty(matchParameters.getRecipe())) {
            matchParameters.setRecipe(".*");
        }
        xacmlTemplateContent = matcher.replaceAll(matchParameters.getRecipe());

        pattern = Pattern.compile("\\$\\{guardActiveStart\\}");
        matcher = pattern.matcher(xacmlTemplateContent);
        xacmlTemplateContent = matcher.replaceAll(constraint.getActive_time_range().get("start"));

        pattern = Pattern.compile("\\$\\{guardActiveEnd\\}");
        matcher = pattern.matcher(xacmlTemplateContent);
        xacmlTemplateContent = matcher.replaceAll(constraint.getActive_time_range().get("end"));
        logger.debug(xacmlTemplateContent);

        for (String target : constraint.getBlacklist()) {
            pattern = Pattern.compile("\\$\\{blackListElement\\}");
            matcher = pattern.matcher(xacmlTemplateContent);
            xacmlTemplateContent =
                    matcher.replaceAll("<AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">" + target
                            + "</AttributeValue>" + "\n\t\t\t\t\t\t\\$\\{blackListElement\\}\n");
        }

        pattern = Pattern.compile("\t\t\t\t\t\t\\$\\{blackListElement\\}\n");
        matcher = pattern.matcher(xacmlTemplateContent);
        xacmlTemplateContent = matcher.replaceAll("");


        return xacmlTemplateContent;
    }
}
