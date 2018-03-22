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

public class PolicyGuardResponseTest {

    @Test
    public void policyGuardResponseTest() {
        UUID requestId = UUID.randomUUID();

        assertNotNull(new PolicyGuardResponse(null, null, null));

        PolicyGuardResponse response = new PolicyGuardResponse("BackHome", requestId, "GetBackHome");

        response.setRequestID(requestId);
        assertEquals(requestId, response.getRequestID());

        response.setResult("BackHome");
        assertEquals("BackHome", response.getResult());

        response.setOperation("GetBackHome");
        assertEquals("GetBackHome", response.getOperation());

        assertEquals("PolicyGuardResponse [requestID=", response.toString().substring(0, 31));
    }
}
