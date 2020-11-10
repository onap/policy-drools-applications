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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
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
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.ControlLoopTargetType;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.actorserviceprovider.ActorService;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.controlloop.ControlLoopEventContext;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.controlloop.eventmanager.ControlLoopEventManager2.NewEventStatus;
import org.onap.policy.controlloop.eventmanager.ControlLoopOperationManager2.State;
import org.onap.policy.controlloop.ophistory.OperationHistoryDataManager;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.controlloop.policy.Target;
import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.drools.core.lock.LockCallback;
import org.onap.policy.drools.core.lock.LockImpl;
import org.onap.policy.drools.core.lock.LockState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

public class ControlLoopEventManager2Test {
    private static final UUID REQ_ID = UUID.randomUUID();
    private static final String CL_NAME = "my-closed-loop-name";
    private static final String POLICY_NAME = "my-policy-name";
    private static final String POLICY_SCOPE = "my-scope";
    private static final String POLICY_VERSION = "1.2.3";
    private static final String MY_TARGET = "my-target";
    private static final String LOCK1 = "my-lock-A";
    private static final String LOCK2 = "my-lock-B";
    private static final Coder yamlCoder = new StandardYamlCoder();

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

    private long preCreateTimeMs;
    private List<LockImpl> locks;
    private Target target;
    private ToscaPolicy tosca;
    private ControlLoopParams params;
    private VirtualControlLoopEvent event;
    private int updateCount;
    private ControlLoopEventManager2 mgr;

    /**
     * Sets up.
     */
    @Before
    public void setUp() throws ControlLoopException, CoderException {
        MockitoAnnotations.initMocks(this);

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

        loadPolicy("eventManager/event-mgr-simple.yaml");

        locks = new ArrayList<>();

        updateCount = 0;

        preCreateTimeMs = System.currentTimeMillis();

        mgr = new MyManagerWithOper(params, event, workMem);
    }

    @Test
    public void testConstructor() {
        assertEquals(POLICY_NAME, mgr.getPolicyName());

        Map<String, String> orig = event.getAai();

        event.setAai(addAai(orig, ControlLoopEventManager2.VSERVER_IS_CLOSED_LOOP_DISABLED, "true"));
        assertThatThrownBy(() -> new ControlLoopEventManager2(params, event, workMem))
                        .hasMessage("is-closed-loop-disabled is set to true on VServer or VNF");

        event.setAai(addAai(orig, ControlLoopEventManager2.VSERVER_PROV_STATUS, "inactive"));
        assertThatThrownBy(() -> new ControlLoopEventManager2(params, event, workMem))
                        .hasMessage("prov-status is not ACTIVE on VServer or VNF");

        // test with both prov-status flags, with mixed case
        event.setAai(addAai(orig, ControlLoopEventManager2.VSERVER_PROV_STATUS, "ACTIVE"));
        assertThatCode(() -> new ControlLoopEventManager2(params, event, workMem)).doesNotThrowAnyException();

        event.setAai(addAai(orig, ControlLoopEventManager2.VSERVER_PROV_STATUS, "active"));
        assertThatCode(() -> new ControlLoopEventManager2(params, event, workMem)).doesNotThrowAnyException();

        event.setAai(addAai(orig, ControlLoopEventManager2.GENERIC_VNF_PROV_STATUS, "ACTIVE"));
        assertThatCode(() -> new ControlLoopEventManager2(params, event, workMem)).doesNotThrowAnyException();

        event.setAai(addAai(orig, ControlLoopEventManager2.GENERIC_VNF_PROV_STATUS, "active"));
        assertThatCode(() -> new ControlLoopEventManager2(params, event, workMem)).doesNotThrowAnyException();

        // valid
        event.setAai(orig);
        assertThatCode(() -> mgr.checkEventSyntax(event)).doesNotThrowAnyException();

        // invalid
        event.setTarget("unknown-target");
        assertThatThrownBy(() -> new ControlLoopEventManager2(params, event, workMem));
    }

    /**
     * Runs through a policy that has several operations.
     */
    @Test
    public void testMultiOperation() throws Exception {

        loadPolicy("eventManager/event-mgr-multi.yaml");

        mgr = new MyManagerWithOper(params, event, workMem);
        mgr.start();

        for (ControlLoopOperationManager2 oper : Arrays.asList(oper1, oper2, oper3)) {
            assertTrue(mgr.isActive());
            nextStep(oper, true, PolicyResult.SUCCESS);
            runRule();

            assertTrue(mgr.isActive());
            nextStep(oper, false, PolicyResult.SUCCESS);
            runRule();
        }

        assertFalse(mgr.isActive());
    }

    @Test
    public void testStart() throws Exception {
        // start it
        mgr.start();

        // cannot re-start
        assertThatCode(() -> mgr.start()).isInstanceOf(IllegalStateException.class)
                        .hasMessage("manager already started");
    }

