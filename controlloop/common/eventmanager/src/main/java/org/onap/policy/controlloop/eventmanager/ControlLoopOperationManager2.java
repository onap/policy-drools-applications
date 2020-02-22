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
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.onap.aai.domain.yang.GenericVnf;
import org.onap.aai.domain.yang.ServiceInstance;
import org.onap.ccsdk.cds.controllerblueprints.processing.api.ExecutionServiceInput;
import org.onap.policy.aai.AaiCqResponse;
import org.onap.policy.aai.util.AaiException;
import org.onap.policy.appc.Response;
import org.onap.policy.appc.ResponseCode;
import org.onap.policy.appclcm.AppcLcmDmaapWrapper;
import org.onap.policy.cds.CdsResponse;
import org.onap.policy.common.utils.jpa.EntityMgrCloser;
import org.onap.policy.common.utils.jpa.EntityTransCloser;
import org.onap.policy.controlloop.ControlLoopEvent;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.ControlLoopResponse;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actor.appc.AppcActorServiceProvider;
import org.onap.policy.controlloop.actor.appclcm.AppcLcmActorServiceProvider;
import org.onap.policy.controlloop.actor.cds.CdsActorServiceProvider;
import org.onap.policy.controlloop.actor.cds.constants.CdsActorConstants;
import org.onap.policy.controlloop.actor.sdnc.SdncActorServiceProvider;
import org.onap.policy.controlloop.actor.sdnr.SdnrActorServiceProvider;
import org.onap.policy.controlloop.actor.so.SoActorServiceProvider;
import org.onap.policy.controlloop.actor.vfc.VfcActorServiceProvider;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.controlloop.ControlLoopEventContext;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.database.operationshistory.Dbao;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.guard.Util;
import org.onap.policy.sdnc.SdncResponse;
import org.onap.policy.sdnr.PciResponseWrapper;
import org.onap.policy.so.SoResponseWrapper;
import org.onap.policy.vfc.VfcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages a single Operation.
 */
@ToString(onlyExplicitlyIncluded = true)
public class ControlLoopOperationManager2 implements Serializable {
    private static final long serialVersionUID = -3773199283624595410L;
    private static final Logger logger = LoggerFactory.getLogger(ControlLoopOperationManager2.class);

    public enum State {
        STARTING, LOCK_REQUESTED, LOCK_DENIED, LOCK_GRANTED, GUARD_STARTED, GUARD_PERMITTED, GUARD_DENIED, OPERATION_STARTED, OPERATION_COMPLETED, UNKNOWN
    }

    /**
     * {@code True} if this object was created by this JVM instance, {@code false}
     * otherwise. This will be {@code false} if this object is reconstituted from a
     * persistent store or by transfer from another server.
     */
    private transient boolean createdByThisJvmInstance;

    private final ControlLoopEventContext context;
    public final Policy policy;

    /**
     * Invoked whenever "this" changes due to callbacks from the Actor.
     */
    private final Consumer<ControlLoopOperationManager2> listener;

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
    private final List<Operation> operationHistory = new LinkedList<>();

    /**
     * Queue of outcomes yet to be processed.
     */
    private final Queue<OperationOutcome> outcomes = new LinkedList<>();

    private final String actor;
    private final String operation;

    /**
     * Construct an instance.
     *
     * @param onset the onset event
     * @param policy the policy
     * @param em the event manager
     * @throws ControlLoopException if an error occurs
     */
    public ControlLoopOperationManager2(ControlLoopEventContext context, Policy policy,
                    Consumer<ControlLoopOperationManager2> listener) throws ControlLoopException {

        this.createdByThisJvmInstance = true;
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

        params.start();
    }

    private synchronized void onResponse(OperationOutcome outcome) {
        String outcomeActor = outcome.getActor();

        if (!"LOCK".equals(outcomeActor) && !"GUARD".equals(outcomeActor)
                        && (!actor.equals(outcomeActor) || !operation.equals(outcome.getOperation()))) {
            // don't care about this outcome - it's for something else (e.g., A&AI query)
            return;
        }

        outcomes.add(new OperationOutcome(outcome));

        if (outcomes.size() == 1) {
            state = detmState();
            listener.accept(this);
        }
    }

