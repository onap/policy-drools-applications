/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.persistence.EntityManagerFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.ophistory.OperationHistoryDataManagerParams.OperationHistoryDataManagerParamsBuilder;

@RunWith(MockitoJUnitRunner.class)
public class OperationHistoryDataManagerImplTest {

    private static final IllegalStateException EXPECTED_EXCEPTION = new IllegalStateException("expected exception");
    private static final String MY_LOOP_NAME = "my-loop-name";
    private static final String MY_ACTOR = "my-actor";
    private static final String MY_OPERATION = "my-operation";
    private static final String MY_TARGET = "my-target";
    private static final String MY_ENTITY = "my-entity";
    private static final String REQ_ID = "my-request-id";
    private static final int BATCH_SIZE = 5;
    private static final int MAX_QUEUE_LENGTH = 23;

    private static EntityManagerFactory emf;

    @Mock
    private Thread thread;

    private OperationHistoryDataManagerParams params;
    private Consumer<EntityManagerFactory> threadFunction;
    private VirtualControlLoopEvent event;
    private ControlLoopOperation operation;
    private EntityManagerFactory emfSpy;

    // decremented when the thread function completes
    private CountDownLatch finished;

    private OperationHistoryDataManagerImpl mgr;


    /**
     * Sets up for all tests.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        OperationHistoryDataManagerParams params = makeBuilder().build();

        // capture the entity manager factory for re-use
        new OperationHistoryDataManagerImpl(params) {
            @Override
            protected EntityManagerFactory makeEntityManagerFactory(String opsHistPu, Properties props) {
                emf = super.makeEntityManagerFactory(opsHistPu, props);
                return emf;
            }
        };
    }

    /**
     * Restores the environment after all tests.
     */
    @AfterClass
    public static void tearDownAfterClass() {
        emf.close();
    }

    /**
     * Sets up for an individual test.
     */
    @Before
    public void setUp() {
        event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(MY_LOOP_NAME);
        event.setRequestId(UUID.randomUUID());

        operation = new ControlLoopOperation();
        operation.setActor(MY_ACTOR);
        operation.setOperation(MY_OPERATION);
        operation.setTarget(MY_TARGET);
        operation.setSubRequestId(UUID.randomUUID().toString());

        threadFunction = null;
        finished = new CountDownLatch(1);

        // prevent the "real" emf from being closed
        emfSpy = spy(emf);
        doAnswer(ans -> null).when(emfSpy).close();

        params = makeBuilder().build();

        mgr = new PseudoThread();
        mgr.start();
    }

    @After
    public void tearDown() {
        mgr.stop();
    }

    @Test
    public void testConstructor() {
        // use a thread and manager that haven't been started yet
        thread = mock(Thread.class);
        mgr = new PseudoThread();

        // should not start the thread before start() is called
        verify(thread, never()).start();

        mgr.start();

        // should have started the thread
        verify(thread).start();

        // invalid properties
        params.setUrl(null);
        assertThatCode(() -> new PseudoThread()).isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("data-manager-properties");
    }

    @Test
    public void testStart() {
        // this should have no effect
        mgr.start();

        mgr.stop();

        // this should also have no effect
        assertThatCode(() -> mgr.start()).doesNotThrowAnyException();
    }

    @Test
    public void testStore_testStop() throws InterruptedException {
        // store
        mgr.store(REQ_ID, event, MY_ENTITY, operation);

        runThread();

        assertEquals(1, mgr.getRecordsCommitted());
    }

    /**
     * Tests stop() when the manager isn't running.
     */
    @Test
    public void testStopNotRunning() {
        // use a manager that hasn't been started yet
        mgr = new PseudoThread();
        mgr.stop();

        verify(emfSpy).close();
    }

    /**
     * Tests store() when it is already stopped.
     */
    @Test
    public void testStoreAlreadyStopped() throws InterruptedException {
        mgr.stop();

        // store
        mgr.store(REQ_ID, event, MY_ENTITY, operation);

        assertEquals(0, mgr.getRecordsCommitted());
    }

    /**
     * Tests store() when when the queue is full.
     */
    @Test
    public void testStoreTooManyItems() throws InterruptedException {
        final int nextra = 5;
        for (int nitems = 0; nitems < MAX_QUEUE_LENGTH + nextra; ++nitems) {
            mgr.store(REQ_ID, event, MY_ENTITY, operation);
        }

        runThread();

        assertEquals(MAX_QUEUE_LENGTH, mgr.getRecordsCommitted());
    }

    @Test
    public void testRun() throws InterruptedException {

        // trigger thread shutdown when it completes this batch
        when(emfSpy.createEntityManager()).thenAnswer(ans -> {
            mgr.stop();
            return emf.createEntityManager();
        });


        mgr = new RealThread();
        mgr.start();

        mgr.store(REQ_ID, event, MY_ENTITY, operation);
        mgr.store(REQ_ID, event, MY_ENTITY, operation);
        mgr.store(REQ_ID, event, MY_ENTITY, operation);

        waitForThread();

        verify(emfSpy).close();

        assertEquals(3, mgr.getRecordsCommitted());
    }

    private void waitForThread() {
        await().atMost(5, TimeUnit.SECONDS).until(() -> !thread.isAlive());
    }

