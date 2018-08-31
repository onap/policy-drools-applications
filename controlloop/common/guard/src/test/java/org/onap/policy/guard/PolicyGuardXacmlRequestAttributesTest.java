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

public class PolicyGuardXacmlRequestAttributesTest {

    @Test
    public void policyGuardXacmlRequestAttributesTest() {
        PolicyGuardXacmlRequestAttributes attributes =
                        new PolicyGuardXacmlRequestAttributes(null, null, null, null, null, null);
        assertNotNull(attributes);

        UUID controlLoopId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Integer vfCount = 20;

        attributes.setRequestID(requestId.toString());
        assertEquals(requestId.toString(), attributes.getRequestID());

        attributes.setOperationID(operationId.toString());
        assertEquals(operationId.toString(), attributes.getOperationID());

        attributes.setActorID(actorId.toString());
        assertEquals(actorId.toString(), attributes.getActorID());

        attributes.setTargetID(targetId.toString());
        assertEquals(targetId.toString(), attributes.getTargetID());

        attributes.setTargetID(targetId.toString());
        assertEquals(targetId.toString(), attributes.getTargetID());

        attributes.setClnameID(controlLoopId.toString());
        assertEquals(controlLoopId.toString(), attributes.getClnameID());

        attributes.setClnameID(null);
        assertEquals(null, attributes.getClnameID());
        
        attributes.setVfCount(vfCount);
        assertEquals(vfCount, attributes.getVfCount());

        attributes.setVfCount(null);
        assertEquals(null, attributes.getVfCount());

        assertEquals("PolicyGuardXacmlRequestAttributes [actorID=", attributes.toString().substring(0, 43));
    }
}
