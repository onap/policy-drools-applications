/*-
 * ============LICENSE_START=======================================================
 * guard
 * ================================================================================
 * Copyright (C) 2017-2019 AT&T Intellectual Property. All rights reserved.
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.drools.core.lock.Lock;
import org.onap.policy.drools.core.lock.LockCallback;
import org.onap.policy.drools.core.lock.PolicyResourceLockManager;
import org.onap.policy.guard.PolicyGuard.Factory;
import org.onap.policy.guard.impl.PnfTargetLock;
import org.onap.policy.guard.impl.VmTargetLock;
import org.onap.policy.guard.impl.VnfTargetLock;

public class PolicyGuardTest {
    private static final String INSTANCENAME = "targetInstance";
    private static final int LOCK_SEC = 10;

    private static Factory saveFactory;

    private Factory factory;
    private PolicyResourceLockManager mgr;
    private UUID uuid;
    private LockCallback dlcb;
    private ArrayList<Lock> locks;

    @BeforeClass
    public static void setUpBeforeClass() {
        saveFactory = PolicyGuard.getFactory();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        PolicyGuard.setFactory(saveFactory);
    }

    /**
     * Setup method.
     */
    @Before
    public void setUp() {
        locks = new ArrayList<>(2);

        mgr = mock(PolicyResourceLockManager.class);
        when(mgr.lock(any(), any(), any(), anyInt(), anyBoolean())).then(ans -> {
            Lock lock = mock(Lock.class);
            when(lock.free()).thenReturn(true);
            when(lock.extend(anyInt())).thenReturn(true);
            locks.add(lock);
            return lock;
        });

        factory = mock(Factory.class);
        when(factory.getManager()).thenReturn(mgr);

        uuid = UUID.randomUUID();
        dlcb = mock(LockCallback.class);

        PolicyGuard.setFactory(factory);
    }

    @Test
    public void testLockVm() {

        VmTargetLock vtl = testLock(TargetType.VM, VmTargetLock.class);

        assertNotNull(vtl.getLockId());
        assertEquals(INSTANCENAME, vtl.getTargetInstance());
        assertEquals(TargetType.VM, vtl.getTargetType());
        assertNotNull(vtl.getRequestId());
    }

    @Test
    public void testLockPnf() {
        PnfTargetLock ptl = testLock(TargetType.PNF, PnfTargetLock.class);

        assertNotNull(ptl.getLockId());
        assertEquals(INSTANCENAME, ptl.getTargetInstance());
        assertEquals(TargetType.PNF, ptl.getTargetType());
        assertNotNull(ptl.getRequestId());
    }

    @Test
    public void testLockVnf() {
        VnfTargetLock vtl = testLock(TargetType.VNF, VnfTargetLock.class);

        assertNotNull(vtl.getLockId());
        assertEquals(INSTANCENAME, vtl.getTargetInstance());
        assertEquals(TargetType.VNF, vtl.getTargetType());
        assertNotNull(vtl.getRequestId());
    }

    @Test
    public void testLockVfModule() {
        VnfTargetLock vtl = testLock(TargetType.VFMODULE, VnfTargetLock.class);

        assertNotNull(vtl.getLockId());
        assertEquals(INSTANCENAME, vtl.getTargetInstance());
        assertEquals(TargetType.VFMODULE, vtl.getTargetType());
        assertNotNull(vtl.getRequestId());
    }

    private <T> T testLock(TargetType type, Class<T> clazz) {
        TargetLock lock = PolicyGuard.lockTarget(type, INSTANCENAME, uuid, dlcb, LOCK_SEC);
        assertEquals(clazz, lock.getClass());
        assertEquals(1, locks.size());

        assertTrue(lock.extend(LOCK_SEC));
        verify(locks.get(0)).extend(LOCK_SEC);

        assertTrue(PolicyGuard.unlockTarget(lock));
        verify(locks.get(0)).free();

        return clazz.cast(lock);
    }

    @Test
    public void testCreateTargetLockTargetTypeStringUuidLockCallback() {
        TargetLock lock = PolicyGuard.createTargetLock(TargetType.VM, INSTANCENAME, uuid, dlcb);
        verify(dlcb).lockAvailable(any());

        // should not have created a real lock
        assertTrue(locks.isEmpty());

        assertTrue(lock.extend(LOCK_SEC));
        verify(dlcb, times(2)).lockAvailable(any());

        assertTrue(PolicyGuard.unlockTarget(lock));

        assertFalse(lock.extend(LOCK_SEC));

    }

    @Test
    public void testLockTargetTargetTypeStringUuidLockCallbackInt_InvalidType() {
        TargetLock lock = PolicyGuard.lockTarget(TargetType.VFC, INSTANCENAME, uuid, dlcb, LOCK_SEC);

        assertNull(lock);

        // should have released the real lock back
        assertEquals(1, locks.size());
        verify(locks.get(0)).free();
    }

    @Test
    public void testLockTargetTargetLockInt() {
        TargetLock lock = PolicyGuard.lockTarget(TargetType.VM, INSTANCENAME, uuid, dlcb, LOCK_SEC);
        assertEquals(1, locks.size());

        assertTrue(PolicyGuard.lockTarget(lock, LOCK_SEC));
        verify(locks.get(0)).extend(LOCK_SEC);

        assertTrue(PolicyGuard.unlockTarget(lock));

        when(locks.get(0).extend(anyInt())).thenReturn(false);

        // extend should fail now
        assertFalse(PolicyGuard.lockTarget(lock, LOCK_SEC));
        verify(locks.get(0), times(2)).extend(LOCK_SEC);
    }

    @Test
    public void testUnlock() {
        TargetLock lock = PolicyGuard.lockTarget(TargetType.VM, INSTANCENAME, uuid, dlcb, LOCK_SEC);
        assertEquals(1, locks.size());

        assertTrue(PolicyGuard.unlockTarget(lock));
        verify(locks.get(0)).free();
    }
}
