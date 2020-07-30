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

package org.onap.policy.drools.apps.controller.usecases;

import static org.onap.policy.controlloop.ControlLoopTargetType.PNF;
import static org.onap.policy.controlloop.ControlLoopTargetType.VM;
import static org.onap.policy.controlloop.ControlLoopTargetType.VNF;
import static org.onap.policy.drools.apps.controller.usecases.UsecasesConstants.GENERIC_VNF_IS_CLOSED_LOOP_DISABLED;
import static org.onap.policy.drools.apps.controller.usecases.UsecasesConstants.GENERIC_VNF_PROV_STATUS;
import static org.onap.policy.drools.apps.controller.usecases.UsecasesConstants.GENERIC_VNF_VNF_ID;
import static org.onap.policy.drools.apps.controller.usecases.UsecasesConstants.GENERIC_VNF_VNF_NAME;
import static org.onap.policy.drools.apps.controller.usecases.UsecasesConstants.PNF_IS_IN_MAINT;
import static org.onap.policy.drools.apps.controller.usecases.UsecasesConstants.PNF_NAME;
import static org.onap.policy.drools.apps.controller.usecases.UsecasesConstants.PROV_STATUS_ACTIVE;
import static org.onap.policy.drools.apps.controller.usecases.UsecasesConstants.VM_NAME;
import static org.onap.policy.drools.apps.controller.usecases.UsecasesConstants.VNF_NAME;
import static org.onap.policy.drools.apps.controller.usecases.UsecasesConstants.VSERVER_IS_CLOSED_LOOP_DISABLED;
import static org.onap.policy.drools.apps.controller.usecases.UsecasesConstants.VSERVER_PROV_STATUS;
import static org.onap.policy.drools.apps.controller.usecases.UsecasesConstants.VSERVER_VSERVER_NAME;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
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
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.OperationProperties;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.controlloop.eventmanager.ControlLoopEventManager;
import org.onap.policy.controlloop.eventmanager.StepContext;
import org.onap.policy.controlloop.policy.FinalResult;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.drools.apps.controller.usecases.step.AaiCqStep2;
import org.onap.policy.drools.apps.controller.usecases.step.AaiGetPnfStep2;
import org.onap.policy.drools.apps.controller.usecases.step.AaiGetTenantStep2;
import org.onap.policy.drools.apps.controller.usecases.step.GuardStep2;
import org.onap.policy.drools.apps.controller.usecases.step.LockStep2;
import org.onap.policy.drools.apps.controller.usecases.step.Step2;
import org.onap.policy.sdnr.PciMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for a single control loop event. Once this has been created, the event can be
 * retracted from working memory. Processing progresses through each policy, which
 * involves at least one step. As a step is processed, additional preprocessor steps may
 * be pushed onto the queue (e.g., locks, A&AI queries, guards).
 */
@ToString(onlyExplicitlyIncluded = true)
public class UsecasesEventManager extends ControlLoopEventManager implements StepContext {

    private static final Logger logger = LoggerFactory.getLogger(UsecasesEventManager.class);
    private static final long serialVersionUID = -1216568161322872641L;

    /**
     * Maximum number of steps, for a single policy, allowed in the queue at a time.
     */
    private static final int MAX_STEPS = 30;

    private static final Set<String> VALID_TARGETS = Stream
                    .of(VM_NAME, VNF_NAME, VSERVER_VSERVER_NAME, GENERIC_VNF_VNF_ID, GENERIC_VNF_VNF_NAME, PNF_NAME)
                    .map(String::toLowerCase).collect(Collectors.toSet());

    private static final Set<String> TRUE_VALUES = Set.of("true", "t", "yes", "y");

    /**
     * Names of Operation properties for which A&AI PNF query is needed.
     */
    private static final Set<String> PNF_PROPERTIES = Set.of(OperationProperties.AAI_PNF);

    /**
     * Names of Operation properties for which A&AI Tenant query is needed.
     */
    private static final Set<String> TENANT_PROPERTIES = Set.of(OperationProperties.AAI_VSERVER_LINK);

    /**
     * Names of Operation properties for which A&AI custom query is needed.
     */
    private static final Set<String> CQ_PROPERTIES = Set.of(OperationProperties.AAI_DEFAULT_CLOUD_REGION,
                    OperationProperties.AAI_VNF, OperationProperties.AAI_SERVICE_MODEL,
                    OperationProperties.AAI_VNF_MODEL, OperationProperties.AAI_SERVICE,
                    OperationProperties.AAI_RESOURCE_VNF, UsecasesConstants.AAI_DEFAULT_GENERIC_VNF);

