/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.controlloop.ophistory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.onap.policy.common.parameters.ValidationResult;
import org.onap.policy.common.utils.jpa.EntityMgrCloser;
import org.onap.policy.common.utils.jpa.EntityTransCloser;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.guard.OperationsHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data manager that stores records in the DB, asynchronously, using a background thread.
 */
public class OperationHistoryDataManagerImpl implements OperationHistoryDataManager {
    private static final Logger logger = LoggerFactory.getLogger(OperationHistoryDataManagerImpl.class);

    /**
     * Added to the end of {@link #operations} when {@link #stop()} is called. This is
     * used to get the background thread out of a blocking wait for the next record.
     */
    private static final Record END_MARKER = new Record();

    // copied from the parameters
    private final int maxQueueLength;
    private final int batchSize;

    private final EntityManagerFactory emFactory;

    /**
     * Thread that takes records from {@link #operations} and stores them in the DB.
     */
    private Thread thread;

    /**
     * Set to {@code true} to stop the background thread.
     */
    private boolean stopped = false;

    /**
     * Queue of operations waiting to be stored in the DB. When {@link #stop()} is called,
     * an {@link #END_MARKER} is added to the end of the queue.
     */
    private final BlockingQueue<Record> operations = new LinkedBlockingQueue<>();

    /**
     * Number of records that have been processed and committed into the DB by this data
     * manager instance.
     */
    @Getter
    private long recordsCommitted = 0;

    /**
     * Number of records that have been inserted into the DB by this data manager
     * instance, whether they were committed.
     */
    @Getter
    private long recordsInserted = 0;

    /**
     * Number of records that have been updated within the DB by this data manager
     * instance, whether they were committed.
     */
    @Getter
    private long recordsUpdated = 0;

    /**
     * Constructs the object.
     *
     * @param params data manager parameters
     */
    public OperationHistoryDataManagerImpl(OperationHistoryDataManagerParams params) {
        ValidationResult result = params.validate("data-manager-properties");
        if (!result.isValid()) {
            throw new IllegalArgumentException(result.getResult());
        }

        this.maxQueueLength = params.getMaxQueueLength();
        this.batchSize = params.getBatchSize();

        // create the factory using the properties
        var props = toProperties(params);
        this.emFactory = makeEntityManagerFactory(params.getPersistenceUnit(), props);
    }

