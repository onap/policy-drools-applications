/*-
 * ============LICENSE_START=======================================================
 * appc
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

package org.onap.policy.appc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class Request implements Serializable{
	private static final long serialVersionUID = -3912323643990646431L;

	@SerializedName("CommonHeader")
	private CommonHeader		        commonHeader;
	
	@SerializedName("Action")
	private String			     	action;
	
	@SerializedName("TargetID")
	private String				    targetID;
	
	@SerializedName("ObjectID")
	private String				    objectID;
	
	@SerializedName("Payload")
	private HashMap<String, Object>	payload = new HashMap<>();
	
	public Request() {
		// Initiate an empty Request instance
	}
	
	public CommonHeader getCommonHeader() {
		return commonHeader;
	}
	
	public Map<String, Object> getPayload() {
		return payload;
	}
	
	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getTargetID() {
		return targetID;
	}

	public void setTargetID(String targetID) {
		this.targetID = targetID;
	}

	public String getObjectID() {
		return objectID;
	}

	public void setObjectID(String objectID) {
		this.objectID = objectID;
	}

	public void setCommonHeader(CommonHeader commonHeader) {
		this.commonHeader = commonHeader;
	}

	public void setPayload(Map<String, Object> payload) {
		this.payload = new HashMap<>(payload);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action == null) ? 0 : action.hashCode());
		result = prime * result + ((commonHeader == null) ? 0 : commonHeader.hashCode());
		result = prime * result + ((objectID == null) ? 0 : objectID.hashCode());
		result = prime * result + ((payload == null) ? 0 : payload.hashCode());
		result = prime * result + ((targetID == null) ? 0 : targetID.hashCode());
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
		
		Request other = (Request) obj;
		if (action == null) {
			if (other.action != null)
				return false;
		} else if (!action.equals(other.action))
			return false;
		
		if (commonHeader == null) {
			if (other.commonHeader != null)
				return false;
		} else if (!commonHeader.equals(other.commonHeader))
			return false;
		
		if (objectID == null) {
			if (other.objectID != null)
				return false;
		} else if (!objectID.equals(other.objectID))
			return false;
		
		if (payload == null) {
			if (other.payload != null)
				return false;
		} else if (!payload.equals(other.payload))
			return false;
		
		if (targetID == null) {
			if (other.targetID != null)
				return false;
		} else if (!targetID.equals(other.targetID))
			return false;
		
		return true;
	}

	@Override
	public String toString() {
		return "Request [CommonHeader=" + commonHeader + ", Action=" + action + ", TargetID=" + targetID + ", ObjectID="
				+ objectID + ", Payload=" + payload + "]";
	}

}
