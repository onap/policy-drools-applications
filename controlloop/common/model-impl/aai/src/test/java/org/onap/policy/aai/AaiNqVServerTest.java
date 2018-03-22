/*-
 * ============LICENSE_START=======================================================
 * aai
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.aai.util.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AaiNqVServerTest {
    private static final Logger logger = LoggerFactory.getLogger(AaiNqVServerTest.class);


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {}

    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    @Test
    public void test() {
        AaiNqVServer aaiNqVServer = new AaiNqVServer();
        aaiNqVServer.setVserverId("dhv-test-vserver");
        aaiNqVServer.setVserverName("dhv-test-vserver-name");
        aaiNqVServer.setVserverName2("dhv-test-vserver-name2");
        aaiNqVServer.setProvStatus("PREPROV");
        aaiNqVServer.setVserverSelflink("dhv-test-vserver-selflink");
        aaiNqVServer.setInMaint(false);
        aaiNqVServer.setIsClosedLoopDisabled(false);
        aaiNqVServer.setResourceVersion("1485366417");
        assertNotNull(aaiNqVServer);
        assertEquals("dhv-test-vserver", aaiNqVServer.getVserverId());
        assertEquals("dhv-test-vserver-name", aaiNqVServer.getVserverName());
        assertEquals("dhv-test-vserver-name2", aaiNqVServer.getVserverName2());
        assertEquals("PREPROV", aaiNqVServer.getProvStatus());
        assertEquals("dhv-test-vserver-selflink", aaiNqVServer.getVserverSelflink());
        assertEquals(false, aaiNqVServer.getInMaint());
        assertEquals(false, aaiNqVServer.getIsClosedLoopDisabled());
        assertEquals("1485366417", aaiNqVServer.getResourceVersion());
        logger.info(Serialization.gsonPretty.toJson(aaiNqVServer));
    }

}
