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

import java.io.Serializable;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;

/**
 * This is the 'Operation' interface -- each object implementing this
 * interface exists for the duration of a single operation.
 *
 * <p>An operation typically includes some of the following steps:
 * 1) Acquiring locks
 * 2) 'Guard' query to see if the operation should proceed (some operations)
 * 3) Outgoing request (usually using DMAAP or UEB, but possibly HTTP or HTTPS)
 * 4) Incoming response
 */
public interface Operation extends Serializable {
    /**
     * This method is used as part of sending out the request. In the case of
     * DMAAP or UEB interfaces, the method returns the message to be sent,
     * but leaves it to the Drools code to do the actual sending. In the case
     * of HTTP or HTTPS (e.g. AOTS), the method itself may run the operation in
     * a different thread.
     *
     * @return an object containing the message
     * @throws ControlLoopException if it occurs
     */
    Object getRequest() throws ControlLoopException;

    /**
     * Return the 'Policy' instance associated with this operation.
     *
     * @return the 'Policy' instance associated with this operation
     */
    Policy getPolicy();

    /**
     * The 'state' of an operation is also the state of the 'Transaction'
     * instance, while that operation is active. The state is often referenced
     * in the 'when' clause of Drools rules, with the rules resembling state
     * transition routines (state + event -> operation). In order to avoid
     * confusion, the state names should be unique across all operations --
     * this is managed by having each state name begin with 'ACTOR.', where
     * 'ACTOR' is the actor associated with the operation.
     *
     * @return a string value indicating the state of the operation
     */
    String getState();

    /**
     * This is set to '1' for the initial attempt, and is incremented by one
     * for each retry. Note that a new 'Operation' instance is created for
     * each attempt.
     *
     * @return '1' for the initial attempt of an operation, and incremented
     *     for each retry
     */
    int getAttempt();

    /**
     * Return the result of the operation.
     *
     * @return the result of the operation
     *     ('null' if the operation is still in progress)
     */
    PolicyResult getResult();

    /**
     * Return the message associated with the completed operation.
     *
     * @return the message associated with the completed operation
     *     ('null' if the operation is still in progress)
     */
    String getMessage();

    /**
     * An incoming message is being delivered to the operation. The type of
     * the message is operation-dependent, and an operation will typically
     * understand only one or two message types, and ignore the rest. The
     * calling Drools code is written to assume that the transaction has been
     * modified -- frequently, a state transition occurs as a result of
     * the message.
     *
     * @param object the incoming message
     */
    void incomingMessage(Object object);

    /**
     * The operation has timed out. This typically results in the operation
     * completing, but that is not enforced.
     */
    void timeout();

    /**
     * This method is called on every operation right after its history
     * entry has been completed. It gives the operation a chance to do some
     * processing based on this entry (e.g. create a 'guard' entry in the DB).
     *
     * @param histEntry the history entry for this particular operation
     */
    default void histEntryCompleted(ControlLoopOperation histEntry) {
    }
}
