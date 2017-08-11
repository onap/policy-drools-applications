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

public class Request implements Serializable{

	private static final long serialVersionUID = -3912323643990646431L;

	public CommonHeader		CommonHeader;
	public String				Action;
	public String				TargetID;
	public String				ObjectID;
	public Map<String, Object>	Payload = new HashMap<>();
	
	public Request() {
		
	}
	
	public CommonHeader getCommonHeader() {
		return CommonHeader;
	}
	
	public Map<String, Object> getPayload() {
		return Payload;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((Action == null) ? 0 : Action.hashCode());
		result = prime * result + ((CommonHeader == null) ? 0 : CommonHeader.hashCode());
		result = prime * result + ((ObjectID == null) ? 0 : ObjectID.hashCode());
		result = prime * result + ((Payload == null) ? 0 : Payload.hashCode());
		result = prime * result + ((TargetID == null) ? 0 : TargetID.hashCode());
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
		if (Action == null) {
			if (other.Action != null)
				return false;
		} else if (!Action.equals(other.Action))
			return false;
		if (CommonHeader == null) {
			if (other.CommonHeader != null)
				return false;
		} else if (!CommonHeader.equals(other.CommonHeader))
			return false;
		if (ObjectID == null) {
			if (other.ObjectID != null)
				return false;
		} else if (!ObjectID.equals(other.ObjectID))
			return false;
		if (Payload == null) {
			if (other.Payload != null)
				return false;
		} else if (!Payload.equals(other.Payload))
			return false;
		if (TargetID == null) {
			if (other.TargetID != null)
				return false;
		} else if (!TargetID.equals(other.TargetID))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Request [CommonHeader=" + CommonHeader + ", Action=" + Action + ", TargetID=" + TargetID + ", ObjectID="
				+ ObjectID + ", Payload=" + Payload + "]";
	}

}
