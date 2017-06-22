/*-
 * ============LICENSE_START=======================================================
 * sdc
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

package org.onap.policy.sdc;

import java.io.Serializable;
import java.util.UUID;

public class ResourceInstance implements Serializable {
	private static final long serialVersionUID = -5506162340393802424L;

	public String resourceInstanceName;
	public String resourceName;
	public UUID	resourceInvariantUUID;
	public String resourceVersion;
	public ResourceType resourceType;
	public UUID	resourceUUID;
	
	public ResourceInstance() {
		
	}
	
	public ResourceInstance(ResourceInstance instance) {
		if (instance == null) { 
			return;
		}
		this.resourceInstanceName = instance.resourceInstanceName;
		this.resourceName = instance.resourceName;
		this.resourceInvariantUUID = instance.resourceInvariantUUID;
		this.resourceVersion = instance.resourceVersion;
		this.resourceType = instance.resourceType;
		this.resourceUUID = instance.resourceUUID;
	}
	
	@Override
	public String toString() {
		return "ResourceInstance [resourceInstanceName=" + resourceInstanceName + ", resourceName=" + resourceName
				+ ", resourceInvariantUUID=" + resourceInvariantUUID + ", resourceVersion=" + resourceVersion
				+ ", resourceType=" + resourceType + ", resourceUUID=" + resourceUUID + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((resourceInstanceName == null) ? 0 : resourceInstanceName.hashCode());
		result = prime * result + ((resourceInvariantUUID == null) ? 0 : resourceInvariantUUID.hashCode());
		result = prime * result + ((resourceName == null) ? 0 : resourceName.hashCode());
		result = prime * result + ((resourceType == null) ? 0 : resourceType.hashCode());
		result = prime * result + ((resourceUUID == null) ? 0 : resourceUUID.hashCode());
		result = prime * result + ((resourceVersion == null) ? 0 : resourceVersion.hashCode());
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
		ResourceInstance other = (ResourceInstance) obj;
		if (resourceInstanceName == null) {
			if (other.resourceInstanceName != null)
				return false;
		} else if (!resourceInstanceName.equals(other.resourceInstanceName))
			return false;
		if (resourceInvariantUUID == null) {
			if (other.resourceInvariantUUID != null)
				return false;
		} else if (!resourceInvariantUUID.equals(other.resourceInvariantUUID))
			return false;
		if (resourceName == null) {
			if (other.resourceName != null)
				return false;
		} else if (!resourceName.equals(other.resourceName))
			return false;
		if (resourceType != other.resourceType)
			return false;
		if (resourceUUID == null) {
			if (other.resourceUUID != null)
				return false;
		} else if (!resourceUUID.equals(other.resourceUUID))
			return false;
		if (resourceVersion == null) {
			if (other.resourceVersion != null)
				return false;
		} else if (!resourceVersion.equals(other.resourceVersion))
			return false;
		return true;
	}
	
}
