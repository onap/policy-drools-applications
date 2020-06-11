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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.aai.domain.yang.GenericVnf;
import org.onap.policy.aai.AaiCqResponse;
import org.onap.policy.common.utils.time.PseudoExecutor;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.ControlLoopResponse;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actor.guard.DecisionOperation;
import org.onap.policy.controlloop.actor.guard.GuardActorServiceProvider;
import org.onap.policy.controlloop.actorserviceprovider.ActorService;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.Operator;
import org.onap.policy.controlloop.actorserviceprovider.controlloop.ControlLoopEventContext;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.actorserviceprovider.spi.Actor;
import org.onap.policy.controlloop.ophistory.OperationHistoryDataManager;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.controlloop.policy.Target;
import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.sdnr.PciBody;
import org.onap.policy.sdnr.PciMessage;
import org.onap.policy.sdnr.PciResponse;

public class ControlLoopOperationManager2Test {
    private static final UUID REQ_ID = UUID.randomUUID();
    private static final String MISMATCH = "mismatch";
    private static final String POLICY_ID = "my-policy";
    private static final String POLICY_ACTOR = "my-actor";
    private static final String POLICY_OPERATION = "my-operation";
    private static final String OTHER_ACTOR = "another-actor";
    private static final String MY_TARGET = "my-target";
    private static final String MY_VNF_ID = "my-vnf-id";
    private static final String PAYLOAD_KEY = "payload-key";
    private static final String PAYLOAD_VALUE = "payload-value";
    private static final long REMAINING_MS = 5000;
    private static final int MAX_RUN = 100;
    private static final Integer POLICY_RETRY = 3;
    private static final Integer POLICY_TIMEOUT = 20;
    private static final IllegalArgumentException EXPECTED_EXCEPTION =
                    new IllegalArgumentException("expected exception");

    @Captor
    private ArgumentCaptor<Consumer<OperationOutcome>> lockCallback;

    @Mock
    private OperationHistoryDataManager dataMgr;
    @Mock
    private ManagerContext mgrctx;
    @Mock
    private Operator policyOperator;
    @Mock
    private Operation policyOperation;
    @Mock
    private Actor policyActor;
    @Mock
    private ActorService actors;
    @Mock
    private AaiCqResponse cqdata;
    @Mock
    private GenericVnf vnf;

    private CompletableFuture<OperationOutcome> lockFuture;
    private CompletableFuture<OperationOutcome> policyFuture;
    private Target target;
    private Map<String, String> payload;
    private Policy policy;
    private VirtualControlLoopEvent event;
    private ControlLoopEventContext context;
    private PseudoExecutor executor;
    private ControlLoopOperationManager2 mgr;

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        lockFuture = new CompletableFuture<>();
        policyFuture = new CompletableFuture<>();

        when(mgrctx.getActorService()).thenReturn(actors);
        when(mgrctx.getDataManager()).thenReturn(dataMgr);
        when(mgrctx.requestLock(any(), any())).thenReturn(lockFuture);

        // configure policy operation
        when(actors.getActor(POLICY_ACTOR)).thenReturn(policyActor);
        when(policyActor.getOperator(POLICY_OPERATION)).thenReturn(policyOperator);
        when(policyOperator.buildOperation(any())).thenReturn(policyOperation);
        when(policyOperation.start()).thenReturn(policyFuture);

        when(vnf.getVnfId()).thenReturn(MY_VNF_ID);
        when(cqdata.getDefaultGenericVnf()).thenReturn(vnf);

        target = new Target();
        target.setType(TargetType.VM);

        payload = Map.of(PAYLOAD_KEY, PAYLOAD_VALUE);

        policy = new Policy();
        policy.setId(POLICY_ID);
        policy.setActor(POLICY_ACTOR);
        policy.setRecipe(POLICY_OPERATION);
        policy.setTarget(target);
        policy.setPayload(payload);
        policy.setRetry(POLICY_RETRY);
        policy.setTimeout(POLICY_TIMEOUT);

        event = new VirtualControlLoopEvent();
        event.setRequestId(REQ_ID);
        event.setTarget(ControlLoopOperationManager2.VSERVER_VSERVER_NAME);
        event.setAai(new TreeMap<>(Map.of(ControlLoopOperationManager2.VSERVER_VSERVER_NAME, MY_TARGET)));

