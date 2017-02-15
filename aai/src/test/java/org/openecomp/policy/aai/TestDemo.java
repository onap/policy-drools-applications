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

package org.openecomp.policy.aai;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;
import org.openecomp.policy.aai.AAINQF199.AAINQF199CloudRegion;
import org.openecomp.policy.aai.AAINQF199.AAINQF199ExtraProperties;
import org.openecomp.policy.aai.AAINQF199.AAINQF199ExtraProperty;
import org.openecomp.policy.aai.AAINQF199.AAINQF199GenericVNF;
import org.openecomp.policy.aai.AAINQF199.AAINQF199VServer;
import org.openecomp.policy.aai.AAINQF199.AAINQF199VfModule;
import org.openecomp.policy.aai.AAINQF199.AAINQF199InstanceFilters;
import org.openecomp.policy.aai.AAINQF199.AAINQF199InventoryResponseItem;
import org.openecomp.policy.aai.AAINQF199.AAINQF199InventoryResponseItems;
import org.openecomp.policy.aai.AAINQF199.AAINQF199Manager;
import org.openecomp.policy.aai.AAINQF199.AAINQF199NamedQuery;
import org.openecomp.policy.aai.AAINQF199.AAINQF199QueryParameters;
import org.openecomp.policy.aai.AAINQF199.AAINQF199Response;
import org.openecomp.policy.aai.AAINQF199.AAINQF199ServiceInstance;
import org.openecomp.policy.aai.AAINQF199.AAINQF199Tenant;
import org.openecomp.policy.aai.AAINQF199.AAINQF199Request;
import org.openecomp.policy.aai.util.Serialization;

public class TestDemo {

