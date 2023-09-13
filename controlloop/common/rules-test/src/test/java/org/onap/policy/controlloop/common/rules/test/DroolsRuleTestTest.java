/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021,2023 Nordix Foundation.
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.onap.policy.appclcm.AppcLcmBody;
import org.onap.policy.appclcm.AppcLcmCommonHeader;
import org.onap.policy.appclcm.AppcLcmDmaapWrapper;
import org.onap.policy.appclcm.AppcLcmInput;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.drools.controller.DroolsController;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.springframework.test.util.ReflectionTestUtils;

class DroolsRuleTestTest {

    private static final String CONTROLLER_NAME = "my-controller-name";
    private static final String POLICY_NAME = "my-policy-name";

    // saved values
    private static Function<String, Rules> ruleMaker;
    private static Supplier<HttpClients> httpClientMaker;
    private static Supplier<Simulators> simMaker;
    private static Supplier<Topics> topicMaker;

    private DroolsRuleTest base;
    private LinkedList<VirtualControlLoopNotification> clMgtQueue;
    private Queue<AppcLcmDmaapWrapper> appcLcmQueue;
    private int permitCount;
    private int finalCount;

    private final PolicyController controller = mock(PolicyController.class);
    private final Rules rules = mock(Rules.class);
    private final HttpClients httpClients = mock(HttpClients.class);
    private final Simulators simulators = mock(Simulators.class);
    private final Topics topics = mock(Topics.class);
    private final Listener<VirtualControlLoopNotification> policyClMgt = mock();
    private final Listener<AppcLcmDmaapWrapper> appcLcmRead = mock();
    private final DroolsController drools = mock(DroolsController.class);
    private final ToscaPolicy policy = mock(ToscaPolicy.class);

    /**
     * Saves static values from the class.
     */
    @SuppressWarnings("unchecked")
    @BeforeAll
    public static void setUpBeforeClass() {
        ruleMaker = (Function<String, Rules>) ReflectionTestUtils.getField(DroolsRuleTest.class, "ruleMaker");
        httpClientMaker = (Supplier<HttpClients>) ReflectionTestUtils.getField(DroolsRuleTest.class, "httpClientMaker");
        simMaker = (Supplier<Simulators>) ReflectionTestUtils.getField(DroolsRuleTest.class, "simMaker");
        topicMaker = (Supplier<Topics>) ReflectionTestUtils.getField(DroolsRuleTest.class, "topicMaker");
    }

    /**
     * Restores static values.
     */
    @AfterAll
    public static void tearDownAfterClass() {
        ReflectionTestUtils.setField(DroolsRuleTest.class, "ruleMaker", ruleMaker);
        ReflectionTestUtils.setField(DroolsRuleTest.class, "httpClientMaker", httpClientMaker);
        ReflectionTestUtils.setField(DroolsRuleTest.class, "simMaker", simMaker);
        ReflectionTestUtils.setField(DroolsRuleTest.class, "topicMaker", topicMaker);
    }

    /**
     * Sets up.
     */
    @BeforeEach
    public void setUp() {
        when(rules.getController()).thenReturn(controller);
        when(rules.setupPolicyFromFile(any())).thenReturn(policy);

        when(topics.createListener(DroolsRuleTest.POLICY_CL_MGT_TOPIC,
             VirtualControlLoopNotification.class, controller)).thenReturn(policyClMgt);
        when(topics.createListener(eq(DroolsRuleTest.APPC_LCM_READ_TOPIC), eq(AppcLcmDmaapWrapper.class),
                        any(StandardCoder.class))).thenReturn(appcLcmRead);

        Function<String, Rules> ruleMaker = this::makeRules;
        Supplier<HttpClients> httpClientMaker = this::makeHttpClients;
        Supplier<Simulators> simMaker = this::makeSim;
        Supplier<Topics> topicMaker = this::makeTopics;

        ReflectionTestUtils.setField(DroolsRuleTest.class, "ruleMaker", ruleMaker);
        ReflectionTestUtils.setField(DroolsRuleTest.class, "httpClientMaker", httpClientMaker);
        ReflectionTestUtils.setField(DroolsRuleTest.class, "simMaker", simMaker);
        ReflectionTestUtils.setField(DroolsRuleTest.class, "topicMaker", topicMaker);

        clMgtQueue = new LinkedList<>();
        appcLcmQueue = new LinkedList<>();

        when(policyClMgt.await(any())).thenAnswer(args -> {
            VirtualControlLoopNotification notif = clMgtQueue.remove();
            Predicate<VirtualControlLoopNotification> pred = args.getArgument(0);
            assertTrue(pred.test(notif));
            return notif;
        });

        when(appcLcmRead.await(any())).thenAnswer(args -> {
            AppcLcmDmaapWrapper req = appcLcmQueue.remove();
            Predicate<AppcLcmDmaapWrapper> pred = args.getArgument(0);
            assertTrue(pred.test(req));
            return req;
        });

        permitCount = 0;
        finalCount = 0;

        base = new MyDroolsTest();
        DroolsRuleTest.initStatics(CONTROLLER_NAME);
        base.init();
    }

