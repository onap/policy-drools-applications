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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
    private static final String GENERIC_VNF_ID = "generic-vnf.vnf-id";
    private static final String VSERVER_NAME = "vserver.vserver-name";
    private static final String GENERIC_VNF_NAME = "generic-vnf.vnf-name";
    private static final String MY_PNF = "my-pnf";
    private static final String MY_VNF = "my-vnf";
    private static final String MY_ACTOR = "my-actor";
    private static final String MY_OPERATION = "my-operation";

    @Mock
    private StepContext stepContext;
    @Mock
    private ControlLoopOperationParams params;

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

        oper = new LockOperation2(stepContext, event, params);
    }

    @Test
    public void testGetPropertyNames() {
        // already have the data - no properties needed
        assertTrue(oper.getPropertyNames().isEmpty());

        // try an operation that needs data
        remakeWithoutData();
        assertEquals(List.of(UsecasesConstants.AAI_DEFAULT_GENERIC_VNF), oper.getPropertyNames());

        assertNull(oper.getTargetEntity());
    }

    @Test
    public void testStart() {
        assertSame(future, oper.start());

        // retry using an operation that still needs data
        remakeWithoutData();
        assertThatIllegalStateException().isThrownBy(() -> oper.start())
                        .withMessage("target lock entity has not been determined yet");
    }

    /**
     * Tests detmTarget() when the target has already been determined.
     */
    @Test
    public void testDetmTargetRepeat() {
        remakeWithoutData();
        assertFalse(oper.getPropertyNames().isEmpty());

        oper.setProperty(UsecasesConstants.AAI_DEFAULT_GENERIC_VNF, vnf);
        assertTrue(oper.getPropertyNames().isEmpty());

        // repeat
        assertTrue(oper.getPropertyNames().isEmpty());
    }

    /**
     * Tests detmTarget() when the target is {@code null}.
     */
    @Test
    public void testDetmTargetNull() {
        remakeWithoutData();
        when(params.getTarget()).thenReturn(null);

        assertThatIllegalArgumentException().isThrownBy(() -> oper.getPropertyNames())
                        .withMessage("The target is null");
    }

    /**
     * Tests detmTarget() when the target type is {@code null}.
     */
    @Test
    public void testDetmTargetNullType() {
        remakeWithoutData();
        target.setType(null);

        assertThatIllegalArgumentException().isThrownBy(() -> oper.getPropertyNames())
                        .withMessage("The target type is null");
    }

    /**
     * Tests detmTarget() when the target type is VM and enrichment data is provided.
     */
    @Test
    public void testDetmTargetVm() {
        target.setType(TargetType.VM);
        enrichTarget(VSERVER_NAME);
        oper.start();
        verify(stepContext).requestLock(MY_VNF);
        assertEquals(MY_VNF, oper.getTargetEntity());
    }

    /**
     * Tests detmTarget() when the target type is VNF and enrichment data is provided.
     */
    @Test
    public void testDetmTargetVnf() {
        target.setType(TargetType.VNF);
        enrichTarget(VSERVER_NAME);
        oper.start();
        verify(stepContext).requestLock(MY_VNF);
        assertEquals(MY_VNF, oper.getTargetEntity());
    }

    /**
     * Tests detmTarget() when the target type is VF Module and enrichment data is
     * provided.
     */
    @Test
    public void testDetmTargetVfModule() {
        target.setType(TargetType.VFMODULE);
        enrichTarget(VSERVER_NAME);
        oper.start();
        verify(stepContext).requestLock(MY_VNF);
        assertEquals(MY_VNF, oper.getTargetEntity());
    }

    /**
     * Tests detmTarget() when the target type is unknown.
     */
    @Test
    public void testDetmTargetUnknownType() {
        target.setType(TargetType.VFC);
        enrichTarget(VSERVER_NAME);
        assertThatIllegalArgumentException().isThrownBy(() -> oper.start())
                        .withMessage("The target type is not supported");
    }

    @Test
    public void testDetmPnfTargetPnf() {
        oper.start();
        verify(stepContext).requestLock(MY_PNF);
        assertEquals(MY_PNF, oper.getTargetEntity());
    }

    /**
     * Tests detmPnfTarget() when the target name is incorrect.
     */
    @Test
    public void testDetmPnfTargetInvalidName() {
        event.setTarget("unknown-pnf");
        assertThatIllegalArgumentException().isThrownBy(() -> oper.start())
                        .withMessage("Target does not match target type");
    }

    /**
     * Tests detmPnfTarget() when the enrichment data is missing.
     */
    @Test
    public void testDetmPnfTargetPnfNoEnrichment() {
        event.setAai(Map.of());
        assertThatIllegalArgumentException().isThrownBy(() -> oper.start())
                        .withMessage("AAI section is missing pnf.pnf-name");
    }

    /**
     * Tests detmVfModuleTarget() when the target is null.
     */
    @Test
    public void testDetmVfModuleTargetNullTarget() {
        target.setType(TargetType.VM);
        event.setTarget(null);
        assertThatIllegalArgumentException().isThrownBy(() -> oper.start()).withMessage("Target is null");
    }

    /**
     * Tests detmVfModuleTarget() when the target is the vserver name.
     */
    @Test
    public void testDetmVfModuleTargetVserverName() {
        target.setType(TargetType.VM);
        enrichTarget(VSERVER_NAME);
        oper.start();
        verify(stepContext).requestLock(MY_VNF);
        assertEquals(MY_VNF, oper.getTargetEntity());
    }

    /**
     * Tests detmVfModuleTarget() when the target is the VNF ID.
     */
    @Test
    public void testDetmVfModuleTargetVnfId() {
        target.setType(TargetType.VM);
        enrichTarget(GENERIC_VNF_ID);
        oper.start();
        verify(stepContext).requestLock(MY_VNF);
        assertEquals(MY_VNF, oper.getTargetEntity());
    }

    /**
     * Tests detmVfModuleTarget() when the target is the VNF name. Note: the enrichment
     * data is actually stored in the VNF ID field.
     */
    @Test
    public void testDetmVfModuleTargetVnfName() {
        target.setType(TargetType.VM);
        enrichTarget(GENERIC_VNF_ID);

        // set this AFTER setting enrichment data
        event.setTarget("generic-vnf.vnf-name");

        oper.start();
        verify(stepContext).requestLock(MY_VNF);
        assertEquals(MY_VNF, oper.getTargetEntity());
    }

    /**
     * Tests detmVfModuleTarget() when the target is unknown.
     */
    @Test
    public void testDetmVfModuleTargetUnknown() {
        target.setType(TargetType.VM);
        enrichTarget(VSERVER_NAME);
        event.setTarget("unknown-vnf");
        assertThatIllegalArgumentException().isThrownBy(() -> oper.start())
                        .withMessage("Target does not match target type");
    }

    /**
     * Tests detmVfModuleTarget() when the enrichment data is missing.
     */
    @Test
    public void testDetmVfModuleTargetMissingEnrichment() {
        target.setType(TargetType.VM);
        event.setTarget(VSERVER_NAME);
        assertThatIllegalArgumentException().isThrownBy(() -> oper.start())
                        .withMessage("Enrichment data is missing " + VSERVER_NAME);
    }

    /**
     * Tests detmVnfName() when enrichment data is available.
     */
    @Test
    public void testDetmVnfNameHaveData() {
        target.setType(TargetType.VM);
        event.setTarget(GENERIC_VNF_NAME);
        event.setAai(Map.of(GENERIC_VNF_ID, MY_VNF));
        assertThat(oper.getPropertyNames()).isEmpty();
        assertEquals(MY_VNF, oper.getTargetEntity());
    }

    /**
     * Tests detmVnfName() when enrichment data is missing.
     */
    @Test
    public void testDetmVnfNameNoData() {
        target.setType(TargetType.VM);
        event.setTarget(GENERIC_VNF_NAME);
        assertThat(oper.getPropertyNames()).isEqualTo(List.of(UsecasesConstants.AAI_DEFAULT_GENERIC_VNF));
    }

    @Test
    public void testSetProperty() {
        target.setType(TargetType.VM);
        event.setTarget(GENERIC_VNF_NAME);

        // no a property of interest - should be ignored
        oper.setProperty("unknown-property", vnf);
        assertNull(oper.getTargetEntity());
        assertThatIllegalStateException().isThrownBy(() -> oper.start())
                        .withMessage("target lock entity has not been determined yet");

        // now set the desired property and try again
        oper.setProperty(UsecasesConstants.AAI_DEFAULT_GENERIC_VNF, vnf);
        assertEquals(MY_VNF, oper.getTargetEntity());
        assertSame(future, oper.start());
    }

    @Test
    public void testGetActorName_testGetName() {
        assertEquals(MY_ACTOR, oper.getActorName());
        assertEquals(MY_OPERATION, oper.getName());
    }

    private void remakeWithoutData() {
        target.setType(TargetType.VNF);
        event.setTarget(GENERIC_VNF_NAME);
        oper = new LockOperation2(stepContext, event, params);
    }

    private void enrichTarget(String enrichmentTargetType) {
        event.setTarget(enrichmentTargetType);
        event.setAai(Map.of(enrichmentTargetType, MY_VNF));
    }
}
