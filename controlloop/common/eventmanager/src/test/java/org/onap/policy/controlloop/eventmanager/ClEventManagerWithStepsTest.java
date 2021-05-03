/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import org.drools.core.WorkingMemory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.rule.FactHandle;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.common.utils.io.Serializer;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopResponse;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.actorserviceprovider.ActorService;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.OperationFinalResult;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.OperationResult;
import org.onap.policy.controlloop.actorserviceprovider.Operator;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.actorserviceprovider.spi.Actor;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.controlloop.ophistory.OperationHistoryDataManager;
import org.onap.policy.drools.core.lock.LockCallback;
import org.onap.policy.drools.core.lock.LockImpl;
import org.onap.policy.drools.core.lock.LockState;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

@RunWith(MockitoJUnitRunner.class)
public class ClEventManagerWithStepsTest {
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

    @Mock
    private PolicyEngine engineMgr;
    @Mock
    private WorkingMemory workMem;
    @Mock
    private FactHandle factHandle;
    @Mock
    private Operator policyOperator;
    @Mock
    private Operation policyOperation;
    @Mock
    private Actor policyActor;
    @Mock
    private ActorService actors;
    @Mock
    private OperationHistoryDataManager dataMgr;
    @Mock
    private ExecutorService executor;
    @Mock
    private MyStep stepa;
    @Mock
    private MyStep stepb;

    private List<LockImpl> locks;
    private ToscaPolicy tosca;
    private ControlLoopParams params;
    private ClEventManagerWithSteps<MyStep> mgr;

    /**
     * Sets up.
     */
    @Before
    public void setUp() throws ControlLoopException, CoderException {
        when(workMem.getFactHandle(any())).thenReturn(factHandle);

        params = new ControlLoopParams();
        params.setClosedLoopControlName(CL_NAME);
        params.setPolicyName(POLICY_NAME);
        params.setPolicyScope(POLICY_SCOPE);
        params.setPolicyVersion(POLICY_VERSION);

        loadPolicy(EVENT_MGR_SIMPLE_YAML);

        locks = new ArrayList<>();

        mgr = new MyManager(params, REQ_ID, workMem);
    }

    @Test
    public void testConstructor() {
        assertEquals(POLICY_NAME, mgr.getPolicyName());

        // invalid
        assertThatThrownBy(() -> new MyManager(params, null, workMem)).isInstanceOf(ControlLoopException.class);
    }

    @Test
    public void testIsActive() throws Exception {
        mgr = new RealManager(params, REQ_ID, workMem);
        assertTrue(mgr.isActive());

        // deserialized manager should be inactive
        RealManager mgr2 = Serializer.roundTrip((RealManager) mgr);
        assertFalse(mgr2.isActive());
    }

