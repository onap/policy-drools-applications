/*-
 * ============LICENSE_START=======================================================
 * Copyright 2017-2018 ZTE, Inc. and others.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.msb.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.msb.sdk.discovery.common.RouteException;
import org.onap.msb.sdk.discovery.entity.MicroServiceFullInfo;
import org.onap.msb.sdk.discovery.entity.NodeInfo;
import org.onap.msb.sdk.httpclient.msb.MSBServiceClient;

public class MsbServiceManagerTest {
    @Mock
    private MSBServiceClient msbClient;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private MsbServiceManager msbManager;

    public MsbServiceManagerTest() {}

    @BeforeClass
    public static void setUpClass() {}

    @AfterClass
    public static void tearDownClass() {}

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        msbManager = new MsbServiceManager(msbClient);
    }

    @After
    public void tearDown() {}

    @Test
    public void testByActor() throws RouteException, UnknownHostException {
        MicroServiceFullInfo serviceInfo =
                MsbServiceManagerTest.build(InetAddress.getLocalHost().getHostAddress(), "8843");
        when(msbClient.queryMicroServiceInfo("aai-search", "v11")).thenReturn(serviceInfo);
        Node node = msbManager.getNode("AAI");
        assertNotNull(node);
        serviceInfo = MsbServiceManagerTest.build(InetAddress.getLocalHost().getHostAddress(), "8840");
        when(msbClient.queryMicroServiceInfo("so", "v2")).thenReturn(serviceInfo);
        node = msbManager.getNode("SO");
        assertNotNull(node);

        serviceInfo = MsbServiceManagerTest.build(InetAddress.getLocalHost().getHostAddress(), "8082");
        when(msbClient.queryMicroServiceInfo("nfvo-nslcm", "v1")).thenReturn(serviceInfo);
        node = msbManager.getNode("VFC");
        assertNotNull(node);

    }

    @Test
    public void testByActor_when_actorNotExist_returnNull() throws RouteException, UnknownHostException {
        MicroServiceFullInfo serviceInfo =
                MsbServiceManagerTest.build(InetAddress.getLocalHost().getHostAddress(), "8843");
        when(msbClient.queryMicroServiceInfo("aai-search", "v11")).thenReturn(serviceInfo);
        Node node = msbManager.getNode("DDD");
        assertNull(node);
    }

    @Test
    public void testByServiceNameAndVersion() throws RouteException, UnknownHostException {
        MicroServiceFullInfo serviceInfo =
                MsbServiceManagerTest.build(InetAddress.getLocalHost().getHostAddress(), "8843");
        when(msbClient.queryMicroServiceInfo("aai-search", "v11")).thenReturn(serviceInfo);
        Node node = msbManager.getNode("aai-search", "v11");
        assertNotNull(node);
    }

    @Test
    public void testByServiceNameAndVersion_when_serice_notRegistedToMsb() throws RouteException, UnknownHostException {
        MsbServiceManagerTest.build(InetAddress.getLocalHost().getHostAddress(), "8843");
        when(msbClient.queryMicroServiceInfo("aai-search", "v11")).thenThrow(new RouteException());
        Node node = msbManager.getNode("aai-search", "v11");
        assertNotNull(node);
        assertTrue(node.getName() == "aai-search");
        assertTrue(node.getIp() == null);
        assertTrue(node.getPort() == null);
    }

    @Test
    public void testReadMsbPolicyProperites_noPropertyFileSpecifed_throwsException()
            throws MsbServiceException, IOException {
        expectedException.expect(MsbServiceException.class);
        expectedException.expectMessage("No msb.policy.properties specified.");
        System.clearProperty("msb.policy.properties");
        msbManager = new MsbServiceManager();
    }

    @Test
    public void testReadMsbPolicyProperites_propertyFileDoesNotExist_throwsException()
            throws MsbServiceException, IOException {
        expectedException.expect(MsbServiceException.class);
        expectedException.expectMessage("No msb.policy.properties specified.");
        System.setProperty("msb.policy.properties", "nonExistingPropertyFile.txt");
        msbManager = new MsbServiceManager();
        System.clearProperty("msb.policy.properties");
    }

    @Test
    public void testReadMsbPolicyProperites_propertyFileExists() throws MsbServiceException, IOException,
            NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        System.setProperty("msb.policy.properties", "src/test/resources/msbPropertyFile.properties");
        msbManager = new MsbServiceManager();
        System.clearProperty("msb.policy.properties");

        Field factoryField = msbManager.getClass().getDeclaredField("factory");
        factoryField.setAccessible(true);
        MsbServiceFactory msbServiceFactory = (MsbServiceFactory) factoryField.get(msbManager);

        Field msbClientField = msbServiceFactory.getClass().getDeclaredField("msbClient");
        msbClientField.setAccessible(true);
        MSBServiceClient msbClient = (MSBServiceClient) msbClientField.get(msbServiceFactory);
        assertEquals("127.0.0.1:20", msbClient.getMsbSvrAddress());
    }

    /**
     * Build a micro service info.
     * 
     * @param ip the up address
     * @param port the port number
     * @return the MicroServiceFullInfo
     */
    public static MicroServiceFullInfo build(String ip, String port) {
        Set<NodeInfo> nodes = new HashSet<NodeInfo>();
        NodeInfo node = new NodeInfo();
        node.setPort(port);
        node.setIp(ip);
        nodes.add(node);
        MicroServiceFullInfo serviceInfo = new MicroServiceFullInfo();
        serviceInfo.setNodes(nodes);
        return serviceInfo;
    }
}
