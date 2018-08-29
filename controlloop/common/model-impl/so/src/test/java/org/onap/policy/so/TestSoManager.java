/*-
 * ============LICENSE_START=======================================================
 * TestSOManager
 * ================================================================================
 * Copyright (C) 2018 Ericsson. All rights reserved.
 * Modifications Copyright (C) 2018 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.so;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.Future;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.drools.core.WorkingMemory;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.drools.system.PolicyEngine;

public class TestSoManager {
    private static final String BASE_URI = "http://localhost:46553/TestSOManager";
    private static final String BASE_SO_URI = BASE_URI + "/SO";
    private static HttpServer server;

    /**
     * Set up test class.
     */
    @BeforeClass
    public static void setUp() throws IOException {
        final ResourceConfig rc = new ResourceConfig(TestSoDummyServer.class);
        //Grizzly by default doesn't allow payload for HTTP methods (ex: DELETE), for which HTTP spec doesn't
        // explicitly state that.
        //allow it before starting the server
        server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc, false);
        server.getServerConfiguration().setAllowPayloadForUndefinedHttpMethods(true);
        server.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void testGrizzlyServer() throws ClientProtocolException, IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("http://localhost:46553/TestSOManager/SO/Stats");
        CloseableHttpResponse response = httpclient.execute(httpGet);

        String returnBody = EntityUtils.toString(response.getEntity(), "UTF-8");
        assertTrue(returnBody.matches("^\\{\"GET\": [0-9]*,\"STAT\": [0-9]*,\"POST\": [0-9]*,\"PUT\": [0-9]*,"
                + "\"DELETE\": [0-9]*\\}$"));
    }

    @Test
    public void testServiceInstantiation() throws IOException {
        SOManager manager = new SOManager();
        assertNotNull(manager);
        manager.setRestGetTimeout(100);

        SOResponse response = manager.createModuleInstance("http:/localhost:99999999", BASE_SO_URI, "sean",
                "citizen", null);
        assertNull(response);

        response = manager.createModuleInstance(BASE_SO_URI + "/serviceInstantiation/v7", BASE_SO_URI, "sean",
                        "citizen", null);
        assertNull(response);

        response = manager.createModuleInstance(BASE_SO_URI + "/serviceInstantiation/v7", BASE_SO_URI, "sean",
                        "citizen", new SORequest());
        assertNull(response);

        SORequest request = new SORequest();
        request.setRequestId(UUID.randomUUID());
        request.setRequestScope("Test");
        request.setRequestType("ReturnBadJson");
        request.setStartTime("2018-03-23 16:31");
        request.setRequestStatus(new SORequestStatus());
        request.getRequestStatus().setRequestState("ONGOING");

        response = manager.createModuleInstance(BASE_SO_URI + "/serviceInstantiation/v7", BASE_SO_URI, "sean",
                        "citizen", request);
        assertNull(response);

        request.setRequestType("ReturnCompleted");
        response = manager.createModuleInstance(BASE_SO_URI + "/serviceInstantiation/v7", BASE_SO_URI, "sean",
                        "citizen", request);
        assertNotNull(response);
        assertEquals("COMPLETE", response.getRequest().getRequestStatus().getRequestState());

        request.setRequestType("ReturnFailed");
        response = manager.createModuleInstance(BASE_SO_URI + "/serviceInstantiation/v7", BASE_SO_URI, "sean",
                        "citizen", request);
        assertNotNull(response);
        assertEquals("FAILED", response.getRequest().getRequestStatus().getRequestState());

        // Use scope to set the number of iterations we'll wait for

        request.setRequestType("ReturnOnging200");
        request.setRequestScope(new Integer(10).toString());
        response = manager.createModuleInstance(BASE_SO_URI + "/serviceInstantiation/v7", BASE_SO_URI, "sean",
                        "citizen", request);
        assertNotNull(response);
        assertNotNull(response.getRequest());
        assertEquals("COMPLETE", response.getRequest().getRequestStatus().getRequestState());

        request.setRequestType("ReturnOnging202");
        request.setRequestScope(new Integer(20).toString());
        response = manager.createModuleInstance(BASE_SO_URI + "/serviceInstantiation/v7", BASE_SO_URI, "sean",
                        "citizen", request);
        assertNotNull(response);
        assertNotNull(response.getRequest());
        assertEquals("COMPLETE", response.getRequest().getRequestStatus().getRequestState());

        // Test timeout after 20 attempts for a response
        request.setRequestType("ReturnOnging202");
        request.setRequestScope(new Integer(21).toString());
        response = manager.createModuleInstance(BASE_SO_URI + "/serviceInstantiation/v7", BASE_SO_URI, "sean",
                        "citizen", request);
        assertNull(response);

        // Test bad response after 3 attempts for a response
        request.setRequestType("ReturnBadAfterWait");
        request.setRequestScope(new Integer(3).toString());
        response = manager.createModuleInstance(BASE_SO_URI + "/serviceInstantiation/v7", BASE_SO_URI, "sean",
                        "citizen", request);
        assertNull(response);
    }

    @Test
    public void testVfModuleCreation() throws IOException {
        SOManager manager = new SOManager();
        assertNotNull(manager);
        manager.setRestGetTimeout(100);

        PolicyEngine.manager.setEnvironmentProperty("so.username", "sean");
        PolicyEngine.manager.setEnvironmentProperty("so.password", "citizen");

        WorkingMemory wm = new DummyWorkingMemory();

        SORequest soRequest = new SORequest();
        soRequest.setOperationType(SOOperationType.SCALE_OUT);
        PolicyEngine.manager.setEnvironmentProperty("so.url", "http:/localhost:99999999");
        Future<SOResponse> asyncRestCallFuture = manager.asyncSORestCall(UUID.randomUUID().toString(), wm,
                        UUID.randomUUID().toString(), UUID.randomUUID().toString(), soRequest);
        try {
            SOResponse response = asyncRestCallFuture.get();
            assertEquals(999, response.getHttpResponseCode());
        } catch (Exception e) {
            fail("test should not throw an exception");
        }

        PolicyEngine.manager.setEnvironmentProperty("so.url", BASE_SO_URI);
        asyncRestCallFuture = manager.asyncSORestCall(UUID.randomUUID().toString(), wm, UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(), soRequest);
        try {
            SOResponse response = asyncRestCallFuture.get();
            assertEquals(999, response.getHttpResponseCode());
        } catch (Exception e) {
            fail("test should not throw an exception");
        }

        SORequest request = new SORequest();
        request.setRequestId(UUID.randomUUID());
        request.setRequestScope("Test");
        request.setRequestType("ReturnBadJson");
        request.setStartTime("2018-03-23 16:31");
        request.setRequestStatus(new SORequestStatus());
        request.getRequestStatus().setRequestState("ONGOING");
        request.setOperationType(SOOperationType.SCALE_OUT);

        asyncRestCallFuture = manager.asyncSORestCall(UUID.randomUUID().toString(), wm, UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(), request);
        try {
            SOResponse response = asyncRestCallFuture.get();
            assertEquals(999, response.getHttpResponseCode());
        } catch (Exception e) {
            fail("test should not throw an exception");
        }

        request.setRequestType("ReturnCompleted");

        asyncRestCallFuture = manager.asyncSORestCall(UUID.randomUUID().toString(), wm, UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(), request);
        try {
            SOResponse response = asyncRestCallFuture.get();
            assertEquals("COMPLETE", response.getRequest().getRequestStatus().getRequestState());
        } catch (Exception e) {
            fail("test should not throw an exception");
        }

        request.setRequestType("ReturnFailed");
        asyncRestCallFuture = manager.asyncSORestCall(UUID.randomUUID().toString(), wm, UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(), request);
        try {
            SOResponse response = asyncRestCallFuture.get();
            assertEquals("FAILED", response.getRequest().getRequestStatus().getRequestState());
        } catch (Exception e) {
            fail("test should not throw an exception");
        }

        // Use scope to set the number of iterations we'll wait for

        request.setRequestType("ReturnOnging200");
        request.setRequestScope(new Integer(10).toString());
        asyncRestCallFuture = manager.asyncSORestCall(UUID.randomUUID().toString(), wm, UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(), request);
        try {
            SOResponse response = asyncRestCallFuture.get();
            assertNotNull(response.getRequest());
            assertEquals("COMPLETE", response.getRequest().getRequestStatus().getRequestState());
        } catch (Exception e) {
            fail("test should not throw an exception");
        }

        request.setRequestType("ReturnOnging202");
        request.setRequestScope(new Integer(20).toString());
        asyncRestCallFuture = manager.asyncSORestCall(UUID.randomUUID().toString(), wm, UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(), request);
        try {
            SOResponse response = asyncRestCallFuture.get();
            assertNotNull(response.getRequest());
            assertEquals("COMPLETE", response.getRequest().getRequestStatus().getRequestState());
        } catch (Exception e) {
            fail("test should not throw an exception");
        }

        // Test timeout after 20 attempts for a response
        request.setRequestType("ReturnOnging202");
        request.setRequestScope(new Integer(21).toString());
        asyncRestCallFuture = manager.asyncSORestCall(UUID.randomUUID().toString(), wm, UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(), request);
        try {
            SOResponse response = asyncRestCallFuture.get();
            assertEquals(999, response.getHttpResponseCode());
        } catch (Exception e) {
            fail("test should not throw an exception");
        }

        // Test bad response after 3 attempts for a response
        request.setRequestType("ReturnBadAfterWait");
        request.setRequestScope(new Integer(3).toString());
        asyncRestCallFuture = manager.asyncSORestCall(UUID.randomUUID().toString(), wm, UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(), request);
        try {
            SOResponse response = asyncRestCallFuture.get();
            assertEquals(999, response.getHttpResponseCode());
        } catch (Exception e) {
            fail("test should not throw an exception");
        }
    }

    @Test
    public void testVfModuleDeletion() {
        SOManager manager = new SOManager();
        assertNotNull(manager);
        manager.setRestGetTimeout(100);

        PolicyEngine.manager.setEnvironmentProperty("so.username", "sean");
        PolicyEngine.manager.setEnvironmentProperty("so.password", "citizen");

        WorkingMemory wm = new DummyWorkingMemory();

        SORequest soRequest = new SORequest();
        soRequest.setOperationType(SOOperationType.DELETE_VF_MODULE);
        PolicyEngine.manager.setEnvironmentProperty("so.url", "http:/localhost:99999999");
        Future<SOResponse> asyncRestCallFuture = manager.asyncSORestCall(UUID.randomUUID().toString(), wm,
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), soRequest);
        try {
            SOResponse response = asyncRestCallFuture.get();
            assertEquals(999, response.getHttpResponseCode());
        } catch (Exception e) {
            fail("test should not throw an exception");
        }

        PolicyEngine.manager.setEnvironmentProperty("so.url", BASE_SO_URI);
        asyncRestCallFuture = manager.asyncSORestCall(UUID.randomUUID().toString(), wm, UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), soRequest);
        try {
            SOResponse response = asyncRestCallFuture.get();
            assertEquals(999, response.getHttpResponseCode());
        } catch (Exception e) {
            fail("test should not throw an exception");
        }

        SORequest request = new SORequest();
        request.setRequestId(UUID.randomUUID());
        request.setRequestScope("Test");
        request.setRequestType("ReturnBadJson");
        request.setStartTime("2018-03-23 16:31");
        request.setRequestStatus(new SORequestStatus());
        request.getRequestStatus().setRequestState("ONGOING");
        request.setOperationType(SOOperationType.DELETE_VF_MODULE);

        asyncRestCallFuture = manager.asyncSORestCall(UUID.randomUUID().toString(), wm, UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), request);
        try {
            SOResponse response = asyncRestCallFuture.get();
            assertEquals(999, response.getHttpResponseCode());
        } catch (Exception e) {
            fail("test should not throw an exception");
        }

        request.setRequestType("ReturnCompleted");

        asyncRestCallFuture = manager.asyncSORestCall(UUID.randomUUID().toString(), wm, UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), request);
        try {
            SOResponse response = asyncRestCallFuture.get();
            assertEquals("COMPLETE", response.getRequest().getRequestStatus().getRequestState());
        } catch (Exception e) {
            fail("test should not throw an exception");
        }

        request.setRequestType("ReturnFailed");
        asyncRestCallFuture = manager.asyncSORestCall(UUID.randomUUID().toString(), wm, UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), request);
        try {
            SOResponse response = asyncRestCallFuture.get();
            assertEquals("FAILED", response.getRequest().getRequestStatus().getRequestState());
        } catch (Exception e) {
            fail("test should not throw an exception");
        }

        // Use scope to set the number of iterations we'll wait for

        request.setRequestType("ReturnOnging200");
        request.setRequestScope(new Integer(10).toString());
        asyncRestCallFuture = manager.asyncSORestCall(UUID.randomUUID().toString(), wm, UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), request);
        try {
            SOResponse response = asyncRestCallFuture.get();
            assertNotNull(response.getRequest());
            assertEquals("COMPLETE", response.getRequest().getRequestStatus().getRequestState());
        } catch (Exception e) {
            fail("test should not throw an exception");
        }

        request.setRequestType("ReturnOnging202");
        request.setRequestScope(new Integer(20).toString());
        asyncRestCallFuture = manager.asyncSORestCall(UUID.randomUUID().toString(), wm, UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), request);
        try {
            SOResponse response = asyncRestCallFuture.get();
            assertNotNull(response.getRequest());
            assertEquals("COMPLETE", response.getRequest().getRequestStatus().getRequestState());
        } catch (Exception e) {
            fail("test should not throw an exception");
        }

        // Test timeout after 20 attempts for a response
        request.setRequestType("ReturnOnging202");
        request.setRequestScope(new Integer(21).toString());
        asyncRestCallFuture = manager.asyncSORestCall(UUID.randomUUID().toString(), wm, UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), request);
        try {
            SOResponse response = asyncRestCallFuture.get();
            assertEquals(999, response.getHttpResponseCode());
        } catch (Exception e) {
            fail("test should not throw an exception");
        }

        // Test bad response after 3 attempts for a response
        request.setRequestType("ReturnBadAfterWait");
        request.setRequestScope(new Integer(3).toString());
        asyncRestCallFuture = manager.asyncSORestCall(UUID.randomUUID().toString(), wm, UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), request);
        try {
            SOResponse response = asyncRestCallFuture.get();
            assertEquals(999, response.getHttpResponseCode());
        } catch (Exception e) {
            fail("test should not throw an exception");
        }
    }
}
