/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.controlloop;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Paths;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.appc.Request;
import org.onap.policy.appc.Response;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;

/**
 * VFW Use Case Tests.
 */
public class VfwTest extends UsecasesBase {

    /**
     * VFW Tosca Policy File.
     */
    private static final String TOSCA_POLICY_VFW = "src/test/resources/vfw/tosca-vfw.json";

    /*
     * VFW Use case Messages.
     */
    private static final String APPC_SUCCESS = "src/test/resources/vfw/vfw.appc.success.json";
    private static final String ONSET = "src/test/resources/vfw/vfw.onset.json";

    /*
     * Topic trackers used by the VFW use case.
     */
    private TopicCallback<VirtualControlLoopNotification> policyClMgt;
    private TopicCallback<Response> appcClSource;
    private TopicCallback<Request> appcClSink;

    /*
     * VFW Tosca Policy.
     */
    private ToscaPolicy policy;

    /**
     * Prepare PDP-D Framework for testing.
     */
    @BeforeClass
    public static void prepareResouces() throws InterruptedException, CoderException, IOException {
        setupLogging();
        preparePdpD();
        setupSimulators();
    }

    /**
     * Take down the resources used by the test framework.
     */
    @AfterClass
    public static void takeDownResources() {
        stopPdpD();
        stopSimulators();
    }

    /**
     * Sunny day scenario for the VFW use case.
     */
    @Test
    public void sunnyDay() throws IOException {

        /* Inject an ONSET event over the DCAE topic */
        injectOnTopic(DCAE_TOPIC, Paths.get(ONSET));

        /* Wait to acquire a LOCK and a PDP-X PERMIT */
        waitForLockAndPermit(policy, policyClMgt);

        /* --- VFW Operation Execution --- */

        /* Ensure that an APPC RESTART request was sent in response to the matching ONSET */
        await().until(() -> !appcClSink.getMessages().isEmpty());
        assertEquals("ModifyConfig", appcClSink.getMessages().remove().getAction());

        /* Inject a 400 APPC Response Return over the APPC topic */
        injectOnTopic(APPC_CL_TOPIC, Paths.get(APPC_SUCCESS));

        /* Ensure that RESTART response is received */
        await().until(() -> !appcClSource.getMessages().isEmpty());
        assertEquals("SUCCESS", appcClSource.getMessages().remove().getStatus().getValue());

        /* --- VFW Operation Completed --- */

        /* Ensure that the VFW RESTART Operation is successfully completed */
        await().until(() -> !policyClMgt.getMessages().isEmpty());
        assertEquals(ControlLoopNotificationType.OPERATION_SUCCESS,
            policyClMgt.getMessages().remove().getNotification());

        /* --- VFW Transaction Completed --- */
        waitForFinalSuccess(policy, policyClMgt);
    }

    /**
     * Observe Topics.
     */
    @Before
    public void topicsRegistration() {
        policyClMgt = createTopicSinkCallback(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class);
        appcClSink = createTopicSinkCallback(APPC_CL_TOPIC, Request.class);
        appcClSource = createTopicSourceCallback(APPC_CL_TOPIC, Response.class);
    }

    /**
     * Unregister Topic Callbacks.
     */
    @After
    public void topicsUnregistration() {
        if (policyClMgt != null) {
            policyClMgt.unregister();
        }

        if (appcClSource != null) {
            appcClSource.unregister();
        }

        if (appcClSink != null) {
            appcClSink.unregister();
        }
    }

    /**
     * Install Policy.
     */
    @Before
    public void installPolicy() throws IOException, CoderException, InterruptedException {
        assertEquals(0, usecases.getDrools().factCount(USECASES));
        policy = setupPolicy(TOSCA_POLICY_VFW);
        assertEquals(2, usecases.getDrools().factCount(USECASES));
    }

    /**
     * Uninstall Policy.
     */
    @After
    public void uninstallPolicy() throws InterruptedException {
        assertEquals(2, usecases.getDrools().factCount(USECASES));
        if (policy != null) {
            deletePolicy(policy);
        }
        assertEquals(0, usecases.getDrools().factCount(USECASES));
    }

}
