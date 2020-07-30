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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.onap.policy.controlloop.actor.guard.DecisionOperation;
import org.onap.policy.controlloop.actor.guard.GuardActor;
import org.onap.policy.controlloop.actor.so.VfModuleCreate;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.OperationProperties;
import org.onap.policy.controlloop.eventmanager.ActorConstants;

/**
 * Wrapper for a Guard operation. Note: this makes a clone of the operation parameters,
 * replacing the payload.
 */
public class GuardStep2 extends Step2 {

    /**
     * Properties of interest to guards.
     */
    private static final Set<String> PROPERTY_NAMES = Set.of(OperationProperties.DATA_VF_COUNT);

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
        payload.put("target", params.getTargetEntity());
        payload.put("requestId", params.getRequestId());
        payload.put("clname", closedLoopControlName);

        params = params.toBuilder().payload(payload).build();
    }

    @Override
    public List<String> getPropertyNames() {
        return policyOper.getPropertyNames().stream().filter(PROPERTY_NAMES::contains).collect(Collectors.toList());
    }

    @Override
    protected void loadVfCount(String propName) {
        // run guard with the proposed VF count
        int count = getVfCount();
        if (VfModuleCreate.NAME.equals(policyOper.getName())) {
            ++count;
        } else {
            --count;
        }

        params.getPayload().put(ActorConstants.PAYLOAD_KEY_VF_COUNT, count);
    }
}
