/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2017-2020 AT&T Intellectual Property. All rights reserved.
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
import static org.onap.policy.controlloop.ControlLoopTargetType.VM;
import static org.onap.policy.controlloop.ControlLoopTargetType.VNF;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.drools.core.WorkingMemory;
import org.kie.api.runtime.rule.FactHandle;
import org.onap.policy.aai.util.AaiException;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.actorserviceprovider.ActorService;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.controlloop.ControlLoopEventContext;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.controlloop.policy.FinalResult;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.controlloop.processor.ControlLoopProcessor;
import org.onap.policy.controlloop.utils.ControlLoopUtils;
import org.onap.policy.drools.core.lock.Lock;
import org.onap.policy.drools.core.lock.LockCallback;
import org.onap.policy.drools.core.lock.LockImpl;
import org.onap.policy.drools.core.lock.LockState;
import org.onap.policy.drools.persistence.SystemPersistenceConstants;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for a single control loop event. Once this has been created, the event can be
 * retracted from working memory. Once this has been created, {@link #start()} should be
 * invoked, and then {@link #nextStep()} should be invoked continually until
 * {@link #isActive()} returns {@code false}, indicating that all steps have completed.
 */
@ToString(onlyExplicitlyIncluded = true)
public class ControlLoopEventManager2 implements ControlLoopOperationManager2.ManagerContext, Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ControlLoopEventManager2.class);
    private static final long serialVersionUID = -1216568161322872641L;

    private static final String ACTOR_SERVICE_PROPERTIES = "actor.service";
    public static final String PROV_STATUS_ACTIVE = "ACTIVE";
    private static final String VM_NAME = "VM_NAME";
    private static final String VNF_NAME = "VNF_NAME";
    public static final String GENERIC_VNF_VNF_ID = "generic-vnf.vnf-id";
    public static final String GENERIC_VNF_VNF_NAME = "generic-vnf.vnf-name";
    public static final String VSERVER_VSERVER_NAME = "vserver.vserver-name";
    public static final String GENERIC_VNF_IS_CLOSED_LOOP_DISABLED = "generic-vnf.is-closed-loop-disabled";
    public static final String VSERVER_IS_CLOSED_LOOP_DISABLED = "vserver.is-closed-loop-disabled";
    private static final String PNF_IS_IN_MAINT = "pnf.in-maint";
    public static final String GENERIC_VNF_PROV_STATUS = "generic-vnf.prov-status";
    public static final String VSERVER_PROV_STATUS = "vserver.prov-status";
    public static final String PNF_ID = "pnf.pnf-id";
    public static final String PNF_NAME = "pnf.pnf-name";

    public enum NewEventStatus {
        FIRST_ONSET, SUBSEQUENT_ONSET, FIRST_ABATEMENT, SUBSEQUENT_ABATEMENT, SYNTAX_ERROR
    }

    private static final int DEFAULT_TIMEOUT_SEC = (int) TimeUnit.SECONDS.convert(1, TimeUnit.HOURS);

    private static final Set<String> VALID_TARGETS = Stream
                    .of(VM_NAME, VNF_NAME, VSERVER_VSERVER_NAME, GENERIC_VNF_VNF_ID, GENERIC_VNF_VNF_NAME, PNF_NAME)
                    .map(String::toLowerCase).collect(Collectors.toSet());

    private static final Set<String> TRUE_VALUES = Set.of("true", "t", "yes", "y");

    // TODO use DroolsExecutor for lock callbacks; eliminate need for "synchronized"

    // TODO limit the number of policies that may be executed by a single event manager,
    // to prevent loops

    /**
     * {@code True} if this object was created by this JVM instance, {@code false}
     * otherwise. This will be {@code false} if this object is reconstituted from a
     * persistent store or by transfer from another server.
     */
    private transient boolean createdByThisJvmInstance;

    @ToString.Include
    public final String closedLoopControlName;
    @ToString.Include
    private final UUID requestId;
    private final ControlLoopEventContext context;
    @ToString.Include
    private int numOnsets = 1;
    @ToString.Include
    private int numAbatements = 0;
    private VirtualControlLoopEvent abatement = null;

    /**
     * Overall control loop timeout, in seconds.
     */
    private final int controlLoopTimeoutSec;

    // fields extracted from the ControlLoopParams
    @Getter
    private final String policyName;
    private final String policyScope;
    private final String policyVersion;

    private final LinkedList<ControlLoopOperation> controlLoopHistory = new LinkedList<>();

    /**
     * This value will typically only be changed by junits.
     */
    @Setter(AccessLevel.PROTECTED)
    private boolean useTargetLock = true;

    /**
     * Maps a target entity to its lock.
     */
    private final transient Map<String, LockData> target2lock = new HashMap<>();

    private final ControlLoopProcessor processor;
    private final AtomicReference<ControlLoopOperationManager2> currentOperation = new AtomicReference<>();

    private FinalResult finalResult = null;

    @Getter
    private VirtualControlLoopNotification notification;

    private final transient WorkingMemory workMem;
    private transient FactHandle factHandle;


    /**
     * Constructs the object.
     *
     * @param params control loop parameters
     * @param event event to be managed by this object
     * @param workMem working memory to update if this changes
     * @throws ControlLoopException if the event is invalid or if a YAML processor cannot
     *         be created
     */
    public ControlLoopEventManager2(ControlLoopParams params, VirtualControlLoopEvent event, WorkingMemory workMem)
                    throws ControlLoopException {

        checkEventSyntax(event);

        Map<String, String> aai = event.getAai();
        if (aai.containsKey(VSERVER_IS_CLOSED_LOOP_DISABLED) || aai.containsKey(GENERIC_VNF_IS_CLOSED_LOOP_DISABLED)) {
            if (isClosedLoopDisabled(event)) {
                throw new IllegalStateException("is-closed-loop-disabled is set to true on VServer or VNF");
            }

            if (isProvStatusInactive(event)) {
                throw new IllegalStateException("prov-status is not ACTIVE on VServer or VNF");
            }
        }

        this.createdByThisJvmInstance = true;
        this.closedLoopControlName = params.getClosedLoopControlName();
        this.requestId = event.getRequestId();
        this.context = new ControlLoopEventContext(event);
        this.controlLoopTimeoutSec = getControlLoopTimeoutSec();
        this.policyName = params.getPolicyName();
        this.policyScope = params.getPolicyScope();
        this.policyVersion = params.getPolicyVersion();
        this.processor = new ControlLoopProcessor(params.getToscaPolicy());
        this.workMem = workMem;
    }

    /**
     * Starts the manager.
     *
     * @throws ControlLoopException if the processor cannot get a policy
     */
    public void start() throws ControlLoopException {
        if ((factHandle = workMem.getFactHandle(this)) == null) {
            throw new IllegalStateException("manager is not in working memory");
        }

        if (!createdByThisJvmInstance) {
            throw new IllegalStateException("manager was not created by this JVM");
        }

        if (currentOperation.get() != null) {
            throw new IllegalStateException("manager already started");
        }

        startOperation();
    }

    /**
     * Starts the next step, whatever that may be.
     */
    public void nextStep() {
        if (!isActive()) {
            return;
        }

        try {
            if (!currentOperation.get().nextStep()) {
                // current operation is done - try the next
                controlLoopHistory.addAll(currentOperation.get().getHistory());
                processor.nextPolicyForResult(currentOperation.get().getOperationResult());
                startOperation();
            }

        } catch (ControlLoopException e) {
            // processor problem - this is fatal
            logger.warn("{}: cannot start next step for {}", closedLoopControlName, requestId, e);
            notification = makeNotification();
            notification.setNotification(ControlLoopNotificationType.FINAL_FAILURE);
            notification.setMessage("Policy processing aborted due to policy error");
            notification.setHistory(controlLoopHistory);
            finalResult = FinalResult.FINAL_FAILURE_EXCEPTION;
        }
    }

    /**
     * Starts an operation for the current processor policy.
     *
     * @throws ControlLoopException if the processor cannot get a policy
     */
    private synchronized void startOperation() throws ControlLoopException {
        currentOperation.set(makeOperationManager());

        if ((finalResult = processor.checkIsCurrentPolicyFinal()) == null) {
            // not final - start the operation
            currentOperation.get().start();
            return;
        }

        notification = makeNotification();
        notification.setHistory(controlLoopHistory);

        switch (finalResult) {
            case FINAL_FAILURE_EXCEPTION:
                notification.setNotification(ControlLoopNotificationType.FINAL_FAILURE);
                notification.setMessage("Exception in processing closed loop");
                break;
            case FINAL_SUCCESS:
                notification.setNotification(ControlLoopNotificationType.FINAL_SUCCESS);
                break;
            case FINAL_OPENLOOP:
                notification.setNotification(ControlLoopNotificationType.FINAL_OPENLOOP);
                break;
            case FINAL_FAILURE:
            default:
                notification.setNotification(ControlLoopNotificationType.FINAL_FAILURE);
                break;
        }
    }

    /**
     * Determines if the manager is still active.
     *
     * @return {@code true} if the manager is still active, {@code false} otherwise
     */
    public boolean isActive() {
        return (createdByThisJvmInstance && finalResult == null);
    }

    /**
     * Updates working memory if this changes.
     *
     * @param changedMgr operation manager that was updated
     */
    @Override
    public synchronized void updated(ControlLoopOperationManager2 changedMgr) {
        if (!isActive() || changedMgr != currentOperation.get()) {
            // no longer working on the given operation
            return;
        }

        notification = makeNotification();

        VirtualControlLoopEvent event = context.getEvent();
        ControlLoopOperationManager2 operation = currentOperation.get();

        notification.setHistory(operation.getHistory());

        switch (operation.getState()) {
            case LOCK_DENIED:
                notification.setNotification(ControlLoopNotificationType.REJECTED);
                notification.setMessage("The target " + event.getAai().get(event.getTarget()) + " is already locked");
                break;
            case LOCK_LOST:
                notification.setNotification(ControlLoopNotificationType.OPERATION_FAILURE);
                notification.setMessage("The target " + event.getAai().get(event.getTarget()) + " is no longer locked");
                break;
            case GUARD_STARTED:
                notification.setNotification(ControlLoopNotificationType.OPERATION);
                notification.setMessage(
                                "Sending guard query for " + operation.getActor() + " " + operation.getOperation());
                break;
            case GUARD_PERMITTED:
                notification.setNotification(ControlLoopNotificationType.OPERATION);
                // TODO use appropriate "Permit" literal
                notification.setMessage("Guard result for " + operation.getActor() + " " + operation.getOperation()
                                + " is Permit");
                break;
            case GUARD_DENIED:
                notification.setNotification(ControlLoopNotificationType.OPERATION);
                // TODO use appropriate "Deny" literal
                notification.setMessage("Guard result for " + operation.getActor() + " " + operation.getOperation()
                                + " is Deny");
                break;
            case OPERATION_STARTED:
                notification.setNotification(ControlLoopNotificationType.OPERATION);
                notification.setMessage(operation.getOperationMessage());
                break;
            case OPERATION_SUCCESS:
                notification.setNotification(ControlLoopNotificationType.OPERATION_SUCCESS);
                break;
            case OPERATION_FAILURE:
                notification.setNotification(ControlLoopNotificationType.OPERATION_FAILURE);
                break;
            default:
                break;
        }

        workMem.update(factHandle, this);
    }

    /**
     * Cancels the current operation and frees all locks.
     */
    public void destroy() {
        if (currentOperation.get() != null) {
            currentOperation.get().cancel();
        }

        target2lock.values().forEach(LockData::free);
    }

    /**
     * Makes a notification message for the current operation.
     *
     * @return a new notification
     */
    public VirtualControlLoopNotification makeNotification() {
        VirtualControlLoopNotification notif = new VirtualControlLoopNotification();
        notif.setNotification(ControlLoopNotificationType.OPERATION);
        notif.setMessage(currentOperation.get().getOperationMessage());
        notif.setHistory(currentOperation.get().getHistory());
        notif.setFrom("policy");
        notif.setPolicyScope(policyScope);
        notif.setPolicyVersion(policyVersion);

        return notif;
    }

    /**
     * An event onset/abatement.
     *
     * @param event the event
     * @return the status
     * @throws AaiException if an error occurs retrieving information from A&AI
     */
    public NewEventStatus onNewEvent(VirtualControlLoopEvent event) throws AaiException {
        try {
            checkEventSyntax(event);

            if (event.getClosedLoopEventStatus() == ControlLoopEventStatus.ONSET) {
                if (event.equals(context.getEvent())) {
                    return NewEventStatus.FIRST_ONSET;
                }

                numOnsets++;
                return NewEventStatus.SUBSEQUENT_ONSET;

            } else if (event.getClosedLoopEventStatus() == ControlLoopEventStatus.ABATED) {
                if (abatement == null) {
                    abatement = event;
                    numAbatements++;
                    return NewEventStatus.FIRST_ABATEMENT;
                } else {
                    numAbatements++;
                    return NewEventStatus.SUBSEQUENT_ABATEMENT;
                }

            } else {
                return NewEventStatus.SYNTAX_ERROR;
            }
        } catch (ControlLoopException e) {
            logger.error("{}: onNewEvent threw: ", this, e);
            return NewEventStatus.SYNTAX_ERROR;
        }
    }

    /**
     * Gets the overall control loop timeout, in seconds.
     *
     * @return the timeout, in seconds
     */
    private int getControlLoopTimeoutSec() {
        if (processor != null && processor.getControlLoop() != null) {
            Integer timeout = processor.getControlLoop().getTimeout();
            if (timeout != null && timeout > 0) {
                return timeout;
            }
        }

        return DEFAULT_TIMEOUT_SEC;
    }

    /**
     * Check an event syntax.
     *
     * @param event the event syntax
     * @throws ControlLoopException if an error occurs
     */
    public void checkEventSyntax(VirtualControlLoopEvent event) throws ControlLoopException {
        validateStatus(event);
        if (StringUtils.isBlank(event.getClosedLoopControlName())) {
            throw new ControlLoopException("No control loop name");
        }
        if (event.getRequestId() == null) {
            throw new ControlLoopException("No request ID");
        }
        if (event.getClosedLoopEventStatus() == ControlLoopEventStatus.ABATED) {
            return;
        }
        if (StringUtils.isBlank(event.getTarget())) {
            throw new ControlLoopException("No target field");
        } else if (!VALID_TARGETS.contains(event.getTarget().toLowerCase())) {
            throw new ControlLoopException("target field invalid");
        }
        validateAaiData(event);
    }

    private void validateStatus(VirtualControlLoopEvent event) throws ControlLoopException {
        if (event.getClosedLoopEventStatus() == null
                        || (event.getClosedLoopEventStatus() != ControlLoopEventStatus.ONSET
                                        && event.getClosedLoopEventStatus() != ControlLoopEventStatus.ABATED)) {
            throw new ControlLoopException("Invalid value in closedLoopEventStatus");
        }
    }

    private void validateAaiData(VirtualControlLoopEvent event) throws ControlLoopException {
        Map<String, String> eventAai = event.getAai();
        if (eventAai == null) {
            throw new ControlLoopException("AAI is null");
        }
        if (event.getTargetType() == null) {
            throw new ControlLoopException("The Target type is null");
        }
        switch (event.getTargetType()) {
            case VM:
            case VNF:
                validateAaiVmVnfData(eventAai);
                return;
            case PNF:
                validateAaiPnfData(eventAai);
                return;
            default:
                throw new ControlLoopException("The target type is not supported");
        }
    }

    private void validateAaiVmVnfData(Map<String, String> eventAai) throws ControlLoopException {
        if (eventAai.get(GENERIC_VNF_VNF_ID) == null && eventAai.get(VSERVER_VSERVER_NAME) == null
                        && eventAai.get(GENERIC_VNF_VNF_NAME) == null) {
            throw new ControlLoopException(
                            "generic-vnf.vnf-id or generic-vnf.vnf-name or vserver.vserver-name information missing");
        }
    }

    private void validateAaiPnfData(Map<String, String> eventAai) throws ControlLoopException {
        if (eventAai.get(PNF_NAME) == null) {
            throw new ControlLoopException("AAI PNF object key pnf-name is missing");
        }
    }

    /**
     * Is closed loop disabled for an event.
     *
     * @param event the event
     * @return <code>true</code> if the control loop is disabled, <code>false</code>
     *         otherwise
     */
    public static boolean isClosedLoopDisabled(VirtualControlLoopEvent event) {
        Map<String, String> aai = event.getAai();
        return (isAaiTrue(aai.get(VSERVER_IS_CLOSED_LOOP_DISABLED))
                        || isAaiTrue(aai.get(GENERIC_VNF_IS_CLOSED_LOOP_DISABLED))
                        || isAaiTrue(aai.get(PNF_IS_IN_MAINT)));
    }

    /**
     * Does provisioning status, for an event, have a value other than ACTIVE.
     *
     * @param event the event
     * @return {@code true} if the provisioning status is neither ACTIVE nor {@code null},
     *         {@code false} otherwise
     */
    protected static boolean isProvStatusInactive(VirtualControlLoopEvent event) {
        Map<String, String> aai = event.getAai();
        return (!PROV_STATUS_ACTIVE.equals(aai.getOrDefault(VSERVER_PROV_STATUS, PROV_STATUS_ACTIVE))
                        && !PROV_STATUS_ACTIVE.equals(aai.getOrDefault(GENERIC_VNF_PROV_STATUS, PROV_STATUS_ACTIVE)));
    }

    /**
     * Determines the boolean value represented by the given AAI field value.
     *
     * @param aaiValue value to be examined
     * @return the boolean value represented by the field value, or {@code false} if the
     *         value is {@code null}
     */
    protected static boolean isAaiTrue(String aaiValue) {
        return (aaiValue != null && TRUE_VALUES.contains(aaiValue.toLowerCase()));
    }

    /**
     * Requests a lock.
     *
     * @param targetEntity entity to be locked
     * @param lockUnavailableCallback function to be invoked if the lock is
     *        unavailable/lost
     * @return a future that can be used to await the lock
     */
    @Override
    public synchronized CompletableFuture<OperationOutcome> requestLock(String targetEntity,
                    Runnable lockUnavailableCallback) {

        LockData data = target2lock.computeIfAbsent(targetEntity, key -> new LockData(targetEntity));
        data.lockUnavailableCallbacks.add(lockUnavailableCallback);

        return data.future.get();
    }

    /**
     * Data for an individual lock.
     */
    private class LockData implements LockCallback {
        private Lock lock;

        /**
         * Future for obtaining the lock.
         */
        private AtomicReference<CompletableFuture<OperationOutcome>> future =
                        new AtomicReference<>(new CompletableFuture<>());

        /**
         * Listeners to invoke if the lock is unavailable/lost.
         */
        private final List<Runnable> lockUnavailableCallbacks = new ArrayList<>();

        /**
         * Constructs the object.
         *
         * @param targetEntity target entity
         */
        public LockData(String targetEntity) {
            if (useTargetLock) {
                lock = createRealLock(targetEntity, requestId, controlLoopTimeoutSec, this);
            } else {
                lock = createPseudoLock(targetEntity, requestId, controlLoopTimeoutSec, null);
                lockAvailable(lock);
            }
        }

        public void free() {
            lock.free();
        }

        @Override
        public void lockAvailable(Lock unused) {
            OperationOutcome outcome = new OperationOutcome();
            outcome.setActor(ControlLoopOperationManager2.LOCK_ACTOR);
            outcome.setOperation(ControlLoopOperationManager2.LOCK_OPERATION);
            outcome.setResult(PolicyResult.SUCCESS);
            outcome.setMessage(ControlLoopOperation.SUCCESS_MSG);

            // TODO do this in a particular thread?
            future.get().complete(outcome);
        }

        @Override
        public void lockUnavailable(Lock unused) {
            OperationOutcome outcome = new OperationOutcome();
            outcome.setActor(ControlLoopOperationManager2.LOCK_ACTOR);
            outcome.setOperation(ControlLoopOperationManager2.LOCK_OPERATION);
            outcome.setResult(PolicyResult.FAILURE);
            outcome.setMessage(ControlLoopOperation.FAILED_MSG);

            /*
             * In case the future was already completed successfully, replace it with a
             * failed future, but complete the old one, too, in case it wasn't completed
             * yet.
             */
            // TODO do this in a particular thread?
            future.getAndSet(CompletableFuture.completedFuture(outcome)).complete(outcome);

            for (Runnable callback : lockUnavailableCallbacks) {
                try {
                    callback.run();
                } catch (RuntimeException e) {
                    logger.warn("lock callback threw an exception for {}", requestId, e);
                }
            }
        }
    }

    /**
     * Initializes various components, on demand.
     */
    private static class LazyInitData {
        private static final OperationHistoryDataManager DATA_MANAGER = new OperationHistoryDataManager();
        private static final ActorService ACTOR_SERVICE = new ActorService();

        // configure and start the actor service
        static {
            try {
                Properties props = SystemPersistenceConstants.getManager().getProperties(ACTOR_SERVICE_PROPERTIES);
                Map<String, Object> parameters = ControlLoopUtils.toObject(props, ACTOR_SERVICE_PROPERTIES);

                ACTOR_SERVICE.configure(parameters);
                ACTOR_SERVICE.start();

            } catch (RuntimeException e) {
                logger.error("failed to start actor services", e);
            }
        }
    }

    // the following methods may be overridden by junit tests

    private ControlLoopOperationManager2 makeOperationManager() throws ControlLoopException {
        return new ControlLoopOperationManager2(this, context, processor.getCurrentPolicy());
    }

    protected Lock createRealLock(String targetEntity, UUID requestId, int holdSec, LockCallback callback) {
        return PolicyEngineConstants.getManager().createLock(targetEntity, requestId.toString(), holdSec, callback,
                        false);
    }

    protected Lock createPseudoLock(String targetEntity, UUID requestId, int holdSec, LockCallback callback) {
        return new LockImpl(LockState.ACTIVE, targetEntity, requestId.toString(), holdSec, callback);
    }

    @Override
    public ActorService getActorService() {
        return LazyInitData.ACTOR_SERVICE;
    }

    @Override
    public OperationHistoryDataManager getDataManager() {
        return LazyInitData.DATA_MANAGER;
    }
}
