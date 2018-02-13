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

public class TestLCMResonseCode {

	@Test
	public void testLCMResponseCode() {
		assertNull(LCMResponseCode.toResponseValue(0));
		
		assertEquals(LCMResponseCode.ACCEPTED, LCMResponseCode.toResponseValue(100));
		assertEquals(LCMResponseCode.ERROR, LCMResponseCode.toResponseValue(200));
		assertEquals(LCMResponseCode.REJECT, LCMResponseCode.toResponseValue(300));
		assertEquals(LCMResponseCode.SUCCESS, LCMResponseCode.toResponseValue(400));
		assertEquals(LCMResponseCode.FAILURE, LCMResponseCode.toResponseValue(450));
		assertEquals(LCMResponseCode.FAILURE, LCMResponseCode.toResponseValue(401));
		assertEquals(LCMResponseCode.FAILURE, LCMResponseCode.toResponseValue(406));
		assertEquals(LCMResponseCode.PARTIAL_SUCCESS, LCMResponseCode.toResponseValue(500));
		assertEquals(LCMResponseCode.PARTIAL_FAILURE, LCMResponseCode.toResponseValue(501));
		assertEquals(LCMResponseCode.PARTIAL_FAILURE, LCMResponseCode.toResponseValue(599));
		
		assertEquals("100", new LCMResponseCode(100).toString());
		assertEquals("200", new LCMResponseCode(200).toString());
		assertEquals("300", new LCMResponseCode(300).toString());
		assertEquals("400", new LCMResponseCode(400).toString());
		assertEquals("450", new LCMResponseCode(450).toString());
		assertEquals("500", new LCMResponseCode(500).toString());
		assertEquals("510", new LCMResponseCode(510).toString());
		
		assertEquals(300, new LCMResponseCode(300).getCode());
	}
}
