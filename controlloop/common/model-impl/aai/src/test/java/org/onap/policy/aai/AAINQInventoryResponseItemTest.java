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
		aaiNQInventoryResponseItem.setModelName("service-instance");
		AAINQCloudRegion aaiNQCloudRegion = new AAINQCloudRegion(); 
		aaiNQCloudRegion.setCloudOwner("OWNER");
		aaiNQCloudRegion.setCloudRegionId("REGIONID");
		aaiNQCloudRegion.setCloudRegionVersion("2.5");
		aaiNQCloudRegion.setComplexName("COMPLEXNAME");
		aaiNQCloudRegion.setResourceVersion("1485365988");
		aaiNQInventoryResponseItem.setCloudRegion(aaiNQCloudRegion);
		AAINQExtraProperties aaiNQExtraProperties = new AAINQExtraProperties();
		aaiNQExtraProperties.setExtraProperty(new LinkedList<>());
		aaiNQExtraProperties.getExtraProperty().add(new AAINQExtraProperty("model.model-name", "generic-vnf"));
		aaiNQExtraProperties.getExtraProperty().add(new AAINQExtraProperty("model.model-type", "widget"));
		aaiNQExtraProperties.getExtraProperty().add(new AAINQExtraProperty("model.model-version", "1.0"));
		aaiNQExtraProperties.getExtraProperty().add(new AAINQExtraProperty("model.model-id", "acc6edd8-a8d4-4b93-afaa-0994068be14c"));
		aaiNQExtraProperties.getExtraProperty().add(new AAINQExtraProperty("model.model-name-version-id", "93a6166f-b3d5-4f06-b4ba-aed48d009ad9"));
		aaiNQInventoryResponseItem.setExtraProperties(aaiNQExtraProperties);
		AAINQGenericVNF aaiNQGenericVNF = new AAINQGenericVNF();
		aaiNQGenericVNF.setVnfID("dhv-test-gvnf");
		aaiNQGenericVNF.setVnfName("dhv-test-gvnf-name");
		aaiNQGenericVNF.setVnfName2("dhv-test-gvnf-name2");
		aaiNQGenericVNF.setVnfType("SW");
		aaiNQGenericVNF.setServiceId("d7bb0a21-66f2-4e6d-87d9-9ef3ced63ae4");
		aaiNQGenericVNF.setProvStatus("PREPROV");
		aaiNQGenericVNF.setOperationalState("dhv-test-operational-state");
		aaiNQGenericVNF.setIpv4OamAddress("dhv-test-gvnf-ipv4-oam-address");
		aaiNQGenericVNF.setIpv4Loopback0Address("dhv-test-gvnfipv4-loopback0-address");
		aaiNQGenericVNF.setInMaint(false);
		aaiNQGenericVNF.setIsClosedLoopDisabled(false);
		aaiNQGenericVNF.setResourceVersion("1485366450");
		aaiNQGenericVNF.setEncrypedAccessFlag(true);
		aaiNQGenericVNF.setPersonaModelId("acc6edd8-a8d4-4b93-afaa-0994068be14c");
		aaiNQGenericVNF.setPersonaModelVersion("1.0");
		aaiNQInventoryResponseItem.setGenericVNF(aaiNQGenericVNF);
		AAINQInventoryResponseItems aaiNQInventoryResponseItems = new AAINQInventoryResponseItems();
        AAINQInventoryResponseItem serviceItem = new AAINQInventoryResponseItem();
        serviceItem.setModelName("service-instance");
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
		
		aaiNQInventoryResponseItems.getInventoryResponseItems().add(serviceItem);
		aaiNQInventoryResponseItems.getInventoryResponseItems().add(vfModuleItem);
		aaiNQInventoryResponseItem.setItems(aaiNQInventoryResponseItems);
		aaiNQInventoryResponseItem.setModelName("model-name");
		AAINQServiceInstance serviceInstance = new AAINQServiceInstance();
        serviceInstance.setServiceInstanceID("dhv-test-vhnfportal-service-instance-id");
        serviceInstance.setServiceInstanceName("dhv-test-service-instance-name1");
        serviceInstance.setPersonaModelId("82194af1-3c2c-485a-8f44-420e22a9eaa4");
        serviceInstance.setPersonaModelVersion("1.0");
        serviceInstance.setServiceInstanceLocationId("dhv-test-service-instance-location-id1");
        serviceInstance.setResourceVersion("1485366092");
		aaiNQInventoryResponseItem.setServiceInstance(serviceInstance);
		AAINQTenant aaiNQTenant = new AAINQTenant();
		aaiNQTenant.setTenantId("dhv-test-tenant");
		aaiNQTenant.setTenantName("dhv-test-tenant-name");
		aaiNQTenant.setResourceVersion("1485366334");
		aaiNQInventoryResponseItem.setTenant(aaiNQTenant);
		AAINQVfModule aaiNQVfModule = new AAINQVfModule();
		aaiNQVfModule.setVfModuleId("example-vf-module-id-val-49261");
		aaiNQVfModule.setVfModuleName("example-vf-module-name-val-73074");
		aaiNQVfModule.setHeatStackId("example-heat-stack-id-val-86300");
		aaiNQVfModule.setOrchestrationStatus("example-orchestration-status-val-56523");
		aaiNQVfModule.setIsBaseVfModule(true);
		aaiNQVfModule.setResourceVersion("1485366450");
		aaiNQVfModule.setPersonaModelId("ef86f9c5-2165-44f3-8fc3-96018b609ea5");
		aaiNQVfModule.setPersonaModelVersion("1.0");
		aaiNQVfModule.setWidgetModelId("example-widget-model-id-val-92571");
		aaiNQVfModule.setWidgetModelVersion("example-widget-model-version-val-83317");
		aaiNQVfModule.setContrailServiceInstanceFqdn("example-contrail-service-instance-fqdn-val-86796");
		aaiNQInventoryResponseItem.setVfModule(aaiNQVfModule);
		AAINQVServer aaiNQVServer = new AAINQVServer();
		aaiNQVServer.setVserverId("dhv-test-vserver");
		aaiNQVServer.setVserverName("dhv-test-vserver-name");
		aaiNQVServer.setVserverName2("dhv-test-vserver-name2");
		aaiNQVServer.setProvStatus("PREPROV");
		aaiNQVServer.setVserverSelflink("dhv-test-vserver-selflink");
		aaiNQVServer.setInMaint(false);
		aaiNQVServer.setIsClosedLoopDisabled(false);
		aaiNQVServer.setResourceVersion("1485366417");
		aaiNQInventoryResponseItem.setVserver(aaiNQVServer);
		assertNotNull(aaiNQInventoryResponseItem);
		
        logger.info(Serialization.gsonPretty.toJson(aaiNQInventoryResponseItem));
	}

}
