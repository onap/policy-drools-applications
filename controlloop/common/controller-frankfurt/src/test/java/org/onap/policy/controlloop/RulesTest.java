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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.appc.Request;
import org.onap.policy.appclcm.AppcLcmDmaapWrapper;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.StandardCoderInstantAsMillis;
import org.onap.policy.controlloop.common.rules.test.HttpClients;
import org.onap.policy.controlloop.common.rules.test.Listener;
import org.onap.policy.controlloop.common.rules.test.Rules;
import org.onap.policy.controlloop.common.rules.test.Simulators;
import org.onap.policy.controlloop.common.rules.test.Topics;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.simulators.Util;

/**
 * Tests use cases using Frankfurt rules.
 */
public class RulesTest {
    protected static final String CONTROLLER_NAME = "frankfurt";

    /*
     * Canonical Topic Names.
     */
    protected static final String DCAE_TOPIC = "DCAE_TOPIC";
    protected static final String APPC_LCM_WRITE_TOPIC = "APPC-LCM-WRITE";
    protected static final String POLICY_CL_MGT_TOPIC = "POLICY-CL-MGT";
    protected static final String APPC_LCM_READ_TOPIC = "APPC-LCM-READ";
    protected static final String APPC_CL_TOPIC = "APPC-CL";

    /*
     * Tosca Policy files
     */
    private static final String SERVICE123_TOSCA_COMPLIANT_POLICY =
                    "src/test/resources/service123/tosca-compliant-service123.json";

    private static final String DUPLICATES_TOSCA_COMPLIANT_POLICY =
                    "src/test/resources/duplicates/tosca-compliant-duplicates.json";

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
    private static final String SERVICE123_ONSET = "src/test/resources/service123/service123.onset.json";
    private static final String SERVICE123_APPC_RESTART_FAILURE =
                    "src/test/resources/service123/service123.appc.restart.failure.json";
    private static final String SERVICE123_APPC_REBUILD_FAILURE =
                    "src/test/resources/service123/service123.appc.rebuild.failure.json";
    private static final String SERVICE123_APPC_MIGRATE_SUCCESS =
                    "src/test/resources/service123/service123.appc.migrate.success.json";

    private static final String DUPLICATES_ONSET_1 = "src/test/resources/duplicates/duplicates.onset.1.json";
    private static final String DUPLICATES_ONSET_2 = "src/test/resources/duplicates/duplicates.onset.2.json";
    private static final String DUPLICATES_APPC_SUCCESS = "src/test/resources/vcpe/vcpe.appc.success.json";

    private static final String VCPE_ONSET_1 = "src/test/resources/vcpe/vcpe.onset.1.json";
    private static final String VCPE_ONSET_2 = "src/test/resources/vcpe/vcpe.onset.2.json";
    private static final String VCPE_ONSET_3 = "src/test/resources/vcpe/vcpe.onset.3.json";
    private static final String VCPE_APPC_SUCCESS = "src/test/resources/vcpe/vcpe.appc.success.json";

    private static final String VDNS_ONSET = "src/test/resources/vdns/vdns.onset.json";

    private static final String VFW_ONSET = "src/test/resources/vfw/vfw.onset.json";
    private static final String VFW_APPC_SUCCESS = "src/test/resources/vfw/vfw.appc.success.json";

    private static final String VLB_ONSET = "src/test/resources/vlb/vlb.onset.json";

    /*
     * Coders used to decode requests and responses.
     */
    private static final Coder APPC_LEGACY_CODER = new StandardCoderInstantAsMillis();
    private static final Coder APPC_LCM_CODER = new StandardCoder();

    private static Rules rules = new Rules(CONTROLLER_NAME);
    private static HttpClients httpClients = new HttpClients();
    private static Simulators simulators;


    // used to inject and wait for messages
    private Topics topics;

    // used to wait for messages on SINK topics
    private Listener<VirtualControlLoopNotification> policyClMgt;
    private Listener<Request> appcClSink;
    private Listener<AppcLcmDmaapWrapper> appcLcmRead;

    private PolicyController controller;

    /*
     * Tosca Policy that was loaded.
     */
    private ToscaPolicy policy;


