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

public class AAINQInventoryResponseItemTest {
	private static final Logger logger = LoggerFactory.getLogger(AAINQInventoryResponseItemTest.class);
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		AAINQInventoryResponseItem aaiNQInventoryResponseItem = new AAINQInventoryResponseItem();
		aaiNQInventoryResponseItem.modelName = "service-instance";
		AAINQCloudRegion aaiNQCloudRegion = new AAINQCloudRegion(); 
		aaiNQCloudRegion.cloudOwner = "OWNER";
		aaiNQCloudRegion.cloudRegionId = "REGIONID";
		aaiNQCloudRegion.cloudRegionVersion = "2.5";
		aaiNQCloudRegion.complexName = "COMPLEXNAME";
		aaiNQCloudRegion.resourceVersion = "1485365988";
		aaiNQInventoryResponseItem.cloudRegion = aaiNQCloudRegion;
		AAINQExtraProperties aaiNQExtraProperties = new AAINQExtraProperties();
		aaiNQExtraProperties.extraProperty = new LinkedList<>();
		aaiNQExtraProperties.extraProperty.add(new AAINQExtraProperty("model.model-name", "generic-vnf"));
		aaiNQExtraProperties.extraProperty.add(new AAINQExtraProperty("model.model-type", "widget"));
		aaiNQExtraProperties.extraProperty.add(new AAINQExtraProperty("model.model-version", "1.0"));
		aaiNQExtraProperties.extraProperty.add(new AAINQExtraProperty("model.model-id", "acc6edd8-a8d4-4b93-afaa-0994068be14c"));
		aaiNQExtraProperties.extraProperty.add(new AAINQExtraProperty("model.model-name-version-id", "93a6166f-b3d5-4f06-b4ba-aed48d009ad9"));
		aaiNQInventoryResponseItem.extraProperties = aaiNQExtraProperties;
		AAINQGenericVNF aaiNQGenericVNF = new AAINQGenericVNF();
		aaiNQGenericVNF.vnfID = "dhv-test-gvnf";
		aaiNQGenericVNF.vnfName = "dhv-test-gvnf-name";
		aaiNQGenericVNF.vnfName2 = "dhv-test-gvnf-name2";
		aaiNQGenericVNF.vnfType = "SW";
		aaiNQGenericVNF.serviceId = "d7bb0a21-66f2-4e6d-87d9-9ef3ced63ae4";
		aaiNQGenericVNF.provStatus = "PREPROV";
		aaiNQGenericVNF.operationalState = "dhv-test-operational-state";
		aaiNQGenericVNF.ipv4OamAddress = "dhv-test-gvnf-ipv4-oam-address";
		aaiNQGenericVNF.ipv4Loopback0Address = "dhv-test-gvnfipv4-loopback0-address";
		aaiNQGenericVNF.inMaint = false;
		aaiNQGenericVNF.isClosedLoopDisabled = false;
		aaiNQGenericVNF.resourceVersion = "1485366450";
		aaiNQGenericVNF.encrypedAccessFlag = true;
		aaiNQGenericVNF.personaModelId = "acc6edd8-a8d4-4b93-afaa-0994068be14c";
		aaiNQGenericVNF.personaModelVersion = "1.0";
		aaiNQInventoryResponseItem.genericVNF = aaiNQGenericVNF;
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
		
		aaiNQInventoryResponseItems.inventoryResponseItems.add(serviceItem);
		aaiNQInventoryResponseItems.inventoryResponseItems.add(vfModuleItem);
		aaiNQInventoryResponseItem.items = aaiNQInventoryResponseItems;
		aaiNQInventoryResponseItem.modelName = "model-name";
		AAINQServiceInstance serviceInstance = new AAINQServiceInstance();
        serviceInstance.serviceInstanceID = "dhv-test-vhnfportal-service-instance-id";
        serviceInstance.serviceInstanceName = "dhv-test-service-instance-name1";
        serviceInstance.personaModelId = "82194af1-3c2c-485a-8f44-420e22a9eaa4";
        serviceInstance.personaModelVersion = "1.0";
        serviceInstance.serviceInstanceLocationId = "dhv-test-service-instance-location-id1";
        serviceInstance.resourceVersion = "1485366092";
		aaiNQInventoryResponseItem.serviceInstance = serviceInstance;
		AAINQTenant aaiNQTenant = new AAINQTenant();
		aaiNQTenant.tenantId = "dhv-test-tenant";
		aaiNQTenant.tenantName = "dhv-test-tenant-name";
		aaiNQTenant.resourceVersion = "1485366334";
		aaiNQInventoryResponseItem.tenant = aaiNQTenant;
		AAINQVfModule aaiNQVfModule = new AAINQVfModule();
		aaiNQVfModule.vfModuleId = "example-vf-module-id-val-49261";
		aaiNQVfModule.vfModuleName = "example-vf-module-name-val-73074";
		aaiNQVfModule.heatStackId = "example-heat-stack-id-val-86300";
		aaiNQVfModule.orchestrationStatus = "example-orchestration-status-val-56523";
		aaiNQVfModule.isBaseVfModule = true;
		aaiNQVfModule.resourceVersion = "1485366450";
		aaiNQVfModule.personaModelId = "ef86f9c5-2165-44f3-8fc3-96018b609ea5";
		aaiNQVfModule.personaModelVersion = "1.0";
		aaiNQVfModule.widgetModelId = "example-widget-model-id-val-92571";
		aaiNQVfModule.widgetModelVersion = "example-widget-model-version-val-83317";
		aaiNQVfModule.contrailServiceInstanceFqdn = "example-contrail-service-instance-fqdn-val-86796";
		aaiNQInventoryResponseItem.vfModule = aaiNQVfModule;
		AAINQVServer aaiNQVServer = new AAINQVServer();
		aaiNQVServer.vserverId = "dhv-test-vserver";
		aaiNQVServer.vserverName = "dhv-test-vserver-name";
		aaiNQVServer.vserverName2 = "dhv-test-vserver-name2";
		aaiNQVServer.provStatus = "PREPROV";
		aaiNQVServer.vserverSelflink = "dhv-test-vserver-selflink";
		aaiNQVServer.inMaint = false;
		aaiNQVServer.isClosedLoopDisabled = false;
		aaiNQVServer.resourceVersion = "1485366417";
		aaiNQInventoryResponseItem.vserver = aaiNQVServer;
		assertNotNull(aaiNQInventoryResponseItem);
		
        logger.info(Serialization.gsonPretty.toJson(aaiNQInventoryResponseItem));
	}

}
