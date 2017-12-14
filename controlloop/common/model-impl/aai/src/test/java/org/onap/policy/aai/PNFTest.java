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

public class PNFTest {
	private static final Logger logger = LoggerFactory.getLogger(PNFTest.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		PNF pnf = new PNF();
		pnf.setPNFName("pnf-name-test");
		pnf.setPNFType(PNFType.ENODEB);
	    assertNotNull(pnf); 
	    
	    PNF pnfClone = new PNF(pnf);
	    assertNotNull(pnfClone);
	    
	    assertEquals("pnf-name-test", pnfClone.getPNFName());
	    assertEquals(PNFType.ENODEB, pnfClone.getPNFType());
	    
	    assertEquals("PNF [PNFName=pnf-name-test, PNFType=eNodeB]", pnfClone.toString());
	    assertNotEquals(0, pnfClone.hashCode());
	    assertNotEquals(0, new PNF().hashCode());
	    
	    PNF pnfOther = new PNF();
	    pnfOther.setPNFName("pnf-name-test");
	    
	    assertTrue(pnf.equals(pnf));
	    assertFalse(pnf.equals(null));
	    assertFalse(pnf.equals("hello"));
	    assertTrue(pnf.equals(pnfClone));
	    assertFalse(pnf.equals(new PNF()));
	    assertFalse(new PNF().equals(pnf));
	    assertFalse(new PNF().equals(pnfOther));
	    assertFalse(pnfOther.equals(pnf));
	    
	    logger.info(Serialization.gsonPretty.toJson(pnf));
	}

}
