/*-
 * ============LICENSE_START=======================================================
 * vFW simulator
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

package org.openecomp.policy.sim.vfw;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import org.openecomp.policy.controlloop.ControlLoopEventStatus;
import org.openecomp.policy.controlloop.ControlLoopTargetType;
import org.openecomp.policy.controlloop.VirtualControlLoopEvent;

public class OnsetEvent implements Serializable {
	
	private static final long serialVersionUID = -7904064194557621526L;
	
	public static String DEFAULT_CLOSEDLOOP_CONTROL_NAME = "CL-FRWL-LOW-TRAFFIC-SIG-d925ed73-8231-4d02-9545-db4e101f88f8";
	public static String DEFAULT_CLOSEDLOOP_EVENT_CLIENT = "tca.instance00001";
	public static String DEFAULT_REQUEST_ID = "664be3d2-6c12-4f4b-a3e7-c349acced200";
	public static String DEFAULT_TARGET = "generic-vnf.vnf-id";
	public static String DEFAULT_TARGET_TYPE = "VF";
	public static String DEFAULT_FROM = "DCAE";

	public static String DEFAULT_AAI_VNF_ID_NAME = DEFAULT_TARGET;
	public static String DEFAULT_AAI_VNF_ID_VALUE = "fw0001vm001fw001";
	
	public static String DEFAULT_AAI_VSERVER_NAME = "vserver.vserver-name";
	public static String DEFAULT_AAI_VSERVER_VALUE = "vserver-name-16102016-aai3255-data-11-1";

	public String closedLoopControlName = DEFAULT_CLOSEDLOOP_CONTROL_NAME;
	public String closedLoopEventClient = DEFAULT_CLOSEDLOOP_EVENT_CLIENT;
	public String requestID = UUID.randomUUID().toString();
	public String target = DEFAULT_TARGET;
	public String targetType = DEFAULT_TARGET_TYPE;
	public String aaIVnfId = DEFAULT_AAI_VNF_ID_VALUE;
	public String aaIServerName = DEFAULT_AAI_VSERVER_VALUE;
	public String from = "DCAE";
	
	public String dcaeTopic = "DCAE-CL-EVENT";
	public String appcTopic = "APPC-CL";
	public int appcResponseCode = 100;
	
	public OnsetEvent() {
		super();
	}
	
	public OnsetEvent(String closedLoopControlName) {
		super();
		
		if (closedLoopControlName != null && !closedLoopControlName.isEmpty())
			this.closedLoopControlName = closedLoopControlName;
	}
	
	public OnsetEvent(String closedLoopControlName, String dcaeTopic, String appcTopic, int code) {
		super();
		
		if (closedLoopControlName != null && !closedLoopControlName.isEmpty())
			this.closedLoopControlName = closedLoopControlName;
		
		if (dcaeTopic != null && !dcaeTopic.isEmpty())
			this.dcaeTopic = dcaeTopic;
		
		if (appcTopic != null && !appcTopic.isEmpty())
			this.appcTopic = appcTopic;
		
		this.appcResponseCode = code;
	}
	
	public OnsetEvent(String closedLoopControlName, String requestID, String dcaeTopic, String appcTopic, int code) {
		super();
		
		if (closedLoopControlName != null && !closedLoopControlName.isEmpty())
			this.closedLoopControlName = closedLoopControlName;
		
		if (requestID != null)
			this.requestID = requestID;
		
		if (dcaeTopic != null && !dcaeTopic.isEmpty())
			this.dcaeTopic = dcaeTopic;
		
		if (appcTopic != null && !appcTopic.isEmpty())
			this.appcTopic = appcTopic;
		
		this.appcResponseCode = code;
	}

	public OnsetEvent(String closedLoopControlName, String closedLoopEventClient, 
			            String requestID, String target,
			            String targetType, String aaIVnfId, 
			            String aaIServerName, String from,
			            String dcaeTopic, String appcTopic,
			            int code) {
		super();
		
		if (closedLoopControlName != null && !closedLoopControlName.isEmpty())
			this.closedLoopControlName = closedLoopControlName;
		
		if (closedLoopEventClient != null && !closedLoopEventClient.isEmpty())
			this.closedLoopEventClient = closedLoopEventClient;
		
		if (requestID != null)
			this.requestID = requestID;
		
		if (target != null && !target.isEmpty())
			this.target = target;
		
		if (targetType != null && !targetType.isEmpty())
			this.targetType = targetType;
		
		if (aaIVnfId != null && !aaIVnfId.isEmpty())
			this.aaIVnfId = aaIVnfId;
		
		if (aaIServerName != null && !aaIServerName.isEmpty())
			this.aaIServerName = aaIServerName;
		
		if (from != null && !from.isEmpty())
			this.from = from;
		
		if (dcaeTopic != null && !dcaeTopic.isEmpty())
			this.dcaeTopic = dcaeTopic;
		
		if (appcTopic != null && !appcTopic.isEmpty())
			this.appcTopic = appcTopic;
		
		this.appcResponseCode = code;
	}
	
	public VirtualControlLoopEvent toDcaeOnset() {
		
		VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
		
		onsetEvent.closedLoopControlName = this.closedLoopControlName;
		onsetEvent.requestID = UUID.fromString(this.requestID);
		onsetEvent.closedLoopEventClient = this.closedLoopEventClient;
		onsetEvent.target_type = ControlLoopTargetType.valueOf(this.targetType);
		onsetEvent.target = this.target;
		onsetEvent.from = this.from;
		onsetEvent.closedLoopAlarmStart = Instant.now();
		onsetEvent.AAI = new HashMap<String, String>();
		onsetEvent.AAI.put(this.target, this.aaIVnfId);
		onsetEvent.AAI.put(DEFAULT_AAI_VSERVER_NAME, "vserver-name-16102016-aai3255-data-11-1");
		onsetEvent.closedLoopEventStatus = ControlLoopEventStatus.ONSET;
		
		return onsetEvent;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((aaIServerName == null) ? 0 : aaIServerName.hashCode());
		result = prime * result + ((aaIVnfId == null) ? 0 : aaIVnfId.hashCode());
		result = prime * result + appcResponseCode;
		result = prime * result + ((appcTopic == null) ? 0 : appcTopic.hashCode());
		result = prime * result + ((closedLoopControlName == null) ? 0 : closedLoopControlName.hashCode());
		result = prime * result + ((closedLoopEventClient == null) ? 0 : closedLoopEventClient.hashCode());
		result = prime * result + ((dcaeTopic == null) ? 0 : dcaeTopic.hashCode());
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + ((requestID == null) ? 0 : requestID.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		result = prime * result + ((targetType == null) ? 0 : targetType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OnsetEvent other = (OnsetEvent) obj;
		if (aaIServerName == null) {
			if (other.aaIServerName != null)
				return false;
		} else if (!aaIServerName.equals(other.aaIServerName))
			return false;
		if (aaIVnfId == null) {
			if (other.aaIVnfId != null)
				return false;
		} else if (!aaIVnfId.equals(other.aaIVnfId))
			return false;
		if (appcResponseCode != other.appcResponseCode)
			return false;
		if (appcTopic == null) {
			if (other.appcTopic != null)
				return false;
		} else if (!appcTopic.equals(other.appcTopic))
			return false;
		if (closedLoopControlName == null) {
			if (other.closedLoopControlName != null)
				return false;
		} else if (!closedLoopControlName.equals(other.closedLoopControlName))
			return false;
		if (closedLoopEventClient == null) {
			if (other.closedLoopEventClient != null)
				return false;
		} else if (!closedLoopEventClient.equals(other.closedLoopEventClient))
			return false;
		if (dcaeTopic == null) {
			if (other.dcaeTopic != null)
				return false;
		} else if (!dcaeTopic.equals(other.dcaeTopic))
			return false;
		if (from == null) {
			if (other.from != null)
				return false;
		} else if (!from.equals(other.from))
			return false;
		if (requestID == null) {
			if (other.requestID != null)
				return false;
		} else if (!requestID.equals(other.requestID))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		if (targetType == null) {
			if (other.targetType != null)
				return false;
		} else if (!targetType.equals(other.targetType))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "OnsetEvent [closedLoopControlName=" + closedLoopControlName + ", closedLoopEventClient="
				+ closedLoopEventClient + ", requestID=" + requestID + ", target=" + target + ", targetType="
				+ targetType + ", aaIVnfId=" + aaIVnfId + ", aaIServerName=" + aaIServerName + ", from=" + from
				+ ", dcaeTopic=" + dcaeTopic + ", appcTopic=" + appcTopic + ", appcResponseCode=" + appcResponseCode
				+ "]";
	}
	
}
