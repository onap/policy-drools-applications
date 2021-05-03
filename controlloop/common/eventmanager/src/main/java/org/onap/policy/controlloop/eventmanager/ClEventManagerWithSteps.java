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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.drools.core.WorkingMemory;
import org.kie.api.runtime.rule.FactHandle;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.ControlLoopResponse;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.actorserviceprovider.OperationFinalResult;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.OperationResult;
import org.onap.policy.controlloop.actorserviceprovider.TargetType;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.drools.domain.models.operational.ActorOperation;
import org.onap.policy.drools.domain.models.operational.Operation;
import org.onap.policy.drools.domain.models.operational.OperationalTarget;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for a single control loop event. Once this has been created, the event can be
 * retracted from working memory. Processing progresses through each policy, which
 * involves at least one step. As a step is processed, additional preprocessor steps may
 * be pushed onto the queue (e.g., locks, A&AI queries, guards).
 */
@ToString(onlyExplicitlyIncluded = true)
public abstract class ClEventManagerWithSteps<T extends Step> extends ControlLoopEventManager implements StepContext {

    private static final Logger logger = LoggerFactory.getLogger(ClEventManagerWithSteps.class);
    private static final long serialVersionUID = -1216568161322872641L;

    /**
     * Maximum number of steps, for a single policy, allowed in the queue at a time. This
     * prevents an infinite loop occurring with calls to {@link #loadPreprocessorSteps()}.
     */
    public static final int MAX_STEPS = 30;

    public enum State {
        LOAD_POLICY, POLICY_LOADED, AWAITING_OUTCOME, DONE
    }

    /**
     * Request ID, as a String.
     */
    @Getter
    private final String requestIdStr;

    @Getter
    @Setter
    private State state;

    /**
     * {@code True} if the event has been accepted (i.e., an "ACTIVE" notification has
     * been delivered), {@code false} otherwise.
     */
    @Getter
    @Setter
    private boolean accepted;

    /**
     * Queue of steps waiting to be performed.
     */
    @Getter
    private final transient Deque<T> steps = new ArrayDeque<>(6);

    /**
     * Number of attempts, so far, for the current step.
     */
    @Getter
    private int attempts;

    /**
     * Policy currently being processed.
     */
    private Operation policy;

    /**
     * Result of the last policy operation. This is just a place where the rules can store
     * the value for passing to {@link #loadNextPolicy()}.
     */
    @Getter
    @Setter
    private OperationResult result = OperationResult.SUCCESS;

    @Getter
    @ToString.Include
    private int numOnsets = 1;
    @Getter
    @ToString.Include
    private int numAbatements = 0;

    /**
     * Full history of operations that have been processed by the rules. This includes the
     * items in {@link #partialHistory}.
     */
    @Getter
    private final transient Deque<OperationOutcome2> fullHistory = new LinkedList<>();

    /**
     * History of operations that have been processed by the rules for the current policy.
     * When a step is started, its "start" outcome is added. However, once it completes,
     * its "start" outcome is removed and the "completed" outcome is added.
     */
    @Getter
    private final transient Deque<OperationOutcome2> partialHistory = new LinkedList<>();

    @Getter
    private OperationFinalResult finalResult = null;

    /**
     * Message to be placed into the final notification. Typically used when something
     * causes processing to abort.
     */
    @Getter
    private String finalMessage = null;

    private final transient WorkingMemory workMem;
    private transient FactHandle factHandle;


    /**
     * Constructs the object.
     *
     * @param params control loop parameters
     * @param requestId event request ID
     * @param workMem working memory to update if this changes
     * @throws ControlLoopException if the event is invalid or if a YAML processor cannot
     *         be created
     */
    public ClEventManagerWithSteps(ControlLoopParams params, UUID requestId, WorkingMemory workMem)
                    throws ControlLoopException {

        super(params, requestId);

        if (requestId == null) {
            throw new ControlLoopException("No request ID");
        }

        this.workMem = workMem;
        this.requestIdStr = getRequestId().toString();
    }

    @Override
    public void destroy() {
        for (T step : getSteps()) {
            step.cancel();
        }

        super.destroy();
    }

