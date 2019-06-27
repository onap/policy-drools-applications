/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2018-2019 AT&T Intellectual Property. All rights reserved.
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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.drools.apps.controlloop.feature.management.ControlLoopManagementFeature.Factory;
import org.onap.policy.drools.controller.DroolsController;
import org.onap.policy.drools.system.PolicyController;
import org.powermock.reflect.Whitebox;

/**
 * Control Loop Management Feature Test.
 */
public class ControlLoopManagementFeatureTest {
    private static final String FACTORY_FIELD = "factory";
    private static final String SESSION_NAME = "my-session";
    private static final String CONTROLLER_NAME = "my-controller";

    private static Factory saveFactory;

    @BeforeClass
    public static void setUpBeforeClass() {
        saveFactory = Whitebox.getInternalState(ControlLoopManagementFeature.class, FACTORY_FIELD);
    }

    @After
    public void tearDown() {
        Whitebox.setInternalState(ControlLoopManagementFeature.class, FACTORY_FIELD, saveFactory);
    }

    /**
     * Sequence Number Test.
     */
    @Test
    public void getSequenceNumber() {
        Assert.assertEquals(1000, new ControlLoopManagementFeature().getSequenceNumber());
    }

    /**
     * Name Test.
     */
    @Test
    public void getName() {
        Assert.assertEquals("controlloop-management", new ControlLoopManagementFeature().getName());
    }

    @Test
    public void testControlLoops_InvalidArgs() {
        Factory factory = mock(Factory.class);
        Whitebox.setInternalState(ControlLoopManagementFeature.class, FACTORY_FIELD, factory);

        // returns null controller
        when(factory.getController(any())).thenReturn(null);
        assertThatIllegalArgumentException()
            .isThrownBy(() -> ControlLoopManagementFeature.controlLoops(CONTROLLER_NAME, SESSION_NAME))
            .withMessage("Invalid Controller Name");

        // non-matching session name
        PolicyController ctlr = mock(PolicyController.class);
        DroolsController drools = mock(DroolsController.class);
        when(drools.getSessionNames()).thenReturn(Collections.emptyList());
        when(ctlr.getDrools()).thenReturn(drools);
        when(factory.getController(any())).thenReturn(ctlr);
        assertThatIllegalArgumentException()
            .isThrownBy(() -> ControlLoopManagementFeature.controlLoops(CONTROLLER_NAME, SESSION_NAME))
            .withMessage("Invalid Session Name");
    }

    @Test
    public void testFactoryGetController() {
        // invoking controlLoops() will invoke the factory.getController() method
        assertThatIllegalArgumentException().isThrownBy(
            () -> ControlLoopManagementFeature.controlLoops("unknown-controller", SESSION_NAME));
    }
}
