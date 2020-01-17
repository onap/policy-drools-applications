/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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
import org.onap.policy.appclcm.AppcLcmDmaapWrapper;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;

/**
 * VCPE Use Case Tests.
 */
public class VcpeTest extends UsecasesBase {

    /**
     * VCPE Tosca Policy File.
     */
    private static final String TOSCA_POLICY_VCPE = "src/test/resources/vcpe/tosca-vcpe.json";

    /*
     * VCPE Use case Messages.
     */
    private static final String APPC_SUCCESS = "src/test/resources/vcpe/vcpe.appc.success.json";
    private static final String ONSET_1 = "src/test/resources/vcpe/vcpe.onset.1.json";
    private static final String ONSET_2 = "src/test/resources/vcpe/vcpe.onset.2.json";
    private static final String ONSET_3 = "src/test/resources/vcpe/vcpe.onset.3.json";

    /*
     * Topic trackers used by the VCPE use case.
     */
    private TopicCallback<VirtualControlLoopNotification> policyClMgt;
    private TopicCallback<AppcLcmDmaapWrapper> appcLcmRead;
    private TopicCallback<AppcLcmDmaapWrapper> appcLcmWrite;

    /*
     * VCPE Tosca Policy.
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
     * Sunny day scenario for the VCPE use case.
     */
    @Test
    public void sunnyDay() throws IOException {

        /* Inject an ONSET event over the DCAE topic */
        injectOnTopic(DCAE_TOPIC, Paths.get(ONSET_1));

        /* Wait to acquire a LOCK and a PDP-X PERMIT */
        waitForLockAndPermit(policy, policyClMgt);

        /* --- VCPE Operation Execution --- */

        /* Ensure that an APPC RESTART request was sent in response to the matching ONSET */
        await().until(() -> !appcLcmRead.getMessages().isEmpty());
        assertEquals("restart", appcLcmRead.getMessages().remove().getRpcName());

        /* Inject a 400 APPC Response Return over the APPC topic */
        injectOnTopic(APPC_LCM_WRITE_TOPIC, Paths.get(APPC_SUCCESS));

        /* Ensure that RESTART response is received */
        await().until(() -> !appcLcmWrite.getMessages().isEmpty());
        assertEquals("restart", appcLcmWrite.getMessages().peek().getRpcName());
        assertEquals(400, appcLcmWrite.getMessages().remove().getBody().getOutput().getStatus().getCode());

        /* --- VCPE Operation Completed --- */

        /* Ensure that the VCPE RESTART Operation is successfully completed */
        await().until(() -> !policyClMgt.getMessages().isEmpty());
        assertEquals(ControlLoopNotificationType.OPERATION_SUCCESS, policyClMgt.getMessages().peek().getNotification());
        assertEquals(policy.getIdentifier().getName() + ".APPC.LCM.RESPONSE",
            policyClMgt.getMessages().remove().getPolicyName());

        /* --- VCPE Transaction Completed --- */
        waitForFinalSuccess(policy, policyClMgt);
    }

    /**
     * An ONSET flood prevention test that injects a few ONSETs at once.
     * It attempts to simulate the flooding behavior of the DCAE TCA microservice.
     * TCA could blast tenths or hundreds of ONSETs within sub-second intervals.
     */
    @Test
    public void onsetFloodPrevention() throws IOException {
        injectOnTopic(DCAE_TOPIC, Paths.get(ONSET_1));
        injectOnTopic(DCAE_TOPIC, Paths.get(ONSET_2));
        injectOnTopic(DCAE_TOPIC, Paths.get(ONSET_3));

        assertEquals(1, usecases.getDrools().facts(USECASES, VirtualControlLoopEvent.class).stream().count());
        assertEquals(1, usecases.getDrools().facts(USECASES, CanonicalOnset.class).stream().count());
        assertEquals(usecases.getDrools().facts(USECASES, CanonicalOnset.class).get(0),
                usecases.getDrools().facts(USECASES, VirtualControlLoopEvent.class).get(0));

        sunnyDay();
    }

    /**
     * Observe Topics.
     */
    @Before
    public void topicsRegistration() {
        policyClMgt = createTopicSinkCallback(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class);
        appcLcmRead = createTopicSinkCallback(APPC_LCM_READ_TOPIC, AppcLcmDmaapWrapper.class);
        appcLcmWrite = createTopicSourceCallback(APPC_LCM_WRITE_TOPIC, AppcLcmDmaapWrapper.class);
    }

    /**
     * Unregister Topic Callbacks.
     */
    @After
    public void topicsUnregistration() {
        if (policyClMgt != null) {
            policyClMgt.unregister();
        }

        if (appcLcmRead != null) {
            appcLcmRead.unregister();
        }

        if (appcLcmWrite != null) {
            appcLcmWrite.unregister();
        }
    }

    /**
     * Install Policy.
     */
    @Before
    public void installPolicy() throws IOException, CoderException, InterruptedException {
        assertEquals(0, usecases.getDrools().factCount(USECASES));
        policy = setupPolicy(TOSCA_POLICY_VCPE);
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
