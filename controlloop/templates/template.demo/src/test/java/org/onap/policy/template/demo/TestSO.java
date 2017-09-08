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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.Results;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopLogger;
import org.onap.policy.controlloop.ControlLoopTargetType;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.impl.ControlLoopLoggerStdOutImpl;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.drools.http.server.HttpServletServer;
import org.onap.policy.drools.impl.PolicyEngineJUnitImpl;
import org.onap.policy.mso.util.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TestSO {

	private static final Logger log = LoggerFactory.getLogger(TestSO.class);
	
	@BeforeClass
	public static void setUpSimulator() {
		try {
			Util.buildAaiSim();
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}
	}

	@AfterClass
	public static void tearDownSimulator() {
		HttpServletServer.factory.destroy();
	}
	
	@Ignore
	@Test
	public void testvDNS() throws IOException {
		
		final String yaml = "src/test/resources/yaml/policy_ControlLoop_SO-test.yaml";
		
		//
		// Pull info from the yaml
		//
		final Util.Pair<ControlLoopPolicy, String> pair = Util.loadYaml(yaml);
		assertNotNull(pair);
		assertNotNull(pair.a);
		assertNotNull(pair.a.getControlLoop());
		assertNotNull(pair.a.getControlLoop().getControlLoopName());
		assertTrue(pair.a.getControlLoop().getControlLoopName().length() > 0);

		//
		// Build a container
		//
		final String closedLoopControlName = pair.a.getControlLoop().getControlLoopName();
		final KieSession kieSession = buildContainer("src/main/resources/ControlLoop_Template_xacml_guard.drl", 
				closedLoopControlName, 
				"type=operational", 
				"myVDNSDemoPolicy", 
				"v1.0",
				"SO",
				"POLICY-CL-MGT",
				"APPC-CL",
				URLEncoder.encode(pair.b, "UTF-8")
				);
		
		log.debug("============ PRINTING YAML ============");
		log.debug(URLEncoder.encode(pair.b, "UTF-8"));
		log.debug("================ DONE =================");

		//
		// Insert our globals
		//
		final ControlLoopLogger logger = new ControlLoopLoggerStdOutImpl();
		kieSession.setGlobal("Logger", logger);
		final PolicyEngineJUnitImpl engine = new PolicyEngineJUnitImpl();
		kieSession.setGlobal("Engine", engine);

		//
		// Initial fire of rules
		//
		kieSession.fireAllRules();
		//
		// Kick a thread that starts testing
		//
		new Thread(new Runnable() {

			@Override
			public void run() {
				
				//Moved simulator startup to an @BeforeClass method
//				log.debug("\n***** Starting AAI Simulator ***** ");
//				AaiSimulatorTest.setUpSimulator();
//				log.debug("\n***** AAI Simulator started ***** ");

				log.debug("\n************ Starting vDNS Test *************\n");

				//
				// Generate an invalid DCAE Event with requestID=null
				//
				VirtualControlLoopEvent invalidEvent = new VirtualControlLoopEvent();
				invalidEvent.closedLoopControlName = closedLoopControlName;
				invalidEvent.requestID = null;
				invalidEvent.closedLoopEventClient = "tca.instance00001";
				invalidEvent.target_type = ControlLoopTargetType.VNF;
				invalidEvent.target = "generic-vnf.vnf-id";
				invalidEvent.from = "DCAE";
				invalidEvent.closedLoopAlarmStart = Instant.now();
				invalidEvent.AAI = new HashMap<String, String>();
				invalidEvent.AAI.put("vserver.vserver-name", "vserver-name-16102016-aai3255-data-11-1");
				invalidEvent.closedLoopEventStatus = ControlLoopEventStatus.ONSET;
				
				log.debug("-------- Sending Invalid ONSET --------");
				log.debug(Serialization.gsonPretty.toJson(invalidEvent));
				
				//
				// Insert invalid DCAE Event into memory
				//
				kieSession.insert(invalidEvent);	
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
				//
				// Generate first DCAE ONSET Event
				//
				VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
				onsetEvent.closedLoopControlName = closedLoopControlName;
				onsetEvent.requestID = UUID.randomUUID();
				onsetEvent.closedLoopEventClient = "tca.instance00001";
				onsetEvent.target_type = ControlLoopTargetType.VNF;
				onsetEvent.target = "VNF_NAME";
				onsetEvent.from = "DCAE";
				onsetEvent.closedLoopAlarmStart = Instant.now();
				onsetEvent.AAI = new HashMap<String, String>();
				onsetEvent.AAI.put("vserver.vserver-name", "vserver-name-16102016-aai3255-data-11-1");
				onsetEvent.AAI.put("vserver.is-closed-loop-disabled", "false");
				onsetEvent.closedLoopEventStatus = ControlLoopEventStatus.ONSET;
				
				log.debug("-------- Sending Valid ONSET --------");
				log.debug(Serialization.gsonPretty.toJson(onsetEvent));
				
				//
				// Insert first DCAE ONSET Event into memory
				//
				kieSession.insert(onsetEvent);
				//
				// We have test for subsequent ONSET Events in testvFirewall()
				// So no need to test it again here
				//
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
				}
				//
				// Test is finished, so stop the kieSession
				//
				kieSession.halt();
			}
		//
		}).start();
		//
		// Start firing rules
		//
		kieSession.fireUntilHalt();
		//
		// Dump working memory
		//
		dumpFacts(kieSession);
		
		//Moved simulator teardown to an @AfterClass method
//		log.debug("\n***** Stopping AAI Simulator ***** ");
//		AaiSimulatorTest.tearDownSimulator();
//		log.debug("\n***** AAI Simulator stopped ***** ");

		//
		// See if there is anything left in memory, there SHOULD only be
		// a params fact.
		//
		assertEquals("There should only be 1 Fact left in memory.", 1, kieSession.getFactCount());
		for (FactHandle handle : kieSession.getFactHandles()) {
			Object fact = kieSession.getObject(handle);
			assertEquals("Non-Param Fact left in working memory", "org.onap.policy.controlloop.Params", fact.getClass().getName());
		}
		
	}
		
	public static void dumpFacts(KieSession kieSession) {
		log.debug("Fact Count: " + kieSession.getFactCount());
		for (FactHandle handle : kieSession.getFactHandles()) {
			log.debug("FACT: " + handle);
		}
	}

	public static KieSession buildContainer(String droolsTemplate, 
			String closedLoopControlName, 
			String policyScope, 
			String policyName, 
			String policyVersion, 
			String actor, 
			String notificationTopic,
			String appcTopic,
			String yamlSpecification) throws IOException {
	   	//
    	// Get our Drools Kie factory
    	//
        KieServices ks = KieServices.Factory.get();
        
        KieModuleModel kModule = ks.newKieModuleModel();
        
        log.debug("KMODULE:" + System.lineSeparator() + kModule.toXML());
        
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
									actor,
									notificationTopic,
									appcTopic,
									yamlSpecification
									);
	        
	        kfs.write("src/main/resources/" + policyName + ".drl", ks.getResources().newByteArrayResource(drlContents.getBytes()));
        }
        //
        // Compile the rule
        //
        KieBuilder builder = ks.newKieBuilder(kfs).buildAll();
        Results results = builder.getResults();
        if (results.hasMessages(Message.Level.ERROR)) {
        	for (Message msg : results.getMessages()) {
        		System.err.println(msg.toString());
        	}
    		throw new RuntimeException("Drools Rule has Errors");
        }
    	for (Message msg : results.getMessages()) {
    		log.debug(msg.toString());
    	}
    	//
    	// Create our kie Session and container
    	//
        ReleaseId releaseId = ks.getRepository().getDefaultReleaseId();
        log.debug("ReleaseId: " + releaseId);
	    KieContainer kContainer = ks.newKieContainer(releaseId);
	    
	    return kContainer.newKieSession();
	}
	public static String	generatePolicy(String ruleContents, 
			String closedLoopControlName, 
			String policyScope, 
			String policyName, 
			String policyVersion,
			String actor,
			String notificationTopic,
			String appcTopic,
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
		
		p = Pattern.compile("\\$\\{actor\\}");
		m = p.matcher(ruleContents);
		ruleContents = m.replaceAll(actor);
		
//		p = Pattern.compile("\\$\\{aaiURL\\}");
//		m = p.matcher(ruleContents);
//		if (aaiURL == null) {
//			ruleContents = m.replaceAll("null");
//		} else {
//			ruleContents = m.replaceAll(aaiURL);
//		}
//		
//		p = Pattern.compile("\\$\\{aaiUsername\\}");
//		m = p.matcher(ruleContents);
//		if (aaiUsername == null) {
//			ruleContents = m.replaceAll("null");
//		} else {
//			ruleContents = m.replaceAll(aaiUsername);
//		}
//
//		p = Pattern.compile("\\$\\{aaiPassword\\}");
//		m = p.matcher(ruleContents);
//		if (aaiPassword == null) {
//			ruleContents = m.replaceAll("null");
//		} else {
//			ruleContents = m.replaceAll(aaiPassword);
//		}
//
//		p = Pattern.compile("\\$\\{msoURL\\}");
//		m = p.matcher(ruleContents);
//		if (msoURL == null) {
//			ruleContents = m.replaceAll("null");
//		} else {
//			ruleContents = m.replaceAll(msoURL);
//		}
//
//		p = Pattern.compile("\\$\\{msoUsername\\}");
//		m = p.matcher(ruleContents);
//		if (msoUsername == null) {
//			ruleContents = m.replaceAll("null");
//		} else {
//			ruleContents = m.replaceAll(msoUsername);
//		}
//
//		p = Pattern.compile("\\$\\{msoPassword\\}");
//		m = p.matcher(ruleContents);
//		if (msoPassword == null) {
//			ruleContents = m.replaceAll("null");
//		} else {
//			ruleContents = m.replaceAll(msoPassword);
//		}
//
//		p = Pattern.compile("\\$\\{aaiNamedQueryUUID\\}");
//		m = p.matcher(ruleContents);
//		if (aaiNamedQueryUUID == null) {
//			ruleContents = m.replaceAll("null");
//		} else {
//			ruleContents = m.replaceAll(aaiNamedQueryUUID);
//		}
//
//		p = Pattern.compile("\\$\\{aaiPatternMatch\\}");
//		m = p.matcher(ruleContents);
//		if (aaiPatternMatch == 1) {
//			ruleContents = m.replaceAll("1");
//		} else {
//			ruleContents = m.replaceAll("0");
//		}
		
		p = Pattern.compile("\\$\\{notificationTopic\\}");
		m = p.matcher(ruleContents);
		if (notificationTopic == null) {
			ruleContents = m.replaceAll("null");
		} else {
			ruleContents = m.replaceAll(notificationTopic);
		}
		
		p = Pattern.compile("\\$\\{appcTopic\\}");
		m = p.matcher(ruleContents);
		if (appcTopic == null) {
			ruleContents = m.replaceAll("null");
		} else {
			ruleContents = m.replaceAll(appcTopic);
		}
		
		p = Pattern.compile("\\$\\{controlLoopYaml\\}");
		m = p.matcher(ruleContents);
		ruleContents = m.replaceAll(controlLoopYaml);
		
		log.debug(ruleContents);

		return ruleContents;
	}

}
