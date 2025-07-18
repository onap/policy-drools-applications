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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.aai.domain.yang.CloudRegion;
import org.onap.aai.domain.yang.GenericVnf;
import org.onap.aai.domain.yang.ModelVer;
import org.onap.aai.domain.yang.ServiceInstance;
import org.onap.aai.domain.yang.Tenant;
import org.onap.aai.domain.yang.Vserver;
import org.onap.policy.aai.AaiCqResponse;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actor.aai.AaiGetPnfOperation;
import org.onap.policy.controlloop.actor.aai.AaiGetTenantOperation;
import org.onap.policy.controlloop.actorserviceprovider.ActorService;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.OperationProperties;
import org.onap.policy.controlloop.actorserviceprovider.Operator;
import org.onap.policy.controlloop.actorserviceprovider.TargetType;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.actorserviceprovider.spi.Actor;
import org.onap.policy.controlloop.eventmanager.StepContext;
import org.onap.policy.drools.apps.controller.usecases.UsecasesConstants;

class Step2Test {
    private static final UUID REQ_ID = UUID.randomUUID();
    private static final String POLICY_ACTOR = "my-actor";
    private static final String POLICY_OPERATION = "my-operation";
    private static final String MY_TARGET = "my-target";
    private static final String PAYLOAD_KEY = "payload-key";
    private static final String PAYLOAD_VALUE = "payload-value";
    private static final String NO_SLASH = "noslash";
    private static final String ONE_SLASH = "/one";

    private final Operator policyOperator = mock(Operator.class);
    private final Operation policyOperation = mock(Operation.class);
    private final Actor policyActor = mock(Actor.class);
    private final ActorService actors = mock(ActorService.class);
    private final StepContext stepContext = mock(StepContext.class);
    private final AaiCqResponse aaicq = mock(AaiCqResponse.class);

    private Map<String, String> payload;
    private VirtualControlLoopEvent event;
    private BlockingQueue<OperationOutcome> starts;
    private BlockingQueue<OperationOutcome> completions;
    private ControlLoopOperationParams params;
    private Step2 step;

    /**
     * Sets up.
     */
    @BeforeEach
    void setUp() {
        // configure policy operation
        when(actors.getActor(POLICY_ACTOR)).thenReturn(policyActor);
        when(policyActor.getOperator(POLICY_OPERATION)).thenReturn(policyOperator);
        when(policyOperator.buildOperation(any())).thenReturn(policyOperation);

        when(policyOperation.getPropertyNames()).thenReturn(List.of());

        when(stepContext.getProperty(AaiCqResponse.CONTEXT_KEY)).thenReturn(aaicq);

        payload = Map.of(PAYLOAD_KEY, PAYLOAD_VALUE);

        event = new VirtualControlLoopEvent();
        event.setRequestId(REQ_ID);

        starts = new LinkedBlockingQueue<>();
        completions = new LinkedBlockingQueue<>();

        Map<String, String> entityIds = new HashMap<>();

        params = ControlLoopOperationParams.builder().actor(POLICY_ACTOR).actorService(actors)
                        .completeCallback(completions::add).executor(ForkJoinPool.commonPool())
                        .operation(POLICY_OPERATION).payload(new TreeMap<>(payload)).startCallback(starts::add)
                        .targetType(TargetType.VM).targetEntityIds(entityIds)
                        .requestId(REQ_ID).build();

        step = new Step2(stepContext, params, event);
        step.init();
    }

    @Test
    void testConstructor() {
        assertSame(stepContext, step.stepContext);
        assertSame(event, step.event);
        assertSame(actors, step.getParams().getActorService());
    }

    @Test
    void testConstructorStep2() {
        step = new Step2(step, "actorB", "operationB");
        assertSame(stepContext, step.stepContext);
        assertSame(event, step.event);

        assertEquals("actorB", step.getActorName());
        assertEquals("operationB", step.getOperationName());
        assertSame(actors, step.getParams().getActorService());
    }

    @Test
    void testAcceptsEvent() {
        // it's a policy step, thus it accepts events
        assertTrue(step.acceptsEvent());

        step = new Step2(step, "actorB", "operationB");

        // it's not a policy step, thus it does not accept events
        assertFalse(step.acceptsEvent());
    }

    @Test
    void testSuccess() {
        assertThatCode(() -> step.success(null)).doesNotThrowAnyException();
    }

    @Test
    void testGetPropertyNames() {
        // empty property list
        assertThat(step.getPropertyNames()).isEmpty();

        // try with non-empty list
        when(policyOperation.getPropertyNames()).thenReturn(List.of("propA", "propB"));
        assertThat(step.getPropertyNames()).isEqualTo(List.of("propA", "propB"));
    }

