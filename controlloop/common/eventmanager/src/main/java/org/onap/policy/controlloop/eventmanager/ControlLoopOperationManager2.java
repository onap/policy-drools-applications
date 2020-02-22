/*-
 * ============LICENSE_START=======================================================
 * controlloop operation manager
 * ================================================================================
 * Copyright (C) 2017-2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019 Huawei Technologies Co., Ltd. All rights reserved.
 * Modifications Copyright (C) 2019 Tech Mahindra
 * Modifications Copyright (C) 2019 Bell Canada.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.ToString;
import org.onap.policy.aai.AaiConstants;
import org.onap.policy.aai.AaiCqResponse;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.controlloop.ControlLoopEventContext;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.actorserviceprovider.pipeline.PipelineUtil;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages a single Operation.
 */
@ToString(onlyExplicitlyIncluded = true)
public class ControlLoopOperationManager2 implements Serializable {
    private static final long serialVersionUID = -3773199283624595410L;
    private static final Logger logger = LoggerFactory.getLogger(ControlLoopOperationManager2.class);
    public static final String LOCK_ACTOR = "LOCK";
    public static final String LOCK_OPERATION = "Lock";
    private static final String GUARD_ACTOR = "GUARD";
    private static final String VSERVER_VSERVER_NAME = "vserver.vserver-name";
    private static final String GENERIC_VNF_VNF_NAME = "generic-vnf.vnf-name";
    private static final String GENERIC_VNF_VNF_ID = "generic-vnf.vnf-id";
    private static final String PNF_NAME = "pnf.pnf-name";

    // @formatter:off
    public enum State {
        STARTING,
        LOCK_DENIED,
        LOCK_LOST,
        GUARD_STARTED,
        GUARD_PERMITTED,
        GUARD_DENIED,
        OPERATION_STARTED,
        OPERATION_SUCCESS,
        OPERATION_FAILURE,
        OPERATION_FINAL_FAILURE,
        UNKNOWN
    }
    // @formatter:on

    private final transient ControlLoopEventContext context;
    private final Policy policy;
    private final transient BiFunction<String, Runnable, CompletableFuture<OperationOutcome>> lockManager;
    private final transient OperationHistoryDataManager dataManager;

    /**
     * Invoked whenever "this" changes due to callbacks from the Actor.
     */
    private final transient Consumer<ControlLoopOperationManager2> listener;

    @Getter
    @ToString.Include
    private State state = State.STARTING;

    @ToString.Include
    private final String requestId;

    @ToString.Include
    private final String policyId;

    /**
     * Bumped each time the "start" callback is invoked by the Actor, provided it's for
     * this operation.
     */
    @ToString.Include
    private int attempts = 0;

    @ToString.Include
    private final Deque<Operation> operationHistory = new ConcurrentLinkedDeque<>();

    /**
     * Queue of outcomes yet to be processed. Outcomes are added to this each time the
     * "start" or "complete" callback is invoked.
     */
    private final transient Deque<OperationOutcome> outcomes = new ConcurrentLinkedDeque<>();

    /**
     * Used to cancel the running operation.
     */
    private transient CompletableFuture<OperationOutcome> future = null;

    /**
     * Target entity. Determined after the lock is granted, though it may require the
     * custom query to be performed, first.
     */
    private String targetEntity;

    private final transient ControlLoopOperationParams params;
    private final transient PipelineUtil taskUtil;

    // values extracted from the policy
    private final String actor;
    private final String operation;


    /**
     * Construct an instance.
     *
     * @param context event context
     * @param policy operation's policy
     * @param lockManager lock manager
     * @param listener callback to invoke when this changes
     */
    public ControlLoopOperationManager2(ControlLoopEventContext context, Policy policy,
                    BiFunction<String, Runnable, CompletableFuture<OperationOutcome>> lockManager,
                    OperationHistoryDataManager dataManager, Consumer<ControlLoopOperationManager2> listener) {

        this.context = context;
        this.policy = policy;
        this.listener = listener;
        this.lockManager = lockManager;
        this.dataManager = dataManager;

        this.requestId = "" + context.getEvent().getRequestId();
        this.policyId = "" + policy.getId();
        this.actor = policy.getActor();
        this.operation = policy.getRecipe();

        // TODO populate actorService

        // @formatter:off
        params = ControlLoopOperationParams.builder()
                        .actor(AaiConstants.ACTOR_NAME)
                        .operation(AaiCqResponse.OPERATION)
                        .context(context)
                        .build();
        // @formatter:on

        taskUtil = new PipelineUtil(params);
    }

