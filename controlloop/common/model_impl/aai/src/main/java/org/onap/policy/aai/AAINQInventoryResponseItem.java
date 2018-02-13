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

	private static final long serialVersionUID = 7142072567154675183L;
	
	@SerializedName("model-name")
	private String modelName;
	
	@SerializedName("vf-module")
	private AAINQVfModule vfModule;
	
	@SerializedName("service-instance")
	private AAINQServiceInstance serviceInstance;
	
	@SerializedName("vserver")
	private AAINQVServer vserver;
	
	@SerializedName("tenant")
	private AAINQTenant tenant;
	
	@SerializedName("cloud-region")
	private AAINQCloudRegion cloudRegion;
	
	@SerializedName("generic-vnf")
	private AAINQGenericVNF genericVNF;
	
	@SerializedName("extra-properties")
	private AAINQExtraProperties extraProperties;
	
	@SerializedName("inventory-response-items")
	private AAINQInventoryResponseItems items;

	public String getModelName() {
		return modelName;
	}

	public AAINQVfModule getVfModule() {
		return vfModule;
	}

	public AAINQServiceInstance getServiceInstance() {
		return serviceInstance;
	}

	public AAINQVServer getVserver() {
		return vserver;
	}

	public AAINQTenant getTenant() {
		return tenant;
	}

	public AAINQCloudRegion getCloudRegion() {
		return cloudRegion;
	}

	public AAINQGenericVNF getGenericVNF() {
		return genericVNF;
	}

	public AAINQExtraProperties getExtraProperties() {
		return extraProperties;
	}

	public AAINQInventoryResponseItems getItems() {
		return items;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public void setVfModule(AAINQVfModule vfModule) {
		this.vfModule = vfModule;
	}

	public void setServiceInstance(AAINQServiceInstance serviceInstance) {
		this.serviceInstance = serviceInstance;
	}

	public void setVserver(AAINQVServer vserver) {
		this.vserver = vserver;
	}

	public void setTenant(AAINQTenant tenant) {
		this.tenant = tenant;
	}

	public void setCloudRegion(AAINQCloudRegion cloudRegion) {
		this.cloudRegion = cloudRegion;
	}

	public void setGenericVNF(AAINQGenericVNF genericVNF) {
		this.genericVNF = genericVNF;
	}

	public void setExtraProperties(AAINQExtraProperties extraProperties) {
		this.extraProperties = extraProperties;
	}

	public void setItems(AAINQInventoryResponseItems items) {
		this.items = items;
	}
}
