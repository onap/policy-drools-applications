/*-
 * ============LICENSE_START=======================================================
 * m2/base
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.m2.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.guard.GuardContext;

public class GuardAdjunctTest {

    private static GuardAdjunct adjunct;
    private static Transaction transaction;
    private static GuardContext context;

    /**
     * Class-level initialization.
     */
    @BeforeClass
    public static void setup() {
        transaction = mock(Transaction.class);
        context = mock(GuardContext.class);

        adjunct = new GuardAdjunct();
        adjunct.transaction = transaction;
        adjunct.context = context;

        when(transaction.getAdjunct(GuardAdjunct.class)).thenReturn(adjunct);
        when(transaction.getClosedLoopControlName()).thenReturn("testCL");
        when(transaction.getRequestId()).thenReturn(UUID.randomUUID());
    }

    @Test
    public void createGuardAdjunctTest() {
        GuardAdjunct.create(transaction, context);
        assertEquals(context, transaction.getAdjunct(GuardAdjunct.class).get());
    }

    @Test
    public void asyncQueryTest() {
        Policy policy = new Policy();
        policy.setActor("APPCLCM");
        policy.setRecipe("test");

        assertTrue(adjunct.asyncQuery(policy, "testTarget", UUID.randomUUID().toString()));

        GuardContext savedContext = adjunct.context;
        adjunct.context = null;

        assertFalse(adjunct.asyncQuery(policy, "testTarget", UUID.randomUUID().toString()));

        adjunct.context = savedContext;
    }

    @Test
    public void asyncCreateDbEntryTest() {
        ControlLoopOperation op = new ControlLoopOperation();
        op.setStart(Instant.now().minusSeconds(1));
        op.setEnd(Instant.now());
        op.setActor("testActor");
        op.setOperation("testOperation");
        op.setSubRequestId("0");
        op.setMessage("test");
        op.setOutcome("success");

        adjunct.asyncCreateDbEntry(op, "testTarget");
        verify(context, times(1)).asyncCreateDbEntry(op.getStart(), op.getEnd(),
            transaction.getClosedLoopControlName(), op.getActor(),
            op.getOperation(), "testTarget", transaction.getRequestId().toString(),
            op.getSubRequestId(), op.getMessage(), op.getOutcome());

    }
}
