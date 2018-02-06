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

	public static void fromYamlToXacml(String yamlFile, String xacmlTemplate, String xacmlPolicyOutput) {
		ControlLoopGuard yamlGuardObject = Util.loadYamlGuard(yamlFile);
		logger.debug("clname: {}", yamlGuardObject.getGuards().getFirst().getMatch_parameters().getControlLoopName());
		logger.debug("actor: {}", yamlGuardObject.getGuards().getFirst().getMatch_parameters().getActor());
		logger.debug("recipe: {}", yamlGuardObject.getGuards().getFirst().getMatch_parameters().getRecipe());
		logger.debug("num: {}", yamlGuardObject.getGuards().getFirst().getLimit_constraints().getFirst().getFreq_limit_per_target());
		logger.debug("duration: {}", yamlGuardObject.getGuards().getFirst().getLimit_constraints().getFirst().getTime_window());
		logger.debug("time_in_range: {}", yamlGuardObject.getGuards().getFirst().getLimit_constraints().getFirst().getActive_time_range());

		Path xacmlTemplatePath = Paths.get(xacmlTemplate);
		String xacmlTemplateContent;

		try {
			xacmlTemplateContent = new String(Files.readAllBytes(xacmlTemplatePath));

			String xacmlPolicyContent = generateXACMLGuard(xacmlTemplateContent,
					yamlGuardObject.getGuards().getFirst().getMatch_parameters(),
					yamlGuardObject.getGuards().getFirst().getLimit_constraints().getFirst()
					);

			Files.write(Paths.get(xacmlPolicyOutput), xacmlPolicyContent.getBytes());

		} catch (IOException e) {
			logger.error("fromYamlToXacml threw: ", e);
		}
	}

	private static String generateXACMLGuard(String xacmlTemplateContent, MatchParameters matchParameters, Constraint constraint) {
		Pattern p = Pattern.compile("\\$\\{clname\\}");
		Matcher m = p.matcher(xacmlTemplateContent);
		if (isNullOrEmpty(matchParameters.getControlLoopName())) matchParameters.setControlLoopName(".*");
		xacmlTemplateContent = m.replaceAll(matchParameters.getControlLoopName());

		p = Pattern.compile("\\$\\{actor\\}");
		m = p.matcher(xacmlTemplateContent);
		if(isNullOrEmpty(matchParameters.getActor())) matchParameters.setActor(".*");
		xacmlTemplateContent = m.replaceAll(matchParameters.getActor());

		p = Pattern.compile("\\$\\{recipe\\}");
		m = p.matcher(xacmlTemplateContent);
		if(isNullOrEmpty(matchParameters.getRecipe())) matchParameters.setRecipe(".*");
		xacmlTemplateContent = m.replaceAll(matchParameters.getRecipe());

		p = Pattern.compile("\\$\\{targets\\}");
		m = p.matcher(xacmlTemplateContent);
		String targetsRegex = "";
		if(isNullOrEmptyList(matchParameters.getTargets())) { 
			targetsRegex = ".*";
		}
		else {
			StringBuilder targetsRegexSB = new StringBuilder();
			boolean addBarChar = false;
			for (String t : matchParameters.getTargets()){
				targetsRegexSB.append(t);
				if (addBarChar) {
					targetsRegexSB.append("|");
				}
				else {
					addBarChar = true;
				}
			}
			targetsRegex = targetsRegexSB.toString();
		}
		xacmlTemplateContent = m.replaceAll(targetsRegex);

		p = Pattern.compile("\\$\\{limit\\}");
		m = p.matcher(xacmlTemplateContent);
		xacmlTemplateContent = m.replaceAll(constraint.getFreq_limit_per_target().toString());

		p = Pattern.compile("\\$\\{twValue\\}");
		m = p.matcher(xacmlTemplateContent);
		xacmlTemplateContent = m.replaceAll(constraint.getTime_window().get("value"));

		p = Pattern.compile("\\$\\{twUnits\\}");
		m = p.matcher(xacmlTemplateContent);
		xacmlTemplateContent = m.replaceAll(constraint.getTime_window().get("units"));


		p = Pattern.compile("\\$\\{guardActiveStart\\}");
		m = p.matcher(xacmlTemplateContent);
		xacmlTemplateContent = m.replaceAll(constraint.getActive_time_range().get("start"));

		p = Pattern.compile("\\$\\{guardActiveEnd\\}");
		m = p.matcher(xacmlTemplateContent);
		xacmlTemplateContent = m.replaceAll(constraint.getActive_time_range().get("end"));
		logger.debug(xacmlTemplateContent);

		return xacmlTemplateContent;
	}

	public static boolean isNullOrEmpty(String s) {
		return s == null || s.trim().isEmpty();
	}

	public static boolean isNullOrEmptyList(List<String> list){
		return list == null || list.isEmpty();
	}

	public static void fromYamlToXacmlBlacklist(String yamlFile, String xacmlTemplate, String xacmlPolicyOutput){
		ControlLoopGuard yamlGuardObject = Util.loadYamlGuard(yamlFile);
		logger.debug("actor: {}", yamlGuardObject.getGuards().getFirst().getMatch_parameters().getActor());
		logger.debug("recipe: {}", yamlGuardObject.getGuards().getFirst().getMatch_parameters().getRecipe());
		logger.debug("freq_limit_per_target: {}", yamlGuardObject.getGuards().getFirst().getLimit_constraints().getFirst().getFreq_limit_per_target());
		logger.debug("time_window: {}", yamlGuardObject.getGuards().getFirst().getLimit_constraints().getFirst().getTime_window());
		logger.debug("active_time_range: {}", yamlGuardObject.getGuards().getFirst().getLimit_constraints().getFirst().getActive_time_range());

		Path xacmlTemplatePath = Paths.get(xacmlTemplate);
		String xacmlTemplateContent;

		try {
			xacmlTemplateContent = new String(Files.readAllBytes(xacmlTemplatePath));
			String xacmlPolicyContent = generateXacmlGuardBlacklist(xacmlTemplateContent,
					yamlGuardObject.getGuards().getFirst().getMatch_parameters(),
					yamlGuardObject.getGuards().getFirst().getLimit_constraints().getFirst()
					);

			Files.write(Paths.get(xacmlPolicyOutput), xacmlPolicyContent.getBytes());

		} catch (IOException e) {
			logger.error("fromYamlToXacmlBlacklist threw: ", e);
		}
	}

	private static String generateXacmlGuardBlacklist(String xacmlTemplateContent, MatchParameters matchParameters, Constraint constraint) {
		Pattern p = Pattern.compile("\\$\\{clname\\}");
		Matcher m = p.matcher(xacmlTemplateContent);
		if(isNullOrEmpty(matchParameters.getControlLoopName())) matchParameters.setControlLoopName(".*");
		xacmlTemplateContent = m.replaceAll(matchParameters.getControlLoopName());

		p = Pattern.compile("\\$\\{actor\\}");
		m = p.matcher(xacmlTemplateContent);
		if(isNullOrEmpty(matchParameters.getActor())) matchParameters.setActor(".*");
		xacmlTemplateContent = m.replaceAll(matchParameters.getActor());

		p = Pattern.compile("\\$\\{recipe\\}");
		m = p.matcher(xacmlTemplateContent);
		if(isNullOrEmpty(matchParameters.getRecipe())) matchParameters.setRecipe(".*");
		xacmlTemplateContent = m.replaceAll(matchParameters.getRecipe());

		p = Pattern.compile("\\$\\{guardActiveStart\\}");
		m = p.matcher(xacmlTemplateContent);
		xacmlTemplateContent = m.replaceAll(constraint.getActive_time_range().get("start"));

		p = Pattern.compile("\\$\\{guardActiveEnd\\}");
		m = p.matcher(xacmlTemplateContent);
		xacmlTemplateContent = m.replaceAll(constraint.getActive_time_range().get("end"));
		logger.debug(xacmlTemplateContent);

		for(String target : constraint.getBlacklist()){
			p = Pattern.compile("\\$\\{blackListElement\\}");
			m = p.matcher(xacmlTemplateContent);
			xacmlTemplateContent = m.replaceAll("<AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">" 
					+ target
					+ "</AttributeValue>"
					+ "\n\t\t\t\t\t\t\\$\\{blackListElement\\}\n");
		}

		p = Pattern.compile("\t\t\t\t\t\t\\$\\{blackListElement\\}\n");
		m = p.matcher(xacmlTemplateContent);
		xacmlTemplateContent = m.replaceAll("");


		return xacmlTemplateContent;
	}
}
