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
        serviceItem.setModelName("service-instance");
        assertEquals("service-instance", serviceItem.getModelName());

        serviceItem.setServiceInstance(new AAINQServiceInstance());
        serviceItem.getServiceInstance().setServiceInstanceID("dhv-test-vhnfportal-service-instance-id");
        serviceItem.getServiceInstance().setServiceInstanceName("dhv-test-service-instance-name1");
        serviceItem.getServiceInstance().setPersonaModelId("82194af1-3c2c-485a-8f44-420e22a9eaa4");
        serviceItem.getServiceInstance().setPersonaModelVersion("1.0");
        serviceItem.getServiceInstance().setServiceInstanceLocationId("dhv-test-service-instance-location-id1");
        serviceItem.getServiceInstance().setResourceVersion("1485366092");
        serviceItem.setExtraProperties(new AAINQExtraProperties());
        serviceItem.getExtraProperties().getExtraProperty().add(new AAINQExtraProperty("model.model-name", "service-instance"));
        serviceItem.getExtraProperties().getExtraProperty().add(new AAINQExtraProperty("model.model-type", "widget"));
        serviceItem.getExtraProperties().getExtraProperty().add(new AAINQExtraProperty("model.model-version", "1.0"));
        serviceItem.getExtraProperties().getExtraProperty().add(new AAINQExtraProperty("model.model-id", "82194af1-3c2c-485a-8f44-420e22a9eaa4"));
        serviceItem.getExtraProperties().getExtraProperty().add(new AAINQExtraProperty("model.model-name", "46b92144-923a-4d20-b85a-3cbd847668a9"));

	    AAINQInventoryResponseItem vfModuleItem = new AAINQInventoryResponseItem();
	    vfModuleItem.setModelName("vf-module");
	    vfModuleItem.setVfModule(new AAINQVfModule());
	    vfModuleItem.getVfModule().setVfModuleId("example-vf-module-id-val-49261");
	    vfModuleItem.getVfModule().setVfModuleName("example-vf-module-name-val-73074");
	    vfModuleItem.getVfModule().setHeatStackId("example-heat-stack-id-val-86300");
	    vfModuleItem.getVfModule().setOrchestrationStatus("example-orchestration-status-val-56523");
	    vfModuleItem.getVfModule().setIsBaseVfModule(true);
	    vfModuleItem.getVfModule().setResourceVersion("1485366450");
	    vfModuleItem.getVfModule().setPersonaModelId("ef86f9c5-2165-44f3-8fc3-96018b609ea5");
	    vfModuleItem.getVfModule().setPersonaModelVersion("1.0");
	    vfModuleItem.getVfModule().setWidgetModelId("example-widget-model-id-val-92571");
	    vfModuleItem.getVfModule().setWidgetModelVersion("example-widget-model-version-val-83317");
	    vfModuleItem.getVfModule().setContrailServiceInstanceFqdn("example-contrail-service-instance-fqdn-val-86796");
	    vfModuleItem.setExtraProperties(new AAINQExtraProperties());
	    vfModuleItem.getExtraProperties().getExtraProperty().add(new AAINQExtraProperty("model.model-name", "vf-module"));
	    vfModuleItem.getExtraProperties().getExtraProperty().add(new AAINQExtraProperty("model.model-type", "widget"));
	    vfModuleItem.getExtraProperties().getExtraProperty().add(new AAINQExtraProperty("model.model-version", "1.0"));
	    vfModuleItem.getExtraProperties().getExtraProperty().add(new AAINQExtraProperty("model.model-id", "ef86f9c5-2165-44f3-8fc3-96018b609ea5"));
	    vfModuleItem.getExtraProperties().getExtraProperty().add(new AAINQExtraProperty("model.model-name", "c00563ae-812b-4e62-8330-7c4d0f47088a"));
	    
	    AAINQInventoryResponseItem genericVNFItem = new AAINQInventoryResponseItem();
	    genericVNFItem.setModelName("generic-vnf");
	    genericVNFItem.setGenericVNF(new AAINQGenericVNF());
	    genericVNFItem.getGenericVNF().setVnfID("dhv-test-gvnf");
	    genericVNFItem.getGenericVNF().setVnfName("dhv-test-gvnf-name");
	    genericVNFItem.getGenericVNF().setVnfName2("dhv-test-gvnf-name2");
	    genericVNFItem.getGenericVNF().setVnfType("SW");
	    genericVNFItem.getGenericVNF().setServiceId("d7bb0a21-66f2-4e6d-87d9-9ef3ced63ae4");
	    genericVNFItem.getGenericVNF().setProvStatus("PREPROV");
	    genericVNFItem.getGenericVNF().setOperationalState("dhv-test-operational-state");
	    genericVNFItem.getGenericVNF().setIpv4OamAddress("dhv-test-gvnf-ipv4-oam-address");
	    genericVNFItem.getGenericVNF().setIpv4Loopback0Address("dhv-test-gvnfipv4-loopback0-address");
	    genericVNFItem.getGenericVNF().setInMaint(false);
	    genericVNFItem.getGenericVNF().setIsClosedLoopDisabled(false);
	    genericVNFItem.getGenericVNF().setResourceVersion("1485366450");
	    genericVNFItem.getGenericVNF().setEncrypedAccessFlag(true);
	    genericVNFItem.getGenericVNF().setPersonaModelId("acc6edd8-a8d4-4b93-afaa-0994068be14c");
	    genericVNFItem.getGenericVNF().setPersonaModelVersion("1.0");
	    genericVNFItem.setExtraProperties(new AAINQExtraProperties());
	    genericVNFItem.getExtraProperties().setExtraProperty(new LinkedList<>());
	    genericVNFItem.getExtraProperties().getExtraProperty().add(new AAINQExtraProperty("model.model-name", "generic-vnf"));
	    genericVNFItem.getExtraProperties().getExtraProperty().add(new AAINQExtraProperty("model.model-type", "widget"));
	    genericVNFItem.getExtraProperties().getExtraProperty().add(new AAINQExtraProperty("model.model-version", "1.0"));
	    genericVNFItem.getExtraProperties().getExtraProperty().add(new AAINQExtraProperty("model.model-id", "acc6edd8-a8d4-4b93-afaa-0994068be14c"));
	    genericVNFItem.getExtraProperties().getExtraProperty().add(new AAINQExtraProperty("model.model-name-version-id", "93a6166f-b3d5-4f06-b4ba-aed48d009ad9"));
	    genericVNFItem.setItems(new AAINQInventoryResponseItems());
	    genericVNFItem.getItems().setInventoryResponseItems(new LinkedList<>());
	    genericVNFItem.getItems().getInventoryResponseItems().add(serviceItem);
	    genericVNFItem.getItems().getInventoryResponseItems().add(vfModuleItem);

	    AAINQInventoryResponseItem cloudItem = new AAINQInventoryResponseItem();
	    cloudItem.setCloudRegion(new AAINQCloudRegion());
	    cloudItem.getCloudRegion().setCloudOwner("OWNER");
	    cloudItem.getCloudRegion().setCloudRegionId("REGIONID");
	    cloudItem.getCloudRegion().setCloudRegionVersion("2.5");
	    cloudItem.getCloudRegion().setComplexName("COMPLEXNAME");
	    cloudItem.getCloudRegion().setResourceVersion("1485365988");

	    AAINQInventoryResponseItem tenantItem = new AAINQInventoryResponseItem();
	    tenantItem.setTenant(new AAINQTenant());
	    tenantItem.getTenant().setTenantId("dhv-test-tenant");
	    tenantItem.getTenant().setTenantName("dhv-test-tenant-name");
	    tenantItem.getTenant().setResourceVersion("1485366334");
	    tenantItem.setItems(new AAINQInventoryResponseItems());
	    tenantItem.getItems().setInventoryResponseItems(new LinkedList<>());
	    tenantItem.getItems().getInventoryResponseItems().add(cloudItem);
	    AAINQInventoryResponseItem vserverItem = new AAINQInventoryResponseItem();
	    vserverItem.setVserver(new AAINQVServer());
	    vserverItem.getVserver().setVserverId("dhv-test-vserver");
	    vserverItem.getVserver().setVserverName("dhv-test-vserver-name");
	    vserverItem.getVserver().setVserverName2("dhv-test-vserver-name2");
	    vserverItem.getVserver().setProvStatus("PREPROV");
	    vserverItem.getVserver().setVserverSelflink("dhv-test-vserver-selflink");
	    vserverItem.getVserver().setInMaint(false);
	    vserverItem.getVserver().setIsClosedLoopDisabled(false);
	    vserverItem.getVserver().setResourceVersion("1485366417");
	    vserverItem.setItems(new AAINQInventoryResponseItems());
	    vserverItem.getItems().setInventoryResponseItems(new LinkedList<>());
	    vserverItem.getItems().getInventoryResponseItems().add(genericVNFItem);
	    vserverItem.getItems().getInventoryResponseItems().add(tenantItem);	
	    aaiNQInventoryResponseItems.getInventoryResponseItems().add(vserverItem);
	    assertNotNull(aaiNQInventoryResponseItems);
	    logger.info(Serialization.gsonPretty.toJson(aaiNQInventoryResponseItems));
	}

}
