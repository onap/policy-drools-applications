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

public class AAIGETVnfResponse implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6247505944905898870L;
 
	@SerializedName("vnf-id")
	public String vnfID;
	
	@SerializedName("vnf-name")
	public String vnfName;
	
	@SerializedName("vnf-type")
	public String vnfType;
	
	@SerializedName("service-id")
	public String serviceId;
	
	@SerializedName("orchestration-status")
	public String orchestrationStatus;
	
	@SerializedName("in-maint")
	public String inMaint;
	
	@SerializedName("is-closed-loop-disabled")
	public String isClosedLoopDisabled;
	
	@SerializedName("resource-version")
	public String resourceVersion; 
	
	@SerializedName("model-invariant-id")
	public String modelInvariantId;
	
	@SerializedName("relationship-list")
	public RelationshipList relationshipList;

	public AAIGETVnfResponse() {
	}

}
/* sample vnf data 
{
   "vnf-id": "5e49ca06-2972-4532-9ed4-6d071588d792",
   "vnf-name": "USUCP0PCOIL0110UJRT01",
   "vnf-type": "RT",
   "service-id": "d7bb0a21-66f2-4e6d-87d9-9ef3ced63ae4",
   "equipment-role": "UCPE",
   "orchestration-status": "created",
   "management-option": "ATT",
   "ipv4-oam-address": "32.40.68.35",
   "ipv4-loopback0-address": "32.40.64.57",
   "nm-lan-v6-address": "2001:1890:e00e:fffe::1345",
   "management-v6-address": "2001:1890:e00e:fffd::36",
   "in-maint": false,
   "is-closed-loop-disabled": false,
   "resource-version": "1493389458092",
   "relationship-list": {"relationship":    [
            {
         "related-to": "service-instance",
         "related-link": "/aai/v11/business/customers/customer/1610_Func_Global_20160817084727/service-subscriptions/service-subscription/uCPE-VMS/service-instances/service-instance/USUCP0PCOIL0110UJZZ01",
         "relationship-data":          [
                        {
               "relationship-key": "customer.global-customer-id",
               "relationship-value": "1610_Func_Global_20160817084727"
            },
                        {
               "relationship-key": "service-subscription.service-type",
               "relationship-value": "uCPE-VMS"
            },
                        {
               "relationship-key": "service-instance.service-instance-id",
               "relationship-value": "USUCP0PCOIL0110UJZZ01"
            }
         ],
         "related-to-property": [{"property-key": "service-instance.service-instance-name"}]
      },
            {
         "related-to": "vserver",
         "related-link": "/aai/v11/cloud-infrastructure/cloud-regions/cloud-region/att-aic/AAIAIC25/tenants/tenant/USUCP0PCOIL0110UJZZ01%3A%3AuCPE-VMS/vservers/vserver/3b2558f4-39d8-40e7-bfc7-30660fb52c45",
         "relationship-data":          [
                        {
               "relationship-key": "cloud-region.cloud-owner",
               "relationship-value": "att-aic"
            },
                        {
               "relationship-key": "cloud-region.cloud-region-id",
               "relationship-value": "AAIAIC25"
            },
                        {
               "relationship-key": "tenant.tenant-id",
               "relationship-value": "USUCP0PCOIL0110UJZZ01::uCPE-VMS"
            },
                        {
               "relationship-key": "vserver.vserver-id",
               "relationship-value": "3b2558f4-39d8-40e7-bfc7-30660fb52c45"
            }
         ],
         "related-to-property": [         {
            "property-key": "vserver.vserver-name",
            "property-value": "USUCP0PCOIL0110UJZZ01-vsrx"
         }]
      }
   ]}
} 
 */
