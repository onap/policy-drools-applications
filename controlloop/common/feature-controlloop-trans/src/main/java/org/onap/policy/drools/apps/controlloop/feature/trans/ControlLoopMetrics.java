/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights reserved.
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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.drools.persistence.SystemPersistence;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.drools.utils.logging.MDCTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Control Loop Metrics Tracker.
 */
public interface ControlLoopMetrics {

    /**
     * Singleton manager object.
     */
    ControlLoopMetrics manager = new CacheBasedControlLoopMetricsManager();

    /**
     * Gets all transaction identifiers being monitored.
     *
     * @return transaction id list
     */
    List<UUID> getTransactionIds();

    /**
     * Gets all detailed transactions.
     *
     * @return list of transactions
     */
    List<VirtualControlLoopNotification> getTransactions();

    /**
     * Track controller's notification events.
     *
     * @param controller policy controller sending out notification
     * @param notification notification
     */
    void transactionEvent(PolicyController controller, VirtualControlLoopNotification notification);

    /**
     * Gets an in-progress transaction.
     *
     * @param requestId request ID
     * @return in progress notification
     */
    VirtualControlLoopNotification getTransaction(UUID requestId);

    /**
     * Removes an in-progress transaction.
     *
     * @param requestId request ID
     */
    void removeTransaction(UUID requestId);

    /**
     * Get cache size.
     *
     * @return cache size
     */
    long getCacheSize();

    /**
     * Get cache size.
     *
     * @return cache size
     */
    long getCacheOccupancy();

    /**
     * Sets cache size.
     *
     * @param cacheSize cache size
     */
    void setMaxCacheSize(long cacheSize);

    /**
     * Cached transaction expiration timeout in seconds.
     *
     * @return transaction timeout in seconds
     */
    long getTransactionTimeout();

    /**
     * Sets transaction timeout in seconds.
     *
     * @param transactionTimeout transaction timeout in seconds
     */
    void setTransactionTimeout(long transactionTimeout);

    /**
     * Reset cache.
     *
     * @param cacheSize new cache size
     * @param transactionTimeout new transaction timeout in seconds
     */
    void resetCache(long cacheSize, long transactionTimeout);

    /**
     * Refresh underlying transaction management.
     */
    void refresh();
}


/**
 * Control Loop Metrics Tracker Implementation.
 */
class CacheBasedControlLoopMetricsManager implements ControlLoopMetrics {

    private static final Logger logger = LoggerFactory.getLogger(CacheBasedControlLoopMetricsManager.class);

    private LoadingCache<UUID, VirtualControlLoopNotification> cache;
    private long cacheSize = ControlLoopMetricsFeature.CL_CACHE_TRANS_SIZE_DEFAULT;

    private long transactionTimeout = ControlLoopMetricsFeature.CL_CACHE_TRANS_TIMEOUT_SECONDS_DEFAULT;

    public CacheBasedControlLoopMetricsManager() {

        Properties properties =
                SystemPersistence.manager.getProperties(ControlLoopMetricsFeature.CONFIGURATION_PROPERTIES_NAME);

        /* cache size */

        try {
            this.cacheSize =
                    Long.parseLong(properties.getProperty(ControlLoopMetricsFeature.CL_CACHE_TRANS_SIZE_PROPERTY,
                            "" + ControlLoopMetricsFeature.CL_CACHE_TRANS_SIZE_DEFAULT));
        } catch (Exception e) {
            logger.warn("{}:{} property cannot be accessed", ControlLoopMetricsFeature.CONFIGURATION_PROPERTIES_NAME,
                    ControlLoopMetricsFeature.CL_CACHE_TRANS_SIZE_PROPERTY, e);
        }

        /* transaction timeout */

        try {
            this.transactionTimeout = Long
                    .parseLong(properties.getProperty(ControlLoopMetricsFeature.CL_CACHE_TRANS_TIMEOUT_SECONDS_PROPERTY,
                            "" + ControlLoopMetricsFeature.CL_CACHE_TRANS_TIMEOUT_SECONDS_DEFAULT));
        } catch (Exception e) {
            logger.warn("{}:{} property cannot be accessed", ControlLoopMetricsFeature.CONFIGURATION_PROPERTIES_NAME,
                    ControlLoopMetricsFeature.CL_CACHE_TRANS_TIMEOUT_SECONDS_PROPERTY, e);
        }

        resetCache(this.cacheSize, this.transactionTimeout);
    }

