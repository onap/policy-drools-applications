/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021, 2023 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import org.drools.core.WorkingMemory;
import org.drools.core.common.InternalFactHandle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.common.utils.io.Serializer;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.actorserviceprovider.ActorService;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.OperationFinalResult;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.OperationResult;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.drools.core.lock.LockCallback;
import org.onap.policy.drools.core.lock.LockImpl;
import org.onap.policy.drools.core.lock.LockState;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

class ClEventManagerWithStepsTest {
    private static final UUID REQ_ID = UUID.randomUUID();
    private static final String CL_NAME = "my-closed-loop-name";
    private static final String POLICY_NAME = "my-policy-name";
    private static final String POLICY_SCOPE = "my-scope";
    private static final String POLICY_VERSION = "1.2.3";
    private static final String SIMPLE_ACTOR = "First";
    private static final String SIMPLE_OPERATION = "OperationA";
    private static final String MY_TARGET = "my-target";
    private static final String EVENT_MGR_MULTI_YAML =
                    "../eventmanager/src/test/resources/eventManager/event-mgr-multi.yaml";
    private static final String EVENT_MGR_SIMPLE_YAML =
                    "../eventmanager/src/test/resources/eventManager/event-mgr-simple.yaml";
    private static final Coder yamlCoder = new StandardYamlCoder();
    private static final String OUTCOME_MSG = "my outcome message";
    private static final String MY_SINK = "my-topic-sink";

    private final PolicyEngine engineMgr = mock(PolicyEngine.class);
    private final WorkingMemory workMem = mock(WorkingMemory.class);
    private final InternalFactHandle factHandle = mock(InternalFactHandle.class);
    private final Operation policyOperation = mock(Operation.class);
    private final ExecutorService executor = mock(ExecutorService.class);
    private final EventManagerServices services = mock(EventManagerServices.class);
    private final ActorService actors = mock(ActorService.class);
    private MyStep stepa = mock(MyStep.class);
    private final MyStep stepb = mock(MyStep.class);

    private List<LockImpl> locks;
    private ControlLoopParams params;
    private ClEventManagerWithSteps<MyStep> mgr;

    /**
     * Sets up.
     */
    @BeforeEach
    void setUp() throws ControlLoopException, CoderException {
        when(services.getActorService()).thenReturn(actors);

        when(workMem.getFactHandle(any())).thenReturn(factHandle);

        params = new ControlLoopParams();
        params.setClosedLoopControlName(CL_NAME);
        params.setPolicyName(POLICY_NAME);
        params.setPolicyScope(POLICY_SCOPE);
        params.setPolicyVersion(POLICY_VERSION);

        loadPolicy(EVENT_MGR_SIMPLE_YAML);

        locks = new ArrayList<>();

        mgr = new MyManager(services, params, REQ_ID, workMem);
    }

    @Test
    void testConstructor() {
        assertEquals(POLICY_NAME, mgr.getPolicyName());

        // invalid
        assertThatThrownBy(() -> new MyManager(services, params, null, workMem))
                        .isInstanceOf(ControlLoopException.class);
    }

    @Test
    void testDestroy_testGetSteps() {
        // add some steps to the queue
        mgr.getSteps().add(stepa);
        mgr.getSteps().add(stepb);

        mgr.destroy();

        verify(stepa).cancel();
        verify(stepb).cancel();

        // if superclass destroy() was invoked, then freeLock() should have been submitted
        // to the executor
        verify(executor).execute(any());
    }

    @Test
    void testOnStart() throws ControlLoopException {
        var outcome = makeOutcome();

        mgr.start();
        mgr.onStart(outcome);

        assertSame(outcome, mgr.getOutcomes().poll());
        assertThat(mgr.getOutcomes()).isEmpty();

        verify(workMem).update(factHandle, mgr);
    }

    @Test
    void testOnComplete() throws ControlLoopException {
        var outcome = makeCompletedOutcome();

        mgr.start();
        mgr.onComplete(outcome);

        assertSame(outcome, mgr.getOutcomes().poll());
        assertThat(mgr.getOutcomes()).isEmpty();

        verify(workMem).update(factHandle, mgr);
    }

    @Test
    void testToString() {
        assertNotNull(mgr.toString());
    }

    @Test
    void testStart() throws ControlLoopException {
        // start it
        mgr.start();

        // cannot re-start
        assertThatCode(() -> mgr.start()).isInstanceOf(IllegalStateException.class)
                        .hasMessage("manager already started");
    }

