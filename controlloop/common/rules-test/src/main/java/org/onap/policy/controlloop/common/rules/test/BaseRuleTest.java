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

package org.onap.policy.controlloop.common.rules.test;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import org.junit.Test;
import org.onap.policy.appc.Request;
import org.onap.policy.appclcm.AppcLcmDmaapWrapper;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.StandardCoderInstantAsMillis;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;

/**
 * Superclass used for rule tests.
 */
public abstract class BaseRuleTest {
    private static final String APPC_RESTART_OP = "restart";

    /*
     * Canonical Topic Names.
     */
    protected static final String DCAE_TOPIC = "DCAE_TOPIC";
    protected static final String APPC_LCM_WRITE_TOPIC = "APPC-LCM-WRITE";
    protected static final String POLICY_CL_MGT_TOPIC = "POLICY-CL-MGT";
    protected static final String APPC_LCM_READ_TOPIC = "APPC-LCM-READ";
    protected static final String APPC_CL_TOPIC = "APPC-CL";

    /*
     * Constants for each test case.
     */

    // service123 (i.e., multi-operation policy)
    private static final String SERVICE123_TOSCA_COMPLIANT_POLICY = "service123/tosca-compliant-service123.json";
    private static final String SERVICE123_ONSET = "service123/service123.onset.json";
    private static final String SERVICE123_APPC_RESTART_FAILURE = "service123/service123.appc.restart.failure.json";
    private static final String SERVICE123_APPC_REBUILD_FAILURE = "service123/service123.appc.rebuild.failure.json";
    private static final String SERVICE123_APPC_MIGRATE_SUCCESS = "service123/service123.appc.migrate.success.json";

    // duplicates (i.e., mutliple events in the engine at the same time)
    private static final String DUPLICATES_TOSCA_COMPLIANT_POLICY = "duplicates/tosca-compliant-duplicates.json";
    private static final String DUPLICATES_ONSET_1 = "duplicates/duplicates.onset.1.json";
    private static final String DUPLICATES_ONSET_2 = "duplicates/duplicates.onset.2.json";
    private static final String DUPLICATES_APPC_SUCCESS = "duplicates/duplicates.appc.success.json";

    // VCPE
    private static final String VCPE_TOSCA_LEGACY_POLICY = "vcpe/tosca-legacy-vcpe.json";
    private static final String VCPE_TOSCA_COMPLIANT_POLICY = "vcpe/tosca-compliant-vcpe.json";
    private static final String VCPE_ONSET_1 = "vcpe/vcpe.onset.1.json";
    private static final String VCPE_ONSET_2 = "vcpe/vcpe.onset.2.json";
    private static final String VCPE_ONSET_3 = "vcpe/vcpe.onset.3.json";
    private static final String VCPE_APPC_SUCCESS = "vcpe/vcpe.appc.success.json";

    // VDNS
    private static final String VDNS_TOSCA_COMPLIANT_POLICY = "vdns/tosca-compliant-vdns.json";
    private static final String VDNS_ONSET = "vdns/vdns.onset.json";

    // VFW
    private static final String VFW_TOSCA_LEGACY_POLICY = "vfw/tosca-vfw.json";
    private static final String VFW_TOSCA_COMPLIANT_POLICY = "vfw/tosca-compliant-vfw.json";
    private static final String VFW_TOSCA_COMPLIANT_TIME_OUT_POLICY = "vfw/tosca-compliant-timeout-vfw.json";
    private static final String VFW_ONSET = "vfw/vfw.onset.json";
    private static final String VFW_APPC_SUCCESS = "vfw/vfw.appc.success.json";
    private static final String VFW_APPC_FAILURE = "vfw/vfw.appc.failure.json";

    // VLB
    private static final String VLB_TOSCA_LEGACY_POLICY = "vlb/tosca-vlb.json";
    private static final String VLB_TOSCA_COMPLIANT_POLICY = "vlb/tosca-compliant-vlb.json";
    private static final String VLB_ONSET = "vlb/vlb.onset.json";

    /*
     * Coders used to decode requests and responses.
     */
    private static final Coder APPC_LEGACY_CODER = new StandardCoderInstantAsMillis();
    private static final Coder APPC_LCM_CODER = new StandardCoder();

    // these may be overridden by junit tests
    private static Function<String, Rules> ruleMaker = Rules::new;
    private static Supplier<HttpClients> httpClientMaker = HttpClients::new;
    private static Supplier<Simulators> simMaker = Simulators::new;
    private static Supplier<Topics> topicMaker = Topics::new;

