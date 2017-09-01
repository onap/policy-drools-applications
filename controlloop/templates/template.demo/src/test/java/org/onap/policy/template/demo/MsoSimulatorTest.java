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

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.drools.http.server.HttpServletServer;
import org.onap.policy.mso.MSOResponse;
import org.onap.policy.mso.util.Serialization;
import org.onap.policy.rest.RESTManager;
import org.onap.policy.rest.RESTManager.Pair;

public class MsoSimulatorTest {
	
	@BeforeClass
	public static void setUpSimulator() {
		try {
			Util.buildMsoSim();
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}
	}
	
	@AfterClass
	public static void tearDownSimulator() {
		HttpServletServer.factory.destroy();
	}
	
	@Test
	public void testResponse(){
		Pair<Integer, String> httpDetails = RESTManager.post("http://localhost:6667/serviceInstances/v2/12345/vnfs/12345/vfModulesHTTPS/1.1", "username", "password", new HashMap<String, String>(), "application/json", "Some Request Here");
		assertNotNull(httpDetails);
		MSOResponse response = Serialization.gsonPretty.fromJson(httpDetails.b, MSOResponse.class);
		assertNotNull(response);
	}
}
