package org.oanp.policy.msb.client;

import org.onap.policy.msb.client.MSBServiceManager;
import org.onap.policy.msb.client.Node;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MSBServiceManagerTest {
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
