/*-
 * ============LICENSE_START=======================================================
 * demo
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

package org.onap.policy.template.demo;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.Results;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.controlloop.policy.guard.ControlLoopGuard;
import org.onap.policy.common.endpoints.http.server.HttpServletServer;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.guard.PolicyGuardYamlToXacml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.att.research.xacml.util.XACMLProperties;

public final class Util {

	private static final String OPSHISTPUPROP = "OperationsHistoryPU";
	private static final Logger logger = LoggerFactory.getLogger(Util.class);

	public static class Pair<A, B> {
		public final A a;
		public final B b;
		
		public Pair(A a, B b) {
			this.a = a;
			this.b = b;
		}
	}
	
	public static Pair<ControlLoopPolicy, String>	loadYaml(String testFile) {
		try (InputStream is = new FileInputStream(new File(testFile))) {
			String contents = IOUtils.toString(is, StandardCharsets.UTF_8);
			//
			// Read the yaml into our Java Object
			//
			Yaml yaml = new Yaml(new Constructor(ControlLoopPolicy.class));
			Object obj = yaml.load(contents);
			
			//String ttt = ((ControlLoopPolicy)obj).policies.getFirst().payload.get("asdas");
			logger.debug(contents);
			//for(Policy policy : ((ControlLoopPolicy)obj).policies){
			
			return new Pair<ControlLoopPolicy, String>((ControlLoopPolicy) obj, contents);
		} catch (FileNotFoundException e) {
			fail(e.getLocalizedMessage());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
		return null;
	}
	
	public static ControlLoopGuard	loadYamlGuard(String testFile) {
		try (InputStream is = new FileInputStream(new File(testFile))) {
			String contents = IOUtils.toString(is, StandardCharsets.UTF_8);
			//
			// Read the yaml into our Java Object
			//
			Yaml yaml = new Yaml(new Constructor(ControlLoopGuard.class));
			Object obj = yaml.load(contents);
			return (ControlLoopGuard) obj;
		} catch (FileNotFoundException e) {
			fail(e.getLocalizedMessage());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
		return null;
	}
	
	public static HttpServletServer buildAaiSim() throws InterruptedException, IOException {
		return org.onap.policy.simulators.Util.buildAaiSim();
	}
	
	public static HttpServletServer buildSoSim() throws InterruptedException, IOException {
		return org.onap.policy.simulators.Util.buildSoSim();
	}
	
	public static HttpServletServer buildVfcSim() throws InterruptedException, IOException {
		return org.onap.policy.simulators.Util.buildVfcSim();
	}
	
	public static HttpServletServer buildGuardSim() throws InterruptedException, IOException {
        return org.onap.policy.simulators.Util.buildGuardSim();
    }
	
	private static String	generatePolicy(String ruleContents, 
			String closedLoopControlName, 
			String policyScope, 
			String policyName, 
			String policyVersion, 
			String controlLoopYaml) {

		Pattern p = Pattern.compile("\\$\\{closedLoopControlName\\}");
		Matcher m = p.matcher(ruleContents);
		ruleContents = m.replaceAll(closedLoopControlName);

		p = Pattern.compile("\\$\\{policyScope\\}");
		m = p.matcher(ruleContents);
		ruleContents = m.replaceAll(policyScope);

		p = Pattern.compile("\\$\\{policyName\\}");
		m = p.matcher(ruleContents);
		ruleContents = m.replaceAll(policyName);

		p = Pattern.compile("\\$\\{policyVersion\\}");
		m = p.matcher(ruleContents);
		ruleContents = m.replaceAll(policyVersion);

		p = Pattern.compile("\\$\\{controlLoopYaml\\}");
		m = p.matcher(ruleContents);
		ruleContents = m.replaceAll(controlLoopYaml);

		return ruleContents;
	}

	public static KieSession buildContainer(String droolsTemplate, String closedLoopControlName, String policyScope, String policyName, String policyVersion, String yamlSpecification) throws IOException {
	   	//
    	// Get our Drools Kie factory
    	//
        KieServices ks = KieServices.Factory.get();
        
        KieModuleModel kModule = ks.newKieModuleModel();
        
        logger.debug("KMODULE:" + System.lineSeparator() + kModule.toXML());
        
        //
        // Generate our drools rule from our template
        //
        KieFileSystem kfs = ks.newKieFileSystem();
        
        kfs.writeKModuleXML(kModule.toXML());
        {
	        Path rule = Paths.get(droolsTemplate);
	        String ruleTemplate = new String(Files.readAllBytes(rule));
	        String drlContents = generatePolicy(ruleTemplate,
	        						closedLoopControlName,
	        						policyScope,
									policyName,
									policyVersion,
									yamlSpecification);
	        
	        kfs.write("src/main/resources/" + policyName + ".drl", ks.getResources().newByteArrayResource(drlContents.getBytes()));
        }
        //
        // Compile the rule
        //
        KieBuilder builder = ks.newKieBuilder(kfs).buildAll();
        Results results = builder.getResults();
        if (results.hasMessages(Message.Level.ERROR)) {
        	for (Message msg : results.getMessages()) {
        		logger.error(msg.toString());
        	}
    		throw new RuntimeException("Drools Rule has Errors");
        }
    	for (Message msg : results.getMessages()) {
    		logger.debug(msg.toString());
    	}
    	//
    	// Create our kie Session and container
    	//
        ReleaseId releaseId = ks.getRepository().getDefaultReleaseId();
        logger.debug(releaseId.toString());
	    KieContainer kContainer = ks.newKieContainer(releaseId);
	    
	    return setupSession(kContainer.newKieSession());
	}
	
	private static KieSession setupSession (KieSession kieSession) {

		
		//
		// Create XACML Guard policy from YAML
		// We prepare 4 Guards. Notice that Rebuilds recipe has two Guards (for checking policy combining algorithm)
		//
		PolicyGuardYamlToXacml.fromYamlToXacml("src/test/resources/yaml/policy_guard_appc_restart.yaml", 
						"src/main/resources/frequency_limiter_template.xml", 
						"src/test/resources/xacml/autogenerated_frequency_limiter_restart.xml");
		
		PolicyGuardYamlToXacml.fromYamlToXacml("src/test/resources/yaml/policy_guard_appc_rebuild.yaml", 
						"src/main/resources/frequency_limiter_template.xml", 
						"src/test/resources/xacml/autogenerated_frequency_limiter_rebuild.xml");
		
		PolicyGuardYamlToXacml.fromYamlToXacml("src/test/resources/yaml/policy_guard_appc_rebuild_1.yaml", 
						"src/main/resources/frequency_limiter_template.xml", 
						"src/test/resources/xacml/autogenerated_frequency_limiter_rebuild_1.xml");
		
		PolicyGuardYamlToXacml.fromYamlToXacml("src/test/resources/yaml/policy_guard_appc_migrate.yaml", 
						"src/main/resources/frequency_limiter_template.xml", 
						"src/test/resources/xacml/autogenerated_frequency_limiter_migrate.xml");
		
		PolicyGuardYamlToXacml.fromYamlToXacml("src/test/resources/yaml/policy_guard_appc_modifyconfig.yaml", 
                "src/main/resources/frequency_limiter_template.xml", 
                "src/test/resources/xacml/autogenerated_frequency_limiter_modifyconfig.xml");
		
		PolicyGuardYamlToXacml.fromYamlToXacmlBlacklist("src/test/resources/yaml/policy_guard_appc_restart_blacklist.yaml", 
														"src/main/resources/blacklist_template.xml", 
														"src/test/resources/xacml/autogenerated_blacklist.xml");

		//
		// Creating an embedded XACML PDP
		//
		System.setProperty(XACMLProperties.XACML_PROPERTIES_NAME, "src/test/resources/xacml/xacml_guard.properties");
		
		return kieSession;
	}
	
	public static void setAAIProps(){
		PolicyEngine.manager.setEnvironmentProperty("aai.url", "http://localhost:6666");
        PolicyEngine.manager.setEnvironmentProperty("aai.username", "AAI");
        PolicyEngine.manager.setEnvironmentProperty("aai.password", "AAI");
	}
	
	public static void setSOProps(){
		PolicyEngine.manager.setEnvironmentProperty("so.url", "http://localhost:6667");
        PolicyEngine.manager.setEnvironmentProperty("so.username", "SO");
        PolicyEngine.manager.setEnvironmentProperty("so.password", "SO");
	}

	public static void setGuardProps(){
		/*
		 * Guard PDP-x connection Properties
		 */
		PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_URL,         "http://localhost:6669/pdp/api/getDecision");
		PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_USER,        "python");
		PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_PASS,        "test");
		PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_CLIENT_USER, "python");
		PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_CLIENT_PASS, "test");
		PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_ENV,         "TEST");
		PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_DISABLED,	   "false");
    }
	
	public static void setVFCProps() {
	    PolicyEngine.manager.setEnvironmentProperty("vfc.url", "http://localhost:6668");
        PolicyEngine.manager.setEnvironmentProperty("vfc.username", "VFC");
        PolicyEngine.manager.setEnvironmentProperty("vfc.password", "VFC");
	}
	public static void setPUProp(){
		System.setProperty(OPSHISTPUPROP, "TestOperationsHistoryPU");
	}

}
