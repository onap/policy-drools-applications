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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.OperationProperties;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.eventmanager.ActorConstants;
import org.onap.policy.controlloop.eventmanager.StepContext;

@RunWith(MockitoJUnitRunner.class)
public class LockStep2Test {
    private static final String MY_TARGET = "my-target";

    @Mock
    private StepContext stepContext;
    @Mock
    private Operation policyOper;
    @Mock
    private VirtualControlLoopEvent event;
    @Mock
    private Consumer<OperationOutcome> callback;

    private ControlLoopOperationParams params;
    private CompletableFuture<OperationOutcome> future;
    private Step2 master;
    private LockStep2 step;

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        future = new CompletableFuture<>();

        when(stepContext.requestLock(MY_TARGET)).thenReturn(future);
        when(stepContext.getProperty(OperationProperties.AAI_TARGET_ENTITY)).thenReturn(MY_TARGET);

        params = ControlLoopOperationParams.builder().completeCallback(callback).build();

        master = new Step2(stepContext, params, event) {
            @Override
            protected Operation buildOperation() {
                return policyOper;
            }
        };

        // force it to build the operation
        master.init();

        step = new LockStep2(master);
    }

    @Test
    public void testConstructor() {
        assertEquals(ActorConstants.LOCK_ACTOR, step.getActorName());
        assertEquals(ActorConstants.LOCK_OPERATION, step.getOperationName());
        assertSame(stepContext, step.stepContext);
        assertSame(event, step.event);
    }

    @Test
    public void testAcceptsEvent() {
        // it should always accept events
        assertTrue(step.acceptsEvent());
    }

    @Test
    public void testStart() {
        // start the operation
        step.init();
        step.setProperties();
        assertTrue(step.start(100));

        // complete the operation's future
        OperationOutcome outcome = step.makeOutcome();
        outcome.setTarget(MY_TARGET);

        future.complete(outcome);

        // ensure it invoked the callback
        verify(callback).accept(outcome);
    }

    /**
     * Tests start when the operation throws an exception.
     */
    @Test
    public void testStartNoFuture() {
        // start the operation
        step.init();

        // force an exception by NOT invoking setProperties()

        assertTrue(step.start(100));

        // should have already invoked the callback
        verify(callback).accept(any());
    }
}
