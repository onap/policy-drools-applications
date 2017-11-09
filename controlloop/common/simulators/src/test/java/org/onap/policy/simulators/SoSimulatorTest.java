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
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.drools.http.server.HttpServletServer;
import org.onap.policy.drools.utils.LoggerUtil;
import org.onap.policy.rest.RESTManager;
import org.onap.policy.rest.RESTManager.Pair;
import org.onap.policy.so.SOCloudConfiguration;
import org.onap.policy.so.SOModelInfo;
import org.onap.policy.so.SORelatedInstance;
import org.onap.policy.so.SORelatedInstanceListElement;
import org.onap.policy.so.SORequest;
import org.onap.policy.so.SORequestDetails;
import org.onap.policy.so.SORequestInfo;
import org.onap.policy.so.SORequestParameters;
import org.onap.policy.so.SOResponse;
import org.onap.policy.so.util.Serialization;

public class SoSimulatorTest {

  @BeforeClass
  public static void setUpSimulator() {
    LoggerUtil.setLevel("ROOT", "INFO");
    LoggerUtil.setLevel("org.eclipse.jetty", "WARN");
    try {
      Util.buildSoSim();
    } catch (final Exception e) {
      fail(e.getMessage());
    }
  }

  @AfterClass
  public static void tearDownSimulator() {
    HttpServletServer.factory.destroy();
  }

  /**
   * Create dummy SO request for TestResponse() junit
   */
  private SORequest createTestRequest() {

    // Construct SO Request
    final SORequest request = new SORequest();
    request.requestId = UUID.randomUUID();
    request.requestDetails = new SORequestDetails();
    request.requestDetails.modelInfo = new SOModelInfo();
    request.requestDetails.cloudConfiguration = new SOCloudConfiguration();
    request.requestDetails.requestInfo = new SORequestInfo();
    request.requestDetails.requestParameters = new SORequestParameters();
    request.requestDetails.requestParameters.userParams = null;
    //
    // cloudConfiguration
    //
    request.requestDetails.cloudConfiguration.lcpCloudRegionId = "DFW";
    request.requestDetails.cloudConfiguration.tenantId = "1015548";
    //
    // modelInfo
    //
    request.requestDetails.modelInfo.modelType = "vfModule";
    request.requestDetails.modelInfo.modelInvariantId = "f32568ec-2f1c-458a-864b-0593d53d141a";
    request.requestDetails.modelInfo.modelVersionId = "69615025-879d-4f0d-afe3-b7d1a7eeed1f";
    request.requestDetails.modelInfo.modelName = "C15ce9e1E9144c8fB8bb..dnsscaling..module-1";
    request.requestDetails.modelInfo.modelVersion = "1.0";
    //
    // requestInfo
    //
    request.requestDetails.requestInfo.instanceName =
        "vDNS_Ete_Named90e1ab3-dcd5-4877-9edb-eadfc84e32c8";
    request.requestDetails.requestInfo.source = "POLICY";
    request.requestDetails.requestInfo.suppressRollback = false;
    request.requestDetails.requestInfo.requestorId = "policy";
    //
    // relatedInstanceList
    //
    final SORelatedInstanceListElement relatedInstanceListElement1 =
        new SORelatedInstanceListElement();
    final SORelatedInstanceListElement relatedInstanceListElement2 =
        new SORelatedInstanceListElement();
    relatedInstanceListElement1.relatedInstance = new SORelatedInstance();
    relatedInstanceListElement2.relatedInstance = new SORelatedInstance();
    //
    relatedInstanceListElement1.relatedInstance.instanceId = "cf8426a6-0b53-4e3d-bfa6-4b2f4d5913a5";
    relatedInstanceListElement1.relatedInstance.modelInfo = new SOModelInfo();
    relatedInstanceListElement1.relatedInstance.modelInfo.modelType = "service";
    relatedInstanceListElement1.relatedInstance.modelInfo.modelInvariantId =
        "4fcbc1c0-7793-46d8-8aa1-fa1c2ed9ec7b";
    relatedInstanceListElement1.relatedInstance.modelInfo.modelVersionId =
        "5c996219-b2e2-4c76-9b43-7e8672a33c1d";
    relatedInstanceListElement1.relatedInstance.modelInfo.modelName = "8330e932-2a23-4943-8606";
    relatedInstanceListElement1.relatedInstance.modelInfo.modelVersion = "1.0";
    //
    relatedInstanceListElement2.relatedInstance.instanceId = "594e2fe0-48b8-41ff-82e2-3d4bab69b192";
    relatedInstanceListElement2.relatedInstance.modelInfo = new SOModelInfo();
    relatedInstanceListElement2.relatedInstance.modelInfo.modelType = "vnf";
    relatedInstanceListElement2.relatedInstance.modelInfo.modelInvariantId =
        "033a32ed-aa65-4764-a736-36f2942f1aa0";
    relatedInstanceListElement2.relatedInstance.modelInfo.modelVersionId =
        "d4d072dc-4e21-4a03-9524-628985819a8e";
    relatedInstanceListElement2.relatedInstance.modelInfo.modelName = "c15ce9e1-e914-4c8f-b8bb";
    relatedInstanceListElement2.relatedInstance.modelInfo.modelVersion = "1";
    relatedInstanceListElement2.relatedInstance.modelInfo.modelCustomizationName =
        "c15ce9e1-e914-4c8f-b8bb 1";
    //
    request.requestDetails.relatedInstanceList.add(relatedInstanceListElement1);
    request.requestDetails.relatedInstanceList.add(relatedInstanceListElement2);

    return request;
  }

  @Test
  public void testResponse() {
    final String request = Serialization.gsonPretty.toJson(this.createTestRequest());
    final Pair<Integer, String> httpDetails = RESTManager.post(
        "http://localhost:6667/serviceInstances/v5/12345/vnfs/12345/vfModules", "username",
        "password", new HashMap<>(), "application/json", request);
    assertNotNull(httpDetails);
    final SOResponse response = Serialization.gsonPretty.fromJson(httpDetails.b, SOResponse.class);
    assertNotNull(response);
  }
}
