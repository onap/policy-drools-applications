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

package org.onap.policy.controlloop.eventmanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.common.utils.io.Serializer;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.OperationResult;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.controlloop.ophistory.OperationHistoryDataManager;
import org.onap.policy.controlloop.ophistory.OperationHistoryDataManagerStub;
import org.onap.policy.drools.core.lock.LockCallback;
import org.onap.policy.drools.core.lock.LockImpl;
import org.onap.policy.drools.core.lock.LockState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

@RunWith(MockitoJUnitRunner.class)
public class ControlLoopEventManagerTest {
    private static final UUID REQ_ID = UUID.randomUUID();
    private static final String EXPECTED_EXCEPTION = "expected exception";
    private static final String CL_NAME = "my-closed-loop-name";
    private static final String POLICY_NAME = "my-policy-name";
    private static final String POLICY_SCOPE = "my-scope";
    private static final String POLICY_VERSION = "1.2.3";
    private static final String LOCK1 = "my-lock-A";
    private static final String LOCK2 = "my-lock-B";
    private static final Coder yamlCoder = new StandardYamlCoder();
    private static final String MY_KEY = "def";

    @Mock
    private ExecutorService executor;
    @Mock
    private EventManagerServices services;
    @Mock
    private OperationHistoryDataManager dataMgr;

    private long preCreateTimeMs;
    private List<LockImpl> locks;
    private ToscaPolicy tosca;
    private ControlLoopParams params;
    private ControlLoopEventManager mgr;

    /**
     * Sets up.
     */
    @Before
    public void setUp() throws ControlLoopException, CoderException {
        when(services.getDataManager()).thenReturn(dataMgr);

        params = new ControlLoopParams();
        params.setClosedLoopControlName(CL_NAME);
        params.setPolicyName(POLICY_NAME);
        params.setPolicyScope(POLICY_SCOPE);
        params.setPolicyVersion(POLICY_VERSION);

        loadPolicy("eventManager/event-mgr-simple.yaml");

        locks = new ArrayList<>();

        preCreateTimeMs = System.currentTimeMillis();

        MyManager.executor = executor;
        MyManager.locks = locks;

        mgr = new MyManager(services, params, REQ_ID);
    }

    @Test
    public void testConstructor() {
        assertEquals(POLICY_NAME, mgr.getPolicyName());

        assertTrue(mgr.isActive());
        assertEquals(CL_NAME, mgr.getClosedLoopControlName());
        assertSame(REQ_ID, mgr.getRequestId());
        assertEquals(POLICY_NAME, mgr.getPolicyName());
        assertEquals(POLICY_VERSION, mgr.getPolicyVersion());
        assertNotNull(mgr.getProcessor());
        assertThat(mgr.getEndTimeMs()).isGreaterThanOrEqualTo(preCreateTimeMs);
    }

    @Test
    public void testGetCreateCount() throws ControlLoopException {
        long original = ControlLoopEventManager.getCreateCount();

        new MyManager(services, params, REQ_ID);
        assertEquals(original + 1, ControlLoopEventManager.getCreateCount());

        new MyManager(services, params, REQ_ID);
        assertEquals(original + 2, ControlLoopEventManager.getCreateCount());
    }

    @Test
    public void testIsActive() throws Exception {
        mgr = new ControlLoopEventManager(services, params, REQ_ID);
        assertTrue(mgr.isActive());

        ControlLoopEventManager mgr2 = Serializer.roundTrip(mgr);
        assertFalse(mgr2.isActive());
    }

    @Test
    public void testDestroy() throws IOException {
        mgr.requestLock(LOCK1);
        mgr.requestLock(LOCK2);
        mgr.requestLock(LOCK1);

        // ensure destroy() doesn't throw an exception if the object is deserialized
        ControlLoopEventManager mgr2 = Serializer.roundTrip(mgr);
        assertThatCode(() -> mgr2.destroy()).doesNotThrowAnyException();

        // locks should not have been freed
        for (LockImpl lock : locks) {
            assertFalse(lock.isUnavailable());
        }

        mgr.destroy();

        runExecutor();

        for (LockImpl lock : locks) {
            assertTrue(lock.isUnavailable());
        }
    }