    @Test
    void testSetProperties() {
        var cloudRegion = new CloudRegion();
        when(aaicq.getDefaultCloudRegion()).thenReturn(cloudRegion);

        var tenant = new Tenant();
        when(aaicq.getDefaultTenant()).thenReturn(tenant);

        when(policyOperation.getPropertyNames()).thenReturn(
                        List.of(OperationProperties.AAI_DEFAULT_CLOUD_REGION, OperationProperties.AAI_DEFAULT_TENANT));

        step.setProperties();

        // should have been exactly two properties set
        verify(policyOperation, times(2)).setProperty(any(), any());
        verify(policyOperation).setProperty(OperationProperties.AAI_DEFAULT_CLOUD_REGION, cloudRegion);
        verify(policyOperation).setProperty(OperationProperties.AAI_DEFAULT_TENANT, tenant);
    }

    /**
     * Tests setProperties() when the property is unknown.
     */
    @Test
    void testSetPropertiesUnknown() {
        when(policyOperation.getPropertyNames()).thenReturn(List.of("unknown-property"));

        assertThatIllegalArgumentException().isThrownBy(() -> step.setProperties())
                        .withMessage("unknown property unknown-property needed by my-actor.my-operation");
    }

    @Test
    void testLoadCloudRegion_testGetCloudRegion() {
        var data = new CloudRegion();
        when(aaicq.getDefaultCloudRegion()).thenReturn(data);
        when(policyOperation.getPropertyNames()).thenReturn(List.of(OperationProperties.AAI_DEFAULT_CLOUD_REGION));

        step.setProperties();
        verify(policyOperation).setProperty(OperationProperties.AAI_DEFAULT_CLOUD_REGION, data);

        when(aaicq.getDefaultCloudRegion()).thenReturn(null);
        assertThatIllegalArgumentException().isThrownBy(() -> step.getCloudRegion())
                        .withMessageContaining("missing default cloud region in A&AI response");
    }

    @Test
    void testLoadTenant_testGetTenant() {
        var data = new Tenant();
        when(aaicq.getDefaultTenant()).thenReturn(data);
        when(policyOperation.getPropertyNames()).thenReturn(List.of(OperationProperties.AAI_DEFAULT_TENANT));

        step.setProperties();
        verify(policyOperation).setProperty(OperationProperties.AAI_DEFAULT_TENANT, data);

        when(aaicq.getDefaultTenant()).thenReturn(null);
        assertThatIllegalArgumentException().isThrownBy(() -> step.getTenant())
                        .withMessageContaining("missing default tenant in A&AI response");
    }

    @Test
    void testLoadPnf_testGetPnf() {
        var data = new StandardCoderObject();
        when(stepContext.getProperty(OperationProperties.AAI_TARGET_ENTITY)).thenReturn(MY_TARGET);
        when(stepContext.getProperty(AaiGetPnfOperation.getKey(MY_TARGET))).thenReturn(data);
        when(policyOperation.getPropertyNames()).thenReturn(List.of(OperationProperties.AAI_PNF));

        step.setProperties();
        verify(policyOperation).setProperty(OperationProperties.AAI_PNF, data);

        when(stepContext.getProperty(AaiGetPnfOperation.getKey(MY_TARGET))).thenReturn(null);
        assertThatIllegalArgumentException().isThrownBy(() -> step.getPnf())
                        .withMessageContaining("missing PNF for my-target");
    }

    @Test
    void testLoadResourceVnf_testGetResourceVnf() {
        params.getTargetEntityIds().put(Step2.TARGET_RESOURCE_ID, "my-resource");
        var data = new GenericVnf();
        when(aaicq.getGenericVnfByModelInvariantId("my-resource")).thenReturn(data);
        when(policyOperation.getPropertyNames()).thenReturn(List.of(OperationProperties.AAI_RESOURCE_VNF));

        step.setProperties();
        verify(policyOperation).setProperty(OperationProperties.AAI_RESOURCE_VNF, data);

        when(aaicq.getGenericVnfByModelInvariantId("my-resource")).thenReturn(null);
        assertThatIllegalArgumentException().isThrownBy(() -> step.getResourceVnf())
                        .withMessageContaining("missing VNF for my-resource");

        // missing resource ID
        params.getTargetEntityIds().put(Step2.TARGET_RESOURCE_ID, null);
        assertThatIllegalArgumentException().isThrownBy(() -> step.setProperties())
                        .withMessageContaining("missing Target resource ID");

        // missing target entity IDs
        params = params.toBuilder().targetEntityIds(null).build();
        step = new Step2(stepContext, params, event);
        step.init();
        assertThatIllegalArgumentException().isThrownBy(() -> step.setProperties())
                        .withMessageContaining(Step2.TARGET_INFO_MSG);
    }

