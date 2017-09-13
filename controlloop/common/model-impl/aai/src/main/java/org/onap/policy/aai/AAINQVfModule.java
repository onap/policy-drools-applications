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

public class AAINQVfModule implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8019133081429638231L;

	@SerializedName("vf-module-id")
	public String vfModuleId;
	
	@SerializedName("vf-module-name")
	public String vfModuleName;
	
	@SerializedName("heat-stack-id")
	public String heatStackId;
	
	@SerializedName("orchestration-status")
	public String orchestrationStatus;
	
	@SerializedName("is-base-vf-module")
	public Boolean isBaseVfModule;
	
	@SerializedName("resource-version")
	public String resourceVersion;
	
	@SerializedName("persona-model-id")
	public String personaModelId;
	
	@SerializedName("persona-model-version")
	public String personaModelVersion;
	
	@SerializedName("widget-model-id")
	public String widgetModelId;
	
	@SerializedName("widget-model-version")
	public String widgetModelVersion;
	
	@SerializedName("contrail-service-instance-fqdn")
	public String contrailServiceInstanceFqdn;
	
	@SerializedName("model-invariant-id")
	public String modelInvariantId;
	
	@SerializedName("model-version-id")
	public String modelVersionId;
	
	public AAINQVfModule() {
	}
}
