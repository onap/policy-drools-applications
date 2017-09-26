/*-
 * ============LICENSE_START=======================================================
 * simulators
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

package org.onap.policy.simulators;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.drools.http.server.HttpServletServer;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.drools.utils.LoggerUtil;
import org.onap.policy.guard.PolicyGuardXacmlHelper;
import org.onap.policy.guard.PolicyGuardXacmlRequestAttributes;
import org.onap.policy.guard.Util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class GuardSimulatorTest {

	@BeforeClass
	public static void setupSimulator() {
	    LoggerUtil.setLevel("ROOT", "INFO");
	    LoggerUtil.setLevel("org.eclipse.jetty", "WARN");
		try {
			org.onap.policy.simulators.Util.buildGuardSim();
		} catch (Exception e) {
			fail(e.getMessage());
		}
		//
		// Set guard properties
		//
		org.onap.policy.guard.Util.setGuardEnvProps("http://localhost:6669/pdp/api/getDecision", 
			"python", 
			"test", 
			"python", 
			"test", 
			"TEST");

	}
	
	@AfterClass
	public static void tearDownSimulator() {
		HttpServletServer.factory.destroy();
	}
	
	@Test
	public void testGuard() {
		PolicyGuardXacmlRequestAttributes request = new PolicyGuardXacmlRequestAttributes("clname_id", "actor_id", "operation_id", "target_id", "request_id");
		String xacmlResponse = new PolicyGuardXacmlHelper().callPDP(request);
		assertNotNull(xacmlResponse);
	}
}
