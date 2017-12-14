/*-
 * ============LICENSE_START=======================================================
 * aai
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
package org.onap.policy.aai;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.onap.policy.aai.util.Serialization;
import org.onap.policy.rest.RESTManager;
import org.onap.policy.rest.RESTManager.Pair;

public class AAIManagerTest {
    RESTManager restManagerMock;
	UUID aaiNQRequestUUID = UUID.randomUUID();
    Pair<Integer, String> httpResponseOK;
    Pair<Integer, String> httpResponseErr0;
    Pair<Integer, String> httpResponseErr1;
    Pair<Integer, String> httpResponseWait;

    @Before
	public void beforeTestAAIManager() {
        restManagerMock = mock(RESTManager.class);
        
        Map<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put("X-FromAppId", "POLICY");
        expectedHeaders.put("X-TransactionId", aaiNQRequestUUID.toString());
        expectedHeaders.put("Accept", "application/json");

        AAINQResponse aaiNQResponse = new AAINQResponseTest().getAAINQResponse();
        httpResponseOK  = restManagerMock.new Pair<>(200, Serialization.gsonPretty.toJson(aaiNQResponse));
        httpResponseErr0 = restManagerMock.new Pair<>(200, null);
        httpResponseErr1 = restManagerMock.new Pair<>(200, "{");
        httpResponseWait = restManagerMock.new Pair<>(503, null);
	}
	
	@Test
	public void testAAIManagerAAINQRequest() {
		
		AAIManager aaiManager = new AAIManager(restManagerMock);
		assertNotNull(aaiManager);
		
		UUID aaiNQUUID = UUID.randomUUID();
		
		AAINQQueryParameters aaiNQQueryParameters = new AAINQQueryParameters();
		AAINQNamedQuery aaiNQNamedQuery = new AAINQNamedQuery(); 
		aaiNQNamedQuery.setNamedQueryUUID(aaiNQUUID);
		aaiNQQueryParameters.setNamedQuery(aaiNQNamedQuery);
		
		AAINQRequest aaiNQRequest = new AAINQRequest();
		aaiNQRequest.setQueryParameters(aaiNQQueryParameters);

        when(restManagerMock.post(startsWith("http://somewhere.over.the.rainbow"), eq("Dorothy"), eq("Gale"), anyMap(), anyString(), anyString())).thenReturn(httpResponseOK);

        AAINQResponse aaiNQOKResponse = aaiManager.postQuery("http://somewhere.over.the.rainbow", "Dorothy", "Gale", aaiNQRequest, aaiNQRequestUUID);
		assertNotNull(aaiNQOKResponse);

        when(restManagerMock.post(isNull(), eq("Dorothy"), anyString(), anyMap(), anyString(), anyString())).thenReturn(null);

        AAINQResponse aaiNQNullResponse = aaiManager.postQuery(null, "Dorothy", "Gale", null, aaiNQRequestUUID);
		assertNull(aaiNQNullResponse);

		when(restManagerMock.post(startsWith("http://somewhere.over.the.rainbow"), eq("Witch"), eq("West"), anyMap(), anyString(), anyString())).thenReturn(httpResponseErr0);

        AAINQResponse aaiNQNOKResponse0 = aaiManager.postQuery("http://somewhere.over.the.rainbow", "Witch", "West", aaiNQRequest, aaiNQRequestUUID);
		assertNull(aaiNQNOKResponse0);

		when(restManagerMock.post(startsWith("http://somewhere.under.the.rainbow"), eq("Witch"), eq("West"), anyMap(), anyString(), anyString())).thenReturn(httpResponseErr1);

        AAINQResponse aaiNQNOKResponse1 = aaiManager.postQuery("http://somewhere.under.the.rainbow", "Witch", "West", aaiNQRequest, aaiNQRequestUUID);
		assertNull(aaiNQNOKResponse1);
	}

	@Test
	public void testAAIManagerQueryByVserverName() {
		AAIManager aaiManager = new AAIManager(restManagerMock);
		assertNotNull(aaiManager);
		
		UUID vserverNameRequestId = UUID.randomUUID();

        when(restManagerMock.get(startsWith("http://somewhere.over.the.rainbow"), eq("Dorothy"), eq("Gale"), anyMap())).thenReturn(httpResponseOK);
        
        AAIGETVserverResponse vserverResponse = aaiManager.getQueryByVserverName("http://somewhere.over.the.rainbow", "Dorothy", "Gale", vserverNameRequestId, "vserverName");
		assertNotNull(vserverResponse);

		AAIGETVserverResponse vserverNullResponse = aaiManager.getQueryByVserverName(null, "Dorothy", "Gale", vserverNameRequestId, "vserverName");
		assertNull(vserverNullResponse);

        when(restManagerMock.get(startsWith("http://somewhere.under.the.rainbow"), eq("Witch"), eq("West"), anyMap())).thenReturn(httpResponseErr0);

        AAIGETVserverResponse vserverNOKResponse0 = aaiManager.getQueryByVserverName("http://somewhere.under.the.rainbow", "Witch", "West", vserverNameRequestId, "vserverName");
		assertNull(vserverNOKResponse0);
	}

	@Test
	public void testAAIManagerQueryByVNFId() {
		AAIManager aaiManager = new AAIManager(restManagerMock);
		assertNotNull(aaiManager);
		
		UUID vserverNameRequestId = UUID.randomUUID();

        when(restManagerMock.get(startsWith("http://somewhere.over.the.rainbow"), eq("Dorothy"), eq("Gale"), anyMap())).thenReturn(httpResponseOK);
        
        AAIGETVnfResponse vnfResponse = aaiManager.getQueryByVnfID("http://somewhere.over.the.rainbow", "Dorothy", "Gale", vserverNameRequestId, "vnfID");
		assertNotNull(vnfResponse);
	}

	@Test
	public void testAAIManagerQueryByVNFName() {
		AAIManager aaiManager = new AAIManager(restManagerMock);
		assertNotNull(aaiManager);
		
		UUID vserverNameRequestId = UUID.randomUUID();

        when(restManagerMock.get(startsWith("http://somewhere.over.the.rainbow"), eq("Dorothy"), eq("Gale"), anyMap())).thenReturn(httpResponseOK);
        
        AAIGETVnfResponse vnfResponse = aaiManager.getQueryByVnfID("http://somewhere.over.the.rainbow", "Dorothy", "Gale", vserverNameRequestId, "vnfName");
		assertNotNull(vnfResponse);
	}
}
