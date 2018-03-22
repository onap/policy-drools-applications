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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.junit.Test;

public class PolicyGuardRequestTest {

    @Test
    public void policyGuardRequestTest() {
        UUID requestId = UUID.randomUUID();

        assertNotNull(new PolicyGuardRequest(null, null, null, null));

        PolicyGuardRequest request = new PolicyGuardRequest("Dorothy", "Kansas", requestId, "GetBackHome");

        request.setRequestID(requestId);
        assertEquals(requestId, request.getRequestID());

        request.setActor("Dorothy");
        assertEquals("Dorothy", request.getActor());

        request.setTarget("Kansas");
        assertEquals("Kansas", request.getTarget());

        request.setOperation("GetBackHome");
        assertEquals("GetBackHome", request.getOperation());

        assertEquals("PolicyGuardRequest [actor=Dorothy", request.toString().substring(0, 33));
    }
}
