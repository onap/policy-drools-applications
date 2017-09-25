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

public class AAINQGenericVNFTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		AAINQGenericVNF aaiNQGenericVNF = new AAINQGenericVNF();
		aaiNQGenericVNF.encrypedAccessFlag = true; 
		aaiNQGenericVNF.inMaint = false; 
		aaiNQGenericVNF.ipv4Loopback0Address = "aa"; 
		aaiNQGenericVNF.ipv4OamAddress = "oamAddress"; 
		aaiNQGenericVNF.isClosedLoopDisabled = false; 
		aaiNQGenericVNF.modelInvariantId = "653d2caa-7e47-4614-95b3-26c8d82755b8"; 
		aaiNQGenericVNF.modelVersionId = "98f410f6-4c63-447b-97d2-42508437cec0"; 
		aaiNQGenericVNF.operationalState = "active";
		aaiNQGenericVNF.personaModelId = "653d2caa-7e47-4614-95b3-26c8d82755b8";
		aaiNQGenericVNF.personaModelVersion = "98f410f6-4c63-447b-97d2-42508437cec0";
		aaiNQGenericVNF.provStatus = "complete";
		aaiNQGenericVNF.resourceVersion = "1505056714553";
		aaiNQGenericVNF.serviceId = "e8cb8968-5411-478b-906a-f28747de72cd";
		aaiNQGenericVNF.vnfID = "ed8b2bce-6b27-4089-992c-4a2c66024bcd"; 
		aaiNQGenericVNF.vnfName = "vCPEInfraVNF14a";
		aaiNQGenericVNF.vnfName2 = "malumabb12";
		aaiNQGenericVNF.vnfType = "vCPEInfraService10/vCPEInfraService10 0";
        assertNotNull(aaiNQGenericVNF); 
	}

}
