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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.LinkedList;
import java.util.UUID;

import org.drools.core.WorkingMemory;
import org.drools.core.impl.StatefulKnowledgeSessionImpl;
import org.junit.Ignore;
import org.junit.Test;

import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopNotification;
import org.onap.policy.controlloop.ControlLoopTargetType;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.policy.ControlLoop;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.controlloop.policy.FinalResult;
import org.onap.policy.controlloop.policy.Policy;

public class TransactionTest {

    private VirtualControlLoopEvent setControlLoopEvent(UUID requestId, String closedLoopControlName,
            Instant eventStart, String targetType, String target) {

        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(closedLoopControlName);
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event.setRequestId(requestId);
        event.setClosedLoopAlarmStart(eventStart);
        event.setTarget(target);
        event.setTargetType(targetType);
        event.getAai().put("vserver.is-closed-loop-disabled", "false");
        event.getAai().put("complex.state", "NJ");
        event.getAai().put("vserver.l-interface.interface-name", "89ee9ee6-1e96-4063-b690-aa5ca9f73b32");
        event.getAai().put("vserver.l-interface.l3-interface-ipv4-address-list.l3-inteface-ipv4-address",
            "135.144.3.49");
        event.getAai().put("vserver.l-interface.l3-interface-ipv6-address-list.l3-inteface-ipv6-address", null);
        event.getAai().put("vserver.in-maint", "N");
        event.getAai().put("complex.city", "AAIDefault");
        event.getAai().put("vserver.vserver-id", "aa7a24f9-8791-491f-b31a-c8ba5ad9e2aa");
        event.getAai().put("vserver.l-interface.network-name", "vUSP_DPA3_OAM_3750");
        event.getAai().put("vserver.vserver-name", "ctsf0002vm013");
        event.getAai().put("generic-vnf.vnf-name", "ctsf0002v");
        event.getAai().put("generic-vnf.service-id", "e433710f-9217-458d-a79d-1c7aff376d89");
        event.getAai().put("vserver.selflink", "https://compute-aic.dpa3.cci.att.com:8774/v2/d0719b845a804b368f8ac0bba39e188b/servers/aa7a24f9-8791-491f-b31a-c8ba5ad9e2aa");
        event.getAai().put("generic-vnf.vnf-type", "vUSP - vCTS");
        event.getAai().put("tenant.tenant-id", "d0719b845a804b368f8ac0bba39e188b");
        event.getAai().put("cloud-region.identity-url", "");
        event.getAai().put("vserver.prov-status", "PROV");
        event.getAai().put("complex.physical-location-id", "LSLEILAA");

        return event;
    }

