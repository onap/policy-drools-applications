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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.drools.core.lock.PolicyResourceLockManager;
import org.onap.policy.guard.PolicyGuard.Factory;
import org.onap.policy.guard.PolicyGuard.LockResult;
import org.onap.policy.guard.impl.PNFTargetLock;
import org.onap.policy.guard.impl.VMTargetLock;
import org.onap.policy.guard.impl.VNFTargetLock;

public class PolicyGuardTest {
    private static final String INSTANCENAME = "targetInstance";
    private static final int LOCK_SEC = 10;
    
    private static Factory saveFactory;
    
    private Factory factory;
    private PolicyResourceLockManager mgr;
    private UUID uuid;
    private DummyLockCallback dlcb;

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
        private TargetType type;
        private UUID reqid;
        
        public DummyTargetLock(TargetType type, UUID reqid) {
            this.type = type;
            this.reqid = reqid;
        }
        
        @Override
        public UUID getLockID() {
            return null;
        }

        @Override
        public TargetType getTargetType() {
            return type;
        }

        @Override
        public String getTargetInstance() {
            return INSTANCENAME;
        }

        @Override
        public UUID getRequestID() {
            return reqid;
        }
    }
    
    @BeforeClass
    public static void setUpBeforeClass() {
        saveFactory = PolicyGuard.getFactory();
    }
    
    @AfterClass
    public static void tearDownAfterClass() {
        PolicyGuard.setFactory(saveFactory);
    }
    
    @Before
    public void setUp() {
        mgr = new PolicyResourceLockManager() {
            // only way to access the constructor            
        };
        
        factory = new Factory() {
            @Override
            public PolicyResourceLockManager getManager() {
                return mgr;
            }
        };
        
        uuid = UUID.randomUUID();
        dlcb = new DummyLockCallback();

        PolicyGuard.setFactory(factory);
    }

    @Test
    public void testLockVm() {
        TargetType type = TargetType.VM;

        // Test isLocked before and after lock added
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
        LockResult<GuardResult, TargetLock> result = PolicyGuard.lockTarget(type, INSTANCENAME, uuid, dlcb, LOCK_SEC);
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
        PolicyGuard.unlockTarget(new DummyTargetLock(type, uuid));
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
    }

    @Test
    public void testLockPnf() {
        TargetType type = TargetType.PNF;

        // Test isLocked before and after lock added
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
        LockResult<GuardResult, TargetLock> result = PolicyGuard.lockTarget(type, INSTANCENAME, uuid, dlcb, LOCK_SEC);
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
        PolicyGuard.unlockTarget(new DummyTargetLock(type, uuid));
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
    }


    @Test
    public void testLockVnf() {
        TargetType type = TargetType.VNF;

        // Test isLocked before and after lock added
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
        LockResult<GuardResult, TargetLock> result = PolicyGuard.lockTarget(type, INSTANCENAME, uuid, dlcb, LOCK_SEC);
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
        PolicyGuard.unlockTarget(new DummyTargetLock(type, uuid));
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
    }

    @Test
    public void testLockVfc() {
        TargetType type = TargetType.VFC;

        // Test isLocked before and after lock added
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
        LockResult<GuardResult, TargetLock> result = PolicyGuard.lockTarget(type, INSTANCENAME, uuid, dlcb, LOCK_SEC);
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));

        assertEquals(GuardResult.LOCK_EXCEPTION, result.getA());
        assertNull(result.getB());

        // Test isLocked after lock removed
        PolicyGuard.unlockTarget(new DummyTargetLock(type, uuid));
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
    }

    @Test
    public void testUnLockNotLocked() {
        TargetType type = TargetType.VM;

        // Test isLocked before and after lock added
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
        LockResult<GuardResult, TargetLock> result = PolicyGuard.lockTarget(type, INSTANCENAME, uuid, dlcb, LOCK_SEC);
        assertTrue(PolicyGuard.isLocked(type, INSTANCENAME, uuid));

        assertEquals(GuardResult.LOCK_ACQUIRED, result.getA());
        assertEquals(VMTargetLock.class, result.getB().getClass());

        // Test isLocked after lock removed
        PolicyGuard.unlockTarget(new DummyTargetLock(type, uuid));
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));

        // Test unlock after lock removed
        PolicyGuard.unlockTarget(new DummyTargetLock(type, uuid));
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
    }

    @Test
    public void testLockAlreadyLocked() {
        TargetType type = TargetType.VM;

        // Test isLocked before and after lock added
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
        LockResult<GuardResult, TargetLock> result = PolicyGuard.lockTarget(type, INSTANCENAME, uuid, dlcb, LOCK_SEC);
        assertTrue(PolicyGuard.isLocked(type, INSTANCENAME, uuid));

        assertEquals(GuardResult.LOCK_ACQUIRED, result.getA());
        assertEquals(VMTargetLock.class, result.getB().getClass());

        UUID uuid2 = UUID.randomUUID();
        result = PolicyGuard.lockTarget(type, INSTANCENAME, uuid2, dlcb, LOCK_SEC);
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid2));

        assertTrue(PolicyGuard.isLocked(type, INSTANCENAME, uuid));

        assertEquals(GuardResult.LOCK_DENIED, result.getA());
        assertNull(result.getB());

        // Test isLocked after lock removed
        PolicyGuard.unlockTarget(new DummyTargetLock(type, uuid));
        assertFalse(PolicyGuard.isLocked(type, INSTANCENAME, uuid));
    }

    @Test
    public void testInnards() {
        TargetType type = TargetType.VM;

        assertFalse(dlcb.isActive());
        assertFalse(dlcb.releaseLock());

        DummyTargetLock dtl = new DummyTargetLock(type, uuid);
        assertNull(dtl.getLockID());
        assertEquals(uuid, dtl.getRequestID());
        assertEquals(INSTANCENAME, dtl.getTargetInstance());
        assertEquals(type, dtl.getTargetType());
    }

    @Test
    public void testManagerLockTarget() throws Exception {
        TargetType type = TargetType.VM;
        
        mgr = mock(PolicyResourceLockManager.class);

        LockResult<GuardResult, TargetLock> result;

        // acquired
        when(mgr.lock(anyString(), anyString(), anyInt())).thenReturn(true);
        result = PolicyGuard.lockTarget(type, INSTANCENAME, uuid, dlcb, LOCK_SEC);
        verify(mgr).lock(INSTANCENAME, type.toString()+":"+uuid.toString(), LOCK_SEC);
        assertEquals(GuardResult.LOCK_ACQUIRED, result.getA());
        assertEquals(VMTargetLock.class, result.getB().getClass());

        // denied
        when(mgr.lock(anyString(), anyString(), anyInt())).thenReturn(false);
        result = PolicyGuard.lockTarget(type, INSTANCENAME, uuid, dlcb, LOCK_SEC+2);
        verify(mgr).lock(INSTANCENAME, type.toString()+":"+uuid.toString(), LOCK_SEC+2);
        assertEquals(GuardResult.LOCK_DENIED, result.getA());
        assertNull(result.getB());
    }

    @Test
    public void testManagerLockTargetTargetLockInt() throws Exception {
        TargetType type = TargetType.VM;
        DummyTargetLock lock = new DummyTargetLock(type, uuid);
        
        mgr = mock(PolicyResourceLockManager.class);

        // acquired
        when(mgr.lock(anyString(), anyString(), anyInt())).thenReturn(true);
        assertEquals(GuardResult.LOCK_ACQUIRED, PolicyGuard.lockTarget(lock, LOCK_SEC));
        verify(mgr).lock(INSTANCENAME, type.toString()+":"+uuid.toString(), LOCK_SEC);

        // denied
        when(mgr.lock(anyString(), anyString(), anyInt())).thenReturn(false);
        assertEquals(GuardResult.LOCK_DENIED, PolicyGuard.lockTarget(lock, LOCK_SEC+1));
        verify(mgr).lock(INSTANCENAME, type.toString()+":"+uuid.toString(), LOCK_SEC+1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMakeOwner_NullTargetType() {
        PolicyGuard.isLocked(null, INSTANCENAME, uuid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMakeOwner_NullReqId() {
        PolicyGuard.isLocked(TargetType.PNF, INSTANCENAME, null);
    }
}
