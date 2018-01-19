/*-
 * ============LICENSE_START=======================================================
 * vfc
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

package org.onap.policy.vfc;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class TestVFCResponseDescriptor {

	@Test
	public void testVFCResponseDescriptor() {
		VFCResponseDescriptor descriptor = new VFCResponseDescriptor();
		assertNotNull(descriptor);
		assertNotEquals(0, descriptor.hashCode());

		String errorCode = "WitchIsDead";
		descriptor.setErrorCode(errorCode);
		assertEquals(errorCode, descriptor.getErrorCode());
		
		String progress = "Visited Wizard";
		descriptor.setProgress(progress);
		assertEquals(progress, descriptor.getProgress());
		
		List<VFCResponseDescriptor> responseHistoryList = new ArrayList<>();
		descriptor.setResponseHistoryList(responseHistoryList);
		assertEquals(responseHistoryList, descriptor.getResponseHistoryList());
		
		String responseId = "WishHard";
		descriptor.setResponseId(responseId);
		assertEquals(responseId, descriptor.getResponseId());
		
		String status = "Back in Kansas";
		descriptor.setStatus(status);
		assertEquals(status, descriptor.getStatus());
		
		String statusDescription = "Back on the prairie";
		descriptor.setStatusDescription(statusDescription);
		assertEquals(statusDescription, descriptor.getStatusDescription());
		
		assertNotEquals(0, descriptor.hashCode());
	}
}
