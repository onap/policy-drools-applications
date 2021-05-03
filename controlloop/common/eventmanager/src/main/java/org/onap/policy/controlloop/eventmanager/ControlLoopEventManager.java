/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2017-2021 AT&T Intellectual Property. All rights reserved.
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

import java.io.Serializable;
import java.time.Instant;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.actorserviceprovider.ActorService;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.OperationResult;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.controlloop.ophistory.OperationHistoryDataManager;
import org.onap.policy.controlloop.ophistory.OperationHistoryDataManagerStub;
import org.onap.policy.controlloop.processor.ControlLoopProcessor;
import org.onap.policy.drools.core.lock.LockCallback;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for a single event. Once this has been created, the event can be retracted from
 * working memory. Invoke {@link #isActive()} to determine if the manager is active (i.e.,
 * hasn't been replicated from another server). When the manager is no longer needed,
 * {@link #destroy()} should be invoked.
 */
@ToString(onlyExplicitlyIncluded = true)
public class ControlLoopEventManager implements StepContext, Serializable {

    private static final Logger logger = LoggerFactory.getLogger(ControlLoopEventManager.class);
    private static final long serialVersionUID = -1216568161322872641L;

    /**
     * Data manager used when the policy engine's guard.disabled property is "true".
     */
    private static final OperationHistoryDataManager STUB_DATA_MANAGER = new OperationHistoryDataManagerStub();

    private static final String GUARD_DISABLED_PROPERTY = "guard.disabled";
    private static final String EVENT_MANAGER_SERVICE_CONFIG = "event-manager";

    /**
     * Counts the number of these objects that have been created. This is used by junit
     * tests.
     */
    private static final AtomicLong createCount = new AtomicLong(0);

    /**
     * {@code True} if this object was created by this JVM instance, {@code false}
     * otherwise. This will be {@code false} if this object is reconstituted from a
     * persistent store or by transfer from another server.
     */
    private transient boolean createdByThisJvmInstance;

    @Getter
    @ToString.Include
    public final String closedLoopControlName;
    @Getter
    @ToString.Include
    private final UUID requestId;

    /**
     * Time, in milliseconds, when the control loop will time out.
     */
    @Getter
    private final long endTimeMs;

    // fields extracted from the ControlLoopParams
    @Getter
    private final String policyName;
    @Getter
    private final String policyVersion;

    /**
     * Maps a target entity to its lock.
     */
    private final transient Map<String, LockData> target2lock = new HashMap<>();

    @Getter(AccessLevel.PROTECTED)
    private final ControlLoopProcessor processor;

    /**
     * Set of properties used while processing the event.
     */
    private Map<String, Serializable> properties = new ConcurrentHashMap<>();

    /**
     * Unprocessed outcomes from the operations. Outcomes are added to this each time the
     * "start" or "complete" callback is invoked, typically by an operation running in a
     * background thread, thus it must be thread safe.
     */
    @Getter
    private final transient Deque<OperationOutcome> outcomes = new ConcurrentLinkedDeque<>();


    /**
     * Constructs the object.
     *
     * @param params control loop parameters
     * @param requestId event request ID
     * @throws ControlLoopException if the event is invalid or if a YAML processor cannot
     *         be created
     */
    public ControlLoopEventManager(ControlLoopParams params, UUID requestId) throws ControlLoopException {

        createCount.incrementAndGet();

        this.createdByThisJvmInstance = true;
        this.closedLoopControlName = params.getClosedLoopControlName();
        this.requestId = requestId;
        this.policyName = params.getPolicyName();
        this.policyVersion = params.getPolicyVersion();
        this.processor = new ControlLoopProcessor(params.getToscaPolicy());
        this.endTimeMs = System.currentTimeMillis() + detmControlLoopTimeoutMs();
    }

    /**
     * Gets the number of manager objects that have been created.
     *
     * @return the number of manager objects that have been created
     */
    public static long getCreateCount() {
        return createCount.get();
    }

    /**
     * Determines if the manager is still active.
     *
     * @return {@code true} if the manager is still active, {@code false} otherwise
     */
    public boolean isActive() {
        return createdByThisJvmInstance;
    }

    /**
     * Cancels the current operation and frees all locks.
     */
    public void destroy() {
        if (isActive()) {
            getBlockingExecutor().execute(this::freeAllLocks);
        }
    }

    /**
     * Frees all locks.
     */
    private void freeAllLocks() {
        target2lock.values().forEach(LockData::free);
    }

    /**
     * Determines the overall control loop timeout.
     *
     * @return the policy timeout, in milliseconds, if specified, a default timeout
     *         otherwise
     */
    private long detmControlLoopTimeoutMs() {
        // validation checks preclude null or 0 timeout values in the policy
        Integer timeout = processor.getPolicy().getProperties().getTimeout();
        return TimeUnit.MILLISECONDS.convert(timeout, TimeUnit.SECONDS);
    }

    @Override
    public synchronized CompletableFuture<OperationOutcome> requestLock(String targetEntity) {

        long remainingMs = endTimeMs - System.currentTimeMillis();
        int remainingSec = 15 + Math.max(0, (int) TimeUnit.SECONDS.convert(remainingMs, TimeUnit.MILLISECONDS));

        LockData data = target2lock.computeIfAbsent(targetEntity, key -> {
            LockData data2 = new LockData(key, requestId);
            makeLock(targetEntity, requestId.toString(), remainingSec, data2);

            data2.addUnavailableCallback(this::onComplete);

            return data2;
        });

        return data.getFuture();
    }

    @Override
    public synchronized CompletableFuture<OperationOutcome> releaseLock(String targetEntity) {
        LockData data = target2lock.remove(targetEntity);

        if (data == null) {
            // lock did not exist - immediately return a success
            OperationOutcome outcome = makeUnlockOutcome(targetEntity);
            outcome.setEnd(outcome.getStart());
            onComplete(outcome);

            return CompletableFuture.completedFuture(outcome);
        }

        /*
         * previous lock operation may not have completed yet, thus we tack the unlock
         * operation onto it.
         *
         * Note: we must invoke free(), asynchronously (i.e., using whenCompleteAsync()),
         * as it may block
         */

        return data.getFuture().whenCompleteAsync((lockOutcome, thrown) -> {

            OperationOutcome outcome = makeUnlockOutcome(targetEntity);

            try {
                data.free();

            } catch (RuntimeException e) {
                logger.warn("failed to unlock {}", targetEntity, e);
                outcome.setResult(OperationResult.FAILURE_EXCEPTION);
                outcome.setMessage(ControlLoopOperation.FAILED_MSG + ": " + e.getMessage());
            }

            outcome.setEnd(Instant.now());
            onComplete(outcome);

        }, getBlockingExecutor());
    }

    private OperationOutcome makeUnlockOutcome(String targetEntity) {
        OperationOutcome outcome = new OperationOutcome();
        outcome.setActor(ActorConstants.LOCK_ACTOR);
        outcome.setOperation(ActorConstants.UNLOCK_OPERATION);
        outcome.setTarget(targetEntity);
        outcome.setResult(OperationResult.SUCCESS);
        outcome.setMessage(ControlLoopOperation.SUCCESS_MSG);
        outcome.setFinalOutcome(true);
        outcome.setStart(Instant.now());
        return outcome;
    }

    public void onStart(OperationOutcome outcome) {
        outcomes.add(outcome);
    }

    public void onComplete(OperationOutcome outcome) {
        outcomes.add(outcome);
    }

    /**
     * Determines if the context contains a property.
     *
     * @param name name of the property of interest
     * @return {@code true} if the context contains the property, {@code false} otherwise
     */
    @Override
    public boolean contains(String name) {
        return properties.containsKey(name);
    }

    /**
     * Gets a property, casting it to the desired type.
     *
     * @param <T> desired type
     * @param name name of the property whose value is to be retrieved
     * @return the property's value, or {@code null} if it does not yet have a value
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String name) {
        return (T) properties.get(name);
    }

    /**
     * Sets a property's value.
     *
     * @param name property name
     * @param value new property value
     */
    @Override
    public void setProperty(String name, Serializable value) {
        logger.info("set property {}={} manager={}", name, value, this);
        properties.put(name, value);
    }

    /**
     * Removes a property.
     *
     * @param name property name
     */
    @Override
    public void removeProperty(String name) {
        properties.remove(name);
    }

    /**
     * Initializes various components, on demand.
     */
    private static class LazyInitData {
        private static final OperationHistoryDataManager DATA_MANAGER;
        private static final ActorService ACTOR_SERVICE;

        static {
            EventManagerServices services = new EventManagerServices(EVENT_MANAGER_SERVICE_CONFIG);
            ACTOR_SERVICE = services.getActorService();
            DATA_MANAGER = services.getDataManager();
        }
    }

    // the following methods may be overridden by junit tests

    public Executor getExecutor() {
        return ForkJoinPool.commonPool();
    }

    protected ExecutorService getBlockingExecutor() {
        return PolicyEngineConstants.getManager().getExecutorService();
    }

    protected void makeLock(String targetEntity, String requestId, int holdSec, LockCallback callback) {
        PolicyEngineConstants.getManager().createLock(targetEntity, requestId, holdSec, callback, false);
    }

    public ActorService getActorService() {
        return LazyInitData.ACTOR_SERVICE;
    }

    public OperationHistoryDataManager getDataManager() {
        boolean guardDisabled = "true".equalsIgnoreCase(getEnvironmentProperty(GUARD_DISABLED_PROPERTY));
        return (guardDisabled ? STUB_DATA_MANAGER : LazyInitData.DATA_MANAGER);
    }

    protected String getEnvironmentProperty(String propName) {
        return PolicyEngineConstants.getManager().getEnvironmentProperty(propName);
    }
}
