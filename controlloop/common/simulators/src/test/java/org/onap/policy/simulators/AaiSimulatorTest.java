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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.aai.AAIGETResponse;
import org.onap.policy.aai.AAINQF199.AAINQF199Manager;
import org.onap.policy.aai.AAINQF199.AAINQF199Request;
import org.onap.policy.aai.AAINQF199.AAINQF199Response;
import org.onap.policy.drools.http.server.HttpServletServer;

public class AaiSimulatorTest {

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

	@Test
	public void testGet() {
		AAIGETResponse response = AAINQF199Manager.getQuery("http://localhost:6666", "testUser", "testPass", UUID.randomUUID(), "5e49ca06-2972-4532-9ed4-6d071588d792");
		assertNotNull(response);
		assertNotNull(response.relationshipList);
	}

	@Test
	public void testPost() {
		AAINQF199Response response = AAINQF199Manager.postQuery("http://localhost:6666", "testUser", "testPass", new AAINQF199Request(), UUID.randomUUID());
		assertNotNull(response);
		assertNotNull(response.inventoryResponseItems);
	}
}
