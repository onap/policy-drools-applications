/*-
 * ============LICENSE_START=======================================================
 * aai
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

package org.onap.policy.aai;

import java.io.Serializable;

import com.google.gson.annotations.SerializedName;

public class AAIGETVnfResponse extends AAIGETResponse implements Serializable {

	private static final long serialVersionUID = -6247505944905898870L;
	
	@SerializedName("vnf-id")
	private String vnfID;
	
	@SerializedName("vnf-name")
	private String vnfName;
	
	@SerializedName("vnf-type")
	private String vnfType;
	
	@SerializedName("service-id")
	private String serviceId;
	
	@SerializedName("orchestration-status")
	private String orchestrationStatus;

	public String getVnfID() {
		return vnfID;
	}

	public String getVnfName() {
		return vnfName;
	}

	public String getVnfType() {
		return vnfType;
	}

	public String getServiceId() {
		return serviceId;
	}

	public String getOrchestrationStatus() {
		return orchestrationStatus;
	}

	public void setVnfID(String vnfID) {
		this.vnfID = vnfID;
	}

	public void setVnfName(String vnfName) {
		this.vnfName = vnfName;
	}

	public void setVnfType(String vnfType) {
		this.vnfType = vnfType;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	public void setOrchestrationStatus(String orchestrationStatus) {
		this.orchestrationStatus = orchestrationStatus;
	}
}