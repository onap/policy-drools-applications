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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
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
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.eventmanager.Step;
import org.onap.policy.controlloop.eventmanager.StepContext;
import org.onap.policy.drools.apps.controller.usecases.UsecasesConstants;

/**
 * Steps specific to the usecases controller. The {@link #setProperties()} method is used
 * to load the various properties into the operation, extracting enrichment data where
 * appropriate, and extracting other data from the step's context. For each property,
 * there is a getXxx() method for extracting the value and a loadXxx() method for loading
 * the extracted value into the operation. In addition, the
 * {@link #success(OperationOutcome)} method is responsible for extracting responses from
 * an operation outcome and recording the data in the step's context for use by subsequent
 * steps.
 */
public class Step2 extends Step {
    public static final String TARGET_INFO_MSG = "Target information";
    public static final String ENRICHMENT_PREFIX = "enrichment/";
    public static final String VSERVER_VSERVER_NAME = "vserver.vserver-name";
    public static final String RESOURCE_LINK = "resource-link";
    public static final String RESULT_DATA = "result-data";

    private static final Map<String, BiConsumer<Step2, String>> PROPERTY_LOADER;
    private static final Map<String, Consumer<Step2>> PROPERTY_SAVER;

    static {
        /*
         * Populate map for PROPERTY_LOADER.
         */
        Map<String, BiConsumer<Step2, String>> map = new HashMap<>();

        map.put(OperationProperties.AAI_DEFAULT_CLOUD_REGION, Step2::loadCloudRegion);
        map.put(OperationProperties.AAI_DEFAULT_TENANT, Step2::loadTenant);
        map.put(OperationProperties.AAI_PNF, Step2::loadPnf);
        map.put(OperationProperties.AAI_RESOURCE_VNF, Step2::loadResourceVnf);
        map.put(OperationProperties.AAI_SERVICE, Step2::loadService);
        map.put(OperationProperties.AAI_SERVICE_MODEL, Step2::loadServiceModel);
        map.put(OperationProperties.AAI_TARGET_ENTITY, Step2::loadTargetEntity);
        map.put(OperationProperties.AAI_VNF, Step2::loadVnf);
        map.put(OperationProperties.AAI_VNF_MODEL, Step2::loadVnfModel);
        map.put(OperationProperties.AAI_VSERVER_LINK, Step2::loadVserverLink);
        map.put(OperationProperties.DATA_VF_COUNT, Step2::loadVfCount);
        map.put(OperationProperties.EVENT_ADDITIONAL_PARAMS, Step2::loadAdditionalEventParams);
        map.put(OperationProperties.EVENT_PAYLOAD, Step2::loadEventPayload);
        map.put(OperationProperties.OPT_CDS_GRPC_AAI_PROPERTIES, Step2::loadOptCdsGrpcAaiProperties);

        map.put(UsecasesConstants.AAI_DEFAULT_GENERIC_VNF, Step2::loadDefaultGenericVnf);

        PROPERTY_LOADER = Collections.unmodifiableMap(map);


        /*
         * Populate map for PROPERTY_SAVER.
         */
        Map<String, Consumer<Step2>> map2 = new HashMap<>();

        map2.put(OperationProperties.DATA_VF_COUNT, Step2::storeVfCount);

        PROPERTY_SAVER = Collections.unmodifiableMap(map2);
    }


    protected final StepContext stepContext;
    protected final VirtualControlLoopEvent event;

    /**
     * {@code True} if the associated preprocessing steps have been loaded, {@code false}
     * otherwise.
     */
    @Getter
    @Setter
    private boolean preprocessed;

    /**
     * Actions to take to store the Operation's properties back into the context.
     */
    private List<Consumer<Step2>> postProcessors = new LinkedList<>();


    /**
     * Constructs the object. This is used when constructing the step for the policy's
     * actual operation.
     *
     * @param stepContext the step's context
     * @param params operation parameters
     * @param event the event being processed
     */
    public Step2(StepContext stepContext, ControlLoopOperationParams params, VirtualControlLoopEvent event) {
        super(params, new AtomicReference<>());
        this.stepContext = stepContext;
        this.event = event;
    }

    /**
     * Constructs the object using information from another step. This is used when
     * constructing a preprocessing step.
     *
     * @param otherStep step whose information should be used
     * @param actor actor name
     * @param operation operation name
     */
    public Step2(Step2 otherStep, String actor, String operation) {
        super(otherStep, actor, operation);
        this.stepContext = otherStep.stepContext;
        this.event = otherStep.event;
    }

    /**
     * Determines if starting this step indicates acceptance of the event. The default
     * method simply invokes {@link #isPolicyStep()}.
     *
     * @return {@code true} if this step accepts the event, {@code false} if it is still
     *         indeterminate
     */
    public boolean acceptsEvent() {
        return isPolicyStep();
    }

    /**
     * Indicates that the step succeeded with the given outcome. Invoked by the rules. The
     * default method invokes the post processors.
     *
     * @param outcome operation's outcome
     */
    public void success(OperationOutcome outcome) {
        for (Consumer<Step2> proc : postProcessors) {
            proc.accept(this);
        }
    }

    /**
     * Gets the names of the properties required by the operation. The default method just
     * delegates to the operation to identify the properties.
     *
     * @return the names of the properties required by the operation
     */
    public List<String> getPropertyNames() {
        return getOperation().getPropertyNames();
    }