    @Test
    public void testDetmControlLoopTimeoutMs() throws Exception {
        long timeMs = 1200 * 1000L;
        long end = mgr.getEndTimeMs();
        assertThat(end).isGreaterThanOrEqualTo(preCreateTimeMs + timeMs).isLessThan(preCreateTimeMs + timeMs + 5000);
    }

    @Test
    public void testRequestLock() {
        final CompletableFuture<OperationOutcome> future1 = mgr.requestLock(LOCK1);
        assertTrue(mgr.getOutcomes().isEmpty());

        final CompletableFuture<OperationOutcome> future2 = mgr.requestLock(LOCK2);
        assertTrue(mgr.getOutcomes().isEmpty());

        assertSame(future1, mgr.requestLock(LOCK1));
        assertTrue(mgr.getOutcomes().isEmpty());

        assertEquals(2, locks.size());

        assertTrue(future1.isDone());
        assertTrue(future2.isDone());

        // indicate that the first lock failed
        locks.get(0).notifyUnavailable();

        verifyLock(OperationResult.FAILURE, ActorConstants.LOCK_OPERATION);
        assertTrue(mgr.getOutcomes().isEmpty());
    }

    @Test
    public void testReleaseLock() {
        mgr.requestLock(LOCK1);
        mgr.requestLock(LOCK2);

        // release one lock
        final CompletableFuture<OperationOutcome> future = mgr.releaseLock(LOCK1);

        // asynchronous, thus should not have executed yet
        assertThat(future.isDone()).isFalse();

        // asynchronous, thus everything should still be locked
        for (LockImpl lock : locks) {
            assertThat(lock.isUnavailable()).isFalse();
        }

        runExecutor();

        verifyLock(OperationResult.SUCCESS, ActorConstants.UNLOCK_OPERATION);
        assertThat(mgr.getOutcomes()).isEmpty();

        // first lock should have been released, thus no longer available to the manager
        assertThat(locks.get(0).isUnavailable()).isTrue();

        // second should still be locked
        assertThat(locks.get(1).isUnavailable()).isFalse();
    }

    /**
     * Tests releaseLock() when there is no lock.
     */
    @Test
    public void testReleaseLockNotLocked() {
        final CompletableFuture<OperationOutcome> future = mgr.releaseLock(LOCK1);

        // lock didn't exist, so the request should already be complete
        assertThat(future.isDone()).isTrue();

        verifyLock(OperationResult.SUCCESS, ActorConstants.UNLOCK_OPERATION);
        assertThat(mgr.getOutcomes()).isEmpty();
    }

