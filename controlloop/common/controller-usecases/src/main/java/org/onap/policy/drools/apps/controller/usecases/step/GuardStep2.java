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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.onap.policy.controlloop.actor.guard.DecisionOperation;
import org.onap.policy.controlloop.actor.guard.GuardActor;
import org.onap.policy.controlloop.actor.so.VfModuleCreate;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.OperationProperties;
import org.onap.policy.controlloop.actorserviceprovider.TargetType;
import org.onap.policy.drools.apps.controller.usecases.UsecasesConstants;

/**
 * Wrapper for a Guard operation. Note: this makes a clone of the operation parameters,
 * replacing the payload. It overrides the operation's property names with that are
 * relevant for guards. In addition, it overrides the relevant loadXxx() methods to load
 * the data into the payload instead of into the operation's properties. It also
 * increments or decrements the VF Count, depending whether the operation is a "VF Module
 * Create" or not.
 */
public class GuardStep2 extends Step2 {
    public static final String PAYLOAD_KEY_TARGET_ENTITY = "target";
    public static final String PAYLOAD_KEY_VF_COUNT = "vfCount";
    public static final String PAYLOAD_KEY_VNF_NAME = "generic-vnf.vnf-name";
    public static final String PAYLOAD_KEY_VNF_ID = "generic-vnf.vnf-id";
    public static final String PAYLOAD_KEY_VNF_TYPE = "generic-vnf.vnf-type";
    public static final String PAYLOAD_KEY_NF_NAMING_CODE = "generic-vnf.nf-naming-code";
    public static final String PAYLOAD_KEY_VSERVER_ID = "vserver.vserver-id";
    public static final String PAYLOAD_KEY_CLOUD_REGION_ID = "cloud-region.cloud-region-id";

    private final Operation policyOper;


    /**
     * Constructs the object using information from another step.
     *
     * @param otherStep step whose information should be used
     */
    public GuardStep2(Step2 otherStep, String closedLoopControlName) {
        super(otherStep, GuardActor.NAME, DecisionOperation.NAME);

        if (!otherStep.isInitialized()) {
            throw new IllegalStateException("policy operation must be initialized before the guard operation");
        }

        this.policyOper = otherStep.getOperation();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("actor", otherStep.getActorName());
        payload.put("operation", otherStep.getOperationName());
        payload.put("requestId", params.getRequestId());
        payload.put("clname", closedLoopControlName);

        params = params.toBuilder().payload(payload).build();
    }

    @Override
    public boolean acceptsEvent() {
        return true;
    }

    /**
     * Builds the list of properties on the policy's actual operation.
     */
    @Override
    public List<String> getPropertyNames() {
        List<String> names = new ArrayList<>(1);

        // include VF Count if the policy's operation needs it
        if (policyOper.getPropertyNames().contains(OperationProperties.DATA_VF_COUNT)) {
            names.add(OperationProperties.DATA_VF_COUNT);
        }

        if (! TargetType.PNF.equals(params.getTargetType())) {
            names.add(UsecasesConstants.AAI_DEFAULT_GENERIC_VNF);
            names.add(OperationProperties.AAI_DEFAULT_CLOUD_REGION);
        }

        return names;
    }

    /**
     * Load the target entity into the payload instead of the operation's properties.
     */
    @Override
    protected void loadTargetEntity(String propName) {
        params.getPayload().put(PAYLOAD_KEY_TARGET_ENTITY, getTargetEntity());
    }

    /**
     * Load the VF Count into the payload instead of the operation's properties.
     * Increments the count for "VF Module Create". Decrements it otherwise.
     */
    @Override
    protected void loadVfCount(String propName) {
        // run guard with the proposed VF count
        int count = getVfCount();
        if (VfModuleCreate.NAME.equals(policyOper.getName())) {
            ++count;
        } else {
            --count;
        }

        params.getPayload().put(PAYLOAD_KEY_VF_COUNT, count);
    }

    @Override
    protected void loadCloudRegion(String propName) {
        // PNF does not support guard filters
        if (TargetType.PNF.equals(params.getTargetType())) {
            return;
        }

        params.getPayload().put(PAYLOAD_KEY_CLOUD_REGION_ID, getCloudRegion().getCloudRegionId());
    }

    @Override
    protected void loadDefaultGenericVnf(String propName) {
        // PNF does not support guard filters
        if (TargetType.PNF.equals(params.getTargetType())) {
            return;
        }

        // add in properties needed for filters
        params.getPayload().put(PAYLOAD_KEY_VNF_ID, getTargetEntity());
        params.getPayload().put(PAYLOAD_KEY_VNF_NAME, getDefaultGenericVnf().getVnfName());
        params.getPayload().put(PAYLOAD_KEY_VNF_TYPE, getDefaultGenericVnf().getVnfType());
        params.getPayload().put(PAYLOAD_KEY_NF_NAMING_CODE, getDefaultGenericVnf().getNfNamingCode());
    }

    @Override
    public void setProperties() {
        // Ensure the properties are set
        super.setProperties();

        // PNF does not support guard filters
        if (TargetType.PNF.equals(params.getTargetType())) {
            return;
        }

        params.getPayload().put(PAYLOAD_KEY_VSERVER_ID, getEnrichment(OperationProperties.ENRICHMENT_VSERVER_ID));
    }

}
