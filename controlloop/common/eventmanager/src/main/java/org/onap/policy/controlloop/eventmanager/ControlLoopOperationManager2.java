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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.onap.policy.common.utils.jpa.EntityMgrCloser;
import org.onap.policy.common.utils.jpa.EntityTransCloser;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.controlloop.ControlLoopEventContext;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.database.operationshistory.Dbao;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.guard.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages a single Operation.
 */
@ToString(onlyExplicitlyIncluded = true)
public class ControlLoopOperationManager2 implements Serializable {
    private static final long serialVersionUID = -3773199283624595410L;
    private static final Logger logger = LoggerFactory.getLogger(ControlLoopOperationManager2.class);
    private static final String LOCK_ACTOR = "LOCK";
    private static final String GUARD_ACTOR = "GUARD";
    private static final String FINAL_FAILURE_ACTOR = "-final failure-";

    public enum State {
        STARTING, LOCK_REQUESTED, LOCK_GRANTED, LOCK_DENIED, GUARD_STARTED, GUARD_PERMITTED, GUARD_DENIED, OPERATION_STARTED, OPERATION_SUCCESS, OPERATION_FAILURE, FINAL_FAILURE, UNKNOWN
    }

    private final ControlLoopEventContext context;
    public final Policy policy;

    /**
     * Invoked whenever "this" changes due to callbacks from the Actor.
     */
    private final transient Consumer<ControlLoopOperationManager2> listener;

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
    private final Deque<Operation> operationHistory = new LinkedList<>();

    /**
     * Queue of outcomes yet to be processed. Outcomes are added to this each time the
     * "start" or "complete" callback is invoked.
     */
    private final transient Queue<OperationOutcome> outcomes = new LinkedList<>();

    // values extracted from the policy
    private final String actor;
    private final String operation;

    /**
     * Used to cancel the running operation.
     */
    private transient CompletableFuture<OperationOutcome> future = null;


