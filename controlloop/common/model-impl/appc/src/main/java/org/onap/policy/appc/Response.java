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

public class Response implements Serializable {

	private static final long serialVersionUID = 434953706339865151L;

	public CommonHeader CommonHeader;
	public ResponseStatus Status = new ResponseStatus();
	public Map<String, Object> Payload = new HashMap<>();
	
	public Response() {
		
	}
	
	public Response(Request request) {
		this.CommonHeader = new CommonHeader(request.CommonHeader);
		if (request.Payload != null) {
			this.Payload.putAll(request.Payload);
		}
	}

	public CommonHeader getCommonHeader() {
		return CommonHeader;
	}

	public void setCommonHeader(CommonHeader commonHeader) {
		CommonHeader = commonHeader;
	}

	public ResponseStatus getStatus() {
		return Status;
	}

	public void setStatus(ResponseStatus status) {
		Status = status;
	}

	public Map<String, Object> getPayload() {
		return Payload;
	}

	public void setPayload(Map<String, Object> payload) {
		Payload = payload;
	}

	@Override
	public String toString() {
		return "Response [CommonHeader=" + CommonHeader + ", Status=" + Status + ", Payload=" + Payload + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((CommonHeader == null) ? 0 : CommonHeader.hashCode());
		result = prime * result + ((Payload == null) ? 0 : Payload.hashCode());
		result = prime * result + ((Status == null) ? 0 : Status.hashCode());
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
		Response other = (Response) obj;
		if (CommonHeader == null) {
			if (other.CommonHeader != null)
				return false;
		} else if (!CommonHeader.equals(other.CommonHeader))
			return false;
		if (Payload == null) {
			if (other.Payload != null)
				return false;
		} else if (!Payload.equals(other.Payload))
			return false;
		if (Status == null) {
			if (other.Status != null)
				return false;
		} else if (!Status.equals(other.Status))
			return false;
		return true;
	}
	
	
	
}
