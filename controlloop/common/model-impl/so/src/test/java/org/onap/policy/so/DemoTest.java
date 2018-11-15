/*-
 * ============LICENSE_START=======================================================
 * mso
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

package org.onap.policy.so;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.onap.policy.so.SOCloudConfiguration;
import org.onap.policy.so.SOModelInfo;
import org.onap.policy.so.SORelatedInstance;
import org.onap.policy.so.SORelatedInstanceListElement;
import org.onap.policy.so.SORequest;
import org.onap.policy.so.SORequestDetails;
import org.onap.policy.so.SORequestInfo;
import org.onap.policy.so.SORequestParameters;
import org.onap.policy.so.util.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemoTest {
    private static final Logger logger = LoggerFactory.getLogger(DemoTest.class);

    @Test
    public void test() {

        SORequest request = new SORequest();
        request.setRequestDetails(new SORequestDetails());
        request.getRequestDetails().setModelInfo(new SOModelInfo());
        request.getRequestDetails().setCloudConfiguration(new SOCloudConfiguration());
        request.getRequestDetails().setRequestInfo(new SORequestInfo());
        request.getRequestDetails().setRequestParameters(new SORequestParameters());

        request.getRequestDetails().getModelInfo().setModelType("vfModule");
        request.getRequestDetails().getModelInfo().setModelInvariantId("ff5256d2-5a33-55df-13ab-12abad84e7ff");
        request.getRequestDetails().getModelInfo().setModelVersionId("fe6478e5-ea33-3346-ac12-ab121484a3fe");
        request.getRequestDetails().getModelInfo().setModelName("vSAMP12..base..module-0");
        request.getRequestDetails().getModelInfo().setModelVersion("1");

        request.getRequestDetails().getCloudConfiguration().setLcpCloudRegionId("mdt1");
        request.getRequestDetails().getCloudConfiguration().setTenantId("88a6ca3ee0394ade9403f075db23167e");

        request.getRequestDetails().getRequestInfo().setInstanceName("SOTEST103a-vSAMP12_base_module-0");
        request.getRequestDetails().getRequestInfo().setSource("VID");
        request.getRequestDetails().getRequestInfo().setSuppressRollback(true);

        SORelatedInstanceListElement relatedInstanceListElement1 =
                new SORelatedInstanceListElement();
        SORelatedInstanceListElement relatedInstanceListElement2 =
                new SORelatedInstanceListElement();
        SORelatedInstanceListElement relatedInstanceListElement3 =
                new SORelatedInstanceListElement();
        relatedInstanceListElement1.setRelatedInstance(new SORelatedInstance());
        relatedInstanceListElement2.setRelatedInstance(new SORelatedInstance());
        relatedInstanceListElement3.setRelatedInstance(new SORelatedInstance());

        relatedInstanceListElement1.getRelatedInstance().setInstanceId(
                "17ef4658-bd1f-4ef0-9ca0-ea76e2bf122c");
        relatedInstanceListElement1.getRelatedInstance().setInstanceName(
                "SOTESTVOL103a-vSAMP12_base_module-0_vol");
        relatedInstanceListElement1.getRelatedInstance().setModelInfo(new SOModelInfo());
        relatedInstanceListElement1.getRelatedInstance().getModelInfo().setModelType("volumeGroup");

        relatedInstanceListElement2.getRelatedInstance().setInstanceId("serviceInstanceId");
        relatedInstanceListElement2.getRelatedInstance().setModelInfo(new SOModelInfo());
        relatedInstanceListElement2.getRelatedInstance().getModelInfo().setModelType("service");
        relatedInstanceListElement2.getRelatedInstance().getModelInfo().setModelInvariantId(
                "ff3514e3-5a33-55df-13ab-12abad84e7ff");
        relatedInstanceListElement2.getRelatedInstance().getModelInfo().setModelVersionId(
                "fe6985cd-ea33-3346-ac12-ab121484a3fe");
        relatedInstanceListElement2.getRelatedInstance().getModelInfo().setModelName(
                "parent service model name");
        relatedInstanceListElement2.getRelatedInstance().getModelInfo().setModelVersion("1.0");

        relatedInstanceListElement3.getRelatedInstance().setInstanceId("vnfInstanceId");
        relatedInstanceListElement3.getRelatedInstance().setModelInfo(new SOModelInfo());
        relatedInstanceListElement3.getRelatedInstance().getModelInfo().setModelType("vnf");
        relatedInstanceListElement3.getRelatedInstance().getModelInfo().setModelInvariantId(
                "ff5256d1-5a33-55df-13ab-12abad84e7ff");
        relatedInstanceListElement3.getRelatedInstance().getModelInfo().setModelVersionId(
                "fe6478e4-ea33-3346-ac12-ab121484a3fe");
        relatedInstanceListElement3.getRelatedInstance().getModelInfo().setModelName("vSAMP12");
        relatedInstanceListElement3.getRelatedInstance().getModelInfo().setModelVersion("1.0");
        relatedInstanceListElement3.getRelatedInstance().getModelInfo().setModelCustomizationName("vSAMP12 1");

        request.getRequestDetails().getRelatedInstanceList().add(relatedInstanceListElement1);
        request.getRequestDetails().getRelatedInstanceList().add(relatedInstanceListElement2);
        request.getRequestDetails().getRelatedInstanceList().add(relatedInstanceListElement3);

        Map<String, String> userParam1 = new HashMap<>();
        userParam1.put("name1", "value1");

        Map<String, String> userParam2 = new HashMap<>();
        userParam2.put("name2", "value2");

        request.getRequestDetails().getRequestParameters().getUserParams().add(userParam1);
        request.getRequestDetails().getRequestParameters().getUserParams().add(userParam2);

        logger.debug(Serialization.gsonPretty.toJson(request));

    }

    @Test
    public void testHack() {

        logger.debug("**  HACK  **");

        SORequest request = new SORequest();

        request.setRequestDetails(new SORequestDetails());
        request.getRequestDetails().setModelInfo(new SOModelInfo());
        request.getRequestDetails().setCloudConfiguration(new SOCloudConfiguration());
        request.getRequestDetails().setRequestInfo(new SORequestInfo());
        request.getRequestDetails().setRequestParameters(new SORequestParameters());
        request.getRequestDetails().getRequestParameters().setUserParams(null);

        request.getRequestDetails().getModelInfo().setModelType("vfModule");
        request.getRequestDetails().getModelInfo().setModelInvariantId("a9c4a35a-de48-451a-9e4e-343f2ac52928");
        request.getRequestDetails().getModelInfo().setModelVersionId("e0d98ad1-238d-4555-b439-023d3f9079f6");
        request.getRequestDetails().getModelInfo().setModelName("0d9e0d9d352749f4B3cb..dnsscaling..module-0");
        request.getRequestDetails().getModelInfo().setModelVersion("2.0");

        request.getRequestDetails().getCloudConfiguration().setLcpCloudRegionId("DFW");
        request.getRequestDetails().getCloudConfiguration().setTenantId("1015548");

        request.getRequestDetails().getRequestInfo().setInstanceName(
                "Vfmodule_Ete_Name1eScaling63928f-ccdc-4b34-bdef-9bf64109026e");
        request.getRequestDetails().getRequestInfo().setSource("POLICY");
        request.getRequestDetails().getRequestInfo().setSuppressRollback(false);

        SORelatedInstanceListElement relatedInstanceListElement1 =
                new SORelatedInstanceListElement();
        SORelatedInstanceListElement relatedInstanceListElement2 =
                new SORelatedInstanceListElement();
        relatedInstanceListElement1.setRelatedInstance(new SORelatedInstance());
        relatedInstanceListElement2.setRelatedInstance(new SORelatedInstance());

        String serviceInstanceId = "98af39ce-6408-466b-921f-c2c7a8f59ed6";
        relatedInstanceListElement1.getRelatedInstance().setInstanceId(serviceInstanceId);
        relatedInstanceListElement1.getRelatedInstance().setModelInfo(new SOModelInfo());
        relatedInstanceListElement1.getRelatedInstance().getModelInfo().setModelType("service");
        relatedInstanceListElement1.getRelatedInstance().getModelInfo().setModelInvariantId(
                "24329a0c-1d57-4210-b1af-a65df64e9d59");
        relatedInstanceListElement1.getRelatedInstance().getModelInfo().setModelVersionId(
                "ac642881-8e7e-4217-bd64-16ad41c42e30");
        relatedInstanceListElement1.getRelatedInstance().getModelInfo().setModelName("5116d67e-0b4f-46bf-a46f");
        relatedInstanceListElement1.getRelatedInstance().getModelInfo().setModelVersion("2.0");

        String vnfInstanceId = "8eb411b8-a936-412f-b01f-9a9a435c0e93";
        relatedInstanceListElement2.getRelatedInstance().setInstanceId(vnfInstanceId);
        relatedInstanceListElement2.getRelatedInstance().setModelInfo(new SOModelInfo());
        relatedInstanceListElement2.getRelatedInstance().getModelInfo().setModelType("vnf");
        relatedInstanceListElement2.getRelatedInstance().getModelInfo().setModelInvariantId(
                "09fd971e-db5f-475d-997c-cf6704b6b8fe");
        relatedInstanceListElement2.getRelatedInstance().getModelInfo().setModelVersionId(
                "152ed917-6dcc-46ee-bf8a-a775c5aa5a74");
        relatedInstanceListElement2.getRelatedInstance().getModelInfo().setModelName("9e4c31d2-4b25-4d9e-9fb4");
        relatedInstanceListElement2.getRelatedInstance().getModelInfo().setModelVersion("2.0");
        relatedInstanceListElement2.getRelatedInstance().getModelInfo().setModelCustomizationName(
                "0d9e0d9d-3527-49f4-b3cb 2");

        request.getRequestDetails().getRelatedInstanceList().add(relatedInstanceListElement1);
        request.getRequestDetails().getRelatedInstanceList().add(relatedInstanceListElement2);

        logger.debug(Serialization.gsonPretty.toJson(request));
    }

}
