/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2018-2019 AT&T Intellectual Property. All rights reserved.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
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
    }

    @Test
    public void cacheDefaults() {
        assertEquals(3, ControlLoopMetricsManager.getManager().getCacheSize());
        assertEquals(2, ControlLoopMetricsManager.getManager().getTransactionTimeout());
        assertEquals(0, ControlLoopMetricsManager.getManager().getCacheOccupancy());
    }

    @Test
    public void invalidNotifications() {
        ControlLoopMetricsFeature feature = new ControlLoopMetricsFeature();
        VirtualControlLoopNotification notification = new VirtualControlLoopNotification();
        feature.beforeDeliver(testController, CommInfrastructure.DMAAP, POLICY_CL_MGT, notification);
        this.cacheDefaults();

        UUID requestId = UUID.randomUUID();
        notification.setRequestId(requestId);

        feature.beforeDeliver(testController, CommInfrastructure.DMAAP, POLICY_CL_MGT, notification);
        assertNull(ControlLoopMetricsManager.getManager().getTransaction(requestId));
        this.cacheDefaults();
    }

    @Test
    public void validActiveNotification() throws InterruptedException {
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
        assertTrue(ControlLoopMetricsManager.getManager().getCacheOccupancy() == 1);

        /* wait for the entries to expire */
        await().atMost(ControlLoopMetricsManager.getManager().getTransactionTimeout() + 1, TimeUnit.SECONDS)
                        .until(() -> ControlLoopMetricsManager.getManager().getTransaction(requestId) == null);

        this.cacheDefaults();
    }

    @Test
    public void reset() {
        VirtualControlLoopNotification notification = this.generateNotification();
        new ControlLoopMetricsFeature().beforeDeliver(testController, CommInfrastructure.DMAAP, POLICY_CL_MGT,
                notification);

        assertNotNull(ControlLoopMetricsManager.getManager().getTransaction(notification.getRequestId()));

        ControlLoopMetricsManager.getManager().resetCache(ControlLoopMetricsManager.getManager().getCacheSize(),
                ControlLoopMetricsManager.getManager().getTransactionTimeout());
        assertNull(ControlLoopMetricsManager.getManager().getTransaction(notification.getRequestId()));
        this.cacheDefaults();
    }

    @Test
    public void removeTransaction() {
        VirtualControlLoopNotification notification = this.generateNotification();
        assertNull(ControlLoopMetricsManager.getManager().getTransaction(notification.getRequestId()));
        ControlLoopMetricsManager.getManager().removeTransaction(notification.getRequestId());

        ControlLoopMetricsManager.getManager().transactionEvent(testController, notification);
        assertNotNull(ControlLoopMetricsManager.getManager().getTransaction(notification.getRequestId()));
        ControlLoopMetricsManager.getManager().removeTransaction(notification.getRequestId());
        assertNull(ControlLoopMetricsManager.getManager().getTransaction(notification.getRequestId()));
    }

    @Test
    public void eviction() throws InterruptedException {
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
        assertTrue(ControlLoopMetricsManager.getManager().getTransactionIds().size() == ControlLoopMetricsManager
                        .getManager().getCacheSize());
        assertTrue(ControlLoopMetricsManager.getManager().getCacheOccupancy() == ControlLoopMetricsManager.getManager()
                        .getCacheSize());
        assertFalse(ControlLoopMetricsManager.getManager().getTransactionIds().isEmpty());
        assertFalse(ControlLoopMetricsManager.getManager().getTransactions().isEmpty());

        /* wait for the entries to expire */
        await().atMost(ControlLoopMetricsManager.getManager().getTransactionTimeout() + 1, TimeUnit.SECONDS)
                        .until(() -> ControlLoopMetricsManager.getManager().getTransactions().isEmpty());

        ControlLoopMetricsManager.getManager().refresh();
        assertTrue(ControlLoopMetricsManager.getManager().getTransactionIds().size() == ControlLoopMetricsManager
                        .getManager().getCacheOccupancy());
        assertFalse(ControlLoopMetricsManager.getManager().getCacheOccupancy() == ControlLoopMetricsManager.getManager()
                        .getCacheSize());
        assertTrue(ControlLoopMetricsManager.getManager().getTransactionIds().isEmpty());
        assertTrue(ControlLoopMetricsManager.getManager().getTransactions().isEmpty());

        this.cacheDefaults();
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
        assertTrue(feature.getSequenceNumber() == ControlLoopMetricsFeature.FEATURE_SEQUENCE_PRIORITY);
    }
}