    /**
     * Starts the manager and loads the first policy.
     *
     * @throws ControlLoopException if the processor cannot get a policy
     */
    public void start() throws ControlLoopException {
        if (!isActive()) {
            throw new IllegalStateException("manager is no longer active");
        }

        if ((factHandle = workMem.getFactHandle(this)) == null) {
            throw new IllegalStateException("manager is not in working memory");
        }

        if (!getSteps().isEmpty()) {
            throw new IllegalStateException("manager already started");
        }

        loadPolicy();
    }

    /**
     * Indicates that processing has been aborted.
     *
     * @param finalState final state
     * @param finalResult final result
     * @param finalMessage final message
     */
    public void abort(@NonNull State finalState, OperationFinalResult finalResult, String finalMessage) {
        this.state = finalState;
        this.finalResult = finalResult;
        this.finalMessage = finalMessage;
    }

    /**
     * Loads the next policy.
     *
     * @param lastResult result from the last policy
     *
     * @throws ControlLoopException if the processor cannot get a policy
     */
    public void loadNextPolicy(@NonNull OperationResult lastResult) throws ControlLoopException {
        getProcessor().nextPolicyForResult(lastResult);
        loadPolicy();
    }

    /**
     * Loads the current policy.
     *
     * @throws ControlLoopException if the processor cannot get a policy
     */
    private void loadPolicy() throws ControlLoopException {
        partialHistory.clear();

        if ((finalResult = getProcessor().checkIsCurrentPolicyFinal()) != null) {
            // final policy - nothing more to do
            return;
        }

        policy = getProcessor().getCurrentPolicy();

        ActorOperation actor = policy.getActorOperation();

        OperationalTarget target = actor.getTarget();
        String targetType = (target != null ? target.getTargetType() : null);
        Map<String, String> entityIds = (target != null ? target.getEntityIds() : null);

        // convert policy payload from Map<String,String> to Map<String,Object>
        Map<String, Object> payload = new LinkedHashMap<>();
        if (actor.getPayload() != null) {
            payload.putAll(actor.getPayload());
        }

        // @formatter:off
        ControlLoopOperationParams params = ControlLoopOperationParams.builder()
                        .actorService(getActorService())
                        .actor(actor.getActor())
                        .operation(actor.getOperation())
                        .requestId(getRequestId())
                        .executor(getExecutor())
                        .retry(policy.getRetries())
                        .timeoutSec(policy.getTimeout())
                        .targetType(TargetType.toTargetType(targetType))
                        .targetEntityIds(entityIds)
                        .payload(payload)
                        .startCallback(this::onStart)
                        .completeCallback(this::onComplete)
                        .build();
        // @formatter:on

        // load the policy's operation
        getSteps().add(makeStep(params));
    }

    /**
     * Makes the step associated with the given parameters.
     *
     * @param params operation's parameters
     * @return a new step
     */
    protected abstract T makeStep(ControlLoopOperationParams params);

    /**
     * Loads the preprocessor steps needed by the step that's at the front of the queue.
     */
    public void loadPreprocessorSteps() {
        if (getSteps().size() >= MAX_STEPS) {
            throw new IllegalStateException("too many steps");
        }

        // initialize the step so we can query its properties
        getSteps().peek().init();
    }

    /**
     * Executes the first step in the queue.
     *
     * @return {@code true} if the step was started, {@code false} if it is no longer
     *         needed (or if the queue is empty)
     */
    public boolean executeStep() {
        attempts = 0;

        T step = getSteps().peek();
        if (step == null) {
            return false;
        }

        return step.start(getEndTimeMs() - System.currentTimeMillis());
    }

    /**
     * Discards the current step, if any.
     */
    public void nextStep() {
        getSteps().poll();
    }

    /**
     * Increments the number of attempts.
     */
    public void bumpAttempts() {
        ++attempts;
    }

    /**
     * Determines if the TOSCA should be aborted due to the given outcome.
     *
     * @param outcome outcome to examine
     * @return {@code true} if the TOSCA should be aborted, {@code false} otherwise
     */
    public boolean isAbort(OperationOutcome outcome) {
        return (outcome.getResult() != OperationResult.SUCCESS);
    }

