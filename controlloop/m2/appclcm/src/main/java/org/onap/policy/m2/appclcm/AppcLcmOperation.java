/*-
 * ============LICENSE_START=======================================================
 * m2/appclcm
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

package org.onap.policy.m2.appclcm;

import com.google.common.collect.ImmutableList;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

import org.onap.policy.appclcm.AppcLcmBody;
import org.onap.policy.appclcm.AppcLcmCommonHeader;
import org.onap.policy.appclcm.AppcLcmDmaapWrapper;
import org.onap.policy.appclcm.AppcLcmInput;
import org.onap.policy.appclcm.AppcLcmOutput;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.controlloop.ControlLoopEvent;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.drools.m2.lock.LockAdjunct;
import org.onap.policy.guard.PolicyGuardResponse;
import org.onap.policy.m2.appclcm.model.AppcLcmResponseCode;
import org.onap.policy.m2.base.GuardAdjunct;
import org.onap.policy.m2.base.Operation;
import org.onap.policy.m2.base.Transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used for all APPC LCM operations. The only difference between
 * operation types (Restart, Rebuild, Migrate, Evacuate, or HealthCheck) as
 * far as DroolsPDP is concerned, is the operation name (policy.recipe).
 * It is up to APPC to interpret these operations.
 */
public class AppcLcmOperation implements Operation, LockAdjunct.Requestor, Serializable {
    public static final String DCAE_CLOSEDLOOP_DISABLED_FIELD = "vserver.is-closed-loop-disabled";
    public static final String DCAE_VSERVER_SELF_LINK_FIELD = "vserver.selflink";
    public static final String DCAE_IDENTITY_FIELD = "cloud-region.identity-url";
    public static final String DCAE_VNF_NAME_FIELD = "generic-vnf.vnf-name";
    public static final String DCAE_VNF_ID_FIELD = "generic-vnf.vnf-id";
    public static final String DCAE_VSERVER_ID_FIELD = "vserver.vserver-id";
    public static final String DCAE_TENANT_ID_FIELD = "tenant.tenant-id";

    public static final String APPC_LCM_VM_ID_FIELD = "vm-id";
    public static final String APPC_LCM_IDENTITY_URL_FIELD = "identity-url";
    public static final String APPC_LCM_TENANT_ID_FIELD = "tenant-id";

    private static Logger logger = LoggerFactory.getLogger(AppcLcmOperation.class);

    private static final long serialVersionUID = 5062964240000304989L;

    // state when waiting for a lock
    public static final String LCM_WAIT_FOR_LOCK = "LCM.WAIT_FOR_LOCK";

    // state when waiting for a response from 'guard'
    public static final String LCM_GUARD_PENDING = "LCM.GUARD_PENDING";

    // state when ready to send out the LCM message
    public static final String LCM_BEGIN = "LCM.BEGIN";

    // state when waiting for a response from APPC
    public static final String LCM_PENDING = "LCM.PENDING";

    // state when processing can't continue due to errors
    public static final String LCM_ERROR = "LCM.ERROR";

    // state when the operation has completed (success, failure, or timeout)
    public static final String LCM_COMPLETE = "LCM.COMPLETE";

    // the APPC LCM recipes supported by Policy
    private static final ImmutableList<String> recipes = ImmutableList.of(
                "Restart", "Rebuild", "Migrate", "Evacuate",
                "HealthCheck", "Reboot", "Start", "Stop");

    // used for JSON <-> String conversion
    protected static StandardCoder coder = new StandardCoder();

    // current state -- one of the 6 values, above
    @Getter(onMethod = @__({@Override}))
    private String state;

    // transaction associated with this operation
    private Transaction transaction;

    // policy associated with this operation
    @Getter(onMethod = @__({@Override}))
    private Policy policy;

    // initial onset message
    private VirtualControlLoopEvent onset;

