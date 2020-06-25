/*-
 * ============LICENSE_START=======================================================
 * util
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

package org.onap.policy.util;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.onap.policy.drools.core.PolicySession;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
public class DroolsSessionCommonSerializableTest {

    private DroolsSessionCommonSerializable droolsSessionCommonSerializable;

    @Test
    public void test() {
        Object object = new Object();
        droolsSessionCommonSerializable = new DroolsSessionCommonSerializable("drools", object);
        assertNotNull(droolsSessionCommonSerializable.toString());
    }

    @PrepareForTest(PolicySession.class)
    @Test
    public void testConstructorGetNullAdjunct() {
        PowerMockito.mockStatic(PolicySession.class);
        PolicySession mockPolicySession = Mockito.mock(PolicySession.class);
        Object mockObject = Mockito.mock(Object.class);

        when(PolicySession.getCurrentSession()).thenReturn(mockPolicySession);
        when(mockPolicySession.getAdjunct(any(Class.class))).thenReturn(null);

        droolsSessionCommonSerializable = new DroolsSessionCommonSerializable("testName", mockObject);

        verify(mockPolicySession).getAdjunct(any(Class.class));
        assertNotNull(droolsSessionCommonSerializable.toString());
    }

    @PrepareForTest(PolicySession.class)
    @Test
    public void testConstructorGetAdjunct() {
        PowerMockito.mockStatic(PolicySession.class);
        PolicySession mockPolicySession = Mockito.mock(PolicySession.class);
        Object mockObject = Mockito.mock(Object.class);

        when(PolicySession.getCurrentSession()).thenReturn(mockPolicySession);
        when(mockPolicySession.getAdjunct(any(Class.class))).thenReturn(mockObject);

        droolsSessionCommonSerializable = new DroolsSessionCommonSerializable("testName", mockObject);

        verify(mockPolicySession).getAdjunct(any(Class.class));
        assertNotNull(droolsSessionCommonSerializable.toString());
    }

    @Test
    public void testReadResolve() throws Exception {
        Object mockObject = Mockito.mock(Object.class);
        droolsSessionCommonSerializable = new DroolsSessionCommonSerializable("testName", mockObject);

        assertNotNull(Whitebox.invokeMethod(droolsSessionCommonSerializable, "readResolve"));
        assertNotNull(droolsSessionCommonSerializable.toString());
    }
}
