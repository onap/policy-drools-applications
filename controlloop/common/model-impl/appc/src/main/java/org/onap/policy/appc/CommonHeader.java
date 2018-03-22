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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import com.google.gson.annotations.SerializedName;

public class CommonHeader implements Serializable {
	private static final long serialVersionUID = -3581658269910980336L;
	
	@SerializedName("TimeStamp")
	private Instant	timeStamp = Instant.now();
	
	@SerializedName("APIver")
	private String	apiVer = "1.01";
	
	@SerializedName("OriginatorID")
	private String	originatorID;
	
	@SerializedName("RequestID")
	private UUID	    requestID;
	
	@SerializedName("SubRequestID")
	private String	subRequestID;
	
	@SerializedName("RequestTrack")
	private Collection<String>	requestTrack = new ArrayList<>();
	
	@SerializedName("Flags")
	private Collection<Map<String, String>> flags = new ArrayList<>();
	
	public CommonHeader() {
	}
	
	public CommonHeader(CommonHeader commonHeader) {
		this.originatorID = commonHeader.originatorID;
		this.requestID = commonHeader.requestID;
		this.subRequestID = commonHeader.subRequestID;
		if (commonHeader.requestTrack != null) {
			this.requestTrack.addAll(commonHeader.requestTrack);
		}
		if (commonHeader.flags != null) {
			this.flags.addAll(commonHeader.flags);
		}
	}

	public Instant getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Instant timeStamp) {
		this.timeStamp = timeStamp;
	}

	public String getApiVer() {
		return apiVer;
	}

	public void setApiVer(String apiVer) {
		this.apiVer = apiVer;
	}

	public String getOriginatorID() {
		return originatorID;
	}

	public void setOriginatorID(String originatorID) {
		this.originatorID = originatorID;
	}

	public UUID getRequestID() {
		return requestID;
	}

	public void setRequestID(UUID requestID) {
		this.requestID = requestID;
	}

	public String getSubRequestID() {
		return subRequestID;
	}

	public void setSubRequestID(String subRequestID) {
		this.subRequestID = subRequestID;
	}

	public Collection<String> getRequestTrack() {
		return requestTrack;
	}

	public void setRequestTrack(Collection<String> requestTrack) {
		this.requestTrack = requestTrack;
	}

	public Collection<Map<String, String>> getFlags() {
		return flags;
	}

	public void setFlags(Collection<Map<String, String>> flags) {
		this.flags = flags;
	}

	@Override
	public String toString() {
		return "CommonHeader [TimeStamp=" + timeStamp + ", APIver=" + apiVer + ", OriginatorID=" + originatorID
				+ ", RequestID=" + requestID + ", SubrequestID=" + subRequestID + ", RequestTrack=" + requestTrack
				+ ", Flags=" + flags + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((apiVer == null) ? 0 : apiVer.hashCode());
		result = prime * result + ((flags == null) ? 0 : flags.hashCode());
		result = prime * result + ((originatorID == null) ? 0 : originatorID.hashCode());
		result = prime * result + ((requestID == null) ? 0 : requestID.hashCode());
		result = prime * result + ((requestTrack == null) ? 0 : requestTrack.hashCode());
		result = prime * result + ((subRequestID == null) ? 0 : subRequestID.hashCode());
		result = prime * result + ((timeStamp == null) ? 0 : timeStamp.hashCode());
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
		CommonHeader other = (CommonHeader) obj;
		if (apiVer == null) {
			if (other.apiVer != null)
				return false;
		} else if (!apiVer.equals(other.apiVer))
			return false;
		if (flags == null) {
			if (other.flags != null)
				return false;
		} else if (!flags.equals(other.flags))
			return false;
		if (originatorID == null) {
			if (other.originatorID != null)
				return false;
		} else if (!originatorID.equals(other.originatorID))
			return false;
		if (requestID == null) {
			if (other.requestID != null)
				return false;
		} else if (!requestID.equals(other.requestID))
			return false;
		if (requestTrack == null) {
			if (other.requestTrack != null)
				return false;
		} else if (!requestTrack.equals(other.requestTrack))
			return false;
		if (subRequestID == null) {
			if (other.subRequestID != null)
				return false;
		} else if (!subRequestID.equals(other.subRequestID))
			return false;
		if (timeStamp == null) {
			if (other.timeStamp != null)
				return false;
		} else if (!timeStamp.equals(other.timeStamp))
			return false;
		return true;
	}
	
}
