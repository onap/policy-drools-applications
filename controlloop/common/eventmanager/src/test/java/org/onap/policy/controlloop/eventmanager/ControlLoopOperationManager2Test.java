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
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import lombok.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.aai.domain.yang.GenericVnf;
import org.onap.policy.aai.AaiCqResponse;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actorserviceprovider.ActorService;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.Operator;
import org.onap.policy.controlloop.actorserviceprovider.controlloop.ControlLoopEventContext;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.actorserviceprovider.spi.Actor;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.Target;
import org.onap.policy.controlloop.policy.TargetType;

public class ControlLoopOperationManager2Test {
    private static final UUID REQ_ID = UUID.randomUUID();
    private static final String MISMATCH = "mismatch";
    private static final String POLICY_ID = "my-policy";
    private static final String POLICY_ACTOR = "my-actor";
    private static final String POLICY_OPERATION = "my-operation";
    private static final String MY_TARGET = "my-target";
    private static final String MY_VNF_ID = "my-vnf-id";

    @Mock
    private ManagerContext mgrctx;
    @Mock
    private Operator operator;
    @Mock
    private Operation operation;
    @Mock
    private Actor actor;
    @Mock
    private ActorService actors;
    @Mock
    private AaiCqResponse cqdata;
    @Mock
    private GenericVnf vnf;

    private ControlLoopOperationParams operationParams;
    private Target target;
    private Policy policy;
    private VirtualControlLoopEvent event;
    private ControlLoopEventContext context;
    private ControlLoopOperationManager2 mgr;

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mgrctx.getActorService()).thenReturn(actors);
        when(actors.getActor(POLICY_ACTOR)).thenReturn(actor);
        when(actor.getOperator(POLICY_OPERATION)).thenReturn(operator);

        when(operator.buildOperation(any())).thenAnswer(args -> {
            operationParams = args.getArgument(0);
            return operation;
        });

        when(vnf.getVnfId()).thenReturn(MY_VNF_ID);
        when(cqdata.getDefaultGenericVnf()).thenReturn(vnf);

        target = new Target();
        target.setType(TargetType.VM);

        policy = new Policy();
        policy.setId(POLICY_ID);
        policy.setActor(POLICY_ACTOR);
        policy.setRecipe(POLICY_OPERATION);
        policy.setTarget(target);

        event = new VirtualControlLoopEvent();
        event.setRequestId(REQ_ID);
        event.setTarget(ControlLoopOperationManager2.VSERVER_VSERVER_NAME);
        event.setAai(new TreeMap<>(Map.of(ControlLoopOperationManager2.VSERVER_VSERVER_NAME, MY_TARGET)));

        context = new ControlLoopEventContext(event);
        context.setProperty(AaiCqResponse.CONTEXT_KEY, cqdata);

        mgr = new ControlLoopOperationManager2(mgrctx, context, policy);
    }

    @Test
    public void testStart() {
        fail("Not yet implemented");
    }

    /**
     * Tests start() when detmTarget() (i.e., the first task) throws an exception.
     */
    @Test
    public void testStartDetmTargetException() {
        fail("Not yet implemented");
    }

    /**
     * Tests start() when a subsequent task throws an exception.
     */
    @Test
    public void testStartException() {
        fail("Not yet implemented");
    }

    /**
     * Tests start() when the control loop times out.
     */
    @Test
    public void testStartTimeout() {
        fail("Not yet implemented");
    }

    @Test
    public void testStartOperation() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetOperationMessage() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetOperationResult() {
        fail("Not yet implemented");
    }

    @Test
    public void testHandleException() {
        fail("Not yet implemented");
    }

    @Test
    public void testHandleTimeout() {
        fail("Not yet implemented");
    }

    @Test
    public void testCancel() {
        fail("Not yet implemented");
    }

    @Test
    public void testRequestLock() {
        fail("Not yet implemented");
    }

    @Test
    public void testLockUnavailable() {
        fail("Not yet implemented");
    }

    @Test
    public void testOnStart() {
        fail("Not yet implemented");
    }

    @Test
    public void testOnComplete() {
        fail("Not yet implemented");
    }

    @Test
    public void testAddOutcome() {
        fail("Not yet implemented");
    }

    @Test
    public void testNextStep() {
        fail("Not yet implemented");
    }

    @Test
    public void testProcessOutcome() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetOperationHistory() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetHistory() {
        fail("Not yet implemented");
    }

    @Test
    public void testStoreFailureInDataBase() {
        fail("Not yet implemented");
    }

    @Test
    public void testStoreOperationInDataBase() {
        fail("Not yet implemented");
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
        assertThat(mgr.toString()).contains("state").contains("requestId").contains("policyId").contains("attempts")
                        .contains("operationHistory");
    }


    private void setTargetPnf() {
        event.setTarget(ControlLoopOperationManager2.PNF_NAME);
        event.getAai().clear();
        event.getAai().putAll(Map.of(ControlLoopOperationManager2.PNF_NAME, MY_TARGET));

        target.setType(TargetType.PNF);
    }

    private void setTargetVnfName() {
        event.setTarget(ControlLoopOperationManager2.GENERIC_VNF_VNF_NAME);
        event.getAai().clear();
        event.getAai().putAll(Map.of(ControlLoopOperationManager2.GENERIC_VNF_VNF_ID, MY_TARGET));

        target.setType(TargetType.VM);
    }
}
