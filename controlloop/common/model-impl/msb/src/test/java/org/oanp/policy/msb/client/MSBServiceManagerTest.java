/*******************************************************************************
 * Copyright 2017 ZTE, Inc. and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.oanp.policy.msb.client;

import org.onap.policy.msb.client.MSBServiceManager;
import org.onap.policy.msb.client.Node;
import org.junit.Test;
import org.junit.Ignore;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MSBServiceManagerTest {
    @Ignore
    @Test
    public void testByActor (){
        String MSB_IP="10.93.132.38";
        int MSB_Port=10081;
        MSBServiceManager msbManager = new MSBServiceManager(MSB_IP, MSB_Port);
        Node node = msbManager.getNode("AAI");
        assertNotNull(node);

        node = msbManager.getNode("SO");
        assertNotNull(node);

        node = msbManager.getNode("VFC");
        assertNotNull(node);
        node = msbManager.getNode("DD");
        assertNull(node);
    }
    @Ignore
    @Test
    public void testByServiceNameAndVersion (){
        String MSB_IP="10.93.132.38";
        int MSB_Port=10081;
        MSBServiceManager msbManager = new MSBServiceManager(MSB_IP, MSB_Port);
        Node node = msbManager.getNode("aai-search","v11");
        assertNotNull(node);
        node = msbManager.getNode("so","v2");
        assertNotNull(node);
        node = msbManager.getNode("nfvo-nslcm","v1");
        assertNotNull(node);
    }
}
