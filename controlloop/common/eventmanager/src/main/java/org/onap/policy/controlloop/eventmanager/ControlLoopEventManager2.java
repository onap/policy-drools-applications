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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.drools.core.WorkingMemory;
import org.kie.api.runtime.rule.FactHandle;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.ControlLoopResponse;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.actorserviceprovider.ActorService;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.controlloop.ControlLoopEventContext;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.controlloop.ophistory.OperationHistoryDataManager;
import org.onap.policy.controlloop.policy.FinalResult;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.processor.ControlLoopProcessor;
import org.onap.policy.drools.core.lock.LockCallback;
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
public class ControlLoopEventManager2 implements ManagerContext, Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ControlLoopEventManager2.class);
    private static final long serialVersionUID = -1216568161322872641L;

    private static final String EVENT_MANAGER_SERVICE_CONFIG = "event-manager";
    public static final String PROV_STATUS_ACTIVE = "ACTIVE";
    private static final String VM_NAME = "VM_NAME";
    private static final String VNF_NAME = "VNF_NAME";
    public static final String GENERIC_VNF_VNF_ID = "generic-vnf.vnf-id";
    public static final String GENERIC_VNF_VNF_NAME = "generic-vnf.vnf-name";
    public static final String VSERVER_VSERVER_NAME = "vserver.vserver-name";
    public static final String GENERIC_VNF_IS_CLOSED_LOOP_DISABLED = "generic-vnf.is-closed-loop-disabled";
    public static final String VSERVER_IS_CLOSED_LOOP_DISABLED = "vserver.is-closed-loop-disabled";
    public static final String PNF_IS_IN_MAINT = "pnf.in-maint";
    public static final String GENERIC_VNF_PROV_STATUS = "generic-vnf.prov-status";
    public static final String VSERVER_PROV_STATUS = "vserver.prov-status";
    public static final String PNF_ID = "pnf.pnf-id";
    public static final String PNF_NAME = "pnf.pnf-name";

    private static final Set<String> VALID_TARGETS = Stream
                    .of(VM_NAME, VNF_NAME, VSERVER_VSERVER_NAME, GENERIC_VNF_VNF_ID, GENERIC_VNF_VNF_NAME, PNF_NAME)
                    .map(String::toLowerCase).collect(Collectors.toSet());

    private static final Set<String> TRUE_VALUES = Set.of("true", "t", "yes", "y");

    /**
     * Counts the number of these objects that have been created.  This is used by junit
     * tests.
     */
    private static final AtomicLong createCount = new AtomicLong(0);

    public enum NewEventStatus {
        FIRST_ONSET, SUBSEQUENT_ONSET, FIRST_ABATEMENT, SUBSEQUENT_ABATEMENT, SYNTAX_ERROR
    }

    /**
     * {@code True} if this object was created by this JVM instance, {@code false}
     * otherwise. This will be {@code false} if this object is reconstituted from a
     * persistent store or by transfer from another server.
     */
    private transient boolean createdByThisJvmInstance;

    @Getter
    @ToString.Include
    public final String closedLoopControlName;
    @Getter
    @ToString.Include
    private final UUID requestId;
    @Getter
    private final ControlLoopEventContext context;
    @ToString.Include
    private int numOnsets = 1;
    @ToString.Include
    private int numAbatements = 0;
    private VirtualControlLoopEvent abatement = null;

    /**
     * Time, in milliseconds, when the control loop will time out.
     */
    @Getter
    private final long endTimeMs;

    // fields extracted from the ControlLoopParams
    @Getter
    private final String policyName;
    private final String policyScope;
    private final String policyVersion;

    private final LinkedList<ControlLoopOperation> controlLoopHistory = new LinkedList<>();

    /**
     * Maps a target entity to its lock.
     */
    private final transient Map<String, LockData> target2lock = new HashMap<>();

    private final ControlLoopProcessor processor;
    private final AtomicReference<ControlLoopOperationManager2> currentOperation = new AtomicReference<>();

    private FinalResult finalResult = null;

    @Getter
    private VirtualControlLoopNotification notification;
    @Getter
    private ControlLoopResponse controlLoopResponse;

    @Getter
    private boolean updated = false;

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

        createCount.incrementAndGet();

        checkEventSyntax(event);

        if (isClosedLoopDisabled(event)) {
            throw new IllegalStateException("is-closed-loop-disabled is set to true on VServer or VNF");
        }

        if (isProvStatusInactive(event)) {
            throw new IllegalStateException("prov-status is not ACTIVE on VServer or VNF");
        }

        this.createdByThisJvmInstance = true;
        this.closedLoopControlName = params.getClosedLoopControlName();
        this.requestId = event.getRequestId();
        this.context = new ControlLoopEventContext(event);
        this.policyName = params.getPolicyName();
        this.policyScope = params.getPolicyScope();
        this.policyVersion = params.getPolicyVersion();
        this.processor = new ControlLoopProcessor(params.getToscaPolicy());
        this.workMem = workMem;
        this.endTimeMs = System.currentTimeMillis() + detmControlLoopTimeoutMs();
    }

    /**
     * Gets the number of managers objects that have been created.
     * @return the number of managers objects that have been created
     */
    public static long getCreateCount() {
        return createCount.get();
    }

    /**
     * Starts the manager.
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

        if (currentOperation.get() != null) {
            throw new IllegalStateException("manager already started");
        }

        startOperation();
    }

    /**
     * Starts an operation for the current processor policy.
     *
     * @throws ControlLoopException if the processor cannot get a policy
     */
    private synchronized void startOperation() throws ControlLoopException {

        if ((finalResult = processor.checkIsCurrentPolicyFinal()) == null) {
            // not final - start the next operation
            currentOperation.set(makeOperationManager(context, processor.getCurrentPolicy()));
            currentOperation.get().start(endTimeMs - System.currentTimeMillis());
            return;
        }

        logger.info("final={} oper state={} for {}", finalResult, currentOperation.get().getState(), requestId);

        controlLoopResponse = null;
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
     * Starts the next step, whatever that may be.
     */
    public synchronized void nextStep() {
        if (!isActive()) {
            return;
        }

        updated = false;

        try {
            if (!currentOperation.get().nextStep()) {
                // current operation is done - try the next
                controlLoopHistory.addAll(currentOperation.get().getHistory());
                processor.nextPolicyForResult(currentOperation.get().getOperationResult());
                startOperation();
            }

        } catch (ControlLoopException | RuntimeException e) {
            // processor problem - this is fatal
            logger.warn("{}: cannot start next step for {}", closedLoopControlName, requestId, e);
            finalResult = FinalResult.FINAL_FAILURE_EXCEPTION;
            controlLoopResponse = null;
            notification = makeNotification();
            notification.setNotification(ControlLoopNotificationType.FINAL_FAILURE);
            notification.setMessage("Policy processing aborted due to policy error");
            notification.setHistory(controlLoopHistory);
        }
    }

    /**
     * Determines if the manager is still active.
     *
     * @return {@code true} if the manager is still active, {@code false} otherwise
     */
    public synchronized boolean isActive() {
        return (createdByThisJvmInstance && finalResult == null);
    }

    /**
     * Updates working memory if this changes.
     *
     * @param operation operation manager that was updated
     */
    @Override
    public synchronized void updated(ControlLoopOperationManager2 operation) {
        if (!isActive() || operation != currentOperation.get()) {
            // no longer working on the given operation
            return;
        }

        controlLoopResponse = operation.getControlLoopResponse();
        notification = makeNotification();

        VirtualControlLoopEvent event = context.getEvent();

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
                notification.setMessage("Guard result for " + operation.getActor() + " " + operation.getOperation()
                                + " is Permit");
                break;
            case GUARD_DENIED:
                notification.setNotification(ControlLoopNotificationType.OPERATION);
                notification.setMessage("Guard result for " + operation.getActor() + " " + operation.getOperation()
                                + " is Deny");
                break;
            case OPERATION_STARTED:
                notification.setNotification(ControlLoopNotificationType.OPERATION);
                notification.setMessage(operation.getOperationMessage());
                notification.setHistory(Collections.emptyList());
                break;
            case OPERATION_SUCCESS:
                notification.setNotification(ControlLoopNotificationType.OPERATION_SUCCESS);
                break;

            case CONTROL_LOOP_TIMEOUT:
                logger.warn("{}: control loop timed out for {}", closedLoopControlName, requestId);
                controlLoopHistory.addAll(currentOperation.get().getHistory());
                notification.setNotification(ControlLoopNotificationType.FINAL_FAILURE);
                notification.setMessage("Control Loop timed out");
                notification.setHistory(controlLoopHistory);
                finalResult = FinalResult.FINAL_FAILURE;
                break;

            case OPERATION_FAILURE:
            default:
                notification.setNotification(ControlLoopNotificationType.OPERATION_FAILURE);
                break;
        }

        updated = true;
        workMem.update(factHandle, this);
    }

    /**
     * Cancels the current operation and frees all locks.
     */
    public synchronized void destroy() {
        ControlLoopOperationManager2 oper = currentOperation.get();
        if (oper != null) {
            oper.cancel();
        }

        getBlockingExecutor().execute(this::freeAllLocks);
    }

    /**
     * Frees all locks.
     */
    private void freeAllLocks() {
        target2lock.values().forEach(LockData::free);
    }

    /**
     * Makes a notification message for the current operation.
     *
     * @return a new notification
     */
    public synchronized VirtualControlLoopNotification makeNotification() {
        VirtualControlLoopNotification notif = new VirtualControlLoopNotification(context.getEvent());
        notif.setNotification(ControlLoopNotificationType.OPERATION);
        notif.setFrom("policy");
        notif.setPolicyScope(policyScope);
        notif.setPolicyVersion(policyVersion);

        if (finalResult == null) {
            ControlLoopOperationManager2 oper = currentOperation.get();
            if (oper != null) {
                notif.setMessage(oper.getOperationHistory());
                notif.setHistory(oper.getHistory());
            }
        }

        return notif;
    }

    /**
     * An event onset/abatement.
     *
     * @param event the event
     * @return the status
     */
    public synchronized NewEventStatus onNewEvent(VirtualControlLoopEvent event) {
        try {
            checkEventSyntax(event);

            if (event.getClosedLoopEventStatus() == ControlLoopEventStatus.ONSET) {
                if (event.equals(context.getEvent())) {
                    return NewEventStatus.FIRST_ONSET;
                }

                numOnsets++;
                return NewEventStatus.SUBSEQUENT_ONSET;

            } else {
                if (abatement == null) {
                    abatement = event;
                    numAbatements++;
                    return NewEventStatus.FIRST_ABATEMENT;
                } else {
                    numAbatements++;
                    return NewEventStatus.SUBSEQUENT_ABATEMENT;
                }
            }
        } catch (ControlLoopException e) {
            logger.error("{}: onNewEvent threw an exception", this, e);
            return NewEventStatus.SYNTAX_ERROR;
        }
    }

    /**
     * Determines the overall control loop timeout.
     *
     * @return the policy timeout, in milliseconds, if specified, a default timeout
     *         otherwise
     */
    private long detmControlLoopTimeoutMs() {
        // validation checks preclude null or 0 timeout values in the policy
        Integer timeout = processor.getControlLoop().getTimeout();
        return TimeUnit.MILLISECONDS.convert(timeout, TimeUnit.SECONDS);
    }

    /**
     * Check an event syntax.
     *
     * @param event the event syntax
     * @throws ControlLoopException if an error occurs
     */
    protected void checkEventSyntax(VirtualControlLoopEvent event) throws ControlLoopException {
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
        if (event.getClosedLoopEventStatus() != ControlLoopEventStatus.ONSET
                        && event.getClosedLoopEventStatus() != ControlLoopEventStatus.ABATED) {
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
    private static boolean isClosedLoopDisabled(VirtualControlLoopEvent event) {
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
    private static boolean isProvStatusInactive(VirtualControlLoopEvent event) {
        Map<String, String> aai = event.getAai();
        return !(PROV_STATUS_ACTIVE.equalsIgnoreCase(aai.getOrDefault(VSERVER_PROV_STATUS, PROV_STATUS_ACTIVE))
                        && PROV_STATUS_ACTIVE.equalsIgnoreCase(
                                        aai.getOrDefault(GENERIC_VNF_PROV_STATUS, PROV_STATUS_ACTIVE)));
    }

    /**
     * Determines the boolean value represented by the given AAI field value.
     *
     * @param aaiValue value to be examined
     * @return the boolean value represented by the field value, or {@code false} if the
     *         value is {@code null}
     */
    private static boolean isAaiTrue(String aaiValue) {
        return (aaiValue != null && TRUE_VALUES.contains(aaiValue.toLowerCase()));
    }

    /**
     * Requests a lock. This requests the lock for the time that remains before the
     * timeout expires. This avoids having to extend the lock.
     *
     * @param targetEntity entity to be locked
     * @param lockUnavailableCallback function to be invoked if the lock is
     *        unavailable/lost
     * @return a future that can be used to await the lock
     */
    @Override
    public synchronized CompletableFuture<OperationOutcome> requestLock(String targetEntity,
                    Consumer<OperationOutcome> lockUnavailableCallback) {

        long remainingMs = endTimeMs - System.currentTimeMillis();
        int remainingSec = 15 + Math.max(0, (int) TimeUnit.SECONDS.convert(remainingMs, TimeUnit.MILLISECONDS));

        LockData data = target2lock.computeIfAbsent(targetEntity, key -> {
            LockData data2 = new LockData(key, requestId);
            makeLock(targetEntity, requestId.toString(), remainingSec, data2);
            return data2;
        });

        data.addUnavailableCallback(lockUnavailableCallback);

        return data.getFuture();
    }

    /**
     * Initializes various components, on demand.
     */
    private static class LazyInitData {
        private static final OperationHistoryDataManager DATA_MANAGER;
        private static final ActorService ACTOR_SERVICE;

        static {
            EventManagerServices services = new EventManagerServices(EVENT_MANAGER_SERVICE_CONFIG);
            ACTOR_SERVICE = services.getActorService();
            DATA_MANAGER = services.getDataManager();
        }
    }

    // the following methods may be overridden by junit tests

    protected ControlLoopOperationManager2 makeOperationManager(ControlLoopEventContext ctx, Policy policy) {
        return new ControlLoopOperationManager2(this, ctx, policy, getExecutor());
    }

    protected Executor getExecutor() {
        return ForkJoinPool.commonPool();
    }

    protected ExecutorService getBlockingExecutor() {
        return PolicyEngineConstants.getManager().getExecutorService();
    }

    protected void makeLock(String targetEntity, String requestId, int holdSec, LockCallback callback) {
        PolicyEngineConstants.getManager().createLock(targetEntity, requestId, holdSec, callback, false);
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