    protected static Rules rules;
    protected static HttpClients httpClients;
    protected static Simulators simulators;

    // used to inject and wait for messages
    @Getter(AccessLevel.PROTECTED)
    private Topics topics;

    // used to wait for messages on SINK topics
    protected Listener<VirtualControlLoopNotification> policyClMgt;
    protected Listener<Request> appcClSink;
    protected Listener<AppcLcmDmaapWrapper> appcLcmRead;

    protected PolicyController controller;

    /*
     * Tosca Policy that was loaded.
     */
    protected ToscaPolicy policy;


    /**
     * Initializes {@link #rules}, {@link #httpClients}, and {@link #simulators}.
     *
     * @param controllerName the rule controller name
     */
    public static void initStatics(String controllerName) {
        rules = ruleMaker.apply(controllerName);
        httpClients = httpClientMaker.get();
        simulators = simMaker.get();
    }

    /**
     * Destroys {@link #httpClients}, {@link #simulators}, and {@link #rules}.
     */
    public static void finishStatics() {
        httpClients.destroy();
        simulators.destroy();
        rules.destroy();
    }

    /**
     * Initializes {@link #topics} and {@link #controller}.
     */
    public void init() {
        topics = topicMaker.get();
        controller = rules.getController();
    }

    /**
     * Destroys {@link #topics} and resets the rule facts.
     */
    public void finish() {
        topics.destroy();
        rules.resetFacts();
    }

    // Service123 (i.e., Policy with multiple operations)

