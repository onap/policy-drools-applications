/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

import java.util.Deque;
import java.util.LinkedList;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.ToString;
import org.drools.core.WorkingMemory;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.ControlLoopResponse;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.OperationResult;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.drools.domain.models.operational.OperationalTarget;

/**
 * Manager for a single control loop event, with operation outcomes.
 */
public abstract class ClEventManagerWithOutcome<T extends Step> extends ClEventManagerWithSteps<T>
                implements StepContext {

    private static final long serialVersionUID = -1216568161322872641L;

    /**
     * Number of attempts, so far, for the current step.
     */
    @Getter
    private int attempts;

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


    /**
     * Constructs the object.
     *
     * @param services services the manager should use when processing the event
     * @param params control loop parameters
     * @param requestId event request ID
     * @param workMem working memory to update if this changes
     * @throws ControlLoopException if the event is invalid or if a YAML processor cannot
     *         be created
     */
    protected ClEventManagerWithOutcome(EventManagerServices services, ControlLoopParams params, UUID requestId,
                    WorkingMemory workMem) throws ControlLoopException {

        super(services, params, requestId, workMem);
    }

    @Override
    protected void loadPolicy() throws ControlLoopException {
        partialHistory.clear();
        super.loadPolicy();
    }

    @Override
    public boolean executeStep() {
        attempts = 0;
        return super.executeStep();
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

        var outcome2 = makeOperationOutcome2(outcome);
        partialHistory.add(outcome2);
        fullHistory.add(outcome2);
    }

    /**
     * Makes a notification message for the current operation.
     *
     * @return a new notification
     */
    public VirtualControlLoopNotification makeNotification() {
        var notif = new VirtualControlLoopNotification();
        populateNotification(notif);

        if (getFinalResult() != null) {
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
        var clRsp = new ControlLoopResponse();
        clRsp.setFrom(outcome.getActor());

        return clRsp;
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
            OperationalTarget target = getPolicy().getActorOperation().getTarget();
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
}
