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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.onap.policy.appc.CommonHeader;
import org.onap.policy.appc.Response;
import org.onap.policy.appc.ResponseStatus;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopTargetType;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.appc.util.Serialization;



public class TestFirewallDemo {

	
	@Test
	public void testvDNS() throws IOException {
		//
		// Build a container
		//
		final String closedLoopControlName = "CL-DNS-LOW-TRAFFIC-SIG-d925ed73-8231-4d02-9545-db4e101f88f8";
		final KieSession kieSession = buildContainer("src/main/resources/archetype-resources/src/main/resources/ControlLoopDemo__closedLoopControlName__.drl", 
				closedLoopControlName, 
				"type=operational", 
				"myFirewallDemoPolicy", 
				"v1.0",
				"MSO",
				"http://localhost:8080/TestREST/Test",
				"POLICY",
				"POLICY",
				"http://localhost:8080/TestREST/Test",
				"POLICY",
				"POLICY",
				"4ff56a54-9e3f-46b7-a337-07a1d3c6b469",
				0,
				"POLICY-CL-MGT",
				"APPC-CL"
				);
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
				//
				// Generate an invalid DCAE Event with requestID=null
				//
				VirtualControlLoopEvent invalidEvent = new VirtualControlLoopEvent();
				invalidEvent.closedLoopControlName = closedLoopControlName;
				invalidEvent.requestID = null;
				invalidEvent.closedLoopEventClient = "tca.instance00001";
				invalidEvent.target_type = ControlLoopTargetType.VF;
				invalidEvent.target = "generic-vnf.vnf-id";
				invalidEvent.from = "DCAE";
				invalidEvent.closedLoopAlarmStart = Instant.now();
				invalidEvent.AAI = new HashMap<String, String>();
				invalidEvent.AAI.put("vserver.vserver-name", "vserver-name-16102016-aai3255-data-11-1");
				invalidEvent.closedLoopEventStatus = ControlLoopEventStatus.ONSET;
				
				System.out.println("----- Invalid ONSET -----");
				System.out.println(Serialization.gsonPretty.toJson(invalidEvent));
				
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
				onsetEvent.target_type = ControlLoopTargetType.VF;
				onsetEvent.target = "generic-vnf.vnf-id";
				onsetEvent.from = "DCAE";
				onsetEvent.closedLoopAlarmStart = Instant.now();
				onsetEvent.AAI = new HashMap<String, String>();
				onsetEvent.AAI.put("vserver.vserver-name", "vserver-name-16102016-aai3255-data-11-1");
				onsetEvent.closedLoopEventStatus = ControlLoopEventStatus.ONSET;
				
				System.out.println("----- ONSET -----");
				System.out.println(Serialization.gsonPretty.toJson(onsetEvent));
				
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
	
	@Test
	public void testvFirewall() throws IOException {
		//
		// Build a container
		//
		final String closedLoopControlName = "CL-FRWL-LOW-TRAFFIC-SIG-d925ed73-8231-4d02-9545-db4e101f88f8";
		final KieSession kieSession = buildContainer("src/main/resources/archetype-resources/src/main/resources/ControlLoopDemo__closedLoopControlName__.drl", 
				closedLoopControlName, 
				"type=operational", 
				"myFirewallDemoPolicy", 
				"v1.0",
				"APPC",
				"http://localhost:8080/TestREST/Test",
				"POLICY",
				"POLICY",
				null,
				null,
				null,
				null,
				1,
				"POLICY-CL-MGT",
				"APPC-CL"
				);
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
				//
				// Generate an invalid DCAE Event with requestID=null
				//
				VirtualControlLoopEvent invalidEvent = new VirtualControlLoopEvent();
				invalidEvent.closedLoopControlName = closedLoopControlName;
				invalidEvent.requestID = null;
				invalidEvent.closedLoopEventClient = "tca.instance00001";
				invalidEvent.target_type = ControlLoopTargetType.VF;
				invalidEvent.target = "generic-vnf.vnf-id";
				invalidEvent.from = "DCAE";
				invalidEvent.closedLoopAlarmStart = Instant.now();
				invalidEvent.AAI = new HashMap<String, String>();
				invalidEvent.AAI.put("generic-vnf.vnf-id", "foo");
				invalidEvent.closedLoopEventStatus = ControlLoopEventStatus.ONSET;
				
				System.out.println("----- Invalid ONSET -----");
				System.out.println(Serialization.gsonPretty.toJson(invalidEvent));
				
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
				onsetEvent.target_type = ControlLoopTargetType.VF;
				onsetEvent.target = "generic-vnf.vnf-id";
				onsetEvent.from = "DCAE";
				onsetEvent.closedLoopAlarmStart = Instant.now();
				onsetEvent.AAI = new HashMap<String, String>();
				onsetEvent.AAI.put("generic-vnf.vnf-id", "fw0001vm001fw001");
				//onsetEvent.AAI.put("vserver.vserver-name", "vserver-name-16102016-aai3255-data-11-1");
				onsetEvent.closedLoopEventStatus = ControlLoopEventStatus.ONSET;
				
				System.out.println("----- ONSET -----");
				System.out.println(Serialization.gsonPretty.toJson(onsetEvent));
				
				//
				// Insert first DCAE ONSET Event into memory
				//
				kieSession.insert(onsetEvent);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
				
				
				Thread thread = new Thread(new Runnable() {

					@Override
					public void run() {
						while (true) {
							//
							// Generate subsequent DCAE ONSET Event
							//
							VirtualControlLoopEvent subOnsetEvent = new VirtualControlLoopEvent();
							subOnsetEvent.closedLoopControlName = closedLoopControlName;
							subOnsetEvent.requestID = UUID.randomUUID();
							subOnsetEvent.closedLoopEventClient = "tca.instance00001";
							subOnsetEvent.target_type = ControlLoopTargetType.VF;
							subOnsetEvent.target = "generic-vnf.vnf-id";
							subOnsetEvent.from = "DCAE";
							subOnsetEvent.closedLoopAlarmStart = Instant.now();
							subOnsetEvent.AAI = new HashMap<String, String>();
							subOnsetEvent.AAI.put("generic-vnf.vnf-id", "fw0001vm001fw001");
							//subOnsetEvent.AAI.put("vserver.vserver-name", "vserver-name-16102016-aai3255-data-11-1");
							subOnsetEvent.closedLoopEventStatus = ControlLoopEventStatus.ONSET;
							
							System.out.println("----- Subsequent ONSET -----");
							System.out.println(Serialization.gsonPretty.toJson(subOnsetEvent));
							
							//
							// Insert subsequent DCAE ONSET Event into memory
							//
							kieSession.insert(subOnsetEvent);
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								break;
							}
						}
					}
					
				});
				thread.start();
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
				}
				//
				// Stop the thread
				//
				thread.interrupt();
				//
				// Generate APPC ACCEPT Response
				//
				Response response1 = new Response();
				// CommonHeader
				CommonHeader commonHeader1 = new CommonHeader();
				commonHeader1.RequestID = onsetEvent.requestID;
				response1.CommonHeader = commonHeader1;
				// ResponseStatus
				ResponseStatus responseStatus1 = new ResponseStatus();
				responseStatus1.Code = 100;
				response1.Status = responseStatus1;
				//
				System.out.println("----- APP-C RESPONSE 100 -----");
				System.out.println(Serialization.gsonPretty.toJson(response1));
				//
				// Insert APPC Response into memory
				//
				kieSession.insert(response1);
				//
				// Simulating APPC takes some time for processing the recipe 
				// and then gives response
				//
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				//
				// Generate APPC SUCCESS Response
				//
				Response response2 = new Response();
				// CommonHeader
				CommonHeader commonHeader2 = new CommonHeader();
				commonHeader2.RequestID = onsetEvent.requestID;
				response2.CommonHeader = commonHeader2;
				// ResponseStatus
				ResponseStatus responseStatus2 = new ResponseStatus();
				responseStatus2.Code = 400;
				response2.Status = responseStatus2;
				//
				System.out.println("----- APP-C RESPONSE 400 -----");
				System.out.println(Serialization.gsonPretty.toJson(response2));
				//
				// Insert APPC Response into memory
				//
				kieSession.insert(response2);
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
		System.out.println("Fact Count: " + kieSession.getFactCount());
		for (FactHandle handle : kieSession.getFactHandles()) {
			System.out.println("FACT: " + handle);
		}
	}

	public static KieSession buildContainer(String droolsTemplate, 
			String closedLoopControlName, 
			String policyScope, 
			String policyName, 
			String policyVersion, 
			String actor, 
			String aaiURL,
			String aaiUsername,
			String aaiPassword,
			String msoURL,
			String msoUsername,
			String msoPassword,
			String aaiNamedQuery,
			int aaiPatternMatch,
			String notificationTopic,
			String appcTopic ) throws IOException {
	   	//
    	// Get our Drools Kie factory
    	//
        KieServices ks = KieServices.Factory.get();
        
        KieModuleModel kModule = ks.newKieModuleModel();
        
        System.out.println("KMODULE:" + System.lineSeparator() + kModule.toXML());
        
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
									aaiURL,
									aaiUsername,
									aaiPassword,
									msoURL,
									msoUsername,
									msoPassword,
									aaiNamedQuery,
									aaiPatternMatch,
									notificationTopic,
									appcTopic
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
    		System.out.println(msg.toString());
    	}
    	//
    	// Create our kie Session and container
    	//
        ReleaseId releaseId = ks.getRepository().getDefaultReleaseId();
        System.out.println(releaseId);
	    KieContainer kContainer = ks.newKieContainer(releaseId);
	    
	    return kContainer.newKieSession();
	}
	public static String	generatePolicy(String ruleContents, 
			String closedLoopControlName, 
			String policyScope, 
			String policyName, 
			String policyVersion,
			String actor,
			String aaiURL,
			String aaiUsername,
			String aaiPassword,
			String msoURL,
			String msoUsername,
			String msoPassword,
			String aaiNamedQueryUUID,
			int aaiPatternMatch,
			String notificationTopic,
			String appcTopic) {

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
		
		p = Pattern.compile("\\$\\{aaiURL\\}");
		m = p.matcher(ruleContents);
		if (aaiURL == null) {
			ruleContents = m.replaceAll("null");
		} else {
			ruleContents = m.replaceAll(aaiURL);
		}
		
		p = Pattern.compile("\\$\\{aaiUsername\\}");
		m = p.matcher(ruleContents);
		if (aaiUsername == null) {
			ruleContents = m.replaceAll("null");
		} else {
			ruleContents = m.replaceAll(aaiUsername);
		}

		p = Pattern.compile("\\$\\{aaiPassword\\}");
		m = p.matcher(ruleContents);
		if (aaiPassword == null) {
			ruleContents = m.replaceAll("null");
		} else {
			ruleContents = m.replaceAll(aaiPassword);
		}

		p = Pattern.compile("\\$\\{msoURL\\}");
		m = p.matcher(ruleContents);
		if (msoURL == null) {
			ruleContents = m.replaceAll("null");
		} else {
			ruleContents = m.replaceAll(msoURL);
		}

		p = Pattern.compile("\\$\\{msoUsername\\}");
		m = p.matcher(ruleContents);
		if (msoUsername == null) {
			ruleContents = m.replaceAll("null");
		} else {
			ruleContents = m.replaceAll(msoUsername);
		}

		p = Pattern.compile("\\$\\{msoPassword\\}");
		m = p.matcher(ruleContents);
		if (msoPassword == null) {
			ruleContents = m.replaceAll("null");
		} else {
			ruleContents = m.replaceAll(msoPassword);
		}

		p = Pattern.compile("\\$\\{aaiNamedQueryUUID\\}");
		m = p.matcher(ruleContents);
		if (aaiNamedQueryUUID == null) {
			ruleContents = m.replaceAll("null");
		} else {
			ruleContents = m.replaceAll(aaiNamedQueryUUID);
		}

		p = Pattern.compile("\\$\\{aaiPatternMatch\\}");
		m = p.matcher(ruleContents);
		if (aaiPatternMatch == 1) {
			ruleContents = m.replaceAll("1");
		} else {
			ruleContents = m.replaceAll("0");
		}
		
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
		
		System.out.println(ruleContents);

		return ruleContents;
	}

}
