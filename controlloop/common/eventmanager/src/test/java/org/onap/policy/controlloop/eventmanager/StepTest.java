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

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.controlloop.ControlLoopTargetType;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actorserviceprovider.ActorService;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.Operator;
import org.onap.policy.controlloop.actorserviceprovider.controlloop.ControlLoopEventContext;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.actorserviceprovider.spi.Actor;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.drools.domain.models.operational.OperationalTarget;

public class StepTest {
    private static final UUID REQ_ID = UUID.randomUUID();
    private static final String POLICY_ACTOR = "my-actor";
    private static final String POLICY_OPERATION = "my-operation";
    private static final String MY_TARGET = "my-target";
    private static final String PAYLOAD_KEY = "payload-key";
    private static final String PAYLOAD_VALUE = "payload-value";
    private static final long REMAINING_MS = 5000;
    private static final String EXPECTED_EXCEPTION = "expected exception";

    @Mock
    private Operator policyOperator;
    @Mock
    private Operation policyOperation;
    @Mock
    private Actor policyActor;
    @Mock
    private ActorService actors;

    private CompletableFuture<OperationOutcome> future;
    private OperationalTarget target;
    private Map<String, String> entityIds;
    private Map<String, String> payload;
    private VirtualControlLoopEvent event;
    private ControlLoopEventContext context;
    private BlockingQueue<OperationOutcome> starts;
    private BlockingQueue<OperationOutcome> completions;
    private ControlLoopOperationParams params;
    private AtomicReference<Instant> startTime;
    private Step step;

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        future = new CompletableFuture<>();

        // configure policy operation
        when(actors.getActor(POLICY_ACTOR)).thenReturn(policyActor);
        when(policyActor.getOperator(POLICY_OPERATION)).thenReturn(policyOperator);
        when(policyOperator.buildOperation(any())).thenReturn(policyOperation);
        when(policyOperation.start()).thenReturn(future);

        entityIds = Map.of("entity-name-A", "entity-value-A");

        target = OperationalTarget.builder()
                        .targetType(ControlLoopTargetType.VM)
                        .entityIds(entityIds)
                        .build();

        payload = Map.of(PAYLOAD_KEY, PAYLOAD_VALUE);

        event = new VirtualControlLoopEvent();
        event.setRequestId(REQ_ID);
        event.setTarget(ControlLoopOperationManager2.VSERVER_VSERVER_NAME);
        event.setAai(new TreeMap<>(Map.of(ControlLoopOperationManager2.VSERVER_VSERVER_NAME, MY_TARGET)));

        context = new ControlLoopEventContext(event);

        starts = new LinkedBlockingQueue<>();
        completions = new LinkedBlockingQueue<>();

        params = ControlLoopOperationParams.builder().actor(POLICY_ACTOR).actorService(actors)
                        .completeCallback(completions::add).context(context).executor(ForkJoinPool.commonPool())
                        .operation(POLICY_OPERATION).payload(new TreeMap<>(payload)).startCallback(starts::add)
                        .targetType(target.getTargetType()).targetEntityIds(target.getEntityIds())
                        .targetEntity(MY_TARGET).build();

        startTime = new AtomicReference<>();

