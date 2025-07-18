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

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.eventmanager.StepContext;
import org.onap.policy.drools.apps.controller.usecases.GetTargetEntityOperation2;
import org.onap.policy.drools.apps.controller.usecases.UsecasesConstants;

class GetTargetEntityStep2Test {
    private final ControlLoopOperationParams params = mock(ControlLoopOperationParams.class);
    private final StepContext stepContext = mock(StepContext.class);
    private final VirtualControlLoopEvent event = mock(VirtualControlLoopEvent.class);

    private Step2 master;
    private GetTargetEntityStep2 step;

    /**
     * Sets up.
     */
    @BeforeEach
    void setUp() {
        when(params.toBuilder()).thenReturn(ControlLoopOperationParams.builder());

        master = new Step2(stepContext, params, event);
        step = new GetTargetEntityStep2(master);
    }

    @Test
    void testConstructor() {
        assertEquals(UsecasesConstants.GET_TARGET_ENTITY_ACTOR, step.getActorName());
        assertEquals(UsecasesConstants.GET_TARGET_ENTITY_OPERATION, step.getOperationName());
        assertSame(stepContext, step.stepContext);
        assertSame(event, step.event);
    }

    @Test
    void testBuildOperation() {
        var oper = step.buildOperation();
        assertTrue(oper instanceof GetTargetEntityOperation2);
    }

    @Test
    void testStart() {
        assertThatIllegalStateException().isThrownBy(() -> step.start(200))
                        .withMessage("step has not been initialized");

        step.init();
        assertFalse(step.start(100));
    }
}
