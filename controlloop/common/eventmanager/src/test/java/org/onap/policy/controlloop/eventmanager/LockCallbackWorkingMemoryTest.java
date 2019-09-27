/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.controlloop.eventmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.drools.core.WorkingMemory;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.runtime.rule.FactHandle;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.drools.core.lock.Lock;

public class LockCallbackWorkingMemoryTest {
    private static final String MY_NAME = "my-name";

    @Mock
    private WorkingMemory workingMemory;

    @Mock
    private Lock lock;

    @Mock
    private FactHandle fact;

    private LockCallbackWorkingMemory callback;


    /**
     * Initializes mocks and creates a call-back.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(workingMemory.getFactHandle(lock)).thenReturn(fact);

        callback = new LockCallbackWorkingMemory(MY_NAME, workingMemory);
    }

    @Test
    public void testLockCallbackWorkingMemory() {
        assertEquals(MY_NAME, callback.getName());
        assertSame(workingMemory, callback.getWorkingMemory());
    }

    @Test
    public void testLockAvailable() {
        callback.lockAvailable(lock);
        verify(workingMemory).update(fact, lock);

        // "remove" from working memory
        when(workingMemory.getFactHandle(lock)).thenReturn(null);
        callback.lockAvailable(lock);

        // should be no additional calls
        verify(workingMemory).update(any(), any());
    }

    @Test
    public void testLockUnavailable() {
        callback.lockUnavailable(lock);
        verify(workingMemory).update(fact, lock);

        // "remove" from working memory
        when(workingMemory.getFactHandle(lock)).thenReturn(null);
        callback.lockUnavailable(lock);

        // should be no additional calls
        verify(workingMemory).update(any(), any());
    }

}
