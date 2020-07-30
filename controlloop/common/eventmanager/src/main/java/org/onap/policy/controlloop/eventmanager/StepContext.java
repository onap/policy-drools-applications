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

package org.onap.policy.controlloop.eventmanager;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.onap.policy.controlloop.actorserviceprovider.ActorService;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.controlloop.ControlLoopEventContext;

/**
 * Context used by a Step.
 */
public interface StepContext {

    /**
     * Gets the actor service.
     *
     * @return the actor service
     */
    ActorService getActorService();

    /**
     * Gets the event context.
     *
     * @return the event context
     */
    ControlLoopEventContext getContext();

    /**
     * Gets the executor to use to complete non-blocking futures.
     *
     * @return the executor to use to complete non-blocking futures
     */
    Executor getExecutor();

    /**
     * Sets the start time to the current time, if not already set.
     */
    void setStartTime();

    /**
     * Gets the start time.
     *
     * @return the start time
     */
    Instant getStartTime();

    /**
     * Invoked when an operation is started.
     *
     * @param outcome the starting outcome
     */
    void onStart(OperationOutcome outcome);

    /**
     * Invoked when an operation completes.
     *
     * @param outcome the completed outcome
     */
    void onComplete(OperationOutcome outcome);

    /**
     * Invoked when a lock becomes unavailable.
     *
     * @param outcome the lock failure outcome
     */
    void abort(OperationOutcome outcome);

    /**
     * Requests a lock. This requests the lock for the time that remains before the
     * timeout expires. This avoids having to extend the lock.
     *
     * @param targetEntity entity to be locked
     * @return a future that can be used to await the lock
     */
    CompletableFuture<OperationOutcome> requestLock(String targetEntity);
}
