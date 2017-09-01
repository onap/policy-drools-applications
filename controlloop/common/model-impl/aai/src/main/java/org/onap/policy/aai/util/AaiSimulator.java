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

package org.onap.policy.aai.util;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("/aai")
public class AaiSimulator {
	
	@GET
	@Path("/v8/network/generic-vnfs/generic-vnf/{vnfId}")
	public String aaiGetQuery (@PathParam("vnfID") String vnfId)
	{
		return "{\"relationship-list\": {\"relationship\":[{\"related-to-property\": [{\"property-key\": \"service-instance.service-instance-name\"}]},{\"related-to-property\": [ {\"property-key\": \"vserver.vserver-name\",\"property-value\": \"USUCP0PCOIL0110UJZZ01-vsrx\" }]} ]}}";
	}
	
	@POST
	@Path("/search/named-query")
	public String aaiPostQuery()
	{
		return "{\"inventory-response-item\":[{\"vserver\":{\"vserver-id\": \"vserver-id-Manisha-01\",\"vserver-name\": \"vserver-name-16102016-aai3255-data-11-1\",\"vserver-name2\": \"example-vserver-name2-val-68608\",\"prov-status\": \"example-prov-status-val-59118\",\"vserver-selflink\": \"example-vserver-selflink-val-10902\",\"in-maint\": true,\"is-closed-loop-disabled\": false,\"resource-version\": \"1477946963\"},\"model-name\": \"service-instance\",\"generic-vnf\": {\"vnf-id\": \"de7cc3ab-0212-47df-9e64-da1c79234deb\",\"vnf-name\": \"ZRDM2MMEX39\",\"vnf-type\": \"vMME Svc Jul 14/vMME VF Jul 14 1\",\"service-id\": \"a9a77d5a-123e-4ca2-9eb9-0b015d2ee0fb\",\"orchestration-status\": \"active\",\"in-maint\": false,\"is-closed-loop-disabled\": false,\"resource-version\": \"1503082370097\",\"model-invariant-id\": \"82194af1-3c2c-485a-8f44-420e22a9eaa4\",\"model-version-id\": \"46b92144-923a-4d20-b85a-3cbd847668a9\"},\"extra-properties\": {},\"inventory-response-items\":{\"inventory-response-item\":[{\"generic-vnf\":{\"vnf-id\": \"generic-vnf-id-Manisha-01\",\"vnf-name\": \"bpsx0001v-16102016-aai3255-data-11\",\"vnf-name2\": \"example-vnf-name2-val-8204\",\"vnf-type\": \"my-vnf-type\",\"service-id\": \"c7611ebe-c324-48f1-8085-94aef0c6ef3d\",\"regional-resource-zone\": \"example-regional-resource-zone-val-8204\",\"prov-status\": \"ACTIVE\",\"operational-state\": \"example-operational-state-val-3289\",\"license-key\": \"example-license-key-val-3289\",\"equipment-role\": \"example-equipment-role-val-3289\",\"orchestration-status\": \"example-orchestration-status-val-3289\",\"heat-stack-id\": \"example-heat-stack-id-val-3289\",\"mso-catalog-key\": \"example-mso-catalog-key-val-3289\",\"management-option\": \"example-management-option-val-8204\",\"ipv4-oam-address\": \"example-ipv4-oam-address-val-8204\",\"ipv4-loopback0-address\": \"example-ipv4-loopback0-address-val-8204\",\"nm-lan-v6-address\": \"example-nm-lan-v6-address-val-8204\",\"management-v6-address\": \"example-management-v6-address-val-8204\",\"vcpu\": 7957,\"vcpu-units\": \"example-vcpu-units-val-8204\",\"vmemory\": 168,\"vmemory-units\": \"example-vmemory-units-val-8204\",\"vdisk\": 3227,\"vdisk-units\": \"example-vdisk-units-val-8204\",\"in-maint\": false,\"is-closed-loop-disabled\": false,\"resource-version\": \"1477946966\"},\"model-name\": \"service-instance\",\"service-instance\": {\"service-instance-id\": \"37b8cdb7-94eb-468f-a0c2-4e3c3546578e\",\"service-instance-name\": \"Changed Service Instance NAME\",\"model-invariant-id\": \"82194af1-3c2c-485a-8f44-420e22a9eaa4\",\"model-version-id\": \"46b92144-923a-4d20-b85a-3cbd847668a9\",\"resource-version\": \"1503082993532\",\"orchestration-status\": \"Active\"},\"extra-properties\": {},\"inventory-response-items\":{\"inventory-response-item\":[{\"service-instance\":{\"service-instance-id\": \"service-instance-id-Manisha-01\",\"service-instance-name\": \"example-service-instance-name-val-5008-1\",\"widget-model-id\": \"example-widget-model-id-val-52958\",\"widget-model-version\": \"example-widget-model-version-val-42840\",\"bandwidth-total\": \"example-bandwidth-total-val-99587\",\"bandwidth-up-wan1\": \"example-bandwidth-up-wan1-val-73709\",\"bandwidth-down-wan1\": \"example-bandwidth-down-wan1-val-20007\",\"bandwidth-up-wan2\": \"example-bandwidth-up-wan2-val-16857\",\"bandwidth-down-wan2\": \"example-bandwidth-down-wan2-val-95839\",\"vhn-portal-url\": \"example-vhn-portal-url-val-21541\",\"operational-status\": \"example-operational-status-val-48090\",\"service-instance-location-id\": \"example-service-instance-location-id-val-9684\",\"resource-version\": \"1477946961\"},\"extra-properties\": {}},{\"model-name\": \"pnf\",\"generic-vnf\": {\"vnf-id\": \"jimmy-test\",\"vnf-name\": \"jimmy-test-vnf\",\"vnf-type\": \"vMME Svc Jul 14/vMME VF Jul 14 1\",\"service-id\": \"a9a77d5a-123e-4ca2-9eb9-0b015d2ee0fb\",\"orchestration-status\": \"active\",\"in-maint\": false,\"is-closed-loop-disabled\": false,\"resource-version\": \"1504013830207\",\"model-invariant-id\": \"862b25a1-262a-4961-bdaa-cdc55d69785a\",\"model-version-id\": \"e9f1fa7d-c839-418a-9601-03dc0d2ad687\"},\"extra-properties\": {}},{\"model-name\": \"service-instance\",\"generic-vnf\": {\"vnf-id\": \"jimmy-test-vnf2\",\"vnf-name\": \"jimmy-test-vnf2-named\",\"vnf-type\": \"vMME Svc Jul 14/vMME VF Jul 14 1\",\"service-id\": \"a9a77d5a-123e-4ca2-9eb9-0b015d2ee0fb\",\"orchestration-status\": \"active\",\"in-maint\": false,\"is-closed-loop-disabled\": false,\"resource-version\": \"1504014833841\",\"model-invariant-id\": \"82194af1-3c2c-485a-8f44-420e22a9eaa4\",\"model-version-id\": \"46b92144-923a-4d20-b85a-3cbd847668a9\"},\"extra-properties\": {}}]}}]}}]}";
	}
	
}
