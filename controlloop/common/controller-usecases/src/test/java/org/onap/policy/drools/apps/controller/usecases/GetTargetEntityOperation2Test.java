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

package org.onap.policy.drools.apps.controller.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.aai.domain.yang.GenericVnf;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actorserviceprovider.OperationProperties;
import org.onap.policy.controlloop.actorserviceprovider.TargetType;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.eventmanager.StepContext;

@RunWith(MockitoJUnitRunner.class)
public class GetTargetEntityOperation2Test {
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
    private GenericVnf vnf;
    private GetTargetEntityOperation2 oper;

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        event = new VirtualControlLoopEvent();
        event.setTarget("pnf.pnf-name");
        event.setAai(Map.of("pnf.pnf-name", MY_PNF));

        vnf = new GenericVnf();
        vnf.setVnfId(MY_VNF);

        when(params.getTargetType()).thenReturn(TargetType.PNF);
        when(params.getActor()).thenReturn(MY_ACTOR);
        when(params.getOperation()).thenReturn(MY_OPERATION);

        oper = new GetTargetEntityOperation2(stepContext, event, params);
    }

    @Test
    public void testGetPropertyNames() {
        // already have the data - no properties needed
        assertThat(oper.getPropertyNames()).isEmpty();

        // try an operation that needs data
        remakeWithoutData();
        assertEquals(List.of(UsecasesConstants.AAI_DEFAULT_GENERIC_VNF), oper.getPropertyNames());

        // tell it the entity is available
        when(stepContext.contains(OperationProperties.AAI_TARGET_ENTITY)).thenReturn(true);
        assertThat(oper.getPropertyNames()).isEmpty();
    }

    @Test
    public void testStart() {
        assertThatThrownBy(() -> oper.start()).isInstanceOf(UnsupportedOperationException.class)
                        .hasMessage("cannot start get-target-entity operation");
    }

    /**
     * Tests detmTarget() when the target has already been determined.
     */
    @Test
    public void testDetmTargetRepeat() {
        remakeWithoutData();
        assertFalse(oper.getPropertyNames().isEmpty());

        // tell it the entity is available
        when(stepContext.contains(OperationProperties.AAI_TARGET_ENTITY)).thenReturn(true);
        assertThat(oper.getPropertyNames()).isEmpty();

        // repeat
        assertThat(oper.getPropertyNames()).isEmpty();
    }

    /**
     * Tests detmTarget() when the target type is {@code null}.
     */
    @Test
    public void testDetmTargetNullType() {
        remakeWithoutData();
        when(params.getTargetType()).thenReturn(null);

        assertThatIllegalArgumentException().isThrownBy(() -> oper.getPropertyNames())
                        .withMessage("The target type is null");
    }

    /**
     * Tests detmTarget() when the target type is VM and enrichment data is provided.
     */
    @Test
    public void testDetmTargetVm() {
        when(params.getTargetType()).thenReturn(TargetType.VM);
        enrichTarget(VSERVER_NAME);
        assertThat(oper.getPropertyNames()).isEmpty();
        verifyTarget(MY_VNF);
    }

    /**
     * Tests detmTarget() when the target type is VNF and enrichment data is provided.
     */
    @Test
    public void testDetmTargetVnf() {
        when(params.getTargetType()).thenReturn(TargetType.VNF);
        enrichTarget(VSERVER_NAME);
        assertThat(oper.getPropertyNames()).isEmpty();
        verifyTarget(MY_VNF);
    }

    /**
     * Tests detmTarget() when the target type is VF Module and enrichment data is
     * provided.
     */
    @Test
    public void testDetmTargetVfModule() {
        when(params.getTargetType()).thenReturn(TargetType.VFMODULE);
        enrichTarget(VSERVER_NAME);
        assertThat(oper.getPropertyNames()).isEmpty();
        verifyTarget(MY_VNF);
    }

    /**
     * Tests detmTarget() when the target type is unknown.
     */
    @Test
    public void testDetmTargetUnknownType() {
        when(params.getTargetType()).thenReturn(TargetType.VFC);
        enrichTarget(VSERVER_NAME);
        assertThatIllegalArgumentException().isThrownBy(() -> oper.getPropertyNames())
                        .withMessage("The target type is not supported");
    }

    @Test
    public void testDetmPnfTargetPnf() {
        assertThat(oper.getPropertyNames()).isEmpty();
        verifyTarget(MY_PNF);
    }

    /**
     * Tests detmPnfTarget() when the target name is incorrect.
     */
    @Test
    public void testDetmPnfTargetInvalidName() {
        event.setTarget("unknown-pnf");
        assertThatIllegalArgumentException().isThrownBy(() -> oper.getPropertyNames())
                        .withMessage("Target does not match target type");
    }

    /**
     * Tests detmPnfTarget() when the enrichment data is missing.
     */
    @Test
    public void testDetmPnfTargetPnfNoEnrichment() {
        event.setAai(Map.of());
        assertThatIllegalArgumentException().isThrownBy(() -> oper.getPropertyNames())
                        .withMessage("AAI section is missing pnf.pnf-name");
    }

    /**
     * Tests detmVfModuleTarget() when the target is null.
     */
    @Test
    public void testDetmVfModuleTargetNullTarget() {
        when(params.getTargetType()).thenReturn(TargetType.VM);
        event.setTarget(null);
        assertThatIllegalArgumentException().isThrownBy(() -> oper.getPropertyNames()).withMessage("Target is null");
    }

    /**
     * Tests detmVfModuleTarget() when the target is the vserver name.
     */
    @Test
    public void testDetmVfModuleTargetVserverName() {
        when(params.getTargetType()).thenReturn(TargetType.VM);
        enrichTarget(VSERVER_NAME);
        assertThat(oper.getPropertyNames()).isEmpty();
        verifyTarget(MY_VNF);
    }

    /**
     * Tests detmVfModuleTarget() when the target is the VNF ID.
     */
    @Test
    public void testDetmVfModuleTargetVnfId() {
        when(params.getTargetType()).thenReturn(TargetType.VM);
        enrichTarget(GENERIC_VNF_ID);
        assertThat(oper.getPropertyNames()).isEmpty();
        verifyTarget(MY_VNF);
    }

    /**
     * Tests detmVfModuleTarget() when the target is the VNF name. Note: the enrichment
     * data is actually stored in the VNF ID field.
     */
    @Test
    public void testDetmVfModuleTargetVnfName() {
        when(params.getTargetType()).thenReturn(TargetType.VM);
        enrichTarget(GENERIC_VNF_ID);

        // set this AFTER setting enrichment data
        event.setTarget("generic-vnf.vnf-name");

        assertThat(oper.getPropertyNames()).isEmpty();
        verifyTarget(MY_VNF);
    }

    /**
     * Tests detmVfModuleTarget() when the target is unknown.
     */
    @Test
    public void testDetmVfModuleTargetUnknown() {
        when(params.getTargetType()).thenReturn(TargetType.VM);
        enrichTarget(VSERVER_NAME);
        event.setTarget("unknown-vnf");
        assertThatIllegalArgumentException().isThrownBy(() -> oper.getPropertyNames())
                        .withMessage("Target does not match target type");
    }

    /**
     * Tests detmVfModuleTarget() when the enrichment data is missing.
     */
    @Test
    public void testDetmVfModuleTargetMissingEnrichment() {
        when(params.getTargetType()).thenReturn(TargetType.VM);
        event.setTarget(VSERVER_NAME);
        assertThatIllegalArgumentException().isThrownBy(() -> oper.getPropertyNames())
                        .withMessage("Enrichment data is missing " + VSERVER_NAME);
    }

    /**
     * Tests detmVnfName() when enrichment data is available.
     */
    @Test
    public void testDetmVnfNameHaveData() {
        when(params.getTargetType()).thenReturn(TargetType.VM);
        event.setTarget(GENERIC_VNF_NAME);
        event.setAai(Map.of(GENERIC_VNF_ID, MY_VNF));
        assertThat(oper.getPropertyNames()).isEmpty();
        verifyTarget(MY_VNF);
    }

    /**
     * Tests detmVnfName() when enrichment data is missing.
     */
    @Test
    public void testDetmVnfNameNoData() {
        when(params.getTargetType()).thenReturn(TargetType.VM);
        event.setTarget(GENERIC_VNF_NAME);
        assertThat(oper.getPropertyNames()).isEqualTo(List.of(UsecasesConstants.AAI_DEFAULT_GENERIC_VNF));
    }

    @Test
    public void testSetProperty() {
        event.setTarget(GENERIC_VNF_NAME);

        // not a property of interest - should be ignored
        oper.setProperty("unknown-property", vnf);
        verify(stepContext, never()).setProperty(any(), any());

        // now set the desired property and try again
        oper.setProperty(UsecasesConstants.AAI_DEFAULT_GENERIC_VNF, vnf);
        verifyTarget(MY_VNF);
    }

    @Test
    public void testGetActorName_testGetName() {
        assertEquals(MY_ACTOR, oper.getActorName());
        assertEquals(MY_OPERATION, oper.getName());
    }

    private void verifyTarget(String targetEntity) {
        verify(stepContext).setProperty(OperationProperties.AAI_TARGET_ENTITY, targetEntity);
    }

    private void remakeWithoutData() {
        when(params.getTargetType()).thenReturn(TargetType.VNF);
        event.setTarget(GENERIC_VNF_NAME);
        oper = new GetTargetEntityOperation2(stepContext, event, params);
    }

    private void enrichTarget(String enrichmentTargetType) {
        event.setTarget(enrichmentTargetType);
        event.setAai(Map.of(enrichmentTargetType, MY_VNF));
    }
}