        context = new ControlLoopEventContext(event);
        context.setProperty(AaiCqResponse.CONTEXT_KEY, cqdata);

        executor = new PseudoExecutor();

        mgr = new ControlLoopOperationManager2(mgrctx, context, policy, executor);
    }

    @Test
    public void testStart() {
        mgr.start(REMAINING_MS);

        // should have determined the target entity by now
        assertEquals(MY_TARGET, mgr.getTargetEntity());

        verify(mgrctx).requestLock(eq(MY_TARGET), any());

        lockFuture.complete(new OperationOutcome());
        genGuardOutcome();
        policyFuture.complete(genOpOutcome());
        runToCompletion();

        assertEquals(ControlLoopOperationManager2.State.GUARD_STARTED, mgr.getState());

        assertTrue(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.GUARD_PERMITTED, mgr.getState());

        assertTrue(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.OPERATION_STARTED, mgr.getState());

        assertTrue(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.OPERATION_SUCCESS, mgr.getState());

        assertFalse(mgr.nextStep());

        OperationOutcome outcome = mgr.getOutcomes().peek();
        assertEquals(PolicyResult.SUCCESS, outcome.getResult());
        assertTrue(outcome.isFinalOutcome());

        verify(mgrctx, times(4)).updated(mgr);
    }

    /**
     * Tests start() when detmTarget() (i.e., the first task) throws an exception.
     */
    @Test
    public void testStartDetmTargetException() {
        policy.setTarget(new Target());
        mgr.start(REMAINING_MS);

        runToCompletion();

        assertFalse(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.OPERATION_FAILURE, mgr.getState());

        // should have called update() for operation-start, but not for any nextStep()
        verify(mgrctx).updated(mgr);
    }

    /**
     * Tests start() when a subsequent task throws an exception.
     */
    @Test
    public void testStartException() {
        when(policyOperation.start()).thenThrow(EXPECTED_EXCEPTION);

        mgr.start(REMAINING_MS);

        lockFuture.complete(new OperationOutcome());
        runToCompletion();

        assertFalse(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.OPERATION_FAILURE, mgr.getState());

        // should have called update() for operation-start, but not for any nextStep()
        verify(mgrctx).updated(mgr);
    }

    /**
     * Tests start() when the control loop times out before the operation starts.
     */
    @Test
    public void testStartClTimeout_testHandleTimeout() throws InterruptedException {
        // catch the callback when it times out
        CountDownLatch updatedLatch = new CountDownLatch(1);
        doAnswer(args -> {
            updatedLatch.countDown();
            return null;
        }).when(mgrctx).updated(any());

        long tstart = System.currentTimeMillis();

        // give it a short timeout
        mgr.start(100);

        assertTrue(updatedLatch.await(5, TimeUnit.SECONDS));
        assertTrue(System.currentTimeMillis() - tstart >= 100);

        // don't generate any responses
        runToCompletion();

        // wait for the future to be canceled, via a background thread
        CountDownLatch futureLatch = new CountDownLatch(1);
        mgr.getFuture().whenComplete((unused, thrown) -> futureLatch.countDown());
        assertTrue(futureLatch.await(5, TimeUnit.SECONDS));

        // lock should have been canceled
        assertTrue(mgr.getFuture().isCancelled());

        assertFalse(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.CONTROL_LOOP_TIMEOUT, mgr.getState());

        // should have called update() for operation-start, but not for any nextStep()
        verify(mgrctx).updated(mgr);

        // should have added a record to the DB
        verify(dataMgr).store(any(), any(), any(), any());
    }

    @Test
    public void testStartOperation() {
        mgr.start(REMAINING_MS);

        lockFuture.complete(new OperationOutcome());
        genGuardOutcome();
        runToCompletion();

        verify(policyOperation).start();

        ArgumentCaptor<ControlLoopOperationParams> captor = ArgumentCaptor.forClass(ControlLoopOperationParams.class);
        verify(policyOperator).buildOperation(captor.capture());

        ControlLoopOperationParams params = captor.getValue();

        assertNotNull(params);
        assertEquals(POLICY_ACTOR, params.getActor());
        assertSame(actors, params.getActorService());
        assertNotNull(params.getCompleteCallback());
        assertSame(context, params.getContext());
        assertSame(executor, params.getExecutor());
        assertEquals(POLICY_OPERATION, params.getOperation());
        assertEquals(payload, params.getPayload());
        assertSame(REQ_ID, params.getRequestId());
        assertSame(POLICY_RETRY, params.getRetry());
        assertNotNull(params.getStartCallback());
        assertSame(target, params.getTarget());
        assertEquals(MY_TARGET, params.getTargetEntity());
        assertSame(POLICY_TIMEOUT, params.getTimeoutSec());
    }

    @Test
    public void testStartOperationNullPayload() {
        policy.setPayload(null);
        mgr.start(REMAINING_MS);

        lockFuture.complete(new OperationOutcome());
        genGuardOutcome();
        runToCompletion();

        verify(policyOperation).start();

        ArgumentCaptor<ControlLoopOperationParams> captor = ArgumentCaptor.forClass(ControlLoopOperationParams.class);
        verify(policyOperator).buildOperation(captor.capture());

        ControlLoopOperationParams params = captor.getValue();

        assertNotNull(params);
        assertEquals(POLICY_ACTOR, params.getActor());
        assertSame(actors, params.getActorService());
        assertNotNull(params.getCompleteCallback());
        assertSame(context, params.getContext());
        assertSame(executor, params.getExecutor());
        assertEquals(POLICY_OPERATION, params.getOperation());
        assertTrue(params.getPayload().isEmpty());
        assertSame(REQ_ID, params.getRequestId());
        assertSame(POLICY_RETRY, params.getRetry());
        assertNotNull(params.getStartCallback());
        assertSame(target, params.getTarget());
        assertEquals(MY_TARGET, params.getTargetEntity());
        assertSame(POLICY_TIMEOUT, params.getTimeoutSec());
    }

    @Test
    public void testMakeControlLoopResponse() {
        final OperationOutcome outcome = new OperationOutcome();
        PciMessage msg = new PciMessage();
        outcome.setResponse(msg);

        PciBody body = new PciBody();
        msg.setBody(body);

        PciResponse output = new PciResponse();
        body.setOutput(output);

        output.setPayload("my-payload");


        // not an SDNR action - should return null
        assertNull(mgr.makeControlLoopResponse(outcome));

        /*
         * now work with SDNR actor
         */
        policy.setActor("SDNR");
        mgr = new ControlLoopOperationManager2(mgrctx, context, policy, executor);

        // should return null for a null input
        assertNull(mgr.makeControlLoopResponse(null));

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
    public void testGetOperationMessage() {
        // no history yet
        assertNull(mgr.getOperationMessage());

        runCyle();
        assertThat(mgr.getOperationMessage()).contains("actor=my-actor").contains("operation=my-operation");
    }

    @Test
    public void testGetOperationResult() {
        // no history yet
        assertNotNull(mgr.getOperationResult());

        runCyle();
        assertEquals(PolicyResult.SUCCESS, mgr.getOperationResult());
    }

    /**
     * Tests getOperationResult() when it ends in a failure.
     */
    @Test
    public void testGetOperationResultFailure() {
        mgr.start(REMAINING_MS);

        genLockFailure();
        runToCompletion();

        assertEquals(PolicyResult.FAILURE_GUARD, mgr.getOperationResult());
    }

    /**
     * Tests handleException() when the exception is a "cancel".
     */
    @Test
    public void testHandleExceptionCanceled() {
        lockFuture.cancel(false);

        mgr.start(REMAINING_MS);

        runToCompletion();

        assertTrue(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.ACTIVE, mgr.getState());
    }

    @Test
    public void testCancel() {
        mgr.start(REMAINING_MS);

        mgr.cancel();
        assertTrue(mgr.getFuture().isCancelled());
    }

    /**
     * Tests cancel() when the operation hasn't been started.
     */
    @Test
    public void testCancelNotStarted() {
        assertNull(mgr.getFuture());

        mgr.cancel();
        assertNull(mgr.getFuture());
    }

    @Test
    public void testLockUnavailable() {
        mgr.start(REMAINING_MS);

        runToCompletion();

        // lock failure outcome
        final OperationOutcome outcome = genLockFailure();

        runToCompletion();

        assertFalse(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.LOCK_DENIED, mgr.getState());

        assertEquals(outcome, mgr.getOutcomes().peek());

        // should have called update() for operation-start, but not for any nextStep()
        verify(mgrctx).updated(mgr);
    }

    /**
     * Tests onStart() and onComplete() with other actors.
     */
    @Test
    public void testOnStart_testOnComplete() {
        mgr.start(REMAINING_MS);

        lockFuture.complete(new OperationOutcome());
        genGuardOutcome();

        // generate failure outcome for ANOTHER actor - should be ignored
        OperationOutcome outcome = mgr.getParams().makeOutcome();
        outcome.setActor(OTHER_ACTOR);
        outcome.setResult(PolicyResult.FAILURE);
        outcome.setStart(Instant.now());
        mgr.getParams().callbackStarted(new OperationOutcome(outcome));

        outcome.setEnd(Instant.now());
        mgr.getParams().callbackCompleted(outcome);

        policyFuture.complete(genOpOutcome());
        runToCompletion();

        // should not include the other actor's outcome
        assertEquals(ControlLoopOperationManager2.State.GUARD_STARTED, mgr.getState());

        assertTrue(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.GUARD_PERMITTED, mgr.getState());

        assertTrue(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.OPERATION_STARTED, mgr.getState());

        assertTrue(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.OPERATION_SUCCESS, mgr.getState());

        assertFalse(mgr.nextStep());

        assertEquals(PolicyResult.SUCCESS, mgr.getOutcomes().peek().getResult());

        verify(mgrctx, times(4)).updated(mgr);
    }

    @Test
    public void testNextStep() {
        mgr.start(REMAINING_MS);

        // only do the lock and the guard
        lockFuture.complete(new OperationOutcome());
        genGuardOutcome();
        runToCompletion();

        assertEquals(ControlLoopOperationManager2.State.GUARD_STARTED, mgr.getState());

        assertTrue(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.GUARD_PERMITTED, mgr.getState());

        assertTrue(mgr.nextStep());
        assertTrue(mgr.nextStep());

        verify(mgrctx, times(2)).updated(mgr);
    }

    /**
     * Tests processOutcome() when the lock is denied.
     */
    @Test
    public void testProcessOutcomeLockDenied() {
        mgr.start(REMAINING_MS);

        // unavailable from the start => "denied"
        genLockFailure();

        runToCompletion();

        assertEquals(ControlLoopOperationManager2.State.LOCK_DENIED, mgr.getState());

        assertFalse(mgr.nextStep());
        verify(mgrctx).updated(mgr);

        verifyDb(1, PolicyResult.FAILURE_GUARD, "Operation denied by Lock");
    }

    /**
     * Tests processOutcome() when the lock is lost.
     */
    @Test
    public void testProcessOutcomeLockLost() {
        mgr.start(REMAINING_MS);

        // indicate lock success initially
        lockFuture.complete(new OperationOutcome());

        // do the guard
        genGuardOutcome();

        // now generate a lock failure => "lost"
        genLockFailure();

        runToCompletion();

        assertEquals(ControlLoopOperationManager2.State.GUARD_STARTED, mgr.getState());

        assertTrue(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.GUARD_PERMITTED, mgr.getState());

        assertTrue(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.LOCK_LOST, mgr.getState());

        assertFalse(mgr.nextStep());
        verify(mgrctx, times(3)).updated(mgr);

        verifyDb(1, PolicyResult.FAILURE, "Operation aborted by Lock");
    }

    /**
     * Tests processOutcome() when the guard is permitted.
     */
    @Test
    public void testProcessOutcomeGuardPermit() {
        mgr.start(REMAINING_MS);

        lockFuture.complete(new OperationOutcome());
        genGuardOutcome();

        runToCompletion();

        assertEquals(ControlLoopOperationManager2.State.GUARD_STARTED, mgr.getState());

        assertTrue(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.GUARD_PERMITTED, mgr.getState());

        assertTrue(mgr.nextStep());
        verify(mgrctx, times(2)).updated(mgr);

        verify(dataMgr, never()).store(any(), any(), any(), any());
    }

    /**
     * Tests processOutcome() when the guard is permitted.
     */
    @Test
    public void testProcessOutcomeGuardDenied() {
        mgr.start(REMAINING_MS);

        lockFuture.complete(new OperationOutcome());
        genGuardOutcome(false);

        runToCompletion();

        assertEquals(ControlLoopOperationManager2.State.GUARD_STARTED, mgr.getState());

        assertTrue(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.GUARD_DENIED, mgr.getState());

        assertFalse(mgr.nextStep());
        verify(mgrctx, times(2)).updated(mgr);

        verifyDb(1, PolicyResult.FAILURE_GUARD, "Operation denied by Guard");
    }

    /**
     * Tests processOutcome() when the operation is a success.
     */
    @Test
    public void testProcessOutcomeOperSuccess() {
        mgr.start(REMAINING_MS);

        lockFuture.complete(new OperationOutcome());
        genGuardOutcome();
        genOpOutcome();

        runToCompletion();

        assertEquals(ControlLoopOperationManager2.State.GUARD_STARTED, mgr.getState());

        assertTrue(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.GUARD_PERMITTED, mgr.getState());

        assertTrue(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.OPERATION_STARTED, mgr.getState());

        assertTrue(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.OPERATION_SUCCESS, mgr.getState());

        assertFalse(mgr.nextStep());
        verify(mgrctx, times(4)).updated(mgr);

        verifyDb(2, PolicyResult.SUCCESS, null);
    }

    /**
     * Tests processOutcome() when the operation is a failure.
     */
    @Test
    public void testProcessOutcomeOperFailure() {
        mgr.start(REMAINING_MS);

        lockFuture.complete(new OperationOutcome());
        genGuardOutcome();
        genOpOutcome(false);

        runToCompletion();

        assertEquals(ControlLoopOperationManager2.State.GUARD_STARTED, mgr.getState());

        assertTrue(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.GUARD_PERMITTED, mgr.getState());

        assertTrue(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.OPERATION_STARTED, mgr.getState());

        assertTrue(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.OPERATION_FAILURE, mgr.getState());
        verifyDb(2, PolicyResult.FAILURE, null);

        assertThat(mgr.toString()).contains("attempts=1");

        // next failure
        genOpOutcome(false);
        runToCompletion();

        assertTrue(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.OPERATION_STARTED, mgr.getState());

        assertTrue(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.OPERATION_FAILURE, mgr.getState());
        verifyDb(4, PolicyResult.FAILURE, null);

        assertThat(mgr.toString()).contains("attempts=2");

        // and finally a success
        genOpOutcome();

        assertTrue(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.OPERATION_STARTED, mgr.getState());

        assertTrue(mgr.nextStep());
        assertEquals(ControlLoopOperationManager2.State.OPERATION_SUCCESS, mgr.getState());
        verifyDb(6, PolicyResult.SUCCESS, null);

        assertThat(mgr.toString()).contains("attempts=3");

        assertFalse(mgr.nextStep());
        verify(mgrctx, times(8)).updated(mgr);
    }

    @Test
    public void testGetOperationHistory() {
        // no history yet
        assertNull(mgr.getOperationHistory());

        runCyle();
        assertThat(mgr.getOperationHistory()).contains("actor=my-actor").contains("operation=my-operation")
                        .contains("outcome=Success");
    }

    @Test
    public void testGetHistory() {
        // no history yet
        assertEquals(0, mgr.getHistory().size());

        runCyle();
        assertEquals(1, mgr.getHistory().size());
    }

    @Test
    public void testDetmTargetVm() {
        target.setType(TargetType.VM);
        assertNull(mgr.detmTarget());
        assertEquals(MY_TARGET, mgr.getTargetEntity());

        target.setType(TargetType.VNF);
        assertNull(mgr.detmTarget());
        assertEquals(MY_TARGET, mgr.getTargetEntity());

        target.setType(TargetType.VFMODULE);
        assertNull(mgr.detmTarget());
        assertEquals(MY_TARGET, mgr.getTargetEntity());

        // unsupported type
        target.setType(TargetType.VFC);
        assertThatIllegalArgumentException().isThrownBy(() -> mgr.detmTarget())
                        .withMessage("The target type is not supported");

        // null type
        target.setType(null);
        assertThatIllegalArgumentException().isThrownBy(() -> mgr.detmTarget()).withMessage("The target type is null");

        // null target
        policy.setTarget(null);
        assertThatIllegalArgumentException().isThrownBy(() -> mgr.detmTarget()).withMessage("The target is null");
    }

    @Test
    public void testDetmPnfTarget() {
        setTargetPnf();
        assertNull(mgr.detmTarget());
        assertEquals(MY_TARGET, mgr.getTargetEntity());

        // missing enrichment data
        event.getAai().clear();
        assertThatIllegalArgumentException().isThrownBy(() -> mgr.detmTarget())
                        .withMessage("AAI section is missing " + ControlLoopOperationManager2.PNF_NAME);

        // wrong target
        event.setTarget(MISMATCH);
        assertThatIllegalArgumentException().isThrownBy(() -> mgr.detmTarget())
                        .withMessage("Target does not match target type");
    }

    @Test
    public void testDetmVfModuleTarget() {
        // vserver
        event.setTarget(ControlLoopOperationManager2.VSERVER_VSERVER_NAME);
        event.getAai().clear();
        event.getAai().putAll(Map.of(ControlLoopOperationManager2.VSERVER_VSERVER_NAME, MY_TARGET));
        assertNull(mgr.detmTarget());
        assertEquals(MY_TARGET, mgr.getTargetEntity());

        // vnf-id
        event.setTarget(ControlLoopOperationManager2.GENERIC_VNF_VNF_ID);
        event.getAai().clear();
        event.getAai().putAll(Map.of(ControlLoopOperationManager2.GENERIC_VNF_VNF_ID, MY_TARGET));
        assertNull(mgr.detmTarget());
        assertEquals(MY_TARGET, mgr.getTargetEntity());

        // wrong type
        event.setTarget(MISMATCH);
        assertThatIllegalArgumentException().isThrownBy(() -> mgr.detmTarget())
                        .withMessage("Target does not match target type");

        // missing enrichment data
        event.setTarget(ControlLoopOperationManager2.VSERVER_VSERVER_NAME);
        event.getAai().clear();
        assertThatIllegalArgumentException().isThrownBy(() -> mgr.detmTarget())
                        .withMessage("Enrichment data is missing " + ControlLoopOperationManager2.VSERVER_VSERVER_NAME);

        // null target
        event.setTarget(null);
        assertThatIllegalArgumentException().isThrownBy(() -> mgr.detmTarget()).withMessage("Target is null");
    }

    @Test
    public void testDetmVnfName() {
        setTargetVnfName();
        assertNull(mgr.detmTarget());
        assertEquals(MY_TARGET, mgr.getTargetEntity());

        // force it to be gotten from the CQ data
        event.getAai().clear();
        assertNull(mgr.detmTarget());
        assertEquals(MY_VNF_ID, mgr.getTargetEntity());
    }

    @Test
    public void testExtractVnfFromCq() {
        // force it to be gotten from the CQ data
        setTargetVnfName();
        event.getAai().clear();

        // missing vnf id in CQ data
        when(vnf.getVnfId()).thenReturn(null);
        assertThatIllegalArgumentException().isThrownBy(() -> mgr.detmTarget()).withMessage("No vnf-id found");

        // missing default vnf in CQ data
        when(cqdata.getDefaultGenericVnf()).thenReturn(null);
        assertThatIllegalArgumentException().isThrownBy(() -> mgr.detmTarget()).withMessage("No vnf-id found");
    }

    @Test
    public void testGetState_testGetActor_testGetOperation() {
        assertEquals(ControlLoopOperationManager2.State.ACTIVE, mgr.getState());
        assertEquals(POLICY_ACTOR, mgr.getActor());
        assertEquals(POLICY_OPERATION, mgr.getOperation());
    }

    @Test
    public void testToString() {
        assertThat(mgr.toString()).contains("state").contains("requestId").contains("policyId").contains("attempts");
    }

    /**
     * Runs a cycle, from start to completion.
     */
    private void runCyle() {
        mgr.start(REMAINING_MS);

        lockFuture.complete(new OperationOutcome());
        genGuardOutcome();
        genOpOutcome();

        runToCompletion();

        // guard start
        assertTrue(mgr.nextStep());

        // guard permit
        assertTrue(mgr.nextStep());

        // operation start
        assertTrue(mgr.nextStep());

        // operation success
        assertFalse(mgr.nextStep());
    }

    /**
     * Runs everything until the executor queue is empty.
     */
    private void runToCompletion() {
        assertTrue(executor.runAll(MAX_RUN));
    }

    /**
     * Generates a failure outcome for the lock, and invokes the callbacks.
     *
     * @return the generated outcome
     */
    private OperationOutcome genLockFailure() {
        OperationOutcome outcome = new OperationOutcome();
        outcome.setActor(ControlLoopOperationManager2.LOCK_ACTOR);
        outcome.setOperation(ControlLoopOperationManager2.LOCK_OPERATION);
        outcome.setResult(PolicyResult.FAILURE);
        outcome.setStart(Instant.now());
        outcome.setEnd(Instant.now());
        outcome.setFinalOutcome(true);

        verify(mgrctx).requestLock(eq(MY_TARGET), lockCallback.capture());
        lockCallback.getValue().accept(outcome);

        lockFuture.complete(outcome);

        return outcome;
    }

    /**
     * Generates an outcome for the guard, and invokes the callbacks.
     *
     * @return the generated outcome
     */
    private OperationOutcome genGuardOutcome() {
        return genGuardOutcome(true);
    }

    /**
     * Generates an outcome for the guard, and invokes the callbacks.
     *
     * @param permit {@code true} if the guard should be permitted, {@code false} if
     *        denied
     * @return the generated outcome
     */
    private OperationOutcome genGuardOutcome(boolean permit) {
        OperationOutcome outcome = mgr.getParams().makeOutcome();
        outcome.setActor(GuardActorServiceProvider.NAME);
        outcome.setOperation(DecisionOperation.NAME);
        outcome.setStart(Instant.now());
        mgr.getParams().callbackStarted(new OperationOutcome(outcome));

        if (!permit) {
            outcome.setResult(PolicyResult.FAILURE);
        }

        outcome.setEnd(Instant.now());
        mgr.getParams().callbackCompleted(outcome);

        return outcome;
    }

    /**
     * Generates an outcome for the operation, itself, and invokes the callbacks.
     *
     * @return the generated outcome
     */
    private OperationOutcome genOpOutcome() {
        return genOpOutcome(true);
    }

    /**
     * Generates an outcome for the operation, itself, and invokes the callbacks.
     *
     * @param success {@code true} if the outcome should be a success, {@code false} if a
     *        failure
     * @return the generated outcome
     */
    private OperationOutcome genOpOutcome(boolean success) {
        OperationOutcome outcome = mgr.getParams().makeOutcome();
        outcome.setStart(Instant.now());
        mgr.getParams().callbackStarted(new OperationOutcome(outcome));

        if (success) {
            outcome.setFinalOutcome(true);
        } else {
            outcome.setResult(PolicyResult.FAILURE);
        }

        outcome.setEnd(Instant.now());
        mgr.getParams().callbackCompleted(outcome);

        return outcome;
    }

    /**
     * Configures the data for a PNF target.
     */
    private void setTargetPnf() {
        event.setTarget(ControlLoopOperationManager2.PNF_NAME);
        event.getAai().clear();
        event.getAai().putAll(Map.of(ControlLoopOperationManager2.PNF_NAME, MY_TARGET));

        target.setType(TargetType.PNF);
    }

    /**
     * Configures the data for a VNF-NAME target.
     */
    private void setTargetVnfName() {
        event.setTarget(ControlLoopOperationManager2.GENERIC_VNF_VNF_NAME);
        event.getAai().clear();
        event.getAai().putAll(Map.of(ControlLoopOperationManager2.GENERIC_VNF_VNF_ID, MY_TARGET));

        target.setType(TargetType.VNF);
    }

    private void checkResp(OperationOutcome outcome, String expectedPayload) {
        ControlLoopResponse resp = mgr.makeControlLoopResponse(outcome);
        assertNotNull(resp);
        assertEquals(REQ_ID, resp.getRequestId());
        assertEquals(expectedPayload, resp.getPayload());
    }

    private void verifyDb(int nrecords, PolicyResult expectedResult, String expectedMsg) {
        ArgumentCaptor<String> entityCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ControlLoopOperation> opCaptor = ArgumentCaptor.forClass(ControlLoopOperation.class);
        verify(dataMgr, times(nrecords)).store(any(), any(), entityCaptor.capture(), opCaptor.capture());

        assertEquals(MY_TARGET, entityCaptor.getValue());

        ControlLoopOperation oper = opCaptor.getValue();

        assertEquals(expectedResult.toString(), oper.getOutcome());
        assertEquals(expectedMsg, oper.getMessage());
    }
}