    // attempt associated with this operation
    @Getter(onMethod = @__({@Override}))
    private int attempt;

    // message, if any, associated with the result of this operation
    @Getter(onMethod = @__({@Override}))
    private String message = null;

    // operation result -- set to a non-null value when the operation completes
    @Getter(onMethod = @__({@Override}))
    private PolicyResult result = null;

    // the APPC LCM 'target' derived from the onset
    private String target;

    // reference to a Transaction adjunct supporting guard operations
    private GuardAdjunct guardAdjunct;

    // counter for how many partial failures were received from appc
    private int partialFailureCount = 0;

    // counter for how many partial success were received from appc
    private int partialSuccessCount = 0;

    /**
     * Constructor -- initialize an LCM operation instance,
     * try to acquire a lock, and start the guard query if we are ready.
     *
     * @param transaction the transaction the operation is running under
     * @param policy the policy associated with this operation
     * @param onset the initial onset event that triggered the transaction
     * @param attempt this value starts at 1, and is incremented for each retry
     */
    public AppcLcmOperation(Transaction transaction, Policy policy, ControlLoopEvent onset,
        int attempt) {
        // state prior to aquiring the lock
        // (will be changed when the lock is acquired)
        this.state = LCM_WAIT_FOR_LOCK;
        this.transaction = transaction;
        this.policy = policy;
        this.attempt = attempt;

        if (!(onset instanceof VirtualControlLoopEvent)) {
            // we need the correct 'onset' event type
            state = LCM_COMPLETE;
            result = PolicyResult.FAILURE;
            message = "Onset event has the wrong type";
            return;
        }

        this.onset = (VirtualControlLoopEvent)onset;

        // fetch or create the guard adjunct -- note that 'guard' operations are
        // only performed if a 'GuardContext' is present, and the adjunct was
        // created by the Drools rules prior to creating this operation
        this.guardAdjunct = transaction.getAdjunct(GuardAdjunct.class);

        // attempt to get a lock for the VM -- if we get it immediately,
        // we can go to the 'LCM_GUARD_PENDING' or 'LCM_BEGIN' state

        target = this.onset.getAai().get(onset.getTarget()).toString();
        String key = onset.getTargetType() + ":" + target;
        if (transaction.getAdjunct(LockAdjunct.class).getLock(this, key,
                transaction.getRequestId().toString(), false)) {
            // lock was acquired immediately -- move on to the 'guard query'
            // phase
            guardQuery();
        }
    }

    /**
     * A method returning true if the A&AI subtag exists
     * and the control loop exists and is not disabled and
     * the target field exists as a key in the A&AI subtag.
     *
     * @param transaction the transaction corresponding to an event
     * @param event the onset containing the A&AI subtag
     * @return true if the A&AI subtag is valid, false otherwise
     */
    public static boolean isAaiValid(Transaction transaction, VirtualControlLoopEvent event) {
        if (event.getAai() == null) {
            transaction.setNotificationMessage("No A&AI Subtag");
            return false;
        } else if (!event.getAai().containsKey(DCAE_CLOSEDLOOP_DISABLED_FIELD)) {
            transaction.setNotificationMessage(DCAE_CLOSEDLOOP_DISABLED_FIELD
                                               + " information missing");
            return false;
        } else if (isClosedLoopDisabled(event.getAai())) {
            transaction.setNotificationMessage(DCAE_CLOSEDLOOP_DISABLED_FIELD
                                               + " is set to true");
            return false;
        } else if (!event.getAai().containsKey(event.getTarget())) {
            transaction.setNotificationMessage("target field invalid - must have corresponding AAI value");
            return false;
        }
        return true;
    }

    private static boolean isClosedLoopDisabled(Map<String, String> map) {
        if (!map.containsKey(DCAE_CLOSEDLOOP_DISABLED_FIELD)) {
            return false;
        }
        String disabled = map.get(DCAE_CLOSEDLOOP_DISABLED_FIELD);
        return ("true".equalsIgnoreCase(disabled) || "y".equalsIgnoreCase(disabled));
    }

