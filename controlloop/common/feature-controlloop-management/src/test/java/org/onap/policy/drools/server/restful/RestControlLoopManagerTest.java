/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights reserved.
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

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response.Status;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.builder.ReleaseId;
import org.onap.policy.common.endpoints.http.client.HttpClient;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.drools.persistence.SystemPersistence;
import org.onap.policy.drools.properties.DroolsProperties;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.drools.util.KieUtils;
import org.onap.policy.drools.utils.logging.LoggerUtil;

/**
 * Test RestControlLoopManager.
 */
public class RestControlLoopManagerTest {

    private static final String KSESSION = "op";
    private static final String KMODULE_DRL_PATH = "src/test/resources/op.drl";
    private static final String KMODULE_POM_PATH = "src/test/resources/op.pom";
    private static final String KMODULE_PATH = "src/test/resources/op.kmodule";
    private static final String KJAR_DRL_PATH = "src/main/resources/kbop/org/onap/policy/drools/test/op.drl";

    private static final String CONTROLLER = KSESSION;
    private static final String CONTROLOOP_NAME = "ControlLoop-vCPE-48f0c2c3-a172-4192-9ae3-052274181b6e";

    private static final String CLIENT_CONFIG = "op-http";

    private static final String URL_CONTEXT_PATH_CONTROLLERS = "controllers/";
    private static final String URL_CONTEXT_PATH_CONTROLLER = URL_CONTEXT_PATH_CONTROLLERS + CONTROLLER;
    private static final String URL_CONTEXT_PATH_KSESSION = URL_CONTEXT_PATH_CONTROLLER + "/drools/facts/" + KSESSION;
    private static final String URL_CONTEXT_PATH_CONTROLLOOPS = URL_CONTEXT_PATH_KSESSION +  "/controlloops/";
    private static final String URL_CONTEXT_PATH_CONTROLLOOP = URL_CONTEXT_PATH_CONTROLLOOPS +  CONTROLOOP_NAME;
    private static final String URL_CONTEXT_PATH_CONTROLLOOP_POLICY = URL_CONTEXT_PATH_CONTROLLOOP + "/policy";

    private static final String POLICY = "src/test/resources/vCPE.yaml";

    private static final String CONTROLLER_FILE = "op-controller.properties";
    private static final String CONTROLLER_FILE_BAK = "op-controller.properties.bak";

    /**
     * test set up.
     *
     * @throws Exception if failure to complete the set up.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        LoggerUtil.setLevel(LoggerUtil.ROOT_LOGGER, "ERROR");

        SystemPersistence.manager.setConfigurationDir("src/test/resources");
        PolicyEngine.manager.configure(PolicyEngine.manager.defaultTelemetryConfig());

        ReleaseId releaseId =
            KieUtils.installArtifact(Paths.get(KMODULE_PATH).toFile(), Paths.get(KMODULE_POM_PATH).toFile(),
                KJAR_DRL_PATH, Paths.get(KMODULE_DRL_PATH).toFile());

        Properties controllerProperties = new Properties();
        controllerProperties.put(DroolsProperties.RULES_GROUPID, releaseId.getGroupId());
        controllerProperties.put(DroolsProperties.RULES_ARTIFACTID, releaseId.getArtifactId());
        controllerProperties.put(DroolsProperties.RULES_VERSION, releaseId.getVersion());

        PolicyEngine.manager.createPolicyController(CONTROLLER, controllerProperties);
        PolicyEngine.manager.start();

        HttpClient.factory.build(SystemPersistence.manager.getProperties(CLIENT_CONFIG));

        if (!NetworkUtil.isTcpPortOpen("localhost", 9696, 6, 10000L)) {
            throw new IllegalStateException("cannot connect to port 9696");
        }

        await().atMost(1, TimeUnit.MINUTES).until(isContainerAlive());
    }

    /**
     * test tear down.
     */
    @AfterClass
    public static void tearDown() {
        PolicyController.factory.get(CONTROLLER).stop();
        await().atMost(1, TimeUnit.MINUTES).until(isContainerAlive(), equalTo(Boolean.FALSE));

        PolicyEngine.manager.removePolicyController(CONTROLLER);
        PolicyEngine.manager.stop();

        final Path controllerPath =
            Paths.get(SystemPersistence.manager.getConfigurationPath().toString(), CONTROLLER_FILE);
        try {
            Files.deleteIfExists(controllerPath);
        } catch (Exception ignored) {
            /* to satisfy checkstyle */
            ;
        }

        Path controllerBakPath =
            Paths.get(SystemPersistence.manager.getConfigurationPath().toString(), CONTROLLER_FILE_BAK);

        try {
            Files.deleteIfExists(controllerBakPath);
        } catch (Exception ignored) {
            /* to satisfy checkstyle */
            ;
        }
    }

    /**
     * Test Operational Policies.
     */
    @Test
    public void testOperationalPolicy() throws IOException {
        assertEquals(HttpClient.factory.get(CONTROLLER)
            .get(URL_CONTEXT_PATH_CONTROLLOOPS).getStatus(), Status.OK.getStatusCode());

        assertEquals(HttpClient.factory.get(CONTROLLER)
            .get(URL_CONTEXT_PATH_CONTROLLOOP).getStatus(), Status.OK.getStatusCode());

        assertEquals(HttpClient.factory.get(CONTROLLER)
            .get(URL_CONTEXT_PATH_CONTROLLOOP_POLICY).getStatus(), Status.NOT_FOUND.getStatusCode());

        String policyFromFile = new String(Files.readAllBytes(Paths.get(POLICY)));
        HttpClient.factory.get(CONTROLLER)
            .put(URL_CONTEXT_PATH_CONTROLLOOP_POLICY, Entity.text(policyFromFile), Collections.emptyMap());

        assertEquals(HttpClient.factory.get(CONTROLLER)
            .get(URL_CONTEXT_PATH_CONTROLLOOP_POLICY)
            .getStatus(), Status.OK.getStatusCode());

        String policyFromPdpD = HttpClient.factory.get(CONTROLLER)
            .get(URL_CONTEXT_PATH_CONTROLLOOP_POLICY)
            .readEntity(String.class);

        assertEquals(policyFromFile, policyFromPdpD);

        assertEquals(HttpClient.factory.get(CONTROLLER)
            .put(URL_CONTEXT_PATH_CONTROLLOOP_POLICY, Entity.text(policyFromFile), Collections.emptyMap())
            .getStatus(), Status.CONFLICT.getStatusCode());
    }

    /**
     * Test if the session is alive
     *
     * @return if the container is alive.
     */
    private static Callable<Boolean> isContainerAlive() {
        return () -> PolicyController.factory.get(CONTROLLER).getDrools().getContainer().isAlive();
    }
}
