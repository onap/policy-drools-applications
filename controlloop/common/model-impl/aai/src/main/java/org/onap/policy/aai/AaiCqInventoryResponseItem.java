/*-
 * ============LICENSE_START=======================================================
 *
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import org.onap.aai.domain.yang.v15.CloudRegion;
import org.onap.aai.domain.yang.v15.GenericVnf;
import org.onap.aai.domain.yang.v15.ServiceInstance;
import org.onap.aai.domain.yang.v15.Tenant;
import org.onap.aai.domain.yang.v15.VfModule;
import org.onap.aai.domain.yang.v15.Vserver;

/*
 * This class represents a collection of various types of A&AI inventory items
 * which can possibly be returned from a custom query
 */
public class AaiCqInventoryResponseItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @SerializedName("vf-module")
    private VfModule vfModule;

    @SerializedName("service-instance")
    private ServiceInstance serviceInstance;

    @SerializedName("vserver")
    private Vserver vserver;

    @SerializedName("tenant")
    private Tenant tenant;

    @SerializedName("cloud-region")
    private CloudRegion cloudRegion;

    @SerializedName("generic-vnf")
    private GenericVnf genericVnf;

    public VfModule getVfModule() {
        return vfModule;
    }

    public ServiceInstance getServiceInstance() {
        return serviceInstance;
    }

    public Vserver getVserver() {
        return vserver;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public CloudRegion getCloudRegion() {
        return cloudRegion;
    }

    public GenericVnf getGenericVnf() {
        return genericVnf;
    }

    public void setVfModule(VfModule vfModule) {
        this.vfModule = vfModule;
    }

    public void setServiceInstance(ServiceInstance serviceInstance) {
        this.serviceInstance = serviceInstance;
    }

    public void setVserver(Vserver vserver) {
        this.vserver = vserver;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public void setCloudRegion(CloudRegion cloudRegion) {
        this.cloudRegion = cloudRegion;
    }

    public void setGenericVnf(GenericVnf genericVnf) {
        this.genericVnf = genericVnf;
    }

}
