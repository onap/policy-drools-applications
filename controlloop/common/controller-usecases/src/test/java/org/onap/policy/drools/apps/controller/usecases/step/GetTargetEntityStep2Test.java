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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.aai.domain.yang.GenericVnf;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.eventmanager.StepContext;
import org.onap.policy.controlloop.policy.Target;
import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.drools.apps.controller.usecases.UsecasesConstants;

public class GetTargetEntityStep2Test {
    private static final String GENERIC_VNF_ID = "generic-vnf.vnf-id";
    private static final String VSERVER_NAME = "vserver.vserver-name";
    private static final String GENERIC_VNF_NAME = "generic-vnf.vnf-name";
    private static final String MY_PNF = "my-pnf";
    private static final String MY_VNF = "my-vnf";

    @Mock
    private StepContext stepContext;

    private ControlLoopOperationParams params;
    private VirtualControlLoopEvent event;
    private Target target;
    private GenericVnf vnf;
    private AtomicReference<String> targetEntity;
    private Step2 master;
    private GetTargetEntityStep2 step;

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

        vnf = new GenericVnf();
        vnf.setVnfId(MY_VNF);

        targetEntity = new AtomicReference<>();

        params = ControlLoopOperationParams.builder().target(target).build();

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
        assertNotNull(step.buildOperation());
    }

    @Test
    public void testStart() {
        step.init(null);

        assertThatIllegalStateException().isThrownBy(() -> step.start(200))
                        .withMessage("Target Entity has not been determined yet");

        targetEntity.set("some-target");
        assertFalse(step.start(100));
    }

    @Test
    public void testGetPropertyNames() {
        // already have the data - no properties needed
        assertTrue(step.getPropertyNames().isEmpty());

        // try an operation that needs data
        remakeWithoutData();
        assertEquals(List.of(UsecasesConstants.AAI_DEFAULT_GENERIC_VNF), step.getPropertyNames());

        assertNull(targetEntity.get());
    }

    /**
     * Tests detmTarget() when the target has already been determined.
     */
    @Test
    public void testDetmTargetRepeat() {
        remakeWithoutData();
        assertFalse(step.getPropertyNames().isEmpty());

        step.init(null);

        step.getOperation().setProperty(UsecasesConstants.AAI_DEFAULT_GENERIC_VNF, vnf);
        assertTrue(step.getPropertyNames().isEmpty());

        // repeat
        assertTrue(step.getPropertyNames().isEmpty());
    }

    /**
     * Tests detmTarget() when the target is {@code null}.
     */
    @Test
    public void testDetmTargetNull() {
        params = params.toBuilder().target(null).build();
        remakeWithoutData();

        assertThatIllegalArgumentException().isThrownBy(() -> step.getPropertyNames())
                        .withMessage("The target is null");
    }

    /**
     * Tests detmTarget() when the target type is {@code null}.
     */
    @Test
    public void testDetmTargetNullType() {
        remakeWithoutData();
        target.setType(null);

        assertThatIllegalArgumentException().isThrownBy(() -> step.getPropertyNames())
                        .withMessage("The target type is null");
    }

    /**
     * Tests detmTarget() when the target type is VM and enrichment data is provided.
     */
    @Test
    public void testDetmTargetVm() {
        target.setType(TargetType.VM);
        enrichTarget(VSERVER_NAME);
        assertThat(step.getPropertyNames()).isEmpty();
        assertEquals(MY_VNF, targetEntity.get());
    }

    /**
     * Tests detmTarget() when the target type is VNF and enrichment data is provided.
     */
    @Test
    public void testDetmTargetVnf() {
        target.setType(TargetType.VNF);
        enrichTarget(VSERVER_NAME);
        assertThat(step.getPropertyNames()).isEmpty();
        assertEquals(MY_VNF, targetEntity.get());
    }

    /**
     * Tests detmTarget() when the target type is VF Module and enrichment data is
     * provided.
     */
    @Test
    public void testDetmTargetVfModule() {
        target.setType(TargetType.VFMODULE);
        enrichTarget(VSERVER_NAME);
        assertThat(step.getPropertyNames()).isEmpty();
        assertEquals(MY_VNF, targetEntity.get());
    }

    /**
     * Tests detmTarget() when the target type is unknown.
     */
    @Test
    public void testDetmTargetUnknownType() {
        target.setType(TargetType.VFC);
        enrichTarget(VSERVER_NAME);
        assertThatIllegalArgumentException().isThrownBy(() -> step.getPropertyNames())
                        .withMessage("The target type is not supported");
    }

    @Test
    public void testDetmPnfTargetPnf() {
        assertThat(step.getPropertyNames()).isEmpty();
        assertEquals(MY_PNF, targetEntity.get());
    }

    /**
     * Tests detmPnfTarget() when the target name is incorrect.
     */
    @Test
    public void testDetmPnfTargetInvalidName() {
        event.setTarget("unknown-pnf");
        assertThatIllegalArgumentException().isThrownBy(() -> step.getPropertyNames())
                        .withMessage("Target does not match target type");
    }

    /**
     * Tests detmPnfTarget() when the enrichment data is missing.
     */
    @Test
    public void testDetmPnfTargetPnfNoEnrichment() {
        event.setAai(Map.of());
        assertThatIllegalArgumentException().isThrownBy(() -> step.getPropertyNames())
                        .withMessage("AAI section is missing pnf.pnf-name");
    }

    /**
     * Tests detmVfModuleTarget() when the target is null.
     */
    @Test
    public void testDetmVfModuleTargetNullTarget() {
        target.setType(TargetType.VM);
        event.setTarget(null);
        assertThatIllegalArgumentException().isThrownBy(() -> step.getPropertyNames()).withMessage("Target is null");
    }

    /**
     * Tests detmVfModuleTarget() when the target is the vserver name.
     */
    @Test
    public void testDetmVfModuleTargetVserverName() {
        target.setType(TargetType.VM);
        enrichTarget(VSERVER_NAME);
        assertThat(step.getPropertyNames()).isEmpty();
        assertEquals(MY_VNF, targetEntity.get());
    }

    /**
     * Tests detmVfModuleTarget() when the target is the VNF ID.
     */
    @Test
    public void testDetmVfModuleTargetVnfId() {
        target.setType(TargetType.VM);
        enrichTarget(GENERIC_VNF_ID);
        assertThat(step.getPropertyNames()).isEmpty();
        assertEquals(MY_VNF, targetEntity.get());
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

        assertThat(step.getPropertyNames()).isEmpty();
        assertEquals(MY_VNF, targetEntity.get());
    }

    /**
     * Tests detmVfModuleTarget() when the target is unknown.
     */
    @Test
    public void testDetmVfModuleTargetUnknown() {
        target.setType(TargetType.VM);
        enrichTarget(VSERVER_NAME);
        event.setTarget("unknown-vnf");
        assertThatIllegalArgumentException().isThrownBy(() -> step.getPropertyNames())
                        .withMessage("Target does not match target type");
    }

    /**
     * Tests detmVfModuleTarget() when the enrichment data is missing.
     */
    @Test
    public void testDetmVfModuleTargetMissingEnrichment() {
        target.setType(TargetType.VM);
        event.setTarget(VSERVER_NAME);
        assertThatIllegalArgumentException().isThrownBy(() -> step.getPropertyNames())
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
        assertThat(step.getPropertyNames()).isEmpty();
        assertEquals(MY_VNF, targetEntity.get());
    }

    /**
     * Tests detmVnfName() when enrichment data is missing.
     */
    @Test
    public void testDetmVnfNameNoData() {
        target.setType(TargetType.VM);
        event.setTarget(GENERIC_VNF_NAME);
        assertThat(step.getPropertyNames()).isEqualTo(List.of(UsecasesConstants.AAI_DEFAULT_GENERIC_VNF));
    }

    @Test
    public void testSetProperty() {
        target.setType(TargetType.VM);
        event.setTarget(GENERIC_VNF_NAME);

        step.init(null);

        // no a property of interest - should be ignored
        step.getOperation().setProperty("unknown-property", vnf);
        assertNull(targetEntity.get());

        // now set the desired property and try again
        step.getOperation().setProperty(UsecasesConstants.AAI_DEFAULT_GENERIC_VNF, vnf);
        assertEquals(MY_VNF, targetEntity.get());
    }

    @Test
    public void testGetActorName_testGetName() {
        assertEquals(UsecasesConstants.GET_TARGET_ENTITY_ACTOR, step.getActorName());
        assertEquals(UsecasesConstants.GET_TARGET_ENTITY_OPERATION, step.getOperationName());
    }

    private void remakeWithoutData() {
        target.setType(TargetType.VNF);
        event.setTarget(GENERIC_VNF_NAME);
        targetEntity = new AtomicReference<>();
        master = new Step2(stepContext, params, event);
        step = new GetTargetEntityStep2(master, targetEntity);
    }

    private void enrichTarget(String enrichmentTargetType) {
        event.setTarget(enrichmentTargetType);
        event.setAai(Map.of(enrichmentTargetType, MY_VNF));
    }
}
