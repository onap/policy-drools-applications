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
import java.util.LinkedList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class AAIGETVserverResponse implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6247505944905898870L;
 
	@SerializedName("vserver-id")
	public String vserverID;
	
	@SerializedName("vserver-name")
	public String vserverName;
	
	@SerializedName("vserver-name2")
	public String vserverName2;
	
	@SerializedName("vserver-selflink")
	public String vserverSelflink;
	
	@SerializedName("in-maint")
	public String inMaint;
	
	@SerializedName("is-closed-loop-disabled")
	public String isClosedLoopDisabled;
	
	@SerializedName("resource-version")
	public String resourceVersion; 
	
	@SerializedName("model-invariant-id")
	public String modelInvariantId;
	
	public RelationshipList relationshipList;

	public AAIGETVserverResponse() {
	}

}

/* sample vserver data
   {"vserver": [{
   "vserver-id": "d0668d4f-c25e-4a1b-87c4-83845c01efd8",
   "vserver-name": "USMSO1SX7NJ0103UJZZ01-vjunos0",
   "vserver-name2": "vjunos0",
   "vserver-selflink": "https://aai-ext1.test.att.com:8443/aai/v7/cloud-infrastructure/cloud-regions/cloud-region/att-aic/AAIAIC25/tenants/tenant/USMSO1SX7NJ0103UJZZ01%3A%3AuCPE-VMS/vservers/vserver/d0668d4f-c25e-4a1b-87c4-83845c01efd8",
   "in-maint": false,
   "is-closed-loop-disabled": false,
   "resource-version": "1494001931513",
   "relationship-list": {"relationship":    [
            {
         "related-to": "generic-vnf",
         "related-link": "/aai/v11/network/generic-vnfs/generic-vnf/e1a41e99-4ede-409a-8f9d-b5e12984203a",
         "relationship-data": [         {
            "relationship-key": "generic-vnf.vnf-id",
            "relationship-value": "e1a41e99-4ede-409a-8f9d-b5e12984203a"
         }],
         "related-to-property": [         {
            "property-key": "generic-vnf.vnf-name",
            "property-value": "USMSO1SX7NJ0103UJSW01"
         }]
      },
            {
         "related-to": "pserver",
         "related-link": "/aai/v11/cloud-infrastructure/pservers/pserver/USMSO1SX7NJ0103UJZZ01",
         "relationship-data": [         {
            "relationship-key": "pserver.hostname",
            "relationship-value": "USMSO1SX7NJ0103UJZZ01"
         }],
         "related-to-property": [{"property-key": "pserver.pserver-name2"}]
      }
   ]}
}]}
*/
