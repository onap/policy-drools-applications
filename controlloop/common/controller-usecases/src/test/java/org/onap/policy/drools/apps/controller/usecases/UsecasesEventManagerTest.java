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

package org.onap.policy.drools.apps.controller.usecases;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import org.drools.core.WorkingMemory;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.runtime.rule.FactHandle;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.common.utils.io.Serializer;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopResponse;
import org.onap.policy.controlloop.ControlLoopTargetType;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.actorserviceprovider.ActorService;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.OperationFinalResult;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.OperationProperties;
import org.onap.policy.controlloop.actorserviceprovider.OperationResult;
import org.onap.policy.controlloop.actorserviceprovider.Operator;
import org.onap.policy.controlloop.actorserviceprovider.TargetType;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.actorserviceprovider.spi.Actor;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.controlloop.eventmanager.ActorConstants;
import org.onap.policy.controlloop.eventmanager.ControlLoopEventManager2;
import org.onap.policy.controlloop.ophistory.OperationHistoryDataManager;
import org.onap.policy.drools.apps.controller.usecases.UsecasesEventManager.NewEventStatus;
import org.onap.policy.drools.apps.controller.usecases.step.AaiCqStep2;
import org.onap.policy.drools.apps.controller.usecases.step.AaiGetPnfStep2;
import org.onap.policy.drools.apps.controller.usecases.step.AaiGetTenantStep2;
import org.onap.policy.drools.apps.controller.usecases.step.GetTargetEntityStep2;
import org.onap.policy.drools.apps.controller.usecases.step.GuardStep2;
import org.onap.policy.drools.apps.controller.usecases.step.Step2;
import org.onap.policy.drools.core.lock.LockCallback;
import org.onap.policy.drools.core.lock.LockImpl;
import org.onap.policy.drools.core.lock.LockState;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.sdnr.PciBody;
import org.onap.policy.sdnr.PciMessage;
import org.onap.policy.sdnr.PciResponse;

public class UsecasesEventManagerTest {
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
    private Step2 stepa;
    @Mock
    private Step2 stepb;

    private List<LockImpl> locks;
    private ToscaPolicy tosca;
    private ControlLoopParams params;
    private VirtualControlLoopEvent event;
    private UsecasesEventManager mgr;

    /**
     * Sets up.
     */
    @Before
    public void setUp() throws ControlLoopException, CoderException {
        MockitoAnnotations.initMocks(this);

        when(actors.getActor(SIMPLE_ACTOR)).thenReturn(policyActor);
        when(policyActor.getOperator(SIMPLE_OPERATION)).thenReturn(policyOperator);
        when(policyOperator.buildOperation(any())).thenReturn(policyOperation);
        when(policyOperation.getPropertyNames()).thenReturn(Collections.emptyList());

        when(workMem.getFactHandle(any())).thenReturn(factHandle);

        event = new VirtualControlLoopEvent();
        event.setRequestId(REQ_ID);
        event.setTarget(UsecasesConstants.VSERVER_VSERVER_NAME);
        event.setAai(new TreeMap<>(Map.of(UsecasesConstants.VSERVER_VSERVER_NAME, MY_TARGET)));
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event.setClosedLoopControlName(CL_NAME);
        event.setTargetType(ControlLoopTargetType.VNF);

        params = new ControlLoopParams();
        params.setClosedLoopControlName(CL_NAME);
        params.setPolicyName(POLICY_NAME);
        params.setPolicyScope(POLICY_SCOPE);
        params.setPolicyVersion(POLICY_VERSION);

        loadPolicy(EVENT_MGR_SIMPLE_YAML);

        locks = new ArrayList<>();

        mgr = new MyManager(params, event, workMem);
    }

