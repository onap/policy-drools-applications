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

package org.onap.policy.drools.apps.controller.usecases.step;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actor.guard.DecisionOperation;
import org.onap.policy.controlloop.actor.guard.GuardActor;
import org.onap.policy.controlloop.actor.so.VfModuleCreate;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.OperationProperties;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.eventmanager.ActorConstants;
import org.onap.policy.controlloop.eventmanager.StepContext;

public class GuardStep2Test {
    private static final String CL_NAME = "my-closed-loop";
    private static final String MASTER_ACTOR = "master-actor";
    private static final String MASTER_OPERATION = "master-operation";
    private static final String MY_TARGET = "my-target";
    private static final UUID REQ_ID = UUID.randomUUID();
    private static final int VF_COUNT = 10;

    @Mock
    private StepContext stepContext;
    @Mock
    private VirtualControlLoopEvent event;
    @Mock
    private Operation policyOper;

    private ControlLoopOperationParams params;
    private Step2 master;
    private GuardStep2 step;

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(event.getRequestId()).thenReturn(REQ_ID);

        when(stepContext.contains(OperationProperties.DATA_VF_COUNT)).thenReturn(true);
        when(stepContext.getProperty(OperationProperties.DATA_VF_COUNT)).thenReturn(VF_COUNT);


        params = ControlLoopOperationParams.builder().actor(MASTER_ACTOR).operation(MASTER_OPERATION)
                        .targetEntity(MY_TARGET).requestId(REQ_ID).build();

        master = new Step2(stepContext, params, event) {
            @Override
            protected Operation buildOperation() {
                return policyOper;
            }
        };

        // force it to build the operation
        master.init();

        step = new GuardStep2(master, CL_NAME);
    }

    @Test
    public void testConstructor() {
        assertEquals(GuardActor.NAME, step.getActorName());
        assertEquals(DecisionOperation.NAME, step.getOperationName());
        assertSame(stepContext, step.stepContext);
        assertSame(event, step.event);

        // test when master is uninitialized
        master = new Step2(stepContext, params, event);
        assertThatIllegalStateException().isThrownBy(() -> new GuardStep2(master, CL_NAME));

        ControlLoopOperationParams params2 = step.getParams();

        // @formatter:off
        assertThat(params2.getPayload()).isEqualTo(Map.of(
                        "actor", MASTER_ACTOR,
                        "operation", MASTER_OPERATION,
                        "target", MY_TARGET,
                        "requestId", REQ_ID,
                        "clname", CL_NAME));
        // @formatter:on
    }

    @Test
    public void testAcceptsEvent() {
        // it should always accept events
        assertTrue(step.acceptsEvent());
    }

    @Test
    public void testGetPropertyNames() {
        // unmatching property names
        when(policyOper.getPropertyNames()).thenReturn(List.of("propA", "propB"));
        assertThat(step.getPropertyNames()).isEmpty();

        // matching property names
        when(policyOper.getPropertyNames()).thenReturn(List.of("propA", OperationProperties.DATA_VF_COUNT, "propB"));
        assertThat(step.getPropertyNames()).isEqualTo(List.of(OperationProperties.DATA_VF_COUNT));
    }

    /**
     * Tests loadVfCount() when the policy operation is NOT "VF Module Create".
     */
    @Test
    public void testLoadVfCountNotVfModuleCreate() {
        // should decrement the count
        step.loadVfCount("");
        assertThat(step.getParams().getPayload().get(ActorConstants.PAYLOAD_KEY_VF_COUNT)).isEqualTo(VF_COUNT - 1);
    }

    /**
     * Tests loadVfCount() when the policy operation is "VF Module Create".
     */
    @Test
    public void testLoadVfCountVfModuleCreate() {
        when(policyOper.getName()).thenReturn(VfModuleCreate.NAME);

        // should increment the count
        step.loadVfCount("");
        assertThat(step.getParams().getPayload().get(ActorConstants.PAYLOAD_KEY_VF_COUNT)).isEqualTo(VF_COUNT + 1);
    }
}
