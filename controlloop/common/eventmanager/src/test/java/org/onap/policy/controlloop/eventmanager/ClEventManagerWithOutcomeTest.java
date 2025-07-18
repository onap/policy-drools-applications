/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021, 2023 AT&T Intellectual Property. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import org.drools.core.WorkingMemory;
import org.drools.core.common.InternalFactHandle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.controlloop.ControlLoopException;
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

class ClEventManagerWithOutcomeTest {
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

    private final PolicyEngine engineMgr = mock(PolicyEngine.class);
    private final WorkingMemory workMem = mock(WorkingMemory.class);
    private final InternalFactHandle factHandle = mock(InternalFactHandle.class);
    private final EventManagerServices services = mock(EventManagerServices.class);
    private final ExecutorService executor = mock(ExecutorService.class);

    private List<LockImpl> locks;
    private ToscaPolicy tosca;
    private ControlLoopParams params;
    private ClEventManagerWithOutcome<MyStep> mgr;

    /**
     * Sets up.
     */
    @BeforeEach
    void setUp() throws ControlLoopException, CoderException {
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
    void testLoadNextPolicy_testGetFullHistory_testGetPartialHistory() throws Exception {
        loadPolicy(EVENT_MGR_MULTI_YAML);
        mgr = new MyManager(services, params, REQ_ID, workMem);

        // start and load step for first policy
        mgr.start();
        assertEquals("OperationA", Objects.requireNonNull(mgr.getSteps().poll()).getOperationName());
        assertNull(mgr.getFinalResult());

        // add an outcome
        var outcome = makeOutcome();
        mgr.addToHistory(outcome);

        // indicate success and load next policy
        mgr.loadNextPolicy(OperationResult.SUCCESS);
        assertEquals("OperationB", Objects.requireNonNull(mgr.getSteps().poll()).getOperationName());
        assertNull(mgr.getFinalResult());

        // loadPolicy() should clear the partial history, but not the full history
        assertThat(mgr.getPartialHistory()).isEmpty();
        assertThat(mgr.getFullHistory()).hasSize(1);
    }

    @Test
    void testExecuteStep() {
        mgr.bumpAttempts();

        // no steps to execute
        assertFalse(mgr.executeStep());
        assertEquals(0, mgr.getAttempts());
    }

    @Test
    void testBumpAttempts() {
        assertEquals(0, mgr.getAttempts());

        mgr.bumpAttempts();
        mgr.bumpAttempts();
        assertEquals(2, mgr.getAttempts());
    }

    @Test
    void testIsAbort() {
        var outcome = makeCompletedOutcome();

        outcome.setResult(OperationResult.FAILURE);
        assertTrue(mgr.isAbort(outcome));

        // no effect for success
        outcome.setResult(OperationResult.SUCCESS);
        assertFalse(mgr.isAbort(outcome));
    }

    @Test
    void testAddToHistory() throws ControlLoopException {
        mgr.start();

        // add a "start" outcome
        var outcome = makeOutcome();
        mgr.addToHistory(outcome);

        assertThat(mgr.getPartialHistory()).hasSize(1);
        assertThat(mgr.getFullHistory()).hasSize(1);

        // add a "completion" outcome - should replace the start
        outcome = makeCompletedOutcome();
        mgr.addToHistory(outcome);

        assertThat(mgr.getPartialHistory()).hasSize(1);
        assertThat(mgr.getFullHistory()).hasSize(1);
        assertSame(outcome, mgr.getPartialHistory().peek().getOutcome());
        assertNotNull(mgr.getFullHistory().peek());
        assertSame(outcome, mgr.getFullHistory().peek().getOutcome());

        // add another start
        outcome = makeOutcome();
        mgr.addToHistory(outcome);

        assertThat(mgr.getPartialHistory()).hasSize(2);
        assertThat(mgr.getFullHistory()).hasSize(2);
        assertSame(outcome, mgr.getPartialHistory().peekLast().getOutcome());
        assertNotNull(mgr.getFullHistory().peekLast());
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
        assertNotNull(mgr.getFullHistory().peekLast());
        assertSame(outcome, mgr.getFullHistory().peekLast().getOutcome());
    }

    @Test
    void testMakeNotification() throws Exception {
        loadPolicy(EVENT_MGR_MULTI_YAML);
        mgr = new MyManager(services, params, REQ_ID, workMem);

        // before started
        assertNotNull(mgr.makeNotification());

        mgr.start();

        mgr.addToHistory(makeCompletedOutcome());
        mgr.addToHistory(makeCompletedOutcome());
        mgr.addToHistory(makeCompletedOutcome());

        // check notification while running
        var notif = mgr.makeNotification();
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
    void testGetOperationMessage() throws ControlLoopException {
        // no history yet
        assertNull(mgr.getOperationMessage());

        // add an outcome
        mgr.start();
        var outcome = makeOutcome();
        mgr.addToHistory(outcome);

        assertThat(mgr.getOperationMessage()).contains("actor=" + SIMPLE_ACTOR)
                        .contains("operation=" + SIMPLE_OPERATION);
    }

    @Test
    void testMakeControlLoopResponse() {
        final var outcome = new OperationOutcome();
        outcome.setActor(SIMPLE_ACTOR);

        var resp = mgr.makeControlLoopResponse(outcome);
        assertNotNull(resp);
        assertEquals(SIMPLE_ACTOR, resp.getFrom());
    }

    private void loadPolicy(String fileName) throws CoderException {
        var template = yamlCoder.decode(ResourceUtils.getResourceAsString(fileName), ToscaServiceTemplate.class);
        tosca = template.getToscaTopologyTemplate().getPolicies().get(0).values().iterator().next();

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


    private class MyManager extends ClEventManagerWithOutcome<MyStep> {
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


    private static class MyStep extends Step {
        public MyStep(ControlLoopOperationParams params) {
            super(params, new AtomicReference<>());
        }
    }
}
