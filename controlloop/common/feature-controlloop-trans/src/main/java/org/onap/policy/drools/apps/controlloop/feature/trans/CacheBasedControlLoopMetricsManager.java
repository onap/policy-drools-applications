/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections4.CollectionUtils;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.drools.persistence.SystemPersistenceConstants;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.drools.utils.logging.MdcTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Control Loop Metrics Tracker Implementation.
 */
class CacheBasedControlLoopMetricsManager implements ControlLoopMetrics {

    private static final String UNEXPECTED_NOTIFICATION_TYPE = "unexpected notification type {} in notification {}";

    private static final Logger logger = LoggerFactory.getLogger(CacheBasedControlLoopMetricsManager.class);

    private LoadingCache<UUID, VirtualControlLoopNotification> cache;
    private long cacheSize = ControlLoopMetricsFeature.CL_CACHE_TRANS_SIZE_DEFAULT;

    private long transactionTimeout = ControlLoopMetricsFeature.CL_CACHE_TRANS_TIMEOUT_SECONDS_DEFAULT;

    /**
     * Numeric response code.
     */
    private static final Map<String, String> note2code = Map.of(
        ControlLoopNotificationType.ACTIVE.name(), "100",
        ControlLoopNotificationType.REJECTED.name(), "200",
        ControlLoopNotificationType.OPERATION.name(), "300",
        ControlLoopNotificationType.OPERATION_SUCCESS.name(), "301",
        ControlLoopNotificationType.OPERATION_FAILURE.name(), "302",
        ControlLoopNotificationType.FINAL_FAILURE.name(), "400",
        ControlLoopNotificationType.FINAL_SUCCESS.name(), "401",
        ControlLoopNotificationType.FINAL_OPENLOOP.name(), "402"
    );

    private static final String UNKNOWN_RESPONSE_CODE = "900";

    public CacheBasedControlLoopMetricsManager() {

        Properties properties = SystemPersistenceConstants.getManager()
                        .getProperties(ControlLoopMetricsFeature.CONFIGURATION_PROPERTIES_NAME);

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

        CacheLoader<UUID, VirtualControlLoopNotification> loader = new CacheLoader<>() {

            @Override
            public VirtualControlLoopNotification load(UUID key) {
                return null;
            }
        };

        RemovalListener<UUID, VirtualControlLoopNotification> listener = notification -> {
            if (notification.wasEvicted()) {
                evicted(notification.getValue());
            } else if (logger.isInfoEnabled()) {
                logger.info("REMOVAL: {} because of {}", notification.getValue().getRequestId(),
                                notification.getCause().name());
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
        if (!isNotificationValid(notification)) {
            return;
        }

        setNotificationValues(controller, notification);

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
                logger.warn(UNEXPECTED_NOTIFICATION_TYPE,
                        notification.getNotification(), notification);
                break;
        }
    }

    private boolean isNotificationValid(VirtualControlLoopNotification notification) {
        if (notification == null || notification.getRequestId() == null || notification.getNotification() == null) {
            logger.warn("Invalid notification: {}", notification);
            return false;
        }

        return true;
    }

    private void setNotificationValues(PolicyController controller, VirtualControlLoopNotification notification) {
        if (notification.getNotificationTime() == null) {
            notification.setNotificationTime(ZonedDateTime.now());
        }

        notification.setFrom(notification.getFrom() + ":" + controller.getName()
            + ":" + controller.getDrools().getCanonicalSessionNames());
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
            removeTransaction(startNotification.getRequestId());
        }
    }

    protected void evicted(VirtualControlLoopNotification notification) {
        MdcTransaction
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
        MdcTransaction trans = getMdcTransaction(notification);
        List<ControlLoopOperation> operations = notification.getHistory();
        switch (notification.getNotification()) {
            case ACTIVE:
                trans.setStatusCode(true).metric().resetTransaction();
                break;
            case OPERATION:
                operation(trans.setStatusCode(true), operations).metric().resetTransaction();
                break;
            case OPERATION_SUCCESS:
                operation(trans.setStatusCode(true), operations).metric().transaction().resetTransaction();
                break;
            case OPERATION_FAILURE:
                operation(trans.setStatusCode(false), operations).metric().transaction().resetTransaction();
                break;
            default:
                /* unexpected */
                logger.warn(UNEXPECTED_NOTIFICATION_TYPE,
                        notification.getNotification(), notification);
                break;
        }
    }

    private MdcTransaction getMdcTransaction(VirtualControlLoopNotification notification) {
        return MdcTransaction
                .newTransaction(notification.getRequestId().toString(), notification.getFrom())
                .setServiceName(notification.getClosedLoopControlName())
                .setServiceInstanceId(notification.getPolicyScope()
                    + ":" + notification.getPolicyName() + ":" + notification.getPolicyVersion())
                .setProcessKey("" + notification.getAai())
                .setTargetEntity(notification.getTargetType() + "." + notification.getTarget())
                .setResponseCode((notification.getNotification() != null)
                    ? notificationTypeToResponseCode(notification.getNotification().name())
                    : UNKNOWN_RESPONSE_CODE)
                .setCustomField3((notification.getNotification() != null)
                    ? notification.getNotification().toString() : "")
                .setResponseDescription(notification.getMessage())
                .setClientIpAddress(notification.getClosedLoopEventClient());
    }

    protected MdcTransaction operation(MdcTransaction trans, List<ControlLoopOperation> operations) {
        if (CollectionUtils.isEmpty(operations)) {
            return trans;
        }

        ControlLoopOperation operation = operations.get(operations.size() - 1);

        if (operation.getActor() != null) {
            trans.setTargetServiceName(operation.getActor() + "." + operation.getOperation());
        }

        if (operation.getTarget() != null) {
            trans.setTargetVirtualEntity(operation.getTarget());
        }

        if (operation.getSubRequestId() != null) {
            trans.setInvocationId(operation.getSubRequestId());
        }

        if (operation.getOutcome() != null) {
            trans.setResponseDescription(operation.getOutcome() + ":" + operation.getMessage());
        }

        if (operation.getStart() != null) {
            trans.setStartTime(operation.getStart());
        }

        if (operation.getEnd() != null) {
            trans.setEndTime(operation.getEnd());
        }

        return trans;
    }

    protected void transaction(VirtualControlLoopNotification notification, ZonedDateTime startTime) {
        MdcTransaction trans = getMdcTransaction(notification)
                .setStartTime(startTime.toInstant())
                .setEndTime(notification.getNotificationTime().toInstant());

        switch (notification.getNotification()) {
            case FINAL_OPENLOOP:
                /* fall through */
            case FINAL_SUCCESS:
                trans.setStatusCode(true);
                break;
            case FINAL_FAILURE:
                /* fall through */
            case REJECTED:
                trans.setStatusCode(false);
                break;
            default:
                /* unexpected */
                logger.warn(UNEXPECTED_NOTIFICATION_TYPE,
                        notification.getNotification(), notification);
                break;
        }

        trans.transaction().resetTransaction();
    }

    @Override
    public String toString() {
        return "CacheBasedControlLoopMetricsManager{" + "cacheSize=" + cacheSize
                       + ",transactionTimeout="
                       + transactionTimeout
                       + ",cacheOccupancy="
                       + getCacheOccupancy()
                       + "}";
    }

    private String notificationTypeToResponseCode(String notificationType) {
        String code = note2code.get(notificationType);
        if (code != null) {
            return code;
        } else {
            return UNKNOWN_RESPONSE_CODE;
        }
    }
}
