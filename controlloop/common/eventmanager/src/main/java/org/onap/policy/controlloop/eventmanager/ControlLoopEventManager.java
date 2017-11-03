/*-
 * ============LICENSE_START=======================================================
 * controlloop event manager
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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.UUID;

import org.onap.policy.aai.AAIGETVnfResponse;
import org.onap.policy.aai.AAIGETVserverResponse;
import org.onap.policy.aai.AAIManager;
import org.onap.policy.aai.util.AAIException;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.policy.FinalResult;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.processor.ControlLoopProcessor;
import org.onap.policy.guard.GuardResult;
import org.onap.policy.guard.LockCallback;
import org.onap.policy.guard.PolicyGuard;
import org.onap.policy.guard.PolicyGuard.LockResult;
import org.onap.policy.guard.TargetLock;
import org.onap.policy.drools.system.PolicyEngine; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlLoopEventManager implements LockCallback, Serializable {
	
	/**
	 * 
	 */
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
	private AAIGETVnfResponse vnfResponse = null;
	private AAIGETVserverResponse vserverResponse = null;
	private static String aaiHostURL; 
	private static String aaiUser; 
	private static String aaiPassword;
	
	private static Collection<String> requiredAAIKeys = new ArrayList<>();
	static {
		requiredAAIKeys.add("AICVServerSelfLink");
		requiredAAIKeys.add("AICIdentity");
		requiredAAIKeys.add("is_closed_loop_disabled");
		requiredAAIKeys.add("VM_NAME");
	}

	public ControlLoopEventManager(String closedLoopControlName, UUID requestID) {
		this.closedLoopControlName = closedLoopControlName;
		this.requestID = requestID;
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
	
	public VirtualControlLoopEvent	getOnsetEvent() {
		return this.onset;
	}
	
	public VirtualControlLoopEvent getAbatementEvent() {
		return this.abatement;
	}
	
	public ControlLoopProcessor getProcessor() {
		return this.processor;
	}

	public VirtualControlLoopNotification	activate(VirtualControlLoopEvent event) {
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
			notification.notification = ControlLoopNotificationType.ACTIVE;
			//
			// Set ourselves as active
			//
			this.isActivated = true;
		} catch (ControlLoopException e) {
			logger.error("{}: activate threw: ",this, e);
			notification.notification = ControlLoopNotificationType.REJECTED;
			notification.message = e.getMessage();
		}
		return notification;
	}
	
	
	
	public VirtualControlLoopNotification	activate(String yamlSpecification, VirtualControlLoopEvent event) {
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
			String decodedYaml = null;
			try {
				decodedYaml = URLDecoder.decode(yamlSpecification, "UTF-8");
				if (decodedYaml != null && decodedYaml.length() > 0) {
					yamlSpecification = decodedYaml;
				}
			} catch (UnsupportedEncodingException e) {
				logger.error("{}: activate threw: ",this, e);
			}
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
			notification.notification = ControlLoopNotificationType.ACTIVE;
			//
			// Set ourselves as active
			//
			this.isActivated = true;
		} catch (ControlLoopException e) {
			logger.error("{}: activate threw: ",this, e);
			notification.notification = ControlLoopNotificationType.REJECTED;
			notification.message = e.getMessage();
		}
		return notification;
	}
	
	public VirtualControlLoopNotification	isControlLoopFinal() throws ControlLoopException {
		//
		// Check if they activated us
		//
		if (this.isActivated == false) {
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
			notification.notification = ControlLoopNotificationType.FINAL_FAILURE;
			notification.message = "Control Loop timed out";
			notification.history.addAll(this.controlLoopHistory);
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
			notification.message = "Exception in processing closed loop";
		case FINAL_FAILURE:
		case FINAL_FAILURE_RETRIES:
		case FINAL_FAILURE_TIMEOUT:
		case FINAL_FAILURE_GUARD:
			notification.notification = ControlLoopNotificationType.FINAL_FAILURE;
			break;
		case FINAL_OPENLOOP:
			notification.notification = ControlLoopNotificationType.FINAL_OPENLOOP;
			break;
		case FINAL_SUCCESS:
			notification.notification = ControlLoopNotificationType.FINAL_SUCCESS;
			break;
		default:
			return null;
		}
		//
		// Be sure to add all the history
		//
		notification.history.addAll(this.controlLoopHistory);
		return notification;
	}
		
	public ControlLoopOperationManager	processControlLoop() throws ControlLoopException, AAIException {
		//
		// Check if they activated us
		//
		if (this.isActivated == false) {
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
				logger.debug("Finishing {} result is {}", this.currentOperation.policy.getRecipe(), this.currentOperation.getOperationResult());
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
			logger.debug("Cannot finish current operation {} does not match given operation {}", this.currentOperation.policy, operation.policy);
			return;
		}
		throw new ControlLoopException("No operation to finish.");
	}
	
	public synchronized LockResult<GuardResult, TargetLock>	lockCurrentOperation() throws ControlLoopException {
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
			return new LockResult<GuardResult, TargetLock>(GuardResult.LOCK_ACQUIRED, this.targetLock);
		} else {
			//
			// Ask the Guard
			//
			LockResult<GuardResult, TargetLock> lockResult = PolicyGuard.lockTarget(
																		this.currentOperation.policy.getTarget().getType(), 
																		this.currentOperation.getTargetEntity(),
																		this.onset.requestID,
																		this);
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
	
	public synchronized TargetLock unlockCurrentOperation() {
		if (this.targetLock == null) {
			return null;
		}
		if (PolicyGuard.unlockTarget(this.targetLock) == true) {
			TargetLock returnLock = this.targetLock;
			this.targetLock = null;
			return returnLock;
		}
		return null;
	}
	
	public enum NEW_EVENT_STATUS {
		FIRST_ONSET,
		SUBSEQUENT_ONSET,
		FIRST_ABATEMENT,
		SUBSEQUENT_ABATEMENT,
		SYNTAX_ERROR
		;
	}
		
	public NEW_EVENT_STATUS	onNewEvent(VirtualControlLoopEvent event) throws AAIException {
		try {
			this.checkEventSyntax(event);
			if (event.closedLoopEventStatus == ControlLoopEventStatus.ONSET) {
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
			} else if (event.closedLoopEventStatus == ControlLoopEventStatus.ABATED) {
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
			} else {
				return NEW_EVENT_STATUS.SYNTAX_ERROR;
			}
		} catch (ControlLoopException e) {
			logger.error("{}: onNewEvent threw: ",this, e);
			return NEW_EVENT_STATUS.SYNTAX_ERROR;
		}
	}
	
	public VirtualControlLoopNotification setControlLoopTimedOut() {
		this.controlLoopTimedOut = FinalResult.FINAL_FAILURE_TIMEOUT;
		VirtualControlLoopNotification notification = new VirtualControlLoopNotification(this.onset);
		notification.notification = ControlLoopNotificationType.FINAL_FAILURE;
		notification.message = "Control Loop timed out";
		notification.history.addAll(this.controlLoopHistory);
		return notification;			
	}
	
	public boolean isControlLoopTimedOut() {
		return (this.controlLoopTimedOut == FinalResult.FINAL_FAILURE_TIMEOUT);
	}
	
	public int	getControlLoopTimeout(Integer defaultTimeout) {
		if (this.processor != null && this.processor.getControlLoop() != null) {
			return this.processor.getControlLoop().getTimeout();
		}
		if (defaultTimeout != null) {
			return defaultTimeout;
		}
		return 0;
	}
	
	public AAIGETVnfResponse getVnfResponse() {
		return vnfResponse; 
	}

	public AAIGETVserverResponse getVserverResponse() {
		return vserverResponse; 
	}
	
	public void checkEventSyntax(VirtualControlLoopEvent event) throws ControlLoopException {
		if (event.closedLoopEventStatus == null || 
				(event.closedLoopEventStatus != ControlLoopEventStatus.ONSET &&
				event.closedLoopEventStatus != ControlLoopEventStatus.ABATED)) {
			throw new ControlLoopException("Invalid value in closedLoopEventStatus");
		}
		if (event.closedLoopControlName == null || event.closedLoopControlName.length() < 1) {
			throw new ControlLoopException("No control loop name");
		}
		if (event.requestID == null) {
			throw new ControlLoopException("No request ID");
		}
		if (event.closedLoopEventStatus == ControlLoopEventStatus.ABATED) {
			return;
		}
		if (event.target == null || event.target.length() < 1) {
			throw new ControlLoopException("No target field");
		} else if (! "VM_NAME".equalsIgnoreCase(event.target) &&
				! "VNF_NAME".equalsIgnoreCase(event.target) &&
				! "vserver.vserver-name".equalsIgnoreCase(event.target) &&
				! "generic-vnf.vnf-id".equalsIgnoreCase(event.target) &&
				! "generic-vnf.vnf-name".equalsIgnoreCase(event.target) ) {
			throw new ControlLoopException("target field invalid - expecting VM_NAME or VNF_NAME");
		}
		if (event.AAI == null) {
            throw new ControlLoopException("AAI is null");
        }
        if (event.AAI.get("generic-vnf.vnf-id") == null && event.AAI.get("vserver.vserver-name") == null &&
                event.AAI.get("generic-vnf.vnf-name") == null) {
            throw new ControlLoopException("generic-vnf.vnf-id or generic-vnf.vnf-name or vserver.vserver-name information missing");
        }
	}
	
	public void queryAai(VirtualControlLoopEvent event) throws AAIException {
		if (event.AAI.get("vserver.is-closed-loop-disabled") == null && 
		        event.AAI.get("generic-vnf.is-closed-loop-disabled") == null) {
			try {
				if (event.AAI.get("generic-vnf.vnf-id") != null) {
					vnfResponse = getAAIVnfInfo(event); 
					if (vnfResponse == null) {
						throw new AAIException("AAI Response is null (query by vnf-id)");
					}
					if (vnfResponse.requestError != null) {
						throw new AAIException("AAI Responded with a request error (query by vnf-id)");
					}
					if (isClosedLoopDisabled(vnfResponse) == true) {
						throw new AAIException("is-closed-loop-disabled is set to true");	
					}
				} else if (event.AAI.get("generic-vnf.vnf-name") != null) {
					vnfResponse = getAAIVnfInfo(event); 
					if (vnfResponse == null) {
						throw new AAIException("AAI Response is null (query by vnf-name)");
					}
					if (vnfResponse.requestError != null) {
						throw new AAIException("AAI Responded with a request error (query by vnf-name)");
					}
					if (isClosedLoopDisabled(vnfResponse) == true) {
						throw new AAIException("is-closed-loop-disabled is set to true");	
					}
				} else if (event.AAI.get("vserver.vserver-name") != null) {
					vserverResponse = getAAIVserverInfo(event); 
					if (vserverResponse == null) {
						throw new AAIException("AAI Response is null (query by vserver-name)");
					}
					if (vserverResponse.requestError != null) {
						throw new AAIException("AAI responded with a request error (query by vserver-name)");
					}
					if (isClosedLoopDisabled(vserverResponse) == true) {
						throw new AAIException("is-closed-loop-disabled is set to true");	
					}
				}
			} catch (Exception e) {
				logger.error("Exception from getAAIInfo: ", e);
				throw new AAIException("Exception from getAAIInfo: " + e.toString());
			}
		} else if (isClosedLoopDisabled(event)) {
			throw new AAIException("is-closed-loop-disabled is set to true");
		}
	}
	
	public static boolean isClosedLoopDisabled(AAIGETVnfResponse aaiResponse) {
       	if (aaiResponse != null && aaiResponse.isClosedLoopDisabled != null) {
       		String value = aaiResponse.isClosedLoopDisabled; 
       		if ("true".equalsIgnoreCase(value) || "T".equalsIgnoreCase(value) ||
       			"yes".equalsIgnoreCase(value)  || "Y".equalsIgnoreCase(value)) {
       			return true; 
       		} 
       	}
  
		return false; 
	}
	
	public static boolean isClosedLoopDisabled(AAIGETVserverResponse aaiResponse) {
       	if (aaiResponse != null && aaiResponse.isClosedLoopDisabled != null) {
       		String value = aaiResponse.isClosedLoopDisabled; 
       		if ("true".equalsIgnoreCase(value) || "T".equalsIgnoreCase(value) ||
           		"yes".equalsIgnoreCase(value)  || "Y".equalsIgnoreCase(value)) {
       			return true; 
       		} 
       	}
  
		return false; 
	}
	
	public static boolean isClosedLoopDisabled(VirtualControlLoopEvent event) {
		if ("true".equalsIgnoreCase(event.AAI.get("vserver.is-closed-loop-disabled")) || 
		    "T".equalsIgnoreCase(event.AAI.get("vserver.is-closed-loop-disabled")) || 
		    "yes".equalsIgnoreCase(event.AAI.get("vserver.is-closed-loop-disabled")) || 
		    "Y".equalsIgnoreCase(event.AAI.get("vserver.is-closed-loop-disabled"))) { 
			return true; 
		}
		else if ("true".equalsIgnoreCase(event.AAI.get("generic-vnf.is-closed-loop-disabled")) || 
	            "T".equalsIgnoreCase(event.AAI.get("generic-vnf.is-closed-loop-disabled")) || 
	            "yes".equalsIgnoreCase(event.AAI.get("generic-vnf.is-closed-loop-disabled")) || 
	            "Y".equalsIgnoreCase(event.AAI.get("generic-vnf.is-closed-loop-disabled"))) { 
	            return true; 
	    }
		return false;
	}
	
	public static AAIGETVserverResponse getAAIVserverInfo(VirtualControlLoopEvent event) throws ControlLoopException {
		UUID requestID = event.requestID;  
		AAIGETVserverResponse response = null; 
		String vserverName = event.AAI.get("vserver.vserver-name"); 

		try {
	        if (vserverName != null) {
	           aaiHostURL  = PolicyEngine.manager.getEnvironmentProperty("aai.url"); 
	           aaiUser     = PolicyEngine.manager.getEnvironmentProperty("aai.username"); 
	           aaiPassword = PolicyEngine.manager.getEnvironmentProperty("aai.password");
	           String aaiGetQueryByVserver = "/aai/v11/nodes/vservers?vserver-name="; 
 	   		   String url = aaiHostURL + aaiGetQueryByVserver; 
 	   		   logger.info("url: " + url);
			   response = AAIManager.getQueryByVserverName(url, aaiUser, aaiPassword, requestID, vserverName);
	        } 
	    } catch (Exception e) {
	    	logger.error("getAAIVserverInfo exception: ", e);
        	throw new ControlLoopException("Exception in getAAIVserverInfo: ", e);
        }
		
		return response; 
	}
	
	public static AAIGETVnfResponse getAAIVnfInfo(VirtualControlLoopEvent event) throws ControlLoopException {
		UUID requestID = event.requestID;  
		AAIGETVnfResponse response = null; 
		String vnfName = event.AAI.get("generic-vnf.vnf-name"); 
		String vnfID   = event.AAI.get("generic-vnf.vnf-id"); 
 
		aaiHostURL  = PolicyEngine.manager.getEnvironmentProperty("aai.url"); 
        aaiUser     = PolicyEngine.manager.getEnvironmentProperty("aai.username"); 
        aaiPassword = PolicyEngine.manager.getEnvironmentProperty("aai.password");
		
		try {
            if (vnfName != null) {
 	           String aaiGetQueryByVnfName = "/aai/v11/network/generic-vnfs/generic-vnf?vnf-name="; 
  	   		   String url = aaiHostURL + aaiGetQueryByVnfName; 
  	   		   logger.info("url: " + url);
			   response = AAIManager.getQueryByVnfName(url, aaiUser, aaiPassword, requestID, vnfName);	        	
	        } else if (vnfID != null) {
	 	       String aaiGetQueryByVnfID = "/aai/v11/network/generic-vnfs/generic-vnf/"; 
	  	   	   String url = aaiHostURL + aaiGetQueryByVnfID; 
	  	   	   logger.info("url: " + url);
			   response = AAIManager.getQueryByVnfID(url, aaiUser, aaiPassword, requestID, vnfID);	        	
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
				+ ", processor=" + processor + ", onset=" + (onset != null ? onset.requestID : "null") + ", numOnsets=" + numOnsets + ", numAbatements="
				+ numAbatements + ", isActivated="
				+ isActivated + ", currentOperation=" + currentOperation + ", targetLock=" + targetLock + "]";
	}
	
}
