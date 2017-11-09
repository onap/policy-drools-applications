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

public class TestDemo {
	private static final Logger logger = LoggerFactory.getLogger(TestDemo.class);
	@Test
	public void test() {
		
		SORequest request = new SORequest();
		request.requestDetails = new SORequestDetails();
		request.requestDetails.modelInfo = new SOModelInfo();
		request.requestDetails.cloudConfiguration = new SOCloudConfiguration();
		request.requestDetails.requestInfo = new SORequestInfo();
		request.requestDetails.requestParameters = new SORequestParameters();
		
		request.requestDetails.modelInfo.modelType = "vfModule";
		request.requestDetails.modelInfo.modelInvariantId = "ff5256d2-5a33-55df-13ab-12abad84e7ff";
		request.requestDetails.modelInfo.modelVersionId = "fe6478e5-ea33-3346-ac12-ab121484a3fe";
		request.requestDetails.modelInfo.modelName = "vSAMP12..base..module-0";
		request.requestDetails.modelInfo.modelVersion = "1";
		
		request.requestDetails.cloudConfiguration.lcpCloudRegionId = "mdt1";
		request.requestDetails.cloudConfiguration.tenantId = "88a6ca3ee0394ade9403f075db23167e";
		
		request.requestDetails.requestInfo.instanceName = "SOTEST103a-vSAMP12_base_module-0";
		request.requestDetails.requestInfo.source = "VID";
		request.requestDetails.requestInfo.suppressRollback = true;
		
		SORelatedInstanceListElement relatedInstanceListElement1 = new SORelatedInstanceListElement();
		SORelatedInstanceListElement relatedInstanceListElement2 = new SORelatedInstanceListElement();
		SORelatedInstanceListElement relatedInstanceListElement3 = new SORelatedInstanceListElement();
		relatedInstanceListElement1.relatedInstance = new SORelatedInstance();
		relatedInstanceListElement2.relatedInstance = new SORelatedInstance();
		relatedInstanceListElement3.relatedInstance = new SORelatedInstance();
		
		relatedInstanceListElement1.relatedInstance.instanceId = "17ef4658-bd1f-4ef0-9ca0-ea76e2bf122c";
		relatedInstanceListElement1.relatedInstance.instanceName = "SOTESTVOL103a-vSAMP12_base_module-0_vol";
		relatedInstanceListElement1.relatedInstance.modelInfo = new SOModelInfo();
		relatedInstanceListElement1.relatedInstance.modelInfo.modelType = "volumeGroup";
		
		relatedInstanceListElement2.relatedInstance.instanceId = "serviceInstanceId";
		relatedInstanceListElement2.relatedInstance.modelInfo = new SOModelInfo();
		relatedInstanceListElement2.relatedInstance.modelInfo.modelType = "service";
		relatedInstanceListElement2.relatedInstance.modelInfo.modelInvariantId = "ff3514e3-5a33-55df-13ab-12abad84e7ff";
		relatedInstanceListElement2.relatedInstance.modelInfo.modelVersionId = "fe6985cd-ea33-3346-ac12-ab121484a3fe";
		relatedInstanceListElement2.relatedInstance.modelInfo.modelName = "parent service model name";
		relatedInstanceListElement2.relatedInstance.modelInfo.modelVersion = "1.0";
		
		relatedInstanceListElement3.relatedInstance.instanceId = "vnfInstanceId";
		relatedInstanceListElement3.relatedInstance.modelInfo = new SOModelInfo();
		relatedInstanceListElement3.relatedInstance.modelInfo.modelType = "vnf";
		relatedInstanceListElement3.relatedInstance.modelInfo.modelInvariantId = "ff5256d1-5a33-55df-13ab-12abad84e7ff";
		relatedInstanceListElement3.relatedInstance.modelInfo.modelVersionId = "fe6478e4-ea33-3346-ac12-ab121484a3fe";
		relatedInstanceListElement3.relatedInstance.modelInfo.modelName = "vSAMP12";
		relatedInstanceListElement3.relatedInstance.modelInfo.modelVersion = "1.0";
		relatedInstanceListElement3.relatedInstance.modelInfo.modelCustomizationName = "vSAMP12 1";
			
		request.requestDetails.relatedInstanceList.add(relatedInstanceListElement1);
		request.requestDetails.relatedInstanceList.add(relatedInstanceListElement2);
		request.requestDetails.relatedInstanceList.add(relatedInstanceListElement3);
		
		Map<String, String> userParam1 = new HashMap<>();
		userParam1.put("name1", "value1");
		
		Map<String, String> userParam2 = new HashMap<>();
		userParam2.put("name2", "value2");
		
		request.requestDetails.requestParameters.userParams.add(userParam1);
		request.requestDetails.requestParameters.userParams.add(userParam2);
		
		logger.debug(Serialization.gsonPretty.toJson(request));
				
	}
	
