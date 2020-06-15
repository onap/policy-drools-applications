/*-
 * ============LICENSE_START=======================================================
 * m2/base
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

package org.onap.policy.m2.base;

import java.io.Serializable;
import java.lang.Class;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.UUID;

import lombok.Getter;
import org.drools.core.WorkingMemory;
import org.kie.api.runtime.rule.FactHandle;

import org.onap.policy.controlloop.ControlLoopEvent;
import org.onap.policy.controlloop.ControlLoopNotification;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.controlloop.policy.FinalResult;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Each instance of this class corresonds to a transaction that is in
 * progress. While active, it resides within Drools memory.
 */

public class Transaction implements Serializable {

    private static Logger logger = LoggerFactory.getLogger(Transaction.class);

    // This table maps 'actor' names to objects implementing the
    // 'Actor' interface. 'ServiceLoader' is used to locate and create
    // these objects, and populate the table.
    private static Map<String,Actor> nameToActor = new HashMap<>();

    static {
        // use 'ServiceLoader' to locate all of the 'Actor' implementations
        for (Actor actor :
                ServiceLoader.load(Actor.class, Actor.class.getClassLoader())) {
            logger.debug("Actor: {}, {}", actor.getName(), actor.getClass());
            nameToActor.put(actor.getName(), actor);
        }
    }

    private static final long serialVersionUID = 4389211793631707360L;

    // Drools working memory containing this transaction
    @Getter
    private transient WorkingMemory workingMemory;

    // a service-identifier specified on the associated onset message
    @Getter
    private String closedLoopControlName;

    // identifies this transaction
    @Getter
    private UUID requestId;

    // the decoded YAML file for the policy
    private ControlLoopPolicy policy;

    // the initial incoming event
    private ControlLoopEvent onset = null;

    // operations specific to the type of 'event'
    private OnsetAdapter onsetAdapter = null;

    // the current (or most recent) policy in effect
    @Getter
    private Policy currentPolicy = null;

    // the operation currently in progress
    @Getter
    private Operation currentOperation = null;

    // a history entry being constructed that is associated with the
    // currently running operation
    private ControlLoopOperation histEntry = null;

    // a list of completed history entries
    @Getter
    private List<ControlLoopOperation> history = new LinkedList<>();

    // when the transaction completes, this is the final transaction result
    @Getter
    private FinalResult finalResult = null;

    //message, if any, associated with the result of this operation
    private String message = null;

    // this table maps a class name into the associated adjunct
    private Map<Class, Adjunct> adjuncts = new HashMap<>();

    /**
     * Constructor - initialize a 'Transaction' instance
     * (typically invoked from 'Drools').
     *
     * @param workingMemory Drools working memory containing this Transaction
     * @param closedLoopControlName a string identifying the associated service
     * @param requestId uniquely identifies this transaction
     * @param policy decoded YAML file containing the policy
     */
    public Transaction(
        WorkingMemory workingMemory,
        String closedLoopControlName,
        UUID requestId,
        ControlLoopPolicy policy) {

        logger.info("Transaction constructor");
        this.workingMemory = workingMemory;
        this.closedLoopControlName = closedLoopControlName;
        this.requestId = requestId;
        this.policy = policy;
    }

    /**
     * Return a string indicating the current state of this transaction.
     * If there is an operation in progress, the state indicates the operation
     * state. Otherwise, the state is 'COMPLETE'.
     *
     * @return a string indicating the current state of this transaction
     */
    public String getState() {
        return currentOperation == null
            ? "COMPLETE" : currentOperation.getState();
    }

    /**
     * Return 'true' if the transaction has completed, and the final result
     * indicates failure.
     *
     * @return 'true' if the transaction has completed, and the final result
     *     indicates failure
     */
    public boolean finalResultFailure() {
        return FinalResult.FINAL_SUCCESS != finalResult
               && FinalResult.FINAL_OPENLOOP != finalResult
               && finalResult != null;
    }