    @Override
    public void resetCache(long cacheSize, long transactionTimeout) {
        this.cacheSize = cacheSize;
        this.transactionTimeout = transactionTimeout;

        CacheLoader<UUID, VirtualControlLoopNotification> loader =
                new CacheLoader<UUID, VirtualControlLoopNotification>() {

            @Override
            public VirtualControlLoopNotification load(UUID key) throws Exception {
                return null;
            }
        };

        RemovalListener<UUID, VirtualControlLoopNotification> listener =
                new RemovalListener<UUID, VirtualControlLoopNotification>() {
            @Override
            public void onRemoval(RemovalNotification<UUID, VirtualControlLoopNotification> notification) {
                if (notification.wasEvicted()) {
                    evicted(notification.getValue());
                } else {
                    logger.info("REMOVAL: {} because of {}", notification.getValue().getRequestId(),
                                    notification.getCause().name());
                }
            }
        };

        synchronized (this) {
            if (this.cache != null) {
                this.cache.cleanUp();
                this.cache.invalidateAll();
            }

            this.cache = CacheBuilder.newBuilder().maximumSize(this.cacheSize)
                    .expireAfterWrite(transactionTimeout, TimeUnit.SECONDS).removalListener(listener).build(loader);
        }
    }

    @Override
    public void refresh() {
        this.cache.cleanUp();
    }

    @Override
    public List<UUID> getTransactionIds() {
        return new ArrayList<>(this.cache.asMap().keySet());
    }

    @Override
    public List<VirtualControlLoopNotification> getTransactions() {
        return new ArrayList<>(this.cache.asMap().values());
    }

    @Override
    public void transactionEvent(PolicyController controller, VirtualControlLoopNotification notification) {
        if (notification == null || notification.getRequestId() == null || notification.getNotification() == null) {
            logger.warn("Invalid notification: {}", notification);
            return;
        }

        if (notification.getNotificationTime() == null) {
            notification.setNotificationTime(ZonedDateTime.now());
        }

        notification.setFrom(notification.getFrom() + ":" + controller.getName());

        switch (notification.getNotification()) {
            case REJECTED:
            case FINAL_FAILURE:
            case FINAL_SUCCESS:
            case FINAL_OPENLOOP:
                endTransaction(notification);
                break;
            case ACTIVE:
            case OPERATION:
            case OPERATION_SUCCESS:
            case OPERATION_FAILURE:
                /* any other value is an in progress transaction */
                inProgressTransaction(notification);
                break;
            default:
                /* unexpected */
                logger.warn("unexpected notification type {} in notification {}",
                        notification.getNotification().toString(), notification);
                break;
        }
    }

    @Override
    public VirtualControlLoopNotification getTransaction(UUID requestId) {
        return cache.getIfPresent(requestId);
    }

    @Override
    public void removeTransaction(UUID requestId) {
        cache.invalidate(requestId);
    }

    /**
     * Tracks an in progress control loop transaction.
     *
     * @param notification control loop notification
     */
    protected void inProgressTransaction(VirtualControlLoopNotification notification) {
        if (cache.getIfPresent(notification.getRequestId()) == null) {
            cache.put(notification.getRequestId(), notification);
        }

        this.metric(notification);
    }

    /**
     * End of a control loop transaction.
     *
     * @param notification control loop notification
     */
    protected void endTransaction(VirtualControlLoopNotification notification) {
        ZonedDateTime startTime;
        VirtualControlLoopNotification startNotification = cache.getIfPresent(notification.getRequestId());
        if (startNotification != null) {
            startTime = startNotification.getNotificationTime();
        } else {
            startTime = notification.getNotificationTime();
        }

        this.transaction(notification, startTime);
        if (startNotification != null) {
            cache.invalidate(startNotification);
        }
    }

