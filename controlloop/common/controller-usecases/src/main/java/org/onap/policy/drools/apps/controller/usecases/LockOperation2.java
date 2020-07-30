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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.OperationProperties;
import org.onap.policy.controlloop.actorserviceprovider.impl.OperationPartial;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.eventmanager.StepContext;

/**
 * A LOCK operation for a particular target entity. This is a "pseudo" operation; it is
 * not found within an ActorService, but is created directly.
 */
public class LockOperation2 extends OperationPartial {

    private final StepContext stepContext;

    private static final List<String> PROPERTY_NAMES = List.of(OperationProperties.AAI_TARGET_ENTITY);

    /**
     * Set when {@link #setProperty(String, Object)} is invoked.
     */
    @Getter
    private String targetEntity;


    /**
     * Constructs the lock operation as a preprocessing step for a policy's operation.
     *
     * @param stepContext the context to use to request the lock
     * @param params operation's parameters
     */
    public LockOperation2(StepContext stepContext, ControlLoopOperationParams params) {
        super(params, null, PROPERTY_NAMES);
        this.stepContext = stepContext;
    }

    @Override
    public CompletableFuture<OperationOutcome> start() {
        if (targetEntity == null) {
            throw new IllegalStateException("target lock entity has not been determined yet");
        }

        return stepContext.requestLock(targetEntity);
    }

    @Override
    public void setProperty(String name, Object value) {
        if (OperationProperties.AAI_TARGET_ENTITY.equals(name) && value != null) {
            targetEntity = value.toString();
        }
    }
}