    @Test
    public void testDestroy_testGetSteps() {
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
    public void testOnStart() throws ControlLoopException {
        OperationOutcome outcome = makeOutcome();

        mgr.start();
        mgr.onStart(outcome);

        assertSame(outcome, mgr.getOutcomes().poll());
        assertThat(mgr.getOutcomes()).isEmpty();

        verify(workMem).update(factHandle, mgr);
    }

    @Test
    public void testOnComplete() throws ControlLoopException {
        OperationOutcome outcome = makeCompletedOutcome();

        mgr.start();
        mgr.onComplete(outcome);

        assertSame(outcome, mgr.getOutcomes().poll());
        assertThat(mgr.getOutcomes()).isEmpty();

        verify(workMem).update(factHandle, mgr);
    }

    @Test
    public void testToString() {
        assertNotNull(mgr.toString());
    }

    @Test
    public void testStart() throws ControlLoopException {
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
    public void testStartNotInWorkingMemory() throws ControlLoopException {
        when(workMem.getFactHandle(any())).thenReturn(null);

        assertThatCode(() -> mgr.start()).isInstanceOf(IllegalStateException.class)
                        .hasMessage("manager is not in working memory");
    }

    /**
     * Tests start() when the manager is not active.
     */
    @Test
    public void testStartInactive() throws Exception {
        // make an inactive manager by deserializing it
        RealManager mgr2 = Serializer.roundTrip(new RealManager(params, REQ_ID, workMem));
        mgr = mgr2;

        // cannot re-start
        assertThatCode(() -> mgr.start()).isInstanceOf(IllegalStateException.class)
                        .hasMessage("manager is no longer active");
    }

    @Test
    public void testAbort() {
        mgr.abort(ClEventManagerWithSteps.State.DONE, OperationFinalResult.FINAL_FAILURE_GUARD, "some message");

        assertEquals(ClEventManagerWithSteps.State.DONE, mgr.getState());
        assertEquals(OperationFinalResult.FINAL_FAILURE_GUARD, mgr.getFinalResult());
        assertEquals("some message", mgr.getFinalMessage());

        // try null state
        assertThatThrownBy(() -> mgr.abort(null, OperationFinalResult.FINAL_FAILURE_GUARD, ""))
                        .isInstanceOf(NullPointerException.class).hasMessageContaining("finalState");
    }

    @Test
    public void testLoadNextPolicy_testGetFullHistory_testGetPartialHistory() throws Exception {
        loadPolicy(EVENT_MGR_MULTI_YAML);
        mgr = new MyManager(params, REQ_ID, workMem);

        // start and load step for first policy
        mgr.start();
        assertEquals("OperationA", mgr.getSteps().poll().getOperationName());
        assertNull(mgr.getFinalResult());

        // add an outcome
        OperationOutcome outcome = makeOutcome();
        mgr.addToHistory(outcome);

        // indicate success and load next policy
        mgr.loadNextPolicy(OperationResult.SUCCESS);
        assertEquals("OperationB", mgr.getSteps().poll().getOperationName());
        assertNull(mgr.getFinalResult());

        // loadPolicy() should clear the partial history, but not the full history
        assertThat(mgr.getPartialHistory()).isEmpty();
        assertThat(mgr.getFullHistory()).hasSize(1);

        // indicate failure - should go to final failure
        mgr.loadNextPolicy(OperationResult.FAILURE);
        assertEquals(OperationFinalResult.FINAL_FAILURE, mgr.getFinalResult());
    }

    @Test
    public void testLoadPolicy() throws ControlLoopException {
        // start() will invoke loadPolicy()
        mgr.start();

        assertNull(mgr.getFinalResult());

        MyStep step = mgr.getSteps().peek();
        assertNotNull(step);
        assertEquals("First", step.getActorName());
        assertEquals("OperationA", step.getOperationName());

        ControlLoopOperationParams params2 = step.getParams();
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
    public void testLoadPreprocessorSteps() {
        stepa = new MyStep(mgr, ControlLoopOperationParams.builder().build()) {
            @Override
            protected Operation buildOperation() {
                return policyOperation;
            }
        };

        Deque<MyStep> steps = mgr.getSteps();
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
    public void testLoadPreprocessorStepsTooManySteps() {
        stepa = new MyStep(mgr, ControlLoopOperationParams.builder().build()) {
            @Override
            protected Operation buildOperation() {
                return policyOperation;
            }
        };

        Deque<MyStep> steps = mgr.getSteps();

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
    public void testExecuteStep() {
        mgr.bumpAttempts();

        // no steps to execute
        assertFalse(mgr.executeStep());
        assertEquals(0, mgr.getAttempts());

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
    public void testNextStep() {
        mgr.getSteps().add(stepa);

        mgr.nextStep();

        assertThat(mgr.getSteps()).isEmpty();
    }

    @Test
    public void testBumpAttempts() {
        assertEquals(0, mgr.getAttempts());

        mgr.bumpAttempts();
        mgr.bumpAttempts();
        assertEquals(2, mgr.getAttempts());
    }

    @Test
    public void testIsAbort() {
        OperationOutcome outcome = makeCompletedOutcome();

        outcome.setResult(OperationResult.FAILURE);
        assertTrue(mgr.isAbort(outcome));

        // no effect for success
        outcome.setResult(OperationResult.SUCCESS);
        assertFalse(mgr.isAbort(outcome));
    }

    @Test
    public void testAddToHistory() throws ControlLoopException {
        mgr.start();

        // add a "start" outcome
        OperationOutcome outcome = makeOutcome();
        mgr.addToHistory(outcome);

        assertThat(mgr.getPartialHistory()).hasSize(1);
        assertThat(mgr.getFullHistory()).hasSize(1);

        // add a "completion" outcome - should replace the start
        outcome = makeCompletedOutcome();
        mgr.addToHistory(outcome);

        assertThat(mgr.getPartialHistory()).hasSize(1);
        assertThat(mgr.getFullHistory()).hasSize(1);
        assertSame(outcome, mgr.getPartialHistory().peek().getOutcome());
        assertSame(outcome, mgr.getFullHistory().peek().getOutcome());

        // add another start
        outcome = makeOutcome();
        mgr.addToHistory(outcome);

        assertThat(mgr.getPartialHistory()).hasSize(2);
        assertThat(mgr.getFullHistory()).hasSize(2);
        assertSame(outcome, mgr.getPartialHistory().peekLast().getOutcome());
        assertSame(outcome, mgr.getFullHistory().peekLast().getOutcome());

        // remove the last item from the full history and then add a "completion"
        mgr.getFullHistory().removeLast();
        outcome = makeCompletedOutcome();
        mgr.addToHistory(outcome);
        assertThat(mgr.getPartialHistory()).hasSize(2);
        assertThat(mgr.getFullHistory()).hasSize(2);

        // add another "start"
        outcome = makeOutcome();
        mgr.addToHistory(outcome);
        assertThat(mgr.getPartialHistory()).hasSize(3);
        assertThat(mgr.getFullHistory()).hasSize(3);

        // add a "completion" for a different actor - should NOT replace the start
        outcome = makeCompletedOutcome();
        outcome.setActor("different-actor");
        mgr.addToHistory(outcome);
        assertThat(mgr.getPartialHistory()).hasSize(4);
        assertThat(mgr.getFullHistory()).hasSize(4);
        assertSame(outcome, mgr.getPartialHistory().peekLast().getOutcome());
        assertSame(outcome, mgr.getFullHistory().peekLast().getOutcome());
    }

    @Test
    public void testMakeNotification() throws Exception {
        loadPolicy(EVENT_MGR_MULTI_YAML);
        mgr = new MyManager(params, REQ_ID, workMem);

        // before started
        assertNotNull(mgr.makeNotification());

        mgr.start();

        mgr.addToHistory(makeCompletedOutcome());
        mgr.addToHistory(makeCompletedOutcome());
        mgr.addToHistory(makeCompletedOutcome());

        // check notification while running
        VirtualControlLoopNotification notif = mgr.makeNotification();
        assertThat(notif.getMessage()).contains(SIMPLE_ACTOR);
        assertThat(notif.getHistory()).hasSize(3);

        // indicate success and load the next policy - should clear the partial history
        mgr.loadNextPolicy(OperationResult.SUCCESS);

        // check notification
        notif = mgr.makeNotification();
        assertNull(notif.getMessage());
        assertThat(notif.getHistory()).isEmpty();

        // add outcomes and check again
        mgr.addToHistory(makeCompletedOutcome());
        mgr.addToHistory(makeCompletedOutcome());

        notif = mgr.makeNotification();
        assertNotNull(notif.getMessage());

        // should only have history for last two outcomes
        assertThat(notif.getHistory()).hasSize(2);

        // indicate failure - should go to final state
        mgr.loadNextPolicy(OperationResult.FAILURE);

        // check notification
        notif = mgr.makeNotification();
        assertNull(notif.getMessage());

        // should be no history
        assertThat(notif.getHistory()).isEmpty();

        // null case
        assertThatThrownBy(() -> mgr.loadNextPolicy(null)).isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("lastResult");
    }

    @Test
    public void testDeliver() {
        mgr.deliver(MY_SINK, null, "null notification", "null rule");
        verify(engineMgr, never()).deliver(any(), any());

        mgr.deliver(MY_SINK, "publishA", "A notification", "A rule");
        verify(engineMgr).deliver(MY_SINK, "publishA");

        // cause deliver() to throw an exception
        when(engineMgr.deliver(any(), any())).thenThrow(new IllegalStateException("expected exception"));
        assertThatCode(() -> mgr.deliver(MY_SINK, "publishB", "B notification", "B rule")).doesNotThrowAnyException();
    }

    @Test
    public void testGetOperationMessage() throws ControlLoopException {
        // no history yet
        assertNull(mgr.getOperationMessage());

        // add an outcome
        mgr.start();
        OperationOutcome outcome = makeOutcome();
        mgr.addToHistory(outcome);

        assertThat(mgr.getOperationMessage()).contains("actor=" + SIMPLE_ACTOR)
                        .contains("operation=" + SIMPLE_OPERATION);
    }

    @Test
    public void testMakeControlLoopResponse() {
        final OperationOutcome outcome = new OperationOutcome();
        outcome.setActor(SIMPLE_ACTOR);

        ControlLoopResponse resp = mgr.makeControlLoopResponse(outcome);
        assertNotNull(resp);
        assertEquals(SIMPLE_ACTOR, resp.getFrom());
    }

    private void loadPolicy(String fileName) throws CoderException {
        ToscaServiceTemplate template =
                        yamlCoder.decode(ResourceUtils.getResourceAsString(fileName), ToscaServiceTemplate.class);
        tosca = template.getToscaTopologyTemplate().getPolicies().get(0).values().iterator().next();

        params.setToscaPolicy(tosca);
    }

    private OperationOutcome makeCompletedOutcome() {
        OperationOutcome outcome = makeOutcome();
        outcome.setEnd(outcome.getStart());

        return outcome;
    }

    private OperationOutcome makeOutcome() {
        OperationOutcome outcome = new OperationOutcome();
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

        public MyManager(ControlLoopParams params, UUID requestId, WorkingMemory workMem) throws ControlLoopException {

            super(params, requestId, workMem);
        }

        @Override
        protected ExecutorService getBlockingExecutor() {
            return executor;
        }

        @Override
        protected void makeLock(String targetEntity, String requestId, int holdSec, LockCallback callback) {
            LockImpl lock = new LockImpl(LockState.ACTIVE, targetEntity, requestId, holdSec, callback);
            locks.add(lock);
            callback.lockAvailable(lock);
        }

        @Override
        public ActorService getActorService() {
            return actors;
        }

        @Override
        public OperationHistoryDataManager getDataManager() {
            return dataMgr;
        }

        @Override
        protected PolicyEngine getPolicyEngineManager() {
            return engineMgr;
        }

        @Override
        protected MyStep makeStep(ControlLoopOperationParams params) {
            return new MyStep(this, params);
        }
    }


    private static class RealManager extends ClEventManagerWithSteps<MyStep> {
        private static final long serialVersionUID = 1L;

        public RealManager(ControlLoopParams params, UUID requestId, WorkingMemory workMem)
                        throws ControlLoopException {

            super(params, requestId, workMem);
        }

        @Override
        protected MyStep makeStep(ControlLoopOperationParams params) {
            return new MyStep(this, params);
        }
    }

    private static class MyStep extends Step {
        public MyStep(StepContext stepContext, ControlLoopOperationParams params) {
            super(params, new AtomicReference<>());
        }
    }
}
