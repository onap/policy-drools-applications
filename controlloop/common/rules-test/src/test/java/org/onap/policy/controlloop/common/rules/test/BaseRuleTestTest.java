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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.appc.CommonHeader;
import org.onap.policy.appc.Request;
import org.onap.policy.appclcm.AppcLcmBody;
import org.onap.policy.appclcm.AppcLcmCommonHeader;
import org.onap.policy.appclcm.AppcLcmDmaapWrapper;
import org.onap.policy.appclcm.AppcLcmInput;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.StandardCoderInstantAsMillis;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.drools.controller.DroolsController;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.sdnr.PciBody;
import org.onap.policy.sdnr.PciCommonHeader;
import org.onap.policy.sdnr.PciMessage;
import org.onap.policy.sdnr.PciRequest;
import org.powermock.reflect.Whitebox;

public class BaseRuleTestTest {
    private static final String CONTROLLER_NAME = "my-controller-name";
    private static final String POLICY_NAME = "my-policy-name";

    // saved values
    private static Function<String, Rules> ruleMaker;
    private static Supplier<HttpClients> httpClientMaker;
    private static Supplier<Simulators> simMaker;
    private static Supplier<Topics> topicMaker;

    private BaseRuleTest base;
    private LinkedList<VirtualControlLoopNotification> clMgtQueue;
    private Queue<AppcLcmDmaapWrapper> appcLcmQueue;
    private Queue<Request> appcLegacyQueue;
    private Queue<PciMessage> sdnrQueue;
    private int permitCount;
    private int finalCount;

    @Mock
    private PolicyController controller;
    @Mock
    private Rules rules;
    @Mock
    private HttpClients httpClients;
    @Mock
    private Simulators simulators;
    @Mock
    private Topics topics;
    @Mock
    private Listener<VirtualControlLoopNotification> policyClMgt;
    @Mock
    private Listener<Request> appcClSink;
    @Mock
    private Listener<AppcLcmDmaapWrapper> appcLcmRead;
    @Mock
    private Listener<PciMessage> sdnrClSink;
    @Mock
    private DroolsController drools;
    @Mock
    private ToscaPolicy policy;
    @Mock
    private ToscaPolicyIdentifier policyIdent;


    /**
     * Saves static values from the class.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        ruleMaker = Whitebox.getInternalState(BaseRuleTest.class, "ruleMaker");
        httpClientMaker = Whitebox.getInternalState(BaseRuleTest.class, "httpClientMaker");
        simMaker = Whitebox.getInternalState(BaseRuleTest.class, "simMaker");
        topicMaker = Whitebox.getInternalState(BaseRuleTest.class, "topicMaker");
    }

    /**
     * Restores static values.
     */
    @AfterClass
    public static void tearDownAfterClass() {
        Whitebox.setInternalState(BaseRuleTest.class, "ruleMaker", ruleMaker);
        Whitebox.setInternalState(BaseRuleTest.class, "httpClientMaker", httpClientMaker);
        Whitebox.setInternalState(BaseRuleTest.class, "simMaker", simMaker);
        Whitebox.setInternalState(BaseRuleTest.class, "topicMaker", topicMaker);
    }

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(policy.getIdentifier()).thenReturn(policyIdent);
        when(policyIdent.getName()).thenReturn(POLICY_NAME);

        when(drools.factCount(CONTROLLER_NAME)).thenReturn(0L);
        when(controller.getDrools()).thenReturn(drools);

        when(rules.getControllerName()).thenReturn(CONTROLLER_NAME);
        when(rules.getController()).thenReturn(controller);
        when(rules.setupPolicyFromFile(any())).thenAnswer(args -> {
            when(drools.factCount(CONTROLLER_NAME)).thenReturn(2L);
            return policy;
        });

        when(topics.createListener(BaseRuleTest.POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class, controller))
                        .thenReturn(policyClMgt);
        when(topics.createListener(eq(BaseRuleTest.APPC_LCM_READ_TOPIC), eq(AppcLcmDmaapWrapper.class),
                        any(StandardCoder.class))).thenReturn(appcLcmRead);
        when(topics.createListener(eq(BaseRuleTest.APPC_CL_TOPIC), eq(Request.class),
                        any(StandardCoderInstantAsMillis.class))).thenReturn(appcClSink);
        when(topics.createListener(eq(BaseRuleTest.SDNR_CL_TOPIC), eq(PciMessage.class),
            any(StandardCoder.class))).thenReturn(sdnrClSink);

