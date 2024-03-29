/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2022 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.controlloop.common.rules.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.MapUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.onap.policy.appc.Request;
import org.onap.policy.appclcm.AppcLcmMessageWrapper;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.StandardCoderInstantAsMillis;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.eventmanager.ControlLoopEventManager;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.drools.system.internal.SimpleLockManager;
import org.onap.policy.drools.system.internal.SimpleLockManager.SimpleLock;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.sdnr.PciMessage;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Superclass used for rule tests.
 */
public abstract class BaseTest {
    private static final String APPC_RESTART_OP = "restart";
    private static final String APPC_MODIFY_CONFIG_OP = "ModifyConfig";

    private static final String SDNR_MODIFY_CONFIG_OP = "ModifyConfig";
    private static final String SNDR_MODIFY_CONFIG_ANR_OP = "ModifyConfigANR";

    /*
     * Canonical Topic Names.
     */
    protected static final String DCAE_TOPIC = "dcae_topic";
    protected static final String APPC_LCM_WRITE_TOPIC = "appc-lcm-write";
    protected static final String POLICY_CL_MGT_TOPIC = "policy-cl-mgt";
    protected static final String APPC_LCM_READ_TOPIC = "appc-lcm-read";
    protected static final String APPC_CL_TOPIC = "appc-cl";
    protected static final String SDNR_CL_TOPIC = "sdnr-cl";
    protected static final String SDNR_CL_RSP_TOPIC = "sdnr-cl-rsp";
    protected static final String A1P_CL_TOPIC = "a1-p";
    protected static final String A1P_CL_RSP_TOPIC = "a1-p-rsp";

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
    private static final String VCPE_TOSCA_COMPLIANT_POLICY = "vcpe/tosca-compliant-vcpe.json";
    private static final String VCPE_ONSET_1 = "vcpe/vcpe.onset.1.json";
    private static final String VCPE_ONSET_2 = "vcpe/vcpe.onset.2.json";
    private static final String VCPE_ONSET_3 = "vcpe/vcpe.onset.3.json";
    private static final String VCPE_APPC_SUCCESS = "vcpe/vcpe.appc.success.json";

    // VDNS
    private static final String VDNS_TOSCA_COMPLIANT_POLICY = "vdns/tosca-compliant-vdns.json";
    private static final String VDNS_TOSCA_COMPLIANT_RAINY_POLICY = "vdns/tosca-compliant-vdns-rainy.json";
    private static final String VDNS_ONSET = "vdns/vdns.onset.json";

    // VFW
    private static final String VFW_TOSCA_COMPLIANT_POLICY = "vfw/tosca-compliant-vfw.json";
    private static final String VFW_TOSCA_COMPLIANT_TIME_OUT_POLICY = "vfw/tosca-compliant-timeout-vfw.json";
    private static final String VFW_ONSET = "vfw/vfw.onset.json";
    private static final String VFW_APPC_SUCCESS = "vfw/vfw.appc.success.json";
    private static final String VFW_APPC_FAILURE = "vfw/vfw.appc.failure.json";

    // 5G SON Legacy - PCI
    private static final String VPCI_TOSCA_COMPLIANT_POLICY = "vpci/tosca-compliant-vpci.json";
    private static final String VPCI_ONSET = "vpci/vpci.onset.json";
    private static final String VPCI_SDNR_SUCCESS = "vpci/vpci.sdnr.success.json";

    // 5G SON Legacy - ANR
    private static final String VSONH_TOSCA_COMPLIANT_POLICY = "vsonh/tosca-compliant-vsonh.json";
    private static final String VSONH_ONSET = "vsonh/vsonh.onset.json";
    private static final String VSONH_SDNR_SUCCESS = "vsonh/vsonh.sdnr.success.json";

    // 5G SON Use case Policies (Kohn+)

    private static final String V5G_SON_O1_TOSCA_POLICY = "policies/v5gSonO1.policy.operational.input.tosca.json";
    private static final String V5G_SON_O1_ONSET = "vpci/v5G.son.O1.onset.json";
    private static final String V5G_SON_O1_SDNR_SUCCESS = "vpci/v5G.son.O1.sdnr.success.json";
    private static final String MODIFY_O1_CONFIG_OPERATION = "ModifyO1Config";