    /**
     * Tests run() when the entity manager throws an exception.
     */
    @Test
    public void testRunException() throws InterruptedException {
        AtomicInteger count = new AtomicInteger(0);

        when(emfSpy.createEntityManager()).thenAnswer(ans -> {
            if (count.incrementAndGet() == 2) {
                // interrupt during one of the attempts
                thread.interrupt();
            }

            // throw an exception for each record
            throw EXPECTED_EXCEPTION;
        });


        mgr = new RealThread();
        mgr.start();

        mgr.store(REQ_ID, event, MY_ENTITY, operation);
        mgr.store(REQ_ID, event, MY_ENTITY, operation);
        mgr.store(REQ_ID, event, MY_ENTITY, operation);

        waitForThread();

        verify(emfSpy).close();
    }

    /**
     * Tests storeRemainingRecords() when the entity manager throws an exception.
     */
    @Test
    public void testStoreRemainingRecordsException() throws InterruptedException {
        // arrange to throw an exception
        when(emfSpy.createEntityManager()).thenThrow(EXPECTED_EXCEPTION);

        mgr.store(REQ_ID, event, MY_ENTITY, operation);

        runThread();
    }

    @Test
    public void testStoreRecord() throws InterruptedException {
        /*
         * Note: we change sub-request ID each time to guarantee that the records are
         * unique.
         */

        // no start time
        mgr.store(REQ_ID, event, MY_ENTITY, operation);

        // no end time
        operation = new ControlLoopOperation(operation);
        operation.setSubRequestId(UUID.randomUUID().toString());
        operation.setStart(Instant.now());
        mgr.store(REQ_ID, event, MY_ENTITY, operation);

        // both start and end times
        operation = new ControlLoopOperation(operation);
        operation.setSubRequestId(UUID.randomUUID().toString());
        operation.setEnd(Instant.now());
        mgr.store(REQ_ID, event, MY_ENTITY, operation);

        // only end time
        operation = new ControlLoopOperation(operation);
        operation.setSubRequestId(UUID.randomUUID().toString());
        operation.setStart(null);
        mgr.store(REQ_ID, event, MY_ENTITY, operation);

        runThread();

        // all of them should have been stored
        assertEquals(4, mgr.getRecordsCommitted());

        // each was unique
        assertEquals(4, mgr.getRecordsInserted());
        assertEquals(0, mgr.getRecordsUpdated());
    }

    /**
     * Tests storeRecord() when records are updated.
     */
    @Test
    public void testStoreRecordUpdate() throws InterruptedException {
        /*
         * Note: we do NOT change sub-request ID, so that records all refer to the same DB
         * record.
         */

        // no start time
        operation.setStart(null);
        mgr.store(REQ_ID, event, MY_ENTITY, operation);

        // no end time
        operation = new ControlLoopOperation(operation);
        operation.setStart(Instant.now());
        mgr.store(REQ_ID, event, MY_ENTITY, operation);

        // both start and end times
        operation = new ControlLoopOperation(operation);
        operation.setEnd(Instant.now());
        mgr.store(REQ_ID, event, MY_ENTITY, operation);

        // only end time
        operation = new ControlLoopOperation(operation);
        operation.setStart(null);
        mgr.store(REQ_ID, event, MY_ENTITY, operation);

        runThread();

        // all of them should have been stored
        assertEquals(4, mgr.getRecordsCommitted());

        // only one new record
        assertEquals(1, mgr.getRecordsInserted());

        // remainder were updates
        assertEquals(3, mgr.getRecordsUpdated());
    }

    private void runThread() throws InterruptedException {
        if (threadFunction == null) {
            return;
        }

        Thread thread2 = new Thread(() -> {
            threadFunction.accept(emfSpy);
            finished.countDown();
        });

        thread2.setDaemon(true);
        thread2.start();

        mgr.stop();

        assertTrue(finished.await(5, TimeUnit.SECONDS));
    }

    private static OperationHistoryDataManagerParamsBuilder makeBuilder() {
        // @formatter:off
        return OperationHistoryDataManagerParams.builder()
                        .url("jdbc:h2:mem:" + OperationHistoryDataManagerImplTest.class.getSimpleName())
                        .userName("sa")
                        .password("")
                        .batchSize(BATCH_SIZE)
                        .maxQueueLength(MAX_QUEUE_LENGTH);
        // @formatter:on
    }

    /**
     * Manager that uses the shared DB.
     */
    private class SharedDb extends OperationHistoryDataManagerImpl {
        public SharedDb() {
            super(params);
        }

        @Override
        protected EntityManagerFactory makeEntityManagerFactory(String opsHistPu, Properties props) {
            // re-use the same factory to avoid re-creating the DB for each test
            return emfSpy;
        }
    }

    /**
     * Manager that uses the shared DB and a pseudo thread.
     */
    private class PseudoThread extends SharedDb {

        @Override
        protected Thread makeThread(EntityManagerFactory emfactory, Consumer<EntityManagerFactory> command) {
            threadFunction = command;
            return thread;
        }
    }

    /**
     * Manager that uses the shared DB and catches the thread.
     */
    private class RealThread extends SharedDb {

        @Override
        protected Thread makeThread(EntityManagerFactory emfactory, Consumer<EntityManagerFactory> command) {
            thread = super.makeThread(emfactory, command);
            return thread;
        }
    }
}
