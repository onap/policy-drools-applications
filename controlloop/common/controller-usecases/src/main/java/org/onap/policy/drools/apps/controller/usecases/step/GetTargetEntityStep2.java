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

import static org.onap.policy.drools.apps.controller.usecases.UsecasesConstants.AAI_DEFAULT_GENERIC_VNF;
import static org.onap.policy.drools.apps.controller.usecases.UsecasesConstants.GENERIC_VNF_VNF_ID;
import static org.onap.policy.drools.apps.controller.usecases.UsecasesConstants.GENERIC_VNF_VNF_NAME;
import static org.onap.policy.drools.apps.controller.usecases.UsecasesConstants.PNF_NAME;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.onap.aai.domain.yang.GenericVnf;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.impl.OperationPartial;
import org.onap.policy.controlloop.policy.Target;
import org.onap.policy.drools.apps.controller.usecases.UsecasesConstants;

/**
 * Step for the pseudo operation to get the target entity.
 */
public class GetTargetEntityStep2 extends Step2 {

    /**
     * Value to be populated by the operation.
     */
    private final AtomicReference<String> targetEntity;

    /**
     * Constructs the object using information from another step.
     *
     * @param otherStep step whose information should be used
     * @param targetEntity where the target entity should be stored once it is determined
     */
    public GetTargetEntityStep2(Step2 otherStep, AtomicReference<String> targetEntity) {
        super(otherStep, UsecasesConstants.GET_TARGET_ENTITY_ACTOR, UsecasesConstants.GET_TARGET_ENTITY_OPERATION);
        this.targetEntity = targetEntity;
    }

    @Override
    protected Operation buildOperation() {
        /*
         * Return a pseudo operation that will update the target entity when the VNF is
         * provided. Note: start() should never be invoked on this operation.
         */
        return new OperationPartial(params, null) {
            @Override
            public void setProperty(String name, Object value) {
                // only care about one property
                if (!UsecasesConstants.AAI_DEFAULT_GENERIC_VNF.equals(name)) {
                    return;
                }

                GenericVnf vnf = (GenericVnf) value;
                targetEntity.set(vnf.getVnfId());
            }
        };
    }

    /**
     * Verifies that the target entity has been set and returns {@code false}, causing the
     * rules to discard the operation.
     */
    @Override
    public boolean start(long remainingMs) {
        if (targetEntity.get() == null) {
            throw new IllegalStateException("Target Entity has not been determined yet");
        }

        return false;
    }

    @Override
    public List<String> getPropertyNames() {
        String propName = detmTarget(params.getTarget());
        return (propName == null ? Collections.emptyList() : List.of(propName));
    }

    /**
     * Determines the target entity.
     *
     * @param target policy target
     *
     * @return the property containing the target entity, or {@code null} if the target
     *         entity is already known
     */
    private String detmTarget(Target target) {
        if (targetEntity.get() != null) {
            // the target entity has already been determined
            return null;
        }

        if (target == null) {
            throw new IllegalArgumentException("The target is null");
        }

        if (target.getType() == null) {
            throw new IllegalArgumentException("The target type is null");
        }

        switch (target.getType()) {
            case PNF:
                return detmPnfTarget();
            case VM:
            case VNF:
            case VFMODULE:
                return detmVfModuleTarget();
            default:
                throw new IllegalArgumentException("The target type is not supported");
        }
    }

    /**
     * Determines the PNF target entity.
     *
     * @return the property containing the target entity, or {@code null} if the target
     *         entity is already known
     */
    private String detmPnfTarget() {
        if (!PNF_NAME.equalsIgnoreCase(event.getTarget())) {
            throw new IllegalArgumentException("Target does not match target type");
        }

        targetEntity.set(event.getAai().get(PNF_NAME));
        if (targetEntity.get() == null) {
            throw new IllegalArgumentException("AAI section is missing " + PNF_NAME);
        }

        return null;
    }

    /**
     * Determines the VF Module target entity.
     *
     * @return the property containing the target entity, or {@code null} if the target
     *         entity is already known
     */
    private String detmVfModuleTarget() {
        String targetFieldName = event.getTarget();
        if (targetFieldName == null) {
            throw new IllegalArgumentException("Target is null");
        }

        switch (targetFieldName.toLowerCase()) {
            case VSERVER_VSERVER_NAME:
                targetEntity.set(event.getAai().get(VSERVER_VSERVER_NAME));
                break;
            case GENERIC_VNF_VNF_ID:
                targetEntity.set(event.getAai().get(GENERIC_VNF_VNF_ID));
                break;
            case GENERIC_VNF_VNF_NAME:
                return detmVnfName();
            default:
                throw new IllegalArgumentException("Target does not match target type");
        }

        if (targetEntity.get() == null) {
            throw new IllegalArgumentException("Enrichment data is missing " + targetFieldName);
        }

        return null;
    }

    /**
     * Determines the VNF Name target entity.
     *
     * @return the property containing the target entity, or {@code null} if the target
     *         entity is already known
     */
    private String detmVnfName() {
        // if the onset is enriched with the vnf-id, we don't need an A&AI response
        targetEntity.set(event.getAai().get(GENERIC_VNF_VNF_ID));
        if (targetEntity.get() != null) {
            return null;
        }

        // don't have the data yet - add a step to retrieve it
        return AAI_DEFAULT_GENERIC_VNF;
    }
}