    private static final String V5G_SON_A1_TOSCA_POLICY = "policies/v5gSonA1.policy.operational.input.tosca.json";
    private static final String V5G_SON_A1_ONSET = "vsonh/v5G.son.A1.onset.json";
    private static final String V5G_SON_A1_SDNR_SUCCESS = "vsonh/v5G.son.A1.sdnr.success.json";
    private static final String PUT_A1_POLICY_OPERATION = "putA1Policy";
    /*
     * Coders used to decode requests and responses.
     */
    protected static final Coder APPC_LEGACY_CODER = new StandardCoderInstantAsMillis();
    protected static final Coder APPC_LCM_CODER = new StandardCoder();
    protected static final Coder POLICY_CL_MGT_CODER = new PolicyClMgtCoder();

    /*
     * Coders used to decode requests and responses.
     */
    private static final Coder SDNR_CODER = new StandardCoder();

    // these may be overridden by junit tests
    protected static Supplier<HttpClients> httpClientMaker = HttpClients::new;
    protected static Supplier<Simulators> simMaker = Simulators::new;
    protected static Supplier<Topics> topicMaker = Topics::new;

    protected static Rules rules;
    protected static HttpClients httpClients;
    protected static Simulators simulators;

    // used to inject and wait for messages
    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    protected static Topics topics;

    // used to wait for messages on SINK topics
    protected Listener<VirtualControlLoopNotification> policyClMgt;
    protected Listener<Request> appcClSink;
    protected Listener<AppcLcmMessageWrapper> appcLcmRead;
    protected Listener<PciMessage> sdnrClSink;

    /*
     * Tosca Policy that was loaded.
     */
    protected ToscaPolicy policy;

    /**
     * Initializes {@link #rules}, {@link #httpClients}, and {@link #simulators}.
     */
    public static void initStatics() {
        httpClients = httpClientMaker.get();
        simulators = simMaker.get();
    }

    /**
     * Destroys {@link #httpClients}, {@link #simulators}, and {@link #rules}.
     */
    public static void finishStatics() {
        httpClients.destroy();
        simulators.destroy();
    }

    /**
     * Initializes {@link #topics}.
     */
    public void init() {
        setTopics(topicMaker.get());

        Map<String, SimpleLock> locks = getLockMap();
        if (!MapUtils.isEmpty(locks)) {
            locks.clear();
        }
    }

    /**
     * Destroys {@link #topics} and resets the rule facts.
     */
    public void finish() {
        topics.destroy();
    }

    // Service123 (i.e., Policy with multiple operations)

