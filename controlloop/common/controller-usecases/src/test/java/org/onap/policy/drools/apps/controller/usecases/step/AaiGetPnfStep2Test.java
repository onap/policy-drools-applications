/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
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

@RunWith(MockitoJUnitRunner.class)
public class AaiGetPnfStep2Test {
    private static final String MY_TARGET = "my-target";
    private static final UUID REQ_ID = UUID.randomUUID();

    @Mock
    private Operator policyOperator;
    @Mock
    private Operation policyOperation;
    @Mock
    private Actor policyActor;
    @Mock
    private ActorService actors;
    @Mock
    private ControlLoopOperationParams params;
    @Mock
    private StepContext stepContext;
    @Mock
    private VirtualControlLoopEvent event;

    private CompletableFuture<OperationOutcome> future;
    private Step2 master;
    private AaiGetPnfStep2 step;

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
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
    public void testConstructor() {
        assertEquals(AaiActor.NAME, step.getActorName());
        assertEquals(AaiGetPnfOperation.NAME, step.getOperationName());
        assertSame(stepContext, step.stepContext);
        assertSame(event, step.event);
    }

    @Test
    public void testStart() {
        step.init();
        assertTrue(step.start(100));
        verify(policyOperation).start();
    }

    /**
     * Tests start() when the data has already been retrieved.
     */
    @Test
    public void testStartAlreadyHaveData() {
        when(stepContext.contains(AaiGetPnfOperation.getKey(MY_TARGET))).thenReturn(true);

        step.init();
        assertFalse(step.start(200));
        verify(policyOperation, never()).start();
    }

    @Test
    public void testSuccess() {
        StandardCoderObject data = new StandardCoderObject();
        OperationOutcome outcome = new OperationOutcome();
        outcome.setResponse(data);

        step.success(outcome);
        verify(stepContext).setProperty(AaiGetPnfOperation.getKey(MY_TARGET), data);
    }
}
