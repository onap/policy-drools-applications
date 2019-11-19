/*-
 * ============LICENSE_START=======================================================
 * m2/base
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

package org.onap.policy.m2.base;

import org.onap.policy.controlloop.ControlLoopEvent;
import org.onap.policy.controlloop.policy.Policy;

/**
 * This is the 'Actor' interface -- objects implementing this interface
 * are placed within the 'nameToActor' table within class 'Transaction'.
 * All of the instances are created and inserted in the table at initialization
 * time, and are located using the 'Policy.actor' field as a key.
 */
public interface Actor {
    /**
     * Return the name associated with this Actor.
     *
     * @return the name associated with this Actor (as it appears in the 'yaml')
     */
    String getName();

    /**
     * Create an operation for this actor, based on the supplied policy.
     *
     * @param transaction the transaction the operation is running under
     * @param policy the policy associated with this operation
     * @param onset the initial onset event that triggered the transaction
     * @param attempt this value starts at 1, and is incremented for each retry
     * @return the Operation instance
     */
    Operation createOperation(
        Transaction transaction, Policy policy, ControlLoopEvent onset,
        int attempt);
}
