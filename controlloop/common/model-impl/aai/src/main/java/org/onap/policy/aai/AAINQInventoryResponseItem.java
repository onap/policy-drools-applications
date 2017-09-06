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

public class AAINQInventoryResponseItem implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7142072567154675183L;
	
	@SerializedName("model-name")
	public String modelName;
	
	@SerializedName("vf-module")
	public AAINQVfModule vfModule;
	
	@SerializedName("service-instance")
	public AAINQServiceInstance serviceInstance;
	
	@SerializedName("vserver")
	public AAINQVServer vserver;
	
	@SerializedName("tenant")
	public AAINQTenant tenant;
	
	@SerializedName("cloud-region")
	public AAINQCloudRegion cloudRegion;
	
	@SerializedName("generic-vnf")
	public AAINQGenericVNF genericVNF;
	
	@SerializedName("extra-properties")
	public AAINQExtraProperties extraProperties;
	
	@SerializedName("inventory-response-items")
	public AAINQInventoryResponseItems items;

	public AAINQInventoryResponseItem() {
	}

}
