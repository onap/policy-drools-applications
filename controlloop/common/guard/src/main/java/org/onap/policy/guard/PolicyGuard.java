/*-
 * ============LICENSE_START=======================================================
 * guard
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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
import org.onap.policy.drools.core.lock.PolicyResourceLockManager;
import org.onap.policy.guard.impl.PNFTargetLock;
import org.onap.policy.guard.impl.VMTargetLock;
import org.onap.policy.guard.impl.VNFTargetLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyGuard {
    private PolicyGuard() {
        // Cannot instantiate this static class
    }

    private static final Logger logger = LoggerFactory.getLogger(PolicyGuard.class);
    
    /**
     * Factory to access various objects.  Can be changed for junit tests.
     */
    private static Factory factory = new Factory();

    public static class LockResult<A, B> {
        private A parameterA;
        private B parameterB;

        public static <A, B> LockResult<A, B> createLockResult(A parameterA, B parameterB) {
            return new LockResult<>(parameterA, parameterB);
        }

        public LockResult(A parameterA, B parameterB) {
            this.parameterA = parameterA;
            this.parameterB = parameterB;
        }

        public A getA() {
            return parameterA;
        }

        public B getB() {
            return parameterB;
        }
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
     * Create a lock.
     * 
     * @param targetType the target type
     * @param targetInstance the target instance
     * @param requestID the request Id
     * @return the TargetLock
     * @throws IllegalArgumentException if an argument is null
     */
    public static TargetLock createTargetLock(TargetType targetType, String targetInstance,
            UUID requestID, LockCallback callback) {
        switch (targetType) {
            case PNF:
                //
                // Create the Lock object
                //
                return new PNFTargetLock(targetType, targetInstance, requestID, callback);
            case VM:
                //
                // Create the Lock object
                //
                return new VMTargetLock(targetType, targetInstance, requestID, callback);
            case VNF:
                //
                // Create the Lock object
                //
                return new VNFTargetLock(targetType, targetInstance, requestID, callback);
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
     * @param requestID the request Id
     * @param callback the LockCallback
     * @param holdSec maximum number of seconds to hold the lock
     * @return the LockResult
     * @throws IllegalArgumentException if an argument is null
     */
    public static LockResult<GuardResult, TargetLock> lockTarget(TargetType targetType, String targetInstance,
            UUID requestID, LockCallback callback, int holdSec) {
        String owner = makeOwner(targetType, requestID);
        boolean result = factory.getManager().lock(targetInstance, owner, holdSec);
        if (!result) {
            return LockResult.createLockResult(GuardResult.LOCK_DENIED, null);
        }

        TargetLock lock = createTargetLock(targetType, targetInstance, requestID, callback);
        if (lock == null) {
            //
            // Bad lock type: unlock and return exception result
            // 
            factory.getManager().unlock(targetInstance, owner);
            return LockResult.createLockResult(GuardResult.LOCK_EXCEPTION, null);
        } else {
            //
            // Return result
            //
            logger.debug("Locked {}", lock);
            return LockResult.createLockResult(GuardResult.LOCK_ACQUIRED, lock);
        }
    }
        
    /**
     * Extends a lock on a target.
     * @param lock      current lock
     * @param holdSec maximum number of seconds to hold the lock
     * @return the result: acquired or denied
     */
    public static GuardResult lockTarget(TargetLock lock, int holdSec) {
        String owner = makeOwner(lock.getTargetType(), lock.getRequestID());
        
        boolean result = factory.getManager().refresh(lock.getTargetInstance(), owner, holdSec);
        
        logger.debug("Lock {} extend {}", lock, result);
        return (result ? GuardResult.LOCK_ACQUIRED : GuardResult.LOCK_DENIED);
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
        String owner = makeOwner(lock.getTargetType(), lock.getRequestID());
        boolean result = factory.getManager().unlock(lock.getTargetInstance(), owner);
        
        if (result) {
            logger.debug("Unlocked {}", lock);
            return true;            
        }
        
        return false;
    }

    /**
     * Check if a target is locked.
     * 
     * @param targetType the target type
     * @param targetInstance the target instance
     * @param requestID the request Id
     * @return <code>true</code> if the target is locked, <code>false</code> otherwise
     * @throws IllegalArgumentException if an argument is null
     */
    public static boolean isLocked(TargetType targetType, String targetInstance, UUID requestID) {
        String owner = makeOwner(targetType, requestID);
        return factory.getManager().isLockedBy(targetInstance, owner);
    }

    /**
     * Combines the target type and request ID to yield a single, "owner" string.
     * @param targetType target type
     * @param requestID request id
     * @return the "owner" of a resource
     * @throws IllegalArgumentException if either argument is null
     */
    private static String makeOwner(TargetType targetType, UUID requestID) {
        if (targetType == null) {
            throw new IllegalArgumentException("null targetType for lock request id " + requestID);
        }

        if (requestID == null) {
            throw new IllegalArgumentException("null requestID for lock type " + targetType);
        }
        
        return targetType + ":" + requestID;
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
