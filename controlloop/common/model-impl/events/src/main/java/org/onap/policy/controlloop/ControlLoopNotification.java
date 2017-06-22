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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public abstract class ControlLoopNotification implements Serializable {

	private static final long serialVersionUID = 7538596984567127915L;

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
	public ControlLoopNotificationType notification;
	public String message;
	public ZonedDateTime notificationTime = ZonedDateTime.now(ZoneOffset.UTC);;
	public Integer OPS_CL_timer;
	public List<ControlLoopOperation> history = new LinkedList<ControlLoopOperation>();
	
	public ControlLoopNotification() {
		
	}
	
	public ControlLoopNotification(ControlLoopEvent event) {
		this.closedLoopControlName = event.closedLoopControlName;
		this.requestID = event.requestID;
		this.closedLoopEventClient = event.closedLoopEventClient;
		this.target_type = event.target_type;
		this.target = event.target;
	}

}
