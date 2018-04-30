/*-
 * ============LICENSE_START=======================================================
 * guard
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
     * @return the factory used to access various objects
     */
    protected static Factory getFactory() {
        return factory;
    }
    
    /**
     * Sets the factory to be used by junit tests.
     * @param factory
     */
    protected static void setFactory(Factory factory) {
        PolicyGuard.factory = factory;
    }

    /**
     * Lock a target.
     * 
     * @param targetType the target type
     * @param targetInstance the target instance
     * @param requestID the request Id
     * @param callback the LockCallback
     * @return the LockResult
     * @throws IllegalArgumentException if an argument is null
     */
    public static LockResult<GuardResult, TargetLock> lockTarget(TargetType targetType, String targetInstance,
            UUID requestID, LockCallback callback) {
        
        String owner = makeOwner(targetType, requestID);
        
        GuardResult guardResult = managerLockTarget(targetInstance, owner);
        if(guardResult != GuardResult.LOCK_ACQUIRED) {
            return LockResult.createLockResult(guardResult, null);
        }
        
        TargetLock lock = null;
        switch (targetType) {
            case PNF:
                //
                // Create the Lock object
                //
                lock = new PNFTargetLock(targetType, targetInstance, requestID, callback);
                break;
            case VM:
                //
                // Create the Lock object
                //
                lock = new VMTargetLock(targetType, targetInstance, requestID, callback);
                break;
            case VNF:
                //
                // Create the Lock object
                //
                lock = new VNFTargetLock(targetType, targetInstance, requestID, callback);
                break;

            default:
                logger.error("invalid target type {} for lock on {}", targetType, targetInstance);
                factory.getManager().unlock(targetInstance, owner);
                return LockResult.createLockResult(GuardResult.LOCK_EXCEPTION, null);
        }
        
        //
        // Return result
        //
        logger.debug("Locked {}", lock);
        return LockResult.createLockResult(GuardResult.LOCK_ACQUIRED, lock);
    }

    /**
     * Asks the manager to lock the given target.
     * @param targetInstance
     * @param owner
     * @return the result: acquired, denied, or exception
     */
    private static GuardResult managerLockTarget(String targetInstance, String owner) {
        try {
            Future<Boolean> result = factory.getManager().lock(targetInstance, owner, null);
            return(result.get() ? GuardResult.LOCK_ACQUIRED : GuardResult.LOCK_DENIED);
            
        } catch(IllegalStateException e) {
            logger.warn("{} attempted to re-lock {}", owner, targetInstance);
            return GuardResult.LOCK_DENIED;
            
        } catch (InterruptedException e) {
            logger.error("exception getting lock for {}", targetInstance, e);
            Thread.currentThread().interrupt();
            return GuardResult.LOCK_EXCEPTION;
            
        } catch (ExecutionException e) {
            logger.error("exception getting lock for {}", targetInstance, e);
            return GuardResult.LOCK_EXCEPTION;
        }
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
        
        if(result) {
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
     * @param targetType
     * @param requestID
     * @return the "owner" of a resource
     * @throws IllegalArgumentException if either argument is null
     */
    private static String makeOwner(TargetType targetType, UUID requestID) {
        if(targetType == null) {
            throw new IllegalArgumentException("null targetType for lock request id " + requestID);
        }

        if(requestID == null) {
            throw new IllegalArgumentException("null requestID for lock type " + targetType);
        }
        
        return targetType.toString() + ":" + requestID.toString();
    }
    
    /**
     * Factory to access various objects.
     */
    public static class Factory {

        /**
         * @return the lock manager to be used
         */
        public PolicyResourceLockManager getManager() {
            return PolicyResourceLockManager.getInstance();
        }
    }
}
