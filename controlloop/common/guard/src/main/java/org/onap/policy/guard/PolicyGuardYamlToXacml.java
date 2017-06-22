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


public class PolicyGuardYamlToXacml {
	
	
	public static void fromYamlToXacml(String yamlFile, String xacmlTemplate, String xacmlPolicyOutput){
		
		ControlLoopGuard yamlGuardObject = Util.loadYamlGuard(yamlFile);
		System.out.println("actor: " + yamlGuardObject.guards.getFirst().actor);
		System.out.println("recipe: " + yamlGuardObject.guards.getFirst().recipe);
		System.out.println("num: " + yamlGuardObject.guards.getFirst().limit_constraints.getFirst().num);
		System.out.println("duration: " + yamlGuardObject.guards.getFirst().limit_constraints.getFirst().duration);
		System.out.println("time_in_range: " + yamlGuardObject.guards.getFirst().limit_constraints.getFirst().time_in_range);
		
		Path xacmlTemplatePath = Paths.get(xacmlTemplate);
        String xacmlTemplateContent;
		
        try {
			xacmlTemplateContent = new String(Files.readAllBytes(xacmlTemplatePath));
			
	        String xacmlPolicyContent = generateXacmlGuard(xacmlTemplateContent,
	        		yamlGuardObject.guards.getFirst().actor,
	        		yamlGuardObject.guards.getFirst().recipe,
	        		yamlGuardObject.guards.getFirst().limit_constraints.getFirst().num,
	        		yamlGuardObject.guards.getFirst().limit_constraints.getFirst().duration,
	        		yamlGuardObject.guards.getFirst().limit_constraints.getFirst().time_in_range.get("arg2"),
	        		yamlGuardObject.guards.getFirst().limit_constraints.getFirst().time_in_range.get("arg3")
	        		);
	        
	
	        Files.write(Paths.get(xacmlPolicyOutput), xacmlPolicyContent.getBytes());
        
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	
	public static String	generateXacmlGuard(String xacmlFileContent, 
			String actor, 
			String recipe, 
			Integer limit,
			Map<String,String> timeWindow,
			String guardActiveStart, 
			String guardActiveEnd) {

		Pattern p = Pattern.compile("\\$\\{actor\\}");
		Matcher m = p.matcher(xacmlFileContent);
		xacmlFileContent = m.replaceAll(actor);

		p = Pattern.compile("\\$\\{recipe\\}");
		m = p.matcher(xacmlFileContent);
		xacmlFileContent = m.replaceAll(recipe);

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
		System.out.println(xacmlFileContent);

		return xacmlFileContent;
	}
	

	
	public static void fromYamlToXacmlBlacklist(String yamlFile, String xacmlTemplate, String xacmlPolicyOutput){
		
		ControlLoopGuard yamlGuardObject = Util.loadYamlGuard(yamlFile);
		System.out.println("actor: " + yamlGuardObject.guards.getFirst().actor);
		System.out.println("recipe: " + yamlGuardObject.guards.getFirst().recipe);
		System.out.println("num: " + yamlGuardObject.guards.getFirst().limit_constraints.getFirst().num);
		System.out.println("duration: " + yamlGuardObject.guards.getFirst().limit_constraints.getFirst().duration);
		System.out.println("time_in_range: " + yamlGuardObject.guards.getFirst().limit_constraints.getFirst().time_in_range);
		
		Path xacmlTemplatePath = Paths.get(xacmlTemplate);
        String xacmlTemplateContent;
		
        try {
			xacmlTemplateContent = new String(Files.readAllBytes(xacmlTemplatePath));
			
	        String xacmlPolicyContent = generateXacmlGuardBlacklist(xacmlTemplateContent,
	        		yamlGuardObject.guards.getFirst().actor,
	        		yamlGuardObject.guards.getFirst().recipe,
	        		yamlGuardObject.guards.getFirst().limit_constraints.getFirst().blacklist,
	        		yamlGuardObject.guards.getFirst().limit_constraints.getFirst().time_in_range.get("arg2"),
	        		yamlGuardObject.guards.getFirst().limit_constraints.getFirst().time_in_range.get("arg3")
	        		);
	        
	
	        Files.write(Paths.get(xacmlPolicyOutput), xacmlPolicyContent.getBytes());
        
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static String	generateXacmlGuardBlacklist(String xacmlFileContent, 
			String actor, 
			String recipe, 
			List<String> blacklist,
			String guardActiveStart, 
			String guardActiveEnd) {

		Pattern p = Pattern.compile("\\$\\{actor\\}");
		Matcher m = p.matcher(xacmlFileContent);
		xacmlFileContent = m.replaceAll(actor);

		p = Pattern.compile("\\$\\{recipe\\}");
		m = p.matcher(xacmlFileContent);
		xacmlFileContent = m.replaceAll(recipe);
		
		p = Pattern.compile("\\$\\{guardActiveStart\\}");
		m = p.matcher(xacmlFileContent);
		xacmlFileContent = m.replaceAll(guardActiveStart);

		p = Pattern.compile("\\$\\{guardActiveEnd\\}");
		m = p.matcher(xacmlFileContent);
		xacmlFileContent = m.replaceAll(guardActiveEnd);
		System.out.println(xacmlFileContent);
		
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
