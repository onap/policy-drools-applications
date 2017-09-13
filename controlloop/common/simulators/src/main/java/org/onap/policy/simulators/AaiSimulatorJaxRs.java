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
import javax.ws.rs.core.MediaType;

import org.onap.policy.aai.AAINQF199.AAINQF199Request;
import org.onap.policy.aai.util.Serialization;

@Path("/aai")
public class AaiSimulatorJaxRs {
	
	@GET
	@Path("/v8/network/generic-vnfs/generic-vnf/{vnfId}")
	public String aaiGetQuery (@PathParam("vnfID") String vnfId)
	{
		return "{\"relationship-list\": {\"relationship\":[{\"related-to-property\": [{\"property-key\": \"service-instance.service-instance-name\"}]},{\"related-to-property\": [ {\"property-key\": \"vserver.vserver-name\",\"property-value\": \"USUCP0PCOIL0110UJZZ01-vsrx\" }]} ]}}";
	}
	
	@POST
	@Path("/search/named-query")
	@Consumes(MediaType.APPLICATION_JSON)
	public String aaiPostQuery(String req)
	{
		AAINQF199Request request = Serialization.gsonPretty.fromJson(req, AAINQF199Request.class);
		
		if (request.instanceFilters.instanceFilter.get(0).containsKey("vserver"))
		{
			return "{\n\t\"inventory-response-item\": [{\n\t\t\t\"extra-properties\": {},\n\t\t\t\"inventory-response-items\": {\n\t\t\t\t\"inventory-response-item\": [{\n\t\t\t\t\t\t\"extra-properties\": {\n\t\t\t\t\t\t\t\"extra-property\": [{\n\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-name\",\n\t\t\t\t\t\t\t\t\t\"property-value\": \"c15ce9e1-e914-4c8f-b8bb\"\n\t\t\t\t\t\t\t\t}, {\n\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-type\",\n\t\t\t\t\t\t\t\t\t\"property-value\": \"resource\"\n\t\t\t\t\t\t\t\t}, {\n\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-version\",\n\t\t\t\t\t\t\t\t\t\"property-value\": \"1\"\n\t\t\t\t\t\t\t\t}, {\n\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-id\",\n\t\t\t\t\t\t\t\t\t\"property-value\": \"033a32ed-aa65-4764-a736-36f2942f1aa0\"\n\t\t\t\t\t\t\t\t}, {\n\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-name-version-id\",\n\t\t\t\t\t\t\t\t\t\"property-value\": \"d4d072dc-4e21-4a03-9524-628985819a8e\"\n\t\t\t\t\t\t\t\t}\n\t\t\t\t\t\t\t]\n\t\t\t\t\t\t},\n\t\t\t\t\t\t\"generic-vnf\": {\n\t\t\t\t\t\t\t\"in-maint\": false,\n\t\t\t\t\t\t\t\"is-closed-loop-disabled\": false,\n\t\t\t\t\t\t\t\"orchestration-status\": \"Created\",\n\t\t\t\t\t\t\t\"model-invariant-id\": \"033a32ed-aa65-4764-a736-36f2942f1aa0\",\n\t\t\t\t\t\t\t\"model-version-id\": \"1.0\",\n\t\t\t\t\t\t\t\"resource-version\": \"1485542422\",\n\t\t\t\t\t\t\t\"service-id\": \"b3f70641-bdb9-4030-825e-6abb73a1f929\",\n\t\t\t\t\t\t\t\"vnf-id\": \"594e2fe0-48b8-41ff-82e2-3d4bab69b192\",\n\t\t\t\t\t\t\t\"vnf-name\": \"Vnf_Ete_Named90e1ab3-dcd5-4877-9edb-eadfc84e32c8\",\n\t\t\t\t\t\t\t\"vnf-type\": \"8330e932-2a23-4943-8606/c15ce9e1-e914-4c8f-b8bb 1\"\n\t\t\t\t\t\t},\n\t\t\t\t\t\t\"inventory-response-items\": {\n\t\t\t\t\t\t\t\"inventory-response-item\": [{\n\t\t\t\t\t\t\t\t\t\"extra-properties\": {\n\t\t\t\t\t\t\t\t\t\t\"extra-property\": [{\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-name\",\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-value\": \"8330e932-2a23-4943-8606\"\n\t\t\t\t\t\t\t\t\t\t\t}, {\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-type\",\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-value\": \"service\"\n\t\t\t\t\t\t\t\t\t\t\t}, {\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-version\",\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-value\": \"1\"\n\t\t\t\t\t\t\t\t\t\t\t}, {\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-id\",\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-value\": \"4fcbc1c0-7793-46d8-8aa1-fa1c2ed9ec7b\"\n\t\t\t\t\t\t\t\t\t\t\t}, {\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-name-version-id\",\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-value\": \"5c996219-b2e2-4c76-9b43-7e8672a33c1d\"\n\t\t\t\t\t\t\t\t\t\t\t}\n\t\t\t\t\t\t\t\t\t\t]\n\t\t\t\t\t\t\t\t\t},\n\t\t\t\t\t\t\t\t\t\"service-instance\": {\n\t\t\t\t\t\t\t\t\t\t\"model-invariant-id\": \"4fcbc1c0-7793-46d8-8aa1-fa1c2ed9ec7b\",\n\t\t\t\t\t\t\t\t\t\t\"model-version-id\": \"1.0\",\n\t\t\t\t\t\t\t\t\t\t\"resource-version\": \"1485542400\",\n\t\t\t\t\t\t\t\t\t\t\"service-instance-id\": \"cf8426a6-0b53-4e3d-bfa6-4b2f4d5913a5\",\n\t\t\t\t\t\t\t\t\t\t\"service-instance-name\": \"Service_Ete_Named90e1ab3-dcd5-4877-9edb-eadfc84e32c8\"\n\t\t\t\t\t\t\t\t\t}\n\t\t\t\t\t\t\t\t}, {\n\t\t\t\t\t\t\t\t\t\"extra-properties\": {\n\t\t\t\t\t\t\t\t\t\t\"extra-property\": [{\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-name\",\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-value\": \"C15ce9e1E9144c8fB8bb..base_vlb..module-0\"\n\t\t\t\t\t\t\t\t\t\t\t}, {\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-type\",\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-value\": \"resource\"\n\t\t\t\t\t\t\t\t\t\t\t}, {\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-version\",\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-value\": \"1\"\n\t\t\t\t\t\t\t\t\t\t\t}, {\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-id\",\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-value\": \"79ee24cd-fc9a-4f14-afae-5e1dd2ab2941\"\n\t\t\t\t\t\t\t\t\t\t\t}, {\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-name-version-id\",\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-value\": \"5484cabb-1a0d-4f29-a616-094a3f643d73\"\n\t\t\t\t\t\t\t\t\t\t\t}\n\t\t\t\t\t\t\t\t\t\t]\n\t\t\t\t\t\t\t\t\t},\n\t\t\t\t\t\t\t\t\t\"model-name\": \"C15ce9e1E9144c8fB8bb..base_vlb..module-0\",\n\t\t\t\t\t\t\t\t\t\"vf-module\": {\n\t\t\t\t\t\t\t\t\t\t\"heat-stack-id\": \"Vfmodule_Ete_Named90e1ab3-dcd5-4877-9edb-eadfc84e32c8/5845f37b-6cda-4e91-8ca3-f5572d226488\",\n\t\t\t\t\t\t\t\t\t\t\"is-base-vf-module\": true,\n\t\t\t\t\t\t\t\t\t\t\"orchestration-status\": \"active\",\n\t\t\t\t\t\t\t\t\t\t\"model-invariant-id\": \"79ee24cd-fc9a-4f14-afae-5e1dd2ab2941\",\n\t\t\t\t\t\t\t\t\t\t\"model-version-id\": \"1\",\n\t\t\t\t\t\t\t\t\t\t\"resource-version\": \"1485542667\",\n\t\t\t\t\t\t\t\t\t\t\"vf-module-id\": \"b0eff878-e2e1-4947-9597-39afdd0f51dd\",\n\t\t\t\t\t\t\t\t\t\t\"vf-module-name\": \"Vfmodule_Ete_Named90e1ab3-dcd5-4877-9edb-eadfc84e32c8\"\n\t\t\t\t\t\t\t\t\t}\n\t\t\t\t\t\t\t\t}, {\n\t\t\t\t\t\t\t\t\t\"extra-properties\": {\n\t\t\t\t\t\t\t\t\t\t\"extra-property\": [{\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-name\",\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-value\": \"C15ce9e1E9144c8fB8bb..dnsscaling..module-1\"\n\t\t\t\t\t\t\t\t\t\t\t}, {\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-type\",\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-value\": \"resource\"\n\t\t\t\t\t\t\t\t\t\t\t}, {\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-version\",\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-value\": \"1\"\n\t\t\t\t\t\t\t\t\t\t\t}, {\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-id\",\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-value\": \"f32568ec-2f1c-458a-864b-0593d53d141a\"\n\t\t\t\t\t\t\t\t\t\t\t}, {\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-name-version-id\",\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-value\": \"69615025-879d-4f0d-afe3-b7d1a7eeed1f\"\n\t\t\t\t\t\t\t\t\t\t\t}\n\t\t\t\t\t\t\t\t\t\t]\n\t\t\t\t\t\t\t\t\t},\n\t\t\t\t\t\t\t\t\t\"vf-module\": {\n\t\t\t\t\t\t\t\t\t\t\"is-base-vf-module\": false,\n\t\t\t\t\t\t\t\t\t\t\"model-invariant-id\": \"f32568ec-2f1c-458a-864b-0593d53d141a\",\n\t\t\t\t\t\t\t\t\t\t\"model-version-id\": \"1.0\",\n\t\t\t\t\t\t\t\t\t\t\"resource-version\": \"1485561752\",\n\t\t\t\t\t\t\t\t\t\t\"vf-module-id\": \"dummy\",\n\t\t\t\t\t\t\t\t\t\t\"vf-module-name\": \"dummy\"\n\t\t\t\t\t\t\t\t\t}\n\t\t\t\t\t\t\t\t}, {\n\t\t\t\t\t\t\t\t\t\"extra-properties\": {\n\t\t\t\t\t\t\t\t\t\t\"extra-property\": [{\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-name\",\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-value\": \"C15ce9e1E9144c8fB8bb..dnsscaling..module-1\"\n\t\t\t\t\t\t\t\t\t\t\t}, {\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-type\",\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-value\": \"resource\"\n\t\t\t\t\t\t\t\t\t\t\t}, {\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-version\",\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-value\": \"1\"\n\t\t\t\t\t\t\t\t\t\t\t}, {\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-id\",\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-value\": \"f32568ec-2f1c-458a-864b-0593d53d141a\"\n\t\t\t\t\t\t\t\t\t\t\t}, {\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-name\": \"model.model-name-version-id\",\n\t\t\t\t\t\t\t\t\t\t\t\t\"property-value\": \"69615025-879d-4f0d-afe3-b7d1a7eeed1f\"\n\t\t\t\t\t\t\t\t\t\t\t}\n\t\t\t\t\t\t\t\t\t\t]\n\t\t\t\t\t\t\t\t\t},\n\t\t\t\t\t\t\t\t\t\"vf-module\": {\n\t\t\t\t\t\t\t\t\t\t\"heat-stack-id\": \"vDNS_Ete_Named90e1ab3-dcd5-4877-9edb-eadfc84e32c8/f447ce51-14dd-4dcd-9957-68a047c79673\",\n\t\t\t\t\t\t\t\t\t\t\"is-base-vf-module\": false,\n\t\t\t\t\t\t\t\t\t\t\"orchestration-status\": \"active\",\n\t\t\t\t\t\t\t\t\t\t\"model-invariant-id\": \"f32568ec-2f1c-458a-864b-0593d53d141a\",\n\t\t\t\t\t\t\t\t\t\t\"model-version-id\": \"1.0\",\n\t\t\t\t\t\t\t\t\t\t\"resource-version\": \"1485562712\",\n\t\t\t\t\t\t\t\t\t\t\"vf-module-id\": \"8cd79e44-1fae-48c1-a160-609f90b46749\",\n\t\t\t\t\t\t\t\t\t\t\"vf-module-name\": \"vDNS_Ete_Named90e1ab3-dcd5-4877-9edb-eadfc84e32c8\"\n\t\t\t\t\t\t\t\t\t}\n\t\t\t\t\t\t\t\t}\n\t\t\t\t\t\t\t]\n\t\t\t\t\t\t}\n\t\t\t\t\t}, {\n\t\t\t\t\t\t\"extra-properties\": {},\n\t\t\t\t\t\t\"inventory-response-items\": {\n\t\t\t\t\t\t\t\"inventory-response-item\": [{\n\t\t\t\t\t\t\t\t\t\"cloud-region\": {\n\t\t\t\t\t\t\t\t\t\t\"cloud-owner\": \"Rackspace\",\n\t\t\t\t\t\t\t\t\t\t\"cloud-region-id\": \"DFW\",\n\t\t\t\t\t\t\t\t\t\t\"cloud-region-version\": \"v1\",\n\t\t\t\t\t\t\t\t\t\t\"cloud-type\": \"SharedNode\",\n\t\t\t\t\t\t\t\t\t\t\"cloud-zone\": \"CloudZone\",\n\t\t\t\t\t\t\t\t\t\t\"owner-defined-type\": \"OwnerType\",\n\t\t\t\t\t\t\t\t\t\t\"resource-version\": \"1485465545\"\n\t\t\t\t\t\t\t\t\t},\n\t\t\t\t\t\t\t\t\t\"extra-properties\": {}\n\t\t\t\t\t\t\t\t}\n\t\t\t\t\t\t\t]\n\t\t\t\t\t\t},\n\t\t\t\t\t\t\"tenant\": {\n\t\t\t\t\t\t\t\"resource-version\": \"1485465545\",\n\t\t\t\t\t\t\t\"tenant-id\": \"1015548\",\n\t\t\t\t\t\t\t\"tenant-name\": \"1015548\"\n\t\t\t\t\t\t}\n\t\t\t\t\t}\n\t\t\t\t]\n\t\t\t},\n\t\t\t\"vserver\": {\n\t\t\t\t\"in-maint\": false,\n\t\t\t\t\"is-closed-loop-disabled\": false,\n\t\t\t\t\"prov-status\": \"ACTIVE\",\n\t\t\t\t\"resource-version\": \"1485546436\",\n\t\t\t\t\"vserver-id\": \"70f081eb-2a87-4c81-9296-4b93d7d145c6\",\n\t\t\t\t\"vserver-name\": \"vlb-lb-32c8\",\n\t\t\t\t\"vserver-name2\": \"vlb-lb-32c8\",\n\t\t\t\t\"vserver-selflink\": \"https://dfw.servers.api.rackspacecloud.com/v2/1015548/servers/70f081eb-2a87-4c81-9296-4b93d7d145c6\"\n\t\t\t}\n\t\t}\n\t]\n}";
		}
		else /*if (request.instanceFilters.instanceFilter.get(0).containsKey("generic-vnf"))*/
		{
			return "{\"inventory-response-item\": [{\"model-name\": \"service-instance\",\"generic-vnf\": {\"vnf-id\": \"de7cc3ab-0212-47df-9e64-da1c79234deb\",\"vnf-name\": \"ZRDM2MMEX39\",\"vnf-type\": \"vMME Svc Jul 14/vMME VF Jul 14 1\",\"service-id\": \"a9a77d5a-123e-4ca2-9eb9-0b015d2ee0fb\",\"orchestration-status\": \"active\",\"in-maint\": false,\"is-closed-loop-disabled\": false,\"resource-version\": \"1503082370097\",\"model-invariant-id\": \"82194af1-3c2c-485a-8f44-420e22a9eaa4\",\"model-version-id\": \"46b92144-923a-4d20-b85a-3cbd847668a9\"},\"extra-properties\": {},\"inventory-response-items\": {\"inventory-response-item\": [{\"model-name\": \"service-instance\",\"service-instance\": {\"service-instance-id\": \"37b8cdb7-94eb-468f-a0c2-4e3c3546578e\",\"service-instance-name\": \"Changed Service Instance NAME\",\"model-invariant-id\": \"82194af1-3c2c-485a-8f44-420e22a9eaa4\",\"model-version-id\": \"46b92144-923a-4d20-b85a-3cbd847668a9\",\"resource-version\": \"1503082993532\",\"orchestration-status\": \"Active\"},\"extra-properties\": {},\"inventory-response-items\": {\"inventory-response-item\": [{\"model-name\": \"pnf\",\"generic-vnf\": {\"vnf-id\": \"jimmy-test\",\"vnf-name\": \"jimmy-test-vnf\",\"vnf-type\": \"vMME Svc Jul 14/vMME VF Jul 14 1\",\"service-id\": \"a9a77d5a-123e-4ca2-9eb9-0b015d2ee0fb\",\"orchestration-status\": \"active\",\"in-maint\": false,\"is-closed-loop-disabled\": false,\"resource-version\": \"1504013830207\",\"model-invariant-id\": \"862b25a1-262a-4961-bdaa-cdc55d69785a\",\"model-version-id\": \"e9f1fa7d-c839-418a-9601-03dc0d2ad687\"},\"extra-properties\": {}},{\"model-name\": \"service-instance\",\"generic-vnf\": {\"vnf-id\": \"jimmy-test-vnf2\",\"vnf-name\": \"jimmy-test-vnf2-named\",\"vnf-type\": \"vMME Svc Jul 14/vMME VF Jul 14 1\",\"service-id\": \"a9a77d5a-123e-4ca2-9eb9-0b015d2ee0fb\",\"orchestration-status\": \"active\",\"in-maint\": false,\"is-closed-loop-disabled\": false,\"resource-version\": \"1504014833841\",\"model-invariant-id\": \"Eace933104d443b496b8.nodes.heat.vpg\",\"model-version-id\": \"46b92144-923a-4d20-b85a-3cbd847668a9\"},\"extra-properties\": {}}]}}]}}]}";
		}
	}
	
