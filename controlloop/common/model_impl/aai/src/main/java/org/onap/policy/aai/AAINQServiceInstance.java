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

public class AAINQServiceInstance implements Serializable {
	private static final long serialVersionUID = -8000944396593889586L;

	@SerializedName("service-instance-id")
	private String serviceInstanceID;
	
	@SerializedName("service-instance-name")
	private String serviceInstanceName;
	
	@SerializedName("persona-model-id")
	private String personaModelId;
	
	@SerializedName("persona-model-version")
	private String personaModelVersion;
	
	@SerializedName("service-instance-location-id")
	private String serviceInstanceLocationId;
	
	@SerializedName("resource-version")
	private String resourceVersion;
	
	@SerializedName("model-invariant-id")
	private String modelInvariantId;
	
	@SerializedName("model-version-id")
	private String modelVersionId;

	public String getServiceInstanceID() {
		return serviceInstanceID;
	}

	public String getServiceInstanceName() {
		return serviceInstanceName;
	}

	public String getPersonaModelId() {
		return personaModelId;
	}

	public String getPersonaModelVersion() {
		return personaModelVersion;
	}

	public String getServiceInstanceLocationId() {
		return serviceInstanceLocationId;
	}

	public String getResourceVersion() {
		return resourceVersion;
	}

	public String getModelInvariantId() {
		return modelInvariantId;
	}

	public String getModelVersionId() {
		return modelVersionId;
	}

	public void setServiceInstanceID(String serviceInstanceID) {
		this.serviceInstanceID = serviceInstanceID;
	}

	public void setServiceInstanceName(String serviceInstanceName) {
		this.serviceInstanceName = serviceInstanceName;
	}

	public void setPersonaModelId(String personaModelId) {
		this.personaModelId = personaModelId;
	}

	public void setPersonaModelVersion(String personaModelVersion) {
		this.personaModelVersion = personaModelVersion;
	}

	public void setServiceInstanceLocationId(String serviceInstanceLocationId) {
		this.serviceInstanceLocationId = serviceInstanceLocationId;
	}

	public void setResourceVersion(String resourceVersion) {
		this.resourceVersion = resourceVersion;
	}

	public void setModelInvariantId(String modelInvariantId) {
		this.modelInvariantId = modelInvariantId;
	}

	public void setModelVersionId(String modelVersionId) {
		this.modelVersionId = modelVersionId;
	}
}
