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

public class CommonHeader implements Serializable {

	private static final long serialVersionUID = -3581658269910980336L;
	
	public Instant	TimeStamp = Instant.now();
	public String	APIver = "1.01";
	public String	OriginatorID;
	public UUID	RequestID;
	public String	SubRequestID;
	public Collection<String>	RequestTrack = new ArrayList<>();
	public Collection<Map<String, String>> Flags = new ArrayList<>();
	
	public CommonHeader() {
		
	}
	
	public CommonHeader(CommonHeader commonHeader) {
		this.OriginatorID = commonHeader.OriginatorID;
		this.RequestID = commonHeader.RequestID;
		this.SubRequestID = commonHeader.SubRequestID;
		if (commonHeader.RequestTrack != null) {
			this.RequestTrack.addAll(commonHeader.RequestTrack);
		}
		if (commonHeader.Flags != null) {
			this.Flags.addAll(commonHeader.Flags);
		}
	}

	@Override
	public String toString() {
		return "CommonHeader [TimeStamp=" + TimeStamp + ", APIver=" + APIver + ", OriginatorID=" + OriginatorID
				+ ", RequestID=" + RequestID + ", SubrequestID=" + SubRequestID + ", RequestTrack=" + RequestTrack
				+ ", Flags=" + Flags + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((APIver == null) ? 0 : APIver.hashCode());
		result = prime * result + ((Flags == null) ? 0 : Flags.hashCode());
		result = prime * result + ((OriginatorID == null) ? 0 : OriginatorID.hashCode());
		result = prime * result + ((RequestID == null) ? 0 : RequestID.hashCode());
		result = prime * result + ((RequestTrack == null) ? 0 : RequestTrack.hashCode());
		result = prime * result + ((SubRequestID == null) ? 0 : SubRequestID.hashCode());
		result = prime * result + ((TimeStamp == null) ? 0 : TimeStamp.hashCode());
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
		if (APIver == null) {
			if (other.APIver != null)
				return false;
		} else if (!APIver.equals(other.APIver))
			return false;
		if (Flags == null) {
			if (other.Flags != null)
				return false;
		} else if (!Flags.equals(other.Flags))
			return false;
		if (OriginatorID == null) {
			if (other.OriginatorID != null)
				return false;
		} else if (!OriginatorID.equals(other.OriginatorID))
			return false;
		if (RequestID == null) {
			if (other.RequestID != null)
				return false;
		} else if (!RequestID.equals(other.RequestID))
			return false;
		if (RequestTrack == null) {
			if (other.RequestTrack != null)
				return false;
		} else if (!RequestTrack.equals(other.RequestTrack))
			return false;
		if (SubRequestID == null) {
			if (other.SubRequestID != null)
				return false;
		} else if (!SubRequestID.equals(other.SubRequestID))
			return false;
		if (TimeStamp == null) {
			if (other.TimeStamp != null)
				return false;
		} else if (!TimeStamp.equals(other.TimeStamp))
			return false;
		return true;
	}
	
}
