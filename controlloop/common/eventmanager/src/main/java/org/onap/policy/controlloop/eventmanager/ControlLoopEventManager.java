/*-
 * ============LICENSE_START=======================================================
 * controlloop event manager
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.onap.policy.aai.AaiGetVnfResponse;
import org.onap.policy.aai.AaiGetVserverResponse;
import org.onap.policy.aai.AaiManager;
import org.onap.policy.aai.AaiNqVServer;
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
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.guard.GuardResult;
import org.onap.policy.guard.LockCallback;
import org.onap.policy.guard.PolicyGuard;
import org.onap.policy.guard.PolicyGuard.LockResult;
import org.onap.policy.guard.TargetLock;
import org.onap.policy.rest.RESTManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlLoopEventManager implements LockCallback, Serializable {
    public static final String PROV_STATUS_ACTIVE = "ACTIVE";
    private static final String VM_NAME = "VM_NAME";
    private static final String VNF_NAME = "VNF_NAME";
    public static final String GENERIC_VNF_VNF_ID = "generic-vnf.vnf-id";
    public static final String GENERIC_VNF_VNF_NAME = "generic-vnf.vnf-name";
    public static final String VSERVER_VSERVER_NAME = "vserver.vserver-name";
    public static final String GENERIC_VNF_IS_CLOSED_LOOP_DISABLED = "generic-vnf.is-closed-loop-disabled";
    public static final String VSERVER_IS_CLOSED_LOOP_DISABLED = "vserver.is-closed-loop-disabled";
    public static final String GENERIC_VNF_PROV_STATUS = "generic-vnf.prov-status";
    public static final String VSERVER_PROV_STATUS = "vserver.prov-status";

    private static final String QUERY_AAI_ERROR_MSG = "Exception from queryAai: ";

    /**
     * Additional time, in seconds, to add to a "lock" request. This ensures that the lock
     * won't expire right before an operation completes.
     */
    private static final int ADDITIONAL_LOCK_SEC = 60;

    private static final Logger logger = LoggerFactory.getLogger(ControlLoopEventManager.class);

    private static final long serialVersionUID = -1216568161322872641L;
    public final String closedLoopControlName;
    public final UUID requestID;

    private String controlLoopResult;
    private transient ControlLoopProcessor processor = null;
    private VirtualControlLoopEvent onset;
    private Integer numOnsets = 0;
    private Integer numAbatements = 0;
    private VirtualControlLoopEvent abatement;
    private FinalResult controlLoopTimedOut = null;

    private boolean isActivated = false;
    private LinkedList<ControlLoopOperation> controlLoopHistory = new LinkedList<>();
    private ControlLoopOperationManager currentOperation = null;
    private transient TargetLock targetLock = null;
    private AaiGetVnfResponse vnfResponse = null;
    private AaiGetVserverResponse vserverResponse = null;

    private static Collection<String> requiredAAIKeys = new ArrayList<>();

    static {
        requiredAAIKeys.add("AICVServerSelfLink");
        requiredAAIKeys.add("AICIdentity");
        requiredAAIKeys.add("is_closed_loop_disabled");
        requiredAAIKeys.add(VM_NAME);
    }

    public ControlLoopEventManager(String closedLoopControlName, UUID requestID) {
        this.closedLoopControlName = closedLoopControlName;
        this.requestID = requestID;
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

    public VirtualControlLoopEvent getOnsetEvent() {
        return this.onset;
    }

    public VirtualControlLoopEvent getAbatementEvent() {
        return this.abatement;
    }

    public ControlLoopProcessor getProcessor() {
        return this.processor;
    }

    public UUID getRequestID() {
        return requestID;
    }

    /**
     * Activate a control loop event.
     *
     * @param event the event
     * @return the VirtualControlLoopNotification
     */
    public VirtualControlLoopNotification activate(VirtualControlLoopEvent event) {
        VirtualControlLoopNotification notification = new VirtualControlLoopNotification(event);
        try {
            //
            // This method should ONLY be called ONCE
            //
            if (this.isActivated) {
                throw new ControlLoopException("ControlLoopEventManager has already been activated.");
            }
            //
            // Syntax check the event
            //
            checkEventSyntax(event);

            //
            // At this point we are good to go with this event
            //
            this.onset = event;
            this.numOnsets = 1;
            //
            notification.setNotification(ControlLoopNotificationType.ACTIVE);
            //
            // Set ourselves as active
            //
            this.isActivated = true;
        } catch (ControlLoopException e) {
            logger.error("{}: activate by event threw: ", this, e);
            notification.setNotification(ControlLoopNotificationType.REJECTED);
            notification.setMessage(e.getMessage());
        }
        return notification;
    }

    /**
     * Activate a control loop event.
     *
     * @param yamlSpecification the yaml specification
     * @param event the event
     * @return the VirtualControlLoopNotification
     */
    public VirtualControlLoopNotification activate(String yamlSpecification, VirtualControlLoopEvent event) {
        VirtualControlLoopNotification notification = new VirtualControlLoopNotification(event);
        try {
            //
            // This method should ONLY be called ONCE
            //
            if (this.isActivated) {
                throw new ControlLoopException("ControlLoopEventManager has already been activated.");
            }
            //
            // Syntax check the event
            //
            checkEventSyntax(event);

            //
            // Check the YAML
            //
            if (yamlSpecification == null || yamlSpecification.length() < 1) {
                throw new ControlLoopException("yaml specification is null or 0 length");
            }
        } catch (ControlLoopException e) {
            logger.error("{}: activate by YAML specification and event threw: ", this, e);
            notification.setNotification(ControlLoopNotificationType.REJECTED);
            notification.setMessage(e.getMessage());
            return notification;
        }

        String decodedYaml = null;
        try {
            decodedYaml = URLDecoder.decode(yamlSpecification, "UTF-8");
            if (decodedYaml != null && decodedYaml.length() > 0) {
                yamlSpecification = decodedYaml;
            }
        } catch (UnsupportedEncodingException e) {
            logger.error("{}: YAML decode in activate by YAML specification and event threw: ", this, e);
            notification.setNotification(ControlLoopNotificationType.REJECTED);
            notification.setMessage(e.getMessage());
            return notification;
        }

        try {
            //
            // Parse the YAML specification
            //
            this.processor = new ControlLoopProcessor(yamlSpecification);

            //
            // At this point we are good to go with this event
            //
            this.onset = event;
            this.numOnsets = 1;
            //
            //
            //
            notification.setNotification(ControlLoopNotificationType.ACTIVE);
            //
            // Set ourselves as active
            //
            this.isActivated = true;
        } catch (ControlLoopException e) {
            logger.error("{}: activate by YAML specification and event threw: ", this, e);
            notification.setNotification(ControlLoopNotificationType.REJECTED);
            notification.setMessage(e.getMessage());
        }
        return notification;
    }

    /**
     * Check if the control loop is final.
     *
     * @return a VirtualControlLoopNotification if the control loop is final, otherwise
     *         <code>null</code> is returned
     * @throws ControlLoopException if an error occurs
     */
    public VirtualControlLoopNotification isControlLoopFinal() throws ControlLoopException {
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

    /**
     * Process the control loop.
     *
     * @return a ControlLoopOperationManager
     * @throws ControlLoopException if an error occurs
     * @throws AaiException if an error occurs retrieving information from A&AI
     */
    public ControlLoopOperationManager processControlLoop() throws ControlLoopException, AaiException {
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
                this.currentOperation = null;
                //
                // TODO: Release our lock
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
     * @return the lock result
     * @throws ControlLoopException if an error occurs
     */
    public synchronized LockResult<GuardResult, TargetLock> lockCurrentOperation() throws ControlLoopException {
        //
        // Sanity check
        //
        if (this.currentOperation == null) {
            throw new ControlLoopException("Do not have a current operation.");
        }
        //
        // Have we acquired it already?
        //
        if (this.targetLock != null) {
            //
            // TODO: Make sure the current lock is for the same target.
            // Currently, it should be. But in the future it may not.
            //
            GuardResult result = PolicyGuard.lockTarget(targetLock,
                            this.currentOperation.getOperationTimeout() + ADDITIONAL_LOCK_SEC);
            return new LockResult<>(result, this.targetLock);
        } else {
            //
            // Ask the Guard
            //
            LockResult<GuardResult, TargetLock> lockResult =
                    PolicyGuard.lockTarget(this.currentOperation.policy.getTarget().getType(),
                            this.currentOperation.getTargetEntity(), this.onset.getRequestId(), this,
                            this.currentOperation.getOperationTimeout() + ADDITIONAL_LOCK_SEC);
            //
            // Was it acquired?
            //
            if (lockResult.getA().equals(GuardResult.LOCK_ACQUIRED)) {
                //
                // Yes, let's save it
                //
                this.targetLock = lockResult.getB();
            }
            return lockResult;
        }
    }

    /**
     * Release the lock for the current operation.
     *
     * @return the target lock
     */
    public synchronized TargetLock unlockCurrentOperation() {
        if (this.targetLock == null) {
            return null;
        }

        TargetLock returnLock = this.targetLock;
        this.targetLock = null;

        PolicyGuard.unlockTarget(returnLock);

        // always return the old target lock so rules can retract it
        return returnLock;
    }

    public enum NEW_EVENT_STATUS {
        FIRST_ONSET, SUBSEQUENT_ONSET, FIRST_ABATEMENT, SUBSEQUENT_ABATEMENT, SYNTAX_ERROR;
    }

    /**
     * An event onset/abatement.
     *
     * @param event the event
     * @return the status
     * @throws AaiException if an error occurs retrieving information from A&AI
     */
    public NEW_EVENT_STATUS onNewEvent(VirtualControlLoopEvent event) throws AaiException {
        try {
            this.checkEventSyntax(event);
            if (event.getClosedLoopEventStatus() == ControlLoopEventStatus.ONSET) {
                //
                // Check if this is our original ONSET
                //
                if (event.equals(this.onset)) {
                    //
                    // Query A&AI if needed
                    //
                    queryAai(event);

                    //
                    // DO NOT retract it
                    //
                    return NEW_EVENT_STATUS.FIRST_ONSET;
                }
                //
                // Log that we got an onset
                //
                this.numOnsets++;
                return NEW_EVENT_STATUS.SUBSEQUENT_ONSET;
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
                    return NEW_EVENT_STATUS.FIRST_ABATEMENT;
                } else {
                    //
                    // Keep track that we received another
                    //
                    this.numAbatements++;
                    //
                    //
                    //
                    return NEW_EVENT_STATUS.SUBSEQUENT_ABATEMENT;
                }
            }
        } catch (ControlLoopException e) {
            logger.error("{}: onNewEvent threw: ", this, e);
        }
        return NEW_EVENT_STATUS.SYNTAX_ERROR;
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
            return this.processor.getControlLoop().getTimeout();
        }
        if (defaultTimeout != null) {
            return defaultTimeout;
        }
        return 0;
    }

    public AaiGetVnfResponse getVnfResponse() {
        return vnfResponse;
    }

    public AaiGetVserverResponse getVserverResponse() {
        return vserverResponse;
    }

    /**
     * Check an event syntax.
     *
     * @param event the event syntax
     * @throws ControlLoopException if an error occurs
     */
    public void checkEventSyntax(VirtualControlLoopEvent event) throws ControlLoopException {
        if (event.getClosedLoopEventStatus() == null
                || (event.getClosedLoopEventStatus() != ControlLoopEventStatus.ONSET
                        && event.getClosedLoopEventStatus() != ControlLoopEventStatus.ABATED)) {
            throw new ControlLoopException("Invalid value in closedLoopEventStatus");
        }
        if (event.getClosedLoopControlName() == null || event.getClosedLoopControlName().length() < 1) {
            throw new ControlLoopException("No control loop name");
        }
        if (event.getRequestId() == null) {
            throw new ControlLoopException("No request ID");
        }
        if (event.getClosedLoopEventStatus() == ControlLoopEventStatus.ABATED) {
            return;
        }
        if (event.getTarget() == null || event.getTarget().length() < 1) {
            throw new ControlLoopException("No target field");
        } else if (!VM_NAME.equalsIgnoreCase(event.getTarget()) && !VNF_NAME.equalsIgnoreCase(event.getTarget())
                && !VSERVER_VSERVER_NAME.equalsIgnoreCase(event.getTarget())
                && !GENERIC_VNF_VNF_ID.equalsIgnoreCase(event.getTarget())
                && !GENERIC_VNF_VNF_NAME.equalsIgnoreCase(event.getTarget())) {
            throw new ControlLoopException("target field invalid - expecting VM_NAME or VNF_NAME");
        }
        if (event.getAai() == null) {
            throw new ControlLoopException("AAI is null");
        }
        if (event.getAai().get(GENERIC_VNF_VNF_ID) == null && event.getAai().get(VSERVER_VSERVER_NAME) == null
                && event.getAai().get(GENERIC_VNF_VNF_NAME) == null) {
            throw new ControlLoopException(
                    "generic-vnf.vnf-id or generic-vnf.vnf-name or vserver.vserver-name information missing");
        }
    }

    /**
     * Query A&AI for an event.
     *
     * @param event the event
     * @throws AaiException if an error occurs retrieving information from A&AI
     */
    public void queryAai(VirtualControlLoopEvent event) throws AaiException {

        if (isClosedLoopDisabled(event)) {
            throw new AaiException("is-closed-loop-disabled is set to true on VServer or VNF");
        }

        if (isProvStatusInactive(event)) {
            throw new AaiException("prov-status is not ACTIVE on VServer or VNF");
        }

        Map<String, String> aai = event.getAai();

        if (aai.containsKey(VSERVER_IS_CLOSED_LOOP_DISABLED) || aai.containsKey(GENERIC_VNF_IS_CLOSED_LOOP_DISABLED)) {
            // no need to query, as we already have the data
            return;
        }

        if (vnfResponse != null || vserverResponse != null) {
            // query has already been performed
            return;
        }

        try {
            if (aai.containsKey(GENERIC_VNF_VNF_ID) || aai.containsKey(GENERIC_VNF_VNF_NAME)) {
                vnfResponse = getAAIVnfInfo(event);
                processVNFResponse(vnfResponse, aai.containsKey(GENERIC_VNF_VNF_ID));
            } else if (aai.containsKey(VSERVER_VSERVER_NAME)) {
                vserverResponse = getAAIVserverInfo(event);
                processVServerResponse(vserverResponse);
            }
        } catch (AaiException e) {
            logger.error(QUERY_AAI_ERROR_MSG, e);
            throw e;
        } catch (Exception e) {
            logger.error(QUERY_AAI_ERROR_MSG, e);
            throw new AaiException(QUERY_AAI_ERROR_MSG + e.toString());
        }
    }

    /**
     * Process a response from A&AI for a VNF.
     *
     * @param aaiResponse the response from A&AI
     * @param queryByVnfId <code>true</code> if the query was based on vnf-id,
     *        <code>false</code> if the query was based on vnf-name
     * @throws AaiException if an error occurs processing the response
     */
    private static void processVNFResponse(AaiGetVnfResponse aaiResponse, boolean queryByVNFID) throws AaiException {
        String queryTypeString = (queryByVNFID ? "vnf-id" : "vnf-name");

        if (aaiResponse == null) {
            throw new AaiException("AAI Response is null (query by " + queryTypeString + ")");
        }
        if (aaiResponse.getRequestError() != null) {
            throw new AaiException("AAI Responded with a request error (query by " + queryTypeString + ")");
        }

        if (aaiResponse.getIsClosedLoopDisabled()) {
            throw new AaiException("is-closed-loop-disabled is set to true (query by " + queryTypeString + ")");
        }

        if (!PROV_STATUS_ACTIVE.equals(aaiResponse.getProvStatus())) {
            throw new AaiException("prov-status is not ACTIVE (query by " + queryTypeString + ")");
        }
    }

    /**
     * Process a response from A&AI for a VServer.
     *
     * @param aaiResponse the response from A&AI
     * @throws AaiException if an error occurs processing the response
     */
    private static void processVServerResponse(AaiGetVserverResponse aaiResponse) throws AaiException {
        if (aaiResponse == null) {
            throw new AaiException("AAI Response is null (query by vserver-name)");
        }
        if (aaiResponse.getRequestError() != null) {
            throw new AaiException("AAI Responded with a request error (query by vserver-name)");
        }

        List<AaiNqVServer> lst = aaiResponse.getVserver();
        if (lst.isEmpty()) {
            return;
        }

        AaiNqVServer svr = lst.get(0);
        if (svr.getIsClosedLoopDisabled()) {
            throw new AaiException("is-closed-loop-disabled is set to true (query by vserver-name)");
        }

        if (!PROV_STATUS_ACTIVE.equals(svr.getProvStatus())) {
            throw new AaiException("prov-status is not ACTIVE (query by vserver-name)");
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
                        || isAaiTrue(aai.get(GENERIC_VNF_IS_CLOSED_LOOP_DISABLED)));
    }

    /**
     * Does provisioning status, for an event, have a value other than ACTIVE?
     *
     * @param event the event
     * @return {@code true} if the provisioning status is neither ACTIVE nor {@code null},
     *         {@code false} otherwise
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
     * @return the boolean value represented by the field value, or {@code false} if the
     *         value is {@code null}
     */
    protected static boolean isAaiTrue(String aaiValue) {
        return ("true".equalsIgnoreCase(aaiValue) || "T".equalsIgnoreCase(aaiValue) || "yes".equalsIgnoreCase(aaiValue)
                        || "Y".equalsIgnoreCase(aaiValue));
    }

    /**
     * Get the A&AI VService information for an event.
     *
     * @param event the event
     * @return a AaiGetVserverResponse
     * @throws ControlLoopException if an error occurs
     */
    public static AaiGetVserverResponse getAAIVserverInfo(VirtualControlLoopEvent event) throws ControlLoopException {
        UUID requestId = event.getRequestId();
        AaiGetVserverResponse response = null;
        String vserverName = event.getAai().get(VSERVER_VSERVER_NAME);

        try {
            if (vserverName != null) {
                String aaiHostURL = PolicyEngine.manager.getEnvironmentProperty("aai.url");
                String aaiUser = PolicyEngine.manager.getEnvironmentProperty("aai.username");
                String aaiPassword = PolicyEngine.manager.getEnvironmentProperty("aai.password");
                String aaiGetQueryByVserver = "/aai/v11/nodes/vservers?vserver-name=";
                String url = aaiHostURL + aaiGetQueryByVserver;
                logger.info("AAI Host URL by VServer: {}", url);
                response = new AaiManager(new RESTManager()).getQueryByVserverName(url, aaiUser, aaiPassword, requestId,
                        vserverName);
            }
        } catch (Exception e) {
            logger.error("getAAIVserverInfo exception: ", e);
            throw new ControlLoopException("Exception in getAAIVserverInfo: ", e);
        }

        return response;
    }

    /**
     * Get A&AI VNF information for an event.
     *
     * @param event the event
     * @return a AaiGetVnfResponse
     * @throws ControlLoopException if an error occurs
     */
    public static AaiGetVnfResponse getAAIVnfInfo(VirtualControlLoopEvent event) throws ControlLoopException {
        UUID requestId = event.getRequestId();
        AaiGetVnfResponse response = null;
        String vnfName = event.getAai().get(GENERIC_VNF_VNF_NAME);
        String vnfId = event.getAai().get(GENERIC_VNF_VNF_ID);

        String aaiHostURL = PolicyEngine.manager.getEnvironmentProperty("aai.url");
        String aaiUser = PolicyEngine.manager.getEnvironmentProperty("aai.username");
        String aaiPassword = PolicyEngine.manager.getEnvironmentProperty("aai.password");

        try {
            if (vnfName != null) {
                String aaiGetQueryByVnfName = "/aai/v11/network/generic-vnfs/generic-vnf?vnf-name=";
                String url = aaiHostURL + aaiGetQueryByVnfName;
                logger.info("AAI Host URL by VNF name: {}", url);
                response = new AaiManager(new RESTManager()).getQueryByVnfName(url, aaiUser, aaiPassword, requestId,
                        vnfName);
            } else if (vnfId != null) {
                String aaiGetQueryByVnfId = "/aai/v11/network/generic-vnfs/generic-vnf/";
                String url = aaiHostURL + aaiGetQueryByVnfId;
                logger.info("AAI Host URL by VNF ID: {}", url);
                response =
                        new AaiManager(new RESTManager()).getQueryByVnfId(url, aaiUser, aaiPassword, requestId, vnfId);
            }
        } catch (Exception e) {
            logger.error("getAAIVnfInfo exception: ", e);
            throw new ControlLoopException("Exception in getAAIVnfInfo: ", e);
        }

        return response;
    }

    @Override
    public boolean isActive() {
        // TODO
        return true;
    }

    @Override
    public boolean releaseLock() {
        // TODO
        return false;
    }

    @Override
    public String toString() {
        return "ControlLoopEventManager [closedLoopControlName=" + closedLoopControlName + ", requestID=" + requestID
                + ", processor=" + processor + ", onset=" + (onset != null ? onset.getRequestId() : "null")
                + ", numOnsets=" + numOnsets + ", numAbatements=" + numAbatements + ", isActivated=" + isActivated
                + ", currentOperation=" + currentOperation + ", targetLock=" + targetLock + "]";
    }

}
