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

import org.junit.Test;

public class TestLCMResponseWrapper {

	@Test
	public void testLCMResponseWrapperWrapper() {
		LCMResponseWrapper responseWrapper = new LCMResponseWrapper();
		assertNotNull(responseWrapper);
		assertNotEquals(0, responseWrapper.hashCode());
		
		LCMResponse response = new LCMResponse();

		responseWrapper.setBody(response);
		assertEquals(response, responseWrapper.getBody());
		
		assertNotEquals(0, responseWrapper.hashCode());
		
		assertEquals("ResponseWrapper [body=Response [commonHeader=n", responseWrapper.toString().substring(0,  46));
		
        LCMResponseWrapper copiedLCMResponseWrapper = new LCMResponseWrapper();
        copiedLCMResponseWrapper.setBody(responseWrapper.getBody());

        assertTrue(responseWrapper.equals(responseWrapper));
        assertTrue(responseWrapper.equals(copiedLCMResponseWrapper));
        assertFalse(responseWrapper.equals(null));
        assertFalse(responseWrapper.equals("Hello"));
        
        responseWrapper.setBody(null);
        assertFalse(responseWrapper.equals(copiedLCMResponseWrapper));
        copiedLCMResponseWrapper.setBody(null);
        assertTrue(responseWrapper.equals(copiedLCMResponseWrapper));
        responseWrapper.setBody(response);
        assertFalse(responseWrapper.equals(copiedLCMResponseWrapper));
        copiedLCMResponseWrapper.setBody(response);
        assertTrue(responseWrapper.equals(copiedLCMResponseWrapper));
	}
}
