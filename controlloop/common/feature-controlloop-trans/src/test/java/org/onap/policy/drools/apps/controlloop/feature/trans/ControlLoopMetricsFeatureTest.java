/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2018-2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.drools.apps.controlloop.feature.trans;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.util.Serialization;
import org.onap.policy.drools.persistence.SystemPersistenceConstants;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.drools.system.PolicyEngineConstants;

/**
 * ControlLoopMetrics Tests.
 */
public class ControlLoopMetricsFeatureTest {

    private static final String POLICY_CL_MGT = "POLICY-CL-MGT";
    private static final Path configPath = SystemPersistenceConstants.getManager().getConfigurationPath();
    private static PolicyController testController;

    private static void resetStats() {
        PolicyEngineConstants.getManager().getStats().getGroupStat().setAverageExecutionTime(0d);
        PolicyEngineConstants.getManager().getStats().getGroupStat().setLastExecutionTime(0L);
        PolicyEngineConstants.getManager().getStats().getGroupStat().setLastStart(0L);
        PolicyEngineConstants.getManager().getStats().getGroupStat().setAverageExecutionTime(0d);
        PolicyEngineConstants.getManager().getStats().getGroupStat().setPolicyExecutedCount(0L);
        PolicyEngineConstants.getManager().getStats().getGroupStat().setPolicyExecutedFailCount(0L);
        PolicyEngineConstants.getManager().getStats().getGroupStat().setPolicyExecutedSuccessCount(0L);
        PolicyEngineConstants.getManager().getStats().getGroupStat().setTotalElapsedTime(0d);
    }

    /**
     * Setup method.
     */
    @BeforeClass
    public static void setUp() {
        SystemPersistenceConstants.getManager().setConfigurationDir("src/test/resources");
        testController = PolicyEngineConstants.getManager().createPolicyController("metrics",
                SystemPersistenceConstants.getManager().getControllerProperties("metrics"));
    }

    @AfterClass
    public static void tearDown() {
        SystemPersistenceConstants.getManager().setConfigurationDir(configPath.toString());
        resetStats();
    }

    @Before
    public void beforeTest() {
        resetStats();
    }

    @Test
    public void testCacheDefaults() {
        assertEquals(3, ControlLoopMetricsManager.getManager().getCacheSize());
        assertEquals(2, ControlLoopMetricsManager.getManager().getTransactionTimeout());
        assertEquals(0, ControlLoopMetricsManager.getManager().getCacheOccupancy());
    }

    @Test
    public void testInvalidNotifications() {
        ControlLoopMetricsFeature feature = new ControlLoopMetricsFeature();
        VirtualControlLoopNotification notification = new VirtualControlLoopNotification();
        feature.beforeDeliver(testController, CommInfrastructure.DMAAP, POLICY_CL_MGT, notification);
        this.testCacheDefaults();

        UUID requestId = UUID.randomUUID();
        notification.setRequestId(requestId);

        feature.beforeDeliver(testController, CommInfrastructure.DMAAP, POLICY_CL_MGT, notification);
        assertNull(ControlLoopMetricsManager.getManager().getTransaction(requestId));
        this.testCacheDefaults();
    }

    @Test
    public void testValidActiveNotification() {
        ControlLoopMetricsFeature feature = new ControlLoopMetricsFeature();
        VirtualControlLoopNotification notification = new VirtualControlLoopNotification();
        UUID requestId = UUID.randomUUID();
        notification.setRequestId(requestId);
        notification.setNotification(ControlLoopNotificationType.ACTIVE);

        feature.beforeDeliver(testController, CommInfrastructure.DMAAP, POLICY_CL_MGT, notification);
        assertNotNull(ControlLoopMetricsManager.getManager().getTransaction(requestId));
        assertTrue(ControlLoopMetricsManager.getManager().getTransaction(requestId).getFrom()
                        .contains(testController.getName()));
        assertNotNull(ControlLoopMetricsManager.getManager().getTransaction(requestId).getNotificationTime());
        assertEquals(1, ControlLoopMetricsManager.getManager().getCacheOccupancy());

        /* wait for the entries to expire */
        await().atMost(ControlLoopMetricsManager.getManager().getTransactionTimeout() + 1, TimeUnit.SECONDS)
                        .until(() -> ControlLoopMetricsManager.getManager().getTransaction(requestId) == null);

        this.testCacheDefaults();
    }

    @Test
    public void testReset() {
        VirtualControlLoopNotification notification = this.generateNotification();
        new ControlLoopMetricsFeature().beforeDeliver(testController, CommInfrastructure.DMAAP, POLICY_CL_MGT,
                notification);

        assertNotNull(ControlLoopMetricsManager.getManager().getTransaction(notification.getRequestId()));

        ControlLoopMetricsManager.getManager().resetCache(ControlLoopMetricsManager.getManager().getCacheSize(),
                ControlLoopMetricsManager.getManager().getTransactionTimeout());
        assertNull(ControlLoopMetricsManager.getManager().getTransaction(notification.getRequestId()));
        this.testCacheDefaults();
    }

