/*-
 * ============LICENSE_START=======================================================
 * appc
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

package org.onap.policy.appclcm;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TestLCMRequest {

	@Test
	public void testLCMRequest() {
		LCMRequest request = new LCMRequest();
		assertNotNull(request);
		assertNotEquals(0, request.hashCode());
		
		LCMCommonHeader commonHeader = new LCMCommonHeader();

		request.setCommonHeader(commonHeader);
		assertEquals(commonHeader, request.getCommonHeader());
		
		request.setAction("Go to Oz");
		assertEquals("Go to Oz", request.getAction());
		
		Map<String, String> actionIdentifiers = new HashMap<>();
		actionIdentifiers.put("North", "Good Witch");
		actionIdentifiers.put("West", "Bad Witch");
		
		request.setActionIdentifiers(actionIdentifiers);
		assertEquals(actionIdentifiers, request.getActionIdentifiers());
		
		request.setPayload("The Emerald City");
		assertEquals("The Emerald City", request.getPayload());
		
		assertNotEquals(0, request.hashCode());
		
		assertEquals("Request [commonHeader=CommonHeader [timeStamp=", request.toString().substring(0,  46));
		
        LCMRequest copiedLCMRequest = new LCMRequest();
        copiedLCMRequest.setCommonHeader(request.getCommonHeader());
        copiedLCMRequest.setAction(request.getAction());
        copiedLCMRequest.setActionIdentifiers(request.getActionIdentifiers());
        copiedLCMRequest.setPayload(request.getPayload());

        assertTrue(request.equals(request));
        assertTrue(request.equals(copiedLCMRequest));
        assertFalse(request.equals(null));
        assertFalse(request.equals("Hello"));
        
        request.setCommonHeader(null);
        assertFalse(request.equals(copiedLCMRequest));
        copiedLCMRequest.setCommonHeader(null);
        assertTrue(request.equals(copiedLCMRequest));
        request.setCommonHeader(commonHeader);
        assertFalse(request.equals(copiedLCMRequest));
        copiedLCMRequest.setCommonHeader(commonHeader);
        assertTrue(request.equals(copiedLCMRequest));
        
        request.setAction(null);
        assertFalse(request.equals(copiedLCMRequest));
        copiedLCMRequest.setAction(null);
        assertTrue(request.equals(copiedLCMRequest));
        request.setAction("Go to Oz");
        assertFalse(request.equals(copiedLCMRequest));
        copiedLCMRequest.setAction("Go to Oz");
        assertTrue(request.equals(copiedLCMRequest));
        
        request.setActionIdentifiers(null);
        assertFalse(request.equals(copiedLCMRequest));
        copiedLCMRequest.setActionIdentifiers(null);
        assertTrue(request.equals(copiedLCMRequest));
        request.setActionIdentifiers(actionIdentifiers);
        assertFalse(request.equals(copiedLCMRequest));
        copiedLCMRequest.setActionIdentifiers(actionIdentifiers);
        assertTrue(request.equals(copiedLCMRequest));
		
        request.setPayload(null);
        assertFalse(request.equals(copiedLCMRequest));
		copiedLCMRequest.setPayload(null);
        assertTrue(request.equals(copiedLCMRequest));
        request.setPayload("The Emerald City");
        assertFalse(request.equals(copiedLCMRequest));
		copiedLCMRequest.setPayload("The Emerald City");
        assertTrue(request.equals(copiedLCMRequest));
	}
}
