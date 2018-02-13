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

public class TestLCMRequestWrapper {

	@Test
	public void testLCMRequestWrapperWrapper() {
		assertNotNull(new LCMRequestWrapper(new LCMRequest()));
		LCMRequestWrapper requestWrapper = new LCMRequestWrapper();
		assertNotNull(requestWrapper);
		assertNotEquals(0, requestWrapper.hashCode());
		
		LCMRequest request = new LCMRequest();

		requestWrapper.setBody(request);
		assertEquals(request, requestWrapper.getBody());
		
		assertNotEquals(0, requestWrapper.hashCode());
		
		assertEquals("RequestWrapper [body=Request [commonHeader=nul", requestWrapper.toString().substring(0,  46));
		
        LCMRequestWrapper copiedLCMRequestWrapper = new LCMRequestWrapper();
        copiedLCMRequestWrapper.setBody(requestWrapper.getBody());

        assertTrue(requestWrapper.equals(requestWrapper));
        assertTrue(requestWrapper.equals(copiedLCMRequestWrapper));
        assertFalse(requestWrapper.equals(null));
        assertFalse(requestWrapper.equals("Hello"));
        
        requestWrapper.setBody(null);
        assertFalse(requestWrapper.equals(copiedLCMRequestWrapper));
        copiedLCMRequestWrapper.setBody(null);
        assertTrue(requestWrapper.equals(copiedLCMRequestWrapper));
        requestWrapper.setBody(request);
        assertFalse(requestWrapper.equals(copiedLCMRequestWrapper));
        copiedLCMRequestWrapper.setBody(request);
        assertTrue(requestWrapper.equals(copiedLCMRequestWrapper));
	}
}