    /**
     * Tests start() error cases.
     */
    @Test
    public void testStartErrors() throws Exception {
        // wrong jvm
        ControlLoopEventManager2 mgr2 = new ControlLoopEventManager2(params, event, workMem);
        ControlLoopEventManager2 mgr3 = Serializer.roundTrip(mgr2);
        assertThatCode(() -> mgr3.start()).isInstanceOf(IllegalStateException.class)
                        .hasMessage("manager is no longer active");

        // no fact handle
        when(workMem.getFactHandle(any())).thenReturn(null);
        assertThatCode(() -> mgr.start()).isInstanceOf(IllegalStateException.class)
                        .hasMessage("manager is not in working memory");
    }

    @Test
    public void testNextStep_testStartOperationSuccess() throws ControlLoopException {
        runOperation(PolicyResult.SUCCESS);

        VirtualControlLoopNotification notif = mgr.getNotification();
        assertEquals(ControlLoopNotificationType.FINAL_SUCCESS, notif.getNotification());
        assertNull(notif.getMessage());

        assertThatCode(() -> mgr.nextStep()).doesNotThrowAnyException();
    }

    /**
     * Tests nextStep() when the next step is invalid, which should cause an exception to
     * be thrown by the processor.
     */
    @Test
    public void testNextStepMissing() throws Exception {
        mgr.start();

        when(oper1.nextStep()).thenThrow(new IllegalArgumentException("expected exception"));

        mgr.nextStep();

        assertFalse(mgr.isActive());

        VirtualControlLoopNotification notif = mgr.getNotification();
        assertEquals(ControlLoopNotificationType.FINAL_FAILURE, notif.getNotification());
        assertEquals("Policy processing aborted due to policy error", notif.getMessage());
        assertTrue(notif.getHistory().isEmpty());
    }

    /**
     * Tests startOperation() with FINAL_FAILURE_EXCEPTION.
     */
    @Test
    public void testStartOperationException() throws ControlLoopException {
        runOperation(PolicyResult.FAILURE_EXCEPTION);

        VirtualControlLoopNotification notif = mgr.getNotification();
        assertEquals(ControlLoopNotificationType.FINAL_FAILURE, notif.getNotification());
        assertEquals("Exception in processing closed loop", notif.getMessage());
    }

    /**
     * Tests startOperation() with FINAL_FAILURE.
     */
    @Test
    public void testStartOperationFailure() throws ControlLoopException {
        runOperation(PolicyResult.FAILURE);

        VirtualControlLoopNotification notif = mgr.getNotification();
        assertEquals(ControlLoopNotificationType.FINAL_FAILURE, notif.getNotification());
        assertNull(notif.getMessage());
    }

    /**
     * Tests startOperation() with FINAL_OPENLOOP.
     */
    @Test
    public void testStartOperationOpenLoop() throws ControlLoopException {
        runOperation(PolicyResult.FAILURE_GUARD);

        VirtualControlLoopNotification notif = mgr.getNotification();
        assertEquals(ControlLoopNotificationType.FINAL_OPENLOOP, notif.getNotification());
        assertNull(notif.getMessage());
    }

    @Test
    public void testIsActive() throws Exception {
        mgr = new ControlLoopEventManager2(params, event, workMem);
        assertTrue(mgr.isActive());

        ControlLoopEventManager2 mgr2 = Serializer.roundTrip(mgr);
        assertFalse(mgr2.isActive());
    }

    @Test
    public void testUpdated() throws ControlLoopException {
        mgr.start();

        // not the active operation - should be ignored
        mgr.updated(oper3);
        verify(workMem, never()).update(any(), any());

        VirtualControlLoopNotification notif;

        // check notification data
        when(oper1.getState()).thenReturn(State.LOCK_DENIED);
        mgr.updated(oper1);
        notif = mgr.getNotification();
        assertNotNull(notif.getHistory());

        /*
         * try the various cases
         */
        when(oper1.getState()).thenReturn(State.LOCK_DENIED);
        mgr.updated(oper1);
        verifyNotification(ControlLoopNotificationType.REJECTED, "The target my-target is already locked");

        when(oper1.getState()).thenReturn(State.LOCK_LOST);
        mgr.updated(oper1);
        verifyNotification(ControlLoopNotificationType.OPERATION_FAILURE, "The target my-target is no longer locked");

        when(oper1.getState()).thenReturn(State.GUARD_STARTED);
        mgr.updated(oper1);
        verifyNotification(ControlLoopNotificationType.OPERATION, "Sending guard query for First OperationA");

        when(oper1.getState()).thenReturn(State.GUARD_PERMITTED);
        mgr.updated(oper1);
        verifyNotification(ControlLoopNotificationType.OPERATION, "Guard result for First OperationA is Permit");

        when(oper1.getState()).thenReturn(State.GUARD_DENIED);
        mgr.updated(oper1);
        verifyNotification(ControlLoopNotificationType.OPERATION, "Guard result for First OperationA is Deny");

        when(oper1.getState()).thenReturn(State.OPERATION_STARTED);
        mgr.updated(oper1);
        verifyNotification(ControlLoopNotificationType.OPERATION, "message-A");

        when(oper1.getState()).thenReturn(State.OPERATION_SUCCESS);
        mgr.updated(oper1);
        verifyNotification(ControlLoopNotificationType.OPERATION_SUCCESS, "history-A");

        when(oper1.getState()).thenReturn(State.OPERATION_FAILURE);
        mgr.updated(oper1);
        verifyNotification(ControlLoopNotificationType.OPERATION_FAILURE, "history-A");

        // should still be active
        assertTrue(mgr.isActive());

        /*
         * control loop time
         */
        when(oper1.getState()).thenReturn(State.CONTROL_LOOP_TIMEOUT);
        mgr.updated(oper1);
        verifyNotification(ControlLoopNotificationType.FINAL_FAILURE, "Control Loop timed out");

        // should now be done
        assertFalse(mgr.isActive());
    }

