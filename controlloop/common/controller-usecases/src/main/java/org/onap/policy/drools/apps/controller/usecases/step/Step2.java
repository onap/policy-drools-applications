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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import org.apache.commons.lang3.StringUtils;
import org.onap.aai.domain.yang.CloudRegion;
import org.onap.aai.domain.yang.GenericVnf;
import org.onap.aai.domain.yang.ModelVer;
import org.onap.aai.domain.yang.ServiceInstance;
import org.onap.aai.domain.yang.Tenant;
import org.onap.policy.aai.AaiCqResponse;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actor.aai.AaiGetPnfOperation;
import org.onap.policy.controlloop.actor.aai.AaiGetTenantOperation;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.OperationProperties;
import org.onap.policy.controlloop.actorserviceprovider.controlloop.ControlLoopEventContext;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.eventmanager.Step;
import org.onap.policy.controlloop.eventmanager.StepContext;
import org.onap.policy.drools.apps.controller.usecases.UsecasesConstants;

public class Step2 extends Step {

    private static final String TARGET_INFO_MSG = "Target information";
    public static final String ENRICHMENT_PREFIX = "enrichment/";
    public static final String VSERVER_VSERVER_NAME = "vserver.vserver-name";
    public static final String RESOURCE_LINK = "resource-link";
    public static final String RESULT_DATA = "result-data";

    private static final Map<String, BiConsumer<Step2, String>> PROPERTY_LOADER;

    static {
        Map<String, BiConsumer<Step2, String>> map = new HashMap<>();

        map.put(OperationProperties.AAI_DEFAULT_CLOUD_REGION, Step2::loadCloudRegion);
        map.put(OperationProperties.AAI_DEFAULT_TENANT, Step2::loadTenant);
        map.put(OperationProperties.AAI_PNF, Step2::loadPnf);
        map.put(OperationProperties.AAI_RESOURCE_VNF, Step2::loadResourceVnf);
        map.put(OperationProperties.AAI_SERVICE, Step2::loadService);
        map.put(OperationProperties.AAI_SERVICE_MODEL, Step2::loadServiceModel);
        map.put(OperationProperties.AAI_VNF, Step2::loadVnf);
        map.put(OperationProperties.AAI_VNF_MODEL, Step2::loadVnfModel);
        map.put(OperationProperties.AAI_VSERVER_LINK, Step2::loadVserverLink);
        map.put(OperationProperties.DATA_VF_COUNT, Step2::loadVfCount);
        map.put(OperationProperties.EVENT_ADDITIONAL_PARAMS, Step2::loadAdditionalEventParams);
        map.put(OperationProperties.EVENT_PAYLOAD, Step2::loadEventPayload);
        map.put(OperationProperties.OPT_CDS_GRPC_AAI_PROPERTIES, Step2::loadOptCdsGrpcAaiProperties);

        map.put(UsecasesConstants.AAI_DEFAULT_GENERIC_VNF, Step2::loadDefaultGenericVnf);

        PROPERTY_LOADER = Collections.unmodifiableMap(map);
    }


    protected final StepContext usecasesContext;
    protected final VirtualControlLoopEvent event;


    /**
     * Constructs the object.
     *
     * @param params operation parameters
     */
    public Step2(StepContext usecasesContext, ControlLoopOperationParams params, VirtualControlLoopEvent event) {
        super(params, new AtomicReference<>());
        this.usecasesContext = usecasesContext;
        this.event = event;
    }

    /**
     * Constructs the object using information from another step.
     *
     * @param otherStep step whose information should be used
     * @param actor actor name
     * @param operation operation name
     */
    public Step2(Step2 otherStep, String actor, String operation) {
        super(otherStep, actor, operation);
        this.usecasesContext = otherStep.usecasesContext;
        this.event = otherStep.event;
    }

