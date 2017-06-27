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

package org.onap.policy.controlloop.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.Results;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.AgendaGroupPoppedEvent;
import org.kie.api.event.rule.AgendaGroupPushedEvent;
import org.kie.api.event.rule.BeforeMatchFiredEvent;
import org.kie.api.event.rule.MatchCancelledEvent;
import org.kie.api.event.rule.MatchCreatedEvent;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.event.rule.RuleFlowGroupActivatedEvent;
import org.kie.api.event.rule.RuleFlowGroupDeactivatedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.onap.policy.appc.Request;
import org.onap.policy.appc.Response;
import org.onap.policy.appc.ResponseCode;
import org.onap.policy.appc.ResponseValue;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopNotificationType;

import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.ControlLoopLogger;
import org.onap.policy.controlloop.impl.ControlLoopLoggerStdOutImpl;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.drools.impl.PolicyEngineJUnitImpl;
import org.onap.policy.guard.PolicyGuard;
import org.onap.policy.guard.PolicyGuardYamlToXacml;
import com.att.research.xacml.api.pdp.PDPEngine;
import com.att.research.xacml.api.pdp.PDPEngineFactory;
import com.att.research.xacml.util.FactoryException;
import com.att.research.xacml.util.XACMLProperties;

import org.onap.policy.controlloop.policy.guard.ControlLoopGuard;


