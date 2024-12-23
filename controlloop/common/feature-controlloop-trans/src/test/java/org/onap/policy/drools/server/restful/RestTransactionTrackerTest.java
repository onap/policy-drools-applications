/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023-2024 Nordix Foundation.
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

package org.onap.policy.drools.server.restful;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.endpoints.http.client.HttpClient;
import org.onap.policy.common.endpoints.http.client.HttpClientFactoryInstance;
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.common.message.bus.event.Topic;
import org.onap.policy.common.parameters.topic.BusTopicParams;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.util.Serialization;
import org.onap.policy.drools.apps.controlloop.feature.trans.ControlLoopMetricsFeature;
import org.onap.policy.drools.apps.controlloop.feature.trans.ControlLoopMetricsManager;
import org.onap.policy.drools.persistence.SystemPersistenceConstants;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.drools.system.PolicyEngineConstants;

class RestTransactionTrackerTest {

    private static PolicyController testController;
    private static HttpClient client;

    @BeforeAll
    public static void testBeforeClass() throws Exception {
        SystemPersistenceConstants.getManager().setConfigurationDir("target/test-classes");

        HttpServletServerFactoryInstance.getServerFactory().destroy();
        HttpClientFactoryInstance.getClientFactory().destroy();

        HttpClientFactoryInstance.getClientFactory().build(
                BusTopicParams.builder()
                        .clientName("trans")
                        .hostname("localhost")
                        .port(8769)
                        .basePath("policy/pdp/engine/controllers/transactions")
                        .managed(true)
                        .build());

        var server =
                HttpServletServerFactoryInstance
                        .getServerFactory()
                        .build("trans", "localhost", 8769, "/", true, true);
        server.addServletClass("/*", RestTransactionTracker.class.getName());
        server.waitedStart(5000L);
        assertTrue(NetworkUtil.isTcpPortOpen("localhost", 8769, 5, 10000L));

        testController = PolicyEngineConstants.getManager().createPolicyController("metrics",
                SystemPersistenceConstants.getManager().getControllerProperties("metrics"));

        client = HttpClientFactoryInstance.getClientFactory().get("trans");
    }

    @AfterAll
    public static void testAfterClass() {
        HttpClientFactoryInstance.getClientFactory().destroy();
        HttpServletServerFactoryInstance.getServerFactory().destroy();

        SystemPersistenceConstants.getManager().setConfigurationDir(null);
    }

    @Test
    void testConfiguration() {
        equals(get("cacheSize", Response.Status.OK.getStatusCode()), Integer.class, 3);
        equals(get("timeout", Response.Status.OK.getStatusCode()), Integer.class, 2);

        put("cacheSize/10", Response.Status.OK.getStatusCode());
        put("timeout/20", Response.Status.OK.getStatusCode());

        equals(get("cacheSize", Response.Status.OK.getStatusCode()), Integer.class, 10);
        equals(get("timeout", Response.Status.OK.getStatusCode()), Integer.class, 20);

        put("cacheSize/3", Response.Status.OK.getStatusCode());
        put("timeout/2", Response.Status.OK.getStatusCode());

        equals(get("cacheSize", Response.Status.OK.getStatusCode()), Integer.class, 3);
        equals(get("timeout", Response.Status.OK.getStatusCode()), Integer.class, 2);
    }

    @Test
    void testTransactions() {
        equals(get("/inprogress", Response.Status.OK.getStatusCode()), List.class, Collections.emptyList());

        ControlLoopMetricsFeature feature = new ControlLoopMetricsFeature();

        assertTrue(HttpClient.getBody(get("/inprogress", Response.Status.OK.getStatusCode()),
                List.class).isEmpty());
        get("/inprogress/664be3d2-6c12-4f4b-a3e7-c349acced200", Response.Status.NOT_FOUND.getStatusCode());

        var activeNotification = ResourceUtils.getResourceAsString("policy-cl-mgt-active.json");
        var active =
                Serialization.gsonPretty.fromJson(activeNotification, VirtualControlLoopNotification.class);
        feature.beforeDeliver(testController, Topic.CommInfrastructure.NOOP, "POLICY-CL-MGT", active);
        assertEquals(1, ControlLoopMetricsManager.getManager().getTransactionIds().size());

        assertFalse(HttpClient.getBody(get("/inprogress", Response.Status.OK.getStatusCode()),
                List.class).isEmpty());
        notNull(get("/inprogress/664be3d2-6c12-4f4b-a3e7-c349acced200", Response.Status.OK.getStatusCode())
        );
    }

    private Response get(String contextPath, int statusCode) {
        var response = client.get(contextPath);
        checkResponse(statusCode, response);
        return response;
    }

    private void put(String contextPath, int statusCode) {
        var response = client.put(contextPath, Entity.json(""), Collections.emptyMap());
        checkResponse(statusCode, response);
        response.close();
    }

    private <T, Y> void equals(Response response, Class<T> clazz, Y expected) {
        assertEquals(expected, HttpClient.getBody(response, clazz));
    }

    private <T> void notNull(Response response) {
        assertNotNull(HttpClient.getBody(response, (Class<T>) String.class));
    }

    private void checkResponse(int statusCode, Response response) {
        assertEquals(statusCode, response.getStatus());
    }
}