    /**
     * Tests start() when the manager is not in working memory.
     */
    @Test
    void testStartNotInWorkingMemory() {
        when(workMem.getFactHandle(any())).thenReturn(null);

        assertThatCode(() -> mgr.start()).isInstanceOf(IllegalStateException.class)
                        .hasMessage("manager is not in working memory");
    }

    /**
     * Tests start() when the manager is not active.
     */
    @Test
    void testStartInactive() throws Exception {
        // make an inactive manager by deserializing it
        mgr = Serializer.roundTrip(new RealManager(services, params, REQ_ID, workMem));

        // cannot re-start
        assertThatCode(() -> mgr.start()).isInstanceOf(IllegalStateException.class)
                        .hasMessage("manager is no longer active");
    }

    @Test
    void testAbort() {
        mgr.abort(ClEventManagerWithSteps.State.DONE, OperationFinalResult.FINAL_FAILURE_GUARD, "some message");

        assertEquals(ClEventManagerWithSteps.State.DONE, mgr.getState());
        assertEquals(OperationFinalResult.FINAL_FAILURE_GUARD, mgr.getFinalResult());
        assertEquals("some message", mgr.getFinalMessage());

        // try null state
        assertThatThrownBy(() -> mgr.abort(null, OperationFinalResult.FINAL_FAILURE_GUARD, ""))
                        .isInstanceOf(NullPointerException.class).hasMessageContaining("finalState");
    }

    @Test
    void testLoadNextPolicy() throws Exception {
        loadPolicy(EVENT_MGR_MULTI_YAML);
        mgr = new MyManager(services, params, REQ_ID, workMem);

        // start and load step for first policy
        mgr.start();
        assertEquals("OperationA", Objects.requireNonNull(mgr.getSteps().poll()).getOperationName());
        assertNull(mgr.getFinalResult());

        // indicate success and load next policy
        mgr.loadNextPolicy(OperationResult.SUCCESS);
        assertEquals("OperationB", Objects.requireNonNull(mgr.getSteps().poll()).getOperationName());
        assertNull(mgr.getFinalResult());

        // indicate failure - should go to final failure
        mgr.loadNextPolicy(OperationResult.FAILURE);
        assertEquals(OperationFinalResult.FINAL_FAILURE, mgr.getFinalResult());
    }

    @Test
    void testLoadPolicy() throws ControlLoopException {
        // start() will invoke loadPolicy()
        mgr.start();

        assertNull(mgr.getFinalResult());

        var step = mgr.getSteps().peek();
        assertNotNull(step);
        assertEquals("First", step.getActorName());
        assertEquals("OperationA", step.getOperationName());

        var params2 = step.getParams();
        assertSame(actors, params2.getActorService());
        assertSame(REQ_ID, params2.getRequestId());
        assertSame(ForkJoinPool.commonPool(), params2.getExecutor());
        assertNotNull(params2.getTargetType());
        assertNotNull(params2.getTargetEntityIds());
        assertEquals(Integer.valueOf(300), params2.getTimeoutSec());
        assertEquals(Integer.valueOf(0), params2.getRetry());
        assertThat(params2.getPayload()).isEmpty();
        assertNotNull(params2.getStartCallback());
        assertNotNull(params2.getCompleteCallback());
    }

    @Test
    void testLoadPreprocessorSteps() {
        stepa = new MyStep(ControlLoopOperationParams.builder().build()) {
            @Override
            protected Operation buildOperation() {
                return policyOperation;
            }
        };

        var steps = mgr.getSteps();
        steps.add(stepa);
        steps.add(stepb);

        mgr.loadPreprocessorSteps();

        // no additional steps should have been loaded
        assertThat(steps).hasSize(2);

        assertSame(stepa, steps.poll());
        assertSame(stepb, steps.poll());
        assertThat(steps).isEmpty();

        assertNotNull(stepa.getOperation());
        assertNull(stepb.getOperation());
    }