	@GET
	@Path("/v11/network/generic-vnfs/generic-vnf?vnf-name={vnfName}")
	public String getByVnfName (@PathParam("vnfName") String vnfName)
	{
		return "{ \"vnf-id\": \"5e49ca06-2972-4532-9ed4-6d071588d792\", \"vnf-name\": \"USUCP0PCOIL0110UJRT01\", \"vnf-type\": \"RT\", \"service-id\": \"d7bb0a21-66f2-4e6d-87d9-9ef3ced63ae4\", \"equipment-role\": \"UCPE\", \"orchestration-status\": \"created\", \"management-option\": \"ATT\", \"ipv4-oam-address\": \"32.40.68.35\", \"ipv4-loopback0-address\": \"32.40.64.57\", \"nm-lan-v6-address\": \"2001:1890:e00e:fffe::1345\", \"management-v6-address\": \"2001:1890:e00e:fffd::36\", \"in-maint\": false, \"is-closed-loop-disabled\": false, \"resource-version\": \"1493389458092\", \"relationship-list\": {\"relationship\":[{ \"related-to\": \"service-instance\", \"related-link\": \"/aai/v11/business/customers/customer/1610_Func_Global_20160817084727/service-subscriptions/service-subscription/uCPE-VMS/service-instances/service-instance/USUCP0PCOIL0110UJZZ01\", \"relationship-data\":[{ \"relationship-key\": \"customer.global-customer-id\", \"relationship-value\": \"1610_Func_Global_20160817084727\"},{ \"relationship-key\": \"service-subscription.service-type\", \"relationship-value\": \"uCPE-VMS\"},{ \"relationship-key\": \"service-instance.service-instance-id\", \"relationship-value\": \"USUCP0PCOIL0110UJZZ01\"} ], \"related-to-property\": [{\"property-key\": \"service-instance.service-instance-name\"}]},{ \"related-to\": \"vserver\", \"related-link\": \"/aai/v11/cloud-infrastructure/cloud-regions/cloud-region/att-aic/AAIAIC25/tenants/tenant/USUCP0PCOIL0110UJZZ01%3A%3AuCPE-VMS/vservers/vserver/3b2558f4-39d8-40e7-bfc7-30660fb52c45\", \"relationship-data\":[{ \"relationship-key\": \"cloud-region.cloud-owner\", \"relationship-value\": \"att-aic\"},{ \"relationship-key\": \"cloud-region.cloud-region-id\", \"relationship-value\": \"AAIAIC25\"},{ \"relationship-key\": \"tenant.tenant-id\", \"relationship-value\": \"USUCP0PCOIL0110UJZZ01::uCPE-VMS\"},{ \"relationship-key\": \"vserver.vserver-id\", \"relationship-value\": \"3b2558f4-39d8-40e7-bfc7-30660fb52c45\"} ], \"related-to-property\": [ {\"property-key\": \"vserver.vserver-name\",\"property-value\": \"USUCP0PCOIL0110UJZZ01-vsrx\" }]} ]}}";
	}
	
