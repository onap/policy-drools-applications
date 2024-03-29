/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2018-2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.drools.apps.controlloop.feature.management;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.drools.apps.controlloop.feature.management.ControlLoopManagementFeature.Factory;
import org.onap.policy.drools.controller.DroolsController;
import org.onap.policy.drools.system.PolicyController;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Control Loop Management Feature Test.
 */
class ControlLoopManagementFeatureTest {
    private static final String FACTORY_FIELD = "factory";
    private static final String SESSION_NAME = "my-session";
    private static final String CONTROLLER_NAME = "my-controller";

    private static Factory saveFactory;

    @BeforeAll
    public static void setUpBeforeClass() {
        saveFactory = (Factory) ReflectionTestUtils.getField(ControlLoopManagementFeature.class, FACTORY_FIELD);
    }

    @AfterEach
    public void tearDown() {
        ReflectionTestUtils.setField(ControlLoopManagementFeature.class, FACTORY_FIELD, saveFactory);
    }

    /**
     * Sequence Number Test.
     */
    @Test
    void getSequenceNumber() {
        assertEquals(1000, new ControlLoopManagementFeature().getSequenceNumber());
    }

    /**
     * Name Test.
     */
    @Test
    void getName() {
        assertEquals("controlloop-management", new ControlLoopManagementFeature().getName());
    }

    @Test
    void testControlLoops_InvalidArgs() {
        var factory = mock(Factory.class);
        ReflectionTestUtils.setField(ControlLoopManagementFeature.class, FACTORY_FIELD, factory);

        // returns null controller
        when(factory.getController(any())).thenReturn(null);
        assertThatIllegalArgumentException()
            .isThrownBy(() -> ControlLoopManagementFeature.controlLoops(CONTROLLER_NAME, SESSION_NAME))
            .withMessage("Invalid Controller Name");

        // non-matching session name
        var drools = mock(DroolsController.class);
        when(drools.getSessionNames()).thenReturn(Collections.emptyList());
        var ctlr = mock(PolicyController.class);
        when(ctlr.getDrools()).thenReturn(drools);
        when(factory.getController(any())).thenReturn(ctlr);
        assertThatIllegalArgumentException()
            .isThrownBy(() -> ControlLoopManagementFeature.controlLoops(CONTROLLER_NAME, SESSION_NAME))
            .withMessage("Invalid Session Name");
    }

    @Test
    void testFactoryGetController() {
        // invoking controlLoops() will invoke the factory.getController() method
        assertThatIllegalArgumentException().isThrownBy(
            () -> ControlLoopManagementFeature.controlLoops("unknown-controller", SESSION_NAME));
    }
}
