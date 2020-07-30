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

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.eventmanager.StepContext;
import org.onap.policy.drools.apps.controller.usecases.GetTargetEntityOperation2;
import org.onap.policy.drools.apps.controller.usecases.UsecasesConstants;

public class GetTargetEntityStep2Test {
    @Mock
    private ControlLoopOperationParams params;
    @Mock
    private StepContext stepContext;
    @Mock
    private VirtualControlLoopEvent event;

    private AtomicReference<String> targetEntity;
    private Step2 master;
    private GetTargetEntityStep2 step;

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(params.toBuilder()).thenReturn(ControlLoopOperationParams.builder());

        targetEntity = new AtomicReference<>();

        master = new Step2(stepContext, params, event);
        step = new GetTargetEntityStep2(master, targetEntity);
    }

    @Test
    public void testConstructor() {
        assertEquals(UsecasesConstants.GET_TARGET_ENTITY_ACTOR, step.getActorName());
        assertEquals(UsecasesConstants.GET_TARGET_ENTITY_OPERATION, step.getOperationName());
        assertSame(stepContext, step.stepContext);
        assertSame(event, step.event);
    }

    @Test
    public void testBuildOperation() {
        Operation oper = step.buildOperation();
        assertTrue(oper instanceof GetTargetEntityOperation2);
    }

    @Test
    public void testStart() {
        step.init();

        assertThatIllegalStateException().isThrownBy(() -> step.start(200))
                        .withMessage("Target Entity has not been determined yet");

        targetEntity.set("some-target");
        assertFalse(step.start(100));
    }
}