	@GET
	@Path("/v11/network/generic-vnfs/generic-vnf/{vnfId}")
	public String getByVnfId (@PathParam("vnfId") String vnfId)
	{
		return "{ \"vnf-id\": \"5e49ca06-2972-4532-9ed4-6d071588d792\", \"vnf-name\": \"USUCP0PCOIL0110UJRT01\", \"vnf-type\": \"RT\", \"service-id\": \"d7bb0a21-66f2-4e6d-87d9-9ef3ced63ae4\", \"equipment-role\": \"UCPE\", \"orchestration-status\": \"created\", \"management-option\": \"ATT\", \"ipv4-oam-address\": \"32.40.68.35\", \"ipv4-loopback0-address\": \"32.40.64.57\", \"nm-lan-v6-address\": \"2001:1890:e00e:fffe::1345\", \"management-v6-address\": \"2001:1890:e00e:fffd::36\", \"in-maint\": false, \"is-closed-loop-disabled\": false, \"resource-version\": \"1493389458092\", \"relationship-list\": {\"relationship\":[{ \"related-to\": \"service-instance\", \"related-link\": \"/aai/v11/business/customers/customer/1610_Func_Global_20160817084727/service-subscriptions/service-subscription/uCPE-VMS/service-instances/service-instance/USUCP0PCOIL0110UJZZ01\", \"relationship-data\":[{ \"relationship-key\": \"customer.global-customer-id\", \"relationship-value\": \"1610_Func_Global_20160817084727\"},{ \"relationship-key\": \"service-subscription.service-type\", \"relationship-value\": \"uCPE-VMS\"},{ \"relationship-key\": \"service-instance.service-instance-id\", \"relationship-value\": \"USUCP0PCOIL0110UJZZ01\"} ], \"related-to-property\": [{\"property-key\": \"service-instance.service-instance-name\"}]},{ \"related-to\": \"vserver\", \"related-link\": \"/aai/v11/cloud-infrastructure/cloud-regions/cloud-region/att-aic/AAIAIC25/tenants/tenant/USUCP0PCOIL0110UJZZ01%3A%3AuCPE-VMS/vservers/vserver/3b2558f4-39d8-40e7-bfc7-30660fb52c45\", \"relationship-data\":[{ \"relationship-key\": \"cloud-region.cloud-owner\", \"relationship-value\": \"att-aic\"},{ \"relationship-key\": \"cloud-region.cloud-region-id\", \"relationship-value\": \"AAIAIC25\"},{ \"relationship-key\": \"tenant.tenant-id\", \"relationship-value\": \"USUCP0PCOIL0110UJZZ01::uCPE-VMS\"},{ \"relationship-key\": \"vserver.vserver-id\", \"relationship-value\": \"3b2558f4-39d8-40e7-bfc7-30660fb52c45\"} ], \"related-to-property\": [ {\"property-key\": \"vserver.vserver-name\",\"property-value\": \"USUCP0PCOIL0110UJZZ01-vsrx\" }]} ]}}";
	}
	
