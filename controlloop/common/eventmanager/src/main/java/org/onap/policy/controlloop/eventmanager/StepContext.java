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

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;

/**
 * Context used by steps to perform their work.
 */
public interface StepContext {

    /**
     * Determines if the context contains a property.
     *
     * @param name name of the property of interest
     * @return {@code true} if the context contains the property, {@code false} otherwise
     */
    public boolean contains(String name);

    /**
     * Gets a property, casting it to the desired type.
     *
     * @param <T> desired type
     * @param name name of the property whose value is to be retrieved
     * @return the property's value, or {@code null} if it does not yet have a value
     */
    public <T> T getProperty(String name);

    /**
     * Sets a property's value.
     *
     * @param name property name
     * @param value new property value
     */
    public void setProperty(String name, Serializable value);

    /**
     * Removes a property.
     *
     * @param name property name
     */
    public void removeProperty(String name);

    /**
     * Requests a lock. This requests the lock for the time that remains before the
     * timeout expires. This avoids having to extend the lock.
     *
     * @param targetEntity entity to be locked
     * @return a future that can be used to await the lock
     */
    public CompletableFuture<OperationOutcome> requestLock(String targetEntity);
}
