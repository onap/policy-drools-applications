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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.eventmanager.StepContext;

/**
 * A LOCK operation for a particular target entity. This is a "pseudo" operation; it is
 * not found within an ActorService, but is created directly.
 */
public class LockOperation2 implements Operation {

    private final StepContext stepContext;
    private final ControlLoopOperationParams params;

    private final AtomicReference<String> targetEntity;


    /**
     * Constructs the lock operation as a preprocessing step for a policy's operation.
     *
     * @param stepContext the context to use to request the lock
     * @param params operation's parameters
     * @param targetEntity place where the target entity is stored
     */
    public LockOperation2(StepContext stepContext, ControlLoopOperationParams params,
                    AtomicReference<String> targetEntity) {
        this.stepContext = stepContext;
        this.params = params;
        this.targetEntity = targetEntity;
    }

    @Override
    public CompletableFuture<OperationOutcome> start() {
        String entity = targetEntity.get();
        if (entity == null) {
            throw new IllegalStateException("target lock entity has not been determined yet");
        }

        // TODO generate outcome if already locked?
        return stepContext.requestLock(entity);
    }

    @Override
    public void setProperty(String name, Object value) {
        return;
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
