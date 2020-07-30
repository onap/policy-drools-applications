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
import java.util.Deque;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import org.onap.policy.controlloop.actorserviceprovider.ActorService;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.controlloop.ControlLoopEventContext;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.actorserviceprovider.pipeline.PipelineUtil;
import org.onap.policy.controlloop.policy.Policy;
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
    private static final String CL_TIMEOUT_ACTOR = "-CL-TIMEOUT-";

    private final ControlLoopOperationParams params;

    /**
     * The operation for this step. Initially {@code null}, this is set to a value once
     * the rules begin to set the properties for the operation.
     */
    private Operation stepOperation;

    // values extracted from the params
    @Getter
    private final String actor;
    @Getter
    private final String operation;

    /**
     * Unprocessed outcomes from the operations. Outcomes are added to this each time the
     * "start" or "complete" callback is invoked, typically by an operation running in a
     * background thread.
     */
    @Getter
    private final Deque<OperationOutcome> outcomes = new ConcurrentLinkedDeque<>();

    /**
     * Used to cancel the running operation.
     */
    private CompletableFuture<OperationOutcome> future;

    /**
     * Time when the policy first started.
     */
    private final AtomicReference<Instant> policyStart;


    /**
     * Constructs the object. This is used to create the first step in a policy and is
     * associated with the actual operation specified in the policy. It is then used to
     * construct the other steps that must be executed prior to this step.
     *
     * @param actorService actor service
     * @param context event context
     * @param policy operation's policy
     * @param executor executor for the Operation
     */
    public Step(ActorService actorService, ControlLoopEventContext context, Policy policy, Executor executor) {

        this.actor = policy.getActor();
        this.operation = policy.getRecipe();
        this.policyStart = new AtomicReference<>();

        // @formatter:off
        this.params = ControlLoopOperationParams.builder()
                        .actorService(actorService)
                        .actor(actor)
                        .operation(operation)
                        .context(context)
                        .executor(executor)
                        .target(policy.getTarget())
                        .startCallback(this::onStart)
                        .completeCallback(this::onComplete)
                        .build();
        // @formatter:on
    }

    /**
     * Constructs a new step. Allocates a new parameter object from given step's parameter
     * object.
     *
     * @param actor step's actor
     * @param operation step's operation
     * @param otherStep another step within the same policy
     */
    public Step(String actor, String operation, Step otherStep) {
        this.params = otherStep.params.toBuilder().actor(actor).operation(operation).payload(null).retry(null)
                        .timeoutSec(null).startCallback(this::onStart).completeCallback(this::onComplete).build();
        this.actor = actor;
        this.operation = operation;
        this.policyStart = otherStep.policyStart;
    }

    /**
     * Handles responses provided via the "start" callback.
     *
     * @param outcome outcome provided to the callback
     */
    private void onStart(OperationOutcome outcome) {
        addOutcome(outcome);
    }

    /**
     * Handles responses provided via the "complete" callback.
     *
     * @param outcome outcome provided to the callback
     */
    private void onComplete(OperationOutcome outcome) {
        addOutcome(outcome);
    }

    /**
     * Adds an outcome to {@link #outcomes}.
     *
     * @param outcome outcome to be added
     */
    private void addOutcome(OperationOutcome outcome) {
        if (!outcome.isFor(actor, operation) && !CL_TIMEOUT_ACTOR.equals(outcome.getActor())) {
            return;
        }

        logger.debug("adding outcome={} for {}", outcome, params.getRequestId());
        outcomes.add(outcome);
    }

    /**
     * Determines if the step has been initialized.
     *
     * @return {@code true} if the step has been initialized, {@code false} otherwise
     */
    public boolean isInitialized() {
        return (stepOperation != null);
    }

    /**
     * Gets the step's operation, building it if necessary.
     *
     * @return the step's operation
     */
    public Operation getStepOperation() {
        if (stepOperation == null) {
            stepOperation = params.build();
        }

        return stepOperation;
    }

    /**
     * Starts the operation.
     *
     * @param remainingMs time remaining, in milliseconds, for the control loop
     */
    public void start(long remainingMs) {
        if (!isInitialized()) {
            throw new IllegalStateException("step has not been initialized");
        }

        if (future != null) {
            throw new IllegalStateException("step is already running");
        }

        try {
            if (policyStart.get() == null) {
                policyStart.set(Instant.now());
            }

            future = stepOperation.start();

            // handle any exceptions that may be thrown, set timeout, and handle timeout

            // @formatter:off
            future.exceptionally(this::handleException)
                    .orTimeout(remainingMs, TimeUnit.MILLISECONDS)
                    .exceptionally(this::handleTimeout);
            // @formatter:on

        } catch (RuntimeException e) {
            handleException(e);
        }
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

        logger.warn("{}.{}: exception starting operation for {}", actor, operation, params.getRequestId(), thrown);
        OperationOutcome outcome = new PipelineUtil(params).setOutcome(params.makeOutcome(), thrown);
        outcome.setStart(policyStart.get());
        outcome.setEnd(Instant.now());
        outcome.setFinalOutcome(true);
        onComplete(outcome);

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
        logger.warn("{}.{}: control loop timeout for {}", actor, operation, params.getRequestId(), thrown);

        OperationOutcome outcome = new PipelineUtil(params).setOutcome(params.makeOutcome(), thrown);
        outcome.setActor(CL_TIMEOUT_ACTOR);
        outcome.setOperation(null);
        outcome.setStart(policyStart.get());
        outcome.setEnd(Instant.now());
        outcome.setFinalOutcome(true);
        onComplete(outcome);

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
}