    /**
     * Construct an instance.
     *
     * @param context event context
     * @param policy operation's policy
     * @param listener callback to invoke when this changes
     */
    public ControlLoopOperationManager2(ControlLoopEventContext context, Policy policy,
                    Consumer<ControlLoopOperationManager2> listener) {

        this.context = context;
        this.policy = policy;
        this.listener = listener;

        this.requestId = "" + context.getEvent().getRequestId();
        this.policyId = "" + policy.getId();
        this.actor = policy.getActor();
        this.operation = policy.getRecipe();
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
     * Start the operation.
     */
    public synchronized void start() {
        // @formatter:off
        ControlLoopOperationParams params = ControlLoopOperationParams.builder()
                        .actor(actor)
                        .operation(operation)
                        .context(context)
                        .payload(new LinkedHashMap<>(policy.getPayload()))
                        .retry(policy.getRetry())
                        .timeoutSec(policy.getTimeout())
                        .target(policy.getTarget())
                        .targetEntity(context.getEvent().getTarget())
                        .startCallback(this::onResponse)
                        .completeCallback(this::onResponse)
                        .actorService(PolicyEngineConstants.getManager().getActorService())
                        .build();
        // @formatter:on

        future = params.start();

        // generate a "final failure" if the outcome is a failure
        future.thenAcceptAsync(outcome -> {
            if (outcome.getResult() != PolicyResult.SUCCESS && actor.equals(outcome.getActor())
                            && operation.equals(outcome.getOperation())) {
                outcome.setActor(FINAL_FAILURE_ACTOR);
                onResponse(outcome);
            }
        }, params.getExecutor());
    }

    /**
     * Cancels the operation.
     */
    public void cancel() {
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * Handles responses provided via the "start" or "complete" callback.
     *
     * @param outcome outcome provided to the callback
     */
    private synchronized void onResponse(OperationOutcome outcome) {
        String outcomeActor = outcome.getActor();

        switch (outcomeActor) {
            case FINAL_FAILURE_ACTOR:
            case LOCK_ACTOR:
            case GUARD_ACTOR:
                break;

            default:
                if (!actor.equals(outcomeActor) || !operation.equals(outcome.getOperation())) {
                    return;
                }
                break;
        }

        // make a copy, as the original may change as the operation progresses
        OperationOutcome outcome2 = new OperationOutcome(outcome);

        // add the copy to the queue
        outcomes.add(outcome2);

        if (outcomes.size() > 1) {
            // not the first in the queue - nothing else to do
            return;
        }

        // this is the first outcome - process it
        processOutcome();

        listener.accept(this);
    }

    /**
     * Looks for the next step in the queue. May change the {@link #state}, but does not
     * invoke the {@link #listener}.
     *
     * @return {@code true} if more responses are expected, {@code false} otherwise
     */
    public synchronized boolean nextStep() {
        if (outcomes.isEmpty()) {
            return expectingMore();
        }

        // first item has been processed, remove it
        outcomes.remove();
        if (outcomes.isEmpty()) {
            return expectingMore();
        }

        // have a new "first" item - process it
        processOutcome();
        return expectingMore();
    }

    /**
     * Processes the first item in {@link #outcomes}. Sets the state, increments
     * {@link #attempts}, if appropriate, and stores the operation history in the DB.
     */
    private void processOutcome() {
        OperationOutcome outcome = outcomes.peek();

        switch (outcome.getActor()) {
            case FINAL_FAILURE_ACTOR:
                // remove the outcome from the queue, as it was a dummy item
                outcomes.remove();
                state = State.FINAL_FAILURE;
                break;

            case LOCK_ACTOR:
                if (outcome.getEnd() == null) {
                    state = State.LOCK_REQUESTED;
                } else if (outcome.getResult() == PolicyResult.SUCCESS) {
                    state = State.LOCK_GRANTED;
                } else {
                    state = State.LOCK_DENIED;
                    storeGuardFailureInDataBase(outcome, "Operation denied by Lock");
                }
                break;

            case GUARD_ACTOR:
                if (outcome.getEnd() == null) {
                    state = State.GUARD_STARTED;
                } else if (outcome.getResult() == PolicyResult.SUCCESS) {
                    state = State.GUARD_PERMITTED;
                } else {
                    state = State.GUARD_DENIED;
                    storeGuardFailureInDataBase(outcome, "Operation denied by Guard");
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

    private void storeGuardFailureInDataBase(OperationOutcome outcome, String message) {
        outcome.setActor(actor);
        outcome.setOperation(operation);
        outcome.setMessage(message);
        outcome.setResult(PolicyResult.FAILURE_GUARD);

        operationHistory.add(new Operation(outcome));
        storeOperationInDataBase();
    }

    /**
     * Determines if more responses are expected.
     *
     * @return {@code true} if more responses are expected, {@code false} otherwise
     */
    private boolean expectingMore() {
        switch (state) {
            case LOCK_DENIED:
            case GUARD_DENIED:
            case OPERATION_SUCCESS:
            case FINAL_FAILURE:
            case UNKNOWN:
                return false;

            default:
                return true;
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
     * Stores the operation in the DB, if guards are enabled.
     */
    private void storeOperationInDataBase() {
        // Only store in DB if enabled
        boolean guardEnabled = "false"
                        .equalsIgnoreCase(PolicyEngineConstants.getManager().getEnvironmentProperty("guard.disabled"));
        if (!guardEnabled) {
            return;
        }

        try (EntityMgrCloser emc = new EntityMgrCloser(getEntityManagerFactory().createEntityManager());
                        EntityTransCloser trans = new EntityTransCloser(emc.getManager().getTransaction())) {

            Operation oper = operationHistory.peekLast();
            ControlLoopOperation cloper = oper.getClOperation();
            VirtualControlLoopEvent event = context.getEvent();

            Dbao newEntry = new Dbao();

            newEntry.setClosedLoopName(event.getClosedLoopControlName());
            newEntry.setRequestId(event.getRequestId().toString());
            newEntry.setActor(cloper.getActor());
            newEntry.setOperation(cloper.getOperation());
            newEntry.setTarget(cloper.getTarget());
            newEntry.setSubrequestId(cloper.getSubRequestId());
            newEntry.setMessage(cloper.getMessage());
            newEntry.setOutcome(cloper.getOutcome());
            if (cloper.getStart() != null) {
                newEntry.setStarttime(new Date(cloper.getStart().toEpochMilli()));
            }
            if (cloper.getEnd() != null) {
                newEntry.setEndtime(new Date(cloper.getEnd().toEpochMilli()));
            }

            emc.getManager().persist(newEntry);

        } catch (RuntimeException e) {
            logger.error("storeOperationInDataBase threw an exception", e);
        }
    }

    /*
     * Initialization-on-demand holder idiom for the entity manager factory.
     */
    private static class LazyEntityManagerFactory {
        static final EntityManagerFactory INSTANCE;

        static {
            // DB Properties
            Properties props = new Properties();
            if (PolicyEngineConstants.getManager().getEnvironmentProperty(Util.ONAP_KEY_URL) != null
                            && PolicyEngineConstants.getManager().getEnvironmentProperty(Util.ONAP_KEY_USER) != null
                            && PolicyEngineConstants.getManager().getEnvironmentProperty(Util.ONAP_KEY_PASS) != null) {
                props.put(Util.ECLIPSE_LINK_KEY_URL,
                                PolicyEngineConstants.getManager().getEnvironmentProperty(Util.ONAP_KEY_URL));
                props.put(Util.ECLIPSE_LINK_KEY_USER,
                                PolicyEngineConstants.getManager().getEnvironmentProperty(Util.ONAP_KEY_USER));
                props.put(Util.ECLIPSE_LINK_KEY_PASS,
                                PolicyEngineConstants.getManager().getEnvironmentProperty(Util.ONAP_KEY_PASS));
                props.put(PersistenceUnitProperties.CLASSLOADER, ControlLoopOperationManager2.class.getClassLoader());
            }

            String opsHistPu = System.getProperty("OperationsHistoryPU");
            if (!"OperationsHistoryPUTest".equals(opsHistPu)) {
                opsHistPu = "OperationsHistoryPU";
            } else {
                props.clear();
            }


            try {
                INSTANCE = Persistence.createEntityManagerFactory(opsHistPu, props);
            } catch (RuntimeException e) {
                logger.error("failed to initialize EntityManagerFactory");
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    /**
     * Gets the entity manager factory.
     *
     * @return the entity manager factory
     */
    public static EntityManagerFactory getEntityManagerFactory() {
        return LazyEntityManagerFactory.INSTANCE;
    }
}
