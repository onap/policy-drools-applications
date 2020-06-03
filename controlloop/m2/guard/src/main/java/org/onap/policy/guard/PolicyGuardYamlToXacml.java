/*-
 * ============LICENSE_START=======================================================
 * guard
 * ================================================================================
 * Copyright (C) 2017, 2019 AT&T Intellectual Property. All rights reserved.
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
import java.util.function.Consumer;
import org.onap.policy.controlloop.policy.guard.Constraint;
import org.onap.policy.controlloop.policy.guard.ControlLoopGuard;
import org.onap.policy.controlloop.policy.guard.GuardPolicy;
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
        fromYamlToXacml(yamlFile, xacmlTemplate, xacmlPolicyOutput, PolicyGuardYamlToXacml::generateXacmlGuard,
            constraint -> {
                logger.debug("num: {}", constraint.getFreq_limit_per_target());
                logger.debug("duration: {}", constraint.getTime_window());
                logger.debug("time_in_range: {}", constraint.getActive_time_range());
            });
    }

    /**
     * Convert from Yaml to Xacml.
     *
     * @param yamlFile the Yaml file
     * @param xacmlTemplate the Xacml template
     * @param xacmlPolicyOutput the Xacml output
     * @param generator function to generate the yaml from the xacml
     * @param logConstraint function to log relevant fields of the constraint
     */
    public static void fromYamlToXacml(String yamlFile, String xacmlTemplate, String xacmlPolicyOutput,
                    Generator generator, Consumer<Constraint> logConstraint) {

        ControlLoopGuard yamlGuardObject = Util.loadYamlGuard(yamlFile);
        GuardPolicy guardPolicy = yamlGuardObject.getGuards().get(0);
        logger.debug("clname: {}", guardPolicy.getMatch_parameters().getControlLoopName());
        logger.debug("actor: {}", guardPolicy.getMatch_parameters().getActor());
        logger.debug("recipe: {}", guardPolicy.getMatch_parameters().getRecipe());
        Constraint constraint = guardPolicy.getLimit_constraints().get(0);
        logConstraint.accept(constraint);

        Path xacmlTemplatePath = Paths.get(xacmlTemplate);
        String xacmlTemplateContent;

        try {
            xacmlTemplateContent = new String(Files.readAllBytes(xacmlTemplatePath));

            String xacmlPolicyContent = generator.apply(xacmlTemplateContent,
                    guardPolicy.getMatch_parameters(), constraint);

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

        xacmlTemplateContent = doCommonReplacements(xacmlTemplateContent, matchParameters, constraint);

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
        xacmlTemplateContent = xacmlTemplateContent.replace("${targets}", targetsRegex);

        xacmlTemplateContent = xacmlTemplateContent.replace("${limit}",
                        constraint.getFreq_limit_per_target().toString());

        xacmlTemplateContent = xacmlTemplateContent.replace("${twValue}", constraint.getTime_window().get("value"));

        xacmlTemplateContent = xacmlTemplateContent.replace("${twUnits}", constraint.getTime_window().get("units"));

        logger.debug(xacmlTemplateContent);

        return xacmlTemplateContent;
    }

    private static String doCommonReplacements(String xacmlTemplateContent, MatchParameters matchParameters,
                    Constraint constraint) {

        replaceNullOrEmpty(matchParameters.getControlLoopName(), matchParameters::setControlLoopName, ".*");
        xacmlTemplateContent = xacmlTemplateContent.replace("${clname}", matchParameters.getControlLoopName());

        replaceNullOrEmpty(matchParameters.getActor(), matchParameters::setActor, ".*");
        xacmlTemplateContent = xacmlTemplateContent.replace("${actor}", matchParameters.getActor());

        replaceNullOrEmpty(matchParameters.getRecipe(), matchParameters::setRecipe, ".*");
        xacmlTemplateContent = xacmlTemplateContent.replace("${recipe}", matchParameters.getRecipe());

        xacmlTemplateContent = xacmlTemplateContent.replace("${guardActiveStart}",
                        constraint.getActive_time_range().get("start"));

        xacmlTemplateContent = xacmlTemplateContent.replace("${guardActiveEnd}",
                        constraint.getActive_time_range().get("end"));

        return xacmlTemplateContent;
    }

    private static void replaceNullOrEmpty(String text, Consumer<String> replacer, String newValue) {
        if (isNullOrEmpty(text)) {
            replacer.accept(newValue);
        }
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
        fromYamlToXacml(yamlFile, xacmlTemplate, xacmlPolicyOutput, PolicyGuardYamlToXacml::generateXacmlGuardBlacklist,
            constraint -> {
                logger.debug("freq_limit_per_target: {}", constraint.getFreq_limit_per_target());
                logger.debug("time_window: {}", constraint.getTime_window());
                logger.debug("active_time_range: {}", constraint.getActive_time_range());
            });
    }

    private static String generateXacmlGuardBlacklist(String xacmlTemplateContent, MatchParameters matchParameters,
            Constraint constraint) {

        String result = doCommonReplacements(xacmlTemplateContent, matchParameters, constraint);

        for (String target : constraint.getBlacklist()) {
            result = result.replace("${blackListElement}",
                            "<AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">" + target
                                            + "</AttributeValue>" + "\n\t\t\t\t\t\t\\${blackListElement}\n");
        }

        result = result.replace("\t\t\t\t\t\t\\${blackListElement}\n", "");

        return result;
    }

    @FunctionalInterface
    private static interface Generator {
        public String apply(String xacmlTemplateContent, MatchParameters matchParameters,
            Constraint constraint);
    }
}
