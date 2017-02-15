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
import java.util.UUID;

import org.openecomp.policy.appc.CommonHeader;
import org.openecomp.policy.appc.Response;
import org.openecomp.policy.appc.ResponseStatus;

public class AppcResponseEvent implements Serializable {

	private static final long serialVersionUID = 6661836261200950007L;
	
	public final String requestID;
	public final String appcTopic;
	public final int code;
	
	public AppcResponseEvent(String requestID, String appcTopic, int code) {
		this.requestID = requestID;
		this.appcTopic = appcTopic;
		this.code = code;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((appcTopic == null) ? 0 : appcTopic.hashCode());
		result = prime * result + code;
		result = prime * result + ((requestID == null) ? 0 : requestID.hashCode());
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
		AppcResponseEvent other = (AppcResponseEvent) obj;
		if (appcTopic == null) {
			if (other.appcTopic != null)
				return false;
		} else if (!appcTopic.equals(other.appcTopic))
			return false;
		if (code != other.code)
			return false;
		if (requestID == null) {
			if (other.requestID != null)
				return false;
		} else if (!requestID.equals(other.requestID))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AppcResponseEvent [requestID=" + requestID + ", appcTopic=" + appcTopic + ", code=" + code + "]";
	}

	public static Response toResponse(String requestId, int code) {
		Response response = new Response();
		
		CommonHeader commonHeader = new CommonHeader();
		commonHeader.RequestID = UUID.fromString(requestId);
		response.CommonHeader = commonHeader;

		ResponseStatus responseStatus = new ResponseStatus();
		responseStatus.Code = responseStatus.Code = code;
		response.Status = responseStatus;
		
		return response;
	}

}
