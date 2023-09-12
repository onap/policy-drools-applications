/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.controlloop.eventmanager;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.OperationResult;
import org.onap.policy.drools.core.lock.Lock;

class LockDataTest {

    private static final String ENTITY = "my-entity";
    private static final UUID REQ_ID = UUID.randomUUID();

    private final Lock lock = mock(Lock.class);
    private final Consumer<OperationOutcome> callback1 = mock();
    private final Consumer<OperationOutcome> callback2 = mock();
    private final Consumer<OperationOutcome> callback3 = mock();

    private LockData data;

    /**
     * Sets up.
     */
    @BeforeEach
    public void setUp() {
        data = new LockData(ENTITY, REQ_ID);
    }

    @Test
    void testGetFuture() {
        var future = data.getFuture();
        assertNotNull(future);
        assertFalse(future.isDone());
    }

    @Test
    void testAddUnavailableCallback() {
        data.addUnavailableCallback(callback1);
        data.addUnavailableCallback(callback2);

        data.lockAvailable(lock);
        verify(callback1, never()).accept(any());
        verify(callback2, never()).accept(any());

        data.lockUnavailable(lock);
        verify(callback1).accept(any());
        verify(callback2).accept(any());
    }

    /**
     * Tests addUnavailableCallback() when the lock never becomes available.
     */
    @Test
    void testAddUnavailableCallbackNeverAvailable() {
        data.addUnavailableCallback(callback1);
        data.addUnavailableCallback(callback2);

        data.lockUnavailable(lock);
        verify(callback1).accept(any());
        verify(callback2).accept(any());

        data.addUnavailableCallback(callback3);
        verify(callback3).accept(any());
    }

    @Test
    void testFree() {
        // no lock yet
        assertThatCode(() -> data.free()).doesNotThrowAnyException();

        // no with a lock
        data.lockAvailable(lock);
        data.free();
        verify(lock).free();
    }

    @Test
    void testLockAvailable() throws Exception {
        data.addUnavailableCallback(callback1);
        data.addUnavailableCallback(callback2);

        var future = data.getFuture();
        data.lockAvailable(lock);

        assertSame(future, data.getFuture());

        assertTrue(future.isDone());
        var outcome = future.get();
        assertEquals(ActorConstants.LOCK_ACTOR, outcome.getActor());
        assertEquals(ActorConstants.LOCK_OPERATION, outcome.getOperation());
        assertEquals(ENTITY, outcome.getTarget());
        assertEquals(OperationResult.SUCCESS, outcome.getResult());
        assertEquals(ControlLoopOperation.SUCCESS_MSG, outcome.getMessage());

        var start = outcome.getStart();
        assertNotNull(start);

        var end = outcome.getEnd();
        assertNotNull(end);
        assertTrue(start.compareTo(end) <= 0);

        verify(callback1, never()).accept(any());
        verify(callback2, never()).accept(any());
    }

    @Test
    void testLockUnavailable() throws Exception {
        data.addUnavailableCallback(callback1);
        data.addUnavailableCallback(callback2);
        data.addUnavailableCallback(callback3);

        // arrange for callback2 to throw an exception
        doThrow(new IllegalStateException("expected exception")).when(callback2).accept(any());

        var future = data.getFuture();
        assertNotNull(future);
        data.lockUnavailable(lock);

        var future2 = data.getFuture();
        assertNotNull(future2);

        assertNotSame(future, future2);

        assertTrue(future.isDone());
        var outcome = future.get();

        assertTrue(future2.isDone());
        assertSame(outcome, future2.get());

        assertEquals(ActorConstants.LOCK_ACTOR, outcome.getActor());
        assertEquals(ActorConstants.LOCK_OPERATION, outcome.getOperation());
        assertEquals(ENTITY, outcome.getTarget());
        assertEquals(OperationResult.FAILURE, outcome.getResult());
        assertEquals(ControlLoopOperation.FAILED_MSG, outcome.getMessage());

        var start = outcome.getStart();
        assertNotNull(start);

        var end = outcome.getEnd();
        assertNotNull(end);
        assertTrue(start.compareTo(end) <= 0);

        verify(callback1).accept(outcome);
        verify(callback2).accept(outcome);
        verify(callback3).accept(outcome);
    }
}
