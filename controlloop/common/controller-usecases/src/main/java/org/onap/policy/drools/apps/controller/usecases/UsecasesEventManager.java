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

import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.drools.core.WorkingMemory;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopResponse;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.actorserviceprovider.OperationProperties;
import org.onap.policy.controlloop.actorserviceprovider.parameters.ControlLoopOperationParams;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.controlloop.eventmanager.ActorConstants;
import org.onap.policy.controlloop.eventmanager.ClEventManagerWithEvent;
import org.onap.policy.controlloop.eventmanager.StepContext;
import org.onap.policy.drools.apps.controller.usecases.step.AaiCqStep2;
import org.onap.policy.drools.apps.controller.usecases.step.AaiGetPnfStep2;
import org.onap.policy.drools.apps.controller.usecases.step.AaiGetTenantStep2;
import org.onap.policy.drools.apps.controller.usecases.step.GetTargetEntityStep2;
import org.onap.policy.drools.apps.controller.usecases.step.GuardStep2;
import org.onap.policy.drools.apps.controller.usecases.step.LockStep2;
import org.onap.policy.drools.apps.controller.usecases.step.Step2;
import org.onap.policy.sdnr.PciMessage;

/**
 * Manager for a single control loop event. Once this has been created, the event can be
 * retracted from working memory. Processing progresses through each policy, which
 * involves at least one step. As a step is processed, additional preprocessor steps may
 * be pushed onto the queue (e.g., locks, A&AI queries, guards).
 */
public class UsecasesEventManager extends ClEventManagerWithEvent<Step2> implements StepContext {

    private static final long serialVersionUID = -1216568161322872641L;

    /**
     * If there's a failure from one of these actors, then the TOSCA processing should be
     * aborted.
     */
    private static final Set<String> ABORT_ACTORS = Set.of(ActorConstants.CL_TIMEOUT_ACTOR, ActorConstants.LOCK_ACTOR);

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

        super(params, event, workMem);

        if (isClosedLoopDisabled(event)) {
            throw new IllegalStateException("is-closed-loop-disabled is set to true on VServer or VNF");
        }

        if (isProvStatusInactive(event)) {
            throw new IllegalStateException("prov-status is not ACTIVE on VServer or VNF");
        }
    }

    /*
     * This is needed to satisfy drools.
     */
    @Override
    public Deque<Step2> getSteps() {
        return super.getSteps();
    }

    /**
     * Loads the preprocessor steps needed by the step that's at the front of the queue.
     */
    public void loadPreprocessorSteps() {
        super.loadPreprocessorSteps();

        final Deque<Step2> steps = getSteps();
        final Step2 step = getSteps().peek();

        // determine if any A&AI queries are needed
        boolean needCq = false;
        boolean needPnf = false;
        boolean needTenant = false;
        boolean needTargetEntity = false;

        for (String propName : step.getPropertyNames()) {
            needCq = needCq || CQ_PROPERTIES.contains(propName);
            needPnf = needPnf || PNF_PROPERTIES.contains(propName);
            needTenant = needTenant || TENANT_PROPERTIES.contains(propName);
            needTargetEntity = needTargetEntity || OperationProperties.AAI_TARGET_ENTITY.equals(propName);
        }

        /*
         * The Policy's actual operation requires additional, implicit steps, such as
         * locking and guards.
         */
        final boolean needPolicySteps = step.isPolicyStep();


        /*
         * NOTE: need to push steps onto the queue in the OPPOSITE order in which they
         * must be performed.
         */


        // GUARD must be pushed first
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

        // LOCK must be pushed after the queries
        if (needPolicySteps) {
            steps.push(new LockStep2(step));
        }

        // GET-TARGET-ENTITY should be pushed last
        if (needTargetEntity) {
            steps.push(new GetTargetEntityStep2(step));
        }
    }

    /**
     * Determines if the TOSCA should be aborted due to the given outcome.
     *
     * @param outcome outcome to examine
     * @return {@code true} if the TOSCA should be aborted, {@code false} otherwise
     */
    public boolean isAbort(OperationOutcome outcome) {
        return (super.isAbort(outcome) && ABORT_ACTORS.contains(outcome.getActor()));
    }

    /**
     * Stores an operation outcome in the DB.
     *
     * @param outcome operation outcome to store
     */
    public void storeInDataBase(OperationOutcome2 outcome) {
        storeInDataBase(outcome, getProperty(OperationProperties.AAI_TARGET_ENTITY));
    }

    /**
     * Makes a control loop response.
     *
     * @param outcome operation outcome
     * @return a new control loop response, or {@code null} if none is required
     */
    public ControlLoopResponse makeControlLoopResponse(OperationOutcome outcome) {
        ControlLoopResponse clRsp = super.makeControlLoopResponse(outcome);

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
     * Check an event syntax.
     *
     * @param event the event syntax
     * @throws ControlLoopException if an error occurs
     */
    protected void checkEventSyntax(VirtualControlLoopEvent event) throws ControlLoopException {
        super.checkEventSyntax(event);
        validateAaiData(event);
    }

    @Override
    protected void validateTarget(VirtualControlLoopEvent event) throws ControlLoopException {
        super.validateTarget(event);

        if (!VALID_TARGETS.contains(event.getTarget().toLowerCase())) {
            throw new ControlLoopException("target field invalid");
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

    @Override
    protected void loadPolicyStep(ControlLoopOperationParams params) {
        getSteps().add(new Step2(this, params, getEvent()));
    }
}
