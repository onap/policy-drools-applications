/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.drools.apps.controller.usecases.step;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actor.aai.AaiActor;
import org.onap.policy.controlloop.actor.aai.AaiGetPnfOperation;
import org.onap.policy.controlloop.actorserviceprovider.ActorService;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.OperationProperties;
import org.onap.policy.controlloop.actorserviceprovider.Operator;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.actorserviceprovider.spi.Actor;
import org.onap.policy.controlloop.eventmanager.StepContext;

class AaiGetPnfStep2Test {
    private static final String MY_TARGET = "my-target";
    private static final UUID REQ_ID = UUID.randomUUID();

    private final Operator policyOperator = mock(Operator.class);
    private final Operation policyOperation = mock(Operation.class);
    private final Actor policyActor = mock(Actor.class);
    private final ActorService actors = mock(ActorService.class);
    private final ControlLoopOperationParams params = mock(ControlLoopOperationParams.class);
    private final StepContext stepContext = mock(StepContext.class);
    private final VirtualControlLoopEvent event = mock(VirtualControlLoopEvent.class);

    private CompletableFuture<OperationOutcome> future;
    private Step2 master;
    private AaiGetPnfStep2 step;

    /**
     * Sets up.
     */
    @BeforeEach
    void setUp() {
        future = new CompletableFuture<>();

        when(params.toBuilder()).thenReturn(ControlLoopOperationParams.builder().actorService(actors)
                        .requestId(REQ_ID));

        // configure policy operation
        when(actors.getActor(AaiActor.NAME)).thenReturn(policyActor);
        when(policyActor.getOperator(AaiGetPnfOperation.NAME)).thenReturn(policyOperator);
        when(policyOperator.buildOperation(any())).thenReturn(policyOperation);
        when(policyOperation.start()).thenReturn(future);
        when(stepContext.getProperty(OperationProperties.AAI_TARGET_ENTITY)).thenReturn(MY_TARGET);

        master = new Step2(stepContext, params, event);
        step = new AaiGetPnfStep2(master);
    }

    @Test
    void testConstructor() {
        assertEquals(AaiActor.NAME, step.getActorName());
        assertEquals(AaiGetPnfOperation.NAME, step.getOperationName());
        assertSame(stepContext, step.stepContext);
        assertSame(event, step.event);
    }

    @Test
    void testStart() {
        step.init();
        assertTrue(step.start(100));
        verify(policyOperation).start();
    }

    /**
     * Tests start() when the data has already been retrieved.
     */
    @Test
    void testStartAlreadyHaveData() {
        when(stepContext.contains(AaiGetPnfOperation.getKey(MY_TARGET))).thenReturn(true);

        step.init();
        assertFalse(step.start(200));
        verify(policyOperation, never()).start();
    }

    @Test
    void testSuccess() {
        var data = new StandardCoderObject();
        var outcome = new OperationOutcome();
        outcome.setResponse(data);

        step.success(outcome);
        verify(stepContext).setProperty(AaiGetPnfOperation.getKey(MY_TARGET), data);
    }
}
