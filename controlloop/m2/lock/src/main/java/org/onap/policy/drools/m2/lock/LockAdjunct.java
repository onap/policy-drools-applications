/*-
 * ============LICENSE_START=======================================================
 * m2/lock
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

package org.onap.policy.drools.m2.lock;

import java.io.Serializable;

import org.onap.policy.drools.core.lock.Lock;
import org.onap.policy.drools.core.lock.LockCallback;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.m2.base.Transaction;

/*
 * Adjunct data placed within the transaction, to contain locks
 * on control loop use case basis. This same instance is shared
 * among all actor operation instances within the transaction
 * regardless of what actor is in the policy chain. As a result,
 * the lock gets passed from one to the next, and is only freed
 * when the transaction completes.
 */
public class LockAdjunct implements Transaction.Adjunct, LockCallback,
    Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * This is the callback interface, which is only used if the lock is
     * initially not available, and we end up waiting for it.
     */
    public interface Requestor {
        /**
         * This method is called once the lock has been acquired.
         */
        void lockAvailable();

        /**
         * This method is called to notify the requestor that the lock could not
         * be obtained.
         */
        void lockUnavailable();
    }

    // lock associated with all of the AOTS and SDNR operations
    // within the transaction
    private Lock lock = null;

    // the initial requestor of the lock
    // (only set if we don't immediately acquire the lock)
    private Requestor requestor = null;

    /**
     * allocate a lock on behalf of the requestor.
     *
     * @param requestor
     *            the AOTS or SDNR operation requesting the lock
     * @param key
     *            string key identifying the lock
     * @param ownerKey
     *            string key identifying the owner
     * @param waitForLock
     *            used to determine if an operation should wait for a lock to
     *            become available or fail immediately
     * @return 'true' if we immediately acquired the lock, 'false' if the lock
     *         is currently in use by another transaction, and we need to wait
     */
    public boolean getLock(Requestor requestor, String key, String ownerKey,
                           boolean waitForLock) {
        if (lock != null) {
            if (lock.isActive()) {
                // we already have an active lock
                return true;
            }

            // We may have timed out earlier waiting for the lock, or
            // we may have lost the lock after persistent restore. In
            // any case, free the lock we have now, and allocate a new one.
            lock.free();
        }

        // register the requestor in case the lockUnavailable() callback runs
        // immediately. Previously the requestor would be set after the
        // lock constructor ran but now the lockUnavailable() callback
        // could be executed while the constructor is still on the stack which
        // would result in a null requestor, thus the callback never
        // notifying the requestor that the lock was unavailable.
        this.requestor = requestor;

        // try to allocate a new lock
        lock = PolicyEngineConstants.getManager().createLock(
               key, ownerKey, 600, this, waitForLock);
        // return the boolean value of the lock.isactive
        return lock.isActive();
    }

    /*=================================*/
    /* 'Transaction.Adjunct' interface */
    /*=================================*/

    /**
     * Notification that the transaction has completed.
     *
     * {@inheritDoc}
     */
    @Override
    public void cleanup(Transaction transaction) {
        if (lock != null) {
            // free the lock or cancel the reservation
            lock.free();
        }
    }

    /*==========================*/
    /* 'LockCallback' interface */
    /*==========================*/

    /**
     * Notification that the lock is available.
     *
     * {@inheritDoc}
     */
    @Override
    public void lockAvailable(Lock lock) {
        this.lock = lock;
        if (requestor != null) {
            // notify requestor
            requestor.lockAvailable();
        }
    }

    /**
     * Notification that the lock is not available.
     *
     * {@inheritDoc}
     */
    @Override
    public void lockUnavailable(Lock lock) {
        this.lock = lock;
        if (requestor != null) {
            // notify requestor
            requestor.lockUnavailable();
        }
    }
}
