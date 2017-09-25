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

import org.junit.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.msb.sdk.discovery.common.RouteException;
import org.onap.msb.sdk.discovery.entity.MicroServiceFullInfo;
import org.onap.msb.sdk.discovery.entity.NodeInfo;
import org.onap.msb.sdk.httpclient.msb.MSBServiceClient;
import org.onap.policy.msb.client.MSBServiceManager;
import org.onap.policy.msb.client.Node;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class MSBServiceManagerTest {
    @Mock
    private MSBServiceClient msbClient;

    private MSBServiceManager msbManager;

    public MSBServiceManagerTest(){}

    @BeforeClass
    public static void setUpClass(){}

    @AfterClass
    public static void tearDownClass(){}

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        msbManager = new MSBServiceManager(msbClient);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testByActor () throws RouteException {
        MicroServiceFullInfo serviceInfo = this.build("192.168.12.10","8843");
        when(msbClient.queryMicroServiceInfo("aai-search","v11")).thenReturn(serviceInfo);
        Node node = msbManager.getNode("AAI");
        assertNotNull(node);
        serviceInfo = this.build("192.168.12.11","8840");
        when(msbClient.queryMicroServiceInfo("so","v2")).thenReturn(serviceInfo);
        node = msbManager.getNode("SO");
        assertNotNull(node);

        serviceInfo = this.build("192.168.12.12","8082");
        when(msbClient.queryMicroServiceInfo("nfvo-nslcm","v1")).thenReturn(serviceInfo);
        node = msbManager.getNode("VFC");
        assertNotNull(node);

    }

    @Test
    public void testByActor_when_actorNotExist_returnNull () throws RouteException {
        MicroServiceFullInfo serviceInfo = this.build("192.168.12.10","8843");
        when(msbClient.queryMicroServiceInfo("aai-search","v11")).thenReturn(serviceInfo);
        Node node = msbManager.getNode("DDD");
        assertNull(node);
    }

    @Test
    public void testByServiceNameAndVersion () throws RouteException {
        MicroServiceFullInfo serviceInfo = this.build("192.168.12.10","8843");
        when(msbClient.queryMicroServiceInfo("aai-search","v11")).thenReturn(serviceInfo);
        Node node = msbManager.getNode("aai-search","v11");
        assertNotNull(node);
    }

    @Test
    public void testByServiceNameAndVersion_when_serice_notRegistedToMSB () throws RouteException {
        MicroServiceFullInfo serviceInfo = this.build("192.168.12.10","8843");
        when(msbClient.queryMicroServiceInfo("aai-search","v11")).thenThrow(new RouteException());
        Node node = msbManager.getNode("aai-search","v11");
        assertNotNull(node);
        assertTrue(node.getName() == "aai-search");
        assertTrue(node.getIp() == null);
        assertTrue(node.getPort() == null);
    }

    public static MicroServiceFullInfo build(String ip,String port){
        MicroServiceFullInfo serviceInfo = new MicroServiceFullInfo();
        Set<NodeInfo> nodes = new HashSet<NodeInfo>();
        NodeInfo node= new NodeInfo();
        node.setPort(port);
        node.setIp(ip);
        nodes.add(node);
        serviceInfo.setNodes(nodes);
        return serviceInfo;
    }

}
