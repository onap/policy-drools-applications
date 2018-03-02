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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.aai.util.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AAINQVfModuleTest {
	private static final Logger logger = LoggerFactory.getLogger(AAINQVfModuleTest.class);


	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
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
		aaiNQVfModule.setModelInvariantId("SomeId");
		aaiNQVfModule.setModelVersionId("SomeVersion");
		assertNotNull(aaiNQVfModule);
		assertEquals("example-vf-module-id-val-49261", aaiNQVfModule.getVfModuleId());
		assertEquals("example-vf-module-name-val-73074", aaiNQVfModule.getVfModuleName());
		assertEquals("example-heat-stack-id-val-86300", aaiNQVfModule.getHeatStackId());
		assertEquals("example-orchestration-status-val-56523", aaiNQVfModule.getOrchestrationStatus());
		assertEquals(true, aaiNQVfModule.getIsBaseVfModule());
		assertEquals("1485366450", aaiNQVfModule.getResourceVersion());
		assertEquals("ef86f9c5-2165-44f3-8fc3-96018b609ea5", aaiNQVfModule.getPersonaModelId());
		assertEquals("1.0", aaiNQVfModule.getPersonaModelVersion());
		assertEquals("example-widget-model-id-val-92571", aaiNQVfModule.getWidgetModelId());
		assertEquals("example-widget-model-version-val-83317", aaiNQVfModule.getWidgetModelVersion());
		assertEquals("example-contrail-service-instance-fqdn-val-86796", aaiNQVfModule.getContrailServiceInstanceFqdn());
		assertEquals("SomeId", aaiNQVfModule.getModelInvariantId());
		assertEquals("SomeVersion", aaiNQVfModule.getModelVersionId());
		logger.info(Serialization.gsonPretty.toJson(aaiNQVfModule));		
	}

}
