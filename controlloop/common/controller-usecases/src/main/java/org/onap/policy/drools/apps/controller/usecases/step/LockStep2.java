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

import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.eventmanager.ActorConstants;
import org.onap.policy.drools.apps.controller.usecases.LockOperation2;

/**
 * Wrapper for the LOCK pseudo operation. Arranges for the lock's future to generate an
 * outcome.
 */
public class LockStep2 extends Step2 {

    /**
     * Constructs the object using information from another step.
     *
     * @param otherStep step whose information should be used
     */
    public LockStep2(Step2 otherStep) {
        super(otherStep, ActorConstants.LOCK_ACTOR, ActorConstants.LOCK_OPERATION);
    }

    @Override
    public boolean acceptsEvent() {
        return true;
    }

    @Override
    public boolean start(long remainingMs) {
        super.start(remainingMs);

        if (future != null) {
            future.whenComplete((outcome, thrown) -> {
                outcome.setFinalOutcome(true);
                params.getCompleteCallback().accept(outcome);
            });
        }

        /*
         * ELSE: The operation must have thrown an exception. Nothing more to do as
         * super.start() will have already propagated the exception to the completion
         * handler.
         */

        return true;
    }

    @Override
    protected Operation buildOperation() {
        return new LockOperation2(stepContext, params);
    }
}
