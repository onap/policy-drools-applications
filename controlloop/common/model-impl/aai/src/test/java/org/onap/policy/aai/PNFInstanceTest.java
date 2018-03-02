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

public class PNFInstanceTest {
	private static final Logger logger = LoggerFactory.getLogger(PNFInstanceTest.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		PNFInstance pnfInstance = new PNFInstance(); 
		pnfInstance.setPNFInstanceName("pnf-instance-name-test"); 
		pnfInstance.setPNFName("pnf-name-test"); 
		pnfInstance.setPNFType(PNFType.ENODEB); 
		pnfInstance.setPNFSerial("pnf-serial-test"); 
	    assertNotNull(pnfInstance); 
	    assertEquals("pnf-instance-name-test", pnfInstance.getPNFInstanceName()); 

	    PNFInstance pnfInstanceNull = new PNFInstance(null);
	    assertNotNull(pnfInstanceNull);
	    
	    PNFInstance pnfInstanceClone = new PNFInstance(pnfInstance);
	    assertNotNull(pnfInstanceClone);
	    
	    assertEquals("pnf-name-test", pnfInstanceClone.getPNFName());
	    assertEquals(PNFType.ENODEB, pnfInstanceClone.getPNFType());
	    assertEquals("pnf-serial-test", pnfInstanceClone.getPNFSerial());
	    
	    assertEquals("PNFInstance [PNFName=pnf-name-test, PNFInstanceName=pnf-instance-name-test, PNFType=eNodeB, PNFSerial=pnf-serial-test]", pnfInstanceClone.toString());
	    assertNotEquals(0, pnfInstanceClone.hashCode());
	    assertNotEquals(0, new PNF().hashCode());
	    
	    PNFInstance pnfInstanceOther0 = new PNFInstance();
	    pnfInstanceOther0.setPNFName("pnf-name-test");

	    PNFInstance pnfInstanceOther1 = new PNFInstance(pnfInstance);
	    pnfInstanceOther1.setPNFName("pnf-name-test-diff");
	    
	    PNFInstance pnfInstanceOther2 = new PNFInstance(pnfInstance);
	    pnfInstanceOther2.setPNFInstanceName("pnf-instance-name-test-diff");
	    
	    PNFInstance pnfInstanceOther3 = new PNFInstance(pnfInstance);
	    pnfInstanceOther3.setPNFName(null);
	    
	    PNFInstance pnfInstanceOther4 = new PNFInstance(pnfInstance);
	    pnfInstanceOther4.setPNFSerial(null);
	    
	    PNFInstance pnfInstanceOther5 = new PNFInstance(pnfInstance);
	    pnfInstanceOther5.setPNFSerial("pnf-serial-test-diff");
	    
	    assertTrue(pnfInstance.equals(pnfInstance));
	    assertFalse(pnfInstance.equals(null));
	    assertFalse(pnfInstance.equals("hello"));
	    assertTrue(pnfInstance.equals(pnfInstanceClone));
	    assertFalse(pnfInstance.equals(new PNF()));
	    assertFalse(new PNF().equals(pnfInstance));
	    assertFalse(new PNF().equals(pnfInstanceOther0));
	    assertFalse(pnfInstanceOther0.equals(pnfInstance));
	    assertFalse(pnfInstanceOther1.equals(pnfInstance));
	    assertFalse(pnfInstanceOther2.equals(pnfInstance));
	    assertFalse(pnfInstanceOther3.equals(pnfInstance));
	    assertFalse(pnfInstanceOther4.equals(pnfInstance));
	    assertFalse(pnfInstanceOther5.equals(pnfInstance));
	    
	    logger.info(Serialization.gsonPretty.toJson(pnfInstance));
	}

}
