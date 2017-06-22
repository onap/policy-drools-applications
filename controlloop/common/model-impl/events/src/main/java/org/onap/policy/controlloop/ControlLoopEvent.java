/*-
 * ============LICENSE_START=======================================================
 * controlloop
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

package org.onap.policy.controlloop;

import java.io.Serializable;
import java.util.UUID;

public abstract class ControlLoopEvent implements Serializable {
	
	private static final long serialVersionUID = 2391252138583119195L;
	
	public String closedLoopControlName;
	public String version = "1.0.2";
	public UUID requestID;
	public String closedLoopEventClient;
	public ControlLoopTargetType target_type;
	public String target;
	public String from;
	public String policyScope;
	public String policyName;
	public String policyVersion;
	public ControlLoopEventStatus closedLoopEventStatus;
	
	public ControlLoopEvent() {
		
	}
	
	public ControlLoopEvent(ControlLoopEvent event) {
		if (event == null) {
			return;
		}
		this.closedLoopControlName = event.closedLoopControlName;
		this.requestID = event.requestID;
		this.closedLoopEventClient = event.closedLoopEventClient;
		this.target_type = event.target_type;
		this.target = event.target;
		this.from = event.from;
		this.policyScope = event.policyScope;
		this.policyName = event.policyName;
		this.policyVersion = event.policyVersion;
		this.closedLoopEventStatus = event.closedLoopEventStatus;
	}

	public boolean	isEventStatusValid() {
		if (this.closedLoopEventStatus == null) {
			return false;
		}
		return true;
	}

}
