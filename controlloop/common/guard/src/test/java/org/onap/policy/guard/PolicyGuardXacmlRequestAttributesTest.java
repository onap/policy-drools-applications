/*-
 * ============LICENSE_START=======================================================
 * guard
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

package org.onap.policy.guard;

import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.Test;

public class PolicyGuardXacmlRequestAttributesTest {

	@Test
	public void policyGuardXacmlRequestAttributesTest() {
		assertNotNull(new PolicyGuardXacmlRequestAttributes(null, null, null, null, null));
		
		UUID controlLoopID = UUID.randomUUID();
		UUID operationID = UUID.randomUUID();
		UUID requestID = UUID.randomUUID();
		UUID actorID = UUID.randomUUID();
		UUID targetID = UUID.randomUUID();
		
		PolicyGuardXacmlRequestAttributes attributes = new PolicyGuardXacmlRequestAttributes(
				controlLoopID.toString(), actorID.toString(), operationID.toString(), targetID.toString(), requestID.toString());
		
		attributes.setRequestID(requestID.toString());
		assertEquals(requestID.toString(), attributes.getRequestID());
		
		attributes.setOperationID(operationID.toString());
		assertEquals(operationID.toString(), attributes.getOperationID());
		
		attributes.setActorID(actorID.toString());
		assertEquals(actorID.toString(), attributes.getActorID());
		
		attributes.setTargetID(targetID.toString());
		assertEquals(targetID.toString(), attributes.getTargetID());
		
		attributes.setTargetID(targetID.toString());
		assertEquals(targetID.toString(), attributes.getTargetID());
		
		attributes.setClnameID(controlLoopID.toString());
		assertEquals(controlLoopID.toString(), attributes.getClnameID());
		
		assertEquals("PolicyGuardXacmlRequestAttributes [actorID=", attributes.toString().substring(0, 43));
	}
}
