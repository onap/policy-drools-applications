/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2017-2020 AT&T Intellectual Property. All rights reserved.
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

import static org.onap.policy.controlloop.ControlLoopTargetType.PNF;
import static org.onap.policy.controlloop.ControlLoopTargetType.VF;
import static org.onap.policy.controlloop.ControlLoopTargetType.VM;
import static org.onap.policy.controlloop.ControlLoopTargetType.VNF;

import java.io.Serializable;
import java.time.Instant;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;
import org.onap.policy.aai.AaiConstants;
import org.onap.policy.aai.AaiCqResponse;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.ControlLoopResponse;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actor.guard.GuardActor;
import org.onap.policy.controlloop.actor.sdnr.SdnrActor;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.OperationResult;
import org.onap.policy.controlloop.actorserviceprovider.controlloop.ControlLoopEventContext;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.actorserviceprovider.pipeline.PipelineUtil;
import org.onap.policy.drools.domain.models.operational.OperationalTarget;
import org.onap.policy.sdnr.PciMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages a single Operation for a single event. Once this has been created,
 * {@link #start()} should be invoked, and then {@link #nextStep()} should be invoked
 * continually until it returns {@code false}, indicating that all steps have completed.
 */
@ToString(onlyExplicitlyIncluded = true)
public class ControlLoopOperationManager2 implements Serializable {
    private static final long serialVersionUID = -3773199283624595410L;
    private static final Logger logger = LoggerFactory.getLogger(ControlLoopOperationManager2.class);
    private static final String CL_TIMEOUT_ACTOR = "-CL-TIMEOUT-";
    public static final String LOCK_ACTOR = "LOCK";
    public static final String LOCK_OPERATION = "Lock";
    private static final String GUARD_ACTOR = GuardActor.NAME;
    public static final String VSERVER_VSERVER_NAME = "vserver.vserver-name";
    public static final String GENERIC_VNF_VNF_NAME = "generic-vnf.vnf-name";
    public static final String GENERIC_VNF_VNF_ID = "generic-vnf.vnf-id";
    public static final String PNF_NAME = "pnf.pnf-name";

    // @formatter:off
    public enum State {
        ACTIVE,
        LOCK_DENIED,
        LOCK_LOST,
        GUARD_STARTED,
        GUARD_PERMITTED,
        GUARD_DENIED,
        OPERATION_STARTED,
        OPERATION_SUCCESS,
        OPERATION_FAILURE,
        CONTROL_LOOP_TIMEOUT
    }
    // @formatter:on

    private final transient ManagerContext operContext;
    private final transient ControlLoopEventContext eventContext;
    private final org.onap.policy.drools.domain.models.operational.Operation policy;

    @Getter
    @ToString.Include
    private State state = State.ACTIVE;

    @ToString.Include
    private final String requestId;

    @ToString.Include
    private final String policyId;

    /**
     * Bumped each time the "complete" callback is invoked by the Actor, provided it's for
     * this operation.
     */
    @ToString.Include
    private int attempts = 0;

    private final Deque<Operation> operationHistory = new ConcurrentLinkedDeque<>();

    /**
     * Set to {@code true} to prevent the last item in {@link #operationHistory} from
     * being included in the outcome of {@link #getHistory()}. Used when the operation
     * aborts prematurely due to lock-denied, guard-denied, etc.
     */
    private boolean holdLast = false;

    /**
     * Queue of outcomes yet to be processed. Outcomes are added to this each time the
     * "start" or "complete" callback is invoked.
     */
    @Getter(AccessLevel.PROTECTED)
    private final transient Deque<OperationOutcome> outcomes = new ConcurrentLinkedDeque<>();

    /**
     * Used to cancel the running operation.
     */
    @Getter(AccessLevel.PROTECTED)
    private transient CompletableFuture<OperationOutcome> future = null;

    /**
     * Target entity. Determined after the lock is granted, though it may require the
     * custom query to be performed first.
     */
    @Getter
    private String targetEntity;

    @Getter(AccessLevel.PROTECTED)
    private final transient ControlLoopOperationParams params;
    private final transient PipelineUtil taskUtil;

    @Getter
    private ControlLoopResponse controlLoopResponse;

    /**
     * Time when the lock was first requested.
     */
    private transient AtomicReference<Instant> lockStart = new AtomicReference<>();

    // values extracted from the policy
    @Getter
    private final String actor;
    @Getter
    private final String operation;

    private final String targetStr;
    private final OperationalTarget target;


    /**
     * Construct an instance.
     *
     * @param operContext this operation's context
     * @param context event context
     * @param operation2 operation's policy
     * @param executor executor for the Operation
     */
    public ControlLoopOperationManager2(ManagerContext operContext, ControlLoopEventContext context,
                    org.onap.policy.drools.domain.models.operational.Operation operation2, Executor executor) {

        this.operContext = operContext;
        this.eventContext = context;
        this.policy = operation2;
        this.requestId = context.getEvent().getRequestId().toString();
        this.policyId = "" + operation2.getId();
        this.actor = operation2.getActorOperation().getActor();
        this.operation = operation2.getActorOperation().getOperation();
        this.target = operation2.getActorOperation().getTarget();

        String targetType = (target != null ? target.getTargetType() : null);
        Map<String, String> entityIds = (target != null ? target.getEntityIds() : null);

        // TODO encode()?
        this.targetStr = (target != null ? target.toString() : null);

        // @formatter:off
        params = ControlLoopOperationParams.builder()
                        .actorService(operContext.getActorService())
                        .actor(actor)
                        .operation(operation)
                        .context(context)
                        .executor(executor)
                        .targetType(targetType)
                        .targetEntityIds(entityIds)
                        .startCallback(this::onStart)
                        .completeCallback(this::onComplete)
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
        private OperationResult policyResult;
        private ControlLoopOperation clOperation;
        private ControlLoopResponse clResponse;

        /**
         * Constructs the object.
         *
         * @param outcome outcome of the operation
         */
        public Operation(OperationOutcome outcome) {
            attempt = ControlLoopOperationManager2.this.attempts;
            policyResult = outcome.getResult();
            clOperation = outcome.toControlLoopOperation();
            clOperation.setTarget(targetStr);
            clResponse = makeControlLoopResponse(outcome);

            if (outcome.getEnd() == null) {
                clOperation.setOutcome("Started");
            } else if (clOperation.getOutcome() == null) {
                clOperation.setOutcome("");
            }
        }
    }

    /**
     * Start the operation, first acquiring any locks that are needed. This should not
     * throw any exceptions, but will, instead, invoke the callbacks with exceptions.
     *
     * @param remainingMs time remaining, in milliseconds, for the control loop
     */
    @SuppressWarnings("unchecked")
    public synchronized void start(long remainingMs) {
        // this is synchronized while we update "future"

        try {
            // provide a default, in case something fails before requestLock() is called
            lockStart.set(Instant.now());

            // @formatter:off
            future = taskUtil.sequence(
                this::detmTarget,
                this::requestLock,
                this::startOperation);
            // @formatter:on

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
     * Start the operation, after the lock has been acquired.
     *
     * @return
     */
    private CompletableFuture<OperationOutcome> startOperation() {
        // @formatter:off
        ControlLoopOperationParams params2 = params.toBuilder()
                    .payload(new LinkedHashMap<>())
                    .retry(policy.getRetries())
                    .timeoutSec(policy.getTimeout())
                    .targetEntity(targetEntity)
                    .build();
        // @formatter:on

        if (policy.getActorOperation().getPayload() != null) {
            params2.getPayload().putAll(policy.getActorOperation().getPayload());
        }

        return params2.start();
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

        logger.warn("{}.{}: exception starting operation for {}", actor, operation, requestId, thrown);
        OperationOutcome outcome = taskUtil.setOutcome(params.makeOutcome(), thrown);
        outcome.setStart(lockStart.get());
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
        logger.warn("{}.{}: control loop timeout for {}", actor, operation, requestId, thrown);

        OperationOutcome outcome = taskUtil.setOutcome(params.makeOutcome(), thrown);
        outcome.setActor(CL_TIMEOUT_ACTOR);
        outcome.setOperation(null);
        outcome.setStart(lockStart.get());
        outcome.setEnd(Instant.now());
        outcome.setFinalOutcome(true);
        onComplete(outcome);

        // cancel the operation, if it's still running
        future.cancel(false);

        // this outcome is not used so just return "null"
        return null;
    }

    /**
     * Cancels the operation.
     */
    public void cancel() {
        synchronized (this) {
            if (future == null) {
                return;
            }
        }

        future.cancel(false);
    }

    /**
     * Requests a lock on the {@link #targetEntity}.
     *
     * @return a future to await the lock
     */
    private CompletableFuture<OperationOutcome> requestLock() {
        /*
         * Failures are handled via the callback, and successes are discarded by
         * sequence(), without passing them to onComplete().
         *
         * Return a COPY of the future so that if we try to cancel it, we'll only cancel
         * the copy, not the original. This is done by tacking thenApply() onto the end.
         */
        lockStart.set(Instant.now());
        return operContext.requestLock(targetEntity, this::lockUnavailable).thenApply(outcome -> outcome);
    }

    /**
     * Indicates that the lock on the target entity is unavailable.
     *
     * @param outcome lock outcome
     */
    private void lockUnavailable(OperationOutcome outcome) {

        // Note: NEVER invoke onStart() for locks; only invoke onComplete()
        onComplete(outcome);

        /*
         * Now that we've added the lock outcome to the queue, ensure the future is
         * canceled, which may, itself, generate an operation outcome.
         */
        cancel();
    }

    /**
     * Handles responses provided via the "start" callback. Note: this is never be invoked
     * for locks; only {@link #onComplete(OperationOutcome)} is invoked for locks.
     *
     * @param outcome outcome provided to the callback
     */
    private void onStart(OperationOutcome outcome) {
        if (outcome.isFor(actor, operation) || GUARD_ACTOR.equals(outcome.getActor())) {
            addOutcome(outcome);
        }
    }

    /**
     * Handles responses provided via the "complete" callback. Note: this is never invoked
     * for "successful" locks.
     *
     * @param outcome outcome provided to the callback
     */
    private void onComplete(OperationOutcome outcome) {

        switch (outcome.getActor()) {
            case LOCK_ACTOR:
            case GUARD_ACTOR:
            case CL_TIMEOUT_ACTOR:
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
    private synchronized void addOutcome(OperationOutcome outcome) {
        /*
         * This is synchronized to prevent nextStep() from invoking processOutcome() at
         * the same time.
         */

        logger.debug("added outcome={} for {}", outcome, requestId);
        outcomes.add(outcome);

        if (outcomes.peekFirst() == outcomes.peekLast()) {
            // this is the first outcome in the queue - process it
            processOutcome();
        }
    }

    /**
     * Looks for the next step in the queue.
     *
     * @return {@code true} if more responses are expected, {@code false} otherwise
     */
    public synchronized boolean nextStep() {
        switch (state) {
            case LOCK_DENIED:
            case LOCK_LOST:
            case GUARD_DENIED:
            case CONTROL_LOOP_TIMEOUT:
                holdLast = false;
                return false;
            default:
                break;
        }

        OperationOutcome outcome = outcomes.peek();
        if (outcome == null) {
            // empty queue
            return true;
        }

        if (outcome.isFinalOutcome() && outcome.isFor(actor, operation)) {
            controlLoopResponse = null;
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
        logger.debug("process outcome={} for {}", outcome, requestId);

        controlLoopResponse = null;

        switch (outcome.getActor()) {

            case CL_TIMEOUT_ACTOR:
                state = State.CONTROL_LOOP_TIMEOUT;
                processAbort(outcome, OperationResult.FAILURE, "Control loop timed out");
                break;

            case LOCK_ACTOR:
                // lock is no longer available
                if (state == State.ACTIVE) {
                    state = State.LOCK_DENIED;
                    storeFailureInDataBase(outcome, OperationResult.FAILURE_GUARD, "Operation denied by Lock");
                } else {
                    state = State.LOCK_LOST;
                    processAbort(outcome, OperationResult.FAILURE, "Operation aborted by Lock");
                }
                break;

            case GUARD_ACTOR:
                if (outcome.getEnd() == null) {
                    state = State.GUARD_STARTED;
                } else if (outcome.getResult() == OperationResult.SUCCESS) {
                    state = State.GUARD_PERMITTED;
                } else {
                    state = State.GUARD_DENIED;
                    storeFailureInDataBase(outcome, OperationResult.FAILURE_GUARD, "Operation denied by Guard");
                }
                break;

            default:
                if (outcome.getEnd() == null) {
                    // operation started
                    ++attempts;
                    state = State.OPERATION_STARTED;

                } else {
                    /*
                     * Operation completed. If the last entry was a "start" (i.e., "end" field
                     * is null), then replace it. Otherwise, just add the completion.
                     */
                    state = (outcome.getResult() == OperationResult.SUCCESS ? State.OPERATION_SUCCESS
                                    : State.OPERATION_FAILURE);
                    controlLoopResponse = makeControlLoopResponse(outcome);
                    if (!operationHistory.isEmpty() && operationHistory.peekLast().getClOperation().getEnd() == null) {
                        operationHistory.removeLast();
                    }
                }

                operationHistory.add(new Operation(outcome));
                storeOperationInDataBase();
                break;
        }

        // indicate that this has changed
        operContext.updated(this);
    }

    /**
     * Processes an operation abort, updating the DB record, if an operation has been
     * started.
     *
     * @param outcome operation outcome
     * @param result result to put into the DB
     * @param message message to put into the DB
     */
    private void processAbort(OperationOutcome outcome, OperationResult result, String message) {
        if (operationHistory.isEmpty() || operationHistory.peekLast().getClOperation().getEnd() != null) {
            // last item was not a "start" operation

            // NOTE: do NOT generate control loop response since operation was not started

            storeFailureInDataBase(outcome, result, message);
            return;
        }

        // last item was a "start" operation - replace it with a failure
        final Operation operOrig = operationHistory.removeLast();

        // use start time from the operation, itself
        if (operOrig != null && operOrig.getClOperation() != null) {
            outcome.setStart(operOrig.getClOperation().getStart());
        }

        controlLoopResponse = makeControlLoopResponse(outcome);

        storeFailureInDataBase(outcome, result, message);
    }

    /**
     * Makes a control loop response.
     *
     * @param outcome operation outcome
     * @return a new control loop response, or {@code null} if none is required
     */
    protected ControlLoopResponse makeControlLoopResponse(OperationOutcome outcome) {

        // only generate response for certain actors.
        if (outcome == null || !actor.equals(SdnrActor.NAME)) {
            return null;
        }

        VirtualControlLoopEvent event = eventContext.getEvent();

        ControlLoopResponse clRsp = new ControlLoopResponse();
        clRsp.setFrom(actor);
        clRsp.setTarget("DCAE");
        clRsp.setClosedLoopControlName(event.getClosedLoopControlName());
        clRsp.setPolicyName(event.getPolicyName());
        clRsp.setPolicyVersion(event.getPolicyVersion());
        clRsp.setRequestId(event.getRequestId());
        clRsp.setVersion(event.getVersion());

        PciMessage msg = outcome.getResponse();
        if (msg != null && msg.getBody() != null && msg.getBody().getOutput() != null) {
            clRsp.setPayload(msg.getBody().getOutput().getPayload());
        }

        return clRsp;
    }

    /**
     * Get the operation, as a message.
     *
     * @return the operation, as a message
     */
    public String getOperationMessage() {
        Operation last = operationHistory.peekLast();
        return (last == null ? null : last.getClOperation().toMessage());
    }

    /**
     * Gets the operation result.
     *
     * @return the operation result
     */
    public OperationResult getOperationResult() {
        Operation last = operationHistory.peekLast();
        return (last == null ? OperationResult.FAILURE_EXCEPTION : last.getPolicyResult());
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
        Operation last = (holdLast ? operationHistory.removeLast() : null);

        List<ControlLoopOperation> result = operationHistory.stream().map(Operation::getClOperation)
                        .map(ControlLoopOperation::new).collect(Collectors.toList());

        if (last != null) {
            operationHistory.add(last);
        }

        return result;
    }

    /**
     * Stores a failure in the DB.
     *
     * @param outcome operation outcome
     * @param result result to put into the DB
     * @param message message to put into the DB
     */
    private void storeFailureInDataBase(OperationOutcome outcome, OperationResult result, String message) {
        // don't include this in history yet
        holdLast = true;

        outcome.setActor(actor);
        outcome.setOperation(operation);
        outcome.setMessage(message);
        outcome.setResult(result);

        operationHistory.add(new Operation(outcome));
        storeOperationInDataBase();
    }

    /**
     * Stores the latest operation in the DB.
     */
    private void storeOperationInDataBase() {
        operContext.getDataManager().store(requestId, eventContext.getEvent(), targetEntity,
                        operationHistory.peekLast().getClOperation());
    }

    /**
     * Determines the target entity.
     *
     * @return a future to determine the target entity, or {@code null} if the entity has
     *         already been determined
     */
    protected CompletableFuture<OperationOutcome> detmTarget() {
        if (target == null) {
            throw new IllegalArgumentException("The target is null");
        }

        if (target.getTargetType() == null) {
            throw new IllegalArgumentException("The target type is null");
        }

        switch (target.getTargetType()) {
            case PNF:
                return detmPnfTarget();
            case VM:
            case VNF:
            case VF:
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
        if (!PNF_NAME.equalsIgnoreCase(eventContext.getEvent().getTarget())) {
            throw new IllegalArgumentException("Target does not match target type");
        }

        targetEntity = eventContext.getEnrichment().get(PNF_NAME);
        if (targetEntity == null) {
            throw new IllegalArgumentException("AAI section is missing " + PNF_NAME);
        }

        return null;
    }

    /**
     * Determines the VF Module target entity.
     *
     * @return a future to determine the target entity, or {@code null} if the entity has
     *         already been determined
     */
    private CompletableFuture<OperationOutcome> detmVfModuleTarget() {
        String targetFieldName = eventContext.getEvent().getTarget();
        if (targetFieldName == null) {
            throw new IllegalArgumentException("Target is null");
        }

        switch (targetFieldName.toLowerCase()) {
            case VSERVER_VSERVER_NAME:
                targetEntity = eventContext.getEnrichment().get(VSERVER_VSERVER_NAME);
                break;
            case GENERIC_VNF_VNF_ID:
                targetEntity = eventContext.getEnrichment().get(GENERIC_VNF_VNF_ID);
                break;
            case GENERIC_VNF_VNF_NAME:
                return detmVnfName();
            default:
                throw new IllegalArgumentException("Target does not match target type");
        }

        if (targetEntity == null) {
            throw new IllegalArgumentException("Enrichment data is missing " + targetFieldName);
        }

        return null;
    }

    /**
     * Determines the VNF Name target entity.
     *
     * @return a future to determine the target entity, or {@code null} if the entity has
     *         already been determined
     */
    @SuppressWarnings("unchecked")
    private CompletableFuture<OperationOutcome> detmVnfName() {
        // if the onset is enriched with the vnf-id, we don't need an A&AI response
        targetEntity = eventContext.getEnrichment().get(GENERIC_VNF_VNF_ID);
        if (targetEntity != null) {
            return null;
        }

        // vnf-id was not in the onset - obtain it via the custom query

        // @formatter:off
        ControlLoopOperationParams cqparams = params.toBuilder()
                        .actor(AaiConstants.ACTOR_NAME)
                        .operation(AaiCqResponse.OPERATION)
                        .targetEntity("")
                        .build();
        // @formatter:on

        // perform custom query and then extract the VNF ID from it
        return taskUtil.sequence(() -> eventContext.obtain(AaiCqResponse.CONTEXT_KEY, cqparams),
                        this::extractVnfFromCq);
    }

    /**
     * Extracts the VNF Name target entity from the custom query data.
     *
     * @return {@code null}
     */
    private CompletableFuture<OperationOutcome> extractVnfFromCq() {
        // already have the CQ data
        AaiCqResponse cq = eventContext.getProperty(AaiCqResponse.CONTEXT_KEY);
        if (cq.getDefaultGenericVnf() == null) {
            throw new IllegalArgumentException("No vnf-id found");
        }

        targetEntity = cq.getDefaultGenericVnf().getVnfId();
        if (targetEntity == null) {
            throw new IllegalArgumentException("No vnf-id found");
        }

        return null;
    }
}
