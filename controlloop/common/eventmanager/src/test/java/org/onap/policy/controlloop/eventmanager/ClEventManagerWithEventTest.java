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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
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
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopResponse;
import org.onap.policy.controlloop.ControlLoopTargetType;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.actorserviceprovider.ActorService;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.OperationResult;
import org.onap.policy.controlloop.actorserviceprovider.Operator;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.actorserviceprovider.spi.Actor;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.controlloop.eventmanager.ClEventManagerWithEvent.NewEventStatus;
import org.onap.policy.controlloop.ophistory.OperationHistoryDataManager;
import org.onap.policy.drools.core.lock.LockCallback;
import org.onap.policy.drools.core.lock.LockImpl;
import org.onap.policy.drools.core.lock.LockState;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

@RunWith(MockitoJUnitRunner.class)
public class ClEventManagerWithEventTest {
    private static final UUID REQ_ID = UUID.randomUUID();
    private static final String CL_NAME = "my-closed-loop-name";
    private static final String POLICY_NAME = "my-policy-name";
    private static final String POLICY_SCOPE = "my-scope";
    private static final String POLICY_VERSION = "1.2.3";
    private static final String SIMPLE_ACTOR = "First";
    private static final String SIMPLE_OPERATION = "OperationA";
    private static final String TARGET_PROP = "my-target-property";
    private static final String MY_TARGET = "my-target";
    private static final String EVENT_MGR_MULTI_YAML =
                    "../eventmanager/src/test/resources/eventManager/event-mgr-multi.yaml";
    private static final String EVENT_MGR_SIMPLE_YAML =
                    "../eventmanager/src/test/resources/eventManager/event-mgr-simple.yaml";
    private static final Coder yamlCoder = new StandardYamlCoder();
    private static final String OUTCOME_MSG = "my outcome message";

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
    private VirtualControlLoopEvent event;
    private ClEventManagerWithEvent<MyStep> mgr;

    /**
     * Sets up.
     */
    @Before
    public void setUp() throws ControlLoopException, CoderException {
        when(workMem.getFactHandle(any())).thenReturn(factHandle);

        event = new VirtualControlLoopEvent();
        event.setRequestId(REQ_ID);
        event.setTarget(TARGET_PROP);
        event.setAai(new TreeMap<>(Map.of(TARGET_PROP, MY_TARGET)));
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

        // valid
        assertThatCode(() -> mgr.checkEventSyntax(event)).doesNotThrowAnyException();

        // invalid
        event.setTarget("");
        assertThatThrownBy(() -> new MyManager(params, event, workMem)).isInstanceOf(ControlLoopException.class);
    }

    @Test
    public void testPopulateNotification() throws Exception {
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
        assertThat(notif.getAai()).isEqualTo(event.getAai());
        assertThat(notif.getClosedLoopAlarmEnd()).isEqualTo(event.getClosedLoopAlarmEnd());
        assertThat(notif.getClosedLoopAlarmStart()).isEqualTo(event.getClosedLoopAlarmStart());
        assertThat(notif.getClosedLoopControlName()).isEqualTo(event.getClosedLoopControlName());
        assertThat(notif.getClosedLoopEventClient()).isEqualTo(event.getClosedLoopEventClient());
        assertThat(notif.getFrom()).isEqualTo("policy");
        assertThat(notif.getTarget()).isEqualTo(event.getTarget());
        assertThat(notif.getTargetType()).isEqualTo(event.getTargetType());

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
    public void testStoreInDataBase() throws ControlLoopException {
        mgr.start();
        OperationOutcome outcome = makeOutcome();
        mgr.addToHistory(outcome);

        mgr.storeInDataBase(mgr.getPartialHistory().peekLast(), MY_TARGET);

        verify(dataMgr).store(REQ_ID.toString(), event.getClosedLoopControlName(), event, MY_TARGET,
                        mgr.getPartialHistory().peekLast().getClOperation());
    }

    @Test
    public void testMakeControlLoopResponse() {
        final OperationOutcome outcome = new OperationOutcome();

        ControlLoopResponse resp = mgr.makeControlLoopResponse(outcome);
        assertNotNull(resp);
        assertEquals("DCAE", resp.getTarget());
        assertEquals(event.getClosedLoopControlName(), resp.getClosedLoopControlName());
        assertEquals(event.getPolicyName(), resp.getPolicyName());
        assertEquals(event.getPolicyVersion(), resp.getPolicyVersion());
        assertEquals(REQ_ID, resp.getRequestId());
        assertEquals(event.getVersion(), resp.getVersion());
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


    private class MyManager extends ClEventManagerWithEvent<MyStep> {
        private static final long serialVersionUID = 1L;

        public MyManager(ControlLoopParams params, VirtualControlLoopEvent event, WorkingMemory workMem)
                        throws ControlLoopException {

            super(null, params, event, workMem);
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
        protected void loadPolicyStep(ControlLoopOperationParams params) {
            getSteps().add(new MyStep(this, params, getEvent()));
        }
    }

    private static class MyStep extends Step {
        public MyStep(StepContext stepContext, ControlLoopOperationParams params, VirtualControlLoopEvent event) {
            super(params, new AtomicReference<>());
        }
    }
}
