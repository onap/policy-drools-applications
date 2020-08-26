/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.OperationResult;
import org.onap.policy.drools.core.lock.Lock;

public class LockDataTest {

    private static final String ENTITY = "my-entity";
    private static final UUID REQ_ID = UUID.randomUUID();

    @Mock
    private Lock lock;
    @Mock
    private Consumer<OperationOutcome> callback1;
    @Mock
    private Consumer<OperationOutcome> callback2;
    @Mock
    private Consumer<OperationOutcome> callback3;

    private LockData data;

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        data = new LockData(ENTITY, REQ_ID);
    }

    @Test
    public void testGetFuture() {
        CompletableFuture<OperationOutcome> future = data.getFuture();
        assertNotNull(future);
        assertFalse(future.isDone());
    }

    @Test
    public void testAddUnavailableCallback() {
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
    public void testAddUnavailableCallbackNeverAvailable() {
        data.addUnavailableCallback(callback1);
        data.addUnavailableCallback(callback2);

        data.lockUnavailable(lock);
        verify(callback1).accept(any());
        verify(callback2).accept(any());

        data.addUnavailableCallback(callback3);
        verify(callback3).accept(any());
    }

    @Test
    public void testFree() {
        // no lock yet
        assertThatCode(() -> data.free()).doesNotThrowAnyException();

        // no with a lock
        data.lockAvailable(lock);
        data.free();
        verify(lock).free();
    }

    @Test
    public void testLockAvailable() throws Exception {
        data.addUnavailableCallback(callback1);
        data.addUnavailableCallback(callback2);

        CompletableFuture<OperationOutcome> future = data.getFuture();
        data.lockAvailable(lock);

        assertSame(future, data.getFuture());

        assertTrue(future.isDone());
        OperationOutcome outcome = future.get();
        assertEquals(ControlLoopOperationManager2.LOCK_ACTOR, outcome.getActor());
        assertEquals(ControlLoopOperationManager2.LOCK_OPERATION, outcome.getOperation());
        assertEquals(ENTITY, outcome.getTarget());
        assertEquals(OperationResult.SUCCESS, outcome.getResult());
        assertEquals(ControlLoopOperation.SUCCESS_MSG, outcome.getMessage());

        Instant start = outcome.getStart();
        assertNotNull(start);

        Instant end = outcome.getEnd();
        assertNotNull(end);
        assertTrue(start.compareTo(end) <= 0);

        verify(callback1, never()).accept(any());
        verify(callback2, never()).accept(any());
    }

    @Test
    public void testLockUnavailable() throws Exception {
        data.addUnavailableCallback(callback1);
        data.addUnavailableCallback(callback2);
        data.addUnavailableCallback(callback3);

        // arrange for callback2 to throw an exception
        doThrow(new IllegalStateException("expected exception")).when(callback2).accept(any());

        CompletableFuture<OperationOutcome> future = data.getFuture();
        assertNotNull(future);
        data.lockUnavailable(lock);

        CompletableFuture<OperationOutcome> future2 = data.getFuture();
        assertNotNull(future2);

        assertNotSame(future, future2);

        assertTrue(future.isDone());
        OperationOutcome outcome = future.get();

        assertTrue(future2.isDone());
        assertSame(outcome, future2.get());

        assertEquals(ControlLoopOperationManager2.LOCK_ACTOR, outcome.getActor());
        assertEquals(ControlLoopOperationManager2.LOCK_OPERATION, outcome.getOperation());
        assertEquals(ENTITY, outcome.getTarget());
        assertEquals(OperationResult.FAILURE, outcome.getResult());
        assertEquals(ControlLoopOperation.FAILED_MSG, outcome.getMessage());

        Instant start = outcome.getStart();
        assertNotNull(start);

        Instant end = outcome.getEnd();
        assertNotNull(end);
        assertTrue(start.compareTo(end) <= 0);

        verify(callback1).accept(eq(outcome));
        verify(callback2).accept(eq(outcome));
        verify(callback3).accept(eq(outcome));
    }
}
