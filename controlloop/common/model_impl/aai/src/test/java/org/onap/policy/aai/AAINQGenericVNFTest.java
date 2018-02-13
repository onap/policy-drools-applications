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
		aaiNQGenericVNF.setEncrypedAccessFlag(true); 
		aaiNQGenericVNF.setInMaint(false); 
		aaiNQGenericVNF.setIpv4Loopback0Address("aa"); 
		aaiNQGenericVNF.setIpv4OamAddress("oamAddress"); 
		aaiNQGenericVNF.setIsClosedLoopDisabled(false); 
		aaiNQGenericVNF.setModelInvariantId("653d2caa-7e47-4614-95b3-26c8d82755b8"); 
		aaiNQGenericVNF.setModelVersionId("98f410f6-4c63-447b-97d2-42508437cec0"); 
		aaiNQGenericVNF.setOperationalState("active");
		aaiNQGenericVNF.setPersonaModelId("653d2caa-7e47-4614-95b3-26c8d82755b8");
		aaiNQGenericVNF.setPersonaModelVersion("98f410f6-4c63-447b-97d2-42508437cec0");
		aaiNQGenericVNF.setProvStatus("complete");
		aaiNQGenericVNF.setResourceVersion("1505056714553");
		aaiNQGenericVNF.setServiceId("e8cb8968-5411-478b-906a-f28747de72cd");
		aaiNQGenericVNF.setVnfID("ed8b2bce-6b27-4089-992c-4a2c66024bcd"); 
		aaiNQGenericVNF.setVnfName("vCPEInfraVNF14a");
		aaiNQGenericVNF.setVnfName2("malumabb12");
		aaiNQGenericVNF.setVnfType("vCPEInfraService10/vCPEInfraService10 0");
        assertNotNull(aaiNQGenericVNF); 
        
		assertEquals(aaiNQGenericVNF.getEncrypedAccessFlag(), true); 
		assertEquals(aaiNQGenericVNF.getInMaint(), false); 
		assertEquals(aaiNQGenericVNF.getIpv4Loopback0Address(), "aa"); 
		assertEquals(aaiNQGenericVNF.getIpv4OamAddress(), "oamAddress"); 
		assertEquals(aaiNQGenericVNF.getIsClosedLoopDisabled(), false); 
		assertEquals(aaiNQGenericVNF.getModelInvariantId(), "653d2caa-7e47-4614-95b3-26c8d82755b8"); 
		assertEquals(aaiNQGenericVNF.getModelVersionId(), "98f410f6-4c63-447b-97d2-42508437cec0"); 
		assertEquals(aaiNQGenericVNF.getOperationalState(), "active");
		assertEquals(aaiNQGenericVNF.getPersonaModelId(), "653d2caa-7e47-4614-95b3-26c8d82755b8");
		assertEquals(aaiNQGenericVNF.getPersonaModelVersion(), "98f410f6-4c63-447b-97d2-42508437cec0");
		assertEquals(aaiNQGenericVNF.getProvStatus(), "complete");
		assertEquals(aaiNQGenericVNF.getResourceVersion(), "1505056714553");
		assertEquals(aaiNQGenericVNF.getServiceId(), "e8cb8968-5411-478b-906a-f28747de72cd");
		assertEquals(aaiNQGenericVNF.getVnfID(), "ed8b2bce-6b27-4089-992c-4a2c66024bcd"); 
		assertEquals(aaiNQGenericVNF.getVnfName(), "vCPEInfraVNF14a");
		assertEquals(aaiNQGenericVNF.getVnfName2(), "malumabb12");
		assertEquals(aaiNQGenericVNF.getVnfType(), "vCPEInfraService10/vCPEInfraService10 0");
	}

}
