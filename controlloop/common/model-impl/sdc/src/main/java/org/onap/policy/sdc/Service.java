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

// TODO: Auto-generated Javadoc
public class Service implements Serializable {

  private static final long serialVersionUID = -1249276698549996806L;

  private UUID serviceUuid;
  private UUID serviceInvariantUuid;
  private String serviceName;
  private String serviceVersion;

  /**
   * Instantiates a new service.
   */
  public Service() {
    // Empty Constructor
  }

  /**
   * Instantiates a new service.
   *
   * @param uuid the uuid
   */
  public Service(UUID uuid) {
    this.serviceUuid = uuid;
  }

  /**
   * Instantiates a new service.
   *
   * @param name the name
   */
  public Service(String name) {
    this.serviceName = name;
  }

  /**
   * Instantiates a new service.
   *
   * @param uuid the UUID of the service
   * @param invariantUuid the invariant UID of the service
   * @param name the name of the service
   * @param version the version of the service
   */
  public Service(UUID uuid, UUID invariantUuid, String name, String version) {
    this.serviceUuid = uuid;
    this.serviceInvariantUuid = invariantUuid;
    this.serviceName = name;
    this.serviceVersion = version;
  }

  /**
   * Instantiates a new service, copy constructor.
   *
   * @param service the service to copy from for this service
   */
  public Service(Service service) {
    this.serviceUuid = service.serviceUuid;
    this.serviceInvariantUuid = service.serviceInvariantUuid;
    this.serviceName = service.serviceName;
    this.serviceVersion = service.serviceVersion;
  }

  /**
   * Gets the service uuid.
   *
   * @return the service uuid
   */
  public UUID getServiceUuid() {
    return serviceUuid;
  }

  /**
   * Sets the service uuid.
   *
   * @param serviceUuid the new service uuid
   */
  public void setServiceUuid(UUID serviceUuid) {
    this.serviceUuid = serviceUuid;
  }

  /**
   * Gets the service invariant UUID.
   *
   * @return the service invariant UUID
   */
  public UUID getServiceInvariantUuid() {
    return serviceInvariantUuid;
  }

  /**
   * Sets the service invariant uuid.
   *
   * @param serviceInvariantUuid the new service invariant uuid
   */
  public void setServiceInvariantUuid(UUID serviceInvariantUuid) {
    this.serviceInvariantUuid = serviceInvariantUuid;
  }

  /**
   * Gets the service name.
   *
   * @return the service name
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * Sets the service name.
   *
   * @param serviceName the new service name
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * Gets the service version.
   *
   * @return the service version
   */
  public String getServiceVersion() {
    return serviceVersion;
  }

  /**
   * Sets the service version.
   *
   * @param serviceVersion the new service version
   */
  public void setServiceVersion(String serviceVersion) {
    this.serviceVersion = serviceVersion;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Service [serviceUUID=" + serviceUuid + ", serviceInvariantUUID=" + serviceInvariantUuid
        + ", serviceName=" + serviceName + ", serviceVersion=" + serviceVersion + "]";
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + ((serviceInvariantUuid == null) ? 0 : serviceInvariantUuid.hashCode());
    result = prime * result + ((serviceName == null) ? 0 : serviceName.hashCode());
    result = prime * result + ((serviceUuid == null) ? 0 : serviceUuid.hashCode());
    result = prime * result + ((serviceVersion == null) ? 0 : serviceVersion.hashCode());
    return result;
  }

  /* (non-Javadoc)
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
    Service other = (Service) obj;
    if (serviceInvariantUuid == null) {
      if (other.serviceInvariantUuid != null) {
        return false;
      }
    } else if (!serviceInvariantUuid.equals(other.serviceInvariantUuid)) {
      return false;
    }
    if (serviceName == null) {
      if (other.serviceName != null) {
        return false;
      }
    } else if (!serviceName.equals(other.serviceName)) {
      return false;
    }
    if (serviceUuid == null) {
      if (other.serviceUuid != null) {
        return false;
      }
    } else if (!serviceUuid.equals(other.serviceUuid)) {
      return false;
    }
    if (serviceVersion == null) {
      if (other.serviceVersion != null) {
        return false;
      }
    } else if (!serviceVersion.equals(other.serviceVersion)) {
      return false;
    }
    return true;
  }

}