    /**
     * Return the overall policy timeout value as a String that can be used
     * in a Drools timer.
     *
     * @return the overall policy timeout value as a String that can be used
     *     in a Drools timer
     */
    public String getTimeout() {
        return String.valueOf(policy.getControlLoop().getTimeout()) + "s";
    }

    /**
     * Return the current operation timeout value as a String that can be used
     * in a Drools timer.
     *
     * @return the current operation timeout value as a String that can be used
     *     in a Drools timer
     */
    public String getOperationTimeout() {
        return String.valueOf(currentPolicy.getTimeout()) + "s";
    }

    /**
     * Let Drools know the transaction has been modified.
     *
     * <p>It is not necessary for Java code to call this method when an incoming
     * message is received for an operation, or an operation timeout occurs --
     * the Drools code has been written with the assumption that the transaction
     * is modified in these cases. Instead, this method should be called when
     * some type of internal occurrence results in a state change, such as when
     * an operation acquires a lock after initially being blocked.
     */
    public void modify() {
        FactHandle handle = workingMemory.getFactHandle(this);
        if (handle != null) {
            workingMemory.update(handle, this);
        }
    }

    /**
     * Set the initial 'onset' event that started this transaction.
     *
     * @param event the initial 'onset' event
     */
    public void setControlLoopEvent(ControlLoopEvent event) {
        if (onset != null) {
            logger.error("'Transaction' received unexpected event");
            return;
        }

        onset = event;

        // fetch associated 'OnsetAdapter'
        onsetAdapter = OnsetAdapter.get(onset);

        // check trigger policy type
        if (isOpenLoop(policy.getControlLoop().getTrigger_policy())) {
            // no operation is needed for open loops
            finalResult = FinalResult.FINAL_OPENLOOP;
            modify();
        } else {
            // fetch current policy
            setPolicyId(policy.getControlLoop().getTrigger_policy());
        }
    }

    /**
     * Validates the onset by ensuring fields that are required
     * for processing are included in the onset. The fields needed
     * include the requestId, targetType, and target.
     *
     * @param onset the initial message that triggers processing
     */
    public boolean isControlLoopEventValid(ControlLoopEvent onset) {
        if (onset.getRequestId() == null) {
            this.message = "No requestID";
            return false;
        } else if (onset.getTargetType() == null) {
            this.message = "No targetType";
            return false;
        } else if (onset.getTarget() == null || onset.getTarget().isEmpty()) {
            this.message = "No target field";
            return false;
        }
        return true;
    }

    /**
     * Create a 'ControlLoopNotification' from the specified event. Note thet
     * the type of the initial 'onset' event is used to determine the type
     * of the 'ControlLoopNotification', rather than the event passed to the
     * method.
     *
     * @param event the event used to generate the notification
     *     (if 'null' is passed, the 'onset' event is used)
     * @return the created 'ControlLoopNotification' (or subclass) instance
     */
    public ControlLoopNotification getNotification(ControlLoopEvent event) {
        ControlLoopNotification notification =
            onsetAdapter.createNotification(event == null ? this.onset : event);

        // include entire history
        notification.setHistory(new ArrayList<>(history));

        return notification;
    }

    /**
     * This method is called when additional incoming messages are received
     * for the transaction. Messages are routed to the current operation,
     * any results are processed, and a notification may be returned to
     * the caller.
     *
     * @param object an incoming message, which should be meaningful to the
     *     operation currently in progress
     * @return a notification message if the operation completed,
     *     or 'null' if it is still in progress
     */
    public ControlLoopNotification incomingMessage(Object object) {
        ControlLoopNotification notification = null;
        if (currentOperation != null) {
            currentOperation.incomingMessage(object);
            notification = processResult(currentOperation.getResult());
        } else {
            logger.error("'Transaction' received unexpected message: {}", object);
        }
        return notification;
    }

