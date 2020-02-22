/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;

/**
 * VLB Use Case Tests.
 */
public class VlbTest extends FrankfurtBase {

    /**
     * VLB Tosca Policy File.
     */
    private static final String TOSCA_LEGACY_POLICY_VLB = "src/test/resources/vlb/tosca-vlb.json";
    private static final String TOSCA_COMPLIANT_POLICY_VLB = "src/test/resources/vlb/tosca-compliant-vlb.json";

    /*
     * VLB Use case Messages.
     */
    private static final String ONSET = "src/test/resources/vlb/vlb.onset.json";

    /*
     * Topic trackers used by the VLB use case.
     */
    private TopicCallback<VirtualControlLoopNotification> policyClMgt;

    /*
     * VLB Tosca Policy.
     */
    private ToscaPolicy policy;

    /**
     * Prepare PDP-D Framework for testing.
     */
    @BeforeClass
    public static void prepareResouces() throws InterruptedException, IOException {
        setupLogging();
        preparePdpD();
        setUpHttpClients();
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
     * Observe Topics.
     */
    @Before
    public void topicsRegistration() {
        policyClMgt = createTopicSinkCallback(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class);
    }

    /**
     * Unregister Topic Callbacks and uninstall the policy.
     */
    @After
    public void topicsUnregistration() throws InterruptedException {
        if (policyClMgt != null) {
            policyClMgt.unregister();
        }

        // uninstall the policy
        assertEquals(2, controller.getDrools().factCount(CONTROLLER_NAME));
        if (policy != null) {
            deletePolicy(policy);
        }
        assertEquals(0, controller.getDrools().factCount(CONTROLLER_NAME));
    }

    /**
     * Sunny Day with Legacy Tosca Policy.
     */
    @Test
    public void sunnyDayLegacy() throws InterruptedException, CoderException, IOException {
        assertEquals(0, controller.getDrools().factCount(CONTROLLER_NAME));
        policy = setupPolicyFromFile(TOSCA_LEGACY_POLICY_VLB);
        assertEquals(2, controller.getDrools().factCount(CONTROLLER_NAME));

        sunnyDay();
    }

    /**
     * Sunny Day with Tosca Compliant Policy.
     */
    @Test
    public void sunnyDayCompliant() throws InterruptedException, CoderException, IOException {
        assertEquals(0, controller.getDrools().factCount(CONTROLLER_NAME));
        policy = setupPolicyFromFile(TOSCA_COMPLIANT_POLICY_VLB);
        assertEquals(2, controller.getDrools().factCount(CONTROLLER_NAME));

        sunnyDay();
    }

    /**
     * Sunny day scenario for the VCPE use case.
     */
    private void sunnyDay() throws IOException {

        /* Inject an ONSET event over the DCAE topic */
        injectOnTopic(DCAE_TOPIC, Paths.get(ONSET));

        /* Wait to acquire a LOCK and a PDP-X PERMIT */
        waitForLockAndPermit(policy, policyClMgt);

        /* Ensure that the VLB SO Operation was successfully completed */

        await().until(() -> !policyClMgt.getMessages().isEmpty());
        assertEquals(ControlLoopNotificationType.OPERATION_SUCCESS,
                        policyClMgt.getMessages().remove().getNotification());

        /* --- VLB Transaction Completed --- */
        waitForFinalSuccess(policy, policyClMgt);
    }
}
