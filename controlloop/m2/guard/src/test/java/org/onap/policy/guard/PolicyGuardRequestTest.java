/*-
 * ============LICENSE_START=======================================================
 * guard
 * ================================================================================
 * Copyright (C) 2018 Ericsson. All rights reserved.
 * ================================================================================
 * Modifications Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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

    private static final String KANSAS = "Kansas";
    private static final String GET_BACK_HOME = "GetBackHome";
    private static final String DOROTHY = "Dorothy";

    @Test
    public void testPolicyGuardRequest() {
        UUID requestId = UUID.randomUUID();

        assertNotNull(new PolicyGuardRequest(null, null, null, null));

        PolicyGuardRequest request = new PolicyGuardRequest(DOROTHY, KANSAS, requestId, GET_BACK_HOME);

        request.setRequestId(requestId);
        assertEquals(requestId, request.getRequestId());

        request.setActor(DOROTHY);
        assertEquals(DOROTHY, request.getActor());

        request.setTarget(KANSAS);
        assertEquals(KANSAS, request.getTarget());

        request.setOperation(GET_BACK_HOME);
        assertEquals(GET_BACK_HOME, request.getOperation());

        assertEquals("PolicyGuardRequest [actor=Dorothy", request.toString().substring(0, 33));
    }
}