    /**
     * Tests loadPreprocessorSteps() when there are too many steps in the queue.
     */
    @Test
    void testLoadPreprocessorStepsTooManySteps() {
        stepa = new MyStep(ControlLoopOperationParams.builder().build()) {
            @Override
            protected Operation buildOperation() {
                return policyOperation;
            }
        };

        var steps = mgr.getSteps();

        // load up a bunch of steps
        for (int nsteps = 0; nsteps < ClEventManagerWithSteps.MAX_STEPS; ++nsteps) {
            steps.add(stepa);
        }

        // should fail
        assertThatIllegalStateException().isThrownBy(() -> mgr.loadPreprocessorSteps()).withMessage("too many steps");

        // add another step, should still fail
        steps.add(stepa);
        assertThatIllegalStateException().isThrownBy(() -> mgr.loadPreprocessorSteps()).withMessage("too many steps");

        // remove two steps - should now succeed
        steps.remove();
        steps.remove();

        int nsteps = steps.size();

        mgr.loadPreprocessorSteps();
        assertEquals(nsteps, steps.size());
    }

    @Test
    void testExecuteStep() {
        // no steps to execute
        assertFalse(mgr.executeStep());

        // add a step to the queue
        mgr.getSteps().add(stepa);

        // step returns false
        when(stepa.start(anyLong())).thenReturn(false);
        assertFalse(mgr.executeStep());

        // step returns true
        when(stepa.start(anyLong())).thenReturn(true);
        assertTrue(mgr.executeStep());
    }

    @Test
    void testNextStep() {
        mgr.getSteps().add(stepa);

        mgr.nextStep();

        assertThat(mgr.getSteps()).isEmpty();
    }

    @Test
    void testDeliver() {
        mgr.deliver(MY_SINK, null, "null notification", "null rule");
        verify(engineMgr, never()).deliver(any(), any());

        mgr.deliver(MY_SINK, "publishA", "A notification", "A rule");
        verify(engineMgr).deliver(MY_SINK, "publishA");

        // cause deliver() to throw an exception
        when(engineMgr.deliver(any(), any())).thenThrow(new IllegalStateException("expected exception"));
        assertThatCode(() -> mgr.deliver(MY_SINK, "publishB", "B notification", "B rule")).doesNotThrowAnyException();
    }

    private void loadPolicy(String fileName) throws CoderException {
        var template = yamlCoder.decode(ResourceUtils.getResourceAsString(fileName), ToscaServiceTemplate.class);
        ToscaPolicy tosca = template.getToscaTopologyTemplate().getPolicies().get(0).values().iterator().next();

        params.setToscaPolicy(tosca);
    }

    private OperationOutcome makeCompletedOutcome() {
        var outcome = makeOutcome();
        outcome.setEnd(outcome.getStart());

        return outcome;
    }

    private OperationOutcome makeOutcome() {
        var outcome = new OperationOutcome();
        outcome.setActor(SIMPLE_ACTOR);
        outcome.setOperation(SIMPLE_OPERATION);
        outcome.setMessage(OUTCOME_MSG);
        outcome.setResult(OperationResult.SUCCESS);
        outcome.setStart(Instant.now());
        outcome.setTarget(MY_TARGET);

        return outcome;
    }


    private class MyManager extends ClEventManagerWithSteps<MyStep> {
        private static final long serialVersionUID = 1L;

        public MyManager(EventManagerServices services, ControlLoopParams params, UUID requestId, WorkingMemory workMem)
                        throws ControlLoopException {

            super(services, params, requestId, workMem);
        }

        @Override
        protected ExecutorService getBlockingExecutor() {
            return executor;
        }

        @Override
        protected void makeLock(String targetEntity, String requestId, int holdSec, LockCallback callback) {
            var lock = new LockImpl(LockState.ACTIVE, targetEntity, requestId, holdSec, callback);
            locks.add(lock);
            callback.lockAvailable(lock);
        }

        @Override
        protected PolicyEngine getPolicyEngineManager() {
            return engineMgr;
        }

        @Override
        protected void loadPolicyStep(ControlLoopOperationParams params) {
            getSteps().add(new MyStep(params));
        }
    }


    private static class RealManager extends ClEventManagerWithSteps<MyStep> {
        private static final long serialVersionUID = 1L;

        public RealManager(EventManagerServices services, ControlLoopParams params, UUID requestId,
                        WorkingMemory workMem) throws ControlLoopException {

            super(services, params, requestId, workMem);
        }

        @Override
        protected void loadPolicyStep(ControlLoopOperationParams params) {
            getSteps().add(new MyStep(params));
        }
    }

    private static class MyStep extends Step {
        public MyStep(ControlLoopOperationParams params) {
            super(params, new AtomicReference<>());
        }
    }
}
