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

import java.util.concurrent.atomic.AtomicReference;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.drools.apps.controller.usecases.GetTargetEntityOperation2;
import org.onap.policy.drools.apps.controller.usecases.UsecasesConstants;

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
        return new GetTargetEntityOperation2(event, params, targetEntity);
    }

    @Override
    public boolean start(long remainingMs) {
        if (targetEntity.get() == null) {
            throw new IllegalStateException("Target Entity has not been determined yet");
        }

        return false;
    }
}