        step = new Step(params, startTime);
    }

    @Test
    public void testConstructor() {
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
    public void testConstructorWithOtherStep_testInitStartTime_testGetStartTimeRef() {
        Step step2 = new Step(step, "actorB", "operB");
        assertFalse(step2.isPolicyStep());
        assertSame(step, step2.getParentStep());

        ControlLoopOperationParams params2 = step2.getParams();
        assertEquals("actorB", params2.getActor());
        assertEquals("operB", params2.getOperation());
        assertNull(params2.getRetry());
        assertNull(params2.getTimeoutSec());
        assertEquals(target.getTargetType(), params2.getTargetType());
        assertSame(entityIds, params2.getTargetEntityIds());
        assertEquals(MY_TARGET, params2.getTargetEntity());
        assertTrue(params2.getPayload().isEmpty());

        when(actors.getActor(params2.getActor())).thenReturn(policyActor);
        when(policyActor.getOperator(params2.getOperation())).thenReturn(policyOperator);

        assertNull(step2.getStartTime());

        // check that it recorded the startTime by starting and checking it
        step2.init();
        step2.start(REMAINING_MS);

        Instant instant = startTime.get();
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
    public void testGetActorName_testGetOperationName() {
        assertEquals(POLICY_ACTOR, step.getActorName());
        assertEquals(POLICY_OPERATION, step.getOperationName());
    }

    @Test
    public void testIsInitialized_testInit_testGetOperation() {
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
    public void testStart() {
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
    public void testStartException() {
        when(policyOperation.start()).thenThrow(new RuntimeException());
        step.init();

        assertTrue(step.start(REMAINING_MS));

        // exception should be immediate
        OperationOutcome outcome = completions.poll();
        assertNotNull(outcome);

        assertNotEquals(PolicyResult.SUCCESS, outcome.getResult());
        assertEquals(POLICY_ACTOR, outcome.getActor());
        assertTrue(outcome.isFinalOutcome());
    }

    /**
     * Tests start() when the operation throws an asynchronous exception.
     */
    @Test
    public void testStartAsyncException() {
        step.init();
        step.start(REMAINING_MS);

        future.completeExceptionally(new RuntimeException(EXPECTED_EXCEPTION));

        // exception should be immediate
        OperationOutcome outcome = completions.poll();
        assertNotNull(outcome);

        assertNotEquals(PolicyResult.SUCCESS, outcome.getResult());
        assertEquals(POLICY_ACTOR, outcome.getActor());
        assertTrue(outcome.isFinalOutcome());
    }

    /**
     * Tests handleException() when the exception is a CancellationException.
     */
    @Test
    public void testHandleExceptionCancellationException() {
        step.init();
        step.start(REMAINING_MS);

        future.completeExceptionally(new CancellationException(EXPECTED_EXCEPTION));

        // should not have generated an outcome
        assertNull(completions.peek());
    }

    @Test
    public void testHandleExceptionCauseCancellationException() {
        step.init();
        step.start(REMAINING_MS);

        future.completeExceptionally(new RuntimeException(EXPECTED_EXCEPTION, new CancellationException()));

        // should not have generated an outcome
        assertNull(completions.peek());
    }

    @Test
    public void testHandleException() {
        when(policyOperation.start()).thenThrow(new RuntimeException());

        step.init();

        assertTrue(step.start(REMAINING_MS));

        // exception should be immediate
        OperationOutcome outcome = completions.poll();
        assertNotNull(outcome);

        assertNotEquals(PolicyResult.SUCCESS, outcome.getResult());
        assertEquals(POLICY_ACTOR, outcome.getActor());
        assertTrue(outcome.isFinalOutcome());
        assertEquals(POLICY_OPERATION, outcome.getOperation());
        assertSame(startTime.get(), outcome.getStart());
        assertNotNull(outcome.getEnd());
        assertTrue(outcome.getEnd().getEpochSecond() >= startTime.get().getEpochSecond());
    }

    @Test
    public void testHandleTimeout() throws InterruptedException {
        step.init();

        long tstart = System.currentTimeMillis();

        // give it a short timeout
        step.start(100);

        OperationOutcome outcome = completions.poll(5, TimeUnit.SECONDS);
        assertNotNull(outcome);

        // should not have timed out before 100ms
        assertTrue(tstart + 100 <= System.currentTimeMillis());

        // must wait for the future to complete before checking that it was cancelled
        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS)).isInstanceOf(Exception.class);

        // verify that the future was cancelled
        assertTrue(future.isCancelled());

        assertNotEquals(PolicyResult.SUCCESS, outcome.getResult());
        assertEquals(ActorConstants.CL_TIMEOUT_ACTOR, outcome.getActor());
        assertTrue(outcome.isFinalOutcome());
        assertNull(outcome.getOperation());
        assertSame(startTime.get(), outcome.getStart());
        assertNotNull(outcome.getEnd());
        assertTrue(outcome.getEnd().getEpochSecond() >= startTime.get().getEpochSecond());
    }

    @Test
    public void testCancel() {
        // should have no effect
        step.cancel();

        step.init();

        step.start(REMAINING_MS);
        step.cancel();

        assertTrue(future.isCancelled());
    }

    @Test
    public void testBuildOperation() {
        assertSame(policyOperation, step.buildOperation());
    }

    @Test
    public void testToString() {
        assertNotNull(step.toString());
    }
}
