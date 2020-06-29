/*-
 * ============LICENSE_START=======================================================
 * controlloop event manager
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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.aai.AaiCqResponse;
import org.onap.policy.aai.AaiManager;
import org.onap.policy.aai.util.AaiException;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.policy.FinalResult;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.processor.ControlLoopProcessor;
import org.onap.policy.drools.core.lock.Lock;
import org.onap.policy.drools.core.lock.LockCallback;
import org.onap.policy.drools.core.lock.LockImpl;
import org.onap.policy.drools.core.lock.LockState;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.rest.RestManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlLoopEventManager implements Serializable {
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

    public static final String AAI_URL = "aai.url";
    public static final String AAI_USERNAME_PROPERTY = "aai.username";
    public static final String AAI_PASS_PROPERTY = "aai.password";


    /**
     * Additional time, in seconds, to add to a "lock" request. This ensures that the lock won't expire right before an
     * operation completes.
     */
    private static final int ADDITIONAL_LOCK_SEC = 60;

    private static final Logger logger = LoggerFactory.getLogger(ControlLoopEventManager.class);

    private static final long serialVersionUID = -1216568161322872641L;

    private static final Set<String> VALID_TARGETS;

    static {
        VALID_TARGETS = Collections.unmodifiableSet(new HashSet<>(
                Stream.of(VM_NAME, VNF_NAME, VSERVER_VSERVER_NAME, GENERIC_VNF_VNF_ID, GENERIC_VNF_VNF_NAME, PNF_NAME)
                        .map(String::toLowerCase).collect(Collectors.toList())));
    }

    public final String closedLoopControlName;
    private final UUID requestId;

    private String controlLoopResult;
    private ControlLoopProcessor processor = null;
    private VirtualControlLoopEvent onset;
    private Integer numOnsets = 0;
    private Integer numAbatements = 0;
    private VirtualControlLoopEvent abatement;
    private FinalResult controlLoopTimedOut = null;

    private boolean isActivated = false;
    private LinkedList<ControlLoopOperation> controlLoopHistory = new LinkedList<>();
    private ControlLoopOperationManager currentOperation = null;
    private ControlLoopOperationManager lastOperationManager = null;
    private transient Lock targetLock = null;
    private boolean useTargetLock = true;

    /**
     * Constructs the object.
     *
     * @param closedLoopControlName name of the control loop
     * @param requestId ID of the request with which this manager is associated
     */
    public ControlLoopEventManager(String closedLoopControlName, UUID requestId) {
        this.closedLoopControlName = closedLoopControlName;
        this.requestId = requestId;
    }

    public String getClosedLoopControlName() {
        return closedLoopControlName;
    }

    public String getControlLoopResult() {
        return controlLoopResult;
    }

    public void setControlLoopResult(String controlLoopResult) {
        this.controlLoopResult = controlLoopResult;
    }

    public Integer getNumOnsets() {
        return numOnsets;
    }

    public void setNumOnsets(Integer numOnsets) {
        this.numOnsets = numOnsets;
    }

    public Integer getNumAbatements() {
        return numAbatements;
    }

    public void setNumAbatements(Integer numAbatements) {
        this.numAbatements = numAbatements;
    }

    public boolean isActivated() {
        return isActivated;
    }

    public void setActivated(boolean isActivated) {
        this.isActivated = isActivated;
    }

    public boolean useTargetLock() {
        return useTargetLock();
    }

    public void setUseTargetLock(boolean useTargetLock) {
        this.useTargetLock = useTargetLock;
    }

    public VirtualControlLoopEvent getOnsetEvent() {
        return this.onset;
    }

    public VirtualControlLoopEvent getAbatementEvent() {
        return this.abatement;
    }

    public ControlLoopProcessor getProcessor() {
        return this.processor;
    }

    public UUID getRequestId() {
        return requestId;
    }


    private VirtualControlLoopNotification rejectNotification(VirtualControlLoopEvent event, String message) {
        VirtualControlLoopNotification notification = new VirtualControlLoopNotification(event);
        notification.setNotification(ControlLoopNotificationType.REJECTED);
        notification.setMessage(message);
        return notification;
    }

    /**
     * Preactivations check for an event.
     *
     * @param event the event
     * @return the VirtualControlLoopNotification
     */
    private VirtualControlLoopNotification preActivationChecks(VirtualControlLoopEvent event) {
        try {
            //
            // This method should ONLY be called ONCE
            //
            if (this.isActivated) {
                return rejectNotification(event, "ControlLoopEventManager has already been activated.");
            }

            //
            // Syntax check the event
            //
            checkEventSyntax(event);
        } catch (ControlLoopException e) {
            logger.warn("{}: invalid event syntax: ", this, e);
            return rejectNotification(event, e.getMessage());

        }

        return new VirtualControlLoopNotification(event);
    }

    /**
     * Activate a control loop event.
     *
     * @param event the event
     * @return the VirtualControlLoopNotification
     */
    public VirtualControlLoopNotification activate(VirtualControlLoopEvent event) {
        VirtualControlLoopNotification notification = preActivationChecks(event);
        if (notification.getNotification() == ControlLoopNotificationType.REJECTED) {
            return notification;
        }

        return postActivate(event, notification);
    }

    /**
     * Activate a control loop event.
     *
     * @param yamlSpecification the yaml specification
     * @param event the event
     * @return the VirtualControlLoopNotification
     */
    public VirtualControlLoopNotification activate(String yamlSpecification, VirtualControlLoopEvent event) {
        VirtualControlLoopNotification notification = preActivationChecks(event);
        if (notification.getNotification() == ControlLoopNotificationType.REJECTED) {
            return notification;
        }

        if (yamlSpecification == null || yamlSpecification.length() < 1) {
            return rejectNotification(event, "yaml specification is null or 0 length");
        }

        String decodedYaml = null;
        try {
            decodedYaml = URLDecoder.decode(yamlSpecification, "UTF-8");
            if (decodedYaml != null && decodedYaml.length() > 0) {
                yamlSpecification = decodedYaml;
            }
        } catch (UnsupportedEncodingException e) {
            logger.warn("{}: YAML decode in activate by YAML specification and event threw: ", this, e);
            return rejectNotification(event, e.getMessage());
        }

        try {
            //
            // Parse the YAML specification
            //
            this.processor = new ControlLoopProcessor(yamlSpecification);
        } catch (ControlLoopException e) {
            logger.error("{}: activate by YAML specification and event threw: ", this, e);
            return rejectNotification(event, e.getMessage());
        }

        return postActivate(event, notification);
    }

    /**
     * Activate a control loop event.
     *
     * @param toscaPolicy the tosca policy
     * @param event the event
     * @return the VirtualControlLoopNotification
     */
    public VirtualControlLoopNotification activate(ToscaPolicy toscaPolicy, VirtualControlLoopEvent event) {
        VirtualControlLoopNotification notification = preActivationChecks(event);
        if (notification.getNotification() == ControlLoopNotificationType.REJECTED) {
            return notification;
        }

        try {
            this.processor = new ControlLoopProcessor(toscaPolicy);
        } catch (ControlLoopException e) {
            logger.error("{}: activate from Tosca Policy threw: ", this, e);
            return rejectNotification(event, e.getMessage());
        }

        return postActivate(event, notification);
    }

    private VirtualControlLoopNotification postActivate(
            VirtualControlLoopEvent event, VirtualControlLoopNotification notification) {
        //
        // At this point we are good to go with this event
        //
        this.onset = event;
        this.numOnsets = 1;

        //
        // Set ourselves as active
        //
        this.isActivated = true;

        notification.setNotification(ControlLoopNotificationType.ACTIVE);
        return notification;
    }

    /**
     * Check if the control loop is final.
     *
     * @return a VirtualControlLoopNotification if the control loop is final, otherwise <code>null</code> is returned
     * @throws ControlLoopException if an error occurs
     */
    public VirtualControlLoopNotification isControlLoopFinal() throws ControlLoopException {
        validateFinalControlLoop();
        //
        // Ok, start creating the notification
        //
        VirtualControlLoopNotification notification = new VirtualControlLoopNotification(this.onset);
        //
        // Check if the overall control loop has timed out
        //
        if (this.isControlLoopTimedOut()) {
            //
            // Yes we have timed out
            //
            notification.setNotification(ControlLoopNotificationType.FINAL_FAILURE);
            notification.setMessage("Control Loop timed out");
            notification.getHistory().addAll(this.controlLoopHistory);
            return notification;
        }
        //
        // Check if the current policy is Final
        //
        FinalResult result = this.processor.checkIsCurrentPolicyFinal();
        if (result == null) {
            //
            // we are not at a final result
            //
            return null;
        }

        switch (result) {
            case FINAL_FAILURE_EXCEPTION:
                notification.setNotification(ControlLoopNotificationType.FINAL_FAILURE);
                notification.setMessage("Exception in processing closed loop");
                break;
            case FINAL_FAILURE:
            case FINAL_FAILURE_RETRIES:
            case FINAL_FAILURE_TIMEOUT:
            case FINAL_FAILURE_GUARD:
                notification.setNotification(ControlLoopNotificationType.FINAL_FAILURE);
                break;
            case FINAL_OPENLOOP:
                notification.setNotification(ControlLoopNotificationType.FINAL_OPENLOOP);
                break;
            case FINAL_SUCCESS:
                notification.setNotification(ControlLoopNotificationType.FINAL_SUCCESS);
                break;
            default:
                return null;
        }
        //
        // Be sure to add all the history
        //
        notification.getHistory().addAll(this.controlLoopHistory);
        return notification;
    }

    private void validateFinalControlLoop() throws ControlLoopException {
        //
        // Check if they activated us
        //
        if (!this.isActivated) {
            throw new ControlLoopException("ControlLoopEventManager MUST be activated first.");
        }
        //
        // Make sure we are expecting this call.
        //
        if (this.onset == null) {
            throw new ControlLoopException("No onset event for ControlLoopEventManager.");
        }
    }

    /**
     * Process the control loop.
     *
     * @return a ControlLoopOperationManager
     * @throws ControlLoopException if an error occurs
     */
    public ControlLoopOperationManager processControlLoop() throws ControlLoopException {
        validateFinalControlLoop();
        //
        // Is there a current operation?
        //
        if (this.currentOperation != null) {
            //
            // Throw an exception, or simply return the current operation?
            //
            throw new ControlLoopException("Already working an Operation, do not call this method.");
        }
        //
        // Ensure we are not FINAL
        //
        VirtualControlLoopNotification notification = this.isControlLoopFinal();
        if (notification != null) {
            //
            // This is weird, we require them to call the isControlLoopFinal() method first
            //
            // We should really abstract this and avoid throwing an exception, because it really
            // isn't an exception.
            //
            throw new ControlLoopException("Control Loop is in FINAL state, do not call this method.");
        }
        //
        // Not final so get the policy that needs to be worked on.
        //
        Policy policy = this.processor.getCurrentPolicy();
        if (policy == null) {
            throw new ControlLoopException("ControlLoopEventManager: processor came upon null Policy.");
        }
        //
        // And setup an operation
        //
        this.lastOperationManager = this.currentOperation;
        this.currentOperation = new ControlLoopOperationManager(this.onset, policy, this);
        //
        // Return it
        //
        return this.currentOperation;
    }

    /**
     * Finish an operation.
     *
     * @param operation the operation
     */
    public void finishOperation(ControlLoopOperationManager operation) throws ControlLoopException {
        //
        // Verify we have a current operation
        //
        if (this.currentOperation != null) {
            //
            // Validate they are finishing the current operation
            // PLD - this is simply comparing the policy. Do we want to equals the whole object?
            //
            if (this.currentOperation.policy.equals(operation.policy)) {
                logger.debug("Finishing {} result is {}", this.currentOperation.policy.getRecipe(),
                        this.currentOperation.getOperationResult());
                //
                // Save history
                //
                this.controlLoopHistory.addAll(this.currentOperation.getHistory());
                //
                // Move to the next Policy
                //
                this.processor.nextPolicyForResult(this.currentOperation.getOperationResult());
                //
                // Just null this out
                //
                this.lastOperationManager = this.currentOperation;
                this.currentOperation = null;

                //
                // Don't release the lock - it may be re-used by the next operation
                //

                return;
            }
            logger.debug("Cannot finish current operation {} does not match given operation {}",
                    this.currentOperation.policy, operation.policy);
            return;
        }
        throw new ControlLoopException("No operation to finish.");
    }

    /**
     * Obtain a lock for the current operation.
     *
     * @param callback call-back to be invoked when the lock state changes
     * @return a pair containing the old lock and the new lock, either of which may be null
     * @throws ControlLoopException if an error occurs
     */
    public synchronized Pair<Lock, Lock> lockCurrentOperation(LockCallback callback) throws ControlLoopException {
        //
        // Sanity check
        //
        if (this.currentOperation == null) {
            throw new ControlLoopException("Do not have a current operation.");
        }

        //
        // Release the old lock if it's for a different resource.
        //
        Lock oldLock = null;
        if (this.targetLock != null
                        && !this.targetLock.getResourceId().equals(this.currentOperation.getTargetEntity())) {
            logger.debug("{}: different resource - releasing old lock", getClosedLoopControlName());
            oldLock = this.targetLock;
            this.targetLock = null;
        }

        // keep the lock a little longer than the operation, including retries
        int optimeout = Math.max(1, this.currentOperation.getOperationTimeout());
        int nattempts = 1 + Math.max(0, this.currentOperation.getMaxRetries());
        int holdSec = optimeout * nattempts + ADDITIONAL_LOCK_SEC;

        //
        // Have we acquired it already?
        //
        if (this.targetLock != null) {
            // we have the lock - just extend it
            this.targetLock.extend(holdSec, callback);
            return Pair.of(oldLock, null);

        } else if (this.useTargetLock) {
            this.targetLock = createRealLock(this.currentOperation.getTargetEntity(), this.onset.getRequestId(),
                            holdSec, callback);
            return Pair.of(oldLock, this.targetLock);

        } else {
            // Not using target locks - create a lock w/o actually locking.
            logger.debug("{}: not using target locking; using pseudo locks", getClosedLoopControlName());
            this.targetLock = createPseudoLock(this.currentOperation.getTargetEntity(), this.onset.getRequestId(),
                            holdSec, callback);

            // Note: no need to invoke callback, as the lock is already ACTIVE

            return Pair.of(oldLock, this.targetLock);
        }
    }

    /**
     * Releases the lock for the current operation, deleting it from working memory.
     *
     * @return the lock, if the operation was locked, {@code null} otherwise
     */
    public synchronized Lock unlockCurrentOperation() {
        if (this.targetLock == null) {
            return null;
        }

        Lock lock = this.targetLock;
        this.targetLock = null;

        lock.free();

        return lock;
    }

    public enum NewEventStatus {
        FIRST_ONSET, SUBSEQUENT_ONSET, FIRST_ABATEMENT, SUBSEQUENT_ABATEMENT, SYNTAX_ERROR;
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
            this.checkEventSyntax(event);
            if (event.getClosedLoopEventStatus() == ControlLoopEventStatus.ONSET) {
                //
                // Check if this is our original ONSET
                //
                if (event.equals(this.onset)) {
                    //
                    // DO NOT retract it
                    //
                    return NewEventStatus.FIRST_ONSET;
                }
                //
                // Log that we got an onset
                //
                this.numOnsets++;
                return NewEventStatus.SUBSEQUENT_ONSET;
            } else if (event.getClosedLoopEventStatus() == ControlLoopEventStatus.ABATED) {
                //
                // Have we already got an abatement?
                //
                if (this.abatement == null) {
                    //
                    // Save this
                    //
                    this.abatement = event;
                    //
                    // Keep track that we received another
                    //
                    this.numAbatements++;
                    //
                    //
                    //
                    return NewEventStatus.FIRST_ABATEMENT;
                } else {
                    //
                    // Keep track that we received another
                    //
                    this.numAbatements++;
                    //
                    //
                    //
                    return NewEventStatus.SUBSEQUENT_ABATEMENT;
                }
            }
        } catch (ControlLoopException e) {
            logger.error("{}: onNewEvent threw: ", this, e);
        }
        return NewEventStatus.SYNTAX_ERROR;
    }


    /**
     * Commit the abatement to the history database.
     *
     * @param message the abatement message
     * @param outcome the abatement outcome
     */
    public void commitAbatement(String message, String outcome) {
        if (this.lastOperationManager == null) {
            logger.error("{}: commitAbatement: no operation manager", this);
            return;
        }
        try {
            this.lastOperationManager.commitAbatement(message, outcome);
        } catch (NoSuchElementException e) {
            logger.error("{}: commitAbatement threw an exception ", this, e);
        }
    }


    /**
     * Set the control loop time out.
     *
     * @return a VirtualControlLoopNotification
     */
    public VirtualControlLoopNotification setControlLoopTimedOut() {
        this.controlLoopTimedOut = FinalResult.FINAL_FAILURE_TIMEOUT;
        VirtualControlLoopNotification notification = new VirtualControlLoopNotification(this.onset);
        notification.setNotification(ControlLoopNotificationType.FINAL_FAILURE);
        notification.setMessage("Control Loop timed out");
        notification.getHistory().addAll(this.controlLoopHistory);
        return notification;
    }

    public boolean isControlLoopTimedOut() {
        return (this.controlLoopTimedOut == FinalResult.FINAL_FAILURE_TIMEOUT);
    }

    /**
     * Get the control loop timeout.
     *
     * @param defaultTimeout the default timeout
     * @return the timeout
     */
    public int getControlLoopTimeout(Integer defaultTimeout) {
        if (this.processor != null && this.processor.getControlLoop() != null) {
            Integer timeout = this.processor.getControlLoop().getTimeout();
            if (timeout != null && timeout > 0) {
                return timeout;
            }
        }
        if (defaultTimeout != null) {
            return defaultTimeout;
        }
        return 0;
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
            throw new ControlLoopException("target field invalid - expecting VM_NAME or VNF_NAME");
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
     * @return <code>true</code> if the control loop is disabled, <code>false</code> otherwise
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
     * @return {@code true} if the provisioning status is neither ACTIVE nor {@code null}, {@code false} otherwise
     */
    protected static boolean isProvStatusInactive(VirtualControlLoopEvent event) {
        Map<String, String> aai = event.getAai();
        return (!PROV_STATUS_ACTIVE.equals(aai.getOrDefault(VSERVER_PROV_STATUS, PROV_STATUS_ACTIVE))
                || !PROV_STATUS_ACTIVE.equals(aai.getOrDefault(GENERIC_VNF_PROV_STATUS, PROV_STATUS_ACTIVE)));
    }

    /**
     * Determines the boolean value represented by the given AAI field value.
     *
     * @param aaiValue value to be examined
     * @return the boolean value represented by the field value, or {@code false} if the value is {@code null}
     */
    protected static boolean isAaiTrue(String aaiValue) {
        return ("true".equalsIgnoreCase(aaiValue) || "T".equalsIgnoreCase(aaiValue) || "yes".equalsIgnoreCase(aaiValue)
                || "Y".equalsIgnoreCase(aaiValue));
    }

    @Override
    public String toString() {
        return "ControlLoopEventManager [closedLoopControlName=" + closedLoopControlName + ", requestId=" + requestId
                + ", processor=" + processor + ", onset=" + (onset != null ? onset.getRequestId() : "null")
                + ", numOnsets=" + numOnsets + ", numAbatements=" + numAbatements + ", isActivated=" + isActivated
                + ", currentOperation=" + currentOperation + ", targetLock=" + targetLock + "]";
    }

    /**
     * This function calls Aai Custom Query and responds with the AaiCqResponse.
     *
     * @param event input event
     * @return AaiCqResponse Response from Aai for custom query. Can not be null.
     * @throws AaiException if error occurs
     */
    public AaiCqResponse getCqResponse(VirtualControlLoopEvent event) throws AaiException {

        Map<String, String> aai = event.getAai();

        if (aai.containsKey(VSERVER_IS_CLOSED_LOOP_DISABLED) || aai.containsKey(GENERIC_VNF_IS_CLOSED_LOOP_DISABLED)) {

            if (isClosedLoopDisabled(event)) {
                throw new AaiException("is-closed-loop-disabled is set to true on VServer or VNF");
            }

            if (isProvStatusInactive(event)) {
                throw new AaiException("prov-status is not ACTIVE on VServer or VNF");
            }
        }

        if (!aai.containsKey(VSERVER_VSERVER_NAME)) {
            throw new AaiException("Vserver name is missing");
        }

        UUID reqId = event.getRequestId();
        AaiCqResponse response = null;
        String vserverId = event.getAai().get(VSERVER_VSERVER_NAME);

        String aaiHostUrl = PolicyEngineConstants.getManager().getEnvironmentProperty(AAI_URL);
        String aaiUser = PolicyEngineConstants.getManager().getEnvironmentProperty(AAI_USERNAME_PROPERTY);
        String aaiPassword = PolicyEngineConstants.getManager().getEnvironmentProperty(AAI_PASS_PROPERTY);

        response = new AaiManager(new RestManager()).getCustomQueryResponse(aaiHostUrl, aaiUser, aaiPassword, reqId,
                vserverId);

        if (response == null) {
            throw new AaiException("Target vnf-id could not be found");
        }

        return response;

    }

    /**
     * Get the specified pnf data from aai.
     * @param event the event containing pnf id.
     * @return pnf key value data.
     * @throws AaiException if an aai error occurs.
     */
    public Map<String, String> getPnf(VirtualControlLoopEvent event) throws AaiException {
        Map<String, String> aai = event.getAai();

        if (!aai.containsKey(PNF_NAME)) {
            throw new AaiException("Missing unique identifier for PNF AAI object in the event.");
        }

        UUID reqId = event.getRequestId();
        String pnfName = event.getAai().get(PNF_NAME);
        String aaiHostUrl = PolicyEngineConstants.getManager().getEnvironmentProperty(AAI_URL);
        String aaiUser = PolicyEngineConstants.getManager().getEnvironmentProperty(AAI_USERNAME_PROPERTY);
        String aaiPassword = PolicyEngineConstants.getManager().getEnvironmentProperty(AAI_PASS_PROPERTY);

        Map<String, String> pnfParams =
                new AaiManager(new RestManager()).getPnf(aaiHostUrl, aaiUser, aaiPassword, reqId, pnfName);

        if (pnfParams == null) {
            throw new AaiException("Aai response is undefined");
        }
        return pnfParams;
    }


    // the following methods may be overridden by junit tests

    protected Lock createRealLock(String targetEntity, UUID requestId, int holdSec, LockCallback callback) {
        return PolicyEngineConstants.getManager().createLock(targetEntity, requestId.toString(), holdSec, callback,
                        false);
    }

    // note: the "callback" is required, because it will be invoked when lock.extend() is
    // invoked
    protected Lock createPseudoLock(String targetEntity, UUID requestId, int holdSec, LockCallback callback) {
        return new LockImpl(LockState.ACTIVE, targetEntity, requestId.toString(), holdSec, callback);
    }
}