    /**
     * This method is called from Drools when the current operation times out.
     *
     * @return a notification message if there is an operation in progress,
     *     or 'null' if not
     */
    public ControlLoopNotification timeout() {
        ControlLoopNotification notification = null;
        if (currentOperation != null) {
            // notify the current operation
            currentOperation.timeout();

            // process the timeout within the transaction
            notification = processResult(currentOperation.getResult());
        } else {
            logger.error("'Transaction' received unexpected timeout");
        }
        return notification;
    }

    /**
     *  This method is called from Drools during a control loop timeout
     *  to ensure the correct final notification is sent.
     */
    public void clTimeout() {
        this.finalResult = FinalResult.FINAL_FAILURE_TIMEOUT;
        message = "Control Loop timed out";
        currentOperation = null;
    }

    /**
     * This method is called from Drools to generate a notification message
     * when an operation is started.
     *
     * @return an initial notification message if there is an operation in
     *     progress, or 'null' if not
     */
    public ControlLoopNotification initialOperationNotification() {
        if (currentOperation == null || histEntry == null) {
            return null;
        }

        ControlLoopNotification notification =
            onsetAdapter.createNotification(onset);
        notification.setNotification(ControlLoopNotificationType.OPERATION);
        notification.setMessage(histEntry.toHistory());
        notification.setHistory(new LinkedList<>());
        for (ControlLoopOperation clo : history) {
            if (histEntry.getOperation().equals(clo.getOperation())
                    && histEntry.getActor().equals(clo.getActor())) {
                notification.getHistory().add(clo);
            }
        }
        return notification;
    }

    /**
     * Return a final notification message for the entire transaction.
     *
     * @return a final notification message for the entire transaction,
     *     or 'null' if we don't have a final result yet
     */
    public ControlLoopNotification finalNotification() {
        if (finalResult == null) {
            return null;
        }

        ControlLoopNotification notification =
            onsetAdapter.createNotification(onset);
        switch (finalResult) {
            case FINAL_SUCCESS:
                notification.setNotification(
                    ControlLoopNotificationType.FINAL_SUCCESS);
                break;
            case FINAL_OPENLOOP:
                notification.setNotification(
                    ControlLoopNotificationType.FINAL_OPENLOOP);
                break;
            default:
                notification.setNotification(
                    ControlLoopNotificationType.FINAL_FAILURE);
                notification.setMessage(this.message);
                break;
        }
        notification.setHistory(history);
        return notification;
    }

    /**
     * Return a 'ControlLoopNotification' instance describing the current operation error.
     *
     * @return a 'ControlLoopNotification' instance describing the current operation error
     */
    public ControlLoopNotification processError() {
        ControlLoopNotification notification = null;
        if (currentOperation != null) {
            // process the error within the transaction
            notification = processResult(currentOperation.getResult());
        }
        return notification;
    }

    /**
     * Update the state of the transaction based upon the result of an operation.
     *
     * @param result if not 'null', this is the result of the current operation
     *     (if 'null', the operation is still in progress,
     *     and no changes are made)
     * @return if not 'null', this is a notification message that should be
     *     sent to RUBY
     */
    private ControlLoopNotification processResult(PolicyResult result) {
        if (result == null) {
            modify();
            return null;
        }
        String nextPolicy = null;

        ControlLoopOperation saveHistEntry = histEntry;
        completeHistEntry(result);

        final ControlLoopNotification notification = processResultHistEntry(saveHistEntry, result);

        // If there is a message from the operation then we set it to be
        // used by the control loop notifications
        message = currentOperation.getMessage();

        // set the value 'nextPolicy' based upon the result of the operation
        switch (result) {
            case SUCCESS:
                nextPolicy = currentPolicy.getSuccess();
                break;

            case FAILURE:
                nextPolicy = processResult_Failure();
                break;

            case FAILURE_TIMEOUT:
                nextPolicy = currentPolicy.getFailure_timeout();
                message = "Operation timed out";
                break;

            case FAILURE_RETRIES:
                nextPolicy = currentPolicy.getFailure_retries();
                message = "Control Loop reached failure retry limit";
                break;

            case FAILURE_EXCEPTION:
                nextPolicy = currentPolicy.getFailure_exception();
                break;

            case FAILURE_GUARD:
                nextPolicy = currentPolicy.getFailure_guard();
                break;

            default:
                break;
        }

        if (nextPolicy != null) {
            finalResult = FinalResult.toResult(nextPolicy);
            if (finalResult == null) {
                // it must be the next state
                logger.debug("advancing to next operation");
                setPolicyId(nextPolicy);
            } else {
                logger.debug("moving to COMPLETE state");
                currentOperation = null;
            }
        } else {
            logger.debug("doing retry with current actor");
        }

        modify();
        return notification;
    }

