/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.drools.apps.controller.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.aai.domain.yang.GenericVnf;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.OperationProperties;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.eventmanager.StepContext;

class LockOperation2Test {
    private static final String MY_PNF = "my-pnf";
    private static final String MY_VNF = "my-vnf";
    private static final String MY_ACTOR = "my-actor";
    private static final String MY_OPERATION = "my-operation";

    private final StepContext stepContext = mock(StepContext.class);
    private final ControlLoopOperationParams params = mock(ControlLoopOperationParams.class);

    private VirtualControlLoopEvent event;
    private CompletableFuture<OperationOutcome> future;
    private GenericVnf vnf;
    private LockOperation2 oper;

    /**
     * Sets up.
     */
    @BeforeEach
    public void setUp() {
        event = new VirtualControlLoopEvent();
        event.setTarget("pnf.pnf-name");
        event.setAai(Map.of("pnf.pnf-name", MY_PNF));

        future = new CompletableFuture<>();

        vnf = new GenericVnf();
        vnf.setVnfId(MY_VNF);

        when(stepContext.requestLock(anyString())).thenReturn(future);

        when(params.getActor()).thenReturn(MY_ACTOR);
        when(params.getOperation()).thenReturn(MY_OPERATION);

        oper = new LockOperation2(stepContext, params);
    }

    @Test
    void testGetPropertyNames() {
        assertThat(oper.getPropertyNames()).isEqualTo(List.of(OperationProperties.AAI_TARGET_ENTITY));
    }

    @Test
    void testStart() {
        // missing data
        assertThatIllegalStateException().isThrownBy(() -> oper.start())
                        .withMessage("target lock entity has not been determined yet");

        oper.setProperty(OperationProperties.AAI_TARGET_ENTITY, "some-target");
        assertSame(future, oper.start());
    }

    @Test
    void testSetProperty() {
        oper.setProperty("unknown-property", "some data");
        assertNull(oper.getTargetEntity());

        oper.setProperty(OperationProperties.AAI_TARGET_ENTITY, "other data");
        assertEquals("other data", oper.getTargetEntity());
    }

    @Test
    void testGetActorName_testGetName() {
        assertEquals(MY_ACTOR, oper.getActorName());
        assertEquals(MY_OPERATION, oper.getName());
    }
}