    public enum State {
        LOAD_POLICY, POLICY_LOADED, AWAITING_OUTCOME, DONE
    }

    public enum NewEventStatus {
        FIRST_ONSET, SUBSEQUENT_ONSET, FIRST_ABATEMENT, SUBSEQUENT_ABATEMENT, SYNTAX_ERROR
    }

    @Getter
    private final VirtualControlLoopEvent event;

    /**
     * Queue of steps waiting to be performed.
     */
    @Getter
    private final transient Deque<Step2> steps = new ArrayDeque<>(6);

    /**
     * Number of attempts, so far, for the current step.
     */
    @Getter
    private int attempts;

    /**
     * Policy currently being processed.
     */
    private Policy policy;

    /**
     * Result of the last policy operation.
     */
    @Getter
    @Setter
    private PolicyResult result = PolicyResult.SUCCESS;

    @ToString.Include
    private int numOnsets = 1;
    @ToString.Include
    private int numAbatements = 0;

    private VirtualControlLoopEvent abatement = null;

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

    private FinalResult finalResult = null;

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
    public UsecasesEventManager(ControlLoopParams params, VirtualControlLoopEvent event, WorkingMemory workMem)
                    throws ControlLoopException {

        super(params, event.getRequestId());

        checkEventSyntax(event);

        if (isClosedLoopDisabled(event)) {
            throw new IllegalStateException("is-closed-loop-disabled is set to true on VServer or VNF");
        }

        if (isProvStatusInactive(event)) {
            throw new IllegalStateException("prov-status is not ACTIVE on VServer or VNF");
        }

        this.event = event;
        this.workMem = workMem;
    }

    @Override
    public void destroy() {
        for (Step2 step : steps) {
            step.cancel();
        }

        super.destroy();
    }

    /**
     * Determines if the manager is still active.
     *
     * @return {@code true} if the manager is still active, {@code false} otherwise
     */
    @Override
    public boolean isActive() {
        return (super.isActive() && finalResult == null);
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

        if (!steps.isEmpty()) {
            throw new IllegalStateException("manager already started");
        }

        loadPolicy();
    }

    /**
     * Loads the next policy.
     *
     * @throws ControlLoopException if the processor cannot get a policy
     */
    public void loadNextPolicy() throws ControlLoopException {
        getProcessor().nextPolicyForResult(result);
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

        // convert policy payload from Map<String,String> to Map<String,Object>
        Map<String, Object> payload = new LinkedHashMap<>();
        if (policy.getPayload() != null) {
            payload.putAll(policy.getPayload());
        }

        // @formatter:off
        ControlLoopOperationParams params = ControlLoopOperationParams.builder()
                        .actorService(getActorService())
                        .actor(policy.getActor())
                        .operation(policy.getRecipe())
                        .requestId(event.getRequestId())
                        .executor(getExecutor())
                        .target(policy.getTarget())
                        .payload(payload)
                        .startCallback(this::onStart)
                        .completeCallback(this::onComplete)
                        .build();
        // @formatter:on

        // load the policy's operation
        steps.add(new Step2(this, params, event));
    }

    /**
     * Load the steps needed to preprocess the step that's at the front of the queue.
     */
    public void loadPreprocessorSteps() {
        if (steps.size() > MAX_STEPS) {
            throw new IllegalStateException("too many steps");
        }

        final Step2 step = steps.peek();

        step.init();

        // determine if any A&AI queries are needed
        boolean needCq = false;
        boolean needPnf = false;
        boolean needTenant = false;

        for (String propName : step.getPropertyNames()) {
            needCq = needCq || CQ_PROPERTIES.contains(propName);
            needPnf = needPnf || PNF_PROPERTIES.contains(propName);
            needTenant = needTenant || TENANT_PROPERTIES.contains(propName);
        }

        /*
         * The Policy's actual operation requires additional, implicit steps, such as
         * locking and guards.
         */
        final boolean needPolicySteps = step.isPolicyStep();


        /*
         * Note: the GUARD should be checked as the last step before the policy, thus it
         * should be "pushed" first
         */
        if (needPolicySteps) {
            steps.push(new GuardStep2(step, getClosedLoopControlName()));
        }

        // A&AI queries
        if (needCq) {
            steps.push(new AaiCqStep2(step));
        }

        if (needPnf) {
            steps.push(new AaiGetPnfStep2(step));
        }

        if (needTenant) {
            steps.push(new AaiGetTenantStep2(step));
        }

        /*
         * Note: the LOCK should be requested as the first step, thus it should be
         * "pushed" last.
         */
        if (needPolicySteps) {
            steps.push(new LockStep2(step));
        }
    }

