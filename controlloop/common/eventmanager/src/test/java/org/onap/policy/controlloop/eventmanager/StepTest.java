/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023, 2025 OpenInfra Foundation Europe. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.controlloop.ControlLoopTargetType;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actorserviceprovider.ActorService;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.OperationProperties;
import org.onap.policy.controlloop.actorserviceprovider.OperationResult;
import org.onap.policy.controlloop.actorserviceprovider.Operator;
import org.onap.policy.controlloop.actorserviceprovider.TargetType;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.actorserviceprovider.spi.Actor;
import org.onap.policy.drools.domain.models.operational.OperationalTarget;

class StepTest {
    private static final UUID REQ_ID = UUID.randomUUID();
    private static final String POLICY_ACTOR = "my-actor";
    private static final String POLICY_OPERATION = "my-operation";
    private static final String MY_TARGET = "my-target";
    private static final String PAYLOAD_KEY = "payload-key";
    private static final String PAYLOAD_VALUE = "payload-value";
    private static final long REMAINING_MS = 5000;
    private static final String EXPECTED_EXCEPTION = "expected exception";

    private final Operator policyOperator = mock(Operator.class);
    private final Operation policyOperation = mock(Operation.class);
    private final Actor policyActor = mock(Actor.class);
    private final ActorService actors = mock(ActorService.class);

    private CompletableFuture<OperationOutcome> future;
    private OperationalTarget target;
    private Map<String, String> entityIds;
    private Map<String, String> payload;
    private VirtualControlLoopEvent event;
    private BlockingQueue<OperationOutcome> starts;
    private BlockingQueue<OperationOutcome> completions;
    private ControlLoopOperationParams params;
    private AtomicReference<Instant> startTime;
    private Step step;

    /**
     * Sets up.
     */
    @BeforeEach
    void setUp() {
        future = new CompletableFuture<>();

        // configure policy operation
        when(actors.getActor(POLICY_ACTOR)).thenReturn(policyActor);
        when(policyActor.getOperator(POLICY_OPERATION)).thenReturn(policyOperator);
        when(policyOperator.buildOperation(any())).thenReturn(policyOperation);
        when(policyOperation.start()).thenReturn(future);
        when(policyOperation.getProperty(OperationProperties.AAI_TARGET_ENTITY)).thenReturn(MY_TARGET);

        entityIds = Map.of("entity-name-A", "entity-value-A");

        target = OperationalTarget.builder()
                        .targetType(ControlLoopTargetType.VM)
                        .entityIds(entityIds)
                        .build();

        payload = Map.of(PAYLOAD_KEY, PAYLOAD_VALUE);

        event = new VirtualControlLoopEvent();
        event.setRequestId(REQ_ID);

        starts = new LinkedBlockingQueue<>();
        completions = new LinkedBlockingQueue<>();

        params = ControlLoopOperationParams.builder().actor(POLICY_ACTOR).actorService(actors)
                        .completeCallback(completions::add).executor(ForkJoinPool.commonPool())
                        .operation(POLICY_OPERATION).payload(new TreeMap<>(payload)).startCallback(starts::add)
                        .targetType(TargetType.valueOf(target.getTargetType())).targetEntityIds(target.getEntityIds())
                        .requestId(REQ_ID).build();

        startTime = new AtomicReference<>();

        step = new Step(params, startTime);
    }

    @Test
    void testConstructor() {
        assertTrue(step.isPolicyStep());
        assertSame(params, step.getParams());
        assertNull(step.getParentStep());

        // check that it recorded the startTime by starting and checking it
        step.init();
        step.start(REMAINING_MS);

        assertNotNull(startTime.get());

        // try with null start time
        assertThatThrownBy(() -> new Step(params, null)).isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("startTime");
    }

    @Test
    void testConstructorWithOtherStep_testInitStartTime_testGetStartTimeRef() {
        var step2 = new Step(step, "actorB", "operB");
        assertFalse(step2.isPolicyStep());
        assertSame(step, step2.getParentStep());

        var params2 = step2.getParams();
        assertEquals("actorB", params2.getActor());
        assertEquals("operB", params2.getOperation());
        assertNull(params2.getRetry());
        assertNull(params2.getTimeoutSec());
        assertEquals(target.getTargetType(), params2.getTargetType().toString());
        assertSame(entityIds, params2.getTargetEntityIds());
        assertTrue(params2.getPayload().isEmpty());

        when(actors.getActor(params2.getActor())).thenReturn(policyActor);
        when(policyActor.getOperator(params2.getOperation())).thenReturn(policyOperator);

        assertNull(step2.getStartTime());

        // check that it recorded the startTime by starting and checking it
        step2.init();
        step2.start(REMAINING_MS);

        var instant = startTime.get();
        assertNotNull(instant);
        assertSame(instant, step2.getStartTime());

        // launch the original step, too, so we can test the other branch of
        // initStartTime()
        step.init();
        step.start(REMAINING_MS);

        assertSame(instant, startTime.get());
        assertSame(instant, step.getStartTime());
    }

    @Test
    void testGetActorName_testGetOperationName() {
        assertEquals(POLICY_ACTOR, step.getActorName());
        assertEquals(POLICY_OPERATION, step.getOperationName());
    }

