/*-
 * ============LICENSE_START=======================================================
 * mso
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

package org.onap.policy.so;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class SORequestDetails implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3283942659786236032L;
	
	@SerializedName("modelInfo")
	public SOModelInfo modelInfo;
	
	@SerializedName("cloudConfiguration")
	public SOCloudConfiguration cloudConfiguration;
	
	@SerializedName("requestInfo")
	public SORequestInfo requestInfo;
	
	@SerializedName("subscriberInfo")
	public SOSubscriberInfo subscriberInfo;
	
	@SerializedName("relatedInstanceList")
	public List<SORelatedInstanceListElement> relatedInstanceList= new LinkedList<SORelatedInstanceListElement>();	
	
	@SerializedName("requestParameters")
	public SORequestParameters requestParameters;

	public SORequestDetails() {
		
	}

	public SORequestDetails(SORequestDetails soRequestDetails) {
		this.modelInfo = soRequestDetails.modelInfo;
		this.cloudConfiguration = soRequestDetails.cloudConfiguration;
		this.requestInfo = soRequestDetails.requestInfo;
		this.relatedInstanceList = soRequestDetails.relatedInstanceList;
		this.requestParameters = soRequestDetails.requestParameters;
	}

	@Override
	public String toString() {
		return "SORequestDetails [modelInfo=" + modelInfo
				+ ", cloudConfiguration=" + cloudConfiguration
				+ ", requestInfo=" + requestInfo + ", relatedInstanceList="
				+ relatedInstanceList + ", requestParameters="
				+ requestParameters + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cloudConfiguration == null) ? 0 : cloudConfiguration.hashCode());
		result = prime * result + ((modelInfo == null) ? 0 : modelInfo.hashCode());
		result = prime * result + ((relatedInstanceList == null) ? 0 : relatedInstanceList.hashCode());
		result = prime * result + ((requestInfo == null) ? 0 : requestInfo.hashCode());
		result = prime * result + ((requestParameters == null) ? 0 : requestParameters.hashCode());
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
		SORequestDetails other = (SORequestDetails) obj;
		if (cloudConfiguration == null) {
			if (other.cloudConfiguration != null)
				return false;
		} else if (!cloudConfiguration.equals(other.cloudConfiguration))
			return false;
		if (modelInfo == null) {
			if (other.modelInfo != null)
				return false;
		} else if (!modelInfo.equals(other.modelInfo))
			return false;
		if (relatedInstanceList == null) {
			if (other.relatedInstanceList != null)
				return false;
		} else if (!relatedInstanceList.equals(other.relatedInstanceList))
			return false;
		if (requestInfo == null) {
			if (other.requestInfo != null)
				return false;
		} else if (!requestInfo.equals(other.requestInfo))
			return false;
		if (requestParameters == null) {
			if (other.requestParameters != null)
				return false;
		} else if (!requestParameters.equals(other.requestParameters))
			return false;
		return true;
	}
	
}
