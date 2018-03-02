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

public class AAINQVServerTest {
	private static final Logger logger = LoggerFactory.getLogger(AAINQVServerTest.class);


	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		AAINQVServer aaiNQVServer = new AAINQVServer();
		aaiNQVServer.setVserverId("dhv-test-vserver");
		aaiNQVServer.setVserverName("dhv-test-vserver-name");
		aaiNQVServer.setVserverName2("dhv-test-vserver-name2");
		aaiNQVServer.setProvStatus("PREPROV");
		aaiNQVServer.setVserverSelflink("dhv-test-vserver-selflink");
		aaiNQVServer.setInMaint(false);
		aaiNQVServer.setIsClosedLoopDisabled(false);
		aaiNQVServer.setResourceVersion("1485366417");
	    assertNotNull(aaiNQVServer); 
	    assertEquals("dhv-test-vserver", aaiNQVServer.getVserverId());
	    assertEquals("dhv-test-vserver-name", aaiNQVServer.getVserverName());
	    assertEquals("dhv-test-vserver-name2", aaiNQVServer.getVserverName2());
	    assertEquals("PREPROV", aaiNQVServer.getProvStatus());
	    assertEquals("dhv-test-vserver-selflink", aaiNQVServer.getVserverSelflink());
	    assertEquals(false, aaiNQVServer.getInMaint());
	    assertEquals(false, aaiNQVServer.getIsClosedLoopDisabled());
		assertEquals("1485366417", aaiNQVServer.getResourceVersion());
	    logger.info(Serialization.gsonPretty.toJson(aaiNQVServer));
	}

}