    // returns a notification message based on the history entry
    private ControlLoopNotification processResultHistEntry(ControlLoopOperation hist, PolicyResult result) {
        if (hist == null) {
            return null;
        }

        // generate notification, containing operation history
        ControlLoopNotification notification = onsetAdapter.createNotification(onset);
        notification.setNotification(
            result == PolicyResult.SUCCESS
            ? ControlLoopNotificationType.OPERATION_SUCCESS
            : ControlLoopNotificationType.OPERATION_FAILURE);
        notification.setMessage(hist.toHistory());

        // include the subset of history that pertains to this
        // actor and operation
        notification.setHistory(new LinkedList<>());
        for (ControlLoopOperation clo : history) {
            if (hist.getOperation().equals(clo.getOperation())
                    && hist.getActor().equals(clo.getActor())) {
                notification.getHistory().add(clo);
            }
        }

        return notification;
    }

    // returns the next policy if the current operation fails
    private String processResult_Failure() {
        String nextPolicy = null;
        int attempt = currentOperation.getAttempt();
        if (attempt <= currentPolicy.getRetry()) {
            // operation failed, but there are retries left
            Actor actor = nameToActor.get(currentPolicy.getActor());
            if (actor != null) {
                attempt += 1;
                logger.debug("found Actor, attempt " + attempt);
                currentOperation =
                    actor.createOperation(this, currentPolicy, onset, attempt);
                createHistEntry();
            } else {
                logger.error("'Transaction' can't find actor "
                             + currentPolicy.getActor());
            }
        } else {
            // operation failed, and no retries (or no retries left)
            nextPolicy = (attempt == 1
                ? currentPolicy.getFailure()
                : currentPolicy.getFailure_retries());
            logger.debug("moving to policy " + nextPolicy);
        }
        return nextPolicy;
    }

    /**
     * Create a history entry at the beginning of an operation, and store it
     * in the 'histEntry' instance variable.
     */
    private void createHistEntry() {
        histEntry = new ControlLoopOperation();
        histEntry.setActor(currentPolicy.getActor());
        histEntry.setOperation(currentPolicy.getRecipe());
        histEntry.setTarget(currentPolicy.getTarget().toString());
        histEntry.setSubRequestId(String.valueOf(currentOperation.getAttempt()));

        // histEntry.end - we will set this one later
        // histEntry.outcome - we will set this one later
        // histEntry.message - we will set this one later
    }

    /**
     * Finish up the history entry at the end of an operation, and add it
     * to the history list.
     *
     * @param result this is the result of the operation, which can't be 'null'
     */
    private void completeHistEntry(PolicyResult result) {
        if (histEntry == null) {
            return;
        }

        // append current entry to history
        histEntry.setEnd(Instant.now());
        histEntry.setOutcome(result.toString());
        histEntry.setMessage(currentOperation.getMessage());
        history.add(histEntry);

        // give current operation a chance to act on it
        currentOperation.histEntryCompleted(histEntry);
        logger.debug("histEntry = {}", histEntry);
        histEntry = null;
    }

