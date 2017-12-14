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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.drools.http.server.HttpServletServer;
import org.onap.policy.drools.utils.LoggerUtil;



public class PolicyGuardXacmlHelperTest {

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
				"DEVL");
	}

	@AfterClass
	/**
	 * Shuts down simulator and performs 1 more test for the case where the connection fails
	 */
	public static void tearDownSimulator() {
		HttpServletServer.factory.destroy();

		// Null/ Bad Connection Case
		PolicyGuardXacmlRequestAttributes xacmlReq = new PolicyGuardXacmlRequestAttributes(
				org.onap.policy.simulators.GuardSimulatorJaxRs.DENY_CLNAME, "actor",  "recipe", "target", "requestId");
		String rawDecision = new PolicyGuardXacmlHelper().callPDP(xacmlReq);
		assertNotNull(rawDecision);
		assertTrue(0 == Util.INDETERMINATE.compareToIgnoreCase(rawDecision));
	}

	@Test
	public void testSimulator() {
		PolicyGuardXacmlRequestAttributes request = new PolicyGuardXacmlRequestAttributes("clname_id", "actor_id", "operation_id", "target_id", "request_id");
		String xacmlResponse = new PolicyGuardXacmlHelper().callPDP(request);
		assertNotNull(xacmlResponse);
	}

	@Test
	/**
	 *  Tests PolicyGuardXacmlHelper.callPDP method to determine if it returns DENY, PERMIT, or INDETERMINATE
	 *  as expected.
	 */
	public void testCallPDP() {
		// Deny Case
		PolicyGuardXacmlRequestAttributes xacmlReq = new PolicyGuardXacmlRequestAttributes(
				org.onap.policy.simulators.GuardSimulatorJaxRs.DENY_CLNAME, "actor",  "recipe", "target", "requestId");
		String rawDecision = new PolicyGuardXacmlHelper().callPDP(xacmlReq);
		assertNotNull(rawDecision);
		assertTrue(0 == Util.DENY.compareToIgnoreCase(rawDecision));

		// Permit Case
		xacmlReq = new PolicyGuardXacmlRequestAttributes(
				"clname", "actor",  "recipe", "target", "requestId");
		rawDecision = new PolicyGuardXacmlHelper().callPDP(xacmlReq);
		assertNotNull(rawDecision);
		assertTrue(0 == Util.PERMIT.compareToIgnoreCase(rawDecision));

		// Indeterminate case is in tearDown for efficiency
	}

}