    @Test
    public void testConstructor() {
        assertEquals(POLICY_NAME, mgr.getPolicyName());
        assertSame(event, mgr.getEvent());

        Map<String, String> orig = event.getAai();

        event.setAai(addAai(orig, UsecasesConstants.VSERVER_IS_CLOSED_LOOP_DISABLED, "true"));
        assertThatThrownBy(() -> new UsecasesEventManager(params, event, workMem))
                        .hasMessage("is-closed-loop-disabled is set to true on VServer or VNF");

        event.setAai(addAai(orig, UsecasesConstants.VSERVER_PROV_STATUS, "inactive"));
        assertThatThrownBy(() -> new UsecasesEventManager(params, event, workMem))
                        .hasMessage("prov-status is not ACTIVE on VServer or VNF");

        // valid
        event.setAai(orig);
        assertThatCode(() -> mgr.checkEventSyntax(event)).doesNotThrowAnyException();

        // invalid
        event.setTarget("unknown-target");
        assertThatThrownBy(() -> new UsecasesEventManager(params, event, workMem))
                        .isInstanceOf(ControlLoopException.class);
    }

    @Test
    public void testIsActive() throws Exception {
        mgr = new UsecasesEventManager(params, event, workMem);
        assertTrue(mgr.isActive());

        // deserialized manager should be inactive
        UsecasesEventManager mgr2 = Serializer.roundTrip(mgr);
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
        mgr = Serializer.roundTrip(new UsecasesEventManager(params, event, workMem));

        // cannot re-start
        assertThatCode(() -> mgr.start()).isInstanceOf(IllegalStateException.class)
                        .hasMessage("manager is no longer active");
    }

    @Test
    public void testAbort() {
        mgr.abort(UsecasesEventManager.State.DONE, OperationFinalResult.FINAL_FAILURE_GUARD, "some message");

        assertEquals(UsecasesEventManager.State.DONE, mgr.getState());
        assertEquals(OperationFinalResult.FINAL_FAILURE_GUARD, mgr.getFinalResult());
        assertEquals("some message", mgr.getFinalMessage());

        // try null state
        assertThatThrownBy(() -> mgr.abort(null, OperationFinalResult.FINAL_FAILURE_GUARD, ""))
                        .isInstanceOf(NullPointerException.class).hasMessageContaining("finalState");
    }

    @Test
    public void testLoadNextPolicy_testGetFullHistory_testGetPartialHistory() throws Exception {
        loadPolicy(EVENT_MGR_MULTI_YAML);
        mgr = new MyManager(params, event, workMem);

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

        Step2 step = mgr.getSteps().peek();
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
        stepa = new Step2(mgr, ControlLoopOperationParams.builder().build(), event) {
            @Override
            public List<String> getPropertyNames() {
                return List.of(OperationProperties.AAI_DEFAULT_CLOUD_REGION);
            }

            @Override
            protected Operation buildOperation() {
                return policyOperation;
            }
        };

        mgr.getSteps().add(stepa);
        mgr.getSteps().add(stepb);

        mgr.loadPreprocessorSteps();

        Deque<Step2> steps = mgr.getSteps();

        Step2 lockStep = steps.poll();
        assertNotNull(lockStep);
        assertEquals(ActorConstants.LOCK_ACTOR, lockStep.getActorName());
        assertThat(steps.poll()).isInstanceOf(AaiCqStep2.class);
        assertThat(steps.poll()).isInstanceOf(GuardStep2.class);
        assertSame(stepa, steps.poll());
        assertSame(stepb, steps.poll());
        assertThat(steps).isEmpty();
    }

