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
import java.util.LinkedList;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import org.onap.policy.appc.Response;
import org.onap.policy.appc.ResponseCode;

import org.onap.policy.controlloop.ControlLoopEvent;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.VirtualControlLoopEvent;

import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.controlloop.actor.appc.APPCActorServiceProvider;


public class ControlLoopOperationManager implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3773199283624595410L;

	@Override
	public String toString() {
		return "ControlLoopOperationManager [onset=" + (onset != null ? onset.requestID : "null") + ", policy=" 
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
	public final Policy policy;

	//
	// Properties used to track the Operation
	//
	private int attempts = 0;
	private Operation currentOperation = null;
	private LinkedList<Operation> operationHistory = new LinkedList<Operation>();
	private PolicyResult policyResult = null;
	private ControlLoopEventManager eventManager = null;

	public ControlLoopEventManager getEventManager() {
		return eventManager;
	}

	public void setEventManager(ControlLoopEventManager eventManager) {
		this.eventManager = eventManager;
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
	private Object operationRequest;
	
	public Object getOperationRequest() {
		return operationRequest;
	}

	public String getGuardApprovalStatus() {
		return guardApprovalStatus;
	}
	public void setGuardApprovalStatus(String guardApprovalStatus) {
		this.guardApprovalStatus = guardApprovalStatus;
	}
	
	
	public ControlLoopOperationManager(/*ATTControlLoopEvent*/ControlLoopEvent onset, Policy policy, ControlLoopEventManager em) throws ControlLoopException {
		this.onset = onset;
		this.policy = policy;
		this.guardApprovalStatus = "NONE";
		this.eventManager = em;
		
		//
		// Let's make a sanity check
		//
		switch (policy.getActor()) {
		case "APPC":
			break;
		case "AOTS":
			break;
		case "MSO":
			break;
		case "SDNO":
			break;
		case "SDNR":
			break;
		default:
			throw new ControlLoopException("ControlLoopEventManager: policy has an unknown actor.");
		}
	}
	
	public Object startOperation(/*VirtualControlLoopEvent*/ControlLoopEvent onset) {
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
		operation.operation.actor = this.policy.getActor().toString();
		operation.operation.operation = this.policy.getRecipe();
		operation.operation.target = this.policy.getTarget().toString();
		operation.operation.subRequestId = Integer.toString(operation.attempt);
		//
		// Now determine which actor we need to construct a request for
		//
		switch (policy.getActor()) {
		case "APPC":
			//Request request = APPCActorServiceProvider.constructRequest(onset, operation.operation, this.policy);
			this.operationRequest = APPCActorServiceProvider.constructRequest((VirtualControlLoopEvent)onset, operation.operation, this.policy);
			//
			// Save the operation
			//
			this.currentOperation = operation;
			//System.out.print("*************   BEFORE STORING.....");
			//this.storeOperationInDataBase("startOperation");
			//System.out.print("*************   AFTER STORING.....");
			//
			return operationRequest;
		case "MSO":
			//
			// We are not supporting MSO interface at the moment
			//
			System.out.println("We are not supporting MSO actor in the latest release.");
			return null;
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
				operationAttempt = Integer.parseInt(appcResponse.CommonHeader.SubRequestID);
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
			if (appcResponse.Status == null) {
				//
				// We cannot tell what happened if this doesn't exist
				//
				this.completeOperation(operationAttempt, "Policy was unable to parse APP-C response status field (it was null).", PolicyResult.FAILURE_EXCEPTION);
				return PolicyResult.FAILURE_EXCEPTION;
			}
			//
			// Get the Response Code
			//
			ResponseCode code = ResponseCode.toResponseCode(appcResponse.Status.Code);
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
				this.completeOperation(operationAttempt, appcResponse.getStatus().Description, PolicyResult.FAILURE_EXCEPTION);
				if (this.policyResult != null && this.policyResult.equals(PolicyResult.FAILURE_TIMEOUT)) {
					return null;
				}
				return PolicyResult.FAILURE_EXCEPTION;
			case SUCCESS:
				//
				//
				//
				this.completeOperation(operationAttempt, appcResponse.getStatus().Description, PolicyResult.SUCCESS);
				if (this.policyResult != null && this.policyResult.equals(PolicyResult.FAILURE_TIMEOUT)) {
					return null;
				}
				return PolicyResult.SUCCESS;
			case FAILURE:
				//
				//
				//
				this.completeOperation(operationAttempt, appcResponse.getStatus().Description, PolicyResult.FAILURE);
				if (this.policyResult != null && this.policyResult.equals(PolicyResult.FAILURE_TIMEOUT)) {
					return null;
				}
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
			System.out.println("getOperationTimeout returning 0");
			return 0;
		}
		System.out.println("getOperationTimeout returning " + this.policy.getTimeout());
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
		if (this.operationHistory != null && this.operationHistory.size() > 0) {
			return this.operationHistory.getLast().operation.toMessage();
		}
		return null;
	}
	
	public String	getOperationMessage(String guardResult) {
		if (this.currentOperation != null && this.currentOperation.operation != null) {
			return this.currentOperation.operation.toMessage()+ ", Guard result: " + guardResult;
		}
		if (this.operationHistory != null && this.operationHistory.size() > 0) {
			return this.operationHistory.getLast().operation.toMessage() + ", Guard result: " + guardResult;
		}
		return null;
	}
	
	public String	getOperationHistory() {
		if (this.currentOperation != null && this.currentOperation.operation != null) {
			return this.currentOperation.operation.toHistory();
		}
		if (this.operationHistory != null && this.operationHistory.size() > 0) {
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
		
		EntityManager em;
		try{
			em = Persistence.createEntityManagerFactory("OperationsHistoryPU").createEntityManager();//emf.createEntityManager();		
		}catch(Exception e){
			System.err.println("Test thread got Exception " + e.getLocalizedMessage() + " Can't write to Operations History DB.");
			return;	
		}
			
		OperationsHistoryDbEntry newEntry = new OperationsHistoryDbEntry(); 
			
		newEntry.closedLoopName = this.onset.closedLoopControlName;
		newEntry.requestId = this.onset.requestID.toString();
		newEntry.actor = this.currentOperation.operation.actor;
		newEntry.operation = this.currentOperation.operation.operation;
		newEntry.target = this.eventManager.getTargetInstance(this.policy);
		newEntry.starttime = Timestamp.from(this.currentOperation.operation.start);
		newEntry.subrequestId = this.currentOperation.operation.subRequestId;
		newEntry.endtime = new Timestamp(this.currentOperation.operation.end.toEpochMilli());
		newEntry.message = this.currentOperation.operation.message;
		newEntry.outcome = this.currentOperation.operation.outcome;
			
		em.getTransaction().begin();
		em.persist(newEntry);
		em.getTransaction().commit();
			
		em.close();

	}

	
	
	private void	completeOperation(Integer attempt, String message, PolicyResult result) {
		if (attempt == null) {
			System.out.println("attempt cannot be null (i.e. subRequestID)");
			return;
		}
		if (this.currentOperation != null) {
			if (this.currentOperation.attempt == attempt.intValue()) {
				this.currentOperation.operation.end = Instant.now();
				this.currentOperation.operation.message = message;
				this.currentOperation.operation.outcome = result.toString();
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
			System.out.println("not current");
		}
		for (Operation op : this.operationHistory) {
			if (op.attempt == attempt.intValue()) {
				op.operation.end = Instant.now();
				op.operation.message = message;
				op.operation.outcome = result.toString();
				op.policyResult = result;
				return;
			}
		}
		System.out.println("Could not find associated operation");
		
	}
	
}
