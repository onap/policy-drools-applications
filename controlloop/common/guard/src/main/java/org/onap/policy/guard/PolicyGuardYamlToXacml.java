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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.onap.policy.controlloop.policy.guard.ControlLoopGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class PolicyGuardYamlToXacml {
	
	private static final Logger logger = LoggerFactory.getLogger(PolicyGuardYamlToXacml.class);
	
	public static void fromYamlToXacml(String yamlFile, String xacmlTemplate, String xacmlPolicyOutput){
		
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
			
	        String xacmlPolicyContent = generateXacmlGuard(xacmlTemplateContent,
	        		yamlGuardObject.getGuards().getFirst().getMatch_parameters().getControlLoopName(),
	        		yamlGuardObject.getGuards().getFirst().getMatch_parameters().getActor(),
	        		yamlGuardObject.getGuards().getFirst().getMatch_parameters().getRecipe(),
	        		yamlGuardObject.getGuards().getFirst().getMatch_parameters().getTargets(),
	        		yamlGuardObject.getGuards().getFirst().getLimit_constraints().getFirst().getFreq_limit_per_target(),
	        		yamlGuardObject.getGuards().getFirst().getLimit_constraints().getFirst().getTime_window(),
	        		yamlGuardObject.getGuards().getFirst().getLimit_constraints().getFirst().getActive_time_range().get("start"),
	        		yamlGuardObject.getGuards().getFirst().getLimit_constraints().getFirst().getActive_time_range().get("end")
	        		);
	        
	
	        Files.write(Paths.get(xacmlPolicyOutput), xacmlPolicyContent.getBytes());
        
		} catch (IOException e) {
			logger.error("fromYamlToXacml threw: ", e);
		}
		
	}
	
	
	
	public static String	generateXacmlGuard(String xacmlFileContent, 
			String clname,
			String actor, 
			String recipe,
			List<String> targets,
			Integer limit,
			Map<String,String> timeWindow,
			String guardActiveStart, 
			String guardActiveEnd) {

		Pattern p = Pattern.compile("\\$\\{clname\\}");
		Matcher m = p.matcher(xacmlFileContent);
		if(isNullOrEmpty(clname)) clname = ".*";
		xacmlFileContent = m.replaceAll(clname);
		
		p = Pattern.compile("\\$\\{actor\\}");
		m = p.matcher(xacmlFileContent);
		if(isNullOrEmpty(actor)) actor = ".*";
		xacmlFileContent = m.replaceAll(actor);

		p = Pattern.compile("\\$\\{recipe\\}");
		m = p.matcher(xacmlFileContent);
		if(isNullOrEmpty(recipe)) recipe = ".*";
		xacmlFileContent = m.replaceAll(recipe);
		
		p = Pattern.compile("\\$\\{targets\\}");
		m = p.matcher(xacmlFileContent);
		String targetsRegex = "";
		if(isNullOrEmptyList(targets)){ 
			targetsRegex = ".*";
		}
		else{
			for(String t : targets){
				targetsRegex += (t + "|");
				
			}
			targetsRegex = targetsRegex.substring(0, targetsRegex.length()-1);
		}
		xacmlFileContent = m.replaceAll(targetsRegex);

		p = Pattern.compile("\\$\\{limit\\}");
		m = p.matcher(xacmlFileContent);
		xacmlFileContent = m.replaceAll(limit.toString());
		
		
		//p = Pattern.compile("\\$\\{timeWindow\\}");
		//m = p.matcher(xacmlFileContent);
		//xacmlFileContent = m.replaceAll("tw"+timeWindow);
		
		p = Pattern.compile("\\$\\{twValue\\}");
		m = p.matcher(xacmlFileContent);
		xacmlFileContent = m.replaceAll(timeWindow.get("value"));
		
		p = Pattern.compile("\\$\\{twUnits\\}");
		m = p.matcher(xacmlFileContent);
		xacmlFileContent = m.replaceAll(timeWindow.get("units"));
		

		p = Pattern.compile("\\$\\{guardActiveStart\\}");
		m = p.matcher(xacmlFileContent);
		xacmlFileContent = m.replaceAll(guardActiveStart);

		p = Pattern.compile("\\$\\{guardActiveEnd\\}");
		m = p.matcher(xacmlFileContent);
		xacmlFileContent = m.replaceAll(guardActiveEnd);
		logger.debug(xacmlFileContent);

		return xacmlFileContent;
	}
	
	public static boolean isNullOrEmpty(String s){
		
		if(s == null){
			return true;
		}
		else if(s.equals("")){
			return true;
		}
		return false;
		
	}
	
	public static boolean isNullOrEmptyList(List<String> list){
		
		if(list == null){
			return true;
		}
		else if(list.isEmpty()){
			return true;
		}
		return false;
		
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
	        		yamlGuardObject.getGuards().getFirst().getMatch_parameters().getControlLoopName(),
	        		yamlGuardObject.getGuards().getFirst().getMatch_parameters().getActor(),
	        		yamlGuardObject.getGuards().getFirst().getMatch_parameters().getRecipe(),
	        		yamlGuardObject.getGuards().getFirst().getLimit_constraints().getFirst().getBlacklist(),
	        		yamlGuardObject.getGuards().getFirst().getLimit_constraints().getFirst().getActive_time_range().get("start"),
	        		yamlGuardObject.getGuards().getFirst().getLimit_constraints().getFirst().getActive_time_range().get("end")
	        		);
	        
	
	        Files.write(Paths.get(xacmlPolicyOutput), xacmlPolicyContent.getBytes());
        
		} catch (IOException e) {
			logger.error("fromYamlToXacmlBlacklist threw: ", e);
		}
		
	}
	
	public static String	generateXacmlGuardBlacklist(String xacmlFileContent, 
			String clname,
			String actor, 
			String recipe, 
			List<String> blacklist,
			String guardActiveStart, 
			String guardActiveEnd) {

		
		Pattern p = Pattern.compile("\\$\\{clname\\}");
		Matcher m = p.matcher(xacmlFileContent);
		if(isNullOrEmpty(clname)) clname = ".*";
		xacmlFileContent = m.replaceAll(clname);
		
		p = Pattern.compile("\\$\\{actor\\}");
		m = p.matcher(xacmlFileContent);
		if(isNullOrEmpty(actor)) actor = ".*";
		xacmlFileContent = m.replaceAll(actor);

		p = Pattern.compile("\\$\\{recipe\\}");
		m = p.matcher(xacmlFileContent);
		if(isNullOrEmpty(recipe)) recipe = ".*";
		xacmlFileContent = m.replaceAll(recipe);
		
		p = Pattern.compile("\\$\\{guardActiveStart\\}");
		m = p.matcher(xacmlFileContent);
		xacmlFileContent = m.replaceAll(guardActiveStart);

		p = Pattern.compile("\\$\\{guardActiveEnd\\}");
		m = p.matcher(xacmlFileContent);
		xacmlFileContent = m.replaceAll(guardActiveEnd);
		logger.debug(xacmlFileContent);
		
		for(String target : blacklist){
			p = Pattern.compile("\\$\\{blackListElement\\}");
			m = p.matcher(xacmlFileContent);
			xacmlFileContent = m.replaceAll("<AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">" 
											+ target
											+ "</AttributeValue>"
											+ "\n\t\t\t\t\t\t\\$\\{blackListElement\\}");
		}
		
		p = Pattern.compile("\t\t\t\t\t\t\\$\\{blackListElement\\}\n");
		m = p.matcher(xacmlFileContent);
		xacmlFileContent = m.replaceAll("");
		
		
		return xacmlFileContent;
	}
	
	
}
