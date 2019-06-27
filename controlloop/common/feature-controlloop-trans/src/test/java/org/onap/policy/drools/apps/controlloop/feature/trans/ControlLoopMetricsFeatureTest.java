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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.drools.persistence.SystemPersistence;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.drools.system.PolicyEngine;

/**
 * ControlLoopMetrics Tests.
 */
public class ControlLoopMetricsFeatureTest {

    private static final String POLICY_CL_MGT = "POLICY-CL-MGT";
    private static final Path configPath = SystemPersistence.manager.getConfigurationPath();
    private static PolicyController testController;

    /**
     * Setup method.
     */
    @BeforeClass
    public static void setUp() {
        SystemPersistence.manager.setConfigurationDir("src/test/resources");
        testController = PolicyEngine.manager.createPolicyController("metrics",
                SystemPersistence.manager.getControllerProperties("metrics"));
    }

    @AfterClass
    public static void tearDown() {
        SystemPersistence.manager.setConfigurationDir(configPath.toString());
    }

    @Test
    public void cacheDefaults() {
        assertEquals(3, ControlLoopMetrics.manager.getCacheSize());
        assertEquals(10, ControlLoopMetrics.manager.getTransactionTimeout());
        assertEquals(0, ControlLoopMetrics.manager.getCacheOccupancy());
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
        assertNull(ControlLoopMetrics.manager.getTransaction(requestId));
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
        assertNotNull(ControlLoopMetrics.manager.getTransaction(requestId));
        assertTrue(ControlLoopMetrics.manager.getTransaction(requestId).getFrom().contains(testController.getName()));
        assertNotNull(ControlLoopMetrics.manager.getTransaction(requestId).getNotificationTime());
        assertTrue(ControlLoopMetrics.manager.getCacheOccupancy() == 1);

        /* let the entries expire */
        Thread.sleep((ControlLoopMetrics.manager.getTransactionTimeout() + 5) * 1000L);

        assertNull(ControlLoopMetrics.manager.getTransaction(requestId));
        this.cacheDefaults();
    }

    @Test
    public void reset() {
        VirtualControlLoopNotification notification = this.generateNotification();
        new ControlLoopMetricsFeature().beforeDeliver(testController, CommInfrastructure.DMAAP, POLICY_CL_MGT,
                notification);

        assertNotNull(ControlLoopMetrics.manager.getTransaction(notification.getRequestId()));

        ControlLoopMetrics.manager.resetCache(ControlLoopMetrics.manager.getCacheSize(),
                ControlLoopMetrics.manager.getTransactionTimeout());
        assertNull(ControlLoopMetrics.manager.getTransaction(notification.getRequestId()));
        this.cacheDefaults();
    }

    @Test
    public void removeTransaction() {
        VirtualControlLoopNotification notification = this.generateNotification();
        assertNull(ControlLoopMetrics.manager.getTransaction(notification.getRequestId()));
        ControlLoopMetrics.manager.removeTransaction(notification.getRequestId());

        ControlLoopMetrics.manager.transactionEvent(testController, notification);
        assertNotNull(ControlLoopMetrics.manager.getTransaction(notification.getRequestId()));
        ControlLoopMetrics.manager.removeTransaction(notification.getRequestId());
        assertNull(ControlLoopMetrics.manager.getTransaction(notification.getRequestId()));
    }

    @Test
    public void eviction() throws InterruptedException {
        ControlLoopMetricsFeature feature = new ControlLoopMetricsFeature();
        for (int i = 0; i < ControlLoopMetrics.manager.getCacheSize(); i++) {
            VirtualControlLoopNotification notification = generateNotification();
            feature.beforeDeliver(testController, CommInfrastructure.DMAAP, POLICY_CL_MGT, notification);
            assertNotNull(ControlLoopMetrics.manager.getTransaction(notification.getRequestId()));
        }

        assertEquals(ControlLoopMetrics.manager.getCacheOccupancy(), ControlLoopMetrics.manager.getCacheOccupancy());

        VirtualControlLoopNotification overflowNotification = generateNotification();
        feature.beforeDeliver(testController, CommInfrastructure.DMAAP, POLICY_CL_MGT, overflowNotification);
        assertEquals(ControlLoopMetrics.manager.getCacheOccupancy(), ControlLoopMetrics.manager.getCacheOccupancy());
        assertNotNull(ControlLoopMetrics.manager.getTransaction(overflowNotification.getRequestId()));
        assertTrue(ControlLoopMetrics.manager.getTransactionIds().size() == ControlLoopMetrics.manager.getCacheSize());
        assertTrue(ControlLoopMetrics.manager.getCacheOccupancy() == ControlLoopMetrics.manager.getCacheSize());
        assertFalse(ControlLoopMetrics.manager.getTransactionIds().isEmpty());
        assertFalse(ControlLoopMetrics.manager.getTransactions().isEmpty());

        /* let the entries expire */
        Thread.sleep((ControlLoopMetrics.manager.getTransactionTimeout() + 5) * 1000L);

        ControlLoopMetrics.manager.refresh();
        assertTrue(ControlLoopMetrics.manager.getTransactionIds().size() == ControlLoopMetrics.manager
                .getCacheOccupancy());
        assertFalse(ControlLoopMetrics.manager.getCacheOccupancy() == ControlLoopMetrics.manager.getCacheSize());
        assertTrue(ControlLoopMetrics.manager.getTransactionIds().isEmpty());
        assertTrue(ControlLoopMetrics.manager.getTransactions().isEmpty());

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
