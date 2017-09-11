/*-
 * ============LICENSE_START=======================================================
 * demo
 * ================================================================================
 * Copyright (C) 2017 Intel Corp. All rights reserved.
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
import org.onap.policy.vfc.util.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VFCControlLoopTest {

	private static final Logger log = LoggerFactory.getLogger(VFCControlLoopTest.class);
	private KieSession kieSession;
	private Util.Pair<ControlLoopPolicy, String> pair;
	private PolicyEngineJUnitImpl engine;

	@BeforeClass
	public static void setUpSimulator() {
		try {
			Util.buildAaiSim();
			Util.buildVfcSim();
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@AfterClass
	public static void tearDownSimulator() {
		HttpServletServer.factory.destroy();
	}

	@Test
	public void testVolte() throws IOException {

		final String yaml = "src/test/resources/yaml/policy_ControlLoop_VFC.yaml";

		//
		// Pull info from the yaml
		//
		final Util.Pair<ControlLoopPolicy, String> pair = Util.loadYaml(yaml);
		assertNotNull(pair);
		assertNotNull(pair.a);
		assertNotNull(pair.a.getControlLoop());
		assertNotNull(pair.a.getControlLoop().getControlLoopName());
		assertTrue(pair.a.getControlLoop().getControlLoopName().length() > 0);
		final String closedLoopControlName = pair.a.getControlLoop().getControlLoopName();

		 /*
         * Start the kie session
         */
		try {
			kieSession = startSession("src/main/resources/ControlLoop_Template_xacml_guard.drl",
					"src/test/resources/yaml/policy_ControlLoop_VFC.yaml",
					"service=ServiceTest;resource=ResourceTest;type=operational",
					"CL_VFC",
					"org.onap.closed_loop.ServiceTest:VNFS:1.0.0");
		} catch (IOException e) {
			e.printStackTrace();
			log.debug("Could not create kieSession");
			fail("Could not create kieSession");
		}


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

				log.debug("\n************ Starting VoLTE Test *************\n");

				//
				// Generate an invalid DCAE Event with requestID=null
				//
				VirtualControlLoopEvent invalidEvent = new VirtualControlLoopEvent();
				invalidEvent.closedLoopControlName = closedLoopControlName;
				invalidEvent.requestID = null;
				invalidEvent.closedLoopEventClient = "tca.instance00009";
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
				onsetEvent.closedLoopEventClient = "tca.instance00009";
				onsetEvent.target_type = ControlLoopTargetType.VM;
				onsetEvent.target = "VM_NAME";
				onsetEvent.from = "DCAE";
				onsetEvent.closedLoopAlarmStart = Instant.now();
				onsetEvent.AAI = new HashMap<String, String>();
				onsetEvent.AAI.put("vserver.vserver-name", "vserver-name-16102016-aai3255-data-11-1");
				onsetEvent.AAI.put("vserver.vserver-id", "vserver-id-16102016-aai3255-data-11-1");
				onsetEvent.AAI.put("generic-vnf.vnf-id", "vnf-id-16102016-aai3255-data-11-1");
				onsetEvent.AAI.put("service-instance.service-instance-id", "service-instance-id-16102016-aai3255-data-11-1");
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
					Thread.sleep(30000);
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
		log.debug("Fact Count: " + kieSession.getFactCount());
		for (FactHandle handle : kieSession.getFactHandles()) {
			log.debug("FACT: " + handle);
		}
	}

	/**
	 * This method will start a kie session and instantiate
	 * the Policy Engine.
	 *
	 * @param droolsTemplate
	 *          the DRL rules file
	 * @param yamlFile
	 *          the yaml file containing the policies
	 * @param policyScope
	 *          scope for policy
	 * @param policyName
	 *          name of the policy
	 * @param policyVersion
	 *          version of the policy
	 * @return the kieSession to be used to insert facts
	 * @throws IOException
	 */
	private KieSession startSession(String droolsTemplate,
					String yamlFile,
					String policyScope,
					String policyName,
					String policyVersion) throws IOException {

        /*
         * Load policies from yaml
         */
		pair = Util.loadYaml(yamlFile);
		assertNotNull(pair);
		assertNotNull(pair.a);
		assertNotNull(pair.a.getControlLoop());
		assertNotNull(pair.a.getControlLoop().getControlLoopName());
		assertTrue(pair.a.getControlLoop().getControlLoopName().length() > 0);

        /*
         * Construct a kie session
         */
		final KieSession kieSession = Util.buildContainer(droolsTemplate,
				pair.a.getControlLoop().getControlLoopName(),
				policyScope,
				policyName,
				policyVersion,
				URLEncoder.encode(pair.b, "UTF-8"));

        /*
         * Retrieve the Policy Engine
         */
		engine = (PolicyEngineJUnitImpl) kieSession.getGlobal("Engine");

		log.debug("============");
		log.debug(URLEncoder.encode(pair.b, "UTF-8"));
		log.debug("============");

		return kieSession;
	}

}