    @Test
    public void testRemoveTransaction() {
        VirtualControlLoopNotification notification = this.generateNotification();
        assertNull(ControlLoopMetricsManager.getManager().getTransaction(notification.getRequestId()));
        ControlLoopMetricsManager.getManager().removeTransaction(notification.getRequestId());

        ControlLoopMetricsManager.getManager().transactionEvent(testController, notification);
        assertNotNull(ControlLoopMetricsManager.getManager().getTransaction(notification.getRequestId()));
        ControlLoopMetricsManager.getManager().removeTransaction(notification.getRequestId());
        assertNull(ControlLoopMetricsManager.getManager().getTransaction(notification.getRequestId()));
    }

    @Test
    public void testEviction() {
        ControlLoopMetricsFeature feature = new ControlLoopMetricsFeature();
        for (int i = 0; i < ControlLoopMetricsManager.getManager().getCacheSize(); i++) {
            VirtualControlLoopNotification notification = generateNotification();
            feature.beforeDeliver(testController, CommInfrastructure.DMAAP, POLICY_CL_MGT, notification);
            assertNotNull(ControlLoopMetricsManager.getManager().getTransaction(notification.getRequestId()));
        }

        assertEquals(ControlLoopMetricsManager.getManager().getCacheOccupancy(),
                        ControlLoopMetricsManager.getManager().getCacheOccupancy());

        VirtualControlLoopNotification overflowNotification = generateNotification();
        feature.beforeDeliver(testController, CommInfrastructure.DMAAP, POLICY_CL_MGT, overflowNotification);
        assertEquals(ControlLoopMetricsManager.getManager().getCacheOccupancy(),
                        ControlLoopMetricsManager.getManager().getCacheOccupancy());
        assertNotNull(ControlLoopMetricsManager.getManager().getTransaction(overflowNotification.getRequestId()));
        assertEquals(ControlLoopMetricsManager.getManager().getTransactionIds().size(),
                ControlLoopMetricsManager.getManager().getCacheSize());
        assertEquals(ControlLoopMetricsManager.getManager().getCacheOccupancy(),
                ControlLoopMetricsManager.getManager().getCacheSize());
        assertFalse(ControlLoopMetricsManager.getManager().getTransactionIds().isEmpty());
        assertFalse(ControlLoopMetricsManager.getManager().getTransactions().isEmpty());

        /* wait for the entries to expire */
        await().atMost(ControlLoopMetricsManager.getManager().getTransactionTimeout() + 1, TimeUnit.SECONDS)
                        .until(() -> ControlLoopMetricsManager.getManager().getTransactions().isEmpty());

        ControlLoopMetricsManager.getManager().refresh();
        assertEquals(ControlLoopMetricsManager.getManager().getTransactionIds().size(),
                ControlLoopMetricsManager.getManager().getCacheOccupancy());
        assertNotEquals(ControlLoopMetricsManager.getManager().getCacheOccupancy(),
                ControlLoopMetricsManager.getManager().getCacheSize());
        assertTrue(ControlLoopMetricsManager.getManager().getTransactionIds().isEmpty());
        assertTrue(ControlLoopMetricsManager.getManager().getTransactions().isEmpty());

        this.testCacheDefaults();
    }

    private VirtualControlLoopNotification generateNotification() {
        VirtualControlLoopNotification notification = new VirtualControlLoopNotification();
        UUID requestId = UUID.randomUUID();
        notification.setRequestId(requestId);
        notification.setNotification(ControlLoopNotificationType.ACTIVE);
        return notification;
    }

    @Test
    public void getSequenceNumber() {
        ControlLoopMetricsFeature feature = new ControlLoopMetricsFeature();
        assertEquals(ControlLoopMetricsFeature.FEATURE_SEQUENCE_PRIORITY, feature.getSequenceNumber());
    }