    /**
     * Service123 with Tosca Compliant Policy.
     */
    @Test
    public void testService123Compliant() {
        policyClMgt = createNoficationTopicListener();
        appcLcmRead = topics.createListener(APPC_LCM_READ_TOPIC, AppcLcmMessageWrapper.class, APPC_LCM_CODER);
        policy = checkPolicy(SERVICE123_TOSCA_COMPLIANT_POLICY);

        // inject an ONSET event over the DCAE topic
        topics.inject(DCAE_TOPIC, SERVICE123_ONSET);
        /* Wait to acquire a LOCK and a PDP-X PERMIT */

        waitForLockAndPermit(policy, policyClMgt);

        // restart request should be sent and fail four times (i.e., because retry=3)
        for (var count = 0; count < 4; ++count) {
            AppcLcmMessageWrapper appcreq = appcLcmRead.await(req -> APPC_RESTART_OP.equals(req.getRpcName()));

            topics.inject(APPC_LCM_WRITE_TOPIC, SERVICE123_APPC_RESTART_FAILURE,
                            appcreq.getBody().getInput().getCommonHeader().getSubRequestId());
        }
        // rebuild request should be sent and fail once
        AppcLcmMessageWrapper appcreq = appcLcmRead.await(req -> "rebuild".equals(req.getRpcName()));
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

        verifyUnlocked();
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
        policyClMgt = createNoficationTopicListener();
        appcLcmRead = topics.createListener(APPC_LCM_READ_TOPIC, AppcLcmMessageWrapper.class, APPC_LCM_CODER);

        policy = checkPolicy(DUPLICATES_TOSCA_COMPLIANT_POLICY);

        final long initCount = getCreateCount();

        /*
         * Inject ONSET events over the DCAE topic. First and last have the same target
         * entity, but different request IDs - only one should succeed. The middle one is
         * for a different target entity, so it should succeed.
         */
        topics.inject(DCAE_TOPIC, DUPLICATES_ONSET_1, UUID.randomUUID().toString());
        topics.inject(DCAE_TOPIC, DUPLICATES_ONSET_2);
        topics.inject(DCAE_TOPIC, DUPLICATES_ONSET_1, UUID.randomUUID().toString());

        // should see two restarts
        for (var count = 0; count < 2; ++count) {
            AppcLcmMessageWrapper appcreq = appcLcmRead.await(req -> APPC_RESTART_OP.equals(req.getRpcName()));

            // indicate success
            topics.inject(APPC_LCM_WRITE_TOPIC, DUPLICATES_APPC_SUCCESS,
                            appcreq.getBody().getInput().getCommonHeader().getSubRequestId());
        }

        // should see two FINAL successes
        VirtualControlLoopNotification notif1 = waitForFinalSuccess(policy, policyClMgt);
        VirtualControlLoopNotification notif2 = waitForFinalSuccess(policy, policyClMgt);

        // get the list of target names, so we can ensure there's one of each
        List<String> actual = Stream.of(notif1, notif2).map(notif -> notif.getAai().get("generic-vnf.vnf-id"))
                        .sorted().collect(Collectors.toList());

        assertEquals(List.of("duplicate-VNF", "vCPE_Infrastructure_vGMUX_demo_app").toString(), actual.toString());

        long added = getCreateCount() - initCount;
        assertEquals(2, added);

        verifyUnlocked();
    }

    // VCPE

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

    /**
     * Vdns Rainy Day with Compliant Tosca Policy.
     */
    @Test
    public void testVdnsRainyDayCompliant() {
        httpRainyDay(VDNS_TOSCA_COMPLIANT_RAINY_POLICY, VDNS_ONSET);
    }


    // VFW

    /**
     * VFW Sunny Day with Tosca Compliant Policy.
     */
    @Test
    public void testVfwSunnyDayCompliant() {
        appcLegacySunnyDay(VFW_TOSCA_COMPLIANT_POLICY, VFW_ONSET, APPC_MODIFY_CONFIG_OP);
    }

    /**
     * VFW Rainy Day using compliant tosca policy (final failure).
     */
    @Test
    public void testVfwRainyDayOverallTimeout() {
        appcLegacyRainyDayNoResponse(VFW_TOSCA_COMPLIANT_TIME_OUT_POLICY, VFW_ONSET, APPC_MODIFY_CONFIG_OP);
    }

    /**
     * VFW Rainy day using compliant tosca policy (final failure due to timeout).
     */
    @Test
    public void testVfwRainyDayCompliantTimeout() {
        appcLegacyRainyDayNoResponse(VFW_TOSCA_COMPLIANT_POLICY, VFW_ONSET, APPC_MODIFY_CONFIG_OP);
    }

    /**
     * VPCI Sunny Day Tosca Policy.
     */
    @Test
    public void testVpciSunnyDayCompliant() {
        sdnrSunnyDay(VPCI_TOSCA_COMPLIANT_POLICY, VPCI_ONSET, VPCI_SDNR_SUCCESS,
            SDNR_MODIFY_CONFIG_OP, SDNR_CL_TOPIC, SDNR_CL_RSP_TOPIC);
    }

    // VSONH

    /**
     * VSONH Sunny Day with Tosca Policy.
     */
    @Test
    public void testVsonhSunnyDayCompliant() {
        sdnrSunnyDay(VSONH_TOSCA_COMPLIANT_POLICY, VSONH_ONSET, VSONH_SDNR_SUCCESS,
            SNDR_MODIFY_CONFIG_ANR_OP, SDNR_CL_TOPIC, SDNR_CL_RSP_TOPIC);
    }