    /**
     * trigger an asynchronous guard query -- if guard is not enabled,
     * we go directly to the 'LCM_BEGIN' state.
     */
    private void guardQuery() {
        if (guardAdjunct.asyncQuery(policy, target, onset.getRequestId().toString())) {
            // 'GuardContext' is available --
            // wait for an incoming 'PolicyGuardResponse' message
            this.state = LCM_GUARD_PENDING;
        } else {
            // no 'GuardContext' is available -- go directly to the 'begin' state
            this.state = LCM_BEGIN;
            transaction.modify();
        }
    }

    /*=====================================*/
    /* 'LockAdjunct.Requestor' interface   */
    /*=====================================*/

    /**
     * This method is called by 'LockAdjunct' if we initially had to wait for
     * the lock, but it has now became available.
     */
    public void lockAvailable() {
        if (LCM_WAIT_FOR_LOCK.equals(this.state)) {
            // we have the lock -- invoke 'quardQuery()',
            // go to the appropriate state, and mark the transaction as modified
            guardQuery();

            // the 'lockAvailable' method was presumably triggered by the
            // release
            // of the lock by an unrelated transaction -- 'transaction.modify'
            // is
            // called to let Drools know that our transaction has gone through a
            // state change
            transaction.modify();
        }
    }

    /**
     * This method is called by 'LockAdjunct' if the lock was unable to be
     * obtained.
     */
    public void lockUnavailable() {
        if (LCM_WAIT_FOR_LOCK.equals(this.state)) {
            try {
                setErrorStatus("Already processing event with this target");
            } catch (ControlLoopException e) {
                logger.debug("Lock could not be obtained for this operation");
            }
        }
    }

    /*=======================*/
    /* 'Operation' interface */
    /*=======================*/

    /**
     * This method maps the recipe to the correct rpc-name syntax.
     */
    private String toRpcName(String recipe) {
        String rpcName = recipe.toLowerCase();
        if ("healthcheck".equals(rpcName)) {
            rpcName = "health-check";
        }
        return rpcName;
    }

    /**
     * This method forwards the construction of the recipe's
     * payload to the proper handler.
     *
     * @return a json representation of the payload
     * @throws ControlLoopException if it occurs
     */
    protected String setPayload(Map<String, String> aai, String recipe) throws ControlLoopException {
        Map<String, String> payload = null;

        switch (recipe) {
            case "restart":
            case "rebuild":
            case "migrate":
            case "evacuate":
            case "start":
            case "stop":
                if (this.policy.getTarget().getType() == TargetType.VM) {
                    payload = setCommonPayload(aai);
                }
                break;
            case "reboot":
                payload = setRebootPayload();
                break;
            default:
                break;
        }

        if (payload == null) {
            return null;
        }

        try {
            return coder.encode(payload);
        } catch (CoderException e) {
            return null;
        }
    }

    /**
     * This method will construct a payload for a restart, rebuild,
     * migrate, or evacuate. The payload must be an escaped json
     * string so gson is used to convert the payload hashmap into
     * json
     *
     * @return a hashmap representation of the payload
     * @throws ControlLoopException if it occurs
     */
    private Map<String, String> setCommonPayload(Map<String, String> aai) throws ControlLoopException {
        Map<String, String> payload = new HashMap<>();

        for (Map.Entry<String, String> entry : aai.entrySet()) {
            switch (entry.getKey()) {
                case DCAE_VSERVER_SELF_LINK_FIELD:
                    if (entry.getValue() != null) {
                        payload.put(APPC_LCM_VM_ID_FIELD, entry.getValue());
                    } else {
                        setErrorStatus("dcae onset is missing " + DCAE_VSERVER_SELF_LINK_FIELD);
                    }
                    break;
                case DCAE_IDENTITY_FIELD:
                    if (entry.getValue() != null) {
                        payload.put(APPC_LCM_IDENTITY_URL_FIELD, entry.getValue());
                    } else {
                        setErrorStatus("dcae onset is missing " + DCAE_IDENTITY_FIELD);
                    }
                    break;
                case DCAE_TENANT_ID_FIELD:
                    if (entry.getValue() != null) {
                        payload.put(APPC_LCM_TENANT_ID_FIELD, entry.getValue());
                    } else {
                        setErrorStatus("dcae onset is missing " + DCAE_TENANT_ID_FIELD);
                    }
                    break;
                default:
                    break;
            }
        }

        return payload;
    }