    @Override
    public synchronized void start() {
        if (stopped || thread != null) {
            // already started
            return;
        }

        logger.info("start operation history thread");

        thread = makeThread(emFactory, this::run);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public synchronized void stop() {
        logger.info("requesting stop of operation history thread");

        stopped = true;

        if (thread == null) {
            // no thread to close the factory - do it here
            emFactory.close();

        } else {
            // the thread will close the factory when it sees the end marker
            operations.add(END_MARKER);
        }
    }

    @Override
    public synchronized void store(String requestId, String clName, Object event, String targetEntity,
        ControlLoopOperation operation) {

        if (stopped) {
            logger.warn("operation history thread is stopped, discarding requestId={} event={} operation={}", requestId,
                event, operation);
            return;
        }

        operations.add(new Record(requestId, clName, event, targetEntity, operation));

        if (operations.size() > maxQueueLength) {
            Record discarded = operations.remove();
            logger.warn("too many items to store in the operation history table, discarding {}", discarded);
        }
    }

    /**
     * Takes records from {@link #operations} and stores them in the queue. Continues to
     * run until {@link #stop()} is invoked, or the thread is interrupted.
     *
     * @param factory entity manager factory
     */
    private void run(EntityManagerFactory factory) {
        try (factory) {
            // store records until stopped, continuing if an exception occurs
            while (!stopped) {
                try {
                    Record triple = operations.take();
                    storeBatch(factory.createEntityManager(), triple);

                } catch (RuntimeException e) {
                    logger.error("failed to save data to operation history table", e);

                } catch (InterruptedException e) {
                    logger.error("interrupted, discarding remaining operation history data", e);
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            storeRemainingRecords(factory);

        } finally {
            synchronized (this) {
                stopped = true;
            }

        }
    }

    /**
     * Store any remaining records, but stop at the first exception.
     *
     * @param factory entity manager factory
     */
    private void storeRemainingRecords(EntityManagerFactory factory) {
        try {
            while (!operations.isEmpty()) {
                try (var em = factory.createEntityManager()) {
                    storeBatch(em, operations.poll());
                }
            }

        } catch (RuntimeException e) {
            logger.error("failed to save remaining data to operation history table", e);
        }
    }

    /**
     * Stores a batch of records.
     *
     * @param entityManager entity manager
     * @param firstRecord first record to be stored
     */
    private void storeBatch(EntityManager entityManager, Record firstRecord) {
        logger.info("store operation history record batch");

        try (var ignored = new EntityMgrCloser(entityManager);
             var trans = new EntityTransCloser(entityManager.getTransaction())) {

            var nrecords = 0;
            var rec = firstRecord;

            while (rec != null && rec != END_MARKER) {
                storeRecord(entityManager, rec);

                if (++nrecords >= batchSize) {
                    break;
                }

                rec = operations.poll();
            }

            trans.commit();
            recordsCommitted += nrecords;
        }
    }

    /**
     * Stores a record.
     *
     * @param entityMgr entity manager
     * @param rec record to be stored
     */
    private void storeRecord(EntityManager entityMgr, Record rec) {

        final String reqId = rec.getRequestId();
        final String clName = rec.getClName();
        final ControlLoopOperation operation = rec.getOperation();

        logger.info("store operation history record for {}", reqId);

        List<OperationsHistory> results = entityMgr
            .createQuery("select e from OperationsHistory e" + " where e.closedLoopName= ?1"
                + " and e.requestId= ?2" + " and e.subrequestId= ?3" + " and e.actor= ?4"
                + " and e.operation= ?5" + " and e.target= ?6", OperationsHistory.class)
            .setParameter(1, clName).setParameter(2, rec.getRequestId())
            .setParameter(3, operation.getSubRequestId()).setParameter(4, operation.getActor())
            .setParameter(5, operation.getOperation()).setParameter(6, rec.getTargetEntity())
            .getResultList();

        if (results.size() > 1) {
            logger.warn("unexpected operation history record count {} for {}", results.size(), reqId);
        }

        OperationsHistory entry = (results.isEmpty() ? new OperationsHistory() : results.get(0));

        entry.setClosedLoopName(clName);
        entry.setRequestId(rec.getRequestId());
        entry.setActor(operation.getActor());
        entry.setOperation(operation.getOperation());
        entry.setTarget(rec.getTargetEntity());
        entry.setSubrequestId(operation.getSubRequestId());
        entry.setMessage(operation.getMessage());
        entry.setOutcome(operation.getOutcome());
        if (operation.getStart() != null) {
            entry.setStarttime(new Date(operation.getStart().toEpochMilli()));
        } else {
            entry.setStarttime(null);
        }
        if (operation.getEnd() != null) {
            entry.setEndtime(new Date(operation.getEnd().toEpochMilli()));
        } else {
            entry.setEndtime(null);
        }

        if (results.isEmpty()) {
            logger.info("insert operation history record for {}", reqId);
            ++recordsInserted;
            entityMgr.persist(entry);
        } else {
            logger.info("update operation history record for {}", reqId);
            ++recordsUpdated;
            entityMgr.merge(entry);
        }
    }

    /**
     * Converts the parameters to Properties.
     *
     * @param params parameters to be converted
     * @return a new property set
     */
    private Properties toProperties(OperationHistoryDataManagerParams params) {
        var props = new Properties();
        props.put("jakarta.persistence.jdbc.driver",   params.getDriver());
        props.put("jakarta.persistence.jdbc.url",      params.getUrl());
        props.put("jakarta.persistence.jdbc.user",     params.getUserName());
        props.put("jakarta.persistence.jdbc.password", params.getPassword());

        return props;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    private static class Record {
        private String requestId;
        private String clName;
        private Object event;
        private String targetEntity;
        private ControlLoopOperation operation;
    }

    // the following may be overridden by junit tests

    protected EntityManagerFactory makeEntityManagerFactory(String opsHistPu, Properties props) {
        logger.info("Starting persistence unit {}", opsHistPu);
        logger.info("Properties {}", props);
        return Persistence.createEntityManagerFactory(opsHistPu, props);
    }

    protected Thread makeThread(EntityManagerFactory emfactory, Consumer<EntityManagerFactory> command) {
        return new Thread(() -> command.accept(emfactory));
    }
}
