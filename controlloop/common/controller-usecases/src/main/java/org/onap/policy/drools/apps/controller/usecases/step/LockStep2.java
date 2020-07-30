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
import org.onap.policy.controlloop.eventmanager.ActorConstants;
import org.onap.policy.drools.apps.controller.usecases.LockOperation2;

public class LockStep2 extends Step2 {

    private final AtomicReference<String> targetEntity;

    /**
     * Constructs the object using information from another step.
     *
     * @param otherStep step whose information should be used
     * @param targetEntity place where the target entity is stored
     */
    public LockStep2(Step2 otherStep, AtomicReference<String> targetEntity) {
        super(otherStep, ActorConstants.LOCK_ACTOR, ActorConstants.LOCK_OPERATION);
        this.targetEntity = targetEntity;
    }

    @Override
    protected Operation buildOperation() {
        return new LockOperation2(stepContext, params, targetEntity);
    }
}
