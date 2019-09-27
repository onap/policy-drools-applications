/*-
 * ============LICENSE_START=======================================================
 * guard
 * ================================================================================
 * Copyright (C) 2017-2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019 Tech Mahindra
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

package org.onap.policy.guard;

import java.util.UUID;
import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.drools.core.lock.Lock;
import org.onap.policy.drools.core.lock.LockCallback;
import org.onap.policy.drools.core.lock.LockImpl;
import org.onap.policy.drools.core.lock.PolicyResourceLockManager;
import org.onap.policy.guard.impl.PnfTargetLock;
import org.onap.policy.guard.impl.VmTargetLock;
import org.onap.policy.guard.impl.VnfTargetLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyGuard {

    private static final Logger logger = LoggerFactory.getLogger(PolicyGuard.class);

    /**
     * Factory to access various objects.  Can be changed for junit tests.
     */
    private static Factory factory = new Factory();

    private PolicyGuard() {
        // Cannot instantiate this static class
    }

    /**
     * Get the factory.
     *
     * @return the factory used to access various objects
     */
    protected static Factory getFactory() {
        return factory;
    }

    /**
     * Sets the factory to be used by junit tests.
     * @param factory factory
     */
    protected static void setFactory(Factory factory) {
        PolicyGuard.factory = factory;
    }

    /**
     * Create a pseudo lock, one that generally returns success.
     *
     * @param targetType the target type
     * @param targetInstance the target instance
     * @param requestId the request Id
     * @return the TargetLock
     * @throws IllegalArgumentException if an argument is null
     */
    public static TargetLock createTargetLock(TargetType targetType, String targetInstance,
            UUID requestId, LockCallback callback) {

        String reqstr = requestId.toString();
        LockImpl lock = new LockImpl(Lock.State.ACTIVE, targetInstance, reqstr, reqstr, callback, 0, true);
        lock.notifyAvailable();

        return createTargetLock(targetType, targetInstance, requestId, lock);
    }

    /**
     * Create a lock.
     *
     * @param targetType the target type
     * @param targetInstance the target instance
     * @param requestId the request Id
     * @param lock the lock to be wrapped within the TargetLock
     * @return the TargetLock
     * @throws IllegalArgumentException if an argument is null
     */
    private static TargetLock createTargetLock(TargetType targetType, String targetInstance,
            UUID requestId, Lock lock) {

        switch (targetType) {
            case PNF:
                //
                // Create the Lock object
                //
                return new PnfTargetLock(targetType, targetInstance, requestId, lock);
            case VM:
                //
                // Create the Lock object
                //
                return new VmTargetLock(targetType, targetInstance, requestId, lock);
            case VNF:
                //
                // Create the Lock object
                //
                return new VnfTargetLock(targetType, targetInstance, requestId, lock);

            case VFMODULE:
                //
                // Create the Lock object
                //
                return new VnfTargetLock(targetType, targetInstance, requestId, lock);
            default:
                logger.error("invalid target type {} for lock on {}", targetType, targetInstance);
                return null;
        }
    }

    /**
     * Lock a target.
     *
     * @param targetType the target type
     * @param targetInstance the target instance
     * @param requestId the request Id
     * @param callback the LockCallback
     * @param holdSec maximum number of seconds to hold the lock
     * @return the LockResult
     * @throws IllegalArgumentException if an argument is null
     */
    public static TargetLock lockTarget(TargetType targetType, String targetInstance,
            UUID requestId, LockCallback callback, int holdSec) {

        Lock realLock = factory.getManager().lock(targetInstance, requestId.toString(), callback, holdSec, false);

        TargetLock lock = createTargetLock(targetType, targetInstance, requestId, realLock);
        if (lock == null) {
            //
            // Bad lock type: unlock and return exception result
            //
            realLock.free();

        } else {
            logger.debug("Lock {} request sent", lock);
        }

        return lock;
    }

    /**
     * Extends a lock on a target.
     * @param lock      current lock
     * @param holdSec maximum number of seconds to hold the lock
     * @return {@code true} if an extension request was issued, {@code false} if it failed
     */
    public static boolean lockTarget(TargetLock lock, int holdSec) {
        boolean result = lock.extend(holdSec);

        logger.debug("Lock {} extending {}", lock, result);
        return result;
    }

    /**
     * Unlock a target.
     *
     * @param lock the target lock to unlock
     * @return <code>true</code> if the target is successfully unlocked, <code>false</code>
     *         otherwise
     * @throws IllegalArgumentException if an argument is null
     */
    public static boolean unlockTarget(TargetLock lock) {
        boolean result = lock.free();

        logger.debug("Lock {} releasing {}", lock, result);
        return result;
    }

    /**
     * Factory to access various objects.
     */
    public static class Factory {

        /**
         * Get the manager.
         *
         * @return the lock manager to be used
         */
        public PolicyResourceLockManager getManager() {
            return PolicyResourceLockManager.getInstance();
        }
    }
}
