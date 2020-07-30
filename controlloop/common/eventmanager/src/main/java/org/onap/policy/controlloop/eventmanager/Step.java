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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.Getter;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
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

    public static final String CL_TIMEOUT_ACTOR = "-CL-TIMEOUT-";


    @Getter
    private final ControlLoopOperationParams params;

    @Getter(AccessLevel.PROTECTED)
    private final StepContext context;

    /**
     * The operation for this step. Initially {@code null}, this is set to a value once
     * the rules begin to set the properties for the operation.
     */
    private Operation policyOperation;

    // values extracted from the params
    @Getter
    private final String actor;
    @Getter
    private final String operation;

    /**
     * Used to cancel the running operation.
     */
    private CompletableFuture<OperationOutcome> future;


    /**
     * Constructs the object. This is used to create the first step in a policy and is
     * associated with the actual operation specified in the policy. It is then used to
     * construct the other steps that must be executed prior to this step.
     *
     * @param context the step's context
     * @param policy operation's policy
     */
    public Step(StepContext context, Policy policy) {

        this.context = context;
        this.actor = policy.getActor();
        this.operation = policy.getRecipe();

        // @formatter:off
        this.params = ControlLoopOperationParams.builder()
                        .actorService(context.getActorService())
                        .actor(actor)
                        .operation(operation)
                        .context(context.getContext())
                        .executor(context.getExecutor())
                        .target(policy.getTarget())
                        .startCallback(context::onStart)
                        .completeCallback(context::onComplete)
                        .build();
        // @formatter:on
    }

    /**
     * Constructs a new step. Allocates a new parameter object from the given step's
     * parameter object.
     *
     * @param actor step's actor
     * @param operation step's operation
     * @param otherStep another step within the same policy
     */
    public Step(String actor, String operation, Step otherStep) {
        this.context = otherStep.context;
        this.actor = actor;
        this.operation = operation;
        this.params = otherStep.params.toBuilder().actor(actor).operation(operation).payload(null).retry(null)
                        .timeoutSec(null).build();
    }

    /**
     * Determines if this step owns the given outcome (i.e., the outcome is for this
     * step).
     *
     * @param outcome outcome that was received
     * @return {@code true} if this steps owns the outcome, {@code false} otherwise
     */
    public boolean owns(OperationOutcome outcome) {
        return outcome.isFor(this.getActor(), this.getOperation());
    }

    /**
     * Determines if the step has been initialized.
     *
     * @return {@code true} if the step has been initialized, {@code false} otherwise
     */
    public boolean isInitialized() {
        return (policyOperation != null);
    }

    /**
     * Gets the step's operation, building it if necessary.
     *
     * @return the step's operation
     */
    public Operation getPolicyOperation() {
        if (policyOperation == null) {
            policyOperation = buildOperation();
        }

        return policyOperation;
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
            context.setStartTime();
            future = policyOperation.start();

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

        logger.warn("{}.{}: exception starting operation for {}", actor, operation, params.getRequestId(), thrown);
        OperationOutcome outcome = new PipelineUtil(params).setOutcome(params.makeOutcome(), thrown);
        outcome.setStart(context.getStartTime());
        outcome.setEnd(Instant.now());
        outcome.setFinalOutcome(true);
        context.onComplete(outcome);

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
        outcome.setStart(context.getStartTime());
        outcome.setEnd(Instant.now());
        outcome.setFinalOutcome(true);
        context.abort(outcome);

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
     * Builds the Operation.
     *
     * @return a new Operation
     */
    protected Operation buildOperation() {
        return params.build();
    }
}
