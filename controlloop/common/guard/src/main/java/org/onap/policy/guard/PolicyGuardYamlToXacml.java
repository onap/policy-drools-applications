/*
 *                        AT&T - PROPRIETARY
 *          THIS FILE CONTAINS PROPRIETARY INFORMATION OF
 *        AT&T AND IS NOT TO BE DISCLOSED OR USED EXCEPT IN
 *             ACCORDANCE WITH APPLICABLE AGREEMENTS.
 *
 *          Copyright (c) 2016 AT&T Knowledge Ventures
 *              Unpublished and Not for Publication
 *                     All Rights Reserved
 */
package org.onap.policy.guard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.onap.policy.controlloop.policy.guard.ControlLoopGuard;



public class PolicyGuardYamlToXacml {
	
	
	public static void fromYamlToXacml(String yamlFile, String xacmlTemplate, String xacmlPolicyOutput){
		
		ControlLoopGuard yamlGuardObject = Util.loadYamlGuard(yamlFile);
		System.out.println("clname: " + yamlGuardObject.guards.getFirst().match_parameters.controlLoopName);
		System.out.println("actor: " + yamlGuardObject.guards.getFirst().match_parameters.actor);
		System.out.println("recipe: " + yamlGuardObject.guards.getFirst().match_parameters.recipe);
		System.out.println("num: " + yamlGuardObject.guards.getFirst().limit_constraints.getFirst().freq_limit_per_target);
		System.out.println("duration: " + yamlGuardObject.guards.getFirst().limit_constraints.getFirst().time_window);
		System.out.println("time_in_range: " + yamlGuardObject.guards.getFirst().limit_constraints.getFirst().active_time_range);
		
		Path xacmlTemplatePath = Paths.get(xacmlTemplate);
        String xacmlTemplateContent;
		
        try {
			xacmlTemplateContent = new String(Files.readAllBytes(xacmlTemplatePath));
			
	        String xacmlPolicyContent = generateXacmlGuard(xacmlTemplateContent,
	        		yamlGuardObject.guards.getFirst().match_parameters.controlLoopName,
	        		yamlGuardObject.guards.getFirst().match_parameters.actor,
	        		yamlGuardObject.guards.getFirst().match_parameters.recipe,
	        		yamlGuardObject.guards.getFirst().match_parameters.targets,
	        		yamlGuardObject.guards.getFirst().limit_constraints.getFirst().freq_limit_per_target,
	        		yamlGuardObject.guards.getFirst().limit_constraints.getFirst().time_window,
	        		yamlGuardObject.guards.getFirst().limit_constraints.getFirst().active_time_range.get("start"),
	        		yamlGuardObject.guards.getFirst().limit_constraints.getFirst().active_time_range.get("end")
	        		);
	        
	
	        Files.write(Paths.get(xacmlPolicyOutput), xacmlPolicyContent.getBytes());
        
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	
	public static String	generateXacmlGuard(String xacmlFileContent, 
			String clname,
			String actor, 
			String recipe,
			LinkedList<String> targets,
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
		System.out.println(xacmlFileContent);

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
	
	public static boolean isNullOrEmptyList(LinkedList<String> list){
		
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
		System.out.println("actor: " + yamlGuardObject.guards.getFirst().match_parameters.actor);
		System.out.println("recipe: " + yamlGuardObject.guards.getFirst().match_parameters.recipe);
		System.out.println("freq_limit_per_target: " + yamlGuardObject.guards.getFirst().limit_constraints.getFirst().freq_limit_per_target);
		System.out.println("time_window: " + yamlGuardObject.guards.getFirst().limit_constraints.getFirst().time_window);
		System.out.println("active_time_range: " + yamlGuardObject.guards.getFirst().limit_constraints.getFirst().active_time_range);
		
		Path xacmlTemplatePath = Paths.get(xacmlTemplate);
        String xacmlTemplateContent;
		
        try {
			xacmlTemplateContent = new String(Files.readAllBytes(xacmlTemplatePath));
			
	        String xacmlPolicyContent = generateXacmlGuardBlacklist(xacmlTemplateContent,
	        		yamlGuardObject.guards.getFirst().match_parameters.controlLoopName,
	        		yamlGuardObject.guards.getFirst().match_parameters.actor,
	        		yamlGuardObject.guards.getFirst().match_parameters.recipe,
	        		yamlGuardObject.guards.getFirst().limit_constraints.getFirst().blacklist,
	        		yamlGuardObject.guards.getFirst().limit_constraints.getFirst().active_time_range.get("start"),
	        		yamlGuardObject.guards.getFirst().limit_constraints.getFirst().active_time_range.get("end")
	        		);
	        
	
	        Files.write(Paths.get(xacmlPolicyOutput), xacmlPolicyContent.getBytes());
        
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
