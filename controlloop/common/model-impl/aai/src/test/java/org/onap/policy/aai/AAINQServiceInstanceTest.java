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

public class AAINQServiceInstanceTest {
	private static final Logger logger = LoggerFactory.getLogger(AAINQServiceInstanceTest.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		AAINQServiceInstance aaiNQServiceInstance = new AAINQServiceInstance();
		aaiNQServiceInstance.setServiceInstanceID("dhv-test-vhnfportal-service-instance-id");
		aaiNQServiceInstance.setServiceInstanceName("dhv-test-service-instance-name1");
		aaiNQServiceInstance.setPersonaModelId("82194af1-3c2c-485a-8f44-420e22a9eaa4");
		aaiNQServiceInstance.setPersonaModelVersion("1.0");
		aaiNQServiceInstance.setServiceInstanceLocationId("dhv-test-service-instance-location-id1");
		aaiNQServiceInstance.setResourceVersion("1485366092");
		aaiNQServiceInstance.setModelInvariantId("SomeID");
		aaiNQServiceInstance.setModelVersionId("SomeVersion");
		assertNotNull(aaiNQServiceInstance);
		assertEquals("dhv-test-vhnfportal-service-instance-id", aaiNQServiceInstance.getServiceInstanceID());
		assertEquals("dhv-test-service-instance-name1", aaiNQServiceInstance.getServiceInstanceName());
		assertEquals("82194af1-3c2c-485a-8f44-420e22a9eaa4", aaiNQServiceInstance.getPersonaModelId());
		assertEquals("1.0", aaiNQServiceInstance.getPersonaModelVersion());
		assertEquals("dhv-test-service-instance-location-id1", aaiNQServiceInstance.getServiceInstanceLocationId());
		assertEquals("1485366092", aaiNQServiceInstance.getResourceVersion());
		assertEquals("SomeID", aaiNQServiceInstance.getModelInvariantId());
		assertEquals("SomeVersion", aaiNQServiceInstance.getModelVersionId());
	    logger.info(Serialization.gsonPretty.toJson(aaiNQServiceInstance));		
	}

}
