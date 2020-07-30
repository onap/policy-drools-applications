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
import static org.onap.policy.drools.apps.controller.usecases.UsecasesConstants.LOCK_ACTOR;
import static org.onap.policy.drools.apps.controller.usecases.UsecasesConstants.LOCK_OPERATION;
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
import org.onap.policy.aai.AaiCqResponse;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.ControlLoopResponse;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.actor.aai.AaiActor;
import org.onap.policy.controlloop.actor.aai.AaiCustomQueryOperation;
import org.onap.policy.controlloop.actor.aai.AaiGetPnfOperation;
import org.onap.policy.controlloop.actor.aai.AaiGetTenantOperation;
import org.onap.policy.controlloop.actor.guard.DecisionOperation;
import org.onap.policy.controlloop.actor.guard.GuardActor;
import org.onap.policy.controlloop.actorserviceprovider.Operation;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.OperationProperties;
import org.onap.policy.controlloop.actorserviceprovider.controlloop.ControlLoopEventContext;
import org.onap.policy.controlloop.actorserviceprovider.parameters.OperatorConfig;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.controlloop.eventmanager.ControlLoopEventManager;
import org.onap.policy.controlloop.eventmanager.Step;
import org.onap.policy.controlloop.eventmanager.StepContext;
import org.onap.policy.controlloop.policy.FinalResult;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.sdnr.PciMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for a single control loop event. Once this has been created, the event can be
 * retracted from working memory. Once this has been created, {@link #start()} should be
 * invoked, and then {@link #nextStep()} should be invoked continually until
 * {@link #isActive()} returns {@code false}, indicating that all steps have completed.
 */
@ToString(onlyExplicitlyIncluded = true)
public class UsecasesEventManager extends ControlLoopEventManager implements StepContext {
    private static final Logger logger = LoggerFactory.getLogger(UsecasesEventManager.class);
    private static final long serialVersionUID = -1216568161322872641L;

    /**
     * Maximum number of steps, for a single policy, allowed in the queue at a time.
     */
    private static final int MAX_STEPS = 100;

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
    private static final Set<String> TENANT_PROPERTIES = Set.of(OperationProperties.AAI_MODEL_TENANT);

    /**
     * Names of Operation properties for which A&AI custom query is needed.
     */
    private static final Set<String> CQ_PROPERTIES = Set.of(OperationProperties.AAI_MODEL_CLOUD_REGION,
                    OperationProperties.AAI_MODEL_INVARIANT_GENERIC_VNF, OperationProperties.AAI_MODEL_SERVICE,
                    OperationProperties.AAI_MODEL_VNF, OperationProperties.AAI_RESOURCE_SERVICE_INSTANCE,
                    OperationProperties.AAI_RESOURCE_VNF, OperationProperties.AAI_VSERVER_LINK,
                    UsecasesConstants.AAI_DEFAULT_GENERIC_VNF_ID);

    public enum State {
        LOAD_POLICY, POLICY_LOADED, AWAITING_OUTCOME, DONE
    }

    public enum NewEventStatus {
        FIRST_ONSET, SUBSEQUENT_ONSET, FIRST_ABATEMENT, SUBSEQUENT_ABATEMENT, SYNTAX_ERROR
    }

    // TODO remove this once operation properties are being used instead
    @Getter
    private final ControlLoopEventContext context;

    /**
     * Steps waiting to be performed.
     */
    protected final transient Deque<Step> steps = new ArrayDeque<>(6);

    /**
     * Number of attempts, so far, for the current step.
     */
    private int nattempts;

    /**
     * Policy currently being processed.
     */
    private Policy policy;

    /**
     * Result of last policy operation.
     */
    @Setter
    private PolicyResult result = PolicyResult.SUCCESS;

    @ToString.Include
    private int numOnsets = 1;
    @ToString.Include
    private int numAbatements = 0;
    private VirtualControlLoopEvent abatement = null;

    /**
     * Full history of operations that have been processed by the rules. This includes
     * {@link #partialHistory}. However, if the last item in {@link #partialHistory} is
     * incomplete, then it is not included.
     */
    @Getter
    private final transient Deque<OperationOutcome2> fullHistory = new LinkedList<>();

    /**
     * History of operations, for the current policy, that have been processed by the
     * rules. When a policy is started, its "start" outcome is added. However, once it
     * completes, its "start" outcome is removed and the "completed" outcome is added.
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

        this.context = new ControlLoopEventContext(event);
        this.workMem = workMem;
    }

    @Override
    public synchronized void destroy() {
        super.destroy();

        Step step = steps.peek();
        if (step != null) {
            step.cancel();
        }
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

        if (!steps.isEmpty()) {
            throw new IllegalStateException("manager already started");
        }

        loadNextPolicy();
    }

    /**
     * Load the steps needed to preprocess the next step.
     */
    public void loadPreprocessorSteps() {
        if (steps.size() > MAX_STEPS) {
            throw new IllegalStateException("too many steps");
        }

        final Step step = steps.peek();

        /*
         * The Policy's actual operation requires additional, implicit steps, such as
         * locking and guards. We know it's the policy operation if it's the only step in
         * the queue.
         */
        boolean needPolicySteps = (steps.size() == 1);

        /*
         * Depending on the preprocessor steps, it possible that the same operation may be
         * executed more than once. However, in their current incarnations, that will not
         * happen.
         */

        // determine if any A&AI queries are needed
        boolean needCq = false;
        boolean needPnf = false;
        boolean needTenant = false;

        for (String propName : step.getPolicyOperation().getPropertyNames()) {
            needCq = needCq || CQ_PROPERTIES.contains(propName);
            needPnf = needPnf || PNF_PROPERTIES.contains(propName);
            needTenant = needTenant || TENANT_PROPERTIES.contains(propName);
        }

        /*
         * Note: the GUARD should be checked as the last step before the policy, thus it
         * should be "pushed" first
         */
        if (needPolicySteps) {
            // TODO build payload for guard actor
            steps.push(new Step(GuardActor.NAME, DecisionOperation.NAME, step));
        }

        if (needCq) {
            steps.push(new AaiCqStep(step));
        }

        if (needPnf) {
            steps.push(new AaiGetPnfStep(step));
        }

        if (needTenant) {
            steps.push(new AaiGetTenantStep(step));
        }

        /*
         * Note: the LOCK should be requested as the first step, thus it should be
         * "pushed" last.
         */
        if (needPolicySteps) {
            steps.push(new LockStep(step));
        }
    }

    /**
     * Determines if the step is for the policy's actual operation (as opposed to a
     * preprocessing step).
     *
     * @return {@code true} if the step is the policy's step, {@code false} if it's a
     *         preprocessing step
     */
    public boolean isPolicyStep() {
        return (steps.size() == 1);
    }

    /**
     * Determines if the next step is initialized.
     *
     * @return {@code true} if the next step is initialized, {@code false} otherwise
     */
    public boolean nextStepIsInitialized() {
        return (!steps.isEmpty() && steps.peek().isInitialized());
    }

    /**
     * Executes the first step in the queue.
     *
     * @return {@code true} if the step was started, {@code false} if it is no longer
     *         needed
     */
    public boolean executeStep() {
        nattempts = 0;

        final Step step = steps.peek();

        // TODO the operation properties should be loaded here

        return step.start(getEndTimeMs() - System.currentTimeMillis());
    }

    /**
     * Discards the current step.
     */
    public void nextStep() {
        steps.remove();
    }

    /**
     * Increments the number of attempts.
     */
    public void bumpAttempts() {
        ++nattempts;
    }

    /**
     * Loads the next policy.
     *
     * @throws ControlLoopException if the processor cannot get a policy
     */
    public void loadNextPolicy() throws ControlLoopException {

        getProcessor().nextPolicyForResult(result);

        if ((finalResult = getProcessor().checkIsCurrentPolicyFinal()) != null) {
            // final policy - nothing more to do
            return;
        }

        policy = getProcessor().getCurrentPolicy();

        clearStartTime();
        partialHistory.clear();

        // load the policy's operation
        steps.add(new Step(this, policy));
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
        }

        OperationOutcome2 outcome2 = new OperationOutcome2(outcome);
        partialHistory.add(outcome2);

        if (outcome.getEnd() != null) {
            // it's a completion - add to full history, too
            fullHistory.add(outcome2);
        }
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

        VirtualControlLoopEvent event = context.getEvent();

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

    @Override
    public void abort(OperationOutcome outcome) {
        super.abort(outcome);
        workMem.update(factHandle, this);
    }

    public class AaiCqStep extends Step {
        public AaiCqStep(Step otherStep) {
            super(AaiActor.NAME, AaiCustomQueryOperation.NAME, otherStep);
        }

        @Override
        public boolean start(long remainingMs) {
            if (context.contains(AaiCqResponse.CONTEXT_KEY)) {
                // already have the data
                return false;
            }

            return super.start(remainingMs);
        }
    }

    public class AaiGetPnfStep extends Step {
        public AaiGetPnfStep(Step otherStep) {
            super(AaiActor.NAME, AaiGetPnfOperation.NAME, otherStep);
        }

        @Override
        public boolean start(long remainingMs) {
            if (context.contains(
                            AaiGetTenantOperation.getKey(AaiGetPnfOperation.getKey(getParams().getTargetEntity())))) {
                // already have the data
                return false;
            }

            return super.start(remainingMs);
        }
    }

    public class AaiGetTenantStep extends Step {
        public AaiGetTenantStep(Step otherStep) {
            super(AaiActor.NAME, AaiGetTenantOperation.NAME, otherStep);
        }

        @Override
        public boolean start(long remainingMs) {
            String vserver = context.getEnrichment().get(VSERVER_VSERVER_NAME);
            if (StringUtils.isBlank(vserver)) {
                throw new IllegalArgumentException("missing " + VSERVER_VSERVER_NAME + " in enrichment data");
            }

            if (context.contains(AaiGetTenantOperation.getKey(vserver))) {
                // already have the data
                return false;
            }

            return super.start(remainingMs);
        }
    }

    public class LockStep extends Step {
        public LockStep(Step otherStep) {
            super(LOCK_ACTOR, LOCK_OPERATION, otherStep);
        }

        @Override
        protected Operation buildOperation() {
            return new LockOperation(UsecasesEventManager.this, getParams(), new OperatorConfig(getBlockingExecutor()));
        }
    }

    @Getter
    @ToString
    private class OperationOutcome2 {
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
            this.attempt = nattempts;

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