	@Test
	public void testHack() {
		
		logger.debug("**  HACK  **");
		
		SORequest request = new SORequest();
		//
		request.requestDetails = new SORequestDetails();
		request.requestDetails.modelInfo = new SOModelInfo();
		request.requestDetails.cloudConfiguration = new SOCloudConfiguration();
		request.requestDetails.requestInfo = new SORequestInfo();
		request.requestDetails.requestParameters = new SORequestParameters();
		request.requestDetails.requestParameters.userParams = null;
		
		request.requestDetails.modelInfo.modelType = "vfModule";
		request.requestDetails.modelInfo.modelInvariantId = "a9c4a35a-de48-451a-9e4e-343f2ac52928";
		request.requestDetails.modelInfo.modelVersionId = "e0d98ad1-238d-4555-b439-023d3f9079f6";
		request.requestDetails.modelInfo.modelName = "0d9e0d9d352749f4B3cb..dnsscaling..module-0";
		request.requestDetails.modelInfo.modelVersion = "2.0";
		
		request.requestDetails.cloudConfiguration.lcpCloudRegionId = "DFW";
		request.requestDetails.cloudConfiguration.tenantId = "1015548";
		
		request.requestDetails.requestInfo.instanceName = "Vfmodule_Ete_Name1eScaling63928f-ccdc-4b34-bdef-9bf64109026e";
		request.requestDetails.requestInfo.source = "POLICY";
		request.requestDetails.requestInfo.suppressRollback = false;
		
		SORelatedInstanceListElement relatedInstanceListElement1 = new SORelatedInstanceListElement();
		SORelatedInstanceListElement relatedInstanceListElement2 = new SORelatedInstanceListElement();
		relatedInstanceListElement1.relatedInstance = new SORelatedInstance();
		relatedInstanceListElement2.relatedInstance = new SORelatedInstance();
		
		String serviceInstanceId = "98af39ce-6408-466b-921f-c2c7a8f59ed6";
		relatedInstanceListElement1.relatedInstance.instanceId = serviceInstanceId;
		relatedInstanceListElement1.relatedInstance.modelInfo = new SOModelInfo();
		relatedInstanceListElement1.relatedInstance.modelInfo.modelType = "service";
		relatedInstanceListElement1.relatedInstance.modelInfo.modelInvariantId = "24329a0c-1d57-4210-b1af-a65df64e9d59";
		relatedInstanceListElement1.relatedInstance.modelInfo.modelVersionId = "ac642881-8e7e-4217-bd64-16ad41c42e30";
		relatedInstanceListElement1.relatedInstance.modelInfo.modelName = "5116d67e-0b4f-46bf-a46f";
		relatedInstanceListElement1.relatedInstance.modelInfo.modelVersion = "2.0";
		
		String vnfInstanceId = "8eb411b8-a936-412f-b01f-9a9a435c0e93";
		relatedInstanceListElement2.relatedInstance.instanceId = vnfInstanceId;
		relatedInstanceListElement2.relatedInstance.modelInfo = new SOModelInfo();
		relatedInstanceListElement2.relatedInstance.modelInfo.modelType = "vnf";
		relatedInstanceListElement2.relatedInstance.modelInfo.modelInvariantId = "09fd971e-db5f-475d-997c-cf6704b6b8fe";
		relatedInstanceListElement2.relatedInstance.modelInfo.modelVersionId = "152ed917-6dcc-46ee-bf8a-a775c5aa5a74";
		relatedInstanceListElement2.relatedInstance.modelInfo.modelName = "9e4c31d2-4b25-4d9e-9fb4";
		relatedInstanceListElement2.relatedInstance.modelInfo.modelVersion = "2.0";
		relatedInstanceListElement2.relatedInstance.modelInfo.modelCustomizationName = "0d9e0d9d-3527-49f4-b3cb 2";
			
		request.requestDetails.relatedInstanceList.add(relatedInstanceListElement1);
		request.requestDetails.relatedInstanceList.add(relatedInstanceListElement2);
		
		logger.debug(Serialization.gsonPretty.toJson(request));
	}

}
