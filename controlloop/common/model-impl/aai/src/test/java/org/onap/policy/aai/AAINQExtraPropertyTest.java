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

public class AAINQExtraPropertyTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		AAINQExtraProperty aaiNQExtraProperty = new AAINQExtraProperty();
		aaiNQExtraProperty.setPropertyName("model.model-name"); 
		aaiNQExtraProperty.setPropertyValue("service-instance");		
		assertNotNull(aaiNQExtraProperty); 

		assertEquals("model.model-name", aaiNQExtraProperty.getPropertyName()); 
		assertEquals("service-instance", aaiNQExtraProperty.getPropertyValue());		
	}
}