    @Test
    void testIsInitialized_testInit_testGetOperation() {
        assertFalse(step.isInitialized());

        // verify it's unchanged
        assertFalse(step.isInitialized());

        assertNull(step.getOperation());

        step.init();

        assertSame(policyOperation, step.getOperation());
        assertTrue(step.isInitialized());

        // repeat - should be unchanged
        step.init();
        assertSame(policyOperation, step.getOperation());
        assertTrue(step.isInitialized());

        // repeat without init - should be unchanged
        assertSame(policyOperation, step.getOperation());
        assertTrue(step.isInitialized());
    }

    @Test
    void testStart() {
        assertThatIllegalStateException().isThrownBy(() -> step.start(REMAINING_MS))
                        .withMessage("step has not been initialized");

        // initialize it, by calling getOperation(), and then try again
        step.init();
        assertTrue(step.start(REMAINING_MS));

        assertNotNull(startTime.get());

        // should fail if we try again
        assertThatIllegalStateException().isThrownBy(() -> step.start(REMAINING_MS))
                        .withMessage("step is already running");
    }

    /**
     * Tests start() when the operation.start() throws an exception.
     */
    @Test
    void testStartException() {
        when(policyOperation.start()).thenThrow(new RuntimeException());
        step.init();

        assertTrue(step.start(REMAINING_MS));

        // exception should be immediate
        var outcome = completions.poll();
        assertNotNull(outcome);

        assertNotEquals(OperationResult.SUCCESS, outcome.getResult());
        assertEquals(POLICY_ACTOR, outcome.getActor());
        assertTrue(outcome.isFinalOutcome());
    }

    /**
     * Tests start() when the operation throws an asynchronous exception.
     */
    @Test
    void testStartAsyncException() {
        step.init();
        step.start(REMAINING_MS);

        future.completeExceptionally(new RuntimeException(EXPECTED_EXCEPTION));

        // exception should be immediate
        var outcome = completions.poll();
        assertNotNull(outcome);

        assertNotEquals(OperationResult.SUCCESS, outcome.getResult());
        assertEquals(POLICY_ACTOR, outcome.getActor());
        assertTrue(outcome.isFinalOutcome());
    }

    /**
     * Tests handleException() when the exception is a CancellationException.
     */
    @Test
    void testHandleExceptionCancellationException() {
        step.init();
        step.start(REMAINING_MS);

        future.completeExceptionally(new CancellationException(EXPECTED_EXCEPTION));

        // should not have generated an outcome
        assertNull(completions.peek());
    }

    @Test
    void testHandleExceptionCauseCancellationException() {
        step.init();
        step.start(REMAINING_MS);

        future.completeExceptionally(new RuntimeException(EXPECTED_EXCEPTION, new CancellationException()));

        // should not have generated an outcome
        assertNull(completions.peek());
    }

    @Test
    void testHandleException() {
        when(policyOperation.start()).thenThrow(new RuntimeException());

        step.init();

        assertTrue(step.start(REMAINING_MS));

        // exception should be immediate
        var outcome = completions.poll();
        assertNotNull(outcome);

        assertNotEquals(OperationResult.SUCCESS, outcome.getResult());
        assertEquals(POLICY_ACTOR, outcome.getActor());
        assertTrue(outcome.isFinalOutcome());
        assertEquals(POLICY_OPERATION, outcome.getOperation());
        assertSame(startTime.get(), outcome.getStart());
        assertNotNull(outcome.getEnd());
        assertTrue(outcome.getEnd().getEpochSecond() >= startTime.get().getEpochSecond());
    }

    @Test
    void testHandleTimeout() throws InterruptedException {
        step.init();

        long tstart = System.currentTimeMillis();

        // give it a short timeout
        step.start(100);

        var outcome = completions.poll(5, TimeUnit.SECONDS);
        assertNotNull(outcome);

        // should not have timed out before 100ms
        assertTrue(tstart + 100 <= System.currentTimeMillis());

        // must wait for the future to complete before checking that it was cancelled
        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS)).isInstanceOf(Exception.class);

        // verify that the future was cancelled
        assertTrue(future.isCancelled());

        assertNotEquals(OperationResult.SUCCESS, outcome.getResult());
        assertEquals(ActorConstants.CL_TIMEOUT_ACTOR, outcome.getActor());
        assertTrue(outcome.isFinalOutcome());
        assertNull(outcome.getOperation());
        assertSame(startTime.get(), outcome.getStart());
        assertNotNull(outcome.getEnd());
        assertTrue(outcome.getEnd().getEpochSecond() >= startTime.get().getEpochSecond());
    }

    @Test
    void testCancel() {
        // should have no effect
        step.cancel();

        step.init();

        step.start(REMAINING_MS);
        step.cancel();

        assertTrue(future.isCancelled());
    }

    @Test
    void testBuildOperation() {
        assertSame(policyOperation, step.buildOperation());
    }

    @Test
    void testMakeOutcome() {
        step.init();
        assertEquals(MY_TARGET, step.makeOutcome().getTarget());
    }

    @Test
    void testToString() {
        assertNotNull(step.toString());
    }
}