    @Test
    void testLoadService_testGetService() {
        var data = new ServiceInstance();
        when(aaicq.getServiceInstance()).thenReturn(data);
        when(policyOperation.getPropertyNames()).thenReturn(List.of(OperationProperties.AAI_SERVICE));

        step.setProperties();
        verify(policyOperation).setProperty(OperationProperties.AAI_SERVICE, data);

        when(aaicq.getServiceInstance()).thenReturn(null);
        assertThatIllegalArgumentException().isThrownBy(() -> step.getService())
                        .withMessageContaining("missing service instance in A&AI response");
    }

    @Test
    void testLoadServiceModel_testGetServiceModel() {
        var service = new ServiceInstance();
        service.setModelVersionId("my-service-version");
        when(aaicq.getServiceInstance()).thenReturn(service);

        var data = new ModelVer();
        when(aaicq.getModelVerByVersionId("my-service-version")).thenReturn(data);
        when(policyOperation.getPropertyNames()).thenReturn(List.of(OperationProperties.AAI_SERVICE_MODEL));

        step.setProperties();
        verify(policyOperation).setProperty(OperationProperties.AAI_SERVICE_MODEL, data);

        when(aaicq.getModelVerByVersionId("my-service-version")).thenReturn(null);
        assertThatIllegalArgumentException().isThrownBy(() -> step.getServiceModel())
                        .withMessageContaining("missing model version for service in A&AI response");

        service.setModelVersionId(null);
        assertThatIllegalArgumentException().isThrownBy(() -> step.getServiceModel())
                        .withMessageContaining("missing service model version ID in A&AI response");

        when(aaicq.getServiceInstance()).thenReturn(null);
        assertThatIllegalArgumentException().isThrownBy(() -> step.getServiceModel())
                        .withMessageContaining("missing service instance in A&AI response");
    }

    @Test
    void testGetVserver() {
        var vserver = new Vserver();
        when(aaicq.getVserver()).thenReturn(vserver);

        assertSame(vserver, step.getVServer());

        when(aaicq.getVserver()).thenReturn(null);
        assertThatIllegalArgumentException().isThrownBy(() -> step.getVServer())
                        .withMessageContaining("missing vserver in A&AI response");
    }

    @Test
    void testGetTargetEntity() {
        when(stepContext.getProperty(OperationProperties.AAI_TARGET_ENTITY)).thenReturn(MY_TARGET);

        assertEquals(MY_TARGET, step.getTargetEntity());

        when(stepContext.getProperty(OperationProperties.AAI_TARGET_ENTITY)).thenReturn(null);
        assertThatIllegalArgumentException().isThrownBy(() -> step.getTargetEntity())
                        .withMessageContaining("missing A&AI target entity");
    }

    @Test
    void testLoadVnf_testGetVnf() {
        params.getTargetEntityIds().put(Step2.TARGET_MODEL_INVARIANT_ID, "my-model-invariant");
        var data = new GenericVnf();
        when(aaicq.getGenericVnfByVfModuleModelInvariantId("my-model-invariant")).thenReturn(data);
        when(policyOperation.getPropertyNames()).thenReturn(List.of(OperationProperties.AAI_VNF));

        step.setProperties();
        verify(policyOperation).setProperty(OperationProperties.AAI_VNF, data);

        when(aaicq.getGenericVnfByVfModuleModelInvariantId("my-model-invariant")).thenReturn(null);
        assertThatIllegalArgumentException().isThrownBy(() -> step.getVnf())
                        .withMessageContaining("missing generic VNF in A&AI response for my-model-invariant");

        // missing model invariant ID
        params.getTargetEntityIds().put(Step2.TARGET_MODEL_INVARIANT_ID, null);
        assertThatIllegalArgumentException().isThrownBy(() -> step.setProperties())
                        .withMessageContaining("missing modelInvariantId");

        // missing target
        params = params.toBuilder().targetEntityIds(null).build();
        step = new Step2(stepContext, params, event);
        step.init();
        assertThatIllegalArgumentException().isThrownBy(() -> step.setProperties())
                        .withMessageContaining(Step2.TARGET_INFO_MSG);
    }

