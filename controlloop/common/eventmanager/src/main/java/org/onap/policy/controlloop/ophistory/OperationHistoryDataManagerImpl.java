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

package org.onap.policy.controlloop.ophistory;

import java.util.Date;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Triple;
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
 * Data manager that stores records in the DB, asynchronously, using a background thread.
 */
public class OperationHistoryDataManagerImpl implements OperationHistoryDataManager {
    private static final Logger logger = LoggerFactory.getLogger(OperationHistoryDataManagerImpl.class);
    public static final String OPERATIONS_HISTORY_PU_PROP = "OperationsHistoryPU";
    private static final String OPERATIONS_HISTORY_PU = "OperationsHistoryPU";
    public static final int MAX_QUEUE_LENGTH = 10000;
    private static final int BATCH_SIZE = 100;
    private static final Triple<String, VirtualControlLoopEvent, ControlLoopOperation> END_MARKER =
                    Triple.of(null, null, null);

    /**
     * Thread that takes records from {@link #operations} and stores them in the DB. The
     * is started in a lazy fashion, during the first call to
     * {@link #store(VirtualControlLoopEvent, ControlLoopOperation)}.
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
    private final BlockingQueue<Triple<String, VirtualControlLoopEvent, ControlLoopOperation>> operations =
                    new LinkedBlockingQueue<>();

    @Getter
    private long recordsAdded = 0;


    @Override
    public synchronized void store(String requestId, VirtualControlLoopEvent event, ControlLoopOperation operation) {

        if (stopped) {
            logger.warn("operation history thread is stopped, discarding requestId={} event={} operation={}", requestId,
                            event, operation);
            return;
        }

        operations.add(Triple.of(requestId, event, operation));

        if (operations.size() > MAX_QUEUE_LENGTH) {
            Triple<String, VirtualControlLoopEvent, ControlLoopOperation> discarded = operations.remove();
            logger.warn("too many items to store in the operation history table, "
                            + "discarding requestId={} event={} operation={}", discarded.getLeft(),
                            discarded.getMiddle(), discarded.getRight());
        }

        if (thread == null) {
            EntityManagerFactory emfactory = createEntityManagerFactory();
            if (emfactory == null) {
                stopped = true;
                return;
            }

            thread = makeThread(() -> run(emfactory));
            thread.setDaemon(true);
            thread.start();
        }
    }

    @Override
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
                    Triple<String, VirtualControlLoopEvent, ControlLoopOperation> triple = operations.take();
                    storeBatch(emfactory.createEntityManager(), triple);

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
                storeBatch(emfactory.createEntityManager(), operations.poll());
            }

        } catch (RuntimeException e) {
            logger.error("failed to save remaining data to operation history table", e);
        }
    }

    /**
     * Stores a batch of records.
     *
     * @param entityManager entity manager
     * @param firstTriple first record to be stored
     */
    private void storeBatch(EntityManager entityManager,
                    Triple<String, VirtualControlLoopEvent, ControlLoopOperation> firstTriple) {

        try (EntityMgrCloser emc = new EntityMgrCloser(entityManager);
                        EntityTransCloser trans = new EntityTransCloser(entityManager.getTransaction())) {

            int nrecords = 0;
            Triple<String, VirtualControlLoopEvent, ControlLoopOperation> triple = firstTriple;

            while (triple != null && triple != END_MARKER) {
                storeRecord(entityManager, triple.getLeft(), triple.getMiddle(), triple.getRight());

                if (++nrecords >= BATCH_SIZE) {
                    break;
                }

                triple = operations.poll();
            }

            trans.commit();
            recordsAdded += nrecords;
        }
    }

    /**
     * Stores a record.
     *
     * @param entityManager entity manager
     * @param requestId request ID
     * @param event event with which the operation is associated
     * @param operation operation to be stored
     */
    private void storeRecord(EntityManager entityMgr, String requestId, VirtualControlLoopEvent event,
                    ControlLoopOperation operation) {

        Dbao newEntry = new Dbao();

        newEntry.setClosedLoopName(event.getClosedLoopControlName());
        newEntry.setRequestId(requestId);
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
    protected EntityManagerFactory createEntityManagerFactory() {
        // DB Properties
        Properties props = new Properties();
        props.put(Util.ECLIPSE_LINK_KEY_URL, getNonNullProp(Util.ONAP_KEY_URL));
        props.put(Util.ECLIPSE_LINK_KEY_USER, getNonNullProp(Util.ONAP_KEY_USER));
        props.put(Util.ECLIPSE_LINK_KEY_PASS, getNonNullProp(Util.ONAP_KEY_PASS));
        props.put(PersistenceUnitProperties.CLASSLOADER, getClass().getClassLoader());

        String opsHistPu = System.getProperty(OPERATIONS_HISTORY_PU_PROP, OPERATIONS_HISTORY_PU);

        try {
            return makeEntityManagerFactory(props, opsHistPu);
        } catch (RuntimeException e) {
            logger.error("failed to initialize operation history EntityManagerFactory", e);
            return null;
        }
    }

    /**
     * Gets a property and verifies that it is non-null.
     *
     * @param name name of the property to get
     * @return the property value
     */
    private String getNonNullProp(String name) {
        return Objects.requireNonNull(PolicyEngineConstants.getManager().getEnvironmentProperty(name));
    }

    protected EntityManagerFactory makeEntityManagerFactory(Properties props, String opsHistPu) {
        return Persistence.createEntityManagerFactory(opsHistPu, props);
    }

    protected Thread makeThread(Runnable command) {
        return new Thread(command);
    }
}
