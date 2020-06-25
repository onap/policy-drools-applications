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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.drools.core.lock.Lock;
import org.onap.policy.drools.core.lock.LockCallback;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.m2.base.Transaction;

public class LockAdjunctTest {

    public class TestOwner implements LockCallback {

        @Override
        public void lockAvailable(Lock arg0) {
            return;
        }

        @Override
        public void lockUnavailable(Lock arg0) {
            return;
        }
    }

    private LockCallback owner;
    private Lock lock;

    @BeforeClass
    public static void start() {
        PolicyEngineConstants.getManager().configure(new Properties());
        PolicyEngineConstants.getManager().start();
    }

    @AfterClass
    public static void stop() {
        PolicyEngineConstants.getManager().stop();
    }

    @Test
    public void testLockAdjunct() {
        owner = new TestOwner();
        lock = PolicyEngineConstants.getManager().createLock("key", "ownerKey", 60, owner, false);
        LockAdjunct lockA = new LockAdjunct();

        assertNotNull(lockA);

        lockA.lockUnavailable(lock);
        assertTrue(lock.isActive());
        assertTrue(lockA.getLock(null, "key", "ownerKey", false));
        LockAdjunct lockB = new LockAdjunct();
        assertFalse(lockB.getLock(null, "key", "ownerKey", false));
        assertTrue(lock.free());

        lockB.lockAvailable(lock);
        assertFalse(lock.isActive());
        assertTrue(lockB.getLock(null, "key", "ownerKey", false));
        assertFalse(lock.free());

        UUID uuid = UUID.randomUUID();
        Transaction transaction = new Transaction(null, "1111", uuid, null);
        lockA.cleanup(transaction);
    }
}