    protected void evicted(VirtualControlLoopNotification notification) {
        MDCTransaction
                .newTransaction(notification.getRequestId().toString(), notification.getFrom())
                .setServiceName(notification.getClosedLoopControlName()).setTargetEntity(notification.getTarget())
                .setStartTime(notification.getNotificationTime().toInstant()).setEndTime(Instant.now())
                .setResponseDescription("EVICTED").setStatusCode(false).metric().resetTransaction();
    }

    @Override
    public long getCacheSize() {
        return this.cacheSize;
    }

    @Override
    public void setMaxCacheSize(long cacheSize) {
        this.cacheSize = cacheSize;
    }

    @Override
    public long getTransactionTimeout() {
        return this.transactionTimeout;
    }

    @Override
    public void setTransactionTimeout(long transactionTimeout) {
        this.transactionTimeout = transactionTimeout;
    }

    @Override
    public long getCacheOccupancy() {
        return this.cache.size();
    }

    protected void metric(VirtualControlLoopNotification notification) {
        MDCTransaction trans = MDCTransaction
                .newTransaction(notification.getRequestId().toString(), notification.getFrom())
                .setServiceName(notification.getClosedLoopControlName()).setTargetEntity(notification.getTarget());

        List<ControlLoopOperation> operations = notification.getHistory();
        switch (notification.getNotification()) {
            case ACTIVE:
                trans.setStatusCode(true);
                trans.metric().resetTransaction();
                break;
            case OPERATION:
                trans.setStatusCode(true);
                if (!operations.isEmpty()) {
                    ControlLoopOperation operation = operations.get(operations.size() - 1);
                    trans.setTargetEntity(operation.getTarget());
                    trans.setTargetServiceName(operation.getActor());
                }
                trans.metric().resetTransaction();
                break;
            case OPERATION_SUCCESS:
                trans.setStatusCode(true);
                operation(trans, operations);
                trans.transaction().resetTransaction();
                break;
            case OPERATION_FAILURE:
                trans.setStatusCode(false);
                operation(trans, operations);
                trans.transaction().resetTransaction();
                break;
            default:
                /* unexpected */
                logger.warn("unexpected notification type {} in notification {}",
                        notification.getNotification().toString(), notification);
                break;
        }
    }

    protected void operation(MDCTransaction trans, List<ControlLoopOperation> operations) {
        if (!operations.isEmpty()) {
            ControlLoopOperation operation = operations.get(operations.size() - 1);

            if (operation.getTarget() != null) {
                trans.setTargetEntity(operation.getTarget());
            }

            if (operation.getActor() != null) {
                trans.setTargetServiceName(operation.getActor());
            }

            if (operation.getMessage() != null) {
                trans.setResponseDescription(operation.getMessage());
            }

            trans.setInvocationId(operation.getSubRequestId());

            if (operation.getOutcome() != null) {
                trans.setResponseCode(operation.getOutcome());
            }

            if (operation.getStart() != null) {
                trans.setStartTime(operation.getStart());
            }

            if (operation.getEnd() != null) {
                trans.setEndTime(operation.getEnd());
            }
        }
    }

    protected void transaction(VirtualControlLoopNotification notification, ZonedDateTime startTime) {
        MDCTransaction trans = MDCTransaction
                .newTransaction(notification.getRequestId().toString(), notification.getFrom())
                .setServiceName(notification.getClosedLoopControlName()).setTargetEntity(notification.getTarget())
                .setStartTime(startTime.toInstant()).setEndTime(notification.getNotificationTime().toInstant())
                .setResponseDescription(notification.getMessage());

        switch (notification.getNotification()) {
            case FINAL_OPENLOOP:
                trans.setStatusCode(true);
                break;
            case FINAL_SUCCESS:
                trans.setStatusCode(true);
                break;
            case FINAL_FAILURE:
                trans.setStatusCode(false);
                break;
            case REJECTED:
                trans.setStatusCode(false);
                break;
            default:
                /* unexpected */
                logger.warn("unexpected notification type {} in notification {}",
                        notification.getNotification(), notification);
                break;
        }

        trans.transaction().resetTransaction();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("CacheBasedControlLoopMetricsManager{");
        sb.append("cacheSize=").append(cacheSize);
        sb.append(", transactionTimeout=").append(transactionTimeout);
        sb.append('}');
        return sb.toString();
    }
}