    /**
     * Sunny day 5G SON 01 Modify01Config Operational Policy.
     */
    @Test
    public void test5gSonO1SunnyDayCompliant() {
        sdnrSunnyDay(V5G_SON_O1_TOSCA_POLICY, V5G_SON_O1_ONSET, V5G_SON_O1_SDNR_SUCCESS,
            MODIFY_O1_CONFIG_OPERATION, SDNR_CL_TOPIC, SDNR_CL_RSP_TOPIC);
    }

    /**
     * Sunny day 5G SON A1 ModifyA1Policy Operational Policy.
     */
    @Test
    public void test5gSonA1SunnyDayCompliant() {
        sdnrSunnyDay(V5G_SON_A1_TOSCA_POLICY, V5G_SON_A1_ONSET, V5G_SON_A1_SDNR_SUCCESS,
            PUT_A1_POLICY_OPERATION, A1P_CL_TOPIC, A1P_CL_RSP_TOPIC);
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
        policyClMgt = createNoficationTopicListener();
        appcLcmRead = topics.createListener(APPC_LCM_READ_TOPIC, AppcLcmMessageWrapper.class, APPC_LCM_CODER);

        policy = checkPolicy(policyFile);


        // inject several ONSET events over the DCAE topic
        for (String onsetFile : onsetFiles) {
            topics.inject(DCAE_TOPIC, onsetFile);
        }

        /* Wait to acquire a LOCK and a PDP-X PERMIT */
        waitForLockAndPermit(policy, policyClMgt);

        /*
         * Ensure that an APPC RESTART request was sent in response to the matching ONSET
         */
        AppcLcmMessageWrapper appcreq = appcLcmRead.await(req -> operation.equals(req.getRpcName()));

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

        verifyUnlocked();
    }

    /**
     * Sunny day scenario for use cases that use Legacy APPC.
     *
     * @param policyFile file containing the ToscaPolicy to be loaded
     * @param onsetFile file containing the ONSET to be injected
     * @param operation expected APPC operation request
     */
    protected void appcLegacySunnyDay(String policyFile, String onsetFile, String operation) {
        policyClMgt = createNoficationTopicListener();
        appcClSink = topics.createListener(APPC_CL_TOPIC, Request.class, APPC_LEGACY_CODER);

        policy = checkPolicy(policyFile);

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

        verifyUnlocked();
    }

    /**
     * Rainy day scenario for use cases that use Legacy APPC.
     *
     * @param policyFile file containing the ToscaPolicy to be loaded
     * @param onsetFile file containing the ONSET to be injected
     * @param operation expected APPC operation request
     */
    protected void appcLegacyRainyDay(String policyFile, String onsetFile, String operation) {
        policyClMgt = createNoficationTopicListener();
        appcClSink = topics.createListener(APPC_CL_TOPIC, Request.class, APPC_LEGACY_CODER);

        policy = checkPolicy(policyFile);

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
        waitForOperationFailure();

        /* --- Transaction Completed --- */
        waitForFinalFailure(policy, policyClMgt);

        verifyUnlocked();
    }

    /**
     * Rainy day scenario for use cases that use Legacy APPC. Expected to fail due to
     * timeout.
     *
     * @param policyFile file containing the ToscaPolicy to be loaded
     * @param onsetFile file containing the ONSET to be injected
     * @param operation expected APPC operation request
     */
    protected void appcLegacyRainyDayNoResponse(String policyFile, String onsetFile, String operation) {
        policyClMgt = createNoficationTopicListener();
        appcClSink = topics.createListener(APPC_CL_TOPIC, Request.class, APPC_LEGACY_CODER);

        policy = checkPolicy(policyFile);

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

        verifyUnlocked();
    }

