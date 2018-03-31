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

  private String resourceInstanceName;
  private String resourceName;
  private UUID resourceInvariantUuid;
  private String resourceVersion;
  private ResourceType resourceType;
  private UUID resourceUuid;

  /**
   * Instantiates a new resource instance.
   */
  public ResourceInstance() {
    // Empty Constructor
  }

  /**
   * Create a resource instance, copy constructor.
   * 
   * @param instance The instance to copy from for the new instance
   */
  public ResourceInstance(ResourceInstance instance) {
    if (instance == null) {
      return;
    }
    this.resourceInstanceName = instance.resourceInstanceName;
    this.resourceName = instance.resourceName;
    this.resourceInvariantUuid = instance.resourceInvariantUuid;
    this.resourceVersion = instance.resourceVersion;
    this.resourceType = instance.resourceType;
    this.resourceUuid = instance.resourceUuid;
  }

  /**
   * Gets the resource instance name.
   *
   * @return the resource instance name
   */
  public String getResourceInstanceName() {
    return resourceInstanceName;
  }

  /**
   * Sets the resource instance name.
   *
   * @param resourceInstanceName the new resource instance name
   */
  public void setResourceInstanceName(String resourceInstanceName) {
    this.resourceInstanceName = resourceInstanceName;
  }

  /**
   * Gets the resource name.
   *
   * @return the resource name
   */
  public String getResourceName() {
    return resourceName;
  }

  /**
   * Sets the resource name.
   *
   * @param resourceName the new resource name
   */
  public void setResourceName(String resourceName) {
    this.resourceName = resourceName;
  }

  /**
   * Gets the resource invariant UUID.
   *
   * @return the resource invariant UUID
   */
  public UUID getResourceInvariantUuid() {
    return resourceInvariantUuid;
  }

  /**
   * Sets the resource invariant UUID.
   *
   * @param resourceInvariantUuid the new resource invariant UUID
   */
  public void setResourceInvariantUuid(UUID resourceInvariantUuid) {
    this.resourceInvariantUuid = resourceInvariantUuid;
  }

  /**
   * Gets the resource version.
   *
   * @return the resource version
   */
  public String getResourceVersion() {
    return resourceVersion;
  }

  /**
   * Sets the resource version.
   *
   * @param resourceVersion the new resource version
   */
  public void setResourceVersion(String resourceVersion) {
    this.resourceVersion = resourceVersion;
  }

  /**
   * Gets the resource type.
   *
   * @return the resource type
   */
  public ResourceType getResourceType() {
    return resourceType;
  }

  /**
   * Sets the resource type.
   *
   * @param resourceType the new resource type
   */
  public void setResourceType(ResourceType resourceType) {
    this.resourceType = resourceType;
  }

  /**
   * Gets the resource uuid.
   *
   * @return the resource uuid
   */
  public UUID getResourceUuid() {
    return resourceUuid;
  }

  /**
   * Sets the resource UUID.
   *
   * @param resourceUuid the new resource UUID
   */
  public void setResourceUuid(UUID resourceUuid) {
    this.resourceUuid = resourceUuid;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "ResourceInstance [resourceInstanceName=" + resourceInstanceName + ", resourceName="
        + resourceName + ", resourceInvariantUUID=" + resourceInvariantUuid + ", resourceVersion="
        + resourceVersion + ", resourceType=" + resourceType + ", resourceUUID=" + resourceUuid
        + "]";
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + ((resourceInstanceName == null) ? 0 : resourceInstanceName.hashCode());
    result =
        prime * result + ((resourceInvariantUuid == null) ? 0 : resourceInvariantUuid.hashCode());
    result = prime * result + ((resourceName == null) ? 0 : resourceName.hashCode());
    result = prime * result + ((resourceType == null) ? 0 : resourceType.hashCode());
    result = prime * result + ((resourceUuid == null) ? 0 : resourceUuid.hashCode());
    result = prime * result + ((resourceVersion == null) ? 0 : resourceVersion.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ResourceInstance other = (ResourceInstance) obj;
    if (resourceInstanceName == null) {
      if (other.resourceInstanceName != null) {
        return false;
      }
    } else if (!resourceInstanceName.equals(other.resourceInstanceName)) {
      return false;
    }
    if (resourceInvariantUuid == null) {
      if (other.resourceInvariantUuid != null) {
        return false;
      }
    } else if (!resourceInvariantUuid.equals(other.resourceInvariantUuid)) {
      return false;
    }
    if (resourceName == null) {
      if (other.resourceName != null) {
        return false;
      }
    } else if (!resourceName.equals(other.resourceName)) {
      return false;
    }
    if (resourceType != other.resourceType) {
      return false;
    }
    if (resourceUuid == null) {
      if (other.resourceUuid != null) {
        return false;
      }
    } else if (!resourceUuid.equals(other.resourceUuid)) {
      return false;
    }
    if (resourceVersion == null) {
      if (other.resourceVersion != null) {
        return false;
      }
    } else if (!resourceVersion.equals(other.resourceVersion)) {
      return false;
    }
    return true;
  }

}
