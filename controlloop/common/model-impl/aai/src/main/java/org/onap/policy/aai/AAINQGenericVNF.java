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

public class AAINQGenericVNF implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 834322706248060559L;
	
	@SerializedName("vnf-id")
	public String vnfID;
	
	@SerializedName("vnf-name")
	public String vnfName;
	
	@SerializedName("vnf-name2")
	public String vnfName2;
	
	@SerializedName("vnf-type")
	public String vnfType;
	
	@SerializedName("service-id")
	public String serviceId;
	
	@SerializedName("prov-status")
	public String provStatus;
	
	@SerializedName("operational-state")
	public String operationalState;
	
	@SerializedName("ipv4-oam-address")
	public String ipv4OamAddress;
	
	@SerializedName("ipv4-loopback0-address")
	public String ipv4Loopback0Address;
	
	@SerializedName("in-maint")
	public Boolean inMaint;
	
	@SerializedName("is-closed-loop-disabled")
	public Boolean isClosedLoopDisabled;
	
	@SerializedName("resource-version")
	public String resourceVersion;
	
	@SerializedName("encrypted-access-flag")
	public Boolean encrypedAccessFlag;
	
	@SerializedName("persona-model-id")
	public String personaModelId;
	
	@SerializedName("persona-model-version")
	public String personaModelVersion;
	
	@SerializedName("model-invariant-id")
	public String modelInvariantId;
	
	@SerializedName("model-version-id")
	public String modelVersionId;

	public AAINQGenericVNF() {
	}

}