    /**
     * Service123 with Tosca Compliant Policy.
     */
    @Test
    public void testService123Compliant() {
        policyClMgt = topics.createListener(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class, controller);
        appcLcmRead = topics.createListener(APPC_LCM_READ_TOPIC, AppcLcmDmaapWrapper.class, APPC_LCM_CODER);

        assertEquals(0, controller.getDrools().factCount(rules.getControllerName()));
        policy = rules.setupPolicyFromFile(SERVICE123_TOSCA_COMPLIANT_POLICY);
        assertEquals(2, controller.getDrools().factCount(rules.getControllerName()));

        // inject an ONSET event over the DCAE topic
        topics.inject(DCAE_TOPIC, SERVICE123_ONSET);

        /* Wait to acquire a LOCK and a PDP-X PERMIT */
        waitForLockAndPermit(policy, policyClMgt);

        // restart request should be sent and fail four times (i.e., because retry=3)
        for (int count = 0; count < 4; ++count) {
            AppcLcmDmaapWrapper appcreq = appcLcmRead.await(req -> APPC_RESTART_OP.equals(req.getRpcName()));

            topics.inject(APPC_LCM_WRITE_TOPIC, SERVICE123_APPC_RESTART_FAILURE,
                            appcreq.getBody().getInput().getCommonHeader().getSubRequestId());
        }

        // rebuild request should be sent and fail once
        AppcLcmDmaapWrapper appcreq = appcLcmRead.await(req -> "rebuild".equals(req.getRpcName()));

        topics.inject(APPC_LCM_WRITE_TOPIC, SERVICE123_APPC_REBUILD_FAILURE,
                        appcreq.getBody().getInput().getCommonHeader().getSubRequestId());

        // migrate request should be sent and succeed
        appcreq = appcLcmRead.await(req -> "migrate".equals(req.getRpcName()));

        topics.inject(APPC_LCM_WRITE_TOPIC, SERVICE123_APPC_MIGRATE_SUCCESS,
                        appcreq.getBody().getInput().getCommonHeader().getSubRequestId());

        /* --- Operation Completed --- */

        waitForOperationSuccess();

        /* --- Transaction Completed --- */
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
    public void testDuplicatesEvents() {
        policyClMgt = topics.createListener(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class, controller);
        appcLcmRead = topics.createListener(APPC_LCM_READ_TOPIC, AppcLcmDmaapWrapper.class, APPC_LCM_CODER);

        assertEquals(0, controller.getDrools().factCount(rules.getControllerName()));
        policy = rules.setupPolicyFromFile(DUPLICATES_TOSCA_COMPLIANT_POLICY);
        assertEquals(2, controller.getDrools().factCount(rules.getControllerName()));

        /*
         * Inject ONSET events over the DCAE topic. First and last have the same target
         * entity, but different request IDs - only one should succeed. The middle one is
         * for a different target entity, so it should succeed.
         */
        topics.inject(DCAE_TOPIC, DUPLICATES_ONSET_1, UUID.randomUUID().toString());
        topics.inject(DCAE_TOPIC, DUPLICATES_ONSET_2);
        topics.inject(DCAE_TOPIC, DUPLICATES_ONSET_1, UUID.randomUUID().toString());

        // one should immediately generate a FINAL failure
        waitForFinal(policy, policyClMgt, ControlLoopNotificationType.FINAL_FAILURE);

        // should see two restarts
        for (int count = 0; count < 2; ++count) {
            AppcLcmDmaapWrapper appcreq = appcLcmRead.await(req -> APPC_RESTART_OP.equals(req.getRpcName()));

            // indicate success
            topics.inject(APPC_LCM_WRITE_TOPIC, DUPLICATES_APPC_SUCCESS,
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
    public void testVcpeSunnyDayLegacy() {
        appcLcmSunnyDay(VCPE_TOSCA_LEGACY_POLICY, VCPE_ONSET_1, APPC_RESTART_OP);
    }

    /**
     * Sunny Day with Tosca Compliant Policy.
     */
    @Test
    public void testVcpeSunnyDayCompliant() {
        appcLcmSunnyDay(VCPE_TOSCA_COMPLIANT_POLICY, VCPE_ONSET_1, APPC_RESTART_OP);
    }

    /**
     * An ONSET flood prevention test that injects a few ONSETs at once. It attempts to
     * simulate the flooding behavior of the DCAE TCA microservice. TCA could blast tens
     * or hundreds of ONSETs within sub-second intervals.
     */
    @Test
    public void testVcpeOnsetFloodPrevention() {
        appcLcmSunnyDay(VCPE_TOSCA_COMPLIANT_POLICY, List.of(VCPE_ONSET_1, VCPE_ONSET_2, VCPE_ONSET_3),
                        APPC_RESTART_OP);
    }

    // VDNS

    /**
     * Sunny Day with Tosca Compliant Policy.
     */
    @Test
    public void testVdnsSunnyDayCompliant() {
        httpSunnyDay(VDNS_TOSCA_COMPLIANT_POLICY, VDNS_ONSET);
    }

    // VFW

    /**
     * VFW Sunny Day with Legacy Tosca Policy.
     */
    @Test
    public void testVfwSunnyDayLegacy() {
        appcLegacySunnyDay(VFW_TOSCA_LEGACY_POLICY, VFW_ONSET, "ModifyConfig");
    }

    /**
     * VFW Sunny Day with Tosca Compliant Policy.
     */
    @Test
    public void testVfwSunnyDayCompliant() {
        appcLegacySunnyDay(VFW_TOSCA_COMPLIANT_POLICY, VFW_ONSET, "ModifyConfig");
    }
    
    /**
     * VFW Rainy Day using legacy tosca policy (failure).
     */
    @Test
    public void testVfwRainyDayLegacyFailure() {
        appcLegacyRainyDay(VFW_TOSCA_LEGACY_POLICY, VFW_ONSET, "ModifyConfig", true);
    }
    
    /**
     * VFW Rainy Day using compliant tosca policy (failure).
     */
    @Test
    public void testVfwRainyDayOverallTimeout() {
        appcLegacyRainyDay(VFW_TOSCA_COMPLIANT_TIME_OUT_POLICY, VFW_ONSET, "ModifyConfig", false);
    }

    /**
     * VFW Rainy day using compliant tosca policy (failure due to timeout).
     */
    @Test
    public void testVfwRainyDayCompliantTimeout() {
        appcLegacyRainyDayNoResponse(VFW_TOSCA_COMPLIANT_POLICY, VFW_ONSET, "ModifyConfig");
    }

    // VLB

    /**
     * Sunny Day with Legacy Tosca Policy.
     */
    @Test
    public void testVlbSunnyDayLegacy() {
        httpSunnyDay(VLB_TOSCA_LEGACY_POLICY, VLB_ONSET);
    }

    /**
     * Sunny Day with Tosca Compliant Policy.
     */
    @Test
    public void testVlbSunnyDayCompliant() {
        httpSunnyDay(VLB_TOSCA_COMPLIANT_POLICY, VLB_ONSET);
    }

    /**
     * Sunny day scenario for use cases that use APPC-LCM.
     *
     * @param policyFile file containing the ToscaPolicy to be loaded
     * @param onsetFile file containing the ONSET to be injected
     * @param operation expected APPC operation request
     */
    protected void appcLcmSunnyDay(String policyFile, String onsetFile, String operation) {
        appcLcmSunnyDay(policyFile, List.of(onsetFile), operation);
    }

    /**
     * Sunny day scenario for use cases that use APPC-LCM.
     *
     * @param policyFile file containing the ToscaPolicy to be loaded
     * @param onsetFiles list of files containing the ONSET to be injected
     * @param operation expected APPC operation request
     */
    protected void appcLcmSunnyDay(String policyFile, List<String> onsetFiles, String operation) {
        policyClMgt = topics.createListener(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class, controller);
        appcLcmRead = topics.createListener(APPC_LCM_READ_TOPIC, AppcLcmDmaapWrapper.class, APPC_LCM_CODER);

        assertEquals(0, controller.getDrools().factCount(rules.getControllerName()));
        policy = rules.setupPolicyFromFile(policyFile);
        assertEquals(2, controller.getDrools().factCount(rules.getControllerName()));

        // inject several ONSET events over the DCAE topic
        for (String onsetFile : onsetFiles) {
            topics.inject(DCAE_TOPIC, onsetFile);
        }

        /* Wait to acquire a LOCK and a PDP-X PERMIT */
        waitForLockAndPermit(policy, policyClMgt);

        /*
         * Ensure that an APPC RESTART request was sent in response to the matching ONSET
         */
        AppcLcmDmaapWrapper appcreq = appcLcmRead.await(req -> operation.equals(req.getRpcName()));

        /*
         * Inject a 400 APPC Response Return over the APPC topic, with appropriate
         * subRequestId
         */
        topics.inject(APPC_LCM_WRITE_TOPIC, VCPE_APPC_SUCCESS,
                        appcreq.getBody().getInput().getCommonHeader().getSubRequestId());

        /* --- Operation Completed --- */

        waitForOperationSuccess();

        /* --- Transaction Completed --- */
        waitForFinalSuccess(policy, policyClMgt);
    }

    /**
     * Sunny day scenario for use cases that use Legacy APPC.
     *
     * @param policyFile file containing the ToscaPolicy to be loaded
     * @param onsetFile file containing the ONSET to be injected
     * @param operation expected APPC operation request
     */
    protected void appcLegacySunnyDay(String policyFile, String onsetFile, String operation) {
        policyClMgt = topics.createListener(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class, controller);
        appcClSink = topics.createListener(APPC_CL_TOPIC, Request.class, APPC_LEGACY_CODER);

        assertEquals(0, controller.getDrools().factCount(rules.getControllerName()));
        policy = rules.setupPolicyFromFile(policyFile);
        assertEquals(2, controller.getDrools().factCount(rules.getControllerName()));

        /* Inject an ONSET event over the DCAE topic */
        topics.inject(DCAE_TOPIC, onsetFile);

        /* Wait to acquire a LOCK and a PDP-X PERMIT */
        waitForLockAndPermit(policy, policyClMgt);

        /*
         * Ensure that an APPC RESTART request was sent in response to the matching ONSET
         */
        Request appcreq = appcClSink.await(req -> operation.equals(req.getAction()));

        /*
         * Inject a 400 APPC Response Return over the APPC topic, with appropriate
         * subRequestId
         */
        topics.inject(APPC_CL_TOPIC, VFW_APPC_SUCCESS, appcreq.getCommonHeader().getSubRequestId());

        /* --- Operation Completed --- */

        waitForOperationSuccess();

        /* --- Transaction Completed --- */
        waitForFinalSuccess(policy, policyClMgt);
    }
    
    /**
     * Rainy day scenario for use cases that use Legacy APPC.
     *
     * @param policyFile file containing the ToscaPolicy to be loaded
     * @param onsetFile file containing the ONSET to be injected
     * @param operation expected APPC operation request
     * @param checkOperation flag to determine whether or not to wait for operation timeout
     */
    protected void appcLegacyRainyDay(String policyFile, String onsetFile, String operation, boolean checkOperation) {
        policyClMgt = topics.createListener(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class, controller);
        appcClSink = topics.createListener(APPC_CL_TOPIC, Request.class, APPC_LEGACY_CODER);

        assertEquals(0, controller.getDrools().factCount(rules.getControllerName()));
        policy = rules.setupPolicyFromFile(policyFile);
        assertEquals(2, controller.getDrools().factCount(rules.getControllerName()));

        /* Inject an ONSET event over the DCAE topic */
        topics.inject(DCAE_TOPIC, onsetFile);

        /* Wait to acquire a LOCK and a PDP-X PERMIT */
        waitForLockAndPermit(policy, policyClMgt);

        /*
         * Ensure that an APPC RESTART request was sent in response to the matching ONSET
         */
        Request appcreq = appcClSink.await(req -> operation.equals(req.getAction()));

        /*
         * Inject a 401 APPC Response Return over the APPC topic, with appropriate
         * subRequestId
         */
        topics.inject(APPC_CL_TOPIC, VFW_APPC_FAILURE, appcreq.getCommonHeader().getSubRequestId());

        /* --- Operation Completed --- */
        if (checkOperation) {
            waitForOperationFailure();
        }

        /* --- Transaction Completed --- */
        waitForFinalFailure(policy, policyClMgt);
    }
    
    /**
     * Rainy day scenario for use cases that use Legacy APPC.
     * Expected to fail due to timeout.
     *
     * @param policyFile file containing the ToscaPolicy to be loaded
     * @param onsetFile file containing the ONSET to be injected
     * @param operation expected APPC operation request
     */
    protected void appcLegacyRainyDayNoResponse(String policyFile, String onsetFile, String operation) {
        policyClMgt = topics.createListener(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class, controller);
        appcClSink = topics.createListener(APPC_CL_TOPIC, Request.class, APPC_LEGACY_CODER);

        assertEquals(0, controller.getDrools().factCount(rules.getControllerName()));
        policy = rules.setupPolicyFromFile(policyFile);
        assertEquals(2, controller.getDrools().factCount(rules.getControllerName()));

        /* Inject an ONSET event over the DCAE topic */
        topics.inject(DCAE_TOPIC, onsetFile);

        /* Wait to acquire a LOCK and a PDP-X PERMIT */
        waitForLockAndPermit(policy, policyClMgt);

        /*
         * Ensure that an APPC RESTART request was sent in response to the matching ONSET
         */
        appcClSink.await(req -> operation.equals(req.getAction()));

        /*
         * Do not inject an APPC Response.
         */
        
        /* --- Transaction Completed --- */
        waitForFinalFailure(policy, policyClMgt);
    }

    /**
     * Sunny day scenario for use cases that use an HTTP simulator.
     *
     * @param policyFile file containing the ToscaPolicy to be loaded
     * @param onsetFile file containing the ONSET to be injected
     * @param operation expected APPC operation request
     */
    protected void httpSunnyDay(String policyFile, String onsetFile) {
        policyClMgt = topics.createListener(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class, controller);

        assertEquals(0, controller.getDrools().factCount(rules.getControllerName()));
        policy = rules.setupPolicyFromFile(policyFile);
        assertEquals(2, controller.getDrools().factCount(rules.getControllerName()));

        /* Inject an ONSET event over the DCAE topic */
        topics.inject(DCAE_TOPIC, onsetFile);

        /* Wait to acquire a LOCK and a PDP-X PERMIT */
        waitForLockAndPermit(policy, policyClMgt);

        /* --- Operation Completed --- */

        waitForOperationSuccess();

        /* --- Transaction Completed --- */
        waitForFinalSuccess(policy, policyClMgt);
    }

    /**
     * Waits for a OPERATION SUCCESS transaction notification.
     */
    protected void waitForOperationSuccess() {
        policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.OPERATION_SUCCESS);
    }

    /**
     * Waits for a FINAL SUCCESS transaction notification.
     *
     * @return the FINAL SUCCESS notification
     */
    protected VirtualControlLoopNotification waitForFinalSuccess(ToscaPolicy policy,
                    Listener<VirtualControlLoopNotification> policyClMgt) {

        return this.waitForFinal(policy, policyClMgt, ControlLoopNotificationType.FINAL_SUCCESS);
    }
    
    /**
     * Waits for a OPERATION FAILURE transaction notification.
     */
    protected void waitForOperationFailure() {
        policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.OPERATION_FAILURE);
    }

    /**
     * Waits for a FINAL FAILURE transaction notification.
     *
     * @return the FINAL FAILURE notification
     */
    protected VirtualControlLoopNotification waitForFinalFailure(ToscaPolicy policy,
                    Listener<VirtualControlLoopNotification> policyClMgt) {

        return this.waitForFinal(policy, policyClMgt, ControlLoopNotificationType.FINAL_FAILURE);
    }

    /**
     * Waits for notifications for LOCK acquisition and GUARD Permit so that event
     * processing may proceed.
     */
    protected abstract void waitForLockAndPermit(ToscaPolicy policy,
                    Listener<VirtualControlLoopNotification> policyClMgt);

    /**
     * Waits for a FINAL transaction notification.
     *
     * @param finalType FINAL_xxx type for which to wait
     *
     * @return the FINAL notification
     */
    protected abstract VirtualControlLoopNotification waitForFinal(ToscaPolicy policy,
                    Listener<VirtualControlLoopNotification> policyClMgt, ControlLoopNotificationType finalType);
}
