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

import java.util.List;
import java.util.UUID;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.drools.system.PolicyController;

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
