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

import org.onap.policy.aai.AaiNqRequest;
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
		AaiNqRequest request = Serialization.gsonPretty.fromJson(req, AaiNqRequest.class);
		
		if (request.getInstanceFilters().getInstanceFilter().get(0).containsKey("vserver"))
		{
			String vserverName = request.getInstanceFilters().getInstanceFilter().get(0).get("vserver").get("vserver-name");
			if ("error".equals(vserverName)) {
				return "{\"requestError\":{\"serviceException\":{\"messageId\":\"SVC3001\",\"text\":\"Resource not found for %1 using id %2 (msg=%3) (ec=%4)\",\"variables\":[\"POST Search\",\"getNamedQueryResponse\",\"Node Not Found:No Node of type vserver found for properties\",\"ERR.5.4.6114\"]}}}";
			}
			else {
				// vll format - new
				// new aai response from Brian 11/13/2017
				return "{\"inventory-response-item\":[{\"vserver\":{\"vserver-id\":\"6ed3642c-f7a1-4a7c-9290-3d51fe1531eb\",\"vserver-name\":\"zdfw1lb01lb02\",\"vserver-name2\":\"zdfw1lb01lb02\",\"prov-status\":\"ACTIVE\",\"vserver-selflink\":\"http://10.12.25.2:8774/v2.1/41d6d38489bd40b09ea8a6b6b852dcbd/servers/6ed3642c-f7a1-4a7c-9290-3d51fe1531eb\",\"in-maint\":false,\"is-closed-loop-disabled\":false,\"resource-version\":\"1510606403522\"},\"extra-properties\":{},\"inventory-response-items\":{\"inventory-response-item\":[{\"model-name\":\"vLoadBalancer\",\"generic-vnf\":{\"vnf-id\":\"db373a8d-f7be-4d02-8ac8-6ca4c305d144\",\"vnf-name\":\"Vfmodule_vLB1113\",\"vnf-type\":\"vLoadBalancer-1106/vLoadBalancer 0\",\"service-id\":\"66f157fc-4148-4880-95f5-e120677e98d1\",\"prov-status\":\"PREPROV\",\"orchestration-status\":\"Created\",\"in-maint\":false,\"is-closed-loop-disabled\":false,\"resource-version\":\"1510604011851\",\"model-invariant-id\":\"cee050ed-92a5-494f-ab04-234307a846dc\",\"model-version-id\":\"fd65becc-6b2c-4fe8-ace9-cc29db9a3da2\",\"model-customization-id\":\"1983c783-444f-4e79-af3a-85e5d49628f3\",\"nf-type\":\"\",\"nf-function\":\"\",\"nf-role\":\"\",\"nf-naming-code\":\"\"},\"extra-properties\":{\"extra-property\":[{\"property-name\":\"model-ver.model-version-id\",\"property-value\":\"fd65becc-6b2c-4fe8-ace9-cc29db9a3da2\"},{\"property-name\":\"model-ver.model-name\",\"property-value\":\"vLoadBalancer\"},{\"property-name\":\"model.model-type\",\"property-value\":\"resource\"},{\"property-name\":\"model.model-invariant-id\",\"property-value\":\"cee050ed-92a5-494f-ab04-234307a846dc\"},{\"property-name\":\"model-ver.model-version\",\"property-value\":\"1.0\"}]},\"inventory-response-items\":{\"inventory-response-item\":[{\"model-name\":\"vLoadBalancer-1106\",\"service-instance\":{\"service-instance-id\":\"3b12f31f-8f2d-4f5c-b875-61ff1194b941\",\"service-instance-name\":\"vLoadBalancer-1113\",\"model-invariant-id\":\"1321d60d-f7ff-4300-96c2-6bf0b3268b7a\",\"model-version-id\":\"732d4692-4b97-46f9-a996-0b3339e88c50\",\"resource-version\":\"1510603936425\"},\"extra-properties\":{\"extra-property\":[{\"property-name\":\"model-ver.model-version-id\",\"property-value\":\"732d4692-4b97-46f9-a996-0b3339e88c50\"},{\"property-name\":\"model-ver.model-name\",\"property-value\":\"vLoadBalancer-1106\"},{\"property-name\":\"model.model-type\",\"property-value\":\"service\"},{\"property-name\":\"model.model-invariant-id\",\"property-value\":\"1321d60d-f7ff-4300-96c2-6bf0b3268b7a\"},{\"property-name\":\"model-ver.model-version\",\"property-value\":\"1.0\"}]}},{\"model-name\":\"Vloadbalancer..base_vlb..module-0\",\"vf-module\":{\"vf-module-id\":\"e6b3e3eb-34e1-4c00-b8c1-2a4fbe479b12\",\"vf-module-name\":\"Vfmodule_vLB1113-1\",\"heat-stack-id\":\"Vfmodule_vLB1113-1/3dd6d900-772f-4fcc-a0cb-e250ab2bb4db\",\"orchestration-status\":\"active\",\"is-base-vf-module\":true,\"resource-version\":\"1510604612557\",\"model-invariant-id\":\"6d760188-9a24-451a-b05b-e08b86cb94f2\",\"model-version-id\":\"93facad9-55f2-4fe0-9574-814c2bc2d071\",\"model-customization-id\":\"93fd5bd4-8051-4074-8530-c0c504604df5\",\"module-index\":0},\"extra-properties\":{\"extra-property\":[{\"property-name\":\"model-ver.model-version-id\",\"property-value\":\"93facad9-55f2-4fe0-9574-814c2bc2d071\"},{\"property-name\":\"model-ver.model-name\",\"property-value\":\"Vloadbalancer..base_vlb..module-0\"},{\"property-name\":\"model.model-type\",\"property-value\":\"resource\"},{\"property-name\":\"model.model-invariant-id\",\"property-value\":\"6d760188-9a24-451a-b05b-e08b86cb94f2\"},{\"property-name\":\"model-ver.model-version\",\"property-value\":\"1\"}]}},{\"model-name\":\"Vloadbalancer..dnsscaling..module-1\",\"vf-module\":{\"vf-module-id\":\"dummy_db373a8d-f7be-4d02-8ac8-6ca4c305d144\",\"vf-module-name\":\"dummy_db373a8d-f7be-4d02-8ac8-6ca4c305d144\",\"is-base-vf-module\":false,\"resource-version\":\"1510610079687\",\"model-invariant-id\":\"356a1cff-71f2-4086-9980-a2927ce11c1c\",\"model-version-id\":\"6b93d804-cfc8-4be3-92cc-9336d135859a\"},\"extra-properties\":{\"extra-property\":[{\"property-name\":\"model-ver.model-version-id\",\"property-value\":\"6b93d804-cfc8-4be3-92cc-9336d135859a\"},{\"property-name\":\"model-ver.model-name\",\"property-value\":\"Vloadbalancer..dnsscaling..module-1\"},{\"property-name\":\"model.model-type\",\"property-value\":\"resource\"},{\"property-name\":\"model.model-invariant-id\",\"property-value\":\"356a1cff-71f2-4086-9980-a2927ce11c1c\"},{\"property-name\":\"model-ver.model-version\",\"property-value\":\"1\"}]}}]}},{\"tenant\":{\"tenant-id\":\"41d6d38489bd40b09ea8a6b6b852dcbd\",\"tenant-name\":\"Integration-SB-00\",\"resource-version\":\"1509587770200\"},\"extra-properties\":{},\"inventory-response-items\":{\"inventory-response-item\":[{\"cloud-region\":{\"cloud-owner\":\"CloudOwner\",\"cloud-region-id\":\"RegionOne\",\"cloud-type\":\"SharedNode\",\"owner-defined-type\":\"OwnerType\",\"cloud-region-version\":\"v1\",\"cloud-zone\":\"CloudZone\",\"sriov-automation\":false,\"resource-version\":\"1509587770092\"},\"extra-properties\":{}}]}}]}}]}";
			}
		}
		else 
		{
			String vnfID = request.getInstanceFilters().getInstanceFilter().get(0).get("generic-vnf").get("vnf-id");
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
	public String getByVnfName (@QueryParam("vnf-name") String vnfName)
	{
		if ("getFail".equals(vnfName)) {
			return "{\"requestError\":{\"serviceException\":{\"messageId\":\"SVC3001\",\"text\":\"Resource not found for %1 using id %2 (msg=%3) (ec=%4)\",\"variables\":[\"GET\",\"network/generic-vnfs/generic-vnf\",\"Node Not Found:No Node of type generic-vnf found at network/generic-vnfs/generic-vnf\",\"ERR.5.4.6114\"]}}}";
		}
		boolean isDisabled = "disableClosedLoop".equals(vnfName);
		if ("error".equals(vnfName)) {
	        return "{ \"vnf-id\": \"error\", \"vnf-name\": \"" + vnfName + "\", \"vnf-type\": \"RT\", \"service-id\": \"d7bb0a21-66f2-4e6d-87d9-9ef3ced63ae4\", \"equipment-role\": \"UCPE\", \"orchestration-status\": \"created\", \"management-option\": \"ATT\", \"ipv4-oam-address\": \"32.40.68.35\", \"ipv4-loopback0-address\": \"32.40.64.57\", \"nm-lan-v6-address\": \"2001:1890:e00e:fffe::1345\", \"management-v6-address\": \"2001:1890:e00e:fffd::36\", \"in-maint\": false, \"is-closed-loop-disabled\": " + isDisabled + ", \"resource-version\": \"1493389458092\", \"relationship-list\": {\"relationship\":[{ \"related-to\": \"service-instance\", \"related-link\": \"/aai/v11/business/customers/customer/1610_Func_Global_20160817084727/service-subscriptions/service-subscription/uCPE-VMS/service-instances/service-instance/USUCP0PCOIL0110UJZZ01\", \"relationship-data\":[{ \"relationship-key\": \"customer.global-customer-id\", \"relationship-value\": \"1610_Func_Global_20160817084727\"},{ \"relationship-key\": \"service-subscription.service-type\", \"relationship-value\": \"uCPE-VMS\"},{ \"relationship-key\": \"service-instance.service-instance-id\", \"relationship-value\": \"USUCP0PCOIL0110UJZZ01\"} ], \"related-to-property\": [{\"property-key\": \"service-instance.service-instance-name\"}]},{ \"related-to\": \"vserver\", \"related-link\": \"/aai/v11/cloud-infrastructure/cloud-regions/cloud-region/att-aic/AAIAIC25/tenants/tenant/USUCP0PCOIL0110UJZZ01%3A%3AuCPE-VMS/vservers/vserver/3b2558f4-39d8-40e7-bfc7-30660fb52c45\", \"relationship-data\":[{ \"relationship-key\": \"cloud-region.cloud-owner\", \"relationship-value\": \"att-aic\"},{ \"relationship-key\": \"cloud-region.cloud-region-id\", \"relationship-value\": \"AAIAIC25\"},{ \"relationship-key\": \"tenant.tenant-id\", \"relationship-value\": \"USUCP0PCOIL0110UJZZ01::uCPE-VMS\"},{ \"relationship-key\": \"vserver.vserver-id\", \"relationship-value\": \"3b2558f4-39d8-40e7-bfc7-30660fb52c45\"} ], \"related-to-property\": [ {\"property-key\": \"vserver.vserver-name\",\"property-value\": \"USUCP0PCOIL0110UJZZ01-vsrx\" }]} ]}}";
		    
		}
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
	public String getByVserverName (@QueryParam("vserver-name") String vserverName)
	{
		if ("getFail".equals(vserverName)) {
			return "{\"requestError\":{\"serviceException\":{\"messageId\":\"SVC3001\",\"text\":\"Resource not found for %1 using id %2 (msg=%3) (ec=%4)\",\"variables\":[\"GET\",\"nodes/vservers\",\"Node Not Found:No Node of type generic-vnf found at nodes/vservers\",\"ERR.5.4.6114\"]}}}";
		}
		boolean isDisabled = "disableClosedLoop".equals(vserverName);
		return "{\"vserver\": [{ \"vserver-id\": \"d0668d4f-c25e-4a1b-87c4-83845c01efd8\", \"vserver-name\": \"" + vserverName + "\", \"vserver-name2\": \"vjunos0\", \"vserver-selflink\": \"https://aai-ext1.test.att.com:8443/aai/v7/cloud-infrastructure/cloud-regions/cloud-region/att-aic/AAIAIC25/tenants/tenant/USMSO1SX7NJ0103UJZZ01%3A%3AuCPE-VMS/vservers/vserver/d0668d4f-c25e-4a1b-87c4-83845c01efd8\", \"in-maint\": false, \"is-closed-loop-disabled\": " + isDisabled + ", \"resource-version\": \"1494001931513\", \"relationship-list\": {\"relationship\":[{ \"related-to\": \"generic-vnf\", \"related-link\": \"/aai/v11/network/generic-vnfs/generic-vnf/e1a41e99-4ede-409a-8f9d-b5e12984203a\", \"relationship-data\": [ {\"relationship-key\": \"generic-vnf.vnf-id\",\"relationship-value\": \"e1a41e99-4ede-409a-8f9d-b5e12984203a\" }], \"related-to-property\": [ {\"property-key\": \"generic-vnf.vnf-name\",\"property-value\": \"USMSO1SX7NJ0103UJSW01\" }]},{ \"related-to\": \"pserver\", \"related-link\": \"/aai/v11/cloud-infrastructure/pservers/pserver/USMSO1SX7NJ0103UJZZ01\", \"relationship-data\": [ {\"relationship-key\": \"pserver.hostname\",\"relationship-value\": \"USMSO1SX7NJ0103UJZZ01\" }], \"related-to-property\": [{\"property-key\": \"pserver.pserver-name2\"}]} ]}}]}";
	}
}
