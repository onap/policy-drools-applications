/*-
 * ============LICENSE_START=======================================================
 * demo
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

package org.openecomp.policy.template.demo;

import java.util.UUID;

import org.openecomp.policy.controlloop.VirtualControlLoopEvent;
import org.openecomp.policy.controlloop.VirtualControlLoopNotification;
import org.openecomp.policy.template.demo.ControlLoopException;
import org.openecomp.policy.controlloop.ControlLoopNotificationType;
import org.openecomp.policy.controlloop.ControlLoopEventStatus;

public class EventManager {
	/*
	 * 
	 */
	public final String closedLoopControlName;
	public final UUID requestID;
	public final String target;
	public String controlLoopResult;
	
	private boolean isActivated = false;
	private VirtualControlLoopEvent onset;
	private VirtualControlLoopEvent abatement;
	private Integer numOnsets = 0;
	private Integer numAbatements = 0;
	
	
	
	public EventManager(String closedLoopControlName, UUID requestID, String target) {
		this.closedLoopControlName = closedLoopControlName;
		this.requestID = requestID;
		this.target = target;
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
	
	public void setControlLoopResult(String result) {
		this.controlLoopResult = result;		
	}
	
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
			notification.notification = ControlLoopNotificationType.ACTIVE;
			//
			// Set ourselves as active
			//
			this.isActivated = true;
		} catch (ControlLoopException e) {
			notification.notification = ControlLoopNotificationType.REJECTED;
			notification.message = e.getMessage();
		}
		return notification;
		
	}
	
	public static void checkEventSyntax(VirtualControlLoopEvent event) throws ControlLoopException {
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
		if (event.AAI == null) {
			throw new ControlLoopException("AAI is null");
		}
		if (event.target == null || event.target.length() < 1) {
			throw new ControlLoopException("No target field");
		} else {
			if (! event.target.equalsIgnoreCase("VM_NAME") &&
				! event.target.equalsIgnoreCase("VNF_NAME") &&
				! event.target.equalsIgnoreCase("vserver.vserver-name") &&
				! event.target.equalsIgnoreCase("generic-vnf.vnf-id") ) {
				throw new ControlLoopException("target field invalid");
			}
		}
	}
	
	public enum NEW_EVENT_STATUS {
		FIRST_ONSET,
		SUBSEQUENT_ONSET,
		FIRST_ABATEMENT,
		SUBSEQUENT_ABATEMENT,
		SYNTAX_ERROR
		;
	}
	
	public NEW_EVENT_STATUS	onNewEvent(VirtualControlLoopEvent event) {
		try {
			EventManager.checkEventSyntax(event);
			if (event.closedLoopEventStatus == ControlLoopEventStatus.ONSET) {
				//
				// Check if this is our original ONSET
				//
				if (event.equals(this.onset)) {
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
			return NEW_EVENT_STATUS.SYNTAX_ERROR;
		}
	}
}