        Function<String, Rules> ruleMaker = this::makeRules;
        Supplier<HttpClients> httpClientMaker = this::makeHttpClients;
        Supplier<Simulators> simMaker = this::makeSim;
        Supplier<Topics> topicMaker = this::makeTopics;

        Whitebox.setInternalState(BaseRuleTest.class, "ruleMaker", ruleMaker);
        Whitebox.setInternalState(BaseRuleTest.class, "httpClientMaker", httpClientMaker);
        Whitebox.setInternalState(BaseRuleTest.class, "simMaker", simMaker);
        Whitebox.setInternalState(BaseRuleTest.class, "topicMaker", topicMaker);

        clMgtQueue = new LinkedList<>();
        appcLcmQueue = new LinkedList<>();
        appcLegacyQueue = new LinkedList<>();
        sdnrQueue = new LinkedList<>();

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

        when(appcClSink.await(any())).thenAnswer(args -> {
            Request req = appcLegacyQueue.remove();
            Predicate<Request> pred = args.getArgument(0);
            assertTrue(pred.test(req));
            return req;
        });

        when(sdnrClSink.await(any())).thenAnswer(args -> {
            PciMessage pcireq = sdnrQueue.remove();
            Predicate<PciMessage> pred = args.getArgument(0);
            assertTrue(pred.test(pcireq));
            return pcireq;
        });

        permitCount = 0;
        finalCount = 0;

        base = new MyTest();