    @Test
    public void testDestroy() {
        mgr.requestLock(LOCK1, callback1);
        mgr.requestLock(LOCK2, callback2);
        mgr.requestLock(LOCK1, callback3);

        mgr.destroy();

        freeLocks();

        for (LockImpl lock : locks) {
            assertTrue(lock.isUnavailable());
        }
    }

    /**
     * Tests destroy() once it has been started.
     */
    @Test
    public void testDestroyStarted() throws ControlLoopException {
        mgr.start();

        mgr.requestLock(LOCK1, callback1);
        mgr.requestLock(LOCK2, callback2);
        mgr.requestLock(LOCK1, callback3);

        mgr.destroy();

        freeLocks();

        // should have canceled the operation
        verify(oper1).cancel();

        for (LockImpl lock : locks) {
            assertTrue(lock.isUnavailable());
        }
    }

    @Test
    public void testMakeNotification() throws ControlLoopException {
        // before started
        assertNotNull(mgr.makeNotification());

        mgr.start();

        nextStep(oper1, true, PolicyResult.SUCCESS);
        runRule();

        // check notification while running
        VirtualControlLoopNotification notif = mgr.getNotification();
        assertEquals("history-A", notif.getMessage());

        List<ControlLoopOperation> history = notif.getHistory();
        assertNotNull(history);

        nextStep(oper1, false, PolicyResult.SUCCESS);
        runRule();

        assertFalse(mgr.isActive());

        // check notification when complete
        notif = mgr.getNotification();
        assertNull(notif.getMessage());
        assertEquals(history, notif.getHistory());
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
    public void testDetmControlLoopTimeoutMs() throws Exception {
        verifyTimeout(1200 * 1000L);
    }

    private void verifyTimeout(long timeMs) {
        long end = mgr.getEndTimeMs();
        assertTrue(end >= preCreateTimeMs + timeMs);
        assertTrue(end < preCreateTimeMs + timeMs + 5000);
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
        assertThatThrownBy(() -> new ControlLoopEventManager2(params, event, workMem));

        event.setAai(addAai(orig, ControlLoopEventManager2.GENERIC_VNF_IS_CLOSED_LOOP_DISABLED, "true"));
        assertThatThrownBy(() -> new ControlLoopEventManager2(params, event, workMem));

        event.setAai(addAai(orig, ControlLoopEventManager2.PNF_IS_IN_MAINT, "true"));
        assertThatThrownBy(() -> new ControlLoopEventManager2(params, event, workMem));
    }

    private Map<String, String> addAai(Map<String, String> original, String key, String value) {
        Map<String, String> map = new TreeMap<>(original);
        map.put(key, value);
        return map;
    }

    @Test
    public void testIsProvStatusInactive() {
        Map<String, String> orig = event.getAai();

        event.setAai(addAai(orig, ControlLoopEventManager2.VSERVER_PROV_STATUS, "ACTIVE"));
        assertThatCode(() -> new ControlLoopEventManager2(params, event, workMem)).doesNotThrowAnyException();

        event.setAai(addAai(orig, ControlLoopEventManager2.VSERVER_PROV_STATUS, "inactive"));
        assertThatThrownBy(() -> new ControlLoopEventManager2(params, event, workMem));

        event.setAai(addAai(orig, ControlLoopEventManager2.GENERIC_VNF_PROV_STATUS, "ACTIVE"));
        assertThatCode(() -> new ControlLoopEventManager2(params, event, workMem)).doesNotThrowAnyException();

        event.setAai(addAai(orig, ControlLoopEventManager2.GENERIC_VNF_PROV_STATUS, "inactive"));
        assertThatThrownBy(() -> new ControlLoopEventManager2(params, event, workMem));
    }

    @Test
    public void testIsAaiTrue() {
        Map<String, String> orig = event.getAai();

        for (String value : Arrays.asList("yes", "y", "true", "t", "yEs", "trUe")) {
            event.setAai(addAai(orig, ControlLoopEventManager2.VSERVER_IS_CLOSED_LOOP_DISABLED, value));
            assertThatThrownBy(() -> new ControlLoopEventManager2(params, event, workMem));
        }

        event.setAai(addAai(orig, ControlLoopEventManager2.VSERVER_IS_CLOSED_LOOP_DISABLED, "false"));
        assertThatCode(() -> new ControlLoopEventManager2(params, event, workMem)).doesNotThrowAnyException();

        event.setAai(addAai(orig, ControlLoopEventManager2.VSERVER_IS_CLOSED_LOOP_DISABLED, "no"));
        assertThatCode(() -> new ControlLoopEventManager2(params, event, workMem)).doesNotThrowAnyException();
    }

    @Test
    public void testRequestLock() {
        final CompletableFuture<OperationOutcome> future1 = mgr.requestLock(LOCK1, callback1);
        final CompletableFuture<OperationOutcome> future2 = mgr.requestLock(LOCK2, callback2);
        assertSame(future1, mgr.requestLock(LOCK1, callback3));

        assertEquals(2, locks.size());

        assertTrue(future1.isDone());
        assertTrue(future2.isDone());

        verify(callback1, never()).accept(any());
        verify(callback2, never()).accept(any());
        verify(callback3, never()).accept(any());

        // indicate that the first lock failed
        locks.get(0).notifyUnavailable();

        verify(callback1).accept(any());
        verify(callback2, never()).accept(any());
        verify(callback3).accept(any());
    }

    @Test
    public void testMakeOperationManager() throws ControlLoopException {
        // use a manager that creates real operation managers
        mgr = new MyManager(params, event, workMem);

        assertThatCode(() -> mgr.start()).doesNotThrowAnyException();
    }

    @Test
    public void testGetBlockingExecutor() throws Exception {
        mgr = new ControlLoopEventManager2(params, event, workMem);
        assertThatCode(() -> mgr.getBlockingExecutor()).doesNotThrowAnyException();
    }

    @Test
    public void testToString() {
        assertNotNull(mgr.toString());
    }


    private void nextStep(ControlLoopOperationManager2 oper, boolean moreSteps, PolicyResult result) {
        when(oper.nextStep()).thenReturn(moreSteps);
        when(oper.getOperationResult()).thenReturn(result);

        if (result == PolicyResult.SUCCESS) {
            when(oper.getState()).thenReturn(State.OPERATION_SUCCESS);
        } else {
            when(oper.getState()).thenReturn(State.OPERATION_FAILURE);
        }

        mgr.updated(oper);

        updateCount++;

        verify(workMem, times(updateCount)).update(factHandle, mgr);
    }

    private void runRule() {
        assertTrue(mgr.isActive());
        mgr.nextStep();
    }

    private void runOperation(PolicyResult finalResult) throws ControlLoopException {
        mgr.start();
        verify(oper1).start(anyLong());

        assertTrue(mgr.isActive());

        nextStep(oper1, true, PolicyResult.SUCCESS);
        runRule();

        nextStep(oper1, false, finalResult);
        runRule();

        assertFalse(mgr.isActive());

        // should have no effect, because it's done
        mgr.updated(oper1);
        verify(workMem, times(updateCount)).update(any(), any());
    }

    private void verifyNotification(ControlLoopNotificationType expectedType, String expectedMsg) {
        VirtualControlLoopNotification notif = mgr.getNotification();
        assertEquals(expectedType, notif.getNotification());
        assertEquals(expectedMsg, notif.getMessage());
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

    private void freeLocks() {
        ArgumentCaptor<Runnable> runCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runCaptor.capture());

        runCaptor.getValue().run();
    }


    private class MyManager extends ControlLoopEventManager2 {
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


    private class MyManagerWithOper extends MyManager {
        private static final long serialVersionUID = 1L;

        public MyManagerWithOper(ControlLoopParams params, VirtualControlLoopEvent event, WorkingMemory workMem)
                        throws ControlLoopException {

            super(params, event, workMem);
        }

        @Override
        protected ControlLoopOperationManager2 makeOperationManager(ControlLoopEventContext ctx, Policy policy) {
            switch (policy.getActor()) {
                case "First":
                    return oper1;
                case "Second":
                    return oper2;
                case "Third":
                    return oper3;
                default:
                    throw new IllegalArgumentException("unknown policy actor " + policy.getActor());
            }
        }
    }
}