    //
    // Internal class used for tracking
    //
    @Getter
    @ToString
    private class Operation implements Serializable {
        private static final long serialVersionUID = 1L;

        private int attempt;
        private PolicyResult policyResult;
        private ControlLoopOperation clOperation;

        /**
         * Constructs the object.
         *
         * @param outcome outcome of the operation
         */
        public Operation(OperationOutcome outcome) {
            attempt = ControlLoopOperationManager2.this.attempts;
            policyResult = outcome.getResult();
            clOperation = outcome.toControlLoopOperation();
        }
    }

    /**
     * Start the operation, first acquiring any locks that are needed.
     */
    @SuppressWarnings("unchecked")
    public void start() {
        // do the steps

        // @formatter:off
        future = taskUtil.sequence(
            this::detmTarget,
            this::requestLock,
            this::startOperation);
        // @formatter:on

        // handle any exceptions that may be thrown
        future.whenComplete((unused, thrown) -> {
            if (thrown != null) {
                logger.warn("{}.{}: exception starting operation for {}", actor, operation, params.getRequestId(),
                                thrown);
                OperationOutcome outcome = taskUtil.setOutcome(params.makeOutcome(), thrown);
                outcome.setStart(Instant.now());
                onStart(new OperationOutcome(outcome));

                outcome.setEnd(outcome.getStart());
                outcome.setFinalOutcome(true);
                onComplete(outcome);
            }
        });
    }

