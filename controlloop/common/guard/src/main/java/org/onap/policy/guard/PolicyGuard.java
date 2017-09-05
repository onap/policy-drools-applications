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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.guard.impl.PNFTargetLock;
import org.onap.policy.guard.impl.VMTargetLock;
import org.onap.policy.guard.impl.VNFTargetLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyGuard {

	private static Map<String, TargetLock> activeLocks = new HashMap<String, TargetLock>();
	private static final Logger logger = LoggerFactory.getLogger(PolicyGuard.class);
	public static class LockResult<A, B> {
		private A a;
		private B b;
		
		public static <A, B> LockResult<A, B> createLockResult(A a, B b) {
			return new LockResult<A, B>(a, b);
		}
		
		public LockResult(A a, B b) {
			this.a = a;
			this.b = b;
		}
		
		public A getA() {
			return a;
		}
		
		public B getB() {
			return b;
		}
	}
	
	public static LockResult<GuardResult, TargetLock> lockTarget(TargetType targetType, String targetInstance, UUID requestID, LockCallback callback) {
		
		synchronized(activeLocks) {
			//
			// Is there a lock on this instance already?
			//
			if (activeLocks.containsKey(targetInstance)) {
				return LockResult.createLockResult(GuardResult.LOCK_DENIED, null);
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
				return LockResult.createLockResult(GuardResult.LOCK_EXCEPTION, null);
			}
			//
			// Keep track of it
			//
			activeLocks.put(targetInstance, lock);
			//
			// Return result
			//
			logger.debug("Locking {}", lock);
			return LockResult.createLockResult(GuardResult.LOCK_ACQUIRED, lock);
		}
	}
	
	public static boolean	unlockTarget(TargetLock lock) {
		synchronized(activeLocks) {
			if (activeLocks.containsKey(lock.getTargetInstance())) {
				logger.debug("Unlocking {}", lock);
				return (activeLocks.remove(lock.getTargetInstance()) != null);
			}
			return false;
		}
	}
	
	public static boolean	isLocked(TargetType targetType, String targetInstance, UUID requestID) {
		synchronized(activeLocks) {
			if (activeLocks.containsKey(targetInstance)) {
				TargetLock lock = activeLocks.get(targetInstance);
				return (lock.getTargetType().equals(targetType) && lock.getRequestID().equals(requestID));
			}
			return false;
		}		
	}
	
}