    /**
     * Inidicates that the step succeeded with the given outcome. Invoked by the rules.
     * The default method does nothing.
     *
     * @param outcome operation's outcome
     */
    public void success(OperationOutcome outcome) {
        // do nothing
    }

    /**
     * Gets the names of the properties required by the operation.
     *
     * @return the names of the properties required by the operation
     */
    public List<String> getPropertyNames() {
        return getOperation().getPropertyNames();
    }

    /**
     * Sets the operation's properties. This is invoked <i>after</i> any preprocessor
     * steps have been performed. The default method does nothing.
     */
    public void setProperties() {
        for (String propName : getPropertyNames()) {
            if (propName.startsWith(ENRICHMENT_PREFIX)) {
                loadEnrichment(propName);
                continue;
            }

            BiConsumer<Step2, String> loader = PROPERTY_LOADER.get(propName);
            if (loader == null) {
                throw new IllegalArgumentException("unknown property " + propName + " needed by " + getActorName() + "."
                                + getOperationName());
            }

            loader.accept(this, propName);
        }
    }

    protected void loadCloudRegion(String propName) {
        getOperation().setProperty(propName, getCloudRegion());
    }

    protected void loadTenant(String propName) {
        getOperation().setProperty(propName, getTenant());
    }

    protected void loadPnf(String propName) {
        getOperation().setProperty(propName, getPnf());
    }

    protected void loadResourceVnf(String propName) {
        getOperation().setProperty(propName, getResourceVnf());
    }

    protected void loadService(String propName) {
        getOperation().setProperty(propName, getService());
    }

    protected void loadServiceModel(String propName) {
        getOperation().setProperty(propName, getServiceModel());
    }

    protected void loadVnf(String propName) {
        getOperation().setProperty(propName, getVnf());
    }

    protected void loadVnfModel(String propName) {
        getOperation().setProperty(propName, getVnfModel());
    }

    protected void loadVserverLink(String propName) {
        getOperation().setProperty(propName, getVserverLink());
    }

    protected void loadVfCount(String propName) {
        getOperation().setProperty(propName, getVfCount());
    }

    protected void loadEnrichment(String propName) {
        getOperation().setProperty(propName, getEnrichment(propName));
    }

    protected void loadAdditionalEventParams(String propName) {
        getOperation().setProperty(propName, getAdditionalEventParams());
    }

    protected void loadEventPayload(String propName) {
        getOperation().setProperty(propName, getEventPayload());
    }

    protected void loadOptCdsGrpcAaiProperties(String propName) {
        // do nothing
    }

    protected void loadDefaultGenericVnf(String propName) {
        getOperation().setProperty(propName, getDefaultGenericVnf());
    }

    protected CloudRegion getCloudRegion() {
        AaiCqResponse aaicq = usecasesContext.getProperty(AaiCqResponse.CONTEXT_KEY);
        return aaicq.getDefaultCloudRegion();
    }

    protected Tenant getTenant() {
        AaiCqResponse aaicq = usecasesContext.getProperty(AaiCqResponse.CONTEXT_KEY);
        return aaicq.getDefaultTenant();
    }

    protected StandardCoderObject getPnf() {
        return usecasesContext.getProperty(AaiGetPnfOperation.getKey(params.getTargetEntity()));
    }

    protected GenericVnf getResourceVnf() {
        verifyNotNull(TARGET_INFO_MSG, params.getTarget());

        String resourceId = params.getTarget().getResourceID();

        verifyNotNull("Target resource ID", resourceId);

        AaiCqResponse aaicq = usecasesContext.getProperty(AaiCqResponse.CONTEXT_KEY);
        return aaicq.getGenericVnfByModelInvariantId(resourceId);
    }

    protected ServiceInstance getService() {
        AaiCqResponse aaicq = usecasesContext.getProperty(AaiCqResponse.CONTEXT_KEY);
        return aaicq.getServiceInstance();
    }

