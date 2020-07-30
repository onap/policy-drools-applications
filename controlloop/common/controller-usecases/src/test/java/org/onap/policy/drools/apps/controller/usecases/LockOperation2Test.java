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

package org.onap.policy.drools.apps.controller.usecases;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.aai.domain.yang.GenericVnf;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.eventmanager.StepContext;
import org.onap.policy.controlloop.policy.Target;
import org.onap.policy.controlloop.policy.TargetType;

public class LockOperation2Test {
    private static final String MY_PNF = "my-pnf";
    private static final String MY_VNF = "my-vnf";
    private static final String MY_ACTOR = "my-actor";
    private static final String MY_OPERATION = "my-operation";

    @Mock
    private StepContext stepContext;
    @Mock
    private ControlLoopOperationParams params;

    private AtomicReference<String> targetEntity;
    private VirtualControlLoopEvent event;
    private Target target;
    private CompletableFuture<OperationOutcome> future;
    private GenericVnf vnf;
    private LockOperation2 oper;

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        targetEntity = new AtomicReference<>();

        event = new VirtualControlLoopEvent();
        event.setTarget("pnf.pnf-name");
        event.setAai(Map.of("pnf.pnf-name", MY_PNF));

        target = new Target();
        target.setType(TargetType.PNF);

        future = new CompletableFuture<>();

        vnf = new GenericVnf();
        vnf.setVnfId(MY_VNF);

        when(stepContext.requestLock(anyString())).thenReturn(future);

        when(params.getTarget()).thenReturn(target);
        when(params.getActor()).thenReturn(MY_ACTOR);
        when(params.getOperation()).thenReturn(MY_OPERATION);

        oper = new LockOperation2(stepContext, params, targetEntity);
    }

    @Test
    public void testGetPropertyNames() {
        assertTrue(oper.getPropertyNames().isEmpty());
    }

    @Test
    public void testStart() {
        // missing data
        assertThatIllegalStateException().isThrownBy(() -> oper.start())
        .withMessage("target lock entity has not been determined yet");

        targetEntity.set("some-target");
        assertSame(future, oper.start());
    }

    @Test
    public void testSetProperty() {
        assertThatCode(() -> oper.setProperty("unknown-property", "some data")).doesNotThrowAnyException();
    }

    @Test
    public void testGetActorName_testGetName() {
        assertEquals(MY_ACTOR, oper.getActorName());
        assertEquals(MY_OPERATION, oper.getName());
    }
}
