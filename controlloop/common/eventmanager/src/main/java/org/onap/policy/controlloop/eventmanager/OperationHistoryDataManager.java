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

package org.onap.policy.controlloop.eventmanager;

import java.util.Date;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.onap.policy.common.utils.jpa.EntityMgrCloser;
import org.onap.policy.common.utils.jpa.EntityTransCloser;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.database.operationshistory.Dbao;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.guard.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data manager for the Operation History table. Stores records in the DB, asynchronously.
 */
public class OperationHistoryDataManager {
    private static final Logger logger = LoggerFactory.getLogger(OperationHistoryDataManager.class);
    private static final int MAX_QUEUE_LENGTH = 10000;
    private static final int BATCH_SIZE = 100;
    private static final Pair<VirtualControlLoopEvent, ControlLoopOperation> END_MARKER = Pair.of(null, null);

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
    private final BlockingQueue<Pair<VirtualControlLoopEvent, ControlLoopOperation>> operations =
                    new LinkedBlockingQueue<>();


    /**
     * Stores an operation in the DB. If the queue is full, then the oldest records is
     * discarded.
     *
     * @param event event with which the operation is associated
     * @param operation operation to be stored
     */
    public synchronized void store(VirtualControlLoopEvent event, ControlLoopOperation operation) {

        // TODO check flag from guard actor configuration

        // Only store in DB if enabled
        boolean guardEnabled = "false"
                        .equalsIgnoreCase(PolicyEngineConstants.getManager().getEnvironmentProperty("guard.disabled"));
        if (!guardEnabled) {
            return;
        }

        if (stopped) {
            logger.warn("operation history thread is stopped, discarding event={} operation={}", event, operation);
            return;
        }

        operations.add(Pair.of(event, operation));

        if (operations.size() > MAX_QUEUE_LENGTH) {
            Pair<VirtualControlLoopEvent, ControlLoopOperation> discarded = operations.remove();
            logger.warn("too many items to store in the operation history table, discarding event={} operation={}",
                            discarded.getLeft(), discarded.getRight());
        }

        if (thread == null) {
            EntityManagerFactory emfactory = createEntityManagerFactory();
            if (emfactory == null) {
                stopped = true;
                return;
            }

            thread = new Thread(() -> run(emfactory));
            thread.setDaemon(true);
            thread.start();
        }
    }

    /**
     * Stops the background thread and places an "end" item into {@link #operations}.
     */
    public void stop() {
        synchronized (this) {
            stopped = true;
        }

        operations.add(END_MARKER);
    }

    /**
     * Takes records from {@link #operations} and stores them in the queue. Continues to
     * run until {@link #stop()} is invoked, or the thread is interrupted.
     *
     * @param emfactory entity manager factory
     */
    private void run(EntityManagerFactory emfactory) {
        try {
            // store records until stopped, continuing if an exception occurs
            while (!stopped) {
                try {
                    storeBatch(emfactory.createEntityManager());

                } catch (RuntimeException e) {
                    logger.error("failed to save data to operation history table", e);

                } catch (InterruptedException e) {
                    logger.error("interrupted, discarding remaining operation history data", e);
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            storeRemainingRecords(emfactory);

        } finally {
            emfactory.close();
        }
    }

    /**
     * Store any remaining records, but stop at the first exception.
     *
     * @param emfactory entity manager factory
     */
    private void storeRemainingRecords(EntityManagerFactory emfactory) {
        try {
            while (!operations.isEmpty() && operations.peek() != END_MARKER) {
                storeBatch(emfactory.createEntityManager());
            }

        } catch (RuntimeException e) {
            logger.error("failed to save remaining data to operation history table", e);

        } catch (InterruptedException e) {
            logger.error("interrupted, discarding remaining operation history data", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Stores a batch of records.
     *
     * @param entityManager entity manager
     * @throws InterruptedException if an interrupt occurs
     */
    private void storeBatch(EntityManager entityManager) throws InterruptedException {

        try (EntityMgrCloser emc = new EntityMgrCloser(entityManager);
                        EntityTransCloser trans = new EntityTransCloser(entityManager.getTransaction())) {

            int nrecords = 0;
            Pair<VirtualControlLoopEvent, ControlLoopOperation> pair = operations.take();

            while (pair != null && pair != END_MARKER) {
                storeRecord(entityManager, pair.getLeft(), pair.getRight());

                if (++nrecords >= BATCH_SIZE) {
                    break;
                }

                pair = operations.poll();
            }

            trans.commit();
        }
    }

    /**
     * Stores a record.
     *
     * @param entityManager entity manager
     * @param event event with which the operation is associated
     * @param operation operation to be stored
     */
    private void storeRecord(EntityManager entityMgr, VirtualControlLoopEvent event, ControlLoopOperation operation) {
        Dbao newEntry = new Dbao();

        newEntry.setClosedLoopName(event.getClosedLoopControlName());
        newEntry.setRequestId(event.getRequestId().toString());
        newEntry.setActor(operation.getActor());
        newEntry.setOperation(operation.getOperation());
        newEntry.setTarget(operation.getTarget());
        newEntry.setSubrequestId(operation.getSubRequestId());
        newEntry.setMessage(operation.getMessage());
        newEntry.setOutcome(operation.getOutcome());
        if (operation.getStart() != null) {
            newEntry.setStarttime(new Date(operation.getStart().toEpochMilli()));
        }
        if (operation.getEnd() != null) {
            newEntry.setEndtime(new Date(operation.getEnd().toEpochMilli()));
        }

        entityMgr.persist(newEntry);
    }

    /**
     * Creates the entity manager factory.
     *
     * @return the entity manager factory
     */
    private EntityManagerFactory createEntityManagerFactory() {
        // DB Properties
        Properties props = new Properties();
        if (PolicyEngineConstants.getManager().getEnvironmentProperty(Util.ONAP_KEY_URL) != null
                        && PolicyEngineConstants.getManager().getEnvironmentProperty(Util.ONAP_KEY_USER) != null
                        && PolicyEngineConstants.getManager().getEnvironmentProperty(Util.ONAP_KEY_PASS) != null) {
            props.put(Util.ECLIPSE_LINK_KEY_URL,
                            PolicyEngineConstants.getManager().getEnvironmentProperty(Util.ONAP_KEY_URL));
            props.put(Util.ECLIPSE_LINK_KEY_USER,
                            PolicyEngineConstants.getManager().getEnvironmentProperty(Util.ONAP_KEY_USER));
            props.put(Util.ECLIPSE_LINK_KEY_PASS,
                            PolicyEngineConstants.getManager().getEnvironmentProperty(Util.ONAP_KEY_PASS));
            props.put(PersistenceUnitProperties.CLASSLOADER, ControlLoopOperationManager2.class.getClassLoader());
        }

        String opsHistPu = System.getProperty("OperationsHistoryPU");
        if (!"OperationsHistoryPUTest".equals(opsHistPu)) {
            opsHistPu = "OperationsHistoryPU";
        } else {
            props.clear();
        }

        try {
            return Persistence.createEntityManagerFactory(opsHistPu, props);
        } catch (RuntimeException e) {
            logger.error("failed to initialize operation history EntityManagerFactory", e);
            return null;
        }
    }
}