    /**
     * This method will construct a payload for a reboot.
     * The payload must be an escaped json string so gson is used
     * to convert the payload hashmap into json. The reboot payload
     * requires a type of "HARD" or "SOFT" reboot from the policy
     * defined through CLAMP.
     *
     * @return an escaped json string representation of the payload
     */
    private Map<String, String> setRebootPayload() throws ControlLoopException {
        Map<String, String> payload = new HashMap<>();

        if (this.policy.getTarget().getType() == TargetType.VM) {
            payload = setCommonPayload(onset.getAai());
            // The tenant-id is not used for the reboot request so we can remove
            // it after being added by the common payload
            payload.remove(APPC_LCM_TENANT_ID_FIELD);
        }

        // Extract "HARD" or "SOFT" from YAML policy
        String type = this.policy.getPayload().get("type").toUpperCase();
        payload.put("type", type);

        return payload;
    }

    /**
     * Return the request message associated with this operation.
     *
     * {@inheritDoc}
     *
     * @throws ControlLoopException if it occurs
     */
    @Override
    public Object getRequest() throws ControlLoopException {
        AppcLcmCommonHeader commonHeader = new AppcLcmCommonHeader();
        commonHeader.setRequestId(onset.getRequestId());
        commonHeader.setOriginatorId("POLICY");
        commonHeader.setSubRequestId(String.valueOf(attempt));

        // Policy will send a default ttl of 10 minutes (600 seconds)
        Map<String, String> flags = new HashMap<>();
        flags.put("ttl", "600");
        commonHeader.setFlags(flags);

        String action = null;
        for (String recipe: recipes) {
            if (recipe.equalsIgnoreCase(policy.getRecipe())) {
                action = recipe;
                break;
            }
        }

        if (action == null) {
            setErrorStatus("Error - invalid recipe");
        }

        Map<String, String> actionIdentifiers = new HashMap<>();

        // The vnf-id is needed for both VNF and VM level operations
        if (onset.getAai().containsKey(DCAE_VNF_NAME_FIELD)) {
            actionIdentifiers.put("vnf-id", onset.getAai().get(DCAE_VNF_ID_FIELD));
        } else {
            logger.error("Error - no AAI DCAE VNF NAME key in the onset");
            setErrorStatus("Error - no VNF NAME key in the onset");
        }

        if (this.policy.getTarget().getType() == TargetType.VM) {
            if (onset.getAai().containsKey(DCAE_VSERVER_ID_FIELD)) {
                actionIdentifiers.put("vserver-id", onset.getAai().get(DCAE_VSERVER_ID_FIELD));
            } else {
                logger.error("Error - no DCAE VSERVER ID key in the onset AAI\n");
                setErrorStatus("Error - no VSERVER ID key in the onset");
            }
        }

        String payload = setPayload(onset.getAai(), action.toLowerCase());

        // construct an APPC LCM 'Request' message
        AppcLcmInput request = new AppcLcmInput();

        request.setCommonHeader(commonHeader);
        request.setAction(action);
        request.setActionIdentifiers(actionIdentifiers);
        request.setPayload(payload);

        // Pass the LCM request to the LCM wrapper
        AppcLcmDmaapWrapper dmaapWrapper = new AppcLcmDmaapWrapper();
        dmaapWrapper.setVersion("2.0");
        AppcLcmBody appcBody = new AppcLcmBody();
        appcBody.setInput(request);
        dmaapWrapper.setBody(appcBody);
        dmaapWrapper.setCorrelationId(onset.getRequestId() + "-" + attempt);
        dmaapWrapper.setRpcName(toRpcName(action));
        dmaapWrapper.setType("request");

        // go to the LCM_PENDING state, under the assumption that the
        // calling Drools code will send out the message we are returning
        this.state = LCM_PENDING;
        transaction.modify();
        return dmaapWrapper;
    }