public class ControlLoopXacmlGuardTest {

	
	@Ignore
	@Test
	public void test() {
		try {
			this.runTest("src/main/resources/ControlLoop_Template_1707_xacml_guard.drl", 
					"src/test/resources/yaml/policy_ControlLoop_vUSP_1707.yaml",
					"service=vUSP;resource=vCTS;type=operational", 
					"CL_VUSP_8888", 
					"com.att.ecomp.closed_loop.vUSP:VNFS:0.0.1");
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void runTest(String droolsTemplate, 
			String yamlFile, 
			String policyScope, 
			String policyName, 
			String policyVersion) throws IOException {
		//
		// Pull info from the yaml
		//
		final Util.Pair<ControlLoopPolicy, String> pair = Util.loadYaml(yamlFile);
		assertNotNull(pair);
		assertNotNull(pair.a);
		assertNotNull(pair.a.controlLoop);
		assertNotNull(pair.a.controlLoop.controlLoopName);
		assertTrue(pair.a.controlLoop.controlLoopName.length() > 0);
		//
		// Build a container
		//
		final KieSession kieSession = buildContainer(droolsTemplate, 
				pair.a.controlLoop.controlLoopName, 
				policyScope, 
				policyName, 
				policyVersion, 
				URLEncoder.encode(pair.b, "UTF-8"));
		
		
		
		System.out.println("============");
		System.out.println(URLEncoder.encode(pair.b, "UTF-8"));
		System.out.println("============");
		
		
		kieSession.addEventListener(new RuleRuntimeEventListener() {

			@Override
			public void objectInserted(ObjectInsertedEvent event) {
			}

			@Override
			public void objectUpdated(ObjectUpdatedEvent event) {
			}

			@Override
			public void objectDeleted(ObjectDeletedEvent event) {
			}
		});
		kieSession.addEventListener(new AgendaEventListener() {

			@Override
			public void matchCreated(MatchCreatedEvent event) {
				//System.out.println("matchCreated: " + event.getMatch().getRule());
			}

			@Override
			public void matchCancelled(MatchCancelledEvent event) {
			}

			@Override
			public void beforeMatchFired(BeforeMatchFiredEvent event) {
				//System.out.println("beforeMatchFired: " + event.getMatch().getRule() + event.getMatch().getObjects());
			}

			@Override
			public void afterMatchFired(AfterMatchFiredEvent event) {
			}

			@Override
			public void agendaGroupPopped(AgendaGroupPoppedEvent event) {
			}

			@Override
			public void agendaGroupPushed(AgendaGroupPushedEvent event) {
			}

			@Override
			public void beforeRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {
			}

			@Override
			public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {
			}

			@Override
			public void beforeRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event) {
			}

			@Override
			public void afterRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event) {
			}
			
		});
		
		//
		// Create XACML Guard policy from YAML
		// We prepare 4 Guards. Notice that Rebuilds recipe has two Guards (for checking policy combining algorithm)
		//
		fromYamlToXacml("src/test/resources/yaml/policy_guard_vUSP_1707_appc_restart.yaml", 
						"src/main/resources/frequency_limiter_template.xml", 
						"src/test/resources/xacml/autogenerated_frequency_limiter_restart.xml");
		
		fromYamlToXacml("src/test/resources/yaml/policy_guard_vUSP_1707_appc_rebuild.yaml", 
						"src/main/resources/frequency_limiter_template.xml", 
						"src/test/resources/xacml/autogenerated_frequency_limiter_rebuild.xml");
		
		fromYamlToXacml("src/test/resources/yaml/policy_guard_vUSP_1707_appc_rebuild_1.yaml", 
						"src/main/resources/frequency_limiter_template.xml", 
						"src/test/resources/xacml/autogenerated_frequency_limiter_rebuild_1.xml");
		
		fromYamlToXacml("src/test/resources/yaml/policy_guard_vUSP_1707_appc_migrate.yaml", 
						"src/main/resources/frequency_limiter_template.xml", 
						"src/test/resources/xacml/autogenerated_frequency_limiter_migrate.xml");
		
		PolicyGuardYamlToXacml.fromYamlToXacmlBlacklist("src/test/resources/yaml/policy_guard_vUSP_1707_appc_restart_blacklist.yaml", 
														"src/main/resources/blacklist_template.xml", 
														"src/test/resources/xacml/autogenerated_blacklist.xml");

        
		//
		// Insert our globals
		//
		final ControlLoopLogger logger = new ControlLoopLoggerStdOutImpl();
		kieSession.setGlobal("Logger", logger);
		final PolicyEngineJUnitImpl engine = new PolicyEngineJUnitImpl();
		kieSession.setGlobal("Engine", engine);
		
		
		//
		// Creating an embedded XACML PDP
		//
		final PDPEngine xacmlPdpEngine;
		System.setProperty(XACMLProperties.XACML_PROPERTIES_NAME, "src/test/resources/xacml/xacml_guard.properties");
		
		PDPEngineFactory factory;
		try {
			factory = PDPEngineFactory.newInstance();
			xacmlPdpEngine = factory.newEngine();
			kieSession.setGlobal("XacmlPdpEngine", xacmlPdpEngine);
		} catch (FactoryException e1) {
			e1.printStackTrace();
		}
			
		
		
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
				try {
					
						
					//
					// Let's use a unique ID for the request and
					// a unique trigger source.
					//
					UUID requestID = UUID.randomUUID();
					String triggerSourceName = "foobartriggersource36";
					
					Object obj = null;
					
					sendGoodEvents(kieSession, pair.a, requestID, triggerSourceName);
					obj = engine.subscribe("UEB", "POLICY-CL-MGT");
					assertNotNull(obj);
					assertTrue(obj instanceof VirtualControlLoopNotification);
					assertTrue(((VirtualControlLoopNotification)obj).notification.equals(ControlLoopNotificationType.ACTIVE));
					//
					// Give the control loop a little time to acquire the lock and publish the request
					//
					Thread.sleep(2000);
					
					
					// "About to query Guard" notification (Querying about Restart)
					obj = engine.subscribe("UEB", "POLICY-CL-MGT");
					assertNotNull(obj);
					System.out.println("\n\n####################### GOING TO QUERY GUARD about Restart!!!!!!");
					System.out.println("Rule: " + ((VirtualControlLoopNotification)obj).policyName +" Message: " + ((VirtualControlLoopNotification)obj).message);
					assertTrue(obj instanceof VirtualControlLoopNotification);
					assertTrue(((VirtualControlLoopNotification)obj).notification.equals(ControlLoopNotificationType.OPERATION));
				
					Thread.sleep(2000);
					// "Response from Guard" notification
					obj = engine.subscribe("UEB", "POLICY-CL-MGT");
					assertNotNull(obj);
					System.out.println("Rule: " + ((VirtualControlLoopNotification)obj).policyName +" Message: " + ((VirtualControlLoopNotification)obj).message);
					assertTrue(obj instanceof VirtualControlLoopNotification);
					assertTrue(((VirtualControlLoopNotification)obj).notification.equals(ControlLoopNotificationType.OPERATION));
				
					
					if(true == ((VirtualControlLoopNotification)obj).message.contains("Guard result: Deny")){
						
						// "About to query Guard" notification (Querying about Rebuild)
						obj = engine.subscribe("UEB", "POLICY-CL-MGT");
						assertNotNull(obj);
						System.out.println("\n\n####################### GOING TO QUERY GUARD about Rebuild!!!!!!");
						System.out.println("Rule: " + ((VirtualControlLoopNotification)obj).policyName +" Message: " + ((VirtualControlLoopNotification)obj).message);
						assertTrue(obj instanceof VirtualControlLoopNotification);
						assertTrue(((VirtualControlLoopNotification)obj).notification.equals(ControlLoopNotificationType.OPERATION));
					
						Thread.sleep(2000);
						
						// "Response from Guard" notification
						obj = engine.subscribe("UEB", "POLICY-CL-MGT");
						assertNotNull(obj);
						System.out.println("Rule: " + ((VirtualControlLoopNotification)obj).policyName +" Message: " + ((VirtualControlLoopNotification)obj).message);
						assertTrue(obj instanceof VirtualControlLoopNotification);
						assertTrue(((VirtualControlLoopNotification)obj).notification.equals(ControlLoopNotificationType.OPERATION));
						
						
						if(true == ((VirtualControlLoopNotification)obj).message.contains("Guard result: Deny")){
							
							// "About to query Guard" notification (Querying about Migrate)
							obj = engine.subscribe("UEB", "POLICY-CL-MGT");
							assertNotNull(obj);
							System.out.println("\n\n####################### GOING TO QUERY GUARD!!!!!!");
							System.out.println("Rule: " + ((VirtualControlLoopNotification)obj).policyName +" Message: " + ((VirtualControlLoopNotification)obj).message);
							assertTrue(obj instanceof VirtualControlLoopNotification);
							assertTrue(((VirtualControlLoopNotification)obj).notification.equals(ControlLoopNotificationType.OPERATION));
							
							Thread.sleep(2000);
							
							// "Response from Guard" notification
							obj = engine.subscribe("UEB", "POLICY-CL-MGT");
							assertNotNull(obj);
							System.out.println("Rule: " + ((VirtualControlLoopNotification)obj).policyName +" Message: " + ((VirtualControlLoopNotification)obj).message);
							assertTrue(obj instanceof VirtualControlLoopNotification);
							assertTrue(((VirtualControlLoopNotification)obj).notification.equals(ControlLoopNotificationType.OPERATION));
							
							
							if(true == ((VirtualControlLoopNotification)obj).message.contains("Guard result: Deny")){
								//All the 3 operations were Denied by Guard
								Thread.sleep(30000);
								
							}	
						}
					}
					
					//
					// In case one of the operations was permitted by Guard
					//
					if(true == ((VirtualControlLoopNotification)obj).message.contains("Guard result: Permit")){
						obj = engine.subscribe("UEB", "POLICY-CL-MGT");
						assertNotNull(obj);
						System.out.println("Rule: " + ((VirtualControlLoopNotification)obj).policyName +" Message: " + ((VirtualControlLoopNotification)obj).message);
						assertTrue(obj instanceof VirtualControlLoopNotification);
						assertTrue(((VirtualControlLoopNotification)obj).notification.equals(ControlLoopNotificationType.OPERATION));
						
						Thread.sleep(500);
						
						obj = engine.subscribe("UEB", "APPC-CL");
						assertNotNull(obj);
						assertTrue(obj instanceof Request);
						assertTrue(((Request)obj).CommonHeader.SubRequestID.equals("1"));
						
						System.out.println("\n============ APP-C Got request!!! ===========\n");
						//
						// Ok - let's simulate ACCEPT
						//

						//
						// now wait for it to finish
						//
						Thread.sleep(500);
						
						//
						// Now we are going to success it
						//
						Response response = new Response((Request) obj);
						response.Status.Code = ResponseCode.SUCCESS.getValue();
						response.Status.Value = ResponseValue.SUCCESS.toString();
						response.Status.Description = "AppC success";
						kieSession.insert(response);
						//
						// Give it some time to process
						//
						Thread.sleep(2000);
						//
						// Insert the abatement event
						//
						sendAbatement(kieSession, pair.a, requestID, triggerSourceName);
						//
						// now wait for it to finish
						//
						Thread.sleep(5000);				
						//
						// Ensure they released the lock
						//
						assertFalse(PolicyGuard.isLocked(TargetType.VM, triggerSourceName, requestID));
						
					}
					
					
					
				} catch (InterruptedException e) {
					System.err.println("Test thread got InterruptedException " + e.getLocalizedMessage());
				} catch (AssertionError e) {
					System.err.println("Test thread got AssertionError " + e.getLocalizedMessage());
					e.printStackTrace();
				} catch (Exception e) {
					System.err.println("Test thread got Exception " + e.getLocalizedMessage());
					e.printStackTrace();
				}
				kieSession.halt();
			}
			
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
		// See if there is anything left in memory
		//
		assertEquals(1, kieSession.getFactCount());
		
