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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.appc.Request;
import org.onap.policy.appclcm.AppcLcmDmaapWrapper;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.StandardCoderInstantAsMillis;
import org.onap.policy.controlloop.common.endpoints.test.HttpClients;
import org.onap.policy.controlloop.common.endpoints.test.Listener;
import org.onap.policy.controlloop.common.endpoints.test.Simulators;
import org.onap.policy.controlloop.common.endpoints.test.Topics;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.simulators.Util;

/**
 * Tests use cases using Frankfurt rules.
 */
public class RulesTest extends FrankfurtBase {

    /*
     * Tosca Policy files
     */
    private static final String VCPE_TOSCA_LEGACY_POLICY = "src/test/resources/vcpe/tosca-legacy-vcpe.json";
    private static final String VCPE_TOSCA_COMPLIANT_POLICY = "src/test/resources/vcpe/tosca-compliant-vcpe.json";

    private static final String VDNS_TOSCA_COMPLIANT_POLICY = "src/test/resources/vdns/tosca-compliant-vdns.json";

    private static final String VFW_TOSCA_LEGACY_POLICY = "src/test/resources/vfw/tosca-vfw.json";
    private static final String VFW_TOSCA_COMPLIANT_POLICY = "src/test/resources/vfw/tosca-compliant-vfw.json";

    private static final String VLB_TOSCA_LEGACY_POLICY = "src/test/resources/vlb/tosca-vlb.json";
    private static final String VLB_TOSCA_COMPLIANT_POLICY = "src/test/resources/vlb/tosca-compliant-vlb.json";

    /*
     * Messages for various use cases
     */
    private static final String VCPE_APPC_SUCCESS = "src/test/resources/vcpe/vcpe.appc.success.json";
    private static final String VCPE_ONSET_1 = "src/test/resources/vcpe/vcpe.onset.1.json";
    private static final String VCPE_ONSET_2 = "src/test/resources/vcpe/vcpe.onset.2.json";
    private static final String VCPE_ONSET_3 = "src/test/resources/vcpe/vcpe.onset.3.json";

    private static final String VDNS_ONSET = "src/test/resources/vdns/vdns.onset.json";

    private static final String VFW_APPC_SUCCESS = "src/test/resources/vfw/vfw.appc.success.json";
    private static final String VFW_ONSET = "src/test/resources/vfw/vfw.onset.json";

    private static final String VLB_ONSET = "src/test/resources/vlb/vlb.onset.json";

    /*
     * Coders used to decode requests and responses.
     */
    private static final Coder APPC_LEGACY_CODER = new StandardCoderInstantAsMillis();
    private static final Coder APPC_CODER = new StandardCoder();

    private static HttpClients httpClients = new HttpClients();
    private static Simulators simulators;


    // used to inject and wait for messages
    private Topics topics;

    // used to wait for messages on SINK topics
    private Listener<VirtualControlLoopNotification> policyClMgt;
    private Listener<Request> appcClSink;
    private Listener<AppcLcmDmaapWrapper> appcLcmRead;

    /*
     * Tosca Policy that was loaded.
     */
    private ToscaPolicy policy;


    /**
     * Sets up statics.
     */
    @BeforeClass
    public static void setUpBeforeClass() throws InterruptedException, IOException {
        prepareResouces();
        simulators = new Simulators();

        httpClients.addClients("frankfurt");
        simulators.start(Util::buildAaiSim, Util::buildSoSim, Util::buildVfcSim, Util::buildGuardSim,
                        Util::buildSdncSim);
    }

