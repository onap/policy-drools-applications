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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
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
import org.onap.policy.controlloop.ControlLoopTargetType;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actorserviceprovider.ActorService;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.Operator;
import org.onap.policy.controlloop.actorserviceprovider.spi.Actor;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.controlloop.eventmanager.ControlLoopEventManager2.NewEventStatus;
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
    private static final String ACTOR1 = "First";
    private static final String OPERATION1 = "OperationA";

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
    private Operator operator1;
    @Mock
    private Operation operation1;
    @Mock
    private Actor actor1;
    @Mock
    private ActorService actors;

    private CompletableFuture<OperationOutcome> future1;
    private List<LockImpl> locks;
    private Target target;
    private ToscaPolicy tosca;
    private ControlLoopParams params;
    private VirtualControlLoopEvent event;
    private ControlLoopEventManager2 mgr;

    /**
     * Sets up.
     */
    @Before
    public void setUp() throws ControlLoopException, CoderException {
        MockitoAnnotations.initMocks(this);

        when(workMem.getFactHandle(any())).thenReturn(factHandle);

        future1 = new CompletableFuture<>();

        // configure actors
        when(actors.getActor(ACTOR1)).thenReturn(actor1);
        when(actor1.getOperator(OPERATION1)).thenReturn(operator1);
        when(operator1.buildOperation(any())).thenReturn(operation1);
        when(operation1.start()).thenReturn(future1);

        event = new VirtualControlLoopEvent();
        event.setRequestId(REQ_ID);
        event.setTarget(ControlLoopOperationManager2.VSERVER_VSERVER_NAME);
        event.setAai(new TreeMap<>(Map.of(ControlLoopOperationManager2.VSERVER_VSERVER_NAME, MY_TARGET)));
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event.setClosedLoopControlName(CL_NAME);
        event.setTargetType(TargetType.VNF.toString());

        target = new Target();
        target.setType(TargetType.VNF);

        ToscaServiceTemplate template = yamlCoder.decode(ResourceUtils.getResourceAsString("event-mgr-simple.yaml"),
                        ToscaServiceTemplate.class);
        tosca = template.getToscaTopologyTemplate().getPolicies().get(0).values().iterator().next();

        params = new ControlLoopParams();
        params.setClosedLoopControlName(CL_NAME);
        params.setPolicyName(POLICY_NAME);
        params.setPolicyScope(POLICY_SCOPE);
        params.setPolicyVersion(POLICY_VERSION);
        params.setToscaPolicy(tosca);

        locks = new ArrayList<>();

        mgr = new ControlLoopEventManager2(params, event, workMem) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void makeLock(String targetEntity, String requestId, int holdSec, LockCallback callback) {
                createPseudoLock(targetEntity, requestId, holdSec, callback);
            }

            @Override
            public ActorService getActorService() {
                return actors;
            }
        };
    }

    @Test
    public void testConstructor() {

        Map<String, String> orig = event.getAai();

        event.setAai(addAai(orig, ControlLoopEventManager2.VSERVER_IS_CLOSED_LOOP_DISABLED, "true"));
        assertThatThrownBy(() -> new ControlLoopEventManager2(params, event, workMem))
                        .hasMessage("is-closed-loop-disabled is set to true on VServer or VNF");

        event.setAai(addAai(orig, ControlLoopEventManager2.VSERVER_PROV_STATUS, "inactive"));
        assertThatThrownBy(() -> new ControlLoopEventManager2(params, event, workMem))
                        .hasMessage("prov-status is not ACTIVE on VServer or VNF");

        // valid
        event.setAai(orig);
        assertThatCode(() -> mgr.checkEventSyntax(event)).doesNotThrowAnyException();

        // invalid
        event.setTarget("unknown-target");
        assertThatThrownBy(() -> new ControlLoopEventManager2(params, event, workMem));
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
                        .hasMessage("manager was not created by this JVM");

        // no fact handle
        when(workMem.getFactHandle(any())).thenReturn(null);
        assertThatCode(() -> mgr.start()).isInstanceOf(IllegalStateException.class)
                        .hasMessage("manager is not in working memory");
    }

    @Test
    public void testNextStep() {
        fail("Not yet implemented");
    }

    @Test
    public void testStartOperation() {
        fail("Not yet implemented");
    }

    @Test
    public void testIsActive() throws Exception {
        mgr = new ControlLoopEventManager2(params, event, workMem);
        assertTrue(mgr.isActive());

        ControlLoopEventManager2 mgr2 = Serializer.roundTrip(mgr);
        assertFalse(mgr2.isActive());
    }

    @Test
    public void testUpdated() {
        fail("Not yet implemented");
    }

    @Test
    public void testDestroy() {
        mgr.requestLock(LOCK1, callback1);
        mgr.requestLock(LOCK2, callback2);
        mgr.requestLock(LOCK1, callback3);

        mgr.destroy();

        for (LockImpl lock : locks) {
            assertTrue(lock.isUnavailable());
        }
    }

    /**
     * Tests destroy() once it has been started.
     */
    @Test
    public void testDestroyStarted() {
        fail("Not yet implemented");
    }

    @Test
    public void testMakeNotification() {
        fail("Not yet implemented");
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
    public void testDetmControlLoopTimeoutMs() {
        fail("Not yet implemented");
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
    public void testMakeOperationManager() {
        fail("Not yet implemented");
    }

    @Test
    public void testCreateRealLock() {
        fail("Not yet implemented");
    }

    @Test
    public void testCreatePseudoLock() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetActorService() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetDataManager() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetPolicyName() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetUseTargetLock() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetNotification() {
        fail("Not yet implemented");
    }

    @Test
    public void testToString() {
        fail("Not yet implemented");
    }


    private void createPseudoLock(String targetEntity, String requestId, int holdSec, LockCallback callback) {
        LockImpl lock = new LockImpl(LockState.ACTIVE, targetEntity, requestId, holdSec, callback);
        locks.add(lock);
        callback.lockAvailable(lock);
    }
}