    /**
     * Sets up statics.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        simulators = new Simulators();

        rules.configure("src/main/resources");
        rules.start();
        httpClients.addClients("frankfurt");
        simulators.start(Util::buildAaiSim, Util::buildSoSim, Util::buildVfcSim, Util::buildGuardSim,
                        Util::buildSdncSim);
    }

    /**
     * Cleans up statics.
     */
    @AfterClass
    public static void tearDownAfterClass() {
        httpClients.destroy();
        simulators.destroy();
        rules.destroy();
    }

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        topics = new Topics();
        controller = rules.getController();
    }

    /**
     * Tears down.
     */
    @After
    public void tearDown() throws InterruptedException {
        topics.destroy();
        rules.resetFacts();
    }

    // Service123 (i.e., Policy with multiple operations)

    /**
     * Service123 with Tosca Compliant Policy.
     */
    @Test
    public void service123Compliant() {
        policyClMgt = topics.createListener(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class, controller);
        appcLcmRead = topics.createListener(APPC_LCM_READ_TOPIC, AppcLcmDmaapWrapper.class, APPC_LCM_CODER);

        assertEquals(0, controller.getDrools().factCount(CONTROLLER_NAME));
        policy = rules.setupPolicyFromFile(SERVICE123_TOSCA_COMPLIANT_POLICY);
        assertEquals(2, controller.getDrools().factCount(CONTROLLER_NAME));

        // inject an ONSET event over the DCAE topic
        topics.inject(DCAE_TOPIC, new File(SERVICE123_ONSET));

        /* Wait to acquire a LOCK and a PDP-X PERMIT */
        waitForLockAndPermit(policy, policyClMgt);

        // restart request should be sent and fail four times (i.e., because retry=3)
        for (int count = 0; count < 4; ++count) {
            AppcLcmDmaapWrapper appcreq = appcLcmRead.await(req -> "restart".equals(req.getRpcName()));

            topics.inject(APPC_LCM_WRITE_TOPIC, new File(SERVICE123_APPC_RESTART_FAILURE),
                            appcreq.getBody().getInput().getCommonHeader().getSubRequestId());
        }

        // rebuild request should be sent and fail once
        AppcLcmDmaapWrapper appcreq = appcLcmRead.await(req -> "rebuild".equals(req.getRpcName()));

        topics.inject(APPC_LCM_WRITE_TOPIC, new File(SERVICE123_APPC_REBUILD_FAILURE),
                        appcreq.getBody().getInput().getCommonHeader().getSubRequestId());

        // migrate request should be sent and succeed
        appcreq = appcLcmRead.await(req -> "migrate".equals(req.getRpcName()));

        topics.inject(APPC_LCM_WRITE_TOPIC, new File(SERVICE123_APPC_MIGRATE_SUCCESS),
                        appcreq.getBody().getInput().getCommonHeader().getSubRequestId());

        /* --- VCPE Operation Completed --- */

        /* Ensure that the VFW RESTART Operation is successfully completed */
        policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.OPERATION_SUCCESS);

        /* --- VLB Transaction Completed --- */
        waitForFinalSuccess(policy, policyClMgt);
    }

    // Duplicate events

    /**
     * This test case tests the scenario where 3 events occur and 2 of the requests refer
     * to the same target entity while the 3rd is for another entity. The expected result
     * is that the event with the duplicate target entity will have a final success result
     * for one of the events, and a rejected message for the one that was unable to obtain
     * the lock. The event that is referring to a different target entity should be able
     * to obtain a lock since it is a different target. After processing of all events
     * there should only be the policy and params objects left in memory.
     */
    @Test
    public void duplicateEvents() {
        policyClMgt = topics.createListener(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class, controller);
        appcLcmRead = topics.createListener(APPC_LCM_READ_TOPIC, AppcLcmDmaapWrapper.class, APPC_LCM_CODER);

        assertEquals(0, controller.getDrools().factCount(CONTROLLER_NAME));
        policy = rules.setupPolicyFromFile(DUPLICATES_TOSCA_COMPLIANT_POLICY);
        assertEquals(2, controller.getDrools().factCount(CONTROLLER_NAME));

        /*
         * Inject ONSET events over the DCAE topic. First and last have the same target
         * entity, but different request IDs - only one should succeed. The middle one is
         * for a different target entity, so it should succeed.
         */
        topics.inject(DCAE_TOPIC, new File(DUPLICATES_ONSET_1), UUID.randomUUID().toString());
        topics.inject(DCAE_TOPIC, new File(DUPLICATES_ONSET_2));
        topics.inject(DCAE_TOPIC, new File(DUPLICATES_ONSET_1), UUID.randomUUID().toString());

        // one should immediately generate a FINAL failure
        policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.FINAL_FAILURE
                        && (policy.getIdentifier().getName() + ".EVENT.MANAGER.FINAL").equals(notif.getPolicyName()));

        // should see two restarts
        for (int count = 0; count < 2; ++count) {
            AppcLcmDmaapWrapper appcreq = appcLcmRead.await(req -> "restart".equals(req.getRpcName()));

            // indicate success
            topics.inject(APPC_LCM_WRITE_TOPIC, new File(DUPLICATES_APPC_SUCCESS),
                            appcreq.getBody().getInput().getCommonHeader().getSubRequestId());
        }

        // should see two FINAL successes
        VirtualControlLoopNotification notif1 = waitForFinalSuccess(policy, policyClMgt);
        VirtualControlLoopNotification notif2 = waitForFinalSuccess(policy, policyClMgt);

        // get the list of target names so we can ensure there's one of each
        List<String> actual = List.of(notif1, notif2).stream().map(notif -> notif.getAai().get("generic-vnf.vnf-id"))
                        .sorted().collect(Collectors.toList());

        assertEquals(List.of("duplicate-VNF", "vCPE_Infrastructure_vGMUX_demo_app").toString(), actual.toString());
    }

    // VCPE

    /**
     * Sunny Day with Legacy Tosca Policy.
     */
    @Test
    public void vcpeSunnyDayLegacy() {
        policyClMgt = topics.createListener(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class, controller);
        appcLcmRead = topics.createListener(APPC_LCM_READ_TOPIC, AppcLcmDmaapWrapper.class, APPC_LCM_CODER);

        assertEquals(0, controller.getDrools().factCount(CONTROLLER_NAME));
        policy = rules.setupPolicyFromFile(VCPE_TOSCA_LEGACY_POLICY);
        assertEquals(2, controller.getDrools().factCount(CONTROLLER_NAME));

        // inject an ONSET event over the DCAE topic
        topics.inject(DCAE_TOPIC, new File(VCPE_ONSET_1));

        vcpeSunnyDay();
    }

    /**
     * Sunny Day with Tosca Compliant Policy.
     */
    @Test
    public void vcpeSunnyDayCompliant() {
        policyClMgt = topics.createListener(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class, controller);
        appcLcmRead = topics.createListener(APPC_LCM_READ_TOPIC, AppcLcmDmaapWrapper.class, APPC_LCM_CODER);

        assertEquals(0, controller.getDrools().factCount(CONTROLLER_NAME));
        policy = rules.setupPolicyFromFile(VCPE_TOSCA_COMPLIANT_POLICY);
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
    public void vcpeOnsetFloodPrevention() {
        policyClMgt = topics.createListener(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class, controller);
        appcLcmRead = topics.createListener(APPC_LCM_READ_TOPIC, AppcLcmDmaapWrapper.class, APPC_LCM_CODER);

        assertEquals(0, controller.getDrools().factCount(CONTROLLER_NAME));
        policy = rules.setupPolicyFromFile(VCPE_TOSCA_LEGACY_POLICY);
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
    public void vdnsSunnyDayCompliant() {
        vdnsSunnyDay(VDNS_TOSCA_COMPLIANT_POLICY);
    }

    /**
     * Sunny day scenario for the VCPE use case.
     */
    private void vdnsSunnyDay(String toscaFile) {
        policyClMgt = topics.createListener(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class, controller);

        assertEquals(0, controller.getDrools().factCount(CONTROLLER_NAME));
        policy = rules.setupPolicyFromFile(toscaFile);
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
        policy = rules.setupPolicyFromFile(toscaFile);
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
        policy = rules.setupPolicyFromFile(VLB_TOSCA_COMPLIANT_POLICY);
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

    /**
     * Waits for notifications for LOCK acquisition and GUARD Permit so that event
     * processing may proceed.
     */
    private void waitForLockAndPermit(ToscaPolicy policy, Listener<VirtualControlLoopNotification> policyClMgt) {
        String policyName = policy.getIdentifier().getName();

        policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.ACTIVE
                        && (policyName + ".EVENT").equals(notif.getPolicyName()));

        policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.OPERATION
                        && (policyName + ".EVENT.MANAGER.PROCESSING").equals(notif.getPolicyName())
                        && notif.getMessage().startsWith("Sending guard query"));

        policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.OPERATION
                        && (policyName + ".EVENT.MANAGER.PROCESSING").equals(notif.getPolicyName())
                        && notif.getMessage().startsWith("Guard result") && notif.getMessage().endsWith("Permit"));

        policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.OPERATION
                        && (policyName + ".EVENT.MANAGER.PROCESSING").equals(notif.getPolicyName())
                        && notif.getMessage().startsWith("actor="));
    }

    /**
     * Waits for a FINAL SUCCESS transaction notification.
     *
     * @return the FINAL SUCCESS notification
     */
    private VirtualControlLoopNotification waitForFinalSuccess(ToscaPolicy policy,
                    Listener<VirtualControlLoopNotification> policyClMgt) {

        return policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.FINAL_SUCCESS
                        && (policy.getIdentifier().getName() + ".EVENT.MANAGER.FINAL").equals(notif.getPolicyName()));
    }
}