    @Test
    public void testSuccessControlLoop() {
        ControlLoopMetricsFeature feature = new ControlLoopMetricsFeature();

        String activeNotification = ResourceUtils.getResourceAsString("policy-cl-mgt-active.json");
        VirtualControlLoopNotification active =
                Serialization.gsonPretty.fromJson(activeNotification, VirtualControlLoopNotification.class);
        feature.beforeDeliver(testController, CommInfrastructure.DMAAP, POLICY_CL_MGT, active);
        assertEquals(1, ControlLoopMetricsManager.getManager().getTransactionIds().size());

        String opStartNotification = ResourceUtils.getResourceAsString("policy-cl-mgt-operation.json");
        VirtualControlLoopNotification opStart =
                Serialization.gsonPretty.fromJson(opStartNotification, VirtualControlLoopNotification.class);
        feature.beforeDeliver(testController, CommInfrastructure.DMAAP, POLICY_CL_MGT, opStart);
        assertEquals(1, ControlLoopMetricsManager.getManager().getTransactionIds().size());

        String permitNotification = ResourceUtils.getResourceAsString("policy-cl-mgt-permit.json");
        VirtualControlLoopNotification permit =
                Serialization.gsonPretty.fromJson(permitNotification, VirtualControlLoopNotification.class);
        feature.beforeDeliver(testController, CommInfrastructure.DMAAP, POLICY_CL_MGT, permit);
        assertEquals(1, ControlLoopMetricsManager.getManager().getTransactionIds().size());

        String restartNotification = ResourceUtils.getResourceAsString("policy-cl-mgt-restart.json");
        VirtualControlLoopNotification restart =
                Serialization.gsonPretty.fromJson(restartNotification, VirtualControlLoopNotification.class);
        feature.beforeDeliver(testController, CommInfrastructure.DMAAP, POLICY_CL_MGT, restart);
        assertEquals(1, ControlLoopMetricsManager.getManager().getTransactionIds().size());

        String restartSuccessNotification =
                ResourceUtils.getResourceAsString("policy-cl-mgt-restart-success.json");
        VirtualControlLoopNotification restartSuccess =
                Serialization.gsonPretty.fromJson(restartSuccessNotification, VirtualControlLoopNotification.class);
        feature.beforeDeliver(testController, CommInfrastructure.DMAAP, POLICY_CL_MGT, restartSuccess);
        assertEquals(1, ControlLoopMetricsManager.getManager().getTransactionIds().size());

        String finalSuccessNotification =
                ResourceUtils.getResourceAsString("policy-cl-mgt-final-success.json");
        VirtualControlLoopNotification finalSuccess =
                Serialization.gsonPretty.fromJson(finalSuccessNotification, VirtualControlLoopNotification.class);
        feature.beforeDeliver(testController, CommInfrastructure.DMAAP, POLICY_CL_MGT, finalSuccess);
        assertEquals(0, ControlLoopMetricsManager.getManager().getTransactionIds().size());
        assertEquals(1,
                PolicyEngineConstants.getManager().getStats().getGroupStat().getPolicyExecutedSuccessCount());
        assertEquals(0,
                PolicyEngineConstants.getManager().getStats().getGroupStat().getPolicyExecutedFailCount());
        assertEquals(1, PolicyEngineConstants.getManager().getStats().getGroupStat().getPolicyExecutedCount());
        assertEquals(1587409937684L,
                PolicyEngineConstants.getManager().getStats().getGroupStat().getLastExecutionTime());
        assertEquals(461d,
                PolicyEngineConstants.getManager().getStats().getGroupStat().getAverageExecutionTime(), 0.0d);
        assertEquals(1587409937223L,
                PolicyEngineConstants.getManager().getStats().getGroupStat().getLastStart());
        assertEquals(461d,
                PolicyEngineConstants.getManager().getStats().getGroupStat().getTotalElapsedTime(), 0.0d);
    }

    @Test
    public void testUntrackedNotifications() throws InterruptedException {
        ControlLoopMetricsFeature feature = new ControlLoopMetricsFeature();

        String finalSuccessNotification =
                ResourceUtils.getResourceAsString("policy-cl-mgt-final-success.json");
        VirtualControlLoopNotification finalSuccess =
                Serialization.gsonPretty.fromJson(finalSuccessNotification, VirtualControlLoopNotification.class);
        finalSuccess.setRequestId(UUID.randomUUID());
        feature.beforeDeliver(testController, CommInfrastructure.DMAAP, POLICY_CL_MGT, finalSuccess);
        assertEquals(0, ControlLoopMetricsManager.getManager().getTransactionIds().size());

        String opStartNotification =
                ResourceUtils.getResourceAsString("policy-cl-mgt-operation.json");
        VirtualControlLoopNotification opStart =
                Serialization.gsonPretty.fromJson(opStartNotification, VirtualControlLoopNotification.class);
        feature.beforeDeliver(testController, CommInfrastructure.DMAAP, POLICY_CL_MGT, opStart);
        assertEquals(1, ControlLoopMetricsManager.getManager().getTransactionIds().size());

        Thread.sleep((ControlLoopMetricsManager.getManager().getTransactionTimeout() + 1) * 1000L);  // NOSONAR
        assertEquals(0, ControlLoopMetricsManager.getManager().getTransactionIds().size());
    }

}
