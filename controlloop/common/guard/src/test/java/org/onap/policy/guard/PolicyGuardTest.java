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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;
import org.onap.policy.controlloop.policy.TargetType;

public class PolicyGuardTest {
	private class DummyLockCallback implements LockCallback{
		@Override
		public boolean isActive() {
			// TODO Auto-generated method stub
			return false;
		}
		@Override
		public boolean releaseLock() {
			// TODO Auto-generated method stub
			return false;
		}
	}
	private class DummyTargetLock implements TargetLock{
		@Override
		public UUID getLockID() {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public TargetType getTargetType() {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public String getTargetInstance() {
			return INSTANCENAME;
		}
		@Override
		public UUID getRequestID() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	private static final String INSTANCENAME = "targetInstance";

	@Test
	public void testAll() {
		UUID uuid = UUID.randomUUID();
		TargetType type = TargetType.VM;


		// Test isLocked before and after lock added
		assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
		PolicyGuard.lockTarget(type, INSTANCENAME, uuid , new DummyLockCallback());
		assertTrue(PolicyGuard.isLocked(type, INSTANCENAME, uuid));

		// Test isLocked after lock removed
		PolicyGuard.unlockTarget(new DummyTargetLock());
		assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
	}
}
