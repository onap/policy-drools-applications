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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.NonNull;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.actorserviceprovider.pipeline.PipelineUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single step within a single policy. The rules typically break a policy operation down
 * into separate steps. For instance, a policy for VF-Module-Create would be broken down
 * into lock target, A&AI tenant query, A&AI custom query, guard, and finally
 * VF-Module-Create.
 */
public class Step {
    private static final Logger logger = LoggerFactory.getLogger(Step.class);

    public static final String CL_TIMEOUT_ACTOR = "-CL-TIMEOUT-";

    @Getter
    protected ControlLoopOperationParams params;

    /**
     * Time when the first step started for the current policy. This is shared by all
     * steps for the policy. When a step is started and finds this to be {@code null}, it
     * sets the value. Subsequent steps leave it unchanged.
     */
    private final AtomicReference<Instant> startTime;

    /**
     * {@code True} if this step is for the policy's actual operation, {@code false} if it's a preprocessor step.
     */
    @Getter
    private final boolean policyStep;

    /**
     * The operation for this step.
     */
    private Operation operation = null;

    /**
     * Used to cancel the running operation.
     */
    private CompletableFuture<OperationOutcome> future;


    /**
     * Constructs the object.  This is used when constructing the step for the policy's actual operation.
     *
     * @param params operation parameters
     * @param startTime start time of the first step for the current policy, initially
     *        containing {@code null}
     */
    public Step(ControlLoopOperationParams params, @NonNull AtomicReference<Instant> startTime) {
        this.params = params;
        this.startTime = startTime;
        this.policyStep = true;
    }

    /**
     * Constructs the object using information from another step.  This is used when constructing a preprocessing
     * step.
     *
     * @param otherStep step whose information should be used
     * @param actor actor name
     * @param operation operation name
     */
    public Step(Step otherStep, String actor, String operation) {
        this.params = otherStep.params.toBuilder().actor(actor).operation(operation).retry(null).timeoutSec(null)
                        .payload(new LinkedHashMap<>()).build();
        this.startTime = otherStep.startTime;
        this.policyStep = false;
    }

    public String getActorName() {
        return params.getActor();
    }

    public String getOperationName() {
        return params.getOperation();
    }

    /**
     * Determines if the operation has been initialized (i.e., created).
     *
     * @return {@code true} if the operation has been initialized, {@code false} otherwise
     */
    public boolean isInitialized() {
        return (operation != null);
    }

    /**
     * Initializes the step, creating the operation if it has not yet been created.
     */
    public void init() {
        if (operation == null) {
            operation = buildOperation();
        }
    }

    /**
     * Gets the operation, initializing it if necessary.
     *
     * @return the operation
     */
    public Operation getOperation() {
        return operation;
    }

    /**
     * Starts the operation.
     *
     * @param remainingMs time remaining, in milliseconds, for the control loop
     * @return {@code true} if started, {@code false} if the step is no longer necessary
     *         (i.e., because it was previously completed)
     */
    public boolean start(long remainingMs) {
        if (!isInitialized()) {
            throw new IllegalStateException("step has not been initialized");
        }

        if (future != null) {
            throw new IllegalStateException("step is already running");
        }

        try {
            initStartTime();
            future = operation.start();

            // handle any exceptions that may be thrown, set timeout, and handle timeout

            // @formatter:off
            future.exceptionally(this::handleException)
                    .orTimeout(remainingMs, TimeUnit.MILLISECONDS)
                    .exceptionally(this::handleTimeout);
            // @formatter:on

        } catch (RuntimeException e) {
            handleException(e);
        }

        return true;
    }

    /**
     * Handles exceptions that may be generated.
     *
     * @param thrown exception that was generated
     * @return {@code null}
     */
    private OperationOutcome handleException(Throwable thrown) {
        if (thrown instanceof CancellationException || thrown.getCause() instanceof CancellationException) {
            return null;
        }

        logger.warn("{}.{}: exception starting operation for {}", params.getActor(), params.getOperation(),
                        params.getRequestId(), thrown);
        OperationOutcome outcome = new PipelineUtil(params).setOutcome(params.makeOutcome(), thrown);
        outcome.setStart(startTime.get());
        outcome.setEnd(Instant.now());
        outcome.setFinalOutcome(true);
        params.getCompleteCallback().accept(outcome);

        // this outcome is not used so just return "null"
        return null;
    }

    /**
     * Handles control loop timeout exception.
     *
     * @param thrown exception that was generated
     * @return {@code null}
     */
    private OperationOutcome handleTimeout(Throwable thrown) {
        logger.warn("{}.{}: control loop timeout for {}", params.getActor(), params.getOperation(),
                        params.getRequestId(), thrown);

        OperationOutcome outcome = new PipelineUtil(params).setOutcome(params.makeOutcome(), thrown);
        outcome.setActor(CL_TIMEOUT_ACTOR);
        outcome.setOperation(null);
        outcome.setStart(startTime.get());
        outcome.setEnd(Instant.now());
        outcome.setFinalOutcome(true);
        params.getCompleteCallback().accept(outcome);

        // cancel the operation, if it's still running
        future.cancel(false);

        // this outcome is not used so just return "null"
        return null;
    }

    /**
     * Cancels the operation, if it's running.
     */
    public void cancel() {
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * Initializes the start time, if it's still unset.
     */
    private void initStartTime() {
        if (startTime.get() == null) {
            startTime.set(Instant.now());
        }
    }

    /**
     * Gets the start time.
     *
     * @return the start time, or {@code null} if it hasn't been initialized yet
     */
    public Instant getStartTime() {
        return startTime.get();
    }

    /**
     * Builds the operation. The default method simply invokes
     * {@link ControlLoopOperationParams#build()}.
     *
     * @return a new operation
     */
    protected Operation buildOperation() {
        return params.build();
    }
}
