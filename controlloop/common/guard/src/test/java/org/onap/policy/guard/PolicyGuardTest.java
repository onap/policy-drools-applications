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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;
import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.guard.PolicyGuard.LockResult;
import org.onap.policy.guard.impl.PNFTargetLock;
import org.onap.policy.guard.impl.VMTargetLock;
import org.onap.policy.guard.impl.VNFTargetLock;

public class PolicyGuardTest {
    private static final String INSTANCENAME = "targetInstance";

    private class DummyLockCallback implements LockCallback {
        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public boolean releaseLock() {
            return false;
        }
    }

    private class DummyTargetLock implements TargetLock {
        @Override
        public UUID getLockID() {
            return null;
        }

        @Override
        public TargetType getTargetType() {
            return null;
        }

        @Override
        public String getTargetInstance() {
            return INSTANCENAME;
        }

        @Override
        public UUID getRequestID() {
            return null;
        }
    }

    @Test
    public void testLockVm() {
        UUID uuid = UUID.randomUUID();
        TargetType type = TargetType.VM;

        // Test isLocked before and after lock added
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
        DummyLockCallback dlcb = new DummyLockCallback();
        LockResult<GuardResult, TargetLock> result = PolicyGuard.lockTarget(type, INSTANCENAME, uuid, dlcb);
        assertTrue(PolicyGuard.isLocked(type, INSTANCENAME, uuid));

        assertEquals(GuardResult.LOCK_ACQUIRED, result.getA());
        assertEquals(VMTargetLock.class, result.getB().getClass());

        VMTargetLock vtl = (VMTargetLock) result.getB();
        assertNotNull(vtl.getLockID());
        assertEquals(INSTANCENAME, vtl.getTargetInstance());
        assertEquals(TargetType.VM, vtl.getTargetType());
        assertNotNull(vtl.getRequestID());
        assertEquals(dlcb, vtl.getCallback());

        // Test isLocked after lock removed
        PolicyGuard.unlockTarget(new DummyTargetLock());
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
    }

    @Test
    public void testLockPnf() {
        UUID uuid = UUID.randomUUID();
        TargetType type = TargetType.PNF;

        // Test isLocked before and after lock added
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
        DummyLockCallback dlcb = new DummyLockCallback();
        LockResult<GuardResult, TargetLock> result = PolicyGuard.lockTarget(type, INSTANCENAME, uuid, dlcb);
        assertTrue(PolicyGuard.isLocked(type, INSTANCENAME, uuid));

        assertEquals(GuardResult.LOCK_ACQUIRED, result.getA());
        assertEquals(PNFTargetLock.class, result.getB().getClass());

        PNFTargetLock ptl = (PNFTargetLock) result.getB();
        assertNotNull(ptl.getLockID());
        assertEquals(INSTANCENAME, ptl.getTargetInstance());
        assertEquals(TargetType.PNF, ptl.getTargetType());
        assertNotNull(ptl.getRequestID());
        assertEquals(dlcb, ptl.getCallback());

        // Test isLocked after lock removed
        PolicyGuard.unlockTarget(new DummyTargetLock());
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
    }


    @Test
    public void testLockVnf() {
        UUID uuid = UUID.randomUUID();
        TargetType type = TargetType.VNF;

        // Test isLocked before and after lock added
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
        DummyLockCallback dlcb = new DummyLockCallback();
        LockResult<GuardResult, TargetLock> result = PolicyGuard.lockTarget(type, INSTANCENAME, uuid, dlcb);
        assertTrue(PolicyGuard.isLocked(type, INSTANCENAME, uuid));

        assertEquals(GuardResult.LOCK_ACQUIRED, result.getA());
        assertEquals(VNFTargetLock.class, result.getB().getClass());

        VNFTargetLock vtl = (VNFTargetLock) result.getB();
        assertNotNull(vtl.getLockID());
        assertEquals(INSTANCENAME, vtl.getTargetInstance());
        assertEquals(TargetType.VNF, vtl.getTargetType());
        assertNotNull(vtl.getRequestID());
        assertEquals(dlcb, vtl.getCallback());

        // Test isLocked after lock removed
        PolicyGuard.unlockTarget(new DummyTargetLock());
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
    }

    @Test
    public void testLockVfc() {
        UUID uuid = UUID.randomUUID();
        TargetType type = TargetType.VFC;

        // Test isLocked before and after lock added
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
        DummyLockCallback dlcb = new DummyLockCallback();
        LockResult<GuardResult, TargetLock> result = PolicyGuard.lockTarget(type, INSTANCENAME, uuid, dlcb);
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));

        assertEquals(GuardResult.LOCK_EXCEPTION, result.getA());
        assertNull(result.getB());

        // Test isLocked after lock removed
        PolicyGuard.unlockTarget(new DummyTargetLock());
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
    }

    @Test
    public void testUnLockNotLocked() {
        UUID uuid = UUID.randomUUID();
        TargetType type = TargetType.VM;

        // Test isLocked before and after lock added
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
        DummyLockCallback dlcb = new DummyLockCallback();
        LockResult<GuardResult, TargetLock> result = PolicyGuard.lockTarget(type, INSTANCENAME, uuid, dlcb);
        assertTrue(PolicyGuard.isLocked(type, INSTANCENAME, uuid));

        assertEquals(GuardResult.LOCK_ACQUIRED, result.getA());
        assertEquals(VMTargetLock.class, result.getB().getClass());

        result = PolicyGuard.lockTarget(type, INSTANCENAME, uuid, dlcb);
        assertTrue(PolicyGuard.isLocked(type, INSTANCENAME, uuid));

        assertEquals(GuardResult.LOCK_DENIED, result.getA());
        assertNull(result.getB());

        // Test isLocked after lock removed
        PolicyGuard.unlockTarget(new DummyTargetLock());
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));

        // Test unlock after lock removed
        PolicyGuard.unlockTarget(new DummyTargetLock());
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
    }

    @Test
    public void testLockAlreadyLocked() {
        UUID uuid = UUID.randomUUID();
        TargetType type = TargetType.VM;

        // Test isLocked before and after lock added
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
        DummyLockCallback dlcb = new DummyLockCallback();
        LockResult<GuardResult, TargetLock> result = PolicyGuard.lockTarget(type, INSTANCENAME, uuid, dlcb);
        assertTrue(PolicyGuard.isLocked(type, INSTANCENAME, uuid));

        assertEquals(GuardResult.LOCK_ACQUIRED, result.getA());
        assertEquals(VMTargetLock.class, result.getB().getClass());

        result = PolicyGuard.lockTarget(type, INSTANCENAME, uuid, dlcb);
        assertTrue(PolicyGuard.isLocked(type, INSTANCENAME, uuid));

        assertEquals(GuardResult.LOCK_DENIED, result.getA());
        assertNull(result.getB());

        // Test isLocked after lock removed
        PolicyGuard.unlockTarget(new DummyTargetLock());
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
    }

    @Test
    public void testInnards() {

        DummyLockCallback dlcb = new DummyLockCallback();
        assertFalse(dlcb.isActive());
        assertFalse(dlcb.releaseLock());

        DummyTargetLock dtl = new DummyTargetLock();
        assertNull(dtl.getLockID());
        assertNull(dtl.getRequestID());
        assertEquals(INSTANCENAME, dtl.getTargetInstance());
        assertNull(dtl.getTargetType());
    }
}
