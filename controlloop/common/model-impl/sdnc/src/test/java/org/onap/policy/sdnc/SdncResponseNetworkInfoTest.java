/*
 * ============LICENSE_START=======================================================
 * ONAP : POLICY
 * ================================================================================
 * Copyright 2019 IBM
 *=================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.policy.sdnc;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class SdncResponseNetworkInfoTest {

    SdncResponseNetworkInfo sdncResponseNetworkInfo;

    @Before
    public void setUp() {
        sdncResponseNetworkInfo = new SdncResponseNetworkInfo();
    }

    @Test
    public void testGetInstanceId() {
        sdncResponseNetworkInfo.setInstanceId("instanceId");
        assertEquals("instanceId", sdncResponseNetworkInfo.getInstanceId());
    }

    @Test
    public void testGetObjectPath() {
        sdncResponseNetworkInfo.setObjectPath("ObjectPath");
        assertEquals("ObjectPath", sdncResponseNetworkInfo.getObjectPath());
    }
}
