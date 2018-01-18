/*-
 * ============LICENSE_START=======================================================
 * controlloop operation manager
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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
import java.util.LinkedList;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.onap.policy.aai.util.AAIException;
import org.onap.policy.appc.Response;
import org.onap.policy.appc.ResponseCode;
import org.onap.policy.appclcm.LCMResponseWrapper;
import org.onap.policy.controlloop.ControlLoopEvent;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actor.appc.APPCActorServiceProvider;
import org.onap.policy.controlloop.actor.appclcm.AppcLcmActorServiceProvider;
import org.onap.policy.controlloop.actor.so.SOActorServiceProvider;
import org.onap.policy.controlloop.actor.vfc.VFCActorServiceProvider;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.guard.Util;
import org.onap.policy.so.SOResponseWrapper;
import org.onap.policy.vfc.VFCResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlLoopOperationManager implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -3773199283624595410L;
	private static final Logger logger = LoggerFactory.getLogger(ControlLoopOperationManager.class);

	@Override
	public String toString() {
		return "ControlLoopOperationManager [onset=" + (onset != null ? onset.getRequestID() : "null") + ", policy="
				+ (policy != null ? policy.getId() : "null") + ", attempts=" + attempts
				+ ", policyResult=" + policyResult
				+ ", currentOperation=" + currentOperation + ", operationHistory=" + operationHistory
				+ "]";
	}

	//
	// These properties are not changeable, but accessible
	// for Drools Rule statements.
	//
	//public final ATTControlLoopEvent onset;
	public final ControlLoopEvent onset;
	public final transient Policy policy;

	//
	// Properties used to track the Operation
	//
	private int attempts = 0;
	private transient Operation currentOperation = null;
	private LinkedList<Operation> operationHistory = new LinkedList<Operation>();
	private PolicyResult policyResult = null;
	private ControlLoopEventManager eventManager = null;
	private String targetEntity;

	public ControlLoopEventManager getEventManager() {
		return eventManager;
	}

	public void setEventManager(ControlLoopEventManager eventManager) {
		this.eventManager = eventManager;
	}

	public String getTargetEntity() {
	    return this.targetEntity;
	}

	//
	// Internal class used for tracking
	//
	private class Operation {
		public ControlLoopOperation operation = new ControlLoopOperation();
		public PolicyResult policyResult = null;
		public int attempt = 0;

		@Override
		public String toString() {
			return "Operation [attempt=" + attempt + ", policyResult=" + policyResult + ", operation=" + operation
					+ "]";
		}
	}

	private String guardApprovalStatus = "NONE";//"NONE", "PERMIT", "DENY"
	private transient Object operationRequest;

	public Object getOperationRequest() {
		return operationRequest;
	}

	public String getGuardApprovalStatus() {
		return guardApprovalStatus;
	}
	public void setGuardApprovalStatus(String guardApprovalStatus) {
		this.guardApprovalStatus = guardApprovalStatus;
	}

	public String getTarget(Policy policy) throws ControlLoopException, AAIException {
        if (policy.getTarget() != null) {
            if (policy.getTarget().getType() != null) {
                switch(policy.getTarget().getType()) {
                case PNF:
                    break;
                case VM:
                case VNF:
                    VirtualControlLoopEvent virtualOnset = (VirtualControlLoopEvent) this.onset;
                    if (this.onset.getTarget().equalsIgnoreCase("vserver.vserver-name")) {
                        return virtualOnset.getAAI().get("vserver.vserver-name");
                    }
                    else if (this.onset.getTarget().equalsIgnoreCase("generic-vnf.vnf-id")) {
                        return virtualOnset.getAAI().get("generic-vnf.vnf-id");
                    }
                    else if (this.onset.getTarget().equalsIgnoreCase("generic-vnf.vnf-name")) {
                        /*
                         * If the onset is enriched with the vnf-id,
                         * we don't need an A&AI response
                         */
                        if (virtualOnset.getAAI().containsKey("generic-vnf.vnf-id")) {
                            return virtualOnset.getAAI().get("generic-vnf.vnf-id");
                        }
                        
                        /*
                         * If the vnf-name was retrieved from the onset then the vnf-id
                         * must be obtained from the event manager's A&AI GET query
                         */
                        String vnfId = this.eventManager.getVnfResponse().getVnfID();
                        if (vnfId == null) {
                            throw new AAIException("No vnf-id found");
                        }
                        return vnfId;
                    }
                    break;
                default:
                    throw new ControlLoopException("The target type is not supported");
                }
            }
            else {
                throw new ControlLoopException("The target type is null");
            }
        }
        else {
            throw new ControlLoopException("The target is null");
        }
        throw new ControlLoopException("Target does not match target type");
    }
	
	public ControlLoopOperationManager(ControlLoopEvent onset, Policy policy, ControlLoopEventManager em) throws ControlLoopException, AAIException {
		this.onset = onset;
		this.policy = policy;
		this.guardApprovalStatus = "NONE";
		this.eventManager = em;
		this.targetEntity = getTarget(policy);
		
		//
		// Let's make a sanity check
		//
		switch (policy.getActor()) {
		case "APPC":
		    if ("ModifyConfig".equalsIgnoreCase(policy.getRecipe())) {
		        /*
                 * The target vnf-id may not be the same as the source vnf-id
                 * specified in the yaml, the target vnf-id is retrieved by
                 * a named query to A&AI.
                 */
		        String targetVnf = AppcLcmActorServiceProvider.vnfNamedQuery(
		                    policy.getTarget().getResourceID(), this.targetEntity);
		        this.targetEntity = targetVnf;
		    }
			break;
		case "SO":
		    break;
		case "VFC":
			break;
		default:
			throw new ControlLoopException("ControlLoopEventManager: policy has an unknown actor.");
		}
	}

	public Object startOperation(/*VirtualControlLoopEvent*/ControlLoopEvent onset) throws AAIException {
		//
		// They shouldn't call us if we currently running something
		//
		if (this.currentOperation != null) {
			//
			// what do we do if we are already running an operation?
			//
			return null;
		}
		//
		// Check if we have maxed out on retries
		//
		if (this.policy.getRetry() == null || this.policy.getRetry() < 1) {
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
				return null;
			}
		} else {
			//
			// Have we maxed out on retries?
			//
			if (this.attempts > this.policy.getRetry()) {
				if (this.policyResult == null) {
					this.policyResult = PolicyResult.FAILURE_RETRIES;
				}
				return null;
			}
		}
		//
		// Setup
		//
		this.policyResult = null;
		Operation operation = new Operation();
		operation.attempt = ++this.attempts;
		operation.operation.setActor(this.policy.getActor());
		operation.operation.setOperation(this.policy.getRecipe());
		operation.operation.setTarget(this.policy.getTarget().toString());
		operation.operation.setSubRequestId(Integer.toString(operation.attempt));
		//
		// Now determine which actor we need to construct a request for
		//
		switch (policy.getActor()) {
		case "APPC":
		    /*
		     * If the recipe is ModifyConfig, a legacy APPC
		     * request is constructed. Otherwise an LCMRequest
		     * is constructed.
		     */
			this.currentOperation = operation;
		    if ("ModifyConfig".equalsIgnoreCase(policy.getRecipe())) {

	            this.operationRequest = APPCActorServiceProvider.constructRequest((VirtualControlLoopEvent) onset, 
	                                    operation.operation, this.policy, this.targetEntity);
		    }
		    else {
		        this.operationRequest = AppcLcmActorServiceProvider.constructRequest((VirtualControlLoopEvent) onset, 
		                                operation.operation, this.policy, this.targetEntity);
		    }
			//
			// Save the operation
			//
			
			return operationRequest;
		case "SO":
			SOActorServiceProvider SOAsp = new SOActorServiceProvider();
			this.operationRequest = SOAsp.constructRequest((VirtualControlLoopEvent)onset, operation.operation, this.policy);

			// Save the operation
			this.currentOperation = operation;

			if (this.operationRequest == null) {
				this.policyResult = PolicyResult.FAILURE;
			}

			return operationRequest;
		case "VFC":
            this.operationRequest = VFCActorServiceProvider.constructRequest((VirtualControlLoopEvent) onset, operation.operation, this.policy, this.eventManager.getVnfResponse());
            this.currentOperation = operation;
            if (this.operationRequest == null) {
                this.policyResult = PolicyResult.FAILURE;
            }
            return operationRequest;

		}
		return null;
	}

	public PolicyResult	onResponse(Object response) {
		//
		// Which response is it?
		//
		if (response instanceof Response) {
			//
			// Cast it
			//
			Response appcResponse = (Response) response;
			//
			// Determine which subrequestID (ie. attempt)
			//
			Integer operationAttempt = null;
			try {
				operationAttempt = Integer.parseInt(appcResponse.getCommonHeader().getSubRequestID());
			} catch (NumberFormatException e) {
				//
				// We cannot tell what happened if this doesn't exist
				//
				this.completeOperation(operationAttempt, "Policy was unable to parse APP-C SubRequestID (it was null).", PolicyResult.FAILURE_EXCEPTION);
				return PolicyResult.FAILURE_EXCEPTION;
			}
			//
			// Sanity check the response message
			//
			if (appcResponse.getStatus() == null) {
				//
				// We cannot tell what happened if this doesn't exist
				//
				this.completeOperation(operationAttempt, "Policy was unable to parse APP-C response status field (it was null).", PolicyResult.FAILURE_EXCEPTION);
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
				this.completeOperation(operationAttempt, "Policy was unable to parse APP-C response status code field.", PolicyResult.FAILURE_EXCEPTION);
				return PolicyResult.FAILURE_EXCEPTION;
			}
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
				this.completeOperation(operationAttempt, appcResponse.getStatus().getDescription(), PolicyResult.FAILURE_EXCEPTION);
				if (this.policyResult != null && this.policyResult.equals(PolicyResult.FAILURE_TIMEOUT)) {
					return null;
				}
				return PolicyResult.FAILURE_EXCEPTION;
			case SUCCESS:
				//
				//
				//
				this.completeOperation(operationAttempt, appcResponse.getStatus().getDescription(), PolicyResult.SUCCESS);
				if (this.policyResult != null && this.policyResult.equals(PolicyResult.FAILURE_TIMEOUT)) {
					return null;
				}
				return PolicyResult.SUCCESS;
			case FAILURE:
				//
				//
				//
				this.completeOperation(operationAttempt, appcResponse.getStatus().getDescription(), PolicyResult.FAILURE);
				if (this.policyResult != null && this.policyResult.equals(PolicyResult.FAILURE_TIMEOUT)) {
					return null;
				}
				return PolicyResult.FAILURE;
			}
		}
		else if (response instanceof LCMResponseWrapper) {

		    LCMResponseWrapper dmaapResponse = (LCMResponseWrapper) response;

		    /*
		     * Parse out the operation attempt using the subrequestid
		     */
		    Integer operationAttempt = AppcLcmActorServiceProvider.parseOperationAttempt(dmaapResponse.getBody().getCommonHeader().getSubRequestId());
		    if (operationAttempt == null) {
		        this.completeOperation(operationAttempt, "Policy was unable to parse APP-C SubRequestID (it was null).", PolicyResult.FAILURE_EXCEPTION);
		    }

		    /*
		     * Process the APPCLCM response to see what PolicyResult
		     * should be returned
		     */
		    AbstractMap.SimpleEntry<PolicyResult, String> result = AppcLcmActorServiceProvider.processResponse(dmaapResponse);

		    if (result.getKey() != null) {
    		    this.completeOperation(operationAttempt, result.getValue(), result.getKey());
    		    if (PolicyResult.FAILURE_TIMEOUT.equals(this.policyResult)) {
                    return null;
                }
    		    return result.getKey();
		    }
		    return null;
		} else if (response instanceof SOResponseWrapper) {
			SOResponseWrapper msoResponse = (SOResponseWrapper) response;

			switch (msoResponse.SOResponse.httpResponseCode) {
			case 200:
			case 202:
				//
				// Consider it as success
				//
				this.completeOperation(this.attempts, msoResponse.SOResponse.httpResponseCode + " Success", PolicyResult.SUCCESS);
				if (this.policyResult != null && this.policyResult.equals(PolicyResult.FAILURE_TIMEOUT)) {
					return null;
				}
				return PolicyResult.SUCCESS;
			default:
				//
				// Consider it as failure
				//
				this.completeOperation(this.attempts, msoResponse.SOResponse.httpResponseCode + " Failed", PolicyResult.FAILURE);
				if (this.policyResult != null && this.policyResult.equals(PolicyResult.FAILURE_TIMEOUT)) {
					return null;
				}
				return PolicyResult.FAILURE;
			}

		} else if (response instanceof VFCResponse) {
			VFCResponse vfcResponse = (VFCResponse) response;

			if (vfcResponse.responseDescriptor.getStatus().equalsIgnoreCase("finished")) {
				//
				// Consider it as success
				//
				this.completeOperation(this.attempts, " Success", PolicyResult.SUCCESS);
				if (this.policyResult != null && this.policyResult.equals(PolicyResult.FAILURE_TIMEOUT)) {
					return null;
				}
				return PolicyResult.SUCCESS;
			} else {
				//
				// Consider it as failure
				//
				this.completeOperation(this.attempts, " Failed", PolicyResult.FAILURE);
				if (this.policyResult != null && this.policyResult.equals(PolicyResult.FAILURE_TIMEOUT)) {
					return null;
				}
				// increment operation attempts for retries
				this.attempts += 1;
				return PolicyResult.FAILURE;
			}
		}
		return null;
	}

	public Integer	getOperationTimeout() {
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

	public String	getOperationTimeoutString(int defaultTimeout) {
		Integer to = this.getOperationTimeout();
		if (to == null || to == 0) {
			return Integer.toString(defaultTimeout) + "s";
		}
		return to.toString() + "s";
	}

	public PolicyResult	getOperationResult() {
		return this.policyResult;
	}

	public String	getOperationMessage() {
		if (this.currentOperation != null && this.currentOperation.operation != null) {
			return this.currentOperation.operation.toMessage();
		}

		if (!this.operationHistory.isEmpty()) {
			return this.operationHistory.getLast().operation.toMessage();
		}
		return null;
	}

	public String	getOperationMessage(String guardResult) {
		if (this.currentOperation != null && this.currentOperation.operation != null) {
			return this.currentOperation.operation.toMessage()+ ", Guard result: " + guardResult;
		}
          
		if (!this.operationHistory.isEmpty()) {
			return this.operationHistory.getLast().operation.toMessage() + ", Guard result: " + guardResult;
		}
		return null;
	}

	public String	getOperationHistory() {
		if (this.currentOperation != null && this.currentOperation.operation != null) {
			return this.currentOperation.operation.toHistory();
		}
          
		if (!this.operationHistory.isEmpty()) {
			return this.operationHistory.getLast().operation.toHistory();
		}
		return null;
	}

	public LinkedList<ControlLoopOperation>	getHistory() {
		LinkedList<ControlLoopOperation> history = new LinkedList<ControlLoopOperation>();
		for (Operation op : this.operationHistory) {
			history.add(new ControlLoopOperation(op.operation));

		}
		return history;
	}

	public void		setOperationHasTimedOut() {
		//
		//
		//
		this.completeOperation(this.attempts, "Operation timed out", PolicyResult.FAILURE_TIMEOUT);
	}

	public void		setOperationHasGuardDeny() {
		//
		//
		//
		this.completeOperation(this.attempts, "Operation denied by Guard", PolicyResult.FAILURE_GUARD);
	}
	
	public void setOperationHasException(String message) {
		this.completeOperation(this.attempts, message, PolicyResult.FAILURE_EXCEPTION);
	}

	public boolean	isOperationComplete() {
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
			if (policy.getRetry() == null || policy.getRetry() == 0) {
				//
				// The result is the failure
				//
				return true;
			}
			//
			// Check retries
			//
			if (this.isRetriesMaxedOut()) {
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

	public boolean	isOperationRunning() {
		return (this.currentOperation != null);
	}

	private boolean	isRetriesMaxedOut() {
		if (policy.getRetry() == null || policy.getRetry() == 0) {
			//
			// There were NO retries specified, so declare
			// this as completed.
			//
			return (this.attempts > 0);
		}
		return (this.attempts > policy.getRetry());
	}

	private void	storeOperationInDataBase(){
		// Only store in DB if enabled
		boolean guardEnabled = "false".equalsIgnoreCase(PolicyEngine.manager.getEnvironmentProperty("guard.disabled"));
		if( !guardEnabled ){
			return;
		}


		// DB Properties
		Properties props = new Properties();
		if(PolicyEngine.manager.getEnvironmentProperty(Util.ONAP_KEY_URL) != null &&
				PolicyEngine.manager.getEnvironmentProperty(Util.ONAP_KEY_USER) != null &&
				PolicyEngine.manager.getEnvironmentProperty(Util.ONAP_KEY_PASS) != null){
			props.put(Util.ECLIPSE_LINK_KEY_URL, PolicyEngine.manager.getEnvironmentProperty(Util.ONAP_KEY_URL));
			props.put(Util.ECLIPSE_LINK_KEY_USER, PolicyEngine.manager.getEnvironmentProperty(Util.ONAP_KEY_USER));
			props.put(Util.ECLIPSE_LINK_KEY_PASS, PolicyEngine.manager.getEnvironmentProperty(Util.ONAP_KEY_PASS));
			props.put(PersistenceUnitProperties.CLASSLOADER, ControlLoopOperationManager.class.getClassLoader());
		}
		
		
		String OpsHistPU = System.getProperty("OperationsHistoryPU");
		if(OpsHistPU == null || !OpsHistPU.equals("TestOperationsHistoryPU")){
			OpsHistPU = "OperationsHistoryPU";
		}
		else{
			props.clear();
		}
		EntityManager em;
		try{
			em = Persistence.createEntityManagerFactory(OpsHistPU, props).createEntityManager();
		}catch(Exception e){
			logger.error("storeOperationInDataBase threw: ", e);
			return;
		}

		OperationsHistoryDbEntry newEntry = new OperationsHistoryDbEntry();

		newEntry.closedLoopName = this.onset.getClosedLoopControlName();
		newEntry.requestId = this.onset.getRequestID().toString();
		newEntry.actor = this.currentOperation.operation.getActor();
		newEntry.operation = this.currentOperation.operation.getOperation();
		newEntry.target = this.targetEntity;
		newEntry.starttime = Timestamp.from(this.currentOperation.operation.getStart());
		newEntry.subrequestId = this.currentOperation.operation.getSubRequestId(); 
		newEntry.endtime = new Timestamp(this.currentOperation.operation.getEnd().toEpochMilli());
		newEntry.message = this.currentOperation.operation.getMessage();
		newEntry.outcome = this.currentOperation.operation.getOutcome();

		em.getTransaction().begin();
		em.persist(newEntry);
		em.getTransaction().commit();

		em.close();

	}



	private void	completeOperation(Integer attempt, String message, PolicyResult result) {
		if (attempt == null) {
			logger.debug("attempt cannot be null (i.e. subRequestID)");
			return;
		}
		if (this.currentOperation != null) {
			if (this.currentOperation.attempt == attempt.intValue()) {
				this.currentOperation.operation.setEnd(Instant.now());
				this.currentOperation.operation.setMessage(message);
				this.currentOperation.operation.setOutcome(result.toString());
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
				op.operation.setEnd(Instant.now());
				op.operation.setMessage(message);
				op.operation.setOutcome(result.toString());
				op.policyResult = result;
				return;
			}
		}
		logger.debug("Could not find associated operation");

	}

}
