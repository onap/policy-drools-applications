/*-
 * ============LICENSE_START=======================================================
 * controlloop operation manager
 * ================================================================================
 * Copyright (C) 2017-2020 AT&T Intellectual Property. All rights reserved.
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import org.apache.commons.lang3.tuple.Pair;
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
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.guard.OperationsHistory;
import org.onap.policy.guard.Util;
import org.onap.policy.sdnc.SdncResponse;
import org.onap.policy.sdnr.PciResponseWrapper;
import org.onap.policy.so.SoResponseWrapper;
import org.onap.policy.vfc.VfcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlLoopOperationManager implements Serializable {
    private static final String SUCCESS_MSG = " Success";
    private static final String FAILED_MSG = " Failed";
    private static final long serialVersionUID = -3773199283624595410L;
    private static final Logger logger = LoggerFactory.getLogger(ControlLoopOperationManager.class);

    private static final String VSERVER_VSERVER_NAME = "vserver.vserver-name";
    private static final String GENERIC_VNF_VNF_NAME = "generic-vnf.vnf-name";
    private static final String GENERIC_VNF_VNF_ID = "generic-vnf.vnf-id";

    private static final String AAI_SERVICE_INSTANCE_ID_KEY = "service-instance.service-instance-id";
    private static final String PNF_NAME = "pnf.pnf-name";

    //
    // These properties are not changeable, but accessible
    // for Drools Rule statements.
    //
    public final ControlLoopEvent onset;
    public final Policy policy;

    //
    // Properties used to track the Operation
    //
    private int attempts = 0;
    private Operation currentOperation = null;
    private LinkedList<Operation> operationHistory = new LinkedList<>();
    private PolicyResult policyResult = null;
    private ControlLoopEventManager eventManager = null;
    private String targetEntity;
    private String guardApprovalStatus = "NONE";    // "NONE", "PERMIT", "DENY"
    private AaiCqResponse aaiCqResponse;
    private transient Object operationRequest;

    /**
     * Construct an instance.
     *
     * @param onset the onset event
     * @param policy the policy
     * @param em the event manager
     * @throws ControlLoopException if an error occurs
     */
    public ControlLoopOperationManager(ControlLoopEvent onset, Policy policy,
        ControlLoopEventManager em) throws ControlLoopException {

        this.onset = onset;
        this.policy = policy;
        this.guardApprovalStatus = "NONE";
        this.eventManager = em;

        try {
            if (TargetType.VNF.equals(policy.getTarget().getType())
                || TargetType.VFMODULE.equals(policy.getTarget().getType())) {
                this.aaiCqResponse =
                    this.eventManager.getCqResponse((VirtualControlLoopEvent) onset);
            }

            this.targetEntity = getTarget(policy);

            initActor(policy);

        } catch (AaiException e) {
            throw new ControlLoopException(e.getMessage(), e);
        }
    }

    private void initActor(Policy policy) throws AaiException, ControlLoopException {
        //
        // Let's make a sanity check
        //
        switch (policy.getActor()) {
            case "APPC":
                initAppc(policy);
                break;
            case "SO":
                break;
            case "SDNR":
                break;
            case "VFC":
                break;
            case "SDNC":
                break;
            case "CDS":
                break;
            default:
                throw new ControlLoopException(
                    "ControlLoopEventManager: policy has an unknown actor.");
        }
    }

    private void initAppc(Policy policy) throws AaiException {
        if ("ModifyConfig".equalsIgnoreCase(policy.getRecipe())) {
            /*
             * The target vnf-id may not be the same as the source vnf-id specified in the yaml, the
             * target
             * vnf-id is retrieved by a named query to A&AI.
             */
            GenericVnf genvnf = this.aaiCqResponse
                .getGenericVnfByModelInvariantId(policy.getTarget().getResourceID());
            if (genvnf == null) {
                logger.info("Target entity could not be found");
                throw new AaiException("Target vnf-id could not be found");
            }
            this.targetEntity = genvnf.getVnfId();

        }
    }

    public ControlLoopEventManager getEventManager() {
        return eventManager;
    }

    public void setEventManager(ControlLoopEventManager eventManager) {
        this.eventManager = eventManager;
    }

    public String getTargetEntity() {
        return this.targetEntity;
    }

    @Override
    public String toString() {
        return "ControlLoopOperationManager [onset="
            + (onset != null ? onset.getRequestId() : "null") + ", policy="
            + (policy != null ? policy.getId() : "null") + ", attempts=" + attempts
            + ", policyResult=" + policyResult + ", currentOperation=" + currentOperation
            + ", operationHistory=" + operationHistory + "]";
    }

    //
    // Internal class used for tracking
    //
    private class Operation implements Serializable {
        private static final long serialVersionUID = 1L;

        private ControlLoopOperation clOperation = new ControlLoopOperation();
        private PolicyResult policyResult = null;
        private int attempt = 0;

        @Override
        public String toString() {
            return "Operation [attempt=" + attempt + ", policyResult=" + policyResult
                + ", operation=" + clOperation + "]";
        }
    }

    public Object getOperationRequest() {
        return operationRequest;
    }

    public String getGuardApprovalStatus() {
        return guardApprovalStatus;
    }

    public void setGuardApprovalStatus(String guardApprovalStatus) {
        this.guardApprovalStatus = guardApprovalStatus;
    }

    /**
     * Get the target for a policy.
     *
     * @param policy the policy
     * @return the target
     * @throws ControlLoopException if an error occurs
     */
    public String getTarget(Policy policy) throws ControlLoopException {
        if (policy.getTarget() == null) {
            throw new ControlLoopException("The target is null");
        }

        if (policy.getTarget().getType() == null) {
            throw new ControlLoopException("The target type is null");
        }

        switch (policy.getTarget().getType()) {
            case PNF:
                return getPnfTarget();
            case VM:
            case VNF:
                return getVfModuleTarget();
            case VFMODULE:
                return getVfModuleTarget();
            default:
                throw new ControlLoopException("The target type is not supported");
        }
    }

    private String getVfModuleTarget() throws ControlLoopException {
        VirtualControlLoopEvent virtualOnsetEvent = (VirtualControlLoopEvent) this.onset;
        if (this.onset.getTarget().equalsIgnoreCase(VSERVER_VSERVER_NAME)) {
            return virtualOnsetEvent.getAai().get(VSERVER_VSERVER_NAME);
        } else if (this.onset.getTarget().equalsIgnoreCase(GENERIC_VNF_VNF_ID)) {
            return virtualOnsetEvent.getAai().get(GENERIC_VNF_VNF_ID);
        } else if (this.onset.getTarget().equalsIgnoreCase(GENERIC_VNF_VNF_NAME)) {
            /*
             * If the onset is enriched with the vnf-id, we don't need an A&AI response
             */
            if (virtualOnsetEvent.getAai().containsKey(GENERIC_VNF_VNF_ID)) {
                return virtualOnsetEvent.getAai().get(GENERIC_VNF_VNF_ID);
            }

            /*
             * If the vnf-name was retrieved from the onset then the vnf-id must be obtained from
             * the event
             * manager's A&AI GET query
             */
            try {
                String vnfId = this.aaiCqResponse.getDefaultGenericVnf().getVnfId();
                if (vnfId == null) {
                    throw new AaiException("No vnf-id found");
                }
                return vnfId;

            } catch (AaiException e) {
                throw new ControlLoopException(e.getMessage(), e);
            }
        }
        throw new ControlLoopException("Target does not match target type");
    }

    private String getPnfTarget() throws ControlLoopException {
        VirtualControlLoopEvent virtualOnsetEvent = (VirtualControlLoopEvent) this.onset;
        if (!PNF_NAME.equalsIgnoreCase(onset.getTarget())) {
            throw new ControlLoopException(
                "Target in the onset event is either null or does not match target key expected in AAI section.");
        }
        return virtualOnsetEvent.getAai().get(PNF_NAME);
    }

    /**
     * Start an operation.
     *
     * @param onset the onset event
     * @return the operation request
     * @throws ControlLoopException if an error occurs
     */
    public Object startOperation(/* VirtualControlLoopEvent */ControlLoopEvent onset)
        throws ControlLoopException {
        verifyOperatonCanRun();

        //
        // Setup
        //
        this.policyResult = null;
        Operation operation = new Operation();
        operation.attempt = ++this.attempts;
        operation.clOperation.setActor(this.policy.getActor());
        operation.clOperation.setOperation(this.policy.getRecipe());
        operation.clOperation.setTarget(this.policy.getTarget().toString());
        operation.clOperation.setSubRequestId(Integer.toString(operation.attempt));
        //
        // Now determine which actor we need to construct a request for
        //
        try {
            switch (policy.getActor()) {
                case "APPC":
                    return startAppcOperation(onset, operation);
                case "SO":
                    return startSoOperation(onset, operation);
                case "VFC":
                    return startVfcOperation(onset, operation);
                case "SDNR":
                    return startSdnrOperation(onset, operation);
                case "SDNC":
                    return startSdncOperation(onset, operation);
                case "CDS":
                    return startCdsOperation(onset, operation);
                default:
                    throw new ControlLoopException(
                        "invalid actor " + policy.getActor() + " on policy");
            }

        } catch (AaiException e) {
            throw new ControlLoopException(e.getMessage(), e);
        }
    }

    private Object startAppcOperation(ControlLoopEvent onset, Operation operation) {
        /*
         * If the recipe is ModifyConfig, a legacy APPC request is constructed. Otherwise an
         * LCMRequest is
         * constructed.
         */
        this.currentOperation = operation;
        if ("ModifyConfig".equalsIgnoreCase(policy.getRecipe())) {
            this.operationRequest =
                AppcActorServiceProvider.constructRequest((VirtualControlLoopEvent) onset,
                    operation.clOperation, this.policy, this.targetEntity);
        } else {
            this.operationRequest =
                AppcLcmActorServiceProvider.constructRequest((VirtualControlLoopEvent) onset,
                    operation.clOperation, this.policy, this.targetEntity);
        }
        //
        // Save the operation
        //

        return operationRequest;
    }

    private Object startSoOperation(ControlLoopEvent onset, Operation operation) {
        SoActorServiceProvider soActorSp = new SoActorServiceProvider();
        this.operationRequest = soActorSp.constructRequestCq((VirtualControlLoopEvent) onset,
            operation.clOperation, this.policy, this.aaiCqResponse);

        // Save the operation
        this.currentOperation = operation;

        if (this.operationRequest == null) {
            this.policyResult = PolicyResult.FAILURE;
        }

        return operationRequest;
    }

    private Object startVfcOperation(ControlLoopEvent onset, Operation operation) {
        this.operationRequest =
            VfcActorServiceProvider.constructRequestCq((VirtualControlLoopEvent) onset,
                operation.clOperation, this.policy, this.aaiCqResponse);
        this.currentOperation = operation;
        if (this.operationRequest == null) {
            this.policyResult = PolicyResult.FAILURE;
        }
        return operationRequest;
    }

    private Object startSdnrOperation(ControlLoopEvent onset, Operation operation) {
        /*
         * If the recipe is ModifyConfig or ModifyConfigANR, a SDNR request is constructed.
         */
        this.currentOperation = operation;
        this.operationRequest = SdnrActorServiceProvider
            .constructRequest((VirtualControlLoopEvent) onset, operation.clOperation, this.policy);
        //
        // Save the operation
        //
        if (this.operationRequest == null) {
            this.policyResult = PolicyResult.FAILURE;
        }

        return operationRequest;
    }

    private Object startSdncOperation(ControlLoopEvent onset, Operation operation) {
        SdncActorServiceProvider provider = new SdncActorServiceProvider();
        this.operationRequest = provider.constructRequest((VirtualControlLoopEvent) onset,
            operation.clOperation, this.policy);
        this.currentOperation = operation;
        if (this.operationRequest == null) {
            this.policyResult = PolicyResult.FAILURE;
        }
        return operationRequest;
    }

    private Object startCdsOperation(ControlLoopEvent onset, Operation operation)
        throws AaiException {

        CdsActorServiceProvider provider = new CdsActorServiceProvider();
        Optional<ExecutionServiceInput> optionalRequest =
            provider.constructRequest((VirtualControlLoopEvent) onset, operation.clOperation,
                this.policy, this.buildAaiParams());

        this.currentOperation = operation;
        if (optionalRequest.isPresent()) {
            this.operationRequest = optionalRequest.get();
        } else {
            this.operationRequest = null;
            this.policyResult = PolicyResult.FAILURE;
        }

        return this.operationRequest;
    }

    /**
     * Build AAI parameters for CDS operation.
     *
     * @return a map containing vnf id key and value for the vnf to apply the action to.
     * @throws AaiException if the vnf can not be found.
     */
    private Map<String, String> buildAaiParams() throws AaiException {

        Map<String, String> result = new HashMap<>();

        if (TargetType.VNF.equals(policy.getTarget().getType())
            || TargetType.VFMODULE.equals(policy.getTarget().getType())) {

            ServiceInstance serviceInstance = this.aaiCqResponse.getServiceInstance();
            if (serviceInstance == null) {
                logger.info("Target entity service instance could not be found");
                throw new AaiException("Target service instance could not be found");
            }

            GenericVnf genericVnf = this.aaiCqResponse
                .getGenericVnfByModelInvariantId(policy.getTarget().getResourceID());
            if (genericVnf == null) {
                logger.info("Target entity generic vnf could not be found");
                throw new AaiException("Target generic vnf could not be found");
            }

            result.put(AAI_SERVICE_INSTANCE_ID_KEY, serviceInstance.getServiceInstanceId());
            result.put(GENERIC_VNF_VNF_ID, genericVnf.getVnfId());

        } else if (TargetType.PNF.equals(policy.getTarget().getType())) {
            result = this.eventManager.getPnf((VirtualControlLoopEvent) onset);
        }

        return result;

    }

    /**
     * Handle a response.
     *
     * @param response the response
     * @return a PolicyResult
     */
    public PolicyResult onResponse(Object response) {
        //
        // Which response is it?
        //
        if (response instanceof Response) {
            //
            // Cast APPC response and handle it
            //
            return onResponse((Response) response);
        } else if (response instanceof AppcLcmDmaapWrapper) {
            //
            // Cast LCM response and handle it
            //
            return onResponse((AppcLcmDmaapWrapper) response);
        } else if (response instanceof PciResponseWrapper) {
            //
            // Cast SDNR response and handle it
            //
            return onResponse((PciResponseWrapper) response);
        } else if (response instanceof SoResponseWrapper) {
            //
            // Cast SO response and handle it
            //
            return onResponse((SoResponseWrapper) response);
        } else if (response instanceof VfcResponse) {
            //
            // Cast VFC response and handle it
            //
            return onResponse((VfcResponse) response);
        } else if (response instanceof SdncResponse) {
            //
            // Cast SDNC response and handle it
            //
            return onResponse((SdncResponse) response);
        } else if (response instanceof CdsResponse) {
            //
            // Cast CDS response and handle it
            //
            return onResponse((CdsResponse) response);
        } else {
            return null;
        }
    }

    /**
     * This method handles operation responses from APPC.
     *
     * @param appcResponse the APPC response
     * @return The result of the response handling
     */
    private PolicyResult onResponse(Response appcResponse) {
        //
        // Determine which subrequestID (ie. attempt)
        //
        Integer operationAttempt = getSubRequestId(appcResponse);
        if (operationAttempt == null) {
            this.completeOperation(operationAttempt,
                "Policy was unable to parse APP-C SubRequestID (it was null).",
                PolicyResult.FAILURE_EXCEPTION);
            return PolicyResult.FAILURE_EXCEPTION;
        }
        //
        // Sanity check the response message
        //
        if (appcResponse.getStatus() == null) {
            //
            // We cannot tell what happened if this doesn't exist
            //
            this.completeOperation(operationAttempt,
                "Policy was unable to parse APP-C response status field (it was null).",
                PolicyResult.FAILURE_EXCEPTION);
            return PolicyResult.FAILURE_EXCEPTION;
        }
        //
        // Get the Response Code
        //
        ResponseCode code = ResponseCode.toResponseCode(appcResponse.getStatus().getCode());
        if (code == null) {
            //
            // We are unaware of this code
            //
            this.completeOperation(operationAttempt,
                "Policy was unable to parse APP-C response status code field.",
                PolicyResult.FAILURE_EXCEPTION);
            return PolicyResult.FAILURE_EXCEPTION;
        }

        return onResponse(appcResponse, operationAttempt, code);
    }

    private PolicyResult onResponse(Response appcResponse, Integer operationAttempt,
        ResponseCode code) {
        //
        // Ok, let's figure out what APP-C's response is
        //
        switch (code) {
            case ACCEPT:
                //
                // This is good, they got our original message and
                // acknowledged it.
                //
                // Is there any need to track this?
                //
                return null;
            case ERROR:
            case REJECT:
                //
                // We'll consider these two codes as exceptions
                //
                this.completeOperation(operationAttempt, appcResponse.getStatus().getDescription(),
                    PolicyResult.FAILURE_EXCEPTION);
                return getTimeoutResult(PolicyResult.FAILURE_EXCEPTION);
            case SUCCESS:
                //
                //
                //
                this.completeOperation(operationAttempt, appcResponse.getStatus().getDescription(),
                    PolicyResult.SUCCESS);
                return getTimeoutResult(PolicyResult.SUCCESS);
            case FAILURE:
                //
                //
                //
                this.completeOperation(operationAttempt, appcResponse.getStatus().getDescription(),
                    PolicyResult.FAILURE);
                return getTimeoutResult(PolicyResult.FAILURE);
            default:
                return null;
        }
    }

    /**
     * This method handles operation responses from LCM.
     *
     * @param dmaapResponse the LCM response
     * @return The result of the response handling
     */
    private PolicyResult onResponse(AppcLcmDmaapWrapper dmaapResponse) {
        /*
         * Parse out the operation attempt using the subrequestid
         */
        Integer operationAttempt = AppcLcmActorServiceProvider.parseOperationAttempt(
            dmaapResponse.getBody().getOutput().getCommonHeader().getSubRequestId());
        if (operationAttempt == null) {
            this.completeOperation(operationAttempt,
                "Policy was unable to parse APP-C SubRequestID (it was null).",
                PolicyResult.FAILURE_EXCEPTION);
            return PolicyResult.FAILURE_EXCEPTION;
        }

        /*
         * Process the APPCLCM response to see what PolicyResult should be returned
         */
        AbstractMap.SimpleEntry<PolicyResult, String> result =
            AppcLcmActorServiceProvider.processResponse(dmaapResponse);

        if (result.getKey() != null) {
            this.completeOperation(operationAttempt, result.getValue(), result.getKey());
            if (PolicyResult.FAILURE_TIMEOUT.equals(this.policyResult)) {
                return null;
            }
            return result.getKey();
        }
        return null;
    }

    /**
     * This method handles operation responses from SDNR.
     *
     * @param dmaapResponse the SDNR response
     * @return the result of the response handling
     */
    private PolicyResult onResponse(PciResponseWrapper dmaapResponse) {
        /*
         * Parse out the operation attempt using the subrequestid
         */
        Integer operationAttempt = SdnrActorServiceProvider
            .parseOperationAttempt(dmaapResponse.getBody().getCommonHeader().getSubRequestId());
        if (operationAttempt == null) {
            this.completeOperation(operationAttempt,
                "Policy was unable to parse SDNR SubRequestID.", PolicyResult.FAILURE_EXCEPTION);
            return PolicyResult.FAILURE_EXCEPTION;
        }

        /*
         * Process the SDNR response to see what PolicyResult should be returned
         */
        Pair<PolicyResult, String> result =
            SdnrActorServiceProvider.processResponse(dmaapResponse);

        if (result.getLeft() != null) {
            this.completeOperation(operationAttempt, result.getRight(), result.getLeft());
            if (PolicyResult.FAILURE_TIMEOUT.equals(this.policyResult)) {
                return null;
            }
            return result.getLeft();
        }
        return null;
    }

    /**
     * This method handles operation responses from SO.
     *
     * @param msoResponse the SO response
     * @return The result of the response handling
     */
    private PolicyResult onResponse(SoResponseWrapper msoResponse) {
        switch (msoResponse.getSoResponse().getHttpResponseCode()) {
            case 200:
            case 202:
                //
                // Consider it as success
                //
                this.completeOperation(this.attempts,
                    msoResponse.getSoResponse().getHttpResponseCode() + SUCCESS_MSG,
                    PolicyResult.SUCCESS);
                return getTimeoutResult(PolicyResult.SUCCESS);
            default:
                //
                // Consider it as failure
                //
                this.completeOperation(this.attempts,
                    msoResponse.getSoResponse().getHttpResponseCode() + FAILED_MSG,
                    PolicyResult.FAILURE);
                return getTimeoutResult(PolicyResult.FAILURE);
        }
    }

    /**
     * This method handles operation responses from VFC.
     *
     * @param vfcResponse the VFC response
     * @return The result of the response handling
     */
    private PolicyResult onResponse(VfcResponse vfcResponse) {
        if ("finished".equalsIgnoreCase(vfcResponse.getResponseDescriptor().getStatus())) {
            //
            // Consider it as success
            //
            this.completeOperation(this.attempts, SUCCESS_MSG, PolicyResult.SUCCESS);
            return getTimeoutResult(PolicyResult.SUCCESS);
        } else {
            //
            // Consider it as failure
            //
            this.completeOperation(this.attempts, FAILED_MSG, PolicyResult.FAILURE);
            if (PolicyResult.FAILURE_TIMEOUT.equals(this.policyResult)) {
                return null;
            }
            // increment operation attempts for retries
            this.attempts += 1;
            return PolicyResult.FAILURE;
        }
    }

    /**
     * This method handles operation responses from SDNC.
     *
     * @param sdncResponse the VFC response
     * @return The result of the response handling
     */
    private PolicyResult onResponse(SdncResponse sdncResponse) {
        if ("200".equals(sdncResponse.getResponseOutput().getResponseCode())) {
            //
            // Consider it as success
            //
            this.completeOperation(this.attempts, SUCCESS_MSG, PolicyResult.SUCCESS);
            return getTimeoutResult(PolicyResult.SUCCESS);
        } else {
            //
            // Consider it as failure
            //
            this.completeOperation(this.attempts, FAILED_MSG, PolicyResult.FAILURE);
            if (PolicyResult.FAILURE_TIMEOUT.equals(this.policyResult)) {
                return null;
            }
            // increment operation attempts for retries
            this.attempts += 1;
            return PolicyResult.FAILURE;
        }
    }

    /**
     * This method handles operation responses from CDS.
     *
     * @param response the CDS response
     * @return The result of the response handling
     */
    private PolicyResult onResponse(CdsResponse response) {
        if (response != null && CdsActorConstants.SUCCESS.equals(response.getStatus())) {
            //
            // Consider it as success
            //
            this.completeOperation(this.attempts, SUCCESS_MSG, PolicyResult.SUCCESS);
            return getTimeoutResult(PolicyResult.SUCCESS);
        } else {
            //
            // Consider it as failure
            //
            this.completeOperation(this.attempts, FAILED_MSG, PolicyResult.FAILURE);
            if (PolicyResult.FAILURE_TIMEOUT.equals(this.policyResult)) {
                return null;
            }
            // increment operation attempts for retries
            this.attempts += 1;
            return PolicyResult.FAILURE;
        }
    }

    private PolicyResult getTimeoutResult(PolicyResult result) {
        return (PolicyResult.FAILURE_TIMEOUT.equals(this.policyResult) ? null : result);
    }

    private Integer getSubRequestId(Response appcResponse) {
        try {
            return Integer.valueOf(appcResponse.getCommonHeader().getSubRequestId());
        } catch (NumberFormatException e) {
            //
            // We cannot tell what happened if this doesn't exist
            //
            return null;
        }
    }

    /**
     * Get the operation timeout.
     *
     * @return the timeout
     */
    public Integer getOperationTimeout() {
        //
        // Sanity check
        //
        if (this.policy == null) {
            logger.debug("getOperationTimeout returning 0");
            return 0;
        }
        logger.debug("getOperationTimeout returning {}", this.policy.getTimeout());
        return this.policy.getTimeout();
    }

    /**
     * Get the operation timeout as a String.
     *
     * @param defaultTimeout the default timeout
     * @return the timeout as a String
     */
    public String getOperationTimeoutString(int defaultTimeout) {
        Integer to = this.getOperationTimeout();
        if (to == null || to == 0) {
            return Integer.toString(defaultTimeout) + "s";
        }
        return to.toString() + "s";
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
            return this.operationHistory.getLast().clOperation.toMessage() + ", Guard result: "
                + guardResult;
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
        LinkedList<ControlLoopOperation> history = new LinkedList<>();
        for (Operation op : this.operationHistory) {
            history.add(new ControlLoopOperation(op.clOperation));

        }
        return history;
    }

    /**
     * Set the operation has timed out.
     */
    public void setOperationHasTimedOut() {
        //
        //
        //
        this.completeOperation(this.attempts, "Operation timed out", PolicyResult.FAILURE_TIMEOUT);
    }

    /**
     * Set the operation has been denied by guard.
     */
    public void setOperationHasGuardDeny() {
        //
        //
        //
        this.completeOperation(this.attempts, "Operation denied by Guard",
            PolicyResult.FAILURE_GUARD);
    }

    public void setOperationHasException(String message) {
        this.completeOperation(this.attempts, message, PolicyResult.FAILURE_EXCEPTION);
    }

    /**
     * Is the operation complete.
     *
     * @return <code>true</code> if the operation is complete, <code>false</code> otherwise
     */
    public boolean isOperationComplete() {
        //
        // Is there currently a result?
        //
        if (this.policyResult == null) {
            //
            // either we are in process or we
            // haven't started
            //
            return false;
        }
        //
        // We have some result, check if the operation failed
        //
        if (this.policyResult.equals(PolicyResult.FAILURE)) {
            //
            // Check if there were no retries specified
            //
            if (getMaxRetries() < 1) {
                //
                // The result is the failure
                //
                return true;
            }
            //
            // Check retries
            //
            if (this.attempts > getMaxRetries()) {
                //
                // No more attempts allowed, reset
                // that our actual result is failure due to retries
                //
                this.policyResult = PolicyResult.FAILURE_RETRIES;
                return true;
            } else {
                //
                // There are more attempts available to try the
                // policy recipe.
                //
                return false;
            }
        }
        //
        // Other results mean we are done
        //
        return true;
    }

    public boolean isOperationRunning() {
        return (this.currentOperation != null);
    }

    /**
     * This method verifies that the operation manager may run an operation.
     *
     * @return True if the operation can run, false otherwise
     * @throws ControlLoopException if the operation cannot run
     */
    private void verifyOperatonCanRun() throws ControlLoopException {
        //
        // They shouldn't call us if we currently running something
        //
        if (this.currentOperation != null) {
            //
            // what do we do if we are already running an operation?
            //
            throw new ControlLoopException(
                "current operation is not null (an operation is already running)");
        }
        //
        // Check if we have maxed out on retries
        //
        if (getMaxRetries() < 1) {
            //
            // No retries are allowed, so check have we even made
            // one attempt to execute the operation?
            //
            if (this.attempts >= 1) {
                //
                // We have, let's ensure our PolicyResult is set
                //
                if (this.policyResult == null) {
                    this.policyResult = PolicyResult.FAILURE_RETRIES;
                }
                //
                //
                //
                throw new ControlLoopException(
                    "current operation failed and retries are not allowed");
            }
        } else {
            //
            // Have we maxed out on retries?
            //
            if (this.attempts > getMaxRetries()) {
                if (this.policyResult == null) {
                    this.policyResult = PolicyResult.FAILURE_RETRIES;
                }
                throw new ControlLoopException(
                    "current oepration has failed after " + this.attempts + " retries");
            }
        }
    }

    /**
     * Gets the maximum number of retries.
     *
     * @return the maximum number of retries, or {@code 0}, if not specified
     */
    public int getMaxRetries() {
        return (policy.getRetry() != null ? policy.getRetry() : 0);
    }

    private void storeOperationInDataBase() {
        // Only store in DB if enabled
        boolean guardEnabled = "false".equalsIgnoreCase(
            PolicyEngineConstants.getManager().getEnvironmentProperty("guard.disabled"));
        if (!guardEnabled) {
            return;
        }

        // DB Properties
        Properties props = new Properties();
        if (PolicyEngineConstants.getManager().getEnvironmentProperty(Util.ONAP_KEY_URL) != null
            && PolicyEngineConstants.getManager().getEnvironmentProperty(Util.ONAP_KEY_USER) != null
            && PolicyEngineConstants.getManager()
                .getEnvironmentProperty(Util.ONAP_KEY_PASS) != null) {
            props.put(Util.ECLIPSE_LINK_KEY_URL,
                PolicyEngineConstants.getManager().getEnvironmentProperty(Util.ONAP_KEY_URL));
            props.put(Util.ECLIPSE_LINK_KEY_USER,
                PolicyEngineConstants.getManager().getEnvironmentProperty(Util.ONAP_KEY_USER));
            props.put(Util.ECLIPSE_LINK_KEY_PASS,
                PolicyEngineConstants.getManager().getEnvironmentProperty(Util.ONAP_KEY_PASS));
            props.put(PersistenceUnitProperties.CLASSLOADER,
                ControlLoopOperationManager.class.getClassLoader());
        }

        String opsHistPu = System.getProperty("OperationsHistoryPU");
        if (!"OperationsHistoryPUTest".equals(opsHistPu)) {
            opsHistPu = "OperationsHistoryPU";
        } else {
            props.clear();
        }
        EntityManager em;
        try {
            em = Persistence.createEntityManagerFactory(opsHistPu, props).createEntityManager();
        } catch (Exception e) {
            logger.error("storeOperationInDataBase threw: ", e);
            return;
        }

        OperationsHistory newEntry = new OperationsHistory();

        newEntry.setClosedLoopName(this.onset.getClosedLoopControlName());
        newEntry.setRequestId(this.onset.getRequestId().toString());
        newEntry.setActor(this.currentOperation.clOperation.getActor());
        newEntry.setOperation(this.currentOperation.clOperation.getOperation());
        newEntry.setTarget(this.targetEntity);
        newEntry.setStarttime(Timestamp.from(this.currentOperation.clOperation.getStart()));
        newEntry.setSubrequestId(this.currentOperation.clOperation.getSubRequestId());
        newEntry
            .setEndtime(new Timestamp(this.currentOperation.clOperation.getEnd().toEpochMilli()));
        newEntry.setMessage(this.currentOperation.clOperation.getMessage());
        newEntry.setOutcome(this.currentOperation.clOperation.getOutcome());

        em.getTransaction().begin();
        em.persist(newEntry);
        em.getTransaction().commit();

        em.close();
    }

    private void completeOperation(Integer attempt, String message, PolicyResult result) {
        if (attempt == null) {
            logger.debug("attempt cannot be null (i.e. subRequestID)");
            return;
        }
        if (this.currentOperation != null) {
            if (this.currentOperation.attempt == attempt.intValue()) {
                this.currentOperation.clOperation.setEnd(Instant.now());
                this.currentOperation.clOperation.setMessage(message);
                this.currentOperation.clOperation.setOutcome(result.toString());
                this.currentOperation.policyResult = result;
                //
                // Save it in history
                //
                this.operationHistory.add(this.currentOperation);
                this.storeOperationInDataBase();
                //
                // Set our last result
                //
                this.policyResult = result;
                //
                // Clear the current operation field
                //
                this.currentOperation = null;
                return;
            }
            logger.debug("not current");
        }
        for (Operation op : this.operationHistory) {
            if (op.attempt == attempt.intValue()) {
                op.clOperation.setEnd(Instant.now());
                op.clOperation.setMessage(message);
                op.clOperation.setOutcome(result.toString());
                op.policyResult = result;
                return;
            }
        }
        logger.debug("Could not find associated operation");
    }

    /**
     * Commit the abatement to the history database.
     *
     * @param message the abatement message
     * @param outcome the abatement outcome
     */
    public void commitAbatement(String message, String outcome) {
        logger.info("commitAbatement: {}. {}", message, outcome);

        if (this.currentOperation == null) {
            try {
                this.currentOperation = this.operationHistory.getLast();
            } catch (NoSuchElementException e) {
                logger.error("{}: commitAbatement threw an exception ", this, e);
                return;
            }
        }
        this.currentOperation.clOperation.setEnd(Instant.now());
        this.currentOperation.clOperation.setMessage(message);
        this.currentOperation.clOperation.setOutcome(outcome);
        //
        // Store commit in DB
        //
        this.storeOperationInDataBase();
        //
        // Clear the current operation field
        //
        this.currentOperation = null;
    }

    /**
     * Construct a ControlLoopResponse object from actor response and input event.
     *
     * @param response the response from actor
     * @param event the input event
     *
     * @return a ControlLoopResponse
     */
    public ControlLoopResponse getControlLoopResponse(Object response,
        VirtualControlLoopEvent event) {
        if (response instanceof PciResponseWrapper) {
            //
            // Cast SDNR response and handle it
            //
            return SdnrActorServiceProvider.getControlLoopResponse((PciResponseWrapper) response,
                event);
        } else {
            return null;
        }
    }

}
