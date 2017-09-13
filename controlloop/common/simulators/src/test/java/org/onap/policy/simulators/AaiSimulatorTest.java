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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.aai.AAIGETResponse;
import org.onap.policy.aai.AAINQF199.AAINQF199InstanceFilters;
import org.onap.policy.aai.AAINQF199.AAINQF199Manager;
import org.onap.policy.aai.AAINQF199.AAINQF199NamedQuery;
import org.onap.policy.aai.AAINQF199.AAINQF199QueryParameters;
import org.onap.policy.aai.AAINQF199.AAINQF199Request;
import org.onap.policy.aai.AAINQF199.AAINQF199Response;
import org.onap.policy.drools.http.server.HttpServletServer;

public class AaiSimulatorTest {

	@BeforeClass
	public static void setUpSimulator() {
		try {
			Util.buildAaiSim();
		} catch (Exception e) {
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
		AAINQF199Request request = new AAINQF199Request();
		AAINQF199QueryParameters tempQueryParameters = new AAINQF199QueryParameters();
		AAINQF199NamedQuery tempNamedQuery = new AAINQF199NamedQuery();
		tempNamedQuery.namedQueryUUID = UUID.fromString("4ff56a54-9e3f-46b7-a337-07a1d3c6b469");
		tempQueryParameters.namedQuery = tempNamedQuery;
		request.queryParameters = tempQueryParameters;
		Map<String, String> tempInnerMap = new HashMap<String, String>();
		tempInnerMap.put("vserver-name", "vserver-name-16102016-aai3255-data-11-1");
		Map<String, Map<String, String>> tempOuterMap = new HashMap<String, Map<String, String>>();
		tempOuterMap.put("vserver", tempInnerMap);
		List<Map<String, Map<String, String>>> tempInstanceFilter = new LinkedList<Map<String, Map<String, String>>>();
		tempInstanceFilter.add(tempOuterMap);
		AAINQF199InstanceFilters tempInstanceFilters = new AAINQF199InstanceFilters();
		tempInstanceFilters.instanceFilter = tempInstanceFilter;
		request.instanceFilters = tempInstanceFilters;
		
		AAINQF199Response response = AAINQF199Manager.postQuery("http://localhost:6666", "testUser", "testPass", request, UUID.randomUUID());
		assertNotNull(response);
		assertNotNull(response.inventoryResponseItems);
		
		tempNamedQuery.namedQueryUUID = UUID.fromString("a93ac487-409c-4e8c-9e5f-334ae8f99087");
		tempQueryParameters.namedQuery = tempNamedQuery;
		request.queryParameters = tempQueryParameters;
		tempInnerMap = new HashMap<String, String>();
		tempInnerMap.put("vnf-id", "de7cc3ab-0212-47df-9e64-da1c79234deb");
		tempOuterMap = new HashMap<String, Map<String, String>>();
		tempOuterMap.put("generic-vnf", tempInnerMap);
		tempInstanceFilter = new LinkedList<Map<String, Map<String, String>>>();
		tempInstanceFilter.add(tempOuterMap);
		tempInstanceFilters = new AAINQF199InstanceFilters();
		tempInstanceFilters.instanceFilter = tempInstanceFilter;
		request.instanceFilters = tempInstanceFilters;
		
		response = AAINQF199Manager.postQuery("http://localhost:6666", "testUser", "testPass", request, UUID.randomUUID());
		assertNotNull(response);
		assertNotNull(response.inventoryResponseItems);
	}
}