    /**
     * Sunny day scenario for use cases that use SDNR.
     *
     * @param policyFile file containing the ToscaPolicy to be loaded
     * @param onsetFile file containing the ONSET to be injected
     * @param operation expected SDNR operation request
     */
    protected void sdnrSunnyDay(String policyFile, String onsetFile,
                                String successFile, String operation,
                                String requestTopic, String responseTopic) {
        policyClMgt = createNoficationTopicListener();
        sdnrClSink = topics.createListener(requestTopic, PciMessage.class, SDNR_CODER);

        policy = checkPolicy(policyFile);

        /* Inject an ONSET event over the DCAE topic */
        topics.inject(DCAE_TOPIC, onsetFile);

        /* Wait to acquire a LOCK and a PDP-X PERMIT */
        waitForLockAndPermit(policy, policyClMgt);

        /*
         * Ensure that an SDNR RESTART request was sent in response to the matching ONSET
         */
        PciMessage pcireq = sdnrClSink.await(req -> operation.equals(req.getBody().getInput().getAction()));

        /*
         * Inject response.
         */
        topics.inject(responseTopic, successFile, pcireq.getBody().getInput().getCommonHeader().getSubRequestId());

        /* --- Operation Completed --- */

        waitForOperationSuccess();

        /* --- Transaction Completed --- */
        waitForFinalSuccess(policy, policyClMgt);

        verifyUnlocked();
    }

    /**
     * Sunny day scenario for use cases that use an HTTP simulator.
     *
     * @param policyFile file containing the ToscaPolicy to be loaded
     * @param onsetFile file containing the ONSET to be injected
     */
    protected void httpSunnyDay(String policyFile, String onsetFile) {
        policyClMgt = createNoficationTopicListener();

        policy = checkPolicy(policyFile);

        /* Inject an ONSET event over the DCAE topic */
        topics.inject(DCAE_TOPIC, onsetFile);

        /* Wait to acquire a LOCK and a PDP-X PERMIT */
        waitForLockAndPermit(policy, policyClMgt);

        /* --- Operation Completed --- */

        waitForOperationSuccess();

        /* --- Transaction Completed --- */
        waitForFinalSuccess(policy, policyClMgt);

        verifyUnlocked();
    }

    /**
     * Rainy day scenario for use cases that use an HTTP simulator.
     *
     * @param policyFile file containing the ToscaPolicy to be loaded
     * @param onsetFile file containing the ONSET to be injected
     */
    protected void httpRainyDay(String policyFile, String onsetFile) {
        policyClMgt = createNoficationTopicListener();

        policy = checkPolicy(policyFile);

        /* Inject an ONSET event over the DCAE topic */
        topics.inject(DCAE_TOPIC, onsetFile);

        /* Wait to acquire a LOCK and a PDP-X PERMIT */
        waitForLockAndPermit(policy, policyClMgt);

        /* --- Operation Completed --- */
        waitForOperationFailure();

        /* --- Transaction Completed --- */
        waitForFinalFailure(policy, policyClMgt);

        verifyUnlocked();
    }

    protected long getCreateCount() {
        return ControlLoopEventManager.getCreateCount();
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

    /**
     * Returns ToscaPolicy from File.
     *
     * @param fileName a path name
     * @return ToscaPolicy
     */
    protected ToscaPolicy checkPolicy(String fileName) {
        try {
            return Rules.getPolicyFromFile(fileName);
        } catch (CoderException e) {
            throw new IllegalArgumentException(fileName, e);
        }
    }

    /**
     * Creates a Coder for PolicyClMgt from StandardCoder.
     *
     */
    public static class PolicyClMgtCoder extends StandardCoder {
        public PolicyClMgtCoder() {
            super(org.onap.policy.controlloop.util.Serialization.gson,
                            org.onap.policy.controlloop.util.Serialization.gsonPretty);
        }
    }

    /**
     * Returns Listener from createListener based on Coder.
     *
     * @return the Listener
     */
    protected Listener<VirtualControlLoopNotification> createNoficationTopicListener() {
        return topics.createListener(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class, POLICY_CL_MGT_CODER);
    }

    /**
     * Verifies that all locks have been released, waiting a bit, if necessary.
     */
    private void verifyUnlocked() {
        Map<String, SimpleLock> locks = getLockMap();
        if (!MapUtils.isEmpty(locks)) {
            Awaitility.await().atMost(5, TimeUnit.SECONDS).until(locks::isEmpty);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, SimpleLock> getLockMap() {
        Object lockMgr = ReflectionTestUtils.getField(PolicyEngineConstants.getManager(), "lockManager");
        if (lockMgr instanceof SimpleLockManager) {
            return (Map<String, SimpleLock>) ReflectionTestUtils.getField(lockMgr, "resource2lock");
        }

        return Collections.emptyMap();
    }
}