    private ControlLoopPolicy createControlLoop() {

        ControlLoop controlLoop = new ControlLoop();
        controlLoop.setControlLoopName("cltest");
        controlLoop.setTrigger_policy("testid");
        controlLoop.setTimeout(15);

        Policy policy = new Policy();
        policy.setActor("APPCLCM");
        policy.setId("testid");
        policy.setName("policytest");
        policy.setRecipe("restart");
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

    @Test
    public void validControlLoopEventTest() {
        VirtualControlLoopEvent event = setControlLoopEvent(UUID.randomUUID(),
                                        "cltest", Instant.now(), ControlLoopTargetType.VM, "vserver.vserver-name");
        Transaction transaction = new Transaction(null, "clvusptest", event.getRequestId(), createControlLoop());
        assertTrue(transaction.isControlLoopEventValid(event));
    }

    @Test
    public void noRequestIdControlLoopEventTest() {
        VirtualControlLoopEvent event = setControlLoopEvent(null,
                                        "cltest", Instant.now(), ControlLoopTargetType.VM, "vserver.vserver-name");
        Transaction transaction = new Transaction(null, "clvusptest", event.getRequestId(), createControlLoop());
        assertFalse(transaction.isControlLoopEventValid(event));
        assertEquals("No requestID", transaction.getNotificationMessage());
    }

    @Test
    public void noTargetTypeControlLoopEventTest() {
        VirtualControlLoopEvent event = setControlLoopEvent(UUID.randomUUID(),
                                        "cltest", Instant.now(), null, "vserver.vserver-name");
        Transaction transaction = new Transaction(null, "clvusptest", event.getRequestId(), createControlLoop());
        assertFalse(transaction.isControlLoopEventValid(event));
        assertEquals("No targetType", transaction.getNotificationMessage());
    }

    @Test
    public void noTargetControlLoopEventTest() {
        VirtualControlLoopEvent event = setControlLoopEvent(UUID.randomUUID(),
                                        "cltest", Instant.now(), ControlLoopTargetType.VM, null);
        Transaction transaction = new Transaction(null, "clvusptest", event.getRequestId(), createControlLoop());
        assertFalse(transaction.isControlLoopEventValid(event));
        assertEquals("No target field", transaction.getNotificationMessage());
    }

    @Test
    public void getClosedLoopControlNameTest() {
        Transaction transaction = new Transaction(null, "clvusptest", UUID.randomUUID(), createControlLoop());
        assertEquals("clvusptest", transaction.getClosedLoopControlName());
    }

    @Test
    public void getRequestIdTest() {
        UUID requestId = UUID.randomUUID();
        Transaction transaction = new Transaction(null, "clvusptest", requestId, createControlLoop());
        assertEquals(requestId, transaction.getRequestId());
    }

    @Test
    public void getWorkingMemoryTest() {
        // Create mock working session
        StatefulKnowledgeSessionImpl mockWorkingMemory = mock(StatefulKnowledgeSessionImpl.class);
        Transaction transaction = new Transaction(mockWorkingMemory, "clvusptest",
            UUID.randomUUID(), createControlLoop());
        assertEquals(mockWorkingMemory, transaction.getWorkingMemory());
    }

    @Test
    public void getStateCompleteTest() {
        Transaction transaction = new Transaction(null, "clvusptest", UUID.randomUUID(), createControlLoop());
        assertEquals("COMPLETE", transaction.getState());
    }

    @Test
    public void getFinalResultTest() {
        Transaction transaction = new Transaction(null, "clvusptest", UUID.randomUUID(), createControlLoop());
        assertEquals(null, transaction.getFinalResult());
    }

    @Test
    public void finalResultFailureTest() {
        Transaction transaction = new Transaction(null, "clvusptest", UUID.randomUUID(), createControlLoop());
        assertFalse(transaction.finalResultFailure());
    }

    @Test
    public void getTimeoutTest() {
        Transaction transaction = new Transaction(null, "clvusptest", UUID.randomUUID(), createControlLoop());
        assertEquals("15s", transaction.getTimeout());
    }

    @Test
    public void getOperationTimeoutTest() {
        Transaction transaction = new Transaction(null, "clvusptest", UUID.randomUUID(), createControlLoop());
        VirtualControlLoopEvent onset = setControlLoopEvent(UUID.randomUUID(),
                                        "cltest", null, ControlLoopTargetType.VM, "vserver.vserver-name");
        transaction.setControlLoopEvent(onset);
        assertEquals("10s", transaction.getOperationTimeout());
    }

    @Test
    public void getCurrentPolicy() {
        ControlLoopPolicy controlLoopPolicy = createControlLoop();
        Transaction transaction = new Transaction(null, "clvusptest", UUID.randomUUID(), controlLoopPolicy);
        VirtualControlLoopEvent onset = setControlLoopEvent(UUID.randomUUID(),
                                        "cltest", null, ControlLoopTargetType.VM, "vserver.vserver-name");
        transaction.setControlLoopEvent(onset);
        assertEquals(controlLoopPolicy.getPolicies().get(0), transaction.getCurrentPolicy());
    }

    @Test
    public void getOperationTest() {
        Transaction transaction = new Transaction(null, "clvusptest", UUID.randomUUID(), createControlLoop());
        assertEquals(null, transaction.getCurrentOperation());
    }

    @Test
    public void getNotificationTest() {
        Transaction transaction = new Transaction(null, "clvusptest", UUID.randomUUID(), createControlLoop());
        VirtualControlLoopEvent onset = setControlLoopEvent(UUID.randomUUID(),
                                        "cltest", null, ControlLoopTargetType.VM, "vserver.vserver-name");
        transaction.setControlLoopEvent(onset);
        ControlLoopNotification notification = transaction.getNotification(onset);
        assertEquals(onset.getClosedLoopControlName(), notification.getClosedLoopControlName());
        assertEquals(onset.getRequestId(), notification.getRequestId());
        assertEquals(onset.getTarget(), notification.getTarget());
    }
}