	@GET
	@Path("/v11/nodes/vservers?vserver-name={vserverName}")
	public String getByVserverName (@PathParam("vserverName") String vserverName)
	{
		return "{\"vserver\": [{ \"vserver-id\": \"d0668d4f-c25e-4a1b-87c4-83845c01efd8\", \"vserver-name\": \"USMSO1SX7NJ0103UJZZ01-vjunos0\", \"vserver-name2\": \"vjunos0\", \"vserver-selflink\": \"https://aai-ext1.test.att.com:8443/aai/v7/cloud-infrastructure/cloud-regions/cloud-region/att-aic/AAIAIC25/tenants/tenant/USMSO1SX7NJ0103UJZZ01%3A%3AuCPE-VMS/vservers/vserver/d0668d4f-c25e-4a1b-87c4-83845c01efd8\", \"in-maint\": false, \"is-closed-loop-disabled\": false, \"resource-version\": \"1494001931513\", \"relationship-list\": {\"relationship\":[{ \"related-to\": \"generic-vnf\", \"related-link\": \"/aai/v11/network/generic-vnfs/generic-vnf/e1a41e99-4ede-409a-8f9d-b5e12984203a\", \"relationship-data\": [ {\"relationship-key\": \"generic-vnf.vnf-id\",\"relationship-value\": \"e1a41e99-4ede-409a-8f9d-b5e12984203a\" }], \"related-to-property\": [ {\"property-key\": \"generic-vnf.vnf-name\",\"property-value\": \"USMSO1SX7NJ0103UJSW01\" }]},{ \"related-to\": \"pserver\", \"related-link\": \"/aai/v11/cloud-infrastructure/pservers/pserver/USMSO1SX7NJ0103UJZZ01\", \"relationship-data\": [ {\"relationship-key\": \"pserver.hostname\",\"relationship-value\": \"USMSO1SX7NJ0103UJZZ01\" }], \"related-to-property\": [{\"property-key\": \"pserver.pserver-name2\"}]} ]}}]}";
	}
}