    @Test
    void testLoadVnfModel_testGetVnfModel() {
        params.getTargetEntityIds().put(Step2.TARGET_MODEL_INVARIANT_ID, "my-model-invariant");
        var vnf = new GenericVnf();
        when(aaicq.getGenericVnfByVfModuleModelInvariantId("my-model-invariant")).thenReturn(vnf);

        vnf.setModelVersionId("my-vnf-model-version-id");
        var data = new ModelVer();
        when(aaicq.getModelVerByVersionId("my-vnf-model-version-id")).thenReturn(data);
        when(policyOperation.getPropertyNames()).thenReturn(List.of(OperationProperties.AAI_VNF_MODEL));

        step.setProperties();
        verify(policyOperation).setProperty(OperationProperties.AAI_VNF_MODEL, data);

        when(aaicq.getModelVerByVersionId("my-vnf-model-version-id")).thenReturn(null);
        assertThatIllegalArgumentException().isThrownBy(() -> step.getVnfModel())
                        .withMessageContaining("missing model version for generic VNF in A&AI response");

        vnf.setModelVersionId(null);
        assertThatIllegalArgumentException().isThrownBy(() -> step.getVnfModel())
                        .withMessageContaining("missing model version ID for generic VNF in A&AI response");
    }

    @Test
    void testLoadVserverLink_testGetVserverLink() {
        event.setAai(Map.of(Step2.VSERVER_VSERVER_NAME, "vserverB"));

        var tenant = mock(StandardCoderObject.class);
        when(stepContext.getProperty(AaiGetTenantOperation.getKey("vserverB"))).thenReturn(tenant);

        when(tenant.getString("result-data", 0, "resource-link")).thenReturn("/aai/v7/some/link/bbb");

        when(policyOperation.getPropertyNames()).thenReturn(List.of(OperationProperties.AAI_VSERVER_LINK));

        step.setProperties();
        verify(policyOperation).setProperty(OperationProperties.AAI_VSERVER_LINK, "/some/link/bbb");

        // missing resource link
        when(tenant.getString("result-data", 0, "resource-link")).thenReturn(null);
        assertThatIllegalArgumentException().isThrownBy(() -> step.setProperties())
                        .withMessageContaining("missing tenant data resource-link");

        // missing tenant data
        when(stepContext.getProperty(AaiGetTenantOperation.getKey("vserverB"))).thenReturn(null);
        assertThatIllegalArgumentException().isThrownBy(() -> step.setProperties())
                        .withMessageContaining("missing tenant data for");

        // empty vserver name
        event.setAai(Map.of(Step2.VSERVER_VSERVER_NAME, ""));
        assertThatIllegalArgumentException().isThrownBy(() -> step.setProperties())
                        .withMessageContaining("missing vserver.vserver-name");

        // missing vserver name
        event.setAai(Map.of());
        assertThatIllegalArgumentException().isThrownBy(() -> step.setProperties())
                        .withMessageContaining("missing vserver.vserver-name");
    }

    @Test
    void testLoadVfCount_testGetVfCount() {
        params.getTargetEntityIds().put(Step2.TARGET_MODEL_CUSTOMIZATION_ID, "vf-count-customization");
        params.getTargetEntityIds().put(Step2.TARGET_MODEL_INVARIANT_ID, "vf-count-invariant");
        params.getTargetEntityIds().put(Step2.TARGET_MODEL_VERSION_ID, "vf-count-version");
        when(aaicq.getVfModuleCount("vf-count-customization", "vf-count-invariant", "vf-count-version")).thenReturn(11);
        when(policyOperation.getPropertyNames()).thenReturn(List.of(OperationProperties.DATA_VF_COUNT));

        step.setProperties();
        verify(policyOperation).setProperty(OperationProperties.DATA_VF_COUNT, 11);

        // missing model version id
        params.getTargetEntityIds().put(Step2.TARGET_MODEL_VERSION_ID, null);
        assertThatIllegalArgumentException().isThrownBy(() -> step.setProperties())
                        .withMessageContaining("missing target modelVersionId");

        // missing model invariant id
        params.getTargetEntityIds().put(Step2.TARGET_MODEL_INVARIANT_ID, null);
        assertThatIllegalArgumentException().isThrownBy(() -> step.setProperties())
                        .withMessageContaining("missing target modelInvariantId");

        // missing model customization id
        params.getTargetEntityIds().put(Step2.TARGET_MODEL_CUSTOMIZATION_ID, null);
        assertThatIllegalArgumentException().isThrownBy(() -> step.setProperties())
                        .withMessageContaining("missing target modelCustomizationId");

        // missing target
        params = params.toBuilder().targetEntityIds(null).build();
        step = new Step2(stepContext, params, event);
        step.init();
        assertThatIllegalArgumentException().isThrownBy(() -> step.setProperties())
                        .withMessageContaining(Step2.TARGET_INFO_MSG);

        // get it from the step context
        when(stepContext.contains(OperationProperties.DATA_VF_COUNT)).thenReturn(true);
        when(stepContext.getProperty(OperationProperties.DATA_VF_COUNT)).thenReturn(22);
        step.setProperties();
        verify(policyOperation).setProperty(OperationProperties.DATA_VF_COUNT, 22);
    }