    /**
     * Adds the outcome to the history.
     *
     * @param outcome outcome to add
     */
    public void addToHistory(OperationOutcome outcome) {
        OperationOutcome2 last = partialHistory.peekLast();

        if (last != null && last.getOutcome().getEnd() == null
                        && last.getOutcome().isFor(outcome.getActor(), outcome.getOperation())) {
            // last item was a "start" - remove it
            partialHistory.removeLast();

            if (fullHistory.peekLast() == last) {
                fullHistory.removeLast();
            }
        }

        OperationOutcome2 outcome2 = makeOperationOutcome2(outcome);
        partialHistory.add(outcome2);
        fullHistory.add(outcome2);
    }

    /**
     * Makes a notification message for the current operation.
     *
     * @return a new notification
     */
    public VirtualControlLoopNotification makeNotification() {
        VirtualControlLoopNotification notif = new VirtualControlLoopNotification();
        populateNotification(notif);

        if (finalResult != null) {
            return notif;
        }

        OperationOutcome2 last = partialHistory.peekLast();
        if (last == null) {
            return notif;
        }

        notif.setMessage(last.getClOperation().toHistory());
        notif.setHistory(partialHistory.stream().map(OperationOutcome2::getClOperation).collect(Collectors.toList()));

        return notif;
    }

    /**
     * Populates a notification structure.
     *
     * @param notif the notification to populate
     */
    protected void populateNotification(VirtualControlLoopNotification notif) {
        notif.setNotification(ControlLoopNotificationType.OPERATION);
        notif.setFrom("policy");
        notif.setPolicyVersion(getPolicyVersion());
    }

    /**
     * Delivers a notification to a topic.
     *
     * @param sinkName name of the topic sink
     * @param notification notification to be published, or {@code null} if nothing is to
     *        be published
     * @param notificationType type of notification, used when logging error messages
     * @param ruleName name of the rule doing the publishing
     */
    public <N> void deliver(String sinkName, N notification, String notificationType, String ruleName) {
        try {
            if (notification != null) {
                getPolicyEngineManager().deliver(sinkName, notification);
            }

        } catch (RuntimeException e) {
            logger.warn("{}: {}.{}: manager={} exception publishing {}", getClosedLoopControlName(), getPolicyName(),
                            ruleName, this, notificationType, e);
        }
    }

    /**
     * Get the last operation, as a message.
     *
     * @return the last operation, as a message
     */
    public String getOperationMessage() {
        OperationOutcome2 last = fullHistory.peekLast();
        return (last == null ? null : last.getClOperation().toMessage());
    }

    /**
     * Makes a control loop response.
     *
     * @param outcome operation outcome
     * @return a new control loop response, or {@code null} if none is required
     */
    public ControlLoopResponse makeControlLoopResponse(OperationOutcome outcome) {
        ControlLoopResponse clRsp = new ControlLoopResponse();
        clRsp.setFrom(outcome.getActor());

        return clRsp;
    }

    protected int bumpOffsets() {
        return numOnsets++;
    }

    protected int bumpAbatements() {
        return numAbatements++;
    }

    @Override
    public void onStart(OperationOutcome outcome) {
        super.onStart(outcome);
        workMem.update(factHandle, this);
    }

    @Override
    public void onComplete(OperationOutcome outcome) {
        super.onComplete(outcome);
        workMem.update(factHandle, this);
    }

    @Getter
    @ToString
    public class OperationOutcome2 {
        private final int attempt;
        private final OperationOutcome outcome;
        private final ControlLoopOperation clOperation;

        /**
         * Constructs the object.
         *
         * @param outcome outcome of the operation
         */
        public OperationOutcome2(OperationOutcome outcome) {
            this.outcome = outcome;
            this.attempt = attempts;

            clOperation = outcome.toControlLoopOperation();

            // TODO encode()?
            OperationalTarget target = policy.getActorOperation().getTarget();
            String targetStr = (target != null ? target.toString() : null);
            clOperation.setTarget(targetStr);

            if (outcome.getEnd() == null) {
                clOperation.setOutcome("Started");
            } else if (clOperation.getOutcome() == null) {
                clOperation.setOutcome("");
            }
        }
    }

    protected OperationOutcome2 makeOperationOutcome2(OperationOutcome outcome) {
        return new OperationOutcome2(outcome);
    }

    // these following methods may be overridden by junit tests

    protected PolicyEngine getPolicyEngineManager() {
        return PolicyEngineConstants.getManager();
    }
}