    /**
     * This method is called by 'incomingMessage' when the message is a
     * 'PolicyGuardResponse' message (leaving 'incomingMessage' to focus on
     * 'Response' messages).
     *
     * @param response the received guard response message
     */
    void incomingGuardMessage(PolicyGuardResponse response) {
        // this message is only meaningful if we are waiting for a
        // 'guard' response -- ignore it, if this isn't the case
        if (LCM_GUARD_PENDING.equals(this.state)) {
            if ("Deny".equals(response.getResult())) {
                // this is a guard failure
                logger.error("LCM operation denied by 'Guard'");
                this.message = "Denied by Guard";
                this.result = PolicyResult.FAILURE_GUARD;
                this.state = LCM_COMPLETE;
            } else {
                // everything else is treated as 'Permit'
                this.state = LCM_BEGIN;
                transaction.modify();
            }
        }
    }

    /**
     * An incoming message is being delivered to the operation.
     *
     * {@inheritDoc}
     */
    @Override
    public void incomingMessage(Object object) {
        if (! (object instanceof AppcLcmDmaapWrapper)) {
            if (object instanceof PolicyGuardResponse) {
                incomingGuardMessage((PolicyGuardResponse)object);
            } else if (object instanceof ControlLoopEvent) {
                incomingAbatedEvent((ControlLoopEvent) object);
            }
            // ignore this message (not sure why we even got it)
            return;
        }

        // If we reach this point, we have a 'AppcLcmDmaapWrapper' instance.
        // The rest of this method is mostly copied from
        // 'ControlLoopOperationManager.onResponse'.

        AppcLcmOutput response = ((AppcLcmDmaapWrapper)object).getBody().getOutput();

        //
        // Determine which subrequestID (ie. attempt)
        //
        int operationAttempt;
        try {
            operationAttempt = Integer.parseInt(response.getCommonHeader()
                                                .getSubRequestId());
        } catch (NumberFormatException e) {
            //
            // We cannot tell what happened if this doesn't exist
            // If the attempt cannot be parsed then we assume it is
            // the current attempt
            //
            this.completeOperation(this.attempt, "Policy was unable to parse APP-C SubRequestID (it was null).",
                PolicyResult.FAILURE_EXCEPTION);
            return;
        }
        //
        // Sanity check the response message
        //
        if (response.getStatus() == null) {
            //
            // We cannot tell what happened if this doesn't exist
            //
            this.completeOperation(operationAttempt,
                "Policy was unable to parse APP-C response status field (it was null).",
                PolicyResult.FAILURE_EXCEPTION);
            return;
        }
        //
        // Get the Response Code
        //
        AppcLcmResponseCode responseValue = AppcLcmResponseCode.toResponseValue(response.getStatus().getCode());
        if (responseValue == null) {
            //
            // We are unaware of this code
            //
            this.completeOperation(operationAttempt, "Policy was unable to parse APP-C response status code field.",
                PolicyResult.FAILURE_EXCEPTION);
            return;
        }
        //
        // Ok, let's figure out what APP-C's response is
        //
        switch (responseValue) {
            case ACCEPTED:
                //
                // This is good, they got our original message and
                // acknowledged it.
                //
                // Is there any need to track this?
                //
                return;
            case PARTIAL_SUCCESS:
                //
                // Keep count of partial successes to determine
                // if retries should be done at the vnf level
                //
                this.partialSuccessCount++;
                return;
            case PARTIAL_FAILURE:
                //
                // Keep count of partial failures to determine
                // if no retries should be done
                //
                this.partialFailureCount++;
                return;
            case ERROR:
            case REJECT:
                //
                // We'll consider these two codes as exceptions
                //
                this.completeOperation(operationAttempt, response.getStatus()
                                       .getMessage(), PolicyResult.FAILURE_EXCEPTION);
                return;
            case SUCCESS:
                //
                //
                //
                this.completeOperation(operationAttempt, response.getStatus()
                                       .getMessage(), PolicyResult.SUCCESS);
                return;
            case FAILURE:
                // For the VNF level operations, retries will be attempted only
                // if ALL individual VMs failed the operation
                if (this.partialSuccessCount == 0) {
                    // Since there are no partial successes, that means all VMs failed
                    // if all vms fail, we can retry the VNF level action
                    this.completeOperation(operationAttempt, response.getStatus()
                                           .getMessage(), PolicyResult.FAILURE);
                } else if (this.partialFailureCount > 0) {
                    // Since only a subset of VMs had partial failures,
                    // the result should go to final failure and not
                    // retry or move on to the next policy in the chain.
                    this.completeOperation(operationAttempt, response.getStatus()
                                           .getMessage(), PolicyResult.FAILURE_EXCEPTION);
                }
                return;
            default:
                break;
        }
    }

