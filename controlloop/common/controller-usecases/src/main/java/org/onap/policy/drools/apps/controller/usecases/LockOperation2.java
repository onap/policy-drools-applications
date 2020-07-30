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

import static org.onap.policy.drools.apps.controller.usecases.UsecasesConstants.AAI_DEFAULT_GENERIC_VNF;
import static org.onap.policy.drools.apps.controller.usecases.UsecasesConstants.GENERIC_VNF_VNF_ID;
import static org.onap.policy.drools.apps.controller.usecases.UsecasesConstants.GENERIC_VNF_VNF_NAME;
import static org.onap.policy.drools.apps.controller.usecases.UsecasesConstants.PNF_NAME;
import static org.onap.policy.drools.apps.controller.usecases.UsecasesConstants.VSERVER_VSERVER_NAME;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import org.onap.aai.domain.yang.GenericVnf;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.eventmanager.StepContext;
import org.onap.policy.controlloop.policy.Target;

/**
 * A LOCK operation for a particular target entity. This is a "pseudo" operation; it is
 * not found within an ActorService, but is created directly.
 */
public class LockOperation2 implements Operation {

    private final StepContext stepContext;
    private final ControlLoopOperationParams params;
    private final VirtualControlLoopEvent event;

    /**
     * This is populated on the first call to {@link #getPropertyNames()}, if it is known
     * at that time. Otherwise, it is populated later when
     * {@link #setProperty(String, Object)} is called.
     */
    @Getter
    private String targetEntity;


    /**
     * Constructs the lock operation as a preprocessing step for a policy's operation.
     *
     * @param stepContext the context to use to request the lock
     * @param event event being processed
     * @param params operation's parameters
     */
    public LockOperation2(StepContext stepContext, VirtualControlLoopEvent event, ControlLoopOperationParams params) {
        this.stepContext = stepContext;
        this.event = event;
        this.params = params;
    }

    @Override
    public List<String> getPropertyNames() {
        String propName = detmTarget(params.getTarget());
        return (propName == null ? Collections.emptyList() : List.of(propName));
    }

    @Override
    public CompletableFuture<OperationOutcome> start() {
        if (detmTarget(params.getTarget()) != null) {
            throw new IllegalStateException("target lock entity has not been determined yet");
        }

        // TODO generate outcome if already locked?
        return stepContext.requestLock(targetEntity);
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
        if (targetEntity != null) {
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

        targetEntity = event.getAai().get(PNF_NAME);
        if (targetEntity == null) {
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
                targetEntity = event.getAai().get(VSERVER_VSERVER_NAME);
                break;
            case GENERIC_VNF_VNF_ID:
                targetEntity = event.getAai().get(GENERIC_VNF_VNF_ID);
                break;
            case GENERIC_VNF_VNF_NAME:
                return detmVnfName();
            default:
                throw new IllegalArgumentException("Target does not match target type");
        }

        if (targetEntity == null) {
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
        targetEntity = event.getAai().get(GENERIC_VNF_VNF_ID);
        if (targetEntity != null) {
            return null;
        }

        // don't have the data yet - add a step to retrieve it
        return AAI_DEFAULT_GENERIC_VNF;
    }

    @Override
    public void setProperty(String name, Object value) {
        // only care about one property
        if (!UsecasesConstants.AAI_DEFAULT_GENERIC_VNF.equals(name)) {
            return;
        }

        GenericVnf vnf = (GenericVnf) value;
        targetEntity = vnf.getVnfId();
    }

    @Override
    public String getActorName() {
        return params.getActor();
    }

    @Override
    public String getName() {
        return params.getOperation();
    }
}
