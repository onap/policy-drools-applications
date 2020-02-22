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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.persistence.EntityManagerFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.guard.Util;

public class OperationHistoryDataManagerImplTest {

    private static final IllegalStateException EXPECTED_EXCEPTION = new IllegalStateException("expected exception");
    private static final String MY_TARGET = "my-target";
    private static final String REQ_ID = "my-request-id";

    private static String saveOnapKeyUrl;
    private static String saveOnapKeyUser;
    private static String saveOnapKeyPass;

    private static EntityManagerFactory emf;

    @Mock
    private Thread thread;

    private Runnable threadFunction;
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
        saveOnapKeyUrl = PolicyEngineConstants.getManager().getEnvironmentProperty(Util.ONAP_KEY_URL);
        saveOnapKeyUser = PolicyEngineConstants.getManager().getEnvironmentProperty(Util.ONAP_KEY_USER);
        saveOnapKeyPass = PolicyEngineConstants.getManager().getEnvironmentProperty(Util.ONAP_KEY_PASS);

        initDbProps();

        emf = new OperationHistoryDataManagerImpl().createEntityManagerFactory();
    }

    /**
     * Restores the environment after all tests.
     */
    @AfterClass
    public static void tearDownAfterClass() {
        PolicyEngine engmgr = PolicyEngineConstants.getManager();
        Consumer<String> remover = engmgr.getEnvironment()::remove;
        BiConsumer<String, String> setter = engmgr::setEnvironmentProperty;

        restore(saveOnapKeyUrl, Util.ONAP_KEY_URL, remover, setter);
        restore(saveOnapKeyUser, Util.ONAP_KEY_USER, remover, setter);
        restore(saveOnapKeyPass, Util.ONAP_KEY_PASS, remover, setter);

        emf.close();
    }

    /**
     * Sets up for an individual test.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        initDbProps();

        event = new VirtualControlLoopEvent();
        event.setRequestId(UUID.randomUUID());

        operation = new ControlLoopOperation();
        operation.setTarget(MY_TARGET);

        threadFunction = null;
        finished = new CountDownLatch(1);

        // prevent the "real" emf from being closed
        emfSpy = spy(emf);
        doAnswer(ans -> null).when(emfSpy).close();

        mgr = new PseudoThread();
    }

    @After
    public void tearDown() {
        mgr.stop();
    }

    @Test
    public void testStore_testStop() throws InterruptedException {
        // should not start the thread before store() is called
        verify(thread, never()).start();

        // store
        mgr.store(REQ_ID, event, operation);

        // should have started the thread
        verify(thread).start();

        runThread();

        assertEquals(1, mgr.getRecordsAdded());
    }

    /**
     * Tests store() when it is already stopped.
     */
    @Test
    public void testStoreAlreadyStopped() throws InterruptedException {
        mgr.stop();

        // store
        mgr.store(REQ_ID, event, operation);

        // should not have started a thread
        verify(thread, never()).start();

        assertEquals(0, mgr.getRecordsAdded());
    }

    /**
     * Tests store() when when the queue is full.
     */
    @Test
    public void testStoreTooManyItems() throws InterruptedException {
        final int nextra = 5;
        for (int nitems = 0; nitems < OperationHistoryDataManagerImpl.MAX_QUEUE_LENGTH + nextra; ++nitems) {
            mgr.store(REQ_ID, event, operation);
        }

        runThread();

        assertEquals(OperationHistoryDataManagerImpl.MAX_QUEUE_LENGTH, mgr.getRecordsAdded());
    }

    /**
     * Tests store() when when it cannot create the entity manager.
     */
    @Test
    public void testStoreNoEntityMgr() throws InterruptedException {
        // this manager always gets a null factory
        mgr = new PseudoThread() {
            @Override
            protected EntityManagerFactory createEntityManagerFactory() {
                return null;
            }
        };

        mgr.store(REQ_ID, event, operation);
        mgr.store(REQ_ID, event, operation);

        verify(thread, never()).start();

        assertEquals(0, mgr.getRecordsAdded());
    }

    @Test
    public void testRun() throws InterruptedException {

        // trigger thread shutdown when it completes this batch
        when(emfSpy.createEntityManager()).thenAnswer(ans -> {
            mgr.stop();
            return emf.createEntityManager();
        });


        mgr = new RealThread();

        mgr.store(REQ_ID, event, operation);
        mgr.store(REQ_ID, event, operation);
        mgr.store(REQ_ID, event, operation);

        thread.join(5000);
        assertFalse(thread.isAlive());

        verify(emfSpy).close();

        assertEquals(3, mgr.getRecordsAdded());
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

        mgr.store(REQ_ID, event, operation);
        mgr.store(REQ_ID, event, operation);
        mgr.store(REQ_ID, event, operation);

        thread.join(5000);
        assertFalse(thread.isAlive());

        verify(emfSpy).close();
    }

    /**
     * Tests storeRemainingRecords() when the entity manager throws an exception.
     */
    @Test
    public void testStoreRemainingRecordsException() throws InterruptedException {
        // arrange to throw an exception
        when(emfSpy.createEntityManager()).thenThrow(EXPECTED_EXCEPTION);

        mgr.store(REQ_ID, event, operation);

        runThread();
    }

    @Test
    public void testStoreRecord() throws InterruptedException {
        // no start time
        mgr.store(REQ_ID, event, operation);

        // no start time
        operation = new ControlLoopOperation(operation);
        operation.setStart(Instant.now());
        mgr.store(REQ_ID, event, operation);

        // both start and end times
        operation = new ControlLoopOperation(operation);
        operation.setEnd(Instant.now());
        mgr.store(REQ_ID, event, operation);

        // only end time
        operation = new ControlLoopOperation(operation);
        operation.setStart(null);
        mgr.store(REQ_ID, event, operation);

        runThread();

        // all of them should have been stored
        assertEquals(4, mgr.getRecordsAdded());
    }

    /**
     * Tests createEntityManagerFactory() when an exception is thrown.
     */
    @Test
    public void testCreateEntityManagerFactoryException() {
        // use a real DB
        mgr = new OperationHistoryDataManagerImpl() {
            @Override
            protected Thread makeThread(Runnable command) {
                throw new IllegalStateException("should not happen");
            }

            @Override
            protected EntityManagerFactory makeEntityManagerFactory(Properties props, String opsHistPu) {
                throw EXPECTED_EXCEPTION;
            }
        };

        mgr.store(REQ_ID, event, operation);
        mgr.store(REQ_ID, event, operation);
        mgr.store(REQ_ID, event, operation);

        verify(thread, never()).start();
    }


    private static void initDbProps() {
        PolicyEngine engmgr = PolicyEngineConstants.getManager();
        BiConsumer<String, String> setter = engmgr::setEnvironmentProperty;

        setter.accept(Util.ONAP_KEY_URL, "jdbc:h2:mem:" + OperationHistoryDataManagerImplTest.class.getSimpleName());
        setter.accept(Util.ONAP_KEY_USER, "sa");
        setter.accept(Util.ONAP_KEY_PASS, "");
    }

    private void runThread() throws InterruptedException {
        if (threadFunction == null) {
            return;
        }

        Thread thread2 = new Thread(() -> {
            threadFunction.run();
            finished.countDown();
        });

        thread2.setDaemon(true);
        thread2.start();

        mgr.stop();

        assertTrue(finished.await(5, TimeUnit.SECONDS));
    }

    private static void restore(String savedValue, String key, Consumer<String> remover,
                    BiConsumer<String, String> setter) {

        if (savedValue == null) {
            remover.accept(key);
        } else {
            setter.accept(key, savedValue);
        }
    }

    /**
     * Manager that uses the shared DB.
     */
    private class SharedDb extends OperationHistoryDataManagerImpl {

        @Override
        protected EntityManagerFactory createEntityManagerFactory() {
            // re-use the same factory to avoid re-creating the DB for each test
            return emfSpy;
        }
    }

    /**
     * Manager that uses the shared DB and a pseudo thread.
     */
    private class PseudoThread extends SharedDb {

        @Override
        protected Thread makeThread(Runnable command) {
            threadFunction = command;
            return thread;
        }
    }

    /**
     * Manager that uses the shared DB and catches the thread.
     */
    private class RealThread extends SharedDb {

        @Override
        protected Thread makeThread(Runnable command) {
            // TODO Auto-generated method stub
            thread = super.makeThread(command);
            return thread;
        }
    }
}
