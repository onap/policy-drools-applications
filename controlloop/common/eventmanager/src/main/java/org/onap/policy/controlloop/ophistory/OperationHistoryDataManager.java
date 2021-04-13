/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.controlloop.ophistory;

import org.onap.policy.controlloop.ControlLoopOperation;

/**
 * Data manager for the Operation History table.
 */
public interface OperationHistoryDataManager {

    /**
     * Stores an operation in the DB. If the queue is full, then the oldest records is
     * discarded.
     *
     * @param requestId request ID
     * @param clName control loop name
     * @param event event with which the operation is associated, typically just used for
     *        logging
     * @param targetEntity target entity associated with the operation
     * @param operation operation to be stored
     */
    void store(String requestId, String clName, Object event, String targetEntity, ControlLoopOperation operation);

    /**
     * Starts the background thread.
     */
    public void start();

    /**
     * Stops the background thread and places an "end" item into {@link #operations}.
     */
    public void stop();
}