    /**
     * Look up the identifier for the next policy, and prepare to start that
     * operation.
     *
     * @param id this is the identifier associated with the policy
     */
    private void setPolicyId(String id) {
        currentPolicy = null;
        currentOperation = null;

        // search through the policies for a matching 'id'
        for (Policy tmp : policy.getPolicies()) {
            if (id.equals(tmp.getId())) {
                // found a match
                currentPolicy = tmp;
                break;
            }
        }

        if (currentPolicy != null) {
            // locate the 'Actor' associated with 'currentPolicy'
            Actor actor = nameToActor.get(currentPolicy.getActor());
            if (actor != null) {
                // found the associated 'Actor' instance
                currentOperation =
                    actor.createOperation(this, currentPolicy, onset, 1);
                createHistEntry();
            } else {
                logger.error("'Transaction' can't find actor "
                             + currentPolicy.getActor());
            }
        } else {
            logger.error("Transaction' can't find policy " + id);
        }

        if (currentOperation == null) {

            // either we couldn't find the actor or the operation --
            // the transaction fails
            finalResult = FinalResult.FINAL_FAILURE;
        }
    }

    private boolean isOpenLoop(String policyId) {
        return FinalResult.FINAL_OPENLOOP.name().equalsIgnoreCase(policyId);
    }

    /**
     * This method sets the message for a control loop notification
     * in the case where a custom message wants to be sent due to
     * error processing, etc.
     *
     * @param message the message to be set for the control loop notification
     */
    public void setNotificationMessage(String message) {
        this.message = message;
    }

    /**
     * Return the notification message of this transaction.
     *
     * @return the notification message of this transaction
     */
    public String getNotificationMessage() {
        return this.message;
    }

    /* ============================================================ */

    /**
     * Subclasses of 'Adjunct' provide data and methods to support one or
     * more Actors/Operations, but are stored within the 'Transaction'
     * instance.
     */
    public static interface Adjunct extends Serializable {
        /**
         * Called when an adjunct is automatically created as a result of
         * a 'getAdjunct' call.
         *
         * @param transaction the transaction containing the adjunct
         */
        public default void init(Transaction transaction) {
        }

        /**
         * Called for each adjunct when the transaction completes, and is
         * removed from Drools memory. Any adjunct-specific cleanup can be
         * done at this point (e.g. freeing locks).
         */
        public default void cleanup(Transaction transaction) {
        }
    }

    /**
     * This is a method of class 'Transaction', and returns an adjunct of
     * the specified class (it is created if it doesn't exist).
     *
     * @param clazz this is the class of the adjunct
     * @return an adjunct of the specified class ('null' may be returned if
     *     the 'newInstance' method is unable to create the adjunct)
     */
    public <T extends Adjunct> T getAdjunct(final Class<T> clazz) {
        return clazz.cast(adjuncts.computeIfAbsent(clazz, cl -> {
            T adjunct = null;
            try {
                // create the adjunct (may trigger an exception)
                adjunct = clazz.newInstance();

                // initialize the adjunct (may also trigger an exception */
                adjunct.init(Transaction.this);
            } catch (Exception e) {
                logger.error("Transaction can't create adjunct of {}", cl, e);
            }
            return adjunct;
        }));
    }

    /**
     * Explicitly create an adjunct -- this is useful when the adjunct
     * initialization requires that some parameters be passed.
     *
     * @param adjunct this is the adjunct to insert into the table
     * @return 'true' if successful
     *     ('false' is returned if an adjunct with this class already exists)
     */
    public boolean putAdjunct(Adjunct adjunct) {
        return adjuncts.putIfAbsent(adjunct.getClass(), adjunct) == null;
    }

    /**
     * This method needs to be called when the transaction completes, which
     * is typically right after it is removed from Drools memory.
     */
    public void cleanup() {
        // create a list containing all of the adjuncts (in no particular order)
        List<Adjunct> values;
        synchronized (adjuncts) {
            values = new LinkedList<>(adjuncts.values());
        }

        // iterate over the list
        for (Adjunct a : values) {
            try {
                // call the 'cleanup' method on the adjunct
                a.cleanup(this);
            } catch (Exception e) {
                logger.error("Transaction.cleanup exception", e);
            }
        }
    }
}
