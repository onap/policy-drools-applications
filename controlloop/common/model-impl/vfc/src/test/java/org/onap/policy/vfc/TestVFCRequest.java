/*-
 * ============LICENSE_START=======================================================
 * vfc
 * ================================================================================
 * Copyright (C) 2018 Ericsson. All rights reserved.
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

package org.onap.policy.vfc;

import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.Test;

public class TestVFCRequest {

	@Test
	public void testVFCRequest() {
		VFCRequest request = new VFCRequest();
		assertNotNull(request);
		assertNotEquals(0, request.hashCode());
		
		String nsInstanceId = "Dorothy";
		request.setNSInstanceId(nsInstanceId);
		assertEquals(nsInstanceId, request.getNSInstanceId());
		
		UUID requestId = UUID.randomUUID();
		request.setRequestId(requestId);
		assertEquals(requestId, request.getRequestId());
		
		VFCHealRequest healRequest = new VFCHealRequest();
		request.setHealRequest(healRequest);
		assertEquals(healRequest, request.getHealRequest());
		
		assertNotEquals(0, request.hashCode());
	}
}
