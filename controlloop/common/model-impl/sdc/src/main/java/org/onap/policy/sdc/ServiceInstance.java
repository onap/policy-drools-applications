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

public class ServiceInstance implements Serializable {
  private static final long serialVersionUID = 6285260780966679625L;

  private UUID personaModelUuid;
  private UUID serviceUuid;
  private UUID serviceInstanceUuid;
  private UUID widgetModelUuid;
  private String widgetModelVersion;
  private String serviceName;
  private String serviceInstanceName;

  public ServiceInstance() {
    // Empty Constructor
  }

  /**
   * Create a service instance, copy constructor.
   * @param instance The instance to copy from
   */
  public ServiceInstance(ServiceInstance instance) {
    if (instance == null) {
      return;
    }
    this.personaModelUuid = instance.personaModelUuid;
    this.serviceUuid = instance.serviceUuid;
    this.serviceInstanceUuid = instance.serviceInstanceUuid;
    this.widgetModelUuid = instance.widgetModelUuid;
    this.widgetModelVersion = instance.widgetModelVersion;
    this.serviceName = instance.serviceName;
    this.serviceInstanceName = instance.serviceInstanceName;
  }

  public UUID getPersonaModelUuid() {
    return personaModelUuid;
  }

  public void setPersonaModelUuid(UUID personaModelUuid) {
    this.personaModelUuid = personaModelUuid;
  }

  public UUID getServiceUuid() {
    return serviceUuid;
  }

  public void setServiceUuid(UUID serviceUuid) {
    this.serviceUuid = serviceUuid;
  }

  public UUID getServiceInstanceUuid() {
    return serviceInstanceUuid;
  }

  public void setServiceInstanceUuid(UUID serviceInstanceUuid) {
    this.serviceInstanceUuid = serviceInstanceUuid;
  }

  public UUID getWidgetModelUuid() {
    return widgetModelUuid;
  }

  public void setWidgetModelUuid(UUID widgetModelUuid) {
    this.widgetModelUuid = widgetModelUuid;
  }

  public String getWidgetModelVersion() {
    return widgetModelVersion;
  }

  public void setWidgetModelVersion(String widgetModelVersion) {
    this.widgetModelVersion = widgetModelVersion;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getServiceInstanceName() {
    return serviceInstanceName;
  }

  public void setServiceInstanceName(String serviceInstanceName) {
    this.serviceInstanceName = serviceInstanceName;
  }

  @Override
  public String toString() {
    return "ServiceInstance [personaModelUUID=" + personaModelUuid + ", serviceUUID=" + serviceUuid
        + ", serviceInstanceUUID=" + serviceInstanceUuid + ", widgetModelUUID=" + widgetModelUuid
        + ", widgetModelVersion=" + widgetModelVersion + ", serviceName=" + serviceName
        + ", serviceInstanceName=" + serviceInstanceName + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((personaModelUuid == null) ? 0 : personaModelUuid.hashCode());
    result = prime * result + ((serviceInstanceName == null) ? 0 : serviceInstanceName.hashCode());
    result = prime * result + ((serviceInstanceUuid == null) ? 0 : serviceInstanceUuid.hashCode());
    result = prime * result + ((serviceName == null) ? 0 : serviceName.hashCode());
    result = prime * result + ((serviceUuid == null) ? 0 : serviceUuid.hashCode());
    result = prime * result + ((widgetModelUuid == null) ? 0 : widgetModelUuid.hashCode());
    result = prime * result + ((widgetModelVersion == null) ? 0 : widgetModelVersion.hashCode());
    return result;
  }

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
    ServiceInstance other = (ServiceInstance) obj;
    if (personaModelUuid == null) {
      if (other.personaModelUuid != null) {
        return false;
      }
    } else if (!personaModelUuid.equals(other.personaModelUuid)) {
      return false;
    }
    if (serviceInstanceName == null) {
      if (other.serviceInstanceName != null) {
        return false;
      }
    } else if (!serviceInstanceName.equals(other.serviceInstanceName)) {
      return false;
    }
    if (serviceInstanceUuid == null) {
      if (other.serviceInstanceUuid != null) {
        return false;
      }
    } else if (!serviceInstanceUuid.equals(other.serviceInstanceUuid)) {
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
    if (widgetModelUuid == null) {
      if (other.widgetModelUuid != null) {
        return false;
      }
    } else if (!widgetModelUuid.equals(other.widgetModelUuid)) {
      return false;
    }
    if (widgetModelVersion == null) {
      if (other.widgetModelVersion != null) {
        return false;
      }
    } else if (!widgetModelVersion.equals(other.widgetModelVersion)) {
      return false;
    }
    return true;
  }

}