    /**
     * Tests releaseLock() when lock.free() throws an exception.
     */
    @Test
    public void testReleaseLockException() throws ControlLoopException {
        mgr = new MyManager(services, params, REQ_ID) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void makeLock(String targetEntity, String requestId, int holdSec, LockCallback callback) {

                LockImpl lock = new LockImpl(LockState.ACTIVE, targetEntity, requestId, holdSec, callback) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean free() {
                        throw new RuntimeException(EXPECTED_EXCEPTION);
                    }
                };

                locks.add(lock);
                callback.lockAvailable(lock);
            }
        };

        mgr.requestLock(LOCK1);

        // release the lock
        final CompletableFuture<OperationOutcome> future = mgr.releaseLock(LOCK1);

        // asynchronous, thus should not have executed yet
        assertThat(future.isDone()).isFalse();

        runExecutor();

        verifyLock(OperationResult.FAILURE_EXCEPTION, ActorConstants.UNLOCK_OPERATION);
        assertThat(mgr.getOutcomes()).isEmpty();
    }

    private void verifyLock(OperationResult result, String lockOperation) {
        OperationOutcome outcome = mgr.getOutcomes().poll();
        assertNotNull(outcome);
        assertEquals(ActorConstants.LOCK_ACTOR, outcome.getActor());
        assertEquals(lockOperation, outcome.getOperation());
        assertNotNull(outcome.getEnd());
        assertTrue(outcome.isFinalOutcome());
        assertEquals(result, outcome.getResult());
    }

    @Test
    public void testOnStart() {
        OperationOutcome outcome1 = new OperationOutcome();
        OperationOutcome outcome2 = new OperationOutcome();

        mgr.onStart(outcome1);
        mgr.onStart(outcome2);

        assertSame(outcome1, mgr.getOutcomes().poll());
        assertSame(outcome2, mgr.getOutcomes().poll());
        assertTrue(mgr.getOutcomes().isEmpty());
    }

    @Test
    public void testOnComplete() {
        OperationOutcome outcome1 = new OperationOutcome();
        OperationOutcome outcome2 = new OperationOutcome();

        mgr.onComplete(outcome1);
        mgr.onComplete(outcome2);

        assertSame(outcome1, mgr.getOutcomes().poll());
        assertSame(outcome2, mgr.getOutcomes().poll());
        assertTrue(mgr.getOutcomes().isEmpty());
    }

    @Test
    public void testContains_testGetProperty_testSetProperty_testRemoveProperty() {
        mgr.setProperty("abc", "a string");
        mgr.setProperty(MY_KEY, 100);

        assertTrue(mgr.contains(MY_KEY));
        assertFalse(mgr.contains("ghi"));

        String strValue = mgr.getProperty("abc");
        assertEquals("a string", strValue);

        int intValue = mgr.getProperty(MY_KEY);
        assertEquals(100, intValue);

        mgr.removeProperty(MY_KEY);
        assertFalse(mgr.contains(MY_KEY));
    }

    /**
     * Tests getDataManager() when not disabled.
     */
    @Test
    public void testGetDataManagerNotDisabled() throws ControlLoopException {
        assertThat(mgr.getDataManager()).isSameAs(dataMgr);
    }

    /**
     * Tests getDataManager() when guard.disabled=true.
     */
    @Test
    public void testGetDataManagerDisabled() throws ControlLoopException {
        mgr = new MyManager(services, params, REQ_ID) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getEnvironmentProperty(String propName) {
                return ("guard.disabled".equals(propName) ? "true" : null);
            }
        };

        assertThat(mgr.getDataManager()).isInstanceOf(OperationHistoryDataManagerStub.class);
    }

    @Test
    public void testToString() {
        assertNotNull(mgr.toString());
    }

    private void loadPolicy(String fileName) throws CoderException {
        ToscaServiceTemplate template =
                        yamlCoder.decode(ResourceUtils.getResourceAsString(fileName), ToscaServiceTemplate.class);
        tosca = template.getToscaTopologyTemplate().getPolicies().get(0).values().iterator().next();

        params.setToscaPolicy(tosca);
    }

    private void runExecutor() {
        ArgumentCaptor<Runnable> runCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runCaptor.capture());

        runCaptor.getValue().run();
    }


    private static class MyManager extends ControlLoopEventManager {
        private static final long serialVersionUID = 1L;

        private static ExecutorService executor;
        private static List<LockImpl> locks;

        public MyManager(EventManagerServices services, ControlLoopParams params, UUID requestId)
                        throws ControlLoopException {
            super(services, params, requestId);
        }

        @Override
        protected ExecutorService getBlockingExecutor() {
            return executor;
        }

        @Override
        protected void makeLock(String targetEntity, String requestId, int holdSec, LockCallback callback) {
            LockImpl lock = new LockImpl(LockState.ACTIVE, targetEntity, requestId, holdSec, callback);
            locks.add(lock);
            callback.lockAvailable(lock);
        }
    }
}