    /**
     * Tests loadPreprocessorSteps() when there are too many steps in the queue.
     */
    @Test
    public void testLoadPreprocessorStepsTooManySteps() {
        loadStepsWithProperties(OperationProperties.AAI_PNF);

        Deque<Step2> steps = mgr.getSteps();
        stepa = steps.getFirst();
        steps.clear();

        // load up a bunch of steps
        for (int nsteps = 0; nsteps < UsecasesEventManager.MAX_STEPS; ++nsteps) {
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
        assertEquals(nsteps + 1, steps.size());
    }

    /**
     * Tests loadPreprocessorSteps() when no additional steps are needed.
     */
    @Test
    public void testLoadPreprocessorStepsNothingToLoad() {
        when(stepa.isPolicyStep()).thenReturn(false);
        when(stepa.getPropertyNames()).thenReturn(List.of("unknown-property"));

        Deque<Step2> steps = mgr.getSteps();
        steps.add(stepa);
        steps.add(stepb);

        setTargetEntity();
        mgr.loadPreprocessorSteps();

        assertSame(stepa, steps.poll());
        assertSame(stepb, steps.poll());
        assertThat(steps).isEmpty();
    }

    /**
     * Tests loadPreprocessorSteps() when an A&AI custom query is needed.
     */
    @Test
    public void testLoadPreprocessorStepsCq() {
        loadStepsWithProperties(OperationProperties.AAI_DEFAULT_CLOUD_REGION, OperationProperties.AAI_DEFAULT_TENANT);

        setTargetEntity();
        mgr.loadPreprocessorSteps();

        Deque<Step2> steps = mgr.getSteps();

        assertThat(steps.poll()).isInstanceOf(AaiCqStep2.class);
        assertSame(stepa, steps.poll());
        assertSame(stepb, steps.poll());
        assertThat(steps).isEmpty();
    }

    /**
     * Tests loadPreprocessorSteps() when an A&AI PNF query is needed.
     */
    @Test
    public void testLoadPreprocessorStepsPnf() {
        // doubling up the property to check both branches
        loadStepsWithProperties(OperationProperties.AAI_PNF, OperationProperties.AAI_PNF);

        setTargetEntity();
        mgr.loadPreprocessorSteps();

        Deque<Step2> steps = mgr.getSteps();

        assertThat(steps.poll()).isInstanceOf(AaiGetPnfStep2.class);
        assertSame(stepa, steps.poll());
        assertSame(stepb, steps.poll());
        assertThat(steps).isEmpty();
    }

    /**
     * Tests loadPreprocessorSteps() when an A&AI Tenant query is needed.
     */
    @Test
    public void testLoadPreprocessorStepsTenant() {
        // doubling up the property to check both branches
        event.getAai().put(Step2.VSERVER_VSERVER_NAME, "my-vserver");
        loadStepsWithProperties(OperationProperties.AAI_VSERVER_LINK, OperationProperties.AAI_VSERVER_LINK);

        setTargetEntity();
        mgr.loadPreprocessorSteps();

        Deque<Step2> steps = mgr.getSteps();

        assertThat(steps.poll()).isInstanceOf(AaiGetTenantStep2.class);
        assertSame(stepa, steps.poll());
        assertSame(stepb, steps.poll());
        assertThat(steps).isEmpty();
    }

    /**
     * Tests loadPreprocessorSteps() when the target entity is unset.
     */
    @Test
    public void testLoadPreprocessorStepsNeedTargetEntity() {
        stepa = new Step2(mgr,
                ControlLoopOperationParams.builder()
                        .targetType(TargetType.toTargetType(event.getTargetType()))
                        .targetEntityIds(Map.of()).build(), event) {
            @Override
            public List<String> getPropertyNames() {
                return List.of(OperationProperties.AAI_TARGET_ENTITY);
            }

            @Override
            protected Operation buildOperation() {
                return policyOperation;
            }

            @Override
            public boolean isPolicyStep() {
                return false;
            }
        };

        Deque<Step2> steps = mgr.getSteps();
        steps.add(stepa);
        steps.add(stepb);

        mgr.loadPreprocessorSteps();

        Step2 entityStep = steps.poll();

        assertThat(entityStep).isInstanceOf(GetTargetEntityStep2.class);
        assertSame(stepa, steps.poll());
        assertSame(stepb, steps.poll());
        assertThat(steps).isEmpty();

        // put get-target-entity back onto the queue and ensure nothing else is added
        steps.add(entityStep);
        mgr.loadPreprocessorSteps();
        assertSame(entityStep, steps.poll());
        assertThat(steps).isEmpty();
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

        // closed loop timeout
        outcome.setActor(ActorConstants.CL_TIMEOUT_ACTOR);
        assertTrue(mgr.isAbort(outcome));

        // lost lock
        outcome.setActor(ActorConstants.LOCK_ACTOR);
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
        mgr = new MyManager(params, event, workMem);

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
    public void testStoreInDataBase() throws ControlLoopException {
        mgr.start();
        OperationOutcome outcome = makeOutcome();
        mgr.addToHistory(outcome);

        mgr.storeInDataBase(mgr.getPartialHistory().peekLast());

        verify(dataMgr).store(REQ_ID.toString(), event, null, mgr.getPartialHistory().peekLast().getClOperation());
    }

    @Test
    public void testMakeControlLoopResponse() {
        final OperationOutcome outcome = new OperationOutcome();

        // no message - should return null
        checkResp(outcome, null);

        // not a PciMessage - should return null
        outcome.setResponse("not-a-pci-message");
        checkResp(outcome, null);

        /*
         * now work with a PciMessage
         */
        PciMessage msg = new PciMessage();
        outcome.setResponse(msg);

        PciBody body = new PciBody();
        msg.setBody(body);

        PciResponse output = new PciResponse();
        body.setOutput(output);

        output.setPayload("my-payload");

        // should generate a response, with a payload
        checkResp(outcome, "my-payload");

        /*
         * these should generate a response, with null payload
         */
        output.setPayload(null);
        checkResp(outcome, null);

        body.setOutput(null);
        checkResp(outcome, null);

        msg.setBody(null);
        checkResp(outcome, null);

        outcome.setResponse(null);
        checkResp(outcome, null);
    }

    @Test
    public void testOnNewEvent() {
        VirtualControlLoopEvent event2 = new VirtualControlLoopEvent(event);
        assertEquals(NewEventStatus.FIRST_ONSET, mgr.onNewEvent(event2));

        event2.setPayload("other payload");
        assertEquals(NewEventStatus.SUBSEQUENT_ONSET, mgr.onNewEvent(event2));
        assertEquals(NewEventStatus.SUBSEQUENT_ONSET, mgr.onNewEvent(event2));
        assertEquals(NewEventStatus.FIRST_ONSET, mgr.onNewEvent(event));

        event2.setClosedLoopEventStatus(ControlLoopEventStatus.ABATED);
        assertEquals(NewEventStatus.FIRST_ABATEMENT, mgr.onNewEvent(event2));

        assertEquals(NewEventStatus.SUBSEQUENT_ABATEMENT, mgr.onNewEvent(event2));
        assertEquals(NewEventStatus.SUBSEQUENT_ABATEMENT, mgr.onNewEvent(event2));

        event2.setClosedLoopEventStatus(null);
        assertEquals(NewEventStatus.SYNTAX_ERROR, mgr.onNewEvent(event2));
    }

    @Test
    public void testCheckEventSyntax() {
        // initially, it's valid
        assertThatCode(() -> mgr.checkEventSyntax(event)).doesNotThrowAnyException();

        event.setTarget("unknown-target");
        assertThatCode(() -> mgr.checkEventSyntax(event)).isInstanceOf(ControlLoopException.class)
                        .hasMessage("target field invalid");

        event.setTarget(null);
        assertThatCode(() -> mgr.checkEventSyntax(event)).isInstanceOf(ControlLoopException.class)
                        .hasMessage("No target field");

        // abated supersedes previous errors - so it shouldn't throw an exception
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ABATED);
        assertThatCode(() -> mgr.checkEventSyntax(event)).doesNotThrowAnyException();

        event.setRequestId(null);
        assertThatCode(() -> mgr.checkEventSyntax(event)).isInstanceOf(ControlLoopException.class)
                        .hasMessage("No request ID");

        event.setClosedLoopControlName(null);
        assertThatCode(() -> mgr.checkEventSyntax(event)).isInstanceOf(ControlLoopException.class)
                        .hasMessage("No control loop name");
    }

    @Test
    public void testValidateStatus() {
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        assertThatCode(() -> mgr.checkEventSyntax(event)).doesNotThrowAnyException();

        event.setClosedLoopEventStatus(ControlLoopEventStatus.ABATED);
        assertThatCode(() -> mgr.checkEventSyntax(event)).doesNotThrowAnyException();

        event.setClosedLoopEventStatus(null);
        assertThatCode(() -> mgr.checkEventSyntax(event)).isInstanceOf(ControlLoopException.class)
                        .hasMessage("Invalid value in closedLoopEventStatus");
    }

    @Test
    public void testValidateAaiData() {
        event.setTargetType("unknown-target-type");
        assertThatCode(() -> mgr.checkEventSyntax(event)).isInstanceOf(ControlLoopException.class)
                        .hasMessage("The target type is not supported");

        event.setTargetType(null);
        assertThatCode(() -> mgr.checkEventSyntax(event)).isInstanceOf(ControlLoopException.class)
                        .hasMessage("The Target type is null");

        event.setAai(null);
        assertThatCode(() -> mgr.checkEventSyntax(event)).isInstanceOf(ControlLoopException.class)
                        .hasMessage("AAI is null");

        // VM case
        event.setTargetType(ControlLoopTargetType.VM);
        event.setAai(Map.of(ControlLoopEventManager2.GENERIC_VNF_VNF_ID, MY_TARGET));
        assertThatCode(() -> mgr.checkEventSyntax(event)).doesNotThrowAnyException();

        event.setAai(Map.of());
        assertThatCode(() -> mgr.checkEventSyntax(event)).isInstanceOf(ControlLoopException.class);

        // VNF case
        event.setTargetType(ControlLoopTargetType.VNF);
        event.setAai(Map.of(ControlLoopEventManager2.GENERIC_VNF_VNF_ID, MY_TARGET));
        assertThatCode(() -> mgr.checkEventSyntax(event)).doesNotThrowAnyException();

        event.setAai(Map.of());
        assertThatCode(() -> mgr.checkEventSyntax(event)).isInstanceOf(ControlLoopException.class);

        // PNF case
        event.setTargetType(ControlLoopTargetType.PNF);
        event.setAai(Map.of(ControlLoopEventManager2.PNF_NAME, MY_TARGET));
        assertThatCode(() -> mgr.checkEventSyntax(event)).doesNotThrowAnyException();

        event.setAai(Map.of());
        assertThatCode(() -> mgr.checkEventSyntax(event)).isInstanceOf(ControlLoopException.class);
    }

    @Test
    public void testValidateAaiVmVnfData() {
        event.setTargetType(ControlLoopTargetType.VM);
        event.setAai(Map.of(ControlLoopEventManager2.GENERIC_VNF_VNF_ID, MY_TARGET));
        assertThatCode(() -> mgr.checkEventSyntax(event)).doesNotThrowAnyException();

        event.setAai(Map.of(ControlLoopEventManager2.VSERVER_VSERVER_NAME, MY_TARGET));
        assertThatCode(() -> mgr.checkEventSyntax(event)).doesNotThrowAnyException();

        event.setAai(Map.of(ControlLoopEventManager2.GENERIC_VNF_VNF_NAME, MY_TARGET));
        assertThatCode(() -> mgr.checkEventSyntax(event)).doesNotThrowAnyException();

        event.setAai(Map.of());
        assertThatCode(() -> mgr.checkEventSyntax(event)).isInstanceOf(ControlLoopException.class).hasMessage(
                        "generic-vnf.vnf-id or generic-vnf.vnf-name or vserver.vserver-name information missing");
    }

    @Test
    public void testValidateAaiPnfData() {
        event.setTargetType(ControlLoopTargetType.PNF);
        event.setAai(Map.of(ControlLoopEventManager2.PNF_NAME, MY_TARGET));
        assertThatCode(() -> mgr.checkEventSyntax(event)).doesNotThrowAnyException();

        event.setAai(Map.of());
        assertThatCode(() -> mgr.checkEventSyntax(event)).isInstanceOf(ControlLoopException.class)
                        .hasMessage("AAI PNF object key pnf-name is missing");
    }

    @Test
    public void testIsClosedLoopDisabled() {
        Map<String, String> orig = event.getAai();

        event.setAai(addAai(orig, ControlLoopEventManager2.VSERVER_IS_CLOSED_LOOP_DISABLED, "true"));
        assertThatThrownBy(() -> new ControlLoopEventManager2(params, event, workMem))
                        .isInstanceOf(IllegalStateException.class);

        event.setAai(addAai(orig, ControlLoopEventManager2.GENERIC_VNF_IS_CLOSED_LOOP_DISABLED, "true"));
        assertThatThrownBy(() -> new ControlLoopEventManager2(params, event, workMem))
                        .isInstanceOf(IllegalStateException.class);

        event.setAai(addAai(orig, ControlLoopEventManager2.PNF_IS_IN_MAINT, "true"));
        assertThatThrownBy(() -> new ControlLoopEventManager2(params, event, workMem))
                        .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testIsProvStatusInactive() {
        Map<String, String> orig = event.getAai();

        event.setAai(addAai(orig, ControlLoopEventManager2.VSERVER_PROV_STATUS, "ACTIVE"));
        assertThatCode(() -> new ControlLoopEventManager2(params, event, workMem)).doesNotThrowAnyException();

        event.setAai(addAai(orig, ControlLoopEventManager2.VSERVER_PROV_STATUS, "inactive"));
        assertThatThrownBy(() -> new ControlLoopEventManager2(params, event, workMem))
                        .isInstanceOf(IllegalStateException.class);

        event.setAai(addAai(orig, ControlLoopEventManager2.GENERIC_VNF_PROV_STATUS, "ACTIVE"));
        assertThatCode(() -> new ControlLoopEventManager2(params, event, workMem)).doesNotThrowAnyException();

        event.setAai(addAai(orig, ControlLoopEventManager2.GENERIC_VNF_PROV_STATUS, "inactive"));
        assertThatThrownBy(() -> new ControlLoopEventManager2(params, event, workMem))
                        .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testIsAaiTrue() {
        Map<String, String> orig = event.getAai();

        for (String value : Arrays.asList("yes", "y", "true", "t", "yEs", "trUe")) {
            event.setAai(addAai(orig, ControlLoopEventManager2.VSERVER_IS_CLOSED_LOOP_DISABLED, value));
            assertThatThrownBy(() -> new ControlLoopEventManager2(params, event, workMem))
                            .isInstanceOf(IllegalStateException.class);
        }

        event.setAai(addAai(orig, ControlLoopEventManager2.VSERVER_IS_CLOSED_LOOP_DISABLED, "false"));
        assertThatCode(() -> new ControlLoopEventManager2(params, event, workMem)).doesNotThrowAnyException();

        event.setAai(addAai(orig, ControlLoopEventManager2.VSERVER_IS_CLOSED_LOOP_DISABLED, "no"));
        assertThatCode(() -> new ControlLoopEventManager2(params, event, workMem)).doesNotThrowAnyException();
    }


    private Map<String, String> addAai(Map<String, String> original, String key, String value) {
        Map<String, String> map = new TreeMap<>(original);
        map.put(key, value);
        return map;
    }

    private void loadPolicy(String fileName) throws CoderException {
        ToscaServiceTemplate template =
                        yamlCoder.decode(ResourceUtils.getResourceAsString(fileName), ToscaServiceTemplate.class);
        tosca = template.getToscaTopologyTemplate().getPolicies().get(0).values().iterator().next();

        params.setToscaPolicy(tosca);
    }

    private void loadStepsWithProperties(String... properties) {
        stepa = new Step2(mgr, ControlLoopOperationParams.builder().build(), event) {

            @Override
            public boolean isPolicyStep() {
                return false;
            }

            @Override
            public List<String> getPropertyNames() {
                return List.of(properties);
            }

            @Override
            protected Operation buildOperation() {
                return policyOperation;
            }
        };

        mgr.getSteps().add(stepa);
        mgr.getSteps().add(stepb);
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

    private void checkResp(OperationOutcome outcome, String expectedPayload) {
        ControlLoopResponse resp = mgr.makeControlLoopResponse(outcome);
        assertNotNull(resp);
        assertEquals(REQ_ID, resp.getRequestId());
        assertEquals(expectedPayload, resp.getPayload());
    }

    /**
     * Sets the target entity so a step doesn't have to be added to set it.
     */
    private void setTargetEntity() {
        mgr.setProperty(OperationProperties.AAI_TARGET_ENTITY, MY_TARGET);
    }


    private class MyManager extends UsecasesEventManager {
        private static final long serialVersionUID = 1L;

        public MyManager(ControlLoopParams params, VirtualControlLoopEvent event, WorkingMemory workMem)
                        throws ControlLoopException {

            super(params, event, workMem);
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
    }
}
