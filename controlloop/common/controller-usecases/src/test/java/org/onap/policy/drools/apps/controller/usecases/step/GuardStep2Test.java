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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.aai.domain.yang.CloudRegion;
import org.onap.aai.domain.yang.GenericVnf;
import org.onap.aai.domain.yang.RelatedToProperty;
import org.onap.aai.domain.yang.Relationship;
import org.onap.aai.domain.yang.RelationshipData;
import org.onap.aai.domain.yang.RelationshipList;
import org.onap.aai.domain.yang.Vserver;
import org.onap.policy.aai.AaiCqResponse;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actor.guard.DecisionOperation;
import org.onap.policy.controlloop.actor.guard.GuardActor;
import org.onap.policy.controlloop.actor.so.VfModuleCreate;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.OperationProperties;
import org.onap.policy.controlloop.actorserviceprovider.TargetType;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.eventmanager.StepContext;
import org.onap.policy.drools.apps.controller.usecases.UsecasesConstants;

@RunWith(MockitoJUnitRunner.class)
public class GuardStep2Test {
    private static final String SOME_OTHER_VALUE = "some-other-value";
    private static final String SOME_OTHER_KEY = "some-other-key";
    private static final String CL_NAME = "my-closed-loop";
    private static final String MASTER_ACTOR = "master-actor";
    private static final String MASTER_OPERATION = "master-operation";
    private static final String MY_TARGET = "my-target";
    private static final String MY_NAME = "my-name";
    private static final String MY_TYPE = "my-type";
    private static final String MY_CODE = "my-code";
    private static final String MY_SERVER2 = "my-server-2";
    private static final String MY_SERVERNAME = "my-server-name";
    private static final String MY_REGION = "my-region";
    private static final UUID REQ_ID = UUID.randomUUID();
    private static final int VF_COUNT = 10;

    @Mock
    private StepContext stepContext;
    @Mock
    private VirtualControlLoopEvent event;
    @Mock
    private Operation policyOper;
    @Mock
    private AaiCqResponse customQuery;
    @Mock
    private GenericVnf genericVnf;
    @Mock
    private CloudRegion cloudRegion;
    @Mock Vserver theVserver;

    TargetType target;
    Map<String, String> aai = new HashMap<>();

    private ControlLoopOperationParams params;
    private Step2 master;
    private GuardStep2 step;

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        aai.put("vserver.vserver-name", MY_SERVERNAME);
        when(event.getAai()).thenReturn(aai);

        when(genericVnf.getVnfName()).thenReturn(MY_NAME);
        when(genericVnf.getVnfType()).thenReturn(MY_TYPE);
        when(genericVnf.getNfNamingCode()).thenReturn(MY_CODE);

        RelationshipList relList = new RelationshipList();
        when(genericVnf.getRelationshipList()).thenReturn(relList);

        relList.getRelationship().add(new Relationship());

        Relationship relationship = new Relationship();
        relList.getRelationship().add(relationship);
        relationship.setRelatedTo("vserver");

        relationship.getRelatedToProperty().add(new RelatedToProperty());

        // property key mismatch
        RelatedToProperty relProp = new RelatedToProperty();
        relationship.getRelatedToProperty().add(relProp);
        relProp.setPropertyKey(SOME_OTHER_KEY);
        relProp.setPropertyValue(MY_NAME);

        // property value mismatch
        relProp = new RelatedToProperty();
        relationship.getRelatedToProperty().add(relProp);
        relProp.setPropertyKey("vserver.vserver-name");
        relProp.setPropertyValue(SOME_OTHER_VALUE);

        // matching property
        relProp = new RelatedToProperty();
        relationship.getRelatedToProperty().add(relProp);
        relProp.setPropertyKey("vserver.vserver-name");
        relProp.setPropertyValue(MY_SERVERNAME);

        // data key mismatch
        RelationshipData relData = new RelationshipData();
        relationship.getRelationshipData().add(relData);
        relData.setRelationshipKey(SOME_OTHER_KEY);
        relData.setRelationshipValue(SOME_OTHER_VALUE);

        // matching data
        relData = new RelationshipData();
        relationship.getRelationshipData().add(relData);
        relData.setRelationshipKey(GuardStep2.PAYLOAD_KEY_VSERVER_ID);
        relData.setRelationshipValue(MY_SERVER2);

        when(customQuery.getGenericVnfByVnfId(MY_TARGET)).thenReturn(genericVnf);

        when(cloudRegion.getCloudRegionId()).thenReturn(MY_REGION);
        when(customQuery.getDefaultCloudRegion()).thenReturn(cloudRegion);

        when(stepContext.getProperty(OperationProperties.AAI_TARGET_ENTITY)).thenReturn(MY_TARGET);
        when(stepContext.getProperty(AaiCqResponse.CONTEXT_KEY)).thenReturn(customQuery);
        //when(stepContext.getProperty(VSERVER_VSERVER_NAME)).thenReturn()