    /**
     * Cancels the operation.
     */
    public synchronized void cancel() {
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * Determines the target entity.
     *
     * @return a future to determine the target entity, or {@code null} if the entity has
     *         already been determined
     */
    private CompletableFuture<OperationOutcome> detmTarget() {
        if (policy.getTarget() == null) {
            throw new IllegalArgumentException("The target is null");
        }

        if (policy.getTarget().getType() == null) {
            throw new IllegalArgumentException("The target type is null");
        }

        switch (policy.getTarget().getType()) {
            case PNF:
                return detmPnfTarget();
            case VM:
            case VNF:
            case VFMODULE:
                return detmVfModuleTarget();
            default:
                throw new IllegalArgumentException("The target type is not supported");
        }
    }

    /**
     * Determines the PNF target entity.
     *
     * @return a future to determine the target entity, or {@code null} if the entity has
     *         already been determined
     */
    private CompletableFuture<OperationOutcome> detmPnfTarget() {
        if (!PNF_NAME.equalsIgnoreCase(context.getEvent().getTarget())) {
            throw new IllegalArgumentException("Target in the onset event is either null or does not match"
                            + " target key expected in AAI section.");
        }

        targetEntity = context.getEnrichment().get(PNF_NAME);
        return null;
    }

    /**
     * Determines the VF Module target entity.
     *
     * @return a future to determine the target entity, or {@code null} if the entity has
     *         already been determined
     */
    private CompletableFuture<OperationOutcome> detmVfModuleTarget() {
        String targetType2 = context.getEvent().getTarget();
        if (targetType2 == null) {
            throw new IllegalArgumentException("Target is null");
        }

        switch (targetType2.toLowerCase()) {
            case VSERVER_VSERVER_NAME:
                targetEntity = context.getEnrichment().get(VSERVER_VSERVER_NAME);
                return null;

            case GENERIC_VNF_VNF_ID:
                targetEntity = context.getEnrichment().get(GENERIC_VNF_VNF_ID);
                return null;

            case GENERIC_VNF_VNF_NAME:
                return detmVnfName();

            default:
                throw new IllegalArgumentException("Target does not match target type");
        }
    }

    /**
     * Determines the VNF Name target entity.
     *
     * @return a future to determine the target entity, or {@code null} if the entity has
     *         already been determined
     */
    @SuppressWarnings("unchecked")
    private CompletableFuture<OperationOutcome> detmVnfName() {
        /*
         * If the onset is enriched with the vnf-id, we don't need an A&AI response
         */
        if (context.getEnrichment().containsKey(GENERIC_VNF_VNF_ID)) {
            targetEntity = context.getEnrichment().get(GENERIC_VNF_VNF_ID);
            return null;
        }

        /*
         * If the vnf-name was retrieved from the onset then the vnf-id must be obtained
         * via the custom query.
         */

        // TODO populate targetEntity

        // @formatter:off
        ControlLoopOperationParams cqparams = params.toBuilder()
                        .actor(AaiConstants.ACTOR_NAME)
                        .operation(AaiCqResponse.OPERATION)
                        .build();
        // @formatter:on

        // perform custom query and then extract the VNF ID from it
        return taskUtil.sequence(() -> context.obtain(AaiCqResponse.CONTEXT_KEY, cqparams), this::extractVnfFromCq);
    }

    /**
     * Extracts the VNF Name target entity from the custom query data.
     *
     * @return a completed future
     */
    private CompletableFuture<OperationOutcome> extractVnfFromCq() {
        // already have the CQ data
        AaiCqResponse cq = context.getProperty(AaiCqResponse.CONTEXT_KEY);
        if (cq.getDefaultGenericVnf() == null) {
            throw new IllegalArgumentException("No vnf-id found");
        }

        targetEntity = cq.getDefaultGenericVnf().getVnfId();
        if (targetEntity == null) {
            throw new IllegalArgumentException("No vnf-id found");
        }

        return CompletableFuture.completedFuture(params.makeOutcome());
    }

    /**
     * Requests a lock on the {@link #targetEntity}.
     *
     * @return a future to await the lock
     */
    private CompletableFuture<OperationOutcome> requestLock() {
        CompletableFuture<OperationOutcome> lockFuture = lockManager.apply(targetEntity, this::lockUnavailable);

        // exceptions are handled at a higher level - handle other failures here
        lockFuture.thenAccept(outcome -> {
            // discard successes
            if (outcome.getResult() != PolicyResult.SUCCESS) {
                onComplete(new OperationOutcome(outcome));
            }
        });

        return lockFuture;
    }

    /**
     * Indicates that the lock on the target entity is unavailable.
     */
    private void lockUnavailable() {
        // lost the lock - cancel the operation
        cancel();

        logger.warn("{}.{}: lock was lost for {}", actor, operation, params.getRequestId());

        // indicate lock lost
        OperationOutcome outcome = new OperationOutcome();
        outcome.setActor(LOCK_ACTOR);
        outcome.setOperation(LOCK_OPERATION);
        outcome.setStart(Instant.now());
        outcome.setEnd(outcome.getStart());
        outcome.setResult(PolicyResult.FAILURE);
        onComplete(outcome);
    }

    /**
     * Start the operation, after the lock has been acquired.
     *
     * @return
     */
    private CompletableFuture<OperationOutcome> startOperation() {
        // @formatter:off
        return params.toBuilder()
                    .payload(new LinkedHashMap<>(policy.getPayload()))
                    .retry(policy.getRetry())
                    .timeoutSec(policy.getTimeout())
                    .target(policy.getTarget())
                    .targetEntity(targetEntity)
                    .startCallback(this::onStart)
                    .completeCallback(this::onComplete)
                    .build()
                .start();
        // @formatter:on
    }

    /**
     * Handles responses provided via the "start" callback.
     *
     * @param outcome outcome provided to the callback
     */
    private synchronized void onStart(OperationOutcome outcome) {
        if (outcome.isFor(actor, operation) || GUARD_ACTOR.equals(outcome.getActor())) {
            addOutcome(outcome);
        }
    }

    /**
     * Handles responses provided via the "complete" callback.
     *
     * @param outcome outcome provided to the callback
     */
    private synchronized void onComplete(OperationOutcome outcome) {

        switch (outcome.getActor()) {
            case LOCK_ACTOR:
            case GUARD_ACTOR:
                addOutcome(outcome);
                break;

            default:
                if (outcome.isFor(actor, operation)) {
                    addOutcome(outcome);
                }
                break;
        }
    }

    /**
     * Adds an outcome to {@link #outcomes}.
     *
     * @param outcome outcome to be added
     */
    private void addOutcome(OperationOutcome outcome) {
        logger.debug("added outcome={} for {}", outcome, params.getRequestId());
        outcomes.add(outcome);

        if (outcomes.peekFirst() == outcomes.peekLast()) {
            // this is the first outcome in the queue - process it
            processOutcome();
            listener.accept(this);
        }
    }

    /**
     * Looks for the next step in the queue. May change the {@link #state}, but does not
     * invoke the {@link #listener}.
     *
     * @return {@code true} if more responses are expected, {@code false} otherwise
     */
    public synchronized boolean nextStep() {
        switch (state) {
            case LOCK_DENIED:
            case LOCK_LOST:
            case GUARD_DENIED:
                return false;
            default:
                break;
        }

        OperationOutcome outcome = outcomes.peek();
        if (outcome == null) {
            // empty queue
            return true;
        }

        if (outcome.isFinalOutcome()) {
            return false;
        }

        // first item has been processed, remove it
        outcomes.remove();
        if (!outcomes.isEmpty()) {
            // have a new "first" item - process it
            processOutcome();
        }

        return true;
    }

    /**
     * Processes the first item in {@link #outcomes}. Sets the state, increments
     * {@link #attempts}, if appropriate, and stores the operation history in the DB.
     */
    private synchronized void processOutcome() {
        OperationOutcome outcome = outcomes.peek();
        logger.debug("process outcome={} for {}", outcome, params.getRequestId());

        switch (outcome.getActor()) {

            case LOCK_ACTOR:
                if (state == State.STARTING) {
                    state = State.LOCK_DENIED;
                    storeFailureInDataBase(outcome, PolicyResult.FAILURE_GUARD, "Operation denied by Lock");
                } else {
                    state = State.LOCK_LOST;
                    storeFailureInDataBase(outcome, PolicyResult.FAILURE, "Operation aborted by Lock");
                }
                break;

            case GUARD_ACTOR:
                if (outcome.getEnd() == null) {
                    state = State.GUARD_STARTED;
                } else if (outcome.getResult() == PolicyResult.SUCCESS) {
                    state = State.GUARD_PERMITTED;
                } else {
                    state = State.GUARD_DENIED;
                    storeFailureInDataBase(outcome, PolicyResult.FAILURE_GUARD, "Operation denied by Guard");
                }
                break;

            default:
                if (outcome.getEnd() == null) {
                    // start of a new attempt
                    ++attempts;
                    state = State.OPERATION_STARTED;
                } else if (outcome.getResult() == PolicyResult.SUCCESS) {
                    state = State.OPERATION_SUCCESS;
                } else {
                    state = State.OPERATION_FAILURE;
                }
                operationHistory.add(new Operation(outcome));
                storeOperationInDataBase();
                break;
        }
    }

    /**
     * Get the latest operation history.
     *
     * @return the latest operation history
     */
    public String getOperationHistory() {
        Operation last = operationHistory.peekLast();
        return (last == null ? null : last.clOperation.toHistory());
    }

    /**
     * Get the history.
     *
     * @return the list of control loop operations
     */
    public List<ControlLoopOperation> getHistory() {
        return operationHistory.stream().map(Operation::getClOperation).map(ControlLoopOperation::new)
                        .collect(Collectors.toList());
    }

    /**
     * Stores a failure in the DB.
     *
     * @param outcome operation outcome
     * @param result result to put into the DB
     * @param message message to put into the DB
     */
    private void storeFailureInDataBase(OperationOutcome outcome, PolicyResult result, String message) {
        outcome.setActor(actor);
        outcome.setOperation(operation);
        outcome.setMessage(message);
        outcome.setResult(result);

        operationHistory.add(new Operation(outcome));
        storeOperationInDataBase();
    }

    /**
     * Stores the latest operation in the DB, if guards are enabled.
     */
    private void storeOperationInDataBase() {
        dataManager.store(context.getEvent(), operationHistory.peekLast().getClOperation());
    }
}
