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

package org.onap.policy.aai.AAINQF199;

import java.io.Serializable;

import com.google.gson.annotations.SerializedName;

public class AAINQF199InventoryResponseItem implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7142072567154675183L;
	
	@SerializedName("model-name")
	public String modelName;
	
	@SerializedName("vf-module")
	public AAINQF199VfModule vfModule;
	
	@SerializedName("service-instance")
	public AAINQF199ServiceInstance serviceInstance;
	
	@SerializedName("vserver")
	public AAINQF199VServer vserver;
	
	@SerializedName("tenant")
	public AAINQF199Tenant tenant;
	
	@SerializedName("cloud-region")
	public AAINQF199CloudRegion cloudRegion;
	
	@SerializedName("generic-vnf")
	public AAINQF199GenericVNF genericVNF;
	
	@SerializedName("extra-properties")
	public AAINQF199ExtraProperties extraProperties;
	
	@SerializedName("inventory-response-items")
	public AAINQF199InventoryResponseItems items;

	public AAINQF199InventoryResponseItem() {
	}

}