    /**
     * Cleans up statics.
     */
    @AfterClass
    public static void tearDownAfterClass() {
        takeDownResources();
        httpClients.destroy();
        simulators.destroy();
    }

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        topics = new Topics();
    }

    /**
     * Tears down.
     */
    @After
    public void tearDown() throws InterruptedException {
        topics.destroy();
        resetFacts();
    }

    // VCPE

    /**
     * Sunny Day with Legacy Tosca Policy.
     */
    @Test
    public void vcpeSunnyDayLegacy() throws InterruptedException, CoderException, IOException {
        policyClMgt = topics.createListener(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class, controller);
        appcLcmRead = topics.createListener(APPC_LCM_READ_TOPIC, AppcLcmDmaapWrapper.class, APPC_CODER);

        assertEquals(0, controller.getDrools().factCount(CONTROLLER_NAME));
        policy = setupPolicyFromFile(VCPE_TOSCA_LEGACY_POLICY);
        assertEquals(2, controller.getDrools().factCount(CONTROLLER_NAME));

        // inject an ONSET event over the DCAE topic
        topics.inject(DCAE_TOPIC, new File(VCPE_ONSET_1));

        vcpeSunnyDay();
    }

    /**
     * Sunny Day with Tosca Compliant Policy.
     */
    @Test
    public void vcpeSunnyDayCompliant() throws InterruptedException, CoderException, IOException {
        policyClMgt = topics.createListener(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class, controller);
        appcLcmRead = topics.createListener(APPC_LCM_READ_TOPIC, AppcLcmDmaapWrapper.class, APPC_CODER);

        assertEquals(0, controller.getDrools().factCount(CONTROLLER_NAME));
        policy = setupPolicyFromFile(VCPE_TOSCA_COMPLIANT_POLICY);
        assertEquals(2, controller.getDrools().factCount(CONTROLLER_NAME));

        // inject an ONSET event over the DCAE topic
        topics.inject(DCAE_TOPIC, new File(VCPE_ONSET_1));

        vcpeSunnyDay();
    }

    /**
     * An ONSET flood prevention test that injects a few ONSETs at once. It attempts to
     * simulate the flooding behavior of the DCAE TCA microservice. TCA could blast tenths
     * or hundreds of ONSETs within sub-second intervals.
     */
    @Test
    public void vcpeOnsetFloodPrevention() throws IOException, InterruptedException, CoderException {
        policyClMgt = topics.createListener(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class, controller);
        appcLcmRead = topics.createListener(APPC_LCM_READ_TOPIC, AppcLcmDmaapWrapper.class, APPC_CODER);

        assertEquals(0, controller.getDrools().factCount(CONTROLLER_NAME));
        policy = setupPolicyFromFile(VCPE_TOSCA_LEGACY_POLICY);
        assertEquals(2, controller.getDrools().factCount(CONTROLLER_NAME));

        // inject several ONSET events over the DCAE topic
        topics.inject(DCAE_TOPIC, new File(VCPE_ONSET_1));
        topics.inject(DCAE_TOPIC, new File(VCPE_ONSET_2));
        topics.inject(DCAE_TOPIC, new File(VCPE_ONSET_3));

        assertEquals(1, controller.getDrools().facts(CONTROLLER_NAME, VirtualControlLoopEvent.class).stream().count());
        assertEquals(1, controller.getDrools().facts(CONTROLLER_NAME, CanonicalOnset.class).stream().count());
        assertEquals(controller.getDrools().facts(CONTROLLER_NAME, CanonicalOnset.class).get(0),
                        controller.getDrools().facts(CONTROLLER_NAME, VirtualControlLoopEvent.class).get(0));

        vcpeSunnyDay();
    }

    /**
     * Sunny day scenario for the VCPE use case.
     */
    public void vcpeSunnyDay() {
        /* Wait to acquire a LOCK and a PDP-X PERMIT */
        waitForLockAndPermit(policy, policyClMgt);

        /*
         * Ensure that an APPC RESTART request was sent in response to the matching ONSET
         */
        AppcLcmDmaapWrapper appcreq = appcLcmRead.await(req -> "restart".equals(req.getRpcName()));

        /*
         * Inject a 400 APPC Response Return over the APPC topic, with appropriate
         * subRequestId
         */
        topics.inject(APPC_LCM_WRITE_TOPIC, new File(VCPE_APPC_SUCCESS),
                        appcreq.getBody().getInput().getCommonHeader().getSubRequestId());

        /* --- VCPE Operation Completed --- */

        /* Ensure that the VFW RESTART Operation is successfully completed */
        policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.OPERATION_SUCCESS);

        /* --- VLB Transaction Completed --- */
        waitForFinalSuccess(policy, policyClMgt);
    }

    // VDNS

    /**
     * Sunny Day with Tosca Compliant Policy.
     */
    @Test
    public void vdnsSunnyDayCompliant() throws InterruptedException, CoderException, IOException {
        vdnsSunnyDay(VDNS_TOSCA_COMPLIANT_POLICY);
    }

    /**
     * Sunny day scenario for the VCPE use case.
     */
    private void vdnsSunnyDay(String toscaFile) throws IOException {
        policyClMgt = topics.createListener(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class, controller);

        assertEquals(0, controller.getDrools().factCount(CONTROLLER_NAME));
        policy = setupPolicyFromFile(toscaFile);
        assertEquals(2, controller.getDrools().factCount(CONTROLLER_NAME));

        /* Inject an ONSET event over the DCAE topic */
        topics.inject(DCAE_TOPIC, new File(VDNS_ONSET));

        /* Wait to acquire a LOCK and a PDP-X PERMIT */
        waitForLockAndPermit(policy, policyClMgt);

        /* --- VFW Operation Completed --- */

        /* Ensure that the VFW RESTART Operation is successfully completed */
        policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.OPERATION_SUCCESS);

        /* --- VLB Transaction Completed --- */
        waitForFinalSuccess(policy, policyClMgt);
    }

    // VFW

    /**
     * VFW Sunny Day with Legacy Tosca Policy.
     */
    @Test
    public void vfwSunnyDayLegacy() {
        vfwSunnyDay(VFW_TOSCA_LEGACY_POLICY);
    }

    /**
     * VFW Sunny Day with Tosca Compliant Policy.
     */
    @Test
    public void vfwSunnyDayCompliant() {
        vfwSunnyDay(VFW_TOSCA_COMPLIANT_POLICY);
    }

    /**
     * Sunny day scenario for the VFW use case.
     */
    private void vfwSunnyDay(String toscaFile) {
        policyClMgt = topics.createListener(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class, controller);
        appcClSink = topics.createListener(APPC_CL_TOPIC, Request.class, APPC_LEGACY_CODER);

        assertEquals(0, controller.getDrools().factCount(CONTROLLER_NAME));
        policy = setupPolicyFromFile(toscaFile);
        assertEquals(2, controller.getDrools().factCount(CONTROLLER_NAME));

        /* Inject an ONSET event over the DCAE topic */
        topics.inject(DCAE_TOPIC, new File(VFW_ONSET));

        /* Wait to acquire a LOCK and a PDP-X PERMIT */
        waitForLockAndPermit(policy, policyClMgt);

        /*
         * Ensure that an APPC RESTART request was sent in response to the matching ONSET
         */
        Request appcreq = appcClSink.await(req -> "ModifyConfig".equals(req.getAction()));

        /*
         * Inject a 400 APPC Response Return over the APPC topic, with appropriate
         * subRequestId
         */
        topics.inject(APPC_CL_TOPIC, new File(VFW_APPC_SUCCESS), appcreq.getCommonHeader().getSubRequestId());

        /* --- VFW Operation Completed --- */

        /* Ensure that the VFW RESTART Operation is successfully completed */
        policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.OPERATION_SUCCESS);

        /* --- VLB Transaction Completed --- */
        waitForFinalSuccess(policy, policyClMgt);
    }

    // VLB

    /**
     * Sunny Day with Legacy Tosca Policy.
     */
    @Test
    public void vlbSunnyDayLegacy() {
        vlbSunnyDay(VLB_TOSCA_LEGACY_POLICY);
    }

    /**
     * Sunny Day with Tosca Compliant Policy.
     */
    @Test
    public void vlbSunnyDayCompliant() {
        vlbSunnyDay(VLB_TOSCA_COMPLIANT_POLICY);
    }

    /**
     * Sunny day scenario for the VLB use case.
     */
    private void vlbSunnyDay(String toscaFile) {
        policyClMgt = topics.createListener(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class, controller);

        assertEquals(0, controller.getDrools().factCount(CONTROLLER_NAME));
        policy = setupPolicyFromFile(VLB_TOSCA_COMPLIANT_POLICY);
        assertEquals(2, controller.getDrools().factCount(CONTROLLER_NAME));

        /* Inject an ONSET event over the DCAE topic */
        topics.inject(DCAE_TOPIC, new File(VLB_ONSET));

        /* Wait to acquire a LOCK and a PDP-X PERMIT */
        waitForLockAndPermit(policy, policyClMgt);

        /* Ensure that the VLB SO Operation was successfully completed */

        policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.OPERATION_SUCCESS);

        /* --- VLB Transaction Completed --- */
        waitForFinalSuccess(policy, policyClMgt);
    }
}