    @Test
    void testInitStatics() {
        assertSame(rules, DroolsRuleTest.rules);
        assertSame(httpClients, DroolsRuleTest.httpClients);
        assertSame(simulators, DroolsRuleTest.simulators);
    }

    @Test
    void testFinishStatics() {
        DroolsRuleTest.finishStatics();

        verify(rules).destroy();
        verify(httpClients).destroy();
        verify(simulators).destroy();
    }

    @Test
    void testInit() {
        assertSame(topics, BaseTest.getTopics());
        assertSame(controller, base.controller);
    }

    @Test
    void testDroolsTestService123Compliant() {
        enqueueAppcLcm("restart", "restart", "restart", "restart", "rebuild", "migrate");
        enqueueClMgt(ControlLoopNotificationType.OPERATION_SUCCESS);
        enqueueClMgt(ControlLoopNotificationType.FINAL_SUCCESS);
        System.out.println("Drools TestTest Here");
        base.testService123Compliant();

        assertEquals(1, permitCount);
        assertEquals(1, finalCount);

        assertTrue(appcLcmQueue.isEmpty());
        assertTrue(clMgtQueue.isEmpty());

        // initial event
        verify(topics).inject(eq(DroolsRuleTest.DCAE_TOPIC), any());

        // replies to each APPC request
        verify(topics, times(6)).inject(eq(DroolsRuleTest.APPC_LCM_WRITE_TOPIC), any(), any());
    }

    private void enqueueClMgt(ControlLoopNotificationType type) {
        VirtualControlLoopNotification notif = new VirtualControlLoopNotification();
        notif.setNotification(type);
        notif.setPolicyName(POLICY_NAME + ".EVENT.MANAGER.FINAL");

        clMgtQueue.add(notif);
    }

    private void enqueueAppcLcm(String... operationNames) {
        for (String oper : operationNames) {
            AppcLcmDmaapWrapper req = new AppcLcmDmaapWrapper();
            req.setRpcName(oper);

            AppcLcmBody body = new AppcLcmBody();
            req.setBody(body);

            AppcLcmInput input = new AppcLcmInput();
            body.setInput(input);

            AppcLcmCommonHeader header = new AppcLcmCommonHeader();
            input.setCommonHeader(header);

            header.setSubRequestId("my-subrequest-id");

            appcLcmQueue.add(req);
        }
    }

    private Rules makeRules(String controllerName) {
        return rules;
    }

    private HttpClients makeHttpClients() {
        return httpClients;
    }

    private Simulators makeSim() {
        return simulators;
    }

    private Topics makeTopics() {
        return topics;
    }
    /*
     * We don't want junit trying to run this, so it's marked "Ignore".
     */

    @Disabled
    private class MyDroolsTest extends DroolsRuleTest {

        @Override
        protected void waitForLockAndPermit(ToscaPolicy policy, Listener<VirtualControlLoopNotification> policyClMgt) {
            permitCount++;
        }

        @Override
        protected VirtualControlLoopNotification waitForFinal(ToscaPolicy policy,
                        Listener<VirtualControlLoopNotification> policyClMgt, ControlLoopNotificationType finalType) {
            finalCount++;
            return policyClMgt.await(notif -> notif.getNotification() == finalType);
        }
    }
}