    protected ModelVer getServiceModel() {
        AaiCqResponse aaicq = usecasesContext.getProperty(AaiCqResponse.CONTEXT_KEY);
        ServiceInstance service = aaicq.getServiceInstance();
        return aaicq.getModelVerByVersionId(service.getModelVersionId());
    }

    protected GenericVnf getVnf() {
        verifyNotNull(TARGET_INFO_MSG, params.getTarget());

        String modelInvariantId = params.getTarget().getModelInvariantId();

        verifyNotNull("modelInvariantId", modelInvariantId);

        AaiCqResponse aaicq = usecasesContext.getProperty(AaiCqResponse.CONTEXT_KEY);
        return aaicq.getGenericVnfByVfModuleModelInvariantId(modelInvariantId);
    }

    protected ModelVer getVnfModel() {
        GenericVnf vnf = getVnf();

        AaiCqResponse aaicq = usecasesContext.getProperty(AaiCqResponse.CONTEXT_KEY);
        return aaicq.getModelVerByVersionId(vnf.getModelVersionId());
    }

    protected String getVserverLink() {
        ControlLoopEventContext context = params.getContext();

        String vserver = context.getEnrichment().get(VSERVER_VSERVER_NAME);
        if (StringUtils.isBlank(vserver)) {
            throw new IllegalArgumentException("missing " + VSERVER_VSERVER_NAME + " in enrichment data");
        }

        StandardCoderObject tenant = context.getProperty(AaiGetTenantOperation.getKey(vserver));
        if (tenant == null) {
            throw new IllegalStateException("cannot determine resource-link");
        }

        String resourceLink = tenant.getString(RESULT_DATA, 0, RESOURCE_LINK);
        if (resourceLink == null) {
            throw new IllegalArgumentException("missing resource-link in tenant data");
        }

        return stripPrefix(resourceLink, 3);
    }

    protected static String stripPrefix(String resourceLink, int ncomponents) {
        int previdx = 0;
        for (int nslashes = 0; nslashes < ncomponents; ++nslashes) {
            int idx = resourceLink.indexOf('/');
            if (idx < 0) {
                break;
            }

            previdx = idx;
        }

        return resourceLink.substring(previdx);
    }

    protected Integer getVfCount() {
        if (usecasesContext.contains(OperationProperties.DATA_VF_COUNT)) {
            return usecasesContext.getProperty(OperationProperties.DATA_VF_COUNT);
        }

        verifyNotNull(TARGET_INFO_MSG, params.getTarget());

        String modelCustomizationId = params.getTarget().getModelCustomizationId();
        String modelInvariantId = params.getTarget().getModelInvariantId();
        String modelVersionId = params.getTarget().getModelVersionId();

        verifyNotNull("modelCustomizationId", modelCustomizationId);
        verifyNotNull("modelInvariantId", modelInvariantId);
        verifyNotNull("modelVersionId", modelVersionId);

        AaiCqResponse aaicq = usecasesContext.getProperty(AaiCqResponse.CONTEXT_KEY);
        return aaicq.getVfModuleCount(modelCustomizationId, modelInvariantId, modelVersionId);
    }

    protected String getEnrichment(String propName) {
        String enrichmentKey = propName.substring(ENRICHMENT_PREFIX.length());
        String value = event.getAai().get(enrichmentKey);
        verifyNotNull(propName, value);

        return value;
    }

    protected Map<String, String> getAdditionalEventParams() {
        return event.getAdditionalEventParams();
    }

    protected String getEventPayload() {
        return event.getPayload();
    }

    protected GenericVnf getDefaultGenericVnf() {
        AaiCqResponse aaicq = usecasesContext.getProperty(AaiCqResponse.CONTEXT_KEY);
        return aaicq.getDefaultGenericVnf();
    }

    protected void verifyNotNull(String propName, Object value) {
        if (value == null) {
            throw new IllegalArgumentException(
                            "missing " + propName + " for " + getActorName() + "." + getOperationName());
        }
    }
}