        when(stepContext.contains(OperationProperties.DATA_VF_COUNT)).thenReturn(true);
        when(stepContext.getProperty(OperationProperties.DATA_VF_COUNT)).thenReturn(VF_COUNT);

        // @formatter:off
        params = ControlLoopOperationParams.builder()
                    .actor(MASTER_ACTOR)
                    .operation(MASTER_OPERATION)
                    .requestId(REQ_ID)
                    .targetType(target)
                    .build();
        // @formatter:on

        master = new Step2(stepContext, params, event) {
            @Override
            protected Operation buildOperation() {
                return policyOper;
            }
        };

        // force it to build the operation
        master.init();

        step = new GuardStep2(master, CL_NAME);
    }

    @Test
    public void testConstructor() {
        assertEquals(GuardActor.NAME, step.getActorName());
        assertEquals(DecisionOperation.NAME, step.getOperationName());
        assertSame(stepContext, step.stepContext);
        assertSame(event, step.event);

        // test when master is uninitialized
        master = new Step2(stepContext, params, event);
        assertThatIllegalStateException().isThrownBy(() -> new GuardStep2(master, CL_NAME));

        ControlLoopOperationParams params2 = step.getParams();

        // @formatter:off
        assertThat(params2.getPayload()).isEqualTo(Map.of(
                        "actor", MASTER_ACTOR,
                        "operation", MASTER_OPERATION,
                        "requestId", REQ_ID,
                        "clname", CL_NAME));
        // @formatter:on
    }

    @Test
    public void testAcceptsEvent() {
        // it should always accept events
        assertTrue(step.acceptsEvent());
    }

    @Test
    public void testGetPropertyNames() {
        // unmatching property names
        when(policyOper.getPropertyNames()).thenReturn(List.of("propA", "propB"));
        assertThat(step.getPropertyNames())
                .containsAll(List.of(UsecasesConstants.AAI_DEFAULT_GENERIC_VNF,
                        OperationProperties.AAI_DEFAULT_CLOUD_REGION));

        // matching property names
        when(policyOper.getPropertyNames())
                .thenReturn(List.of("propA", OperationProperties.DATA_VF_COUNT, "propB"));
        assertThat(step.getPropertyNames()).containsAll(List.of(OperationProperties.DATA_VF_COUNT,
                UsecasesConstants.AAI_DEFAULT_GENERIC_VNF,
                OperationProperties.AAI_DEFAULT_CLOUD_REGION));
    }

    @Test
    public void testLoadTargetEntity() {
        step.loadTargetEntity(OperationProperties.AAI_TARGET_ENTITY);
        assertThat(step.getParams().getPayload()).containsEntry(GuardStep2.PAYLOAD_KEY_TARGET_ENTITY, MY_TARGET);
    }

    @Test
    public void testLoadDefaultGenericVnf() {
        step.loadDefaultGenericVnf(OperationProperties.AAI_VNF);
        assertThat(step.getParams().getPayload())
                .containsEntry(GuardStep2.PAYLOAD_KEY_VNF_ID, MY_TARGET)
                .containsEntry(GuardStep2.PAYLOAD_KEY_VNF_NAME, MY_NAME)
                .containsEntry(GuardStep2.PAYLOAD_KEY_VNF_TYPE, MY_TYPE)
                .containsEntry(GuardStep2.PAYLOAD_KEY_NF_NAMING_CODE, MY_CODE)
                .containsEntry(GuardStep2.PAYLOAD_KEY_VSERVER_ID, MY_SERVER2);
    }

    @Test
    public void testLoadCloudRegion() {
        step.loadCloudRegion(OperationProperties.AAI_DEFAULT_CLOUD_REGION);
        assertThat(step.getParams().getPayload()).containsEntry(GuardStep2.PAYLOAD_KEY_CLOUD_REGION_ID, MY_REGION);
    }

    /**
     * Tests loadVfCount() when the policy operation is NOT "VF Module Create".
     */
    @Test
    public void testLoadVfCountNotVfModuleCreate() {
        // should decrement the count
        step.loadVfCount("");
        assertThat(step.getParams().getPayload()).containsEntry(GuardStep2.PAYLOAD_KEY_VF_COUNT, VF_COUNT - 1);
    }

    /**
     * Tests loadVfCount() when the policy operation is "VF Module Create".
     */
    @Test
    public void testLoadVfCountVfModuleCreate() {
        when(policyOper.getName()).thenReturn(VfModuleCreate.NAME);

        // should increment the count
        step.loadVfCount("");
        assertThat(step.getParams().getPayload()).containsEntry(GuardStep2.PAYLOAD_KEY_VF_COUNT, VF_COUNT + 1);
    }
}
