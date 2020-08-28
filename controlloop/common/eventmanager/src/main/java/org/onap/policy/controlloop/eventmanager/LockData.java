/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2017-2020 AT&T Intellectual Property. All rights reserved.
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.OperationResult;
import org.onap.policy.drools.core.lock.Lock;
import org.onap.policy.drools.core.lock.LockCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Data for an individual lock.
 */
public class LockData implements LockCallback {
    private static final Logger logger = LoggerFactory.getLogger(LockData.class);

    private final String targetEntity;
    private final UUID requestId;

    /**
     * Time when this was created.
     */
    private final Instant createTime = Instant.now();

    /**
     * Future for obtaining the lock. Initially incomplete.
     */
    private final AtomicReference<CompletableFuture<OperationOutcome>> future =
                    new AtomicReference<>(new CompletableFuture<>());

    /**
     * The lock.
     */
    private Lock theLock = null;

    /**
     * Listeners to invoke if the lock is unavailable/lost.
     */
    private final List<Consumer<OperationOutcome>> unavailableCallbacks = new ArrayList<>();

    /**
     * Set to a failed outcome, if the lock becomes unavailable.
     */
    private OperationOutcome failedOutcome = null;


    /**
     * Constructs the object.
     *
     * @param targetEntity target entity
     */
    public LockData(String targetEntity, UUID requestId) {
        this.targetEntity = targetEntity;
        this.requestId = requestId;
    }

    /**
     * Gets the future to be completed when the lock operation completes.
     *
     * @return the lock operation future
     */
    public CompletableFuture<OperationOutcome> getFuture() {
        return future.get();
    }

    /**
     * Adds a callback to be invoked if the lock becomes unavailable.
     *
     * @param callback callback to be added
     */
    public void addUnavailableCallback(Consumer<OperationOutcome> callback) {
        synchronized (this) {
            if (failedOutcome == null) {
                // hasn't failed yet - add it to the list
                unavailableCallbacks.add(callback);
                return;
            }
        }

        // already failed - invoke the callback immediately
        callback.accept(failedOutcome);
    }

    /**
     * Frees the lock.
     */
    public void free() {
        Lock lock;

        synchronized (this) {
            if ((lock = theLock) == null) {
                return;
            }
        }

        lock.free();
    }

    @Override
    public synchronized void lockAvailable(Lock lock) {
        logger.info("lock granted on {} for {}", targetEntity, requestId);
        theLock = lock;

        OperationOutcome outcome = makeOutcome();
        outcome.setResult(OperationResult.SUCCESS);
        outcome.setMessage(ControlLoopOperation.SUCCESS_MSG);

        future.get().complete(outcome);
    }

    @Override
    public void lockUnavailable(Lock unused) {
        synchronized (this) {
            logger.warn("lock unavailable on {} for {}", targetEntity, requestId);
            failedOutcome = makeOutcome();
            failedOutcome.setResult(OperationResult.FAILURE);
            failedOutcome.setMessage(ControlLoopOperation.FAILED_MSG);
        }

        /*
         * In case the future was already completed successfully, replace it with a failed
         * future, but complete the old one, too, in case it wasn't completed yet.
         */
        future.getAndSet(CompletableFuture.completedFuture(failedOutcome)).complete(failedOutcome);

        for (Consumer<OperationOutcome> callback : unavailableCallbacks) {
            try {
                callback.accept(new OperationOutcome(failedOutcome));
            } catch (RuntimeException e) {
                logger.warn("lock callback threw an exception for {}", requestId, e);
            }
        }

        unavailableCallbacks.clear();
    }

    /**
     * Makes a lock operation outcome.
     *
     * @return a new lock operation outcome
     */
    private OperationOutcome makeOutcome() {
        OperationOutcome outcome = new OperationOutcome();
        outcome.setActor(ControlLoopOperationManager2.LOCK_ACTOR);
        outcome.setOperation(ControlLoopOperationManager2.LOCK_OPERATION);
        outcome.setTarget(targetEntity);
        outcome.setFinalOutcome(true);
        outcome.setStart(createTime);
        outcome.setEnd(Instant.now());

        return outcome;
    }
}