	@Test
	public void test() {
		
		//
		// Test AAINQF199Request
		//
		AAINQF199Request request = new AAINQF199Request();
		request.queryParameters = new AAINQF199QueryParameters();
		request.queryParameters.namedQuery = new AAINQF199NamedQuery();
		request.queryParameters.namedQuery.namedQueryUUID = UUID.fromString("f199cb88-5e69-4b1f-93e0-6f257877d066");
		request.instanceFilters = new AAINQF199InstanceFilters();
		Map<String, Map<String, String>> vserver = new HashMap<String, Map<String, String>>();
		Map<String, String> values = new HashMap<String, String>();
		values.put("vserver-name", "dfw1lb01lb01");
		vserver.put("vserver", values);
		request.instanceFilters.instanceFilter.add(vserver);
		
		String body = Serialization.gsonPretty.toJson(request);
		
		System.out.println(body);
		
		System.out.println();
		
		
/*		
		AAINQF199InventoryResponseItem serviceItem = new AAINQF199InventoryResponseItem();
		serviceItem.serviceInstance = new AAINQF199ServiceInstance();
		serviceItem.serviceInstance.serviceInstanceID = "service-instance-id-Manisha-01";
				
		AAINQF199InventoryResponseItem genericVNFItem = new AAINQF199InventoryResponseItem();
		genericVNFItem.genericVNF = new AAINQF199GenericVNF();
		genericVNFItem.genericVNF.vnfID = "generic-vnf-id-Manisha-01";
		genericVNFItem.items = new AAINQF199InventoryResponseItems();
		genericVNFItem.items.inventoryResponseItems = new LinkedList<AAINQF199InventoryResponseItem>();
		genericVNFItem.items.inventoryResponseItems.add(serviceItem);
		
		AAINQF199InventoryResponseItem vserverItem = new AAINQF199InventoryResponseItem();
		vserverItem.vserver = new AAINQF199VServer();
		vserverItem.vserver.vserverId = "vserver-id-Manisha-01";
		vserverItem.vserver.vserverName = "vserver-name-16102016-aai3255-data-11-1";
		vserverItem.items = new AAINQF199InventoryResponseItems();
		vserverItem.items.inventoryResponseItems = new LinkedList<AAINQF199InventoryResponseItem>();
		vserverItem.items.inventoryResponseItems.add(genericVNFItem);
*/	
		
		
		//
		// Test AAINQF199Response
		//
		AAINQF199InventoryResponseItem serviceItem = new AAINQF199InventoryResponseItem();
		serviceItem.modelName = "service-instance";
		serviceItem.serviceInstance = new AAINQF199ServiceInstance();
		serviceItem.serviceInstance.serviceInstanceID = "dhv-test-vhnfportal-service-instance-id";
		serviceItem.serviceInstance.serviceInstanceName = "dhv-test-service-instance-name1";
		serviceItem.serviceInstance.personaModelId = "82194af1-3c2c-485a-8f44-420e22a9eaa4";
		serviceItem.serviceInstance.personaModelVersion = "1.0";
		serviceItem.serviceInstance.serviceInstanceLocationId = "dhv-test-service-instance-location-id1";
		serviceItem.serviceInstance.resourceVersion = "1485366092";
		serviceItem.extraProperties = new AAINQF199ExtraProperties();
		serviceItem.extraProperties.extraProperty.add(new AAINQF199ExtraProperty("model.model-name", "service-instance"));
		serviceItem.extraProperties.extraProperty.add(new AAINQF199ExtraProperty("model.model-type", "widget"));
		serviceItem.extraProperties.extraProperty.add(new AAINQF199ExtraProperty("model.model-version", "1.0"));
		serviceItem.extraProperties.extraProperty.add(new AAINQF199ExtraProperty("model.model-id", "82194af1-3c2c-485a-8f44-420e22a9eaa4"));
		serviceItem.extraProperties.extraProperty.add(new AAINQF199ExtraProperty("model.model-name", "46b92144-923a-4d20-b85a-3cbd847668a9"));
		
		
		AAINQF199InventoryResponseItem vfModuleItem = new AAINQF199InventoryResponseItem();
		vfModuleItem.modelName = "vf-module";
		vfModuleItem.vfModule = new AAINQF199VfModule();
		vfModuleItem.vfModule.vfModuleId = "example-vf-module-id-val-49261";
		vfModuleItem.vfModule.vfModuleName = "example-vf-module-name-val-73074";
		vfModuleItem.vfModule.heatStackId = "example-heat-stack-id-val-86300";
		vfModuleItem.vfModule.orchestrationStatus = "example-orchestration-status-val-56523";
		vfModuleItem.vfModule.isBaseVfModule = true;
		vfModuleItem.vfModule.resourceVersion = "1485366450";
		vfModuleItem.vfModule.personaModelId = "ef86f9c5-2165-44f3-8fc3-96018b609ea5";
		vfModuleItem.vfModule.personaModelVersion = "1.0";
		vfModuleItem.vfModule.widgetModelId = "example-widget-model-id-val-92571";
		vfModuleItem.vfModule.widgetModelVersion = "example-widget-model-version-val-83317";
		vfModuleItem.vfModule.contrailServiceInstanceFqdn = "example-contrail-service-instance-fqdn-val-86796";
		vfModuleItem.extraProperties = new AAINQF199ExtraProperties();
		vfModuleItem.extraProperties.extraProperty.add(new AAINQF199ExtraProperty("model.model-name", "vf-module"));
		vfModuleItem.extraProperties.extraProperty.add(new AAINQF199ExtraProperty("model.model-type", "widget"));
		vfModuleItem.extraProperties.extraProperty.add(new AAINQF199ExtraProperty("model.model-version", "1.0"));
		vfModuleItem.extraProperties.extraProperty.add(new AAINQF199ExtraProperty("model.model-id", "ef86f9c5-2165-44f3-8fc3-96018b609ea5"));
		vfModuleItem.extraProperties.extraProperty.add(new AAINQF199ExtraProperty("model.model-name", "c00563ae-812b-4e62-8330-7c4d0f47088a"));
		
		
		AAINQF199InventoryResponseItem genericVNFItem = new AAINQF199InventoryResponseItem();
		genericVNFItem.modelName = "generic-vnf";
		genericVNFItem.genericVNF = new AAINQF199GenericVNF();
		genericVNFItem.genericVNF.vnfID = "dhv-test-gvnf";
		genericVNFItem.genericVNF.vnfName = "dhv-test-gvnf-name";
		genericVNFItem.genericVNF.vnfName2 = "dhv-test-gvnf-name2";
		genericVNFItem.genericVNF.vnfType = "SW";
		genericVNFItem.genericVNF.serviceId = "d7bb0a21-66f2-4e6d-87d9-9ef3ced63ae4";
		genericVNFItem.genericVNF.provStatus = "PREPROV";
		genericVNFItem.genericVNF.operationalState = "dhv-test-operational-state";
		genericVNFItem.genericVNF.ipv4OamAddress = "dhv-test-gvnf-ipv4-oam-address";
		genericVNFItem.genericVNF.ipv4Loopback0Address = "dhv-test-gvnfipv4-loopback0-address";
		genericVNFItem.genericVNF.inMaint = false;
		genericVNFItem.genericVNF.isClosedLoopDisabled = false;
		genericVNFItem.genericVNF.resourceVersion = "1485366450";
		genericVNFItem.genericVNF.encrypedAccessFlag = true;
		genericVNFItem.genericVNF.personaModelId = "acc6edd8-a8d4-4b93-afaa-0994068be14c";
		genericVNFItem.genericVNF.personaModelVersion = "1.0";
		genericVNFItem.extraProperties = new AAINQF199ExtraProperties();
		genericVNFItem.extraProperties.extraProperty = new LinkedList<AAINQF199ExtraProperty>();
		genericVNFItem.extraProperties.extraProperty.add(new AAINQF199ExtraProperty("model.model-name", "generic-vnf"));
		genericVNFItem.extraProperties.extraProperty.add(new AAINQF199ExtraProperty("model.model-type", "widget"));
		genericVNFItem.extraProperties.extraProperty.add(new AAINQF199ExtraProperty("model.model-version", "1.0"));
		genericVNFItem.extraProperties.extraProperty.add(new AAINQF199ExtraProperty("model.model-id", "acc6edd8-a8d4-4b93-afaa-0994068be14c"));
		genericVNFItem.extraProperties.extraProperty.add(new AAINQF199ExtraProperty("model.model-name-version-id", "93a6166f-b3d5-4f06-b4ba-aed48d009ad9"));
		genericVNFItem.items = new AAINQF199InventoryResponseItems();
		genericVNFItem.items.inventoryResponseItems = new LinkedList<AAINQF199InventoryResponseItem>();
		genericVNFItem.items.inventoryResponseItems.add(serviceItem);
		genericVNFItem.items.inventoryResponseItems.add(vfModuleItem);
		
		
		AAINQF199InventoryResponseItem cloudItem = new AAINQF199InventoryResponseItem();
		cloudItem.cloudRegion = new AAINQF199CloudRegion();
		cloudItem.cloudRegion.cloudOwner = "OWNER";
		cloudItem.cloudRegion.cloudRegionId = "REGIONID";
		cloudItem.cloudRegion.cloudRegionVersion = "2.5";
		cloudItem.cloudRegion.complexName = "COMPLEXNAME";
		cloudItem.cloudRegion.resourceVersion = "1485365988";
		
		
		AAINQF199InventoryResponseItem tenantItem = new AAINQF199InventoryResponseItem();
		tenantItem.tenant = new AAINQF199Tenant();
		tenantItem.tenant.tenantId = "dhv-test-tenant";
		tenantItem.tenant.tenantName = "dhv-test-tenant-name";
		tenantItem.tenant.resourceVersion = "1485366334";
		tenantItem.items = new AAINQF199InventoryResponseItems();
		tenantItem.items.inventoryResponseItems = new LinkedList<AAINQF199InventoryResponseItem>();
		tenantItem.items.inventoryResponseItems.add(cloudItem);
		
		
		AAINQF199InventoryResponseItem vserverItem = new AAINQF199InventoryResponseItem();
		vserverItem.vserver = new AAINQF199VServer();
		vserverItem.vserver.vserverId = "dhv-test-vserver";
		vserverItem.vserver.vserverName = "dhv-test-vserver-name";
		vserverItem.vserver.vserverName2 = "dhv-test-vserver-name2";
		vserverItem.vserver.provStatus = "PREPROV";
		vserverItem.vserver.vserverSelflink = "dhv-test-vserver-selflink";
		vserverItem.vserver.inMaint = false;
		vserverItem.vserver.isClosedLoopDisabled = false;
		vserverItem.vserver.resourceVersion = "1485366417";
		vserverItem.items = new AAINQF199InventoryResponseItems();
		vserverItem.items.inventoryResponseItems = new LinkedList<AAINQF199InventoryResponseItem>();
		vserverItem.items.inventoryResponseItems.add(genericVNFItem);
		vserverItem.items.inventoryResponseItems.add(tenantItem);
		
		
		AAINQF199Response aaiResponse = new AAINQF199Response();
		aaiResponse.inventoryResponseItems.add(vserverItem);
		
		body = Serialization.gsonPretty.toJson(aaiResponse);
		
		System.out.println(body);
		
		
	}
	
	@Ignore
	@Test
	public void testHttp() {
		AAINQF199Request request = new AAINQF199Request();
		request.queryParameters = new AAINQF199QueryParameters();
		request.queryParameters.namedQuery = new AAINQF199NamedQuery();
		request.queryParameters.namedQuery.namedQueryUUID = UUID.fromString("f199cb88-5e69-4b1f-93e0-6f257877d066");
		request.instanceFilters = new AAINQF199InstanceFilters();
		Map<String, Map<String, String>> vserver = new HashMap<String, Map<String, String>>();
		Map<String, String> values = new HashMap<String, String>();
		values.put("vserver-name", "dfw1lb01lb01");
		vserver.put("vserver", values);
		request.instanceFilters.instanceFilter.add(vserver);
		
		String body = Serialization.gsonPretty.toJson(request);
		
		System.out.println(body);
		
		AAINQF199Response response = AAINQF199Manager.postQuery("http://localhost:8080/TestREST/Test", "POLICY", "POLICY", request, UUID.randomUUID());
		
		body = Serialization.gsonPretty.toJson(response);
		
		System.out.println(body);
		
	}
	
}
