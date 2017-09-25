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

import static org.junit.Assert.*;

import java.util.LinkedList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.aai.util.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AAINQInventoryResponseItemsTest {
	private static final Logger logger = LoggerFactory.getLogger(AAINQInventoryResponseItemsTest.class);
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		AAINQInventoryResponseItems aaiNQInventoryResponseItems = new AAINQInventoryResponseItems();
        AAINQInventoryResponseItem serviceItem = new AAINQInventoryResponseItem();
        serviceItem.modelName = "service-instance";
        serviceItem.serviceInstance = new AAINQServiceInstance();
        serviceItem.serviceInstance.serviceInstanceID = "dhv-test-vhnfportal-service-instance-id";
        serviceItem.serviceInstance.serviceInstanceName = "dhv-test-service-instance-name1";
        serviceItem.serviceInstance.personaModelId = "82194af1-3c2c-485a-8f44-420e22a9eaa4";
        serviceItem.serviceInstance.personaModelVersion = "1.0";
        serviceItem.serviceInstance.serviceInstanceLocationId = "dhv-test-service-instance-location-id1";
        serviceItem.serviceInstance.resourceVersion = "1485366092";
        serviceItem.extraProperties = new AAINQExtraProperties();
        serviceItem.extraProperties.extraProperty.add(new AAINQExtraProperty("model.model-name", "service-instance"));
        serviceItem.extraProperties.extraProperty.add(new AAINQExtraProperty("model.model-type", "widget"));
        serviceItem.extraProperties.extraProperty.add(new AAINQExtraProperty("model.model-version", "1.0"));
        serviceItem.extraProperties.extraProperty.add(new AAINQExtraProperty("model.model-id", "82194af1-3c2c-485a-8f44-420e22a9eaa4"));
        serviceItem.extraProperties.extraProperty.add(new AAINQExtraProperty("model.model-name", "46b92144-923a-4d20-b85a-3cbd847668a9"));

	    AAINQInventoryResponseItem vfModuleItem = new AAINQInventoryResponseItem();
	    vfModuleItem.modelName = "vf-module";
	    vfModuleItem.vfModule = new AAINQVfModule();
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
	    vfModuleItem.extraProperties = new AAINQExtraProperties();
	    vfModuleItem.extraProperties.extraProperty.add(new AAINQExtraProperty("model.model-name", "vf-module"));
	    vfModuleItem.extraProperties.extraProperty.add(new AAINQExtraProperty("model.model-type", "widget"));
	    vfModuleItem.extraProperties.extraProperty.add(new AAINQExtraProperty("model.model-version", "1.0"));
	    vfModuleItem.extraProperties.extraProperty.add(new AAINQExtraProperty("model.model-id", "ef86f9c5-2165-44f3-8fc3-96018b609ea5"));
	    vfModuleItem.extraProperties.extraProperty.add(new AAINQExtraProperty("model.model-name", "c00563ae-812b-4e62-8330-7c4d0f47088a"));
	    
	    AAINQInventoryResponseItem genericVNFItem = new AAINQInventoryResponseItem();
	    genericVNFItem.modelName = "generic-vnf";
	    genericVNFItem.genericVNF = new AAINQGenericVNF();
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
	    genericVNFItem.extraProperties = new AAINQExtraProperties();
	    genericVNFItem.extraProperties.extraProperty = new LinkedList<>();
	    genericVNFItem.extraProperties.extraProperty.add(new AAINQExtraProperty("model.model-name", "generic-vnf"));
	    genericVNFItem.extraProperties.extraProperty.add(new AAINQExtraProperty("model.model-type", "widget"));
	    genericVNFItem.extraProperties.extraProperty.add(new AAINQExtraProperty("model.model-version", "1.0"));
	    genericVNFItem.extraProperties.extraProperty.add(new AAINQExtraProperty("model.model-id", "acc6edd8-a8d4-4b93-afaa-0994068be14c"));
	    genericVNFItem.extraProperties.extraProperty.add(new AAINQExtraProperty("model.model-name-version-id", "93a6166f-b3d5-4f06-b4ba-aed48d009ad9"));
	    genericVNFItem.items = new AAINQInventoryResponseItems();
	    genericVNFItem.items.inventoryResponseItems = new LinkedList<>();
	    genericVNFItem.items.inventoryResponseItems.add(serviceItem);
	    genericVNFItem.items.inventoryResponseItems.add(vfModuleItem);

	    AAINQInventoryResponseItem cloudItem = new AAINQInventoryResponseItem();
	    cloudItem.cloudRegion = new AAINQCloudRegion();
	    cloudItem.cloudRegion.cloudOwner = "OWNER";
	    cloudItem.cloudRegion.cloudRegionId = "REGIONID";
	    cloudItem.cloudRegion.cloudRegionVersion = "2.5";
	    cloudItem.cloudRegion.complexName = "COMPLEXNAME";
	    cloudItem.cloudRegion.resourceVersion = "1485365988";

	    AAINQInventoryResponseItem tenantItem = new AAINQInventoryResponseItem();
	    tenantItem.tenant = new AAINQTenant();
	    tenantItem.tenant.tenantId = "dhv-test-tenant";
	    tenantItem.tenant.tenantName = "dhv-test-tenant-name";
	    tenantItem.tenant.resourceVersion = "1485366334";
	    tenantItem.items = new AAINQInventoryResponseItems();
	    tenantItem.items.inventoryResponseItems = new LinkedList<>();
	    tenantItem.items.inventoryResponseItems.add(cloudItem);
	    AAINQInventoryResponseItem vserverItem = new AAINQInventoryResponseItem();
	    vserverItem.vserver = new AAINQVServer();
	    vserverItem.vserver.vserverId = "dhv-test-vserver";
	    vserverItem.vserver.vserverName = "dhv-test-vserver-name";
	    vserverItem.vserver.vserverName2 = "dhv-test-vserver-name2";
	    vserverItem.vserver.provStatus = "PREPROV";
	    vserverItem.vserver.vserverSelflink = "dhv-test-vserver-selflink";
	    vserverItem.vserver.inMaint = false;
	    vserverItem.vserver.isClosedLoopDisabled = false;
	    vserverItem.vserver.resourceVersion = "1485366417";
	    vserverItem.items = new AAINQInventoryResponseItems();
	    vserverItem.items.inventoryResponseItems = new LinkedList<>();
	    vserverItem.items.inventoryResponseItems.add(genericVNFItem);
	    vserverItem.items.inventoryResponseItems.add(tenantItem);	
	    aaiNQInventoryResponseItems.inventoryResponseItems.add(vserverItem);
	    assertNotNull(aaiNQInventoryResponseItems);
	    logger.info(Serialization.gsonPretty.toJson(aaiNQInventoryResponseItems));
	}

}
