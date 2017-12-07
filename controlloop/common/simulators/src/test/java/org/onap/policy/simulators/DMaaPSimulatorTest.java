/*-
 * ============LICENSE_START=======================================================
 * simulators
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.simulators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.drools.http.server.HttpServletServer;
import org.onap.policy.drools.utils.LoggerUtil;
import org.onap.policy.rest.RESTManager;
import org.onap.policy.rest.RESTManager.Pair;

public class DMaaPSimulatorTest {

    @BeforeClass
    public static void setUpSimulator() {
        LoggerUtil.setLevel("ROOT", "INFO");
        LoggerUtil.setLevel("org.eclipse.jetty", "WARN");
        try {
            Util.buildDMaaPSim();
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

    @AfterClass
    public static void tearDownSimulator() {
        HttpServletServer.factory.destroy();
    }
    
    @Test
    public void testGetNoData() {
        int timeout = 1000;
        Pair <Integer, String> response = dmaapGet("myTopicNoData", timeout);
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        assertEquals("No topic", response.b);
    }
    
    @Test
    public void testSinglePost() {
        String myTopic = "myTopicSinglePost";
        String testData = "This is some test data";
        Pair<Integer, String> response = dmaapPost(myTopic, testData);
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        
        response = dmaapGet(myTopic, 1000);
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        assertEquals(testData, response.b);
    }
    
    @Test
    public void testOneTopicMultiPost() {
        String[] data = {"data point 1", "data point 2", "something random"};
        String myTopic = "myTopicMultiPost";
        Pair<Integer, String> response = dmaapPost(myTopic, data[0]);
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        
        response = dmaapPost(myTopic, data[1]);
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        
        response = dmaapPost(myTopic, data[2]);
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        
        response = dmaapGet(myTopic, 1000);
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        assertEquals(data[0], response.b);
        
        response = dmaapGet(myTopic, 1000);
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        assertEquals(data[1], response.b);
        
        response = dmaapGet(myTopic, 1000);
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        assertEquals(data[2], response.b);
    }
    
    @Test
    public void testMultiTopic() {
        String[][] data = {{"Topic one message one", "Topic one message two"}, {"Topic two message one", "Topic two message two"}};
        String[] topics = {"topic1", "topic2"};
        
        Pair<Integer, String> response = dmaapPost(topics[0], data[0][0]);
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        
        response = dmaapGet(topics[0], 1000);
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        assertEquals(data[0][0], response.b);
        
        response = dmaapGet(topics[1], 1000);
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        assertEquals("No topic", response.b);
        
        response = dmaapPost(topics[1], data[1][0]);
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        
        response = dmaapPost(topics[1], data[1][1]);
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        
        response = dmaapPost(topics[0], data[0][1]);
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        
        response = dmaapGet(topics[1], 1000);
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        assertEquals(data[1][0], response.b);
        
        response = dmaapGet(topics[0], 1000);
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        assertEquals(data[0][1], response.b);
        
        response = dmaapGet(topics[1], 1000);
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        assertEquals(data[1][1], response.b);
        
        response = dmaapGet(topics[0], 1000);
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        assertEquals("No Data", response.b);
    }
    
    @Test
    public void testResponseCode() {
        Pair<Integer, String> response = dmaapPost("myTopic", "myTopicData");
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        
        response = setStatus(503);
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        
        response = dmaapGet("myTopic", 500);
        assertNotNull(response);
        assertEquals(503, response.a.intValue());
        assertEquals("You got response code: 503", response.b);
        
        response = setStatus(202);
        assertNotNull(response);
        assertNotNull(response.a);
        assertNotNull(response.b);
        
        response = dmaapGet("myTopic", 500);
        assertNotNull(response);
        assertEquals(202, response.a.intValue());
        assertEquals("myTopicData", response.b);
    }
    
    private static Pair<Integer, String> dmaapGet (String topic, int timeout) {
        return dmaapGet(topic, "1", "1", timeout);
    }
    
    private static Pair<Integer, String> dmaapGet (String topic, String consumerGroup, String consumerId, int timeout) {
        String url = "http://localhost:" + Util.DMAAPSIM_SERVER_PORT + "/events/" + topic + "/" + consumerGroup + "/" + consumerId + "?timeout=" + timeout;
        return RESTManager.get(url, "", "", null);
    }
    
    private static Pair<Integer, String> dmaapPost (String topic, String data) {
        String url = "http://localhost:" + Util.DMAAPSIM_SERVER_PORT + "/events/" + topic;
        return RESTManager.post(url, "", "", null, "text/plain", data);
    }
    
    private static Pair<Integer, String> setStatus (int status) {
        String url = "http://localhost:" + Util.DMAAPSIM_SERVER_PORT + "/events/setStatus?statusCode=" + status;
        return RESTManager.post(url, "", "", null, "text/plain", "");
    }
}