    /**
     * Sets the operation's properties. This is invoked <i>after</i> any preprocessor
     * steps have been performed. It also adds items to {@link #postProcessors}.
     */
    public void setProperties() {
        postProcessors.clear();

        for (String propName : getPropertyNames()) {
            // identify the saver, if any
            Consumer<Step2> saver = PROPERTY_SAVER.get(propName);
            if (saver != null) {
                postProcessors.add(saver);
            }


            // load data
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

    protected void loadTargetEntity(String propName) {
        getOperation().setProperty(propName, getTargetEntity());
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
        AaiCqResponse aaicq = getCustomQueryData();
        return aaicq.getDefaultCloudRegion();
    }

    protected Tenant getTenant() {
        AaiCqResponse aaicq = getCustomQueryData();
        return aaicq.getDefaultTenant();
    }

    protected StandardCoderObject getPnf() {
        return stepContext.getProperty(AaiGetPnfOperation.getKey(getTargetEntity()));
    }

    protected GenericVnf getResourceVnf() {
        verifyNotNull(TARGET_INFO_MSG, params.getTarget());

        String resourceId = params.getTarget().getResourceID();

        verifyNotNull("Target resource ID", resourceId);

        AaiCqResponse aaicq = getCustomQueryData();
        return aaicq.getGenericVnfByModelInvariantId(resourceId);
    }

    protected ServiceInstance getService() {
        AaiCqResponse aaicq = getCustomQueryData();
        return aaicq.getServiceInstance();
    }

    protected ModelVer getServiceModel() {
        AaiCqResponse aaicq = getCustomQueryData();
        ServiceInstance service = aaicq.getServiceInstance();
        return aaicq.getModelVerByVersionId(service.getModelVersionId());
    }

    /**
     * The default method assumes there is only one target entity and that it's stored
     * within the step's context.
     */
    protected String getTargetEntity() {
        return stepContext.getProperty(OperationProperties.AAI_TARGET_ENTITY);
    }

    protected GenericVnf getVnf() {
        verifyNotNull(TARGET_INFO_MSG, params.getTarget());

        String modelInvariantId = params.getTarget().getModelInvariantId();

        verifyNotNull("modelInvariantId", modelInvariantId);

        AaiCqResponse aaicq = getCustomQueryData();
        return aaicq.getGenericVnfByVfModuleModelInvariantId(modelInvariantId);
    }

    protected ModelVer getVnfModel() {
        GenericVnf vnf = getVnf();
        AaiCqResponse aaicq = getCustomQueryData();
        return aaicq.getModelVerByVersionId(vnf.getModelVersionId());
    }

    protected String getVserverLink() {
        String vserver = event.getAai().get(VSERVER_VSERVER_NAME);
        if (StringUtils.isBlank(vserver)) {
            throw new IllegalArgumentException("missing " + VSERVER_VSERVER_NAME + " in enrichment data");
        }

        StandardCoderObject tenant = stepContext.getProperty(AaiGetTenantOperation.getKey(vserver));
        verifyNotNull("tenant data", tenant);

        String resourceLink = tenant.getString(RESULT_DATA, 0, RESOURCE_LINK);
        verifyNotNull("tenant data resource-link", resourceLink);

        return stripPrefix(resourceLink, 3);
    }

    protected Integer getVfCount() {
        if (stepContext.contains(OperationProperties.DATA_VF_COUNT)) {
            return stepContext.getProperty(OperationProperties.DATA_VF_COUNT);
        }

        verifyNotNull(TARGET_INFO_MSG, params.getTarget());

        String modelCustomizationId = params.getTarget().getModelCustomizationId();
        String modelInvariantId = params.getTarget().getModelInvariantId();
        String modelVersionId = params.getTarget().getModelVersionId();

        verifyNotNull("target modelCustomizationId", modelCustomizationId);
        verifyNotNull("target modelInvariantId", modelInvariantId);
        verifyNotNull("target modelVersionId", modelVersionId);

        AaiCqResponse aaicq = getCustomQueryData();
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
        AaiCqResponse aaicq = getCustomQueryData();
        return aaicq.getDefaultGenericVnf();
    }

    protected AaiCqResponse getCustomQueryData() {
        AaiCqResponse aaicq = stepContext.getProperty(AaiCqResponse.CONTEXT_KEY);
        verifyNotNull("custom query data", aaicq);

        return aaicq;
    }

    protected void storeVfCount() {
        if (!getOperation().containsProperty(OperationProperties.DATA_VF_COUNT)) {
            return;
        }

        int vfcount = getOperation().getProperty(OperationProperties.DATA_VF_COUNT);
        stepContext.setProperty(OperationProperties.DATA_VF_COUNT, vfcount);
    }

    protected void verifyNotNull(String propName, Object value) {
        if (value == null) {
            throw new IllegalArgumentException(
                            "missing " + propName + " for " + getActorName() + "." + getOperationName());
        }
    }

    protected static String stripPrefix(String resourceLink, int ncomponents) {
        int previdx = -1;
        for (int nslashes = 0; nslashes < ncomponents; ++nslashes) {
            int idx = resourceLink.indexOf('/', previdx + 1);
            if (idx < 0) {
                break;
            }

            previdx = idx;
        }

        return resourceLink.substring(Math.max(0, previdx));
    }
}