    /**
     * Executes the first step in the queue.
     *
     * @return {@code true} if the step was started, {@code false} if it is no longer
     *         needed (or if the queue is empty)
     */
    public boolean executeStep() {
        attempts = 0;

        Step2 step = steps.peek();
        if (step == null) {
            return false;
        }

        return step.start(getEndTimeMs() - System.currentTimeMillis());
    }

    /**
     * Discards the current step, if any.
     */
    public void nextStep() {
        steps.poll();
    }

    /**
     * Increments the number of attempts.
     */
    public void bumpAttempts() {
        ++attempts;
    }

    /**
     * Adds the outcome to the history.
     *
     * @param outcome outcome to add
     */
    public void addOutcome(OperationOutcome outcome) {
        OperationOutcome2 last = partialHistory.peekLast();

        if (last != null && last.getOutcome().getEnd() == null
                        && last.getOutcome().isFor(outcome.getActor(), outcome.getOperation())) {
            // last item was a "start" - remove it
            partialHistory.removeLast();

            if (fullHistory.peekLast() == last) {
                fullHistory.removeLast();
            }
        }

        OperationOutcome2 outcome2 = new OperationOutcome2(outcome);
        partialHistory.add(outcome2);
        fullHistory.add(outcome2);
    }

    /**
     * Makes a notification message for the current operation.
     *
     * @return a new notification
     */
    public synchronized VirtualControlLoopNotification makeNotification() {
        VirtualControlLoopNotification notif = new VirtualControlLoopNotification(event);
        notif.setNotification(ControlLoopNotificationType.OPERATION);
        notif.setFrom("policy");
        notif.setPolicyScope(getPolicyScope());
        notif.setPolicyVersion(getPolicyVersion());

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
     * Makes a control loop response.
     *
     * @param outcome operation outcome
     * @return a new control loop response, or {@code null} if none is required
     */
    protected ControlLoopResponse makeControlLoopResponse(OperationOutcome outcome) {
        ControlLoopResponse clRsp = new ControlLoopResponse();
        clRsp.setFrom(outcome.getActor());
        clRsp.setTarget("DCAE");
        clRsp.setClosedLoopControlName(event.getClosedLoopControlName());
        clRsp.setPolicyName(event.getPolicyName());
        clRsp.setPolicyVersion(event.getPolicyVersion());
        clRsp.setRequestId(event.getRequestId());
        clRsp.setVersion(event.getVersion());

        Object obj = outcome.getResponse();
        if (!(obj instanceof PciMessage)) {
            return clRsp;
        }

        PciMessage msg = (PciMessage) obj;
        if (msg.getBody() != null && msg.getBody().getOutput() != null) {
            clRsp.setPayload(msg.getBody().getOutput().getPayload());
        }

        return clRsp;
    }

    /**
     * An event onset/abatement.
     *
     * @param newEvent the event
     * @return the status
     */
    public synchronized NewEventStatus onNewEvent(VirtualControlLoopEvent newEvent) {
        try {
            checkEventSyntax(newEvent);

            if (newEvent.getClosedLoopEventStatus() == ControlLoopEventStatus.ONSET) {
                if (newEvent.equals(event)) {
                    return NewEventStatus.FIRST_ONSET;
                }

                numOnsets++;
                return NewEventStatus.SUBSEQUENT_ONSET;

            } else {
                if (abatement == null) {
                    abatement = newEvent;
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
        return !(PROV_STATUS_ACTIVE.equals(aai.getOrDefault(VSERVER_PROV_STATUS, PROV_STATUS_ACTIVE))
                        && PROV_STATUS_ACTIVE.equals(aai.getOrDefault(GENERIC_VNF_PROV_STATUS, PROV_STATUS_ACTIVE)));
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
    protected class OperationOutcome2 {
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
            clOperation.setTarget(policy.getTarget().toString());

            if (outcome.getEnd() == null) {
                clOperation.setOutcome("Started");
            } else if (clOperation.getOutcome() == null) {
                clOperation.setOutcome("");
            }
        }
    }
}
