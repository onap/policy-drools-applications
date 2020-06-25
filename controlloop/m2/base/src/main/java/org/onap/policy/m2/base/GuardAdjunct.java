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
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.guard.GuardContext;

/**
 * This adjunct class provides a way of accessing 'GuardContext' for any
 * operations that use 'guard'. It needs to be created and inserted into the
 * transaction 'adjunct' list prior to creating any operations that use it.
 *
 * <p>TBD: Serialization will need to factor in the fact that 'GuardContext'
 * is a shared object.
 */
public class GuardAdjunct implements Transaction.Adjunct, Serializable {
    private static final long serialVersionUID = 1L;

    // the associated transaction
    private Transaction transaction = null;

    // the 'GuardContext' instance
    private GuardContext context = null;

    /**
     * Constructor -- just in case 'getInstance()' is used to create this
     * instance (in which case 'guard' will not be used).
     */
    public GuardAdjunct() {
        // This comment is here to keep SONAR from getting upset
    }

    /**
     * This method is called to create the adjunct, and insert it into the
     * transaction.
     *
     * @param transaction the associated transaction
     * @param context the GuardContext derived from the controller properties
     */
    public static void create(Transaction transaction, GuardContext context) {
        GuardAdjunct ga = new GuardAdjunct();
        ga.transaction = transaction;
        ga.context = context;
        transaction.putAdjunct(ga);
    }

    /**
     * Return the GuardContext instance.
     *
     * @return the GuardContext instance
     */
    public GuardContext get() {
        return context;
    }

    /**
     * Do an asynchronous 'guard' query, and place the result in Drools memory.
     *
     * @param policy the policy associated with the operation
     * @param target the target in a form meaningful to 'guard'
     * @param requestId the transaction's request id
     * @return 'true' if we have a 'GuardContext', and a response is expected,
     *     'false' if 'guard' was not used, and should be skipped
     */
    public boolean asyncQuery(Policy policy, String target, String requestId) {
        if (context != null) {
            // note that we still return 'true' as long as we have a
            // 'GuardContext', even when 'guard.disabled' is set -- as long
            // as there is an asynchronous response coming
            context.asyncQuery(transaction.getWorkingMemory(),
                               policy.getActor(),
                               policy.getRecipe(),
                               target,
                               requestId,
                               transaction.getClosedLoopControlName());
            return true;
        }
        return false;
    }

    /**
     * Create a DB entry describing this operation.
     *
     * @param op the history entry associated with the operation
     * @param target the same target that was passed on the 'asyncQuery' call
     */
    public void asyncCreateDbEntry(ControlLoopOperation op, String target) {
        if (context != null) {
            context.asyncCreateDbEntry(
                op.getStart(),
                op.getEnd(),
                transaction.getClosedLoopControlName(),
                op.getActor(),
                op.getOperation(),
                target,
                transaction.getRequestId().toString(),
                op.getSubRequestId(),
                op.getMessage(),
                op.getOutcome());
        }
    }
}
