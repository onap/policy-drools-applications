/*-
 * ============LICENSE_START=======================================================
 * simulators
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

package org.onap.policy.simulators;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.onap.policy.aai.AAINQRequest;
import org.onap.policy.aai.util.Serialization;

@Path("/aai")
public class AaiSimulatorJaxRs {
	
	@GET
	@Path("/v8/network/generic-vnfs/generic-vnf/{vnfId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces("application/json")
	public String aaiGetQuery (@PathParam("vnfID") String vnfId)
	{
		return "{\"relationship-list\": {\"relationship\":[{\"related-to-property\": [{\"property-key\": \"service-instance.service-instance-name\"}]},{\"related-to-property\": [ {\"property-key\": \"vserver.vserver-name\",\"property-value\": \"USUCP0PCOIL0110UJZZ01-vsrx\" }]} ]}}";
	}
	
	@POST
	@Path("/search/named-query")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces("application/json")
	public String aaiPostQuery(String req)
	{
		AAINQRequest request = Serialization.gsonPretty.fromJson(req, AAINQRequest.class);
		
		if (request.instanceFilters.instanceFilter.get(0).containsKey("vserver"))
		{
			String vserverName = request.instanceFilters.instanceFilter.get(0).get("vserver").get("vserver-name");
			System.out.println("The name is... "+vserverName);
			if ("error".equals(vserverName)) {
				return "{\"requestError\":{\"serviceException\":{\"messageId\":\"SVC3001\",\"text\":\"Resource not found for %1 using id %2 (msg=%3) (ec=%4)\",\"variables\":[\"POST Search\",\"getNamedQueryResponse\",\"Node Not Found:No Node of type vserver found for properties\",\"ERR.5.4.6114\"]}}}";
			}
			else {
				// vll format - new
				return "{\"inventory-response-item\":[{\"extra-properties\":{},\"inventory-response-items\":{\"inventory-response-item\":[{\"extra-properties\":{\"extra-property\":[{\"property-name\":\"model-ver.model-version-id\",\"property-value\":\"93a6166f-b3d5-4f06-b4ba-aed48d009ad9\"},{\"property-name\":\"model-ver.model-name\",\"property-value\":\"generic-vnf\"},{\"property-name\":\"model.model-type\",\"property-value\":\"widget\"},{\"property-name\":\"model.model-invariant-id\",\"property-value\":\"acc6edd8-a8d4-4b93-afaa-0994068be14c\"},{\"property-name\":\"model-ver.model-version\",\"property-value\":\"1.0\"}]},\"generic-vnf\":{\"in-maint\":false,\"is-closed-loop-disabled\":false,\"model-invariant-id\":\"acc6edd8-a8d4-4b93-afaa-0994068be14c\",\"model-version-id\":\"93a6166f-b3d5-4f06-b4ba-aed48d009ad9\",\"orchestration-status\":\"Created\",\"resource-version\":\"1507826325834\",\"service-id\":\"b3f70641-bdb9-4030-825e-6abb73a1f929\",\"vnf-id\":\"594e2fe0-48b8-41ff-82e2-3d4bab69b192\",\"vnf-name\":\"Vnf_Ete_Named90e1ab3-dcd5-4877-9edb-eadfc84e32c8\",\"vnf-type\":\"8330e932-2a23-4943-8606/c15ce9e1-e914-4c8f-b8bb 1\"},\"inventory-response-items\":{\"inventory-response-item\":[{\"extra-properties\":{\"extra-property\":[{\"property-name\":\"model-ver.model-version-id\",\"property-value\":\"46b92144-923a-4d20-b85a-3cbd847668a9\"},{\"property-name\":\"model-ver.model-name\",\"property-value\":\"service-instance\"},{\"property-name\":\"model.model-type\",\"property-value\":\"widget\"},{\"property-name\":\"model.model-invariant-id\",\"property-value\":\"82194af1-3c2c-485a-8f44-420e22a9eaa4\"},{\"property-name\":\"model-ver.model-version\",\"property-value\":\"1.0\"}]},\"model-name\":\"service-instance\",\"service-instance\":{\"model-invariant-id\":\"82194af1-3c2c-485a-8f44-420e22a9eaa4\",\"model-version-id\":\"46b92144-923a-4d20-b85a-3cbd847668a9\",\"resource-version\":\"1507827626200\",\"service-instance-id\":\"cf8426a6-0b53-4e3d-bfa6-4b2f4d5913a5\",\"service-instance-name\":\"Service_Ete_Named90e1ab3-dcd5-4877-9edb-eadfc84e32c8\"}},{\"extra-properties\":{\"extra-property\":[{\"property-name\":\"model-ver.model-version-id\",\"property-value\":\"93a6166f-b3d5-4f06-b4ba-aed48d009ad9\"},{\"property-name\":\"model-ver.model-name\",\"property-value\":\"generic-vnf\"},{\"property-name\":\"model.model-type\",\"property-value\":\"widget\"},{\"property-name\":\"model.model-invariant-id\",\"property-value\":\"acc6edd8-a8d4-4b93-afaa-0994068be14c\"},{\"property-name\":\"model-ver.model-version\",\"property-value\":\"1.0\"}]},\"model-name\":\"generic-vnf\",\"vf-module\":{\"heat-stack-id\":\"Vfmodule_Ete_Named90e1ab3-dcd5-4877-9edb-eadfc84e32c8/5845f37b-6cda-4e91-8ca3-f5572d226488\",\"is-base-vf-module\":true,\"model-invariant-id\":\"acc6edd8-a8d4-4b93-afaa-0994068be14c\",\"model-version-id\":\"93a6166f-b3d5-4f06-b4ba-aed48d009ad9\",\"orchestration-status\":\"active\",\"resource-version\":\"1507826326804\",\"vf-module-id\":\"b0eff878-e2e1-4947-9597-39afdd0f51dd\",\"vf-module-name\":\"Vfmodule_Ete_Named90e1ab3-dcd5-4877-9edb-eadfc84e32c8\"}}]},\"model-name\":\"generic-vnf\"},{\"extra-properties\":{},\"inventory-response-items\":{\"inventory-response-item\":[{\"cloud-region\":{\"cloud-owner\":\"Rackspace\",\"cloud-region-id\":\"DFW\",\"cloud-region-version\":\"v1\",\"cloud-type\":\"SharedNode\",\"cloud-zone\":\"CloudZone\",\"owner-defined-type\":\"OwnerType\",\"resource-version\":\"1507828410019\",\"sriov-automation\":false},\"extra-properties\":{}}]},\"tenant\":{\"resource-version\":\"1507828410764\",\"tenant-id\":\"1015548\",\"tenant-name\":\"1015548\"}}]},\"vserver\":{\"in-maint\":false,\"is-closed-loop-disabled\":false,\"prov-status\":\"ACTIVE\",\"resource-version\":\"1507828410832\",\"vserver-id\":\"70f081eb-2a87-4c81-9296-4b93d7d145c6\",\"vserver-name\":\"vlb-lb-32c8\",\"vserver-name2\":\"vlb-lb-32c8\",\"vserver-selflink\":\"https://aai.api.simpledemo.openecomp.org:8443/aai/v11/nodes/vservers?vserver-name=vlb-lb-32c8\"}}]}";
			}
		}
		else 
		{
			String vnfID = request.instanceFilters.instanceFilter.get(0).get("generic-vnf").get("vnf-id");
			if ("error".equals(vnfID)) {
				return "{\"requestError\":{\"serviceException\":{\"messageId\":\"SVC3001\",\"text\":\"Resource not found for %1 using id %2 (msg=%3) (ec=%4)\",\"variables\":[\"POST Search\",\"getNamedQueryResponse\",\"Node Not Found:No Node of type generic-vnf found for properties\",\"ERR.5.4.6114\"]}}}";
			}
			else {
				return "{\"inventory-response-item\": [{\"model-name\": \"service-instance\",\"generic-vnf\": {\"vnf-id\": \""+ vnfID + "\",\"vnf-name\": \"ZRDM2MMEX39\",\"vnf-type\": \"vMME Svc Jul 14/vMME VF Jul 14 1\",\"service-id\": \"a9a77d5a-123e-4ca2-9eb9-0b015d2ee0fb\",\"orchestration-status\": \"active\",\"in-maint\": false,\"is-closed-loop-disabled\": false,\"resource-version\": \"1503082370097\",\"model-invariant-id\": \"82194af1-3c2c-485a-8f44-420e22a9eaa4\",\"model-version-id\": \"46b92144-923a-4d20-b85a-3cbd847668a9\"},\"extra-properties\": {},\"inventory-response-items\": {\"inventory-response-item\": [{\"model-name\": \"service-instance\",\"service-instance\": {\"service-instance-id\": \"37b8cdb7-94eb-468f-a0c2-4e3c3546578e\",\"service-instance-name\": \"Changed Service Instance NAME\",\"model-invariant-id\": \"82194af1-3c2c-485a-8f44-420e22a9eaa4\",\"model-version-id\": \"46b92144-923a-4d20-b85a-3cbd847668a9\",\"resource-version\": \"1503082993532\",\"orchestration-status\": \"Active\"},\"extra-properties\": {},\"inventory-response-items\": {\"inventory-response-item\": [{\"model-name\": \"pnf\",\"generic-vnf\": {\"vnf-id\": \"jimmy-test\",\"vnf-name\": \"jimmy-test-vnf\",\"vnf-type\": \"vMME Svc Jul 14/vMME VF Jul 14 1\",\"service-id\": \"a9a77d5a-123e-4ca2-9eb9-0b015d2ee0fb\",\"orchestration-status\": \"active\",\"in-maint\": false,\"is-closed-loop-disabled\": false,\"resource-version\": \"1504013830207\",\"model-invariant-id\": \"862b25a1-262a-4961-bdaa-cdc55d69785a\",\"model-version-id\": \"e9f1fa7d-c839-418a-9601-03dc0d2ad687\"},\"extra-properties\": {}},{\"model-name\": \"service-instance\",\"generic-vnf\": {\"vnf-id\": \"jimmy-test-vnf2\",\"vnf-name\": \"jimmy-test-vnf2-named\",\"vnf-type\": \"vMME Svc Jul 14/vMME VF Jul 14 1\",\"service-id\": \"a9a77d5a-123e-4ca2-9eb9-0b015d2ee0fb\",\"orchestration-status\": \"active\",\"in-maint\": false,\"is-closed-loop-disabled\": false,\"resource-version\": \"1504014833841\",\"model-invariant-id\": \"Eace933104d443b496b8.nodes.heat.vpg\",\"model-version-id\": \"46b92144-923a-4d20-b85a-3cbd847668a9\"},\"extra-properties\": {}}]}}]}}]}";
			}
		}
	}
	
	@GET
	@Path("/v11/network/generic-vnfs/generic-vnf")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces("application/json")
	public String getByVnfName (@QueryParam("vnfName") String vnfName)
	{
		if ("getFail".equals(vnfName)) {
			return "{\"requestError\":{\"serviceException\":{\"messageId\":\"SVC3001\",\"text\":\"Resource not found for %1 using id %2 (msg=%3) (ec=%4)\",\"variables\":[\"GET\",\"network/generic-vnfs/generic-vnf\",\"Node Not Found:No Node of type generic-vnf found at network/generic-vnfs/generic-vnf\",\"ERR.5.4.6114\"]}}}";
		}
		boolean isDisabled = "disableClosedLoop".equals(vnfName);
		return "{ \"vnf-id\": \"5e49ca06-2972-4532-9ed4-6d071588d792\", \"vnf-name\": \"" + vnfName + "\", \"vnf-type\": \"RT\", \"service-id\": \"d7bb0a21-66f2-4e6d-87d9-9ef3ced63ae4\", \"equipment-role\": \"UCPE\", \"orchestration-status\": \"created\", \"management-option\": \"ATT\", \"ipv4-oam-address\": \"32.40.68.35\", \"ipv4-loopback0-address\": \"32.40.64.57\", \"nm-lan-v6-address\": \"2001:1890:e00e:fffe::1345\", \"management-v6-address\": \"2001:1890:e00e:fffd::36\", \"in-maint\": false, \"is-closed-loop-disabled\": " + isDisabled + ", \"resource-version\": \"1493389458092\", \"relationship-list\": {\"relationship\":[{ \"related-to\": \"service-instance\", \"related-link\": \"/aai/v11/business/customers/customer/1610_Func_Global_20160817084727/service-subscriptions/service-subscription/uCPE-VMS/service-instances/service-instance/USUCP0PCOIL0110UJZZ01\", \"relationship-data\":[{ \"relationship-key\": \"customer.global-customer-id\", \"relationship-value\": \"1610_Func_Global_20160817084727\"},{ \"relationship-key\": \"service-subscription.service-type\", \"relationship-value\": \"uCPE-VMS\"},{ \"relationship-key\": \"service-instance.service-instance-id\", \"relationship-value\": \"USUCP0PCOIL0110UJZZ01\"} ], \"related-to-property\": [{\"property-key\": \"service-instance.service-instance-name\"}]},{ \"related-to\": \"vserver\", \"related-link\": \"/aai/v11/cloud-infrastructure/cloud-regions/cloud-region/att-aic/AAIAIC25/tenants/tenant/USUCP0PCOIL0110UJZZ01%3A%3AuCPE-VMS/vservers/vserver/3b2558f4-39d8-40e7-bfc7-30660fb52c45\", \"relationship-data\":[{ \"relationship-key\": \"cloud-region.cloud-owner\", \"relationship-value\": \"att-aic\"},{ \"relationship-key\": \"cloud-region.cloud-region-id\", \"relationship-value\": \"AAIAIC25\"},{ \"relationship-key\": \"tenant.tenant-id\", \"relationship-value\": \"USUCP0PCOIL0110UJZZ01::uCPE-VMS\"},{ \"relationship-key\": \"vserver.vserver-id\", \"relationship-value\": \"3b2558f4-39d8-40e7-bfc7-30660fb52c45\"} ], \"related-to-property\": [ {\"property-key\": \"vserver.vserver-name\",\"property-value\": \"USUCP0PCOIL0110UJZZ01-vsrx\" }]} ]}}";
	}
	
	@GET
	@Path("/v11/network/generic-vnfs/generic-vnf/{vnfId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces("application/json")
	public String getByVnfId (@PathParam("vnfId") String vnfId)
	{
		if ("getFail".equals(vnfId)) {
			return "{\"requestError\":{\"serviceException\":{\"messageId\":\"SVC3001\",\"text\":\"Resource not found for %1 using id %2 (msg=%3) (ec=%4)\",\"variables\":[\"GET\",\"network/generic-vnfs/generic-vnf/getFail\",\"Node Not Found:No Node of type generic-vnf found at network/generic-vnfs/generic-vnf/getFail\",\"ERR.5.4.6114\"]}}}";
		}
		boolean isDisabled = "disableClosedLoop".equals(vnfId);
		return "{ \"vnf-id\": \"" + vnfId + "\", \"vnf-name\": \"USUCP0PCOIL0110UJRT01\", \"vnf-type\": \"RT\", \"service-id\": \"d7bb0a21-66f2-4e6d-87d9-9ef3ced63ae4\", \"equipment-role\": \"UCPE\", \"orchestration-status\": \"created\", \"management-option\": \"ATT\", \"ipv4-oam-address\": \"32.40.68.35\", \"ipv4-loopback0-address\": \"32.40.64.57\", \"nm-lan-v6-address\": \"2001:1890:e00e:fffe::1345\", \"management-v6-address\": \"2001:1890:e00e:fffd::36\", \"in-maint\": false, \"is-closed-loop-disabled\": " + isDisabled + ", \"resource-version\": \"1493389458092\", \"relationship-list\": {\"relationship\":[{ \"related-to\": \"service-instance\", \"related-link\": \"/aai/v11/business/customers/customer/1610_Func_Global_20160817084727/service-subscriptions/service-subscription/uCPE-VMS/service-instances/service-instance/USUCP0PCOIL0110UJZZ01\", \"relationship-data\":[{ \"relationship-key\": \"customer.global-customer-id\", \"relationship-value\": \"1610_Func_Global_20160817084727\"},{ \"relationship-key\": \"service-subscription.service-type\", \"relationship-value\": \"uCPE-VMS\"},{ \"relationship-key\": \"service-instance.service-instance-id\", \"relationship-value\": \"USUCP0PCOIL0110UJZZ01\"} ], \"related-to-property\": [{\"property-key\": \"service-instance.service-instance-name\"}]},{ \"related-to\": \"vserver\", \"related-link\": \"/aai/v11/cloud-infrastructure/cloud-regions/cloud-region/att-aic/AAIAIC25/tenants/tenant/USUCP0PCOIL0110UJZZ01%3A%3AuCPE-VMS/vservers/vserver/3b2558f4-39d8-40e7-bfc7-30660fb52c45\", \"relationship-data\":[{ \"relationship-key\": \"cloud-region.cloud-owner\", \"relationship-value\": \"att-aic\"},{ \"relationship-key\": \"cloud-region.cloud-region-id\", \"relationship-value\": \"AAIAIC25\"},{ \"relationship-key\": \"tenant.tenant-id\", \"relationship-value\": \"USUCP0PCOIL0110UJZZ01::uCPE-VMS\"},{ \"relationship-key\": \"vserver.vserver-id\", \"relationship-value\": \"3b2558f4-39d8-40e7-bfc7-30660fb52c45\"} ], \"related-to-property\": [ {\"property-key\": \"vserver.vserver-name\",\"property-value\": \"USUCP0PCOIL0110UJZZ01-vsrx\" }]} ]}}";
	}
	
	@GET
	@Path("/v11/nodes/vservers")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces("application/json")
	public String getByVserverName (@QueryParam("vserverName") String vserverName)
	{
		if ("getFail".equals(vserverName)) {
			return "{\"requestError\":{\"serviceException\":{\"messageId\":\"SVC3001\",\"text\":\"Resource not found for %1 using id %2 (msg=%3) (ec=%4)\",\"variables\":[\"GET\",\"nodes/vservers\",\"Node Not Found:No Node of type generic-vnf found at nodes/vservers\",\"ERR.5.4.6114\"]}}}";
		}
		boolean isDisabled = "disableClosedLoop".equals(vserverName);
		return "{\"vserver\": [{ \"vserver-id\": \"d0668d4f-c25e-4a1b-87c4-83845c01efd8\", \"vserver-name\": \"" + vserverName + "\", \"vserver-name2\": \"vjunos0\", \"vserver-selflink\": \"https://aai-ext1.test.att.com:8443/aai/v7/cloud-infrastructure/cloud-regions/cloud-region/att-aic/AAIAIC25/tenants/tenant/USMSO1SX7NJ0103UJZZ01%3A%3AuCPE-VMS/vservers/vserver/d0668d4f-c25e-4a1b-87c4-83845c01efd8\", \"in-maint\": false, \"is-closed-loop-disabled\": " + isDisabled + ", \"resource-version\": \"1494001931513\", \"relationship-list\": {\"relationship\":[{ \"related-to\": \"generic-vnf\", \"related-link\": \"/aai/v11/network/generic-vnfs/generic-vnf/e1a41e99-4ede-409a-8f9d-b5e12984203a\", \"relationship-data\": [ {\"relationship-key\": \"generic-vnf.vnf-id\",\"relationship-value\": \"e1a41e99-4ede-409a-8f9d-b5e12984203a\" }], \"related-to-property\": [ {\"property-key\": \"generic-vnf.vnf-name\",\"property-value\": \"USMSO1SX7NJ0103UJSW01\" }]},{ \"related-to\": \"pserver\", \"related-link\": \"/aai/v11/cloud-infrastructure/pservers/pserver/USMSO1SX7NJ0103UJZZ01\", \"relationship-data\": [ {\"relationship-key\": \"pserver.hostname\",\"relationship-value\": \"USMSO1SX7NJ0103UJZZ01\" }], \"related-to-property\": [{\"property-key\": \"pserver.pserver-name2\"}]} ]}}]}";
	}
}
