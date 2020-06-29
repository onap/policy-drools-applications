/*-
 * ============LICENSE_START=======================================================
 * m2/base
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

package org.onap.policy.m2.base;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.LinkedList;
import java.util.UUID;
import org.drools.core.WorkingMemory;
import org.drools.core.impl.StatefulKnowledgeSessionImpl;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.runtime.rule.FactHandle;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopNotification;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.policy.ControlLoop;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.controlloop.policy.FinalResult;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.Target;

public class TransactionTest {

    private static final String CL_NAME = "clTest";

    private static WorkingMemory mockWorkingMemory;

    /**
     * Mocks the working memory.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        mockWorkingMemory = mock(WorkingMemory.class);
        FactHandle factHandle = mock(FactHandle.class);
        when(mockWorkingMemory.getFactHandle(any())).thenReturn(factHandle);
    }

    @Test
    public void testValidControlLoopEvent() {
        VirtualControlLoopEvent event =
            createControlLoopEvent(UUID.randomUUID(), CL_NAME, null, "VM", "vserver.vserver-name");
        Transaction transaction = new Transaction(null, "clvusptest", event.getRequestId(), createControlLoop());
        assertTrue(transaction.isControlLoopEventValid(event));
    }

    @Test
    public void testNoRequestIdControlLoopEvent() {
        VirtualControlLoopEvent event =
            createControlLoopEvent(null, CL_NAME, null, "VM", "vserver.vserver-name");
        Transaction transaction = new Transaction(null, "clvusptest", event.getRequestId(), createControlLoop());
        assertFalse(transaction.isControlLoopEventValid(event));
        assertEquals("No requestID", transaction.getNotificationMessage());
    }

    @Test
    public void testNoTargetTypeControlLoopEvent() {
        VirtualControlLoopEvent event =
            createControlLoopEvent(UUID.randomUUID(), CL_NAME, null, null, "vserver.vserver-name");
        Transaction transaction = new Transaction(null, "clvusptest", event.getRequestId(), createControlLoop());
        assertFalse(transaction.isControlLoopEventValid(event));
        assertEquals("No targetType", transaction.getNotificationMessage());
    }

    @Test
    public void testNoTargetControlLoopEvent() {
        VirtualControlLoopEvent event =
            createControlLoopEvent(UUID.randomUUID(), CL_NAME, null, "VM", null);
        Transaction transaction = new Transaction(null, "clvusptest", event.getRequestId(), createControlLoop());
        assertFalse(transaction.isControlLoopEventValid(event));
        assertEquals("No target field", transaction.getNotificationMessage());
    }

    @Test
    public void testEmptyTargetControlLoopEvent() {
        assertInvalidTarget("");
    }

    @Test
    public void testGetClosedLoopControlName() {
        Transaction transaction = new Transaction(mockWorkingMemory, CL_NAME, UUID.randomUUID(), createControlLoop());
        assertEquals(CL_NAME, transaction.getClosedLoopControlName());
    }

    @Test
    public void testGetRequestId() {
        UUID requestId = UUID.randomUUID();
        Transaction transaction = new Transaction(mockWorkingMemory, CL_NAME, requestId, createControlLoop());
        assertEquals(requestId, transaction.getRequestId());
    }

    @Test
    public void testGetWorkingMemory() {
        // Create mock working session
        StatefulKnowledgeSessionImpl mockWorkingMemory = mock(StatefulKnowledgeSessionImpl.class);
        Transaction transaction = new Transaction(mockWorkingMemory, "clvusptest",
            UUID.randomUUID(), createControlLoop());
        assertEquals(mockWorkingMemory, transaction.getWorkingMemory());
    }

    @Test
    public void testGetStateComplete() {
        Transaction transaction = new Transaction(mockWorkingMemory, CL_NAME, UUID.randomUUID(), createControlLoop());
        assertEquals("COMPLETE", transaction.getState());
    }

    @Test
    public void testGetFinalResult() {
        Transaction transaction = new Transaction(mockWorkingMemory, CL_NAME, UUID.randomUUID(), createControlLoop());
        assertEquals(null, transaction.getFinalResult());
    }

    @Test
    public void testFinalResultFailure() {
        Transaction transaction = new Transaction(mockWorkingMemory, CL_NAME, UUID.randomUUID(), createControlLoop());
        assertFalse(transaction.finalResultFailure());
        transaction.clTimeout();
        assertTrue(transaction.finalResultFailure());
    }

    @Test
    public void testGetTimeout() {
        Transaction transaction = new Transaction(mockWorkingMemory, CL_NAME, UUID.randomUUID(), createControlLoop());
        assertEquals("15s", transaction.getTimeout());
    }

    @Test
    public void testGetOperationTimeout() {
        Transaction transaction = new Transaction(null, "clvusptest", UUID.randomUUID(), createControlLoop());
        VirtualControlLoopEvent onset =
            createControlLoopEvent(UUID.randomUUID(), CL_NAME, null, "VM", "vserver.vserver-name");
        transaction.setControlLoopEvent(onset);
        assertEquals("10s", transaction.getOperationTimeout());
    }

    @Test
    public void testGetCurrentPolicy() {
        ControlLoopPolicy controlLoopPolicy = createControlLoop();
        Transaction transaction = new Transaction(mockWorkingMemory, CL_NAME, UUID.randomUUID(), controlLoopPolicy);
        VirtualControlLoopEvent onset =
                        createControlLoopEvent(UUID.randomUUID(), CL_NAME, null, "VM", "vserver.vserver-name");
        transaction.setControlLoopEvent(onset);
        assertEquals(controlLoopPolicy.getPolicies().get(0), transaction.getCurrentPolicy());
    }

    @Test
    public void testGetOperation() {
        Transaction transaction = new Transaction(mockWorkingMemory, CL_NAME, UUID.randomUUID(), createControlLoop());
        assertEquals(null, transaction.getCurrentOperation());
    }

    @Test
    public void testGetNotification() {
        Transaction transaction = new Transaction(null, "clvusptest", UUID.randomUUID(), createControlLoop());
        VirtualControlLoopEvent onset =
            createControlLoopEvent(UUID.randomUUID(), CL_NAME, null, "VM", "vserver.vserver-name");
        transaction.setControlLoopEvent(onset);
        ControlLoopNotification notification = transaction.getNotification(onset);
        assertEquals(onset.getClosedLoopControlName(), notification.getClosedLoopControlName());
        assertEquals(onset.getRequestId(), notification.getRequestId());
        assertEquals(onset.getTarget(), notification.getTarget());

        notification = transaction.getNotification(null);
        assertEquals(onset.getClosedLoopControlName(), notification.getClosedLoopControlName());
        assertEquals(onset.getRequestId(), notification.getRequestId());
        assertEquals(onset.getTarget(), notification.getTarget());
    }

    @Test
    public void testSetControlLoopEventOnsetNotNull() {
        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent event = createControlLoopEvent(requestId, CL_NAME, Instant.now(), "VM", "vserver-name");
        Transaction transaction = new Transaction(mockWorkingMemory, CL_NAME, requestId, createControlLoop());
        assertEquals("COMPLETE", transaction.getState());
        transaction.setControlLoopEvent(event);
        assertEquals("TEST.PENDING", transaction.getState());
        transaction.setControlLoopEvent(event);
    }

    @Test
    public void testSetOpenLoopEvent() {
        ControlLoop controlLoop = new ControlLoop();
        controlLoop.setControlLoopName(CL_NAME);
        controlLoop.setTrigger_policy(FinalResult.FINAL_OPENLOOP.toString());

        ControlLoopPolicy controlLoopPolicy = new ControlLoopPolicy();
        controlLoopPolicy.setControlLoop(controlLoop);

        Transaction transaction = new Transaction(mockWorkingMemory, CL_NAME, UUID.randomUUID(), controlLoopPolicy);
        VirtualControlLoopEvent onset = createControlLoopEvent(UUID.randomUUID(), CL_NAME, null, "VM", "vserver-name");
        transaction.setControlLoopEvent(onset);

        assertEquals("COMPLETE", transaction.getState());
    }

    @Test
    public void testIncomingMessage() {
        Transaction transaction = new Transaction(mockWorkingMemory, CL_NAME, UUID.randomUUID(), createControlLoop());
        ControlLoopNotification notification = transaction.incomingMessage("message 1");
        assertNull(notification);

        VirtualControlLoopEvent onset = createControlLoopEvent(UUID.randomUUID(), CL_NAME, null, "VM", "vserver-name");
        transaction.setControlLoopEvent(onset);
        notification = transaction.incomingMessage("message 2");
        assertNotNull(notification);
    }

    @Test
    public void testIncomingMessageNullOperation() {
        Transaction transaction = new Transaction(mockWorkingMemory, CL_NAME, UUID.randomUUID(), createControlLoop());
        ControlLoopNotification notification = transaction.incomingMessage("test");
        assertNull(notification);
    }

    @Test
    public void testTimeout() {
        Transaction transaction = new Transaction(mockWorkingMemory, CL_NAME, UUID.randomUUID(), createControlLoop());
        VirtualControlLoopEvent onset =
            createControlLoopEvent(UUID.randomUUID(), CL_NAME, null, "VM", "vserver.vserver-name");
        transaction.setControlLoopEvent(onset);
        ControlLoopNotification notification = transaction.timeout();
        assertNotNull(notification);

        transaction.clTimeout();
        notification = transaction.timeout();
        assertNull(notification);
    }

    @Test
    public void testClTimeout() {
        Transaction transaction = new Transaction(mockWorkingMemory, CL_NAME, UUID.randomUUID(), createControlLoop());
        transaction.clTimeout();
        Operation operation = transaction.getCurrentOperation();
        assertNull(operation);
    }

    @Test
    public void testInitialOperationNotification() {
        Transaction transaction = new Transaction(mockWorkingMemory, CL_NAME, UUID.randomUUID(), createControlLoop());
        VirtualControlLoopEvent onset =
                createControlLoopEvent(UUID.randomUUID(), CL_NAME, null, "VM", "vserver.vserver-name");
        transaction.setControlLoopEvent(onset);
        ControlLoopNotification notification = transaction.initialOperationNotification();
        assertNotNull(notification);

        transaction.clTimeout();
        notification = transaction.initialOperationNotification();
        assertNull(notification);
    }

    @Test
    public void testFinalNotification() {
        Transaction transaction = new Transaction(mockWorkingMemory, CL_NAME, UUID.randomUUID(), createControlLoop());
        ControlLoopNotification notification = transaction.finalNotification();
        assertNull(notification);

        VirtualControlLoopEvent onset =
                createControlLoopEvent(UUID.randomUUID(), CL_NAME, null, "VM", "vserver.vserver-name");
        transaction.setControlLoopEvent(onset);
        transaction.timeout();
        notification = transaction.finalNotification();
        assertNotNull(notification);

        transaction.clTimeout();
        notification = transaction.finalNotification();
        assertNotNull(notification);

        // openloop test case
        ControlLoop controlLoop = new ControlLoop();
        controlLoop.setControlLoopName(CL_NAME);
        controlLoop.setTrigger_policy(FinalResult.FINAL_OPENLOOP.toString());
        ControlLoopPolicy controlLoopPolicy = new ControlLoopPolicy();
        controlLoopPolicy.setControlLoop(controlLoop);
        transaction = new Transaction(mockWorkingMemory, CL_NAME, UUID.randomUUID(), controlLoopPolicy);
        transaction.setControlLoopEvent(onset);
        notification = transaction.finalNotification();
        assertNotNull(notification);
    }

    @Test
    public void testProcessError() {
        Transaction transaction = new Transaction(mockWorkingMemory, CL_NAME, UUID.randomUUID(), createControlLoop());
        ControlLoopNotification notification = transaction.processError();
        assertNull(notification);

        VirtualControlLoopEvent onset =
                createControlLoopEvent(UUID.randomUUID(), CL_NAME, null, "VM", "vserver.vserver-name");
        transaction.setControlLoopEvent(onset);
        notification = transaction.processError();
        assertNotNull(notification);
    }

    @Test
    public void testSetNotificationMessage() {
        Transaction transaction = new Transaction(mockWorkingMemory, CL_NAME, UUID.randomUUID(), createControlLoop());
        transaction.setNotificationMessage("test");
        assertEquals("test", transaction.getNotificationMessage());
    }

    @Test
    public void testGetAdjunct() {
        Transaction transaction = new Transaction(mockWorkingMemory, CL_NAME, UUID.randomUUID(), createControlLoop());
        GuardAdjunct adjunct = transaction.getAdjunct(GuardAdjunct.class);
        assertNotNull(adjunct);
    }

    @Test
    public void testPutAdjunct() {
        Transaction transaction = new Transaction(mockWorkingMemory, CL_NAME, UUID.randomUUID(), createControlLoop());
        GuardAdjunct adjunct = new GuardAdjunct();
        boolean status = transaction.putAdjunct(adjunct);
        assertTrue(status);
    }

    @Test
    public void testCleanup() {
        Transaction transaction = new Transaction(mockWorkingMemory, CL_NAME, UUID.randomUUID(), createControlLoop());
        GuardAdjunct adjunct = new GuardAdjunct();
        transaction.putAdjunct(adjunct);
        GuardAdjunct adjunct2 = new GuardAdjunct();
        transaction.putAdjunct(adjunct2);
        transaction.cleanup();
        assertThatCode(() -> transaction.cleanup()).doesNotThrowAnyException();
    }

    private VirtualControlLoopEvent createControlLoopEvent(UUID requestId, String closedLoopControlName,
                    Instant eventStart, String targetType, String target) {

        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(closedLoopControlName);
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event.setRequestId(requestId);
        event.setClosedLoopAlarmStart(eventStart);
        event.setTarget(target);
        event.setTargetType(targetType);
        event.getAai().put("vserver.is-closed-loop-disabled", "false");
        event.getAai().put("vserver.in-maint", "N");
        event.getAai().put("vserver.vserver-name", "vnf01vm01");
        event.getAai().put("generic-vnf.vnf-name", "vnf01");

        return event;
    }

    private ControlLoopPolicy createControlLoop() {
        ControlLoop controlLoop = new ControlLoop();
        controlLoop.setControlLoopName(CL_NAME);
        controlLoop.setTrigger_policy("testId");
        controlLoop.setTimeout(15);

        Policy policy = new Policy();
        policy.setActor("testActor");
        policy.setId("testId");
        policy.setName("testPolicy");
        policy.setRecipe("testOperation");
        policy.setTarget(new Target("testTarget"));
        policy.setRetry(1);
        policy.setTimeout(10);
        policy.setSuccess(FinalResult.FINAL_SUCCESS.toString());
        policy.setFailure(FinalResult.FINAL_FAILURE.toString());
        policy.setFailure_exception(FinalResult.FINAL_FAILURE_EXCEPTION.toString());
        policy.setFailure_guard(FinalResult.FINAL_FAILURE_GUARD.toString());
        policy.setFailure_retries(FinalResult.FINAL_FAILURE_RETRIES.toString());
        policy.setFailure_timeout(FinalResult.FINAL_FAILURE_TIMEOUT.toString());

        LinkedList<Policy> policies = new LinkedList<>();
        policies.add(policy);

        ControlLoopPolicy controlLoopPolicy = new ControlLoopPolicy();
        controlLoopPolicy.setControlLoop(controlLoop);
        controlLoopPolicy.setPolicies(policies);

        return controlLoopPolicy;
    }

    private void assertInvalidTarget(String target) {
        VirtualControlLoopEvent event = createControlLoopEvent(UUID.randomUUID(), CL_NAME, Instant.now(), "VM", target);
        Transaction transaction = new Transaction(null, CL_NAME, event.getRequestId(), createControlLoop());
        assertFalse(transaction.isControlLoopEventValid(event));
        assertEquals("No target field", transaction.getNotificationMessage());
    }
}