		for (FactHandle handle : kieSession.getFactHandles()) {
			Object fact = kieSession.getObject(handle);
			assertEquals("", "com.att.ecomp.policy.controlloop.Params", fact.getClass().getName());
		}
	}
	

	
	
	public static void dumpFacts(KieSession kieSession) {
		System.out.println("Fact Count: " + kieSession.getFactCount());
		for (FactHandle handle : kieSession.getFactHandles()) {
			System.out.println("FACT: " + handle);
		}
	}

	protected void sendAbatement(KieSession kieSession, ControlLoopPolicy policy, UUID requestID, String triggerSourceName) throws InterruptedException {
		VirtualControlLoopEvent event = new VirtualControlLoopEvent();
		event.closedLoopControlName = policy.controlLoop.controlLoopName;
		event.requestID = requestID;
		event.target = "vserver.vserver-name";
		event.closedLoopAlarmStart = Instant.now().minusSeconds(5);
		event.closedLoopAlarmEnd = Instant.now();
		event.AAI = new HashMap<String, String>();
		event.AAI.put("cloud-region.identity-url", "foo");
		event.AAI.put("vserver.selflink", "bar");
		event.AAI.put("vserver.is-closed-loop-disabled", "false");
		event.AAI.put("generic-vnf.vnf-name", "testGenericVnfName");
		event.closedLoopEventStatus = ControlLoopEventStatus.ABATED;
		kieSession.insert(event);
	}
	
	protected void sendGoodEvents(KieSession kieSession, ControlLoopPolicy policy, UUID requestID, String triggerSourceName) throws InterruptedException {
		VirtualControlLoopEvent event = new VirtualControlLoopEvent();
		event.closedLoopControlName = policy.controlLoop.controlLoopName;
		event.requestID = requestID;
		event.target = "vserver.vserver-name";
		event.closedLoopAlarmStart = Instant.now();
		event.AAI = new HashMap<String, String>();
		event.AAI.put("cloud-region.identity-url", "foo");
		event.AAI.put("vserver.selflink", "bar");
		event.AAI.put("vserver.is-closed-loop-disabled", "false");
		event.AAI.put("vserver.vserver-name", "testGenericVnfName");
		event.closedLoopEventStatus = ControlLoopEventStatus.ONSET;
		kieSession.insert(event);
		Thread.sleep(1000);

		/*
		event = new ATTControlLoopEvent(event);
		event.triggerID = "107.250.169.145_f5BigIP" + Instant.now().toEpochMilli();
		kieSession.insert(event);
		Thread.sleep(1000);

		event = new ATTControlLoopEvent(event);
		event.triggerID = "107.250.169.145_f5BigIP" + Instant.now().toEpochMilli();
		kieSession.insert(event);
		Thread.sleep(1000);
		
		event = new ATTControlLoopEvent(event);
		event.triggerID = "107.250.169.145_f5BigIP" + Instant.now().toEpochMilli();
		kieSession.insert(event);
		Thread.sleep(1000);
		*/
		
	}
	
	protected void sendBadEvents(KieSession kieSession, ControlLoopPolicy policy, UUID requestID, String triggerSourceName) throws InterruptedException {
		//
		// Insert a bad Event
		//
		VirtualControlLoopEvent event = new VirtualControlLoopEvent();
		event.closedLoopControlName = policy.controlLoop.controlLoopName;
		kieSession.insert(event);
		Thread.sleep(250);
		//
		// add the request id
		//
		event.requestID = requestID;
		kieSession.insert(event);
		Thread.sleep(250);
		//
		// add some aai
		//
		event.AAI = new HashMap<String, String>();
		event.AAI.put("cloud-region.identity-url", "foo");
		event.AAI.put("vserver.selflink", "bar");
		event.AAI.put("vserver.vserver-name", "vmfoo");
		kieSession.insert(event);
		Thread.sleep(250);
		//
		// set a valid status
		//
		event.closedLoopEventStatus = ControlLoopEventStatus.ONSET;
		kieSession.insert(event);
		Thread.sleep(250);
		//
		// add a trigger sourcename
		//
		kieSession.insert(event);
		Thread.sleep(250);
		//
		// add is closed-loop-disabled
		//
		event.AAI.put("vserver.is-closed-loop-disabled", "true");
		kieSession.insert(event);
		Thread.sleep(250);
		//
		// now enable
		//
		event.AAI.put("vserver.is-closed-loop-disabled", "false");
		kieSession.insert(event);
		Thread.sleep(250);
		//
		// Add target, but bad.
		//
		event.target = "VM_BLAH";
		kieSession.insert(event);
		Thread.sleep(250);
	}

	
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
			
	        String xacmlPolicyContent = PolicyGuardYamlToXacml.generateXacmlGuard(xacmlTemplateContent,
	        		yamlGuardObject.guards.getFirst().actor,
	        		yamlGuardObject.guards.getFirst().recipe,
	        		yamlGuardObject.guards.getFirst().limit_constraints.getFirst().num,
	        		yamlGuardObject.guards.getFirst().limit_constraints.getFirst().duration,
	        		yamlGuardObject.guards.getFirst().limit_constraints.getFirst().time_in_range.get("arg2"),
	        		yamlGuardObject.guards.getFirst().limit_constraints.getFirst().time_in_range.get("arg3")
	        		);
	        
	
	        Files.write(Paths.get(xacmlPolicyOutput), xacmlPolicyContent.getBytes());
        
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}


	
	public static String	generatePolicy(String ruleContents, 
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
		System.out.println(ruleContents);

		return ruleContents;
	}

	public static KieSession buildContainer(String droolsTemplate, String closedLoopControlName, String policyScope, String policyName, String policyVersion, String yamlSpecification) throws IOException {
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
	
	
	

}
