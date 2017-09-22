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

	/**
	 * 
	 */
	private static final long serialVersionUID = -6247505944905898870L;
	
	@SerializedName("vnf-id")
	public String vnfID;
	
	@SerializedName("vnf-name")
	public String vnfName;
	
	@SerializedName("vnf-type")
	public String vnfType;
	
	@SerializedName("service-id")
	public String serviceId;
	
	@SerializedName("orchestration-status")
	public String orchestrationStatus;
	
	public AAIGETVnfResponse() {
	}

}