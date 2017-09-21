/*-
 * ============LICENSE_START=======================================================
 * simulators
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

package org.onap.policy.simulators;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.drools.http.server.HttpServletServer;
import org.onap.policy.drools.utils.LoggerUtil;
import org.onap.policy.rest.RESTManager;
import org.onap.policy.rest.RESTManager.Pair;
import org.onap.policy.vfc.VFCResponse;
import org.onap.policy.vfc.util.Serialization;

public class VfcSimulatorTest {

  @BeforeClass
  public static void setUpSimulator() {
    LoggerUtil.setLevel("ROOT", "INFO");
    LoggerUtil.setLevel("org.eclipse.jetty", "WARN");
    try {
      Util.buildVfcSim();
    } catch (final Exception e) {
      fail(e.getMessage());
    }
  }

  @AfterClass
  public static void tearDownSimulator() {
    HttpServletServer.factory.destroy();
  }

  @Test
  public void testPost() {
    final Pair<Integer, String> httpDetails =
        RESTManager.post("http://localhost:6668/api/nslcm/v1/ns/1234567890/heal", "username",
            "password", new HashMap<String, String>(), "application/json", "Some Request Here");
    assertNotNull(httpDetails);
    assertTrue(httpDetails.a == 202);
    final VFCResponse response =
        Serialization.gsonPretty.fromJson(httpDetails.b, VFCResponse.class);
    assertNotNull(response);
  }

  @Test
  public void testGet() {
    final Pair<Integer, String> httpDetails =
        RESTManager.get("http://localhost:6668/api/nslcm/v1/jobs/1234", "username", "password",
            new HashMap<String, String>());
    assertNotNull(httpDetails);
    final VFCResponse response =
        Serialization.gsonPretty.fromJson(httpDetails.b, VFCResponse.class);
    assertNotNull(response);
  }
}