    /**
     * Looks for the next step.
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

        state = detmState();
        listener.accept(this);
        return expectingMore();
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
            case OPERATION_COMPLETED:
            case UNKNOWN:
                return false;

            default:
                return true;
        }
    }

    /**
     * Determines the current state based on the first item in {@link #operationQueue}.
     * Increments {@link #attempts} if it's the start of a new attempt.
     *
     * @return the new state
     */
    private State detmState() {
        OperationOutcome outcome = outcomes.peek();
        String outcomeActor = outcome.getActor();

        switch (outcomeActor) {
            case "LOCK":
                if (outcome.getResult() != PolicyResult.SUCCESS) {
                    return State.LOCK_DENIED;
                } else if (outcome.getEnd() != null) {
                    return State.LOCK_GRANTED;
                } else {
                    return State.LOCK_REQUESTED;
                }

            case "GUARD":
                if (outcome.getResult() != PolicyResult.SUCCESS) {
                    return State.GUARD_DENIED;
                } else if (outcome.getEnd() != null) {
                    return State.GUARD_PERMITTED;
                } else {
                    return State.GUARD_STARTED;
                }

            default:
                if (outcome.getEnd() != null) {
                    return State.OPERATION_COMPLETED;
                } else {
                    // start of a new attempt
                    ++attempts;
                    return State.OPERATION_STARTED;
                }
        }
    }

    public PolicyResult getOperationResult() {
        return this.policyResult;
    }

    /**
     * Get the operation as a message.
     *
     * @return the operation as a message
     */
    public String getOperationMessage() {
        if (this.currentOperation != null && this.currentOperation.clOperation != null) {
            return this.currentOperation.clOperation.toMessage();
        }

        if (!this.operationHistory.isEmpty()) {
            return this.operationHistory.getLast().clOperation.toMessage();
        }
        return null;
    }

    /**
     * Get the operation as a message including the guard result.
     *
     * @param guardResult the guard result
     * @return the operation as a message including the guard result
     */
    public String getOperationMessage(String guardResult) {
        if (this.currentOperation != null && this.currentOperation.clOperation != null) {
            return this.currentOperation.clOperation.toMessage() + ", Guard result: " + guardResult;
        }

        if (!this.operationHistory.isEmpty()) {
            return this.operationHistory.getLast().clOperation.toMessage() + ", Guard result: " + guardResult;
        }
        return null;
    }

    /**
     * Get the operation history.
     *
     * @return the operation history
     */
    public String getOperationHistory() {
        if (this.currentOperation != null && this.currentOperation.clOperation != null) {
            return this.currentOperation.clOperation.toHistory();
        }

        if (!this.operationHistory.isEmpty()) {
            return this.operationHistory.getLast().clOperation.toHistory();
        }
        return null;
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

    private void storeOperationInDataBase() {
        // Only store in DB if enabled
        boolean guardEnabled = "false"
                        .equalsIgnoreCase(PolicyEngineConstants.getManager().getEnvironmentProperty("guard.disabled"));
        if (!guardEnabled) {
            return;
        }

        try (EntityMgrCloser emc = new EntityMgrCloser(getEntityManagerFactory().createEntityManager());
                        EntityTransCloser trans = new EntityTransCloser(emc.getManager().getTransaction())) {
            Dbao newEntry = new Dbao();

            newEntry.setClosedLoopName(this.onset.getClosedLoopControlName());
            newEntry.setRequestId(this.onset.getRequestId().toString());
            newEntry.setActor(this.currentOperation.clOperation.getActor());
            newEntry.setOperation(this.currentOperation.clOperation.getOperation());
            newEntry.setTarget(this.targetEntity);
            newEntry.setStarttime(Timestamp.from(this.currentOperation.clOperation.getStart()));
            newEntry.setSubrequestId(this.currentOperation.clOperation.getSubRequestId());
            newEntry.setEndtime(new Timestamp(this.currentOperation.clOperation.getEnd().toEpochMilli()));
            newEntry.setMessage(this.currentOperation.clOperation.getMessage());
            newEntry.setOutcome(this.currentOperation.clOperation.getOutcome());

            emc.getManager().persist(newEntry);

        } catch (RuntimeException e) {
            logger.error("storeOperationInDataBase threw an exception", e);
        }
    }

    private void completeOperation(OperationOutcome outcome) {
        OperationOutcome outcome2 = outcome;

        if ("GUARD".equals(outcome.getActor())) {
            outcome2 = new OperationOutcome(outcome);
            outcome2.setActor(actor);
            outcome2.setOperation(operation);

            if (outcome2.getResult() != PolicyResult.SUCCESS) {
                outcome2.setMessage("Operation denied by Guard");
                outcome2.setResult(PolicyResult.FAILURE_GUARD);
            }
        }

        if (!actor.equals(outcome2.getActor()) || !operation.equals(outcome2.getOperation())) {
            return;
        }

        if (outcome2.getResult() == PolicyResult.FAILURE_TIMEOUT) {
            outcome2.setMessage("Operation timed out");
        }

        operationHistory.add(new Operation(outcome2));
        storeOperationInDataBase();
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

    public static EntityManagerFactory getEntityManagerFactory() {
        return LazyEntityManagerFactory.INSTANCE;
    }
}