    @Test
    void testLoadEnrichment_testGetEnrichment() {
        event.setAai(Map.of("bandwidth", "bandwidth-value"));
        when(policyOperation.getPropertyNames()).thenReturn(List.of(OperationProperties.ENRICHMENT_BANDWIDTH));

        step.setProperties();
        verify(policyOperation).setProperty(OperationProperties.ENRICHMENT_BANDWIDTH, "bandwidth-value");

        // missing enrichment data
        event.setAai(Map.of());
        assertThatIllegalArgumentException().isThrownBy(() -> step.setProperties());
    }

    @Test
    void testLoadAdditionalEventParams_testGetAdditionalEventParams() {
        Map<String, String> data = Map.of("addA", "add-valueA", "addB", "add-valueB");
        event.setAdditionalEventParams(data);
        when(policyOperation.getPropertyNames()).thenReturn(List.of(OperationProperties.EVENT_ADDITIONAL_PARAMS));

        step.setProperties();
        verify(policyOperation).setProperty(OperationProperties.EVENT_ADDITIONAL_PARAMS, data);
    }

    @Test
    void testLoadEventPayload_testGetEventPayload() {
        event.setPayload("some-event-payload");
        when(policyOperation.getPropertyNames()).thenReturn(List.of(OperationProperties.EVENT_PAYLOAD));

        step.setProperties();
        verify(policyOperation).setProperty(OperationProperties.EVENT_PAYLOAD, "some-event-payload");
    }

    @Test
    void testLoadOptCdsGrpcAaiProperties() {
        when(policyOperation.getPropertyNames()).thenReturn(List.of(OperationProperties.OPT_CDS_GRPC_AAI_PROPERTIES));

        step.setProperties();
        verify(policyOperation, never()).setProperty(any(), anyString());
    }

    @Test
    void testLoadDefaultGenericVnf_testGetDefaultGenericVnf() {
        var data = new GenericVnf();
        when(aaicq.getDefaultGenericVnf()).thenReturn(data);
        when(policyOperation.getPropertyNames()).thenReturn(List.of(UsecasesConstants.AAI_DEFAULT_GENERIC_VNF));

        step.setProperties();
        verify(policyOperation).setProperty(UsecasesConstants.AAI_DEFAULT_GENERIC_VNF, data);

        when(aaicq.getDefaultGenericVnf()).thenReturn(null);
        assertThatIllegalArgumentException().isThrownBy(() -> step.getDefaultGenericVnf())
                        .withMessageContaining("missing generic VNF in A&AI response");
    }

    @Test
    void testGetCustomQueryData() {
        assertSame(aaicq, step.getCustomQueryData());

        when(stepContext.getProperty(AaiCqResponse.CONTEXT_KEY)).thenReturn(null);

        assertThatIllegalArgumentException().isThrownBy(() -> step.getCustomQueryData())
                        .withMessage("missing custom query data for my-actor.my-operation");
    }

    @Test
    void testVerifyNotNull() {
        assertThatCode(() -> step.verifyNotNull("verifyA", "verify-value-A")).doesNotThrowAnyException();

        assertThatIllegalArgumentException().isThrownBy(() -> step.verifyNotNull("verifyB", null))
                        .withMessage("missing verifyB for my-actor.my-operation");
    }

    @Test
    void testStripPrefix() {
        assertEquals(NO_SLASH, Step2.stripPrefix(NO_SLASH, 0));
        assertEquals(NO_SLASH, Step2.stripPrefix(NO_SLASH, 1));
        assertEquals(NO_SLASH, Step2.stripPrefix(NO_SLASH, 2));

        assertEquals(ONE_SLASH, Step2.stripPrefix(ONE_SLASH, 1));
        assertEquals(ONE_SLASH, Step2.stripPrefix(ONE_SLASH, 2));

        assertEquals("/slashes", Step2.stripPrefix("/two/slashes", 2));
        assertEquals("/slashes", Step2.stripPrefix("/two/slashes", 3));

        assertEquals("/and/more", Step2.stripPrefix("/three/slashes/and/more", 3));

        assertEquals("/and/more", Step2.stripPrefix("prefix/three/slashes/and/more", 3));
    }
}