    /**
     * This method is called by 'incomingMessage' only when an 'ABATED' event is received before an APPC
     * request is sent.
     *
     * @param event the control loop event that was received
     */
    private void incomingAbatedEvent(ControlLoopEvent event) {
        // check if ClosedLoopEventStatus is 'abated'
        if (event.isEventStatusValid() && "ABATED".equalsIgnoreCase(event.getClosedLoopEventStatus().toString())) {
            this.result = PolicyResult.SUCCESS;
            this.message = "Abatement received before APPC request was sent";
            this.state = LCM_COMPLETE;
        }
    }

    /**
       * This method is called by 'incomingMessage' in order to complete the
       * operation.
       *
       * @param attempt the operation attempt indicated in the response message
       * @param message the value to store in the 'message' field'
       * @param result the value to store in the 'result' field
       */
    void completeOperation(int attempt, String message, PolicyResult result) {
        logger.debug("LCM: completeOperation("
                     + "this.attempt=" + this.attempt
                     + ", attempt=" + attempt
                     + ", result=" + result
                     + ", message=" + message);
        if (this.attempt == attempt) {
            // we need to verify that the attempt matches in order to reduce the
            // chances that we are reacting to a prior 'Response' message that
            // was received after we timed out (unfortunately, we can't guarantee
            // this, because we have no reliable way to verify the 'recipe')

            this.message = message;
            this.result = result;
            state = LCM_COMPLETE;
        }
    }

    /**
     * The operation has timed out.
     *
     * {@inheritDoc}
     */
    @Override
    public void timeout() {
        result = PolicyResult.FAILURE_TIMEOUT;
        state = LCM_COMPLETE;
    }

    void setErrorStatus(String message) throws ControlLoopException {
        result = PolicyResult.FAILURE_EXCEPTION;
        state = LCM_ERROR;
        this.message = message;
        transaction.modify();
        throw new ControlLoopException(message);
    }

    /**
     * This is called right after it's history entry has been completed.
     *
     * {@inheritDoc}
     */
    @Override
    public void histEntryCompleted(ControlLoopOperation histEntry) {
        // give 'guard' a chance to create a DB entry (this only happens if
        // we really have a 'GuardContext', and all of the needed parameters
        // were provided in the '*-controller.properties' file)
        guardAdjunct.asyncCreateDbEntry(histEntry, target);
    }
}
