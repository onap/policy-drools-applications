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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.drools.core.WorkingMemory;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.runtime.rule.FactHandle;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.common.utils.io.Serializer;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.actorserviceprovider.ActorService;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.OperationProperties;
import org.onap.policy.controlloop.actorserviceprovider.Operator;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.actorserviceprovider.spi.Actor;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.controlloop.eventmanager.ControlLoopEventManager2;
import org.onap.policy.controlloop.eventmanager.ControlLoopOperationManager2;
import org.onap.policy.controlloop.ophistory.OperationHistoryDataManager;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.controlloop.policy.Target;
import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.drools.apps.controller.usecases.step.AaiCqStep2;
import org.onap.policy.drools.apps.controller.usecases.step.AaiGetPnfStep2;
import org.onap.policy.drools.apps.controller.usecases.step.AaiGetTenantStep2;
import org.onap.policy.drools.apps.controller.usecases.step.GuardStep2;
import org.onap.policy.drools.apps.controller.usecases.step.LockStep2;
import org.onap.policy.drools.apps.controller.usecases.step.Step2;
import org.onap.policy.drools.core.lock.LockCallback;
import org.onap.policy.drools.core.lock.LockImpl;
import org.onap.policy.drools.core.lock.LockState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

public class UsecasesEventManagerTest {
    private static final UUID REQ_ID = UUID.randomUUID();
    private static final String CL_NAME = "my-closed-loop-name";
    private static final String POLICY_NAME = "my-policy-name";
    private static final String POLICY_SCOPE = "my-scope";
    private static final String POLICY_VERSION = "1.2.3";
    private static final String SIMPLE_ACTOR = "First";
    private static final String SIMPLE_OPERATION = "OperationA";
    private static final String MY_TARGET = "my-target";
    private static final String LOCK1 = "my-lock-A";
    private static final String LOCK2 = "my-lock-B";
    private static final String EVENT_MGR_MULTI_YAML = "../eventmanager/src/test/resources/eventManager/event-mgr-multi.yaml";
    private static final String EVENT_MGR_SIMPLE_YAML = "../eventmanager/src/test/resources/eventManager/event-mgr-simple.yaml";
    private static final Coder yamlCoder = new StandardYamlCoder();
    private static final String OUTCOME_MSG = "my outcome message";

    @Mock
    private WorkingMemory workMem;
    @Mock
    private Consumer<OperationOutcome> callback1;
    @Mock
    private Consumer<OperationOutcome> callback2;
    @Mock
    private Consumer<OperationOutcome> callback3;
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
    private ControlLoopOperationManager2 oper1;
    @Mock
    private ControlLoopOperationManager2 oper2;
    @Mock
    private ControlLoopOperationManager2 oper3;
    @Mock
    private ExecutorService executor;
    @Mock
    private Step2 stepa;
    @Mock
    private Step2 stepb;

    private long preCreateTimeMs;
    private List<LockImpl> locks;
    private Target target;
    private ToscaPolicy tosca;
    private ControlLoopParams params;
    private VirtualControlLoopEvent event;
    private int updateCount;
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

        when(oper1.getHistory()).thenReturn(makeHistory("A"));
        when(oper2.getHistory()).thenReturn(makeHistory("B"));
        when(oper3.getHistory()).thenReturn(makeHistory("C"));

        when(oper1.getActor()).thenReturn("First");
        when(oper1.getOperation()).thenReturn("OperationA");
        when(oper1.getOperationMessage()).thenReturn("message-A");
        when(oper1.getOperationHistory()).thenReturn("history-A");

        when(oper2.getActor()).thenReturn("Second");
        when(oper2.getOperation()).thenReturn("OperationB");
        when(oper2.getOperationMessage()).thenReturn("message-B");
        when(oper2.getOperationHistory()).thenReturn("history-B");

        when(oper3.getActor()).thenReturn("Third");
        when(oper3.getOperation()).thenReturn("OperationC");
        when(oper3.getOperationMessage()).thenReturn("message-C");
        when(oper3.getOperationHistory()).thenReturn("history-C");

        when(workMem.getFactHandle(any())).thenReturn(factHandle);