        BaseRuleTest.initStatics(CONTROLLER_NAME);
        base.init();
    }

    @Test
    public void testInitStatics() {
        assertSame(rules, BaseRuleTest.rules);
        assertSame(httpClients, BaseRuleTest.httpClients);
        assertSame(simulators, BaseRuleTest.simulators);
    }

    @Test
    public void testFinishStatics() {
        BaseRuleTest.finishStatics();

        verify(rules).destroy();
        verify(httpClients).destroy();
        verify(simulators).destroy();
    }

    @Test
    public void testInit() {
        assertSame(topics, base.getTopics());
        assertSame(controller, base.controller);
    }

    @Test
    public void testFinish() {
        base.finish();

        verify(topics).destroy();
        verify(rules).resetFacts();
    }

    @Test
    public void testTestService123Compliant() {
        enqueueAppcLcm("restart", "restart", "restart", "restart", "rebuild", "migrate");
        enqueueClMgt(ControlLoopNotificationType.OPERATION_SUCCESS);
        enqueueClMgt(ControlLoopNotificationType.FINAL_SUCCESS);

        base.testService123Compliant();

        assertEquals(1, permitCount);
        assertEquals(1, finalCount);

        assertTrue(appcLcmQueue.isEmpty());
        assertTrue(clMgtQueue.isEmpty());

        // initial event
        verify(topics).inject(eq(BaseRuleTest.DCAE_TOPIC), any());

        // replies to each APPC request
        verify(topics, times(6)).inject(eq(BaseRuleTest.APPC_LCM_WRITE_TOPIC), any(), any());
    }

    @Test
    public void testTestDuplicatesEvents() {
        // the test expects the count to be incremented by 2 between calls
        AtomicLong count = new AtomicLong(5);
        base = spy(base);
        when(base.getCreateCount()).thenAnswer(args -> count.getAndAdd(2));

        enqueueAppcLcm("restart", "restart");
        enqueueClMgt(ControlLoopNotificationType.FINAL_SUCCESS);
        enqueueClMgt(ControlLoopNotificationType.FINAL_SUCCESS);

        clMgtQueue.get(0).setAai(Map.of("generic-vnf.vnf-id", "duplicate-VNF"));
        clMgtQueue.get(1).setAai(Map.of("generic-vnf.vnf-id", "vCPE_Infrastructure_vGMUX_demo_app"));

        base.testDuplicatesEvents();

        assertEquals(0, permitCount);
        assertEquals(2, finalCount);

        assertTrue(appcLcmQueue.isEmpty());
        assertTrue(clMgtQueue.isEmpty());

        // initial events
        verify(topics).inject(eq(BaseRuleTest.DCAE_TOPIC), any());
        verify(topics, times(2)).inject(eq(BaseRuleTest.DCAE_TOPIC), any(), any());

        // two restarts
        verify(topics, times(2)).inject(eq(BaseRuleTest.APPC_LCM_WRITE_TOPIC), any(), any());
    }

    @Test
    public void testTestVcpeSunnyDayLegacy() {
        checkAppcLcmPolicy("restart", base::testVcpeSunnyDayLegacy);
    }

    @Test
    public void testTestVcpeSunnyDayCompliant() {
        checkAppcLcmPolicy("restart", base::testVcpeSunnyDayCompliant);
    }

    @Test
    public void testTestVcpeOnsetFloodPrevention() {
        enqueueAppcLcm("restart");
        enqueueClMgt(ControlLoopNotificationType.OPERATION_SUCCESS);
        enqueueClMgt(ControlLoopNotificationType.FINAL_SUCCESS);

        base.testVcpeOnsetFloodPrevention();

        assertEquals(1, permitCount);
        assertEquals(1, finalCount);

        assertTrue(appcLcmQueue.isEmpty());
        assertTrue(clMgtQueue.isEmpty());

        // initial events
        verify(topics, times(3)).inject(eq(BaseRuleTest.DCAE_TOPIC), any());

        // one restart
        verify(topics).inject(eq(BaseRuleTest.APPC_LCM_WRITE_TOPIC), any(), any());
    }

    @Test
    public void testTestVdnsSunnyDayLegacy() {
        checkHttpPolicy(base::testVdnsSunnyDayLegacy);
    }

    @Test
    public void testTestVdnsSunnyDayCompliant() {
        checkHttpPolicy(base::testVdnsSunnyDayCompliant);
    }
	
    @Test
    public void testTestVdnsRainyDayCompliant() {
        checkHttpPolicyCompliantFailure(base::testVdnsRainyDayCompliant);
    }

    @Test
    public void testTestVfwSunnyDayLegacy() {
        checkAppcLegacyPolicy("ModifyConfig", base::testVfwSunnyDayLegacy);
    }

    @Test
    public void testTestVfwSunnyDayCompliant() {
        checkAppcLegacyPolicy("ModifyConfig", base::testVfwSunnyDayCompliant);
    }

    @Test
    public void testTestVfwRainyDayLegacyFailure() {
        checkAppcLegacyPolicyOperationFailure("ModifyConfig", base::testVfwRainyDayLegacyFailure);
    }

    @Test
    public void testTestVfwRainyDayOverallTimeout() {
        checkAppcLegacyPolicyFinalFailure("ModifyConfig", base::testVfwRainyDayOverallTimeout);
    }

    @Test
    public void testTestVfwRainyDayCompliantTimeout() {
        checkAppcLegacyPolicyFinalFailure("ModifyConfig", base::testVfwRainyDayCompliantTimeout);
    }

    @Test
    public void testTestVpciSunnyDayLegacy() {
        checkSdnrPolicy("ModifyConfig", base::testVpciSunnyDayLegacy);
    }

    @Test
    public void testTestVpciSunnyDayCompliant() {
        checkSdnrPolicy("ModifyConfig", base::testVpciSunnyDayCompliant);
    }

    @Test
    public void testTestVsonhSunnyDayLegacy() {
        checkSdnrPolicy("ModifyConfigANR", base::testVsonhSunnyDayLegacy);
    }

    @Test
    public void testTestVsonhSunnyDayCompliant() {
        checkSdnrPolicy("ModifyConfigANR", base::testVsonhSunnyDayCompliant);
    }

    protected void checkAppcLcmPolicy(String operation, Runnable test) {
        enqueueAppcLcm(operation);
        enqueueClMgt(ControlLoopNotificationType.OPERATION_SUCCESS);
        enqueueClMgt(ControlLoopNotificationType.FINAL_SUCCESS);

        test.run();

        assertEquals(1, permitCount);
        assertEquals(1, finalCount);

        assertTrue(appcLcmQueue.isEmpty());
        assertTrue(clMgtQueue.isEmpty());

        // initial event
        verify(topics).inject(eq(BaseRuleTest.DCAE_TOPIC), any());

        // reply to each APPC request
        verify(topics).inject(eq(BaseRuleTest.APPC_LCM_WRITE_TOPIC), any(), any());
    }

    protected void checkAppcLegacyPolicy(String operation, Runnable test) {
        enqueueAppcLegacy(operation);
        enqueueClMgt(ControlLoopNotificationType.OPERATION_SUCCESS);
        enqueueClMgt(ControlLoopNotificationType.FINAL_SUCCESS);

        test.run();

        assertEquals(1, permitCount);
        assertEquals(1, finalCount);

        assertTrue(appcLcmQueue.isEmpty());
        assertTrue(clMgtQueue.isEmpty());

        // initial event
        verify(topics).inject(eq(BaseRuleTest.DCAE_TOPIC), any());

        // reply to each APPC request
        verify(topics).inject(eq(BaseRuleTest.APPC_CL_TOPIC), any(), any());
    }

    protected void checkAppcLegacyPolicyOperationFailure(String operation, Runnable test) {
        enqueueAppcLegacy(operation);
        enqueueClMgt(ControlLoopNotificationType.OPERATION_FAILURE);
        enqueueClMgt(ControlLoopNotificationType.FINAL_FAILURE);

        test.run();

        assertEquals(1, permitCount);
        assertEquals(1, finalCount);

        assertTrue(appcLcmQueue.isEmpty());
        assertTrue(clMgtQueue.isEmpty());

        // initial event
        verify(topics).inject(eq(BaseRuleTest.DCAE_TOPIC), any());

        // reply to each APPC request
        verify(topics).inject(eq(BaseRuleTest.APPC_CL_TOPIC), any(), any());
    }

    protected void checkAppcLegacyPolicyFinalFailure(String operation, Runnable test) {
        enqueueAppcLegacy(operation);
        enqueueClMgt(ControlLoopNotificationType.FINAL_FAILURE);

        test.run();

        assertEquals(1, permitCount);
        assertEquals(1, finalCount);

        assertTrue(appcLcmQueue.isEmpty());
        assertTrue(clMgtQueue.isEmpty());

        // initial event
        verify(topics).inject(eq(BaseRuleTest.DCAE_TOPIC), any());

        // There were no requests sent
    }

    protected void checkSdnrPolicy(String operation, Runnable test) {
        enqueueSdnr(operation);
        enqueueClMgt(ControlLoopNotificationType.OPERATION_SUCCESS);
        enqueueClMgt(ControlLoopNotificationType.FINAL_SUCCESS);

        test.run();

        assertEquals(1, permitCount);
        assertEquals(1, finalCount);

        assertTrue(sdnrQueue.isEmpty());
        assertTrue(clMgtQueue.isEmpty());

        // initial event
        verify(topics).inject(eq(BaseRuleTest.DCAE_TOPIC), any());

        // reply to each SDNR request
        verify(topics).inject(eq(BaseRuleTest.SDNR_CL_RSP_TOPIC), any(), any());
    }

    protected void checkHttpPolicy(Runnable test) {
        enqueueClMgt(ControlLoopNotificationType.OPERATION_SUCCESS);
        enqueueClMgt(ControlLoopNotificationType.FINAL_SUCCESS);

        test.run();

        assertEquals(1, permitCount);
        assertEquals(1, finalCount);

        assertTrue(clMgtQueue.isEmpty());

        // initial event
        verify(topics).inject(eq(BaseRuleTest.DCAE_TOPIC), any());
    }
	
    protected void checkHttpPolicyCompliantFailure(Runnable test) {
        enqueueClMgt(ControlLoopNotificationType.OPERATION_FAILURE);
        enqueueClMgt(ControlLoopNotificationType.FINAL_FAILURE);

        test.run();

        assertEquals(1, permitCount);
        assertEquals(1, finalCount);

        assertTrue(clMgtQueue.isEmpty());

        // initial event
        verify(topics).inject(eq(BaseRuleTest.DCAE_TOPIC), any());
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

    private void enqueueAppcLegacy(String... operationNames) {
        for (String oper : operationNames) {
            Request req = new Request();
            req.setAction(oper);

            CommonHeader header = new CommonHeader();
            req.setCommonHeader(header);

            header.setSubRequestId("my-subrequest-id");

            appcLegacyQueue.add(req);
        }
    }

    private void enqueueSdnr(String... operationNames) {
        for (String oper : operationNames) {
            PciMessage pcimessage = new PciMessage();
            PciRequest req = new PciRequest();
            PciBody body = new PciBody();
            body.setInput(req);
            pcimessage.setBody(body);
            pcimessage.getBody().getInput().setAction(oper);
            PciCommonHeader header = new PciCommonHeader();
            pcimessage.getBody().getInput().setCommonHeader(header);

            header.setSubRequestId("my-subrequest-id");

            sdnrQueue.add(pcimessage);
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
    @Ignore
    private class MyTest extends BaseRuleTest {

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