        event = new VirtualControlLoopEvent();
        event.setRequestId(REQ_ID);
        event.setTarget(ControlLoopOperationManager2.VSERVER_VSERVER_NAME);
        event.setAai(new TreeMap<>(Map.of(ControlLoopOperationManager2.VSERVER_VSERVER_NAME, MY_TARGET)));
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event.setClosedLoopControlName(CL_NAME);
        event.setTargetType(TargetType.VNF.toString());

        target = new Target();
        target.setType(TargetType.VNF);

        params = new ControlLoopParams();
        params.setClosedLoopControlName(CL_NAME);
        params.setPolicyName(POLICY_NAME);
        params.setPolicyScope(POLICY_SCOPE);
        params.setPolicyVersion(POLICY_VERSION);

        loadPolicy(EVENT_MGR_SIMPLE_YAML);

        locks = new ArrayList<>();

        updateCount = 0;

        preCreateTimeMs = System.currentTimeMillis();

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

        UsecasesEventManager mgr2 = Serializer.roundTrip(mgr);
        assertFalse(mgr2.isActive());
    }

    @Test
    public void testDestroy() {
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
        OperationOutcome outcome = makeOutcome();
        outcome.setEnd(outcome.getStart());

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
    public void testLoadNextPolicy() throws Exception {
        loadPolicy(EVENT_MGR_MULTI_YAML);
        mgr = new MyManager(params, event, workMem);

        // start and load step for first policy
        mgr.start();
        assertEquals("OperationA", mgr.getSteps().poll().getOperationName());

        // add an outcome
        OperationOutcome outcome = makeOutcome();
        mgr.addOutcome(outcome);

        // indicate success and load next policy
        mgr.setResult(PolicyResult.SUCCESS);
        mgr.loadNextPolicy();
        assertEquals("OperationB", mgr.getSteps().poll().getOperationName());

        // loadPolicy() should clear the partial history, but not the full history
        assertThat(mgr.getPartialHistory()).isEmpty();
        assertThat(mgr.getFullHistory()).hasSize(1);

        // indicate failure - should go to final failure
        mgr.setResult(PolicyResult.FAILURE);
        mgr.loadNextPolicy();

        // should be inactive due to final failure
        assertFalse(mgr.isActive());
    }

    @Test
    public void testLoadPolicy() throws ControlLoopException {
        mgr.start();

        Step2 step = mgr.getSteps().peek();
        assertNotNull(step);
        assertEquals("First", step.getActorName());
        assertEquals("OperationA", step.getOperationName());

        ControlLoopOperationParams params2 = step.getParams();
        assertSame(actors, params2.getActorService());
        assertSame(REQ_ID, params2.getRequestId());
        assertSame(ForkJoinPool.commonPool(), params2.getExecutor());
        assertNotNull(params2.getTarget());
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

        assertThat(steps.poll()).isInstanceOf(LockStep2.class);
        assertThat(steps.poll()).isInstanceOf(AaiCqStep2.class);
        assertThat(steps.poll()).isInstanceOf(GuardStep2.class);
        assertSame(stepa, steps.poll());
        assertSame(stepb, steps.poll());
        assertThat(steps).isEmpty();
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
        loadStepsWithProperties(OperationProperties.AAI_PNF);

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
        event.getAai().put(Step2.VSERVER_VSERVER_NAME, "my-vserver");
        loadStepsWithProperties(OperationProperties.AAI_VSERVER_LINK);

        mgr.loadPreprocessorSteps();

        Deque<Step2> steps = mgr.getSteps();

        assertThat(steps.poll()).isInstanceOf(AaiGetTenantStep2.class);
        assertSame(stepa, steps.poll());
        assertSame(stepb, steps.poll());
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
    public void testAddOutcome() throws ControlLoopException {
        mgr.start();

        // add a "start" outcome
        OperationOutcome outcome = makeOutcome();
        mgr.addOutcome(outcome);

        assertThat(mgr.getPartialHistory()).hasSize(1);
        assertThat(mgr.getFullHistory()).hasSize(1);

        // add a "completion" outcome - should replace the start
        outcome = makeOutcome();
        outcome.setEnd(Instant.now());
        mgr.addOutcome(outcome);

        assertThat(mgr.getPartialHistory()).hasSize(1);
        assertThat(mgr.getFullHistory()).hasSize(1);
        assertSame(outcome, mgr.getPartialHistory().peek().getOutcome());
        assertSame(outcome, mgr.getFullHistory().peek().getOutcome());

        // add another start
        outcome = makeOutcome();
        mgr.addOutcome(outcome);

        assertThat(mgr.getPartialHistory()).hasSize(2);
        assertThat(mgr.getFullHistory()).hasSize(2);
        assertSame(outcome, mgr.getPartialHistory().peekLast().getOutcome());
        assertSame(outcome, mgr.getFullHistory().peekLast().getOutcome());

        // remove the last item from the full history and then add a "completion"
        mgr.getFullHistory().removeLast();
        outcome = makeOutcome();
        outcome.setEnd(Instant.now());
        mgr.addOutcome(outcome);
        assertThat(mgr.getPartialHistory()).hasSize(2);
        assertThat(mgr.getFullHistory()).hasSize(2);

        // add another "start"
        outcome = makeOutcome();
        mgr.addOutcome(outcome);
        assertThat(mgr.getPartialHistory()).hasSize(3);
        assertThat(mgr.getFullHistory()).hasSize(3);

        // add a "completion" for a different actor - should NOT replace the start
        outcome = makeOutcome();
        outcome.setActor("different-actor");
        outcome.setEnd(Instant.now());
        mgr.addOutcome(outcome);
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

        mgr.addOutcome(makeOutcome());
        mgr.addOutcome(makeOutcome());
        mgr.addOutcome(makeOutcome());

        // check notification while running
        VirtualControlLoopNotification notif = mgr.makeNotification();
        assertThat(notif.getMessage()).contains(SIMPLE_ACTOR);
        assertThat(notif.getHistory()).hasSize(3);

        // indicate success and load the next policy - should clear the partial history
        mgr.setResult(PolicyResult.SUCCESS);
        mgr.loadNextPolicy();

        mgr.addOutcome(makeOutcome());
        mgr.addOutcome(makeOutcome());

        // check notification
        notif = mgr.makeNotification();
        assertNull(notif.getMessage());

        // should only have history for last two outcomes
        assertThat(notif.getHistory()).hasSize(2);

        // indicate failure - should go to final state
        mgr.setResult(PolicyResult.FAILURE);
        mgr.loadNextPolicy();

        // check notification
        notif = mgr.makeNotification();
        assertNull(notif.getMessage());

        // should be no history
        assertThat(notif.getHistory()).isEmpty();
    }

    @Test
    public void testMakeControlLoopResponse() {
        fail("Not yet implemented");
    }

    @Test
    public void testOnNewEvent() {
        fail("Not yet implemented");
    }

    @Test
    public void testCheckEventSyntax() {
        fail("Not yet implemented");
    }

    @Test
    public void testValidateStatus() {
        fail("Not yet implemented");
    }

    @Test
    public void testValidateAaiData() {
        fail("Not yet implemented");
    }

    @Test
    public void testValidateAaiVmVnfData() {
        fail("Not yet implemented");
    }

    @Test
    public void testValidateAaiPnfData() {
        fail("Not yet implemented");
    }

    @Test
    public void testIsClosedLoopDisabled() {
        fail("Not yet implemented");
    }

    @Test
    public void testIsProvStatusInactive() {
        fail("Not yet implemented");
    }

    @Test
    public void testIsAaiTrue() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetSteps() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetResult() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetFullHistory() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetPartialHistory() {
        fail("Not yet implemented");
    }


    private Map<String, String> addAai(Map<String, String> original, String key, String value) {
        Map<String, String> map = new TreeMap<>(original);
        map.put(key, value);
        return map;
    }

    private List<ControlLoopOperation> makeHistory(String message) {
        ControlLoopOperation clo = new ControlLoopOperation();
        clo.setMessage("history-" + message);

        return List.of(clo);
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

    private void runExecutor() {
        ArgumentCaptor<Runnable> runCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runCaptor.capture());

        runCaptor.getValue().run();
    }

    private OperationOutcome makeOutcome() {
        OperationOutcome outcome = new OperationOutcome();
        outcome.setActor(SIMPLE_ACTOR);
        outcome.setOperation(SIMPLE_OPERATION);
        outcome.setMessage(OUTCOME_MSG);
        outcome.setResult(PolicyResult.SUCCESS);
        outcome.setStart(Instant.now());
        outcome.setTarget(MY_TARGET);

        return outcome;
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
    }
}
