/*-
 * ============LICENSE_START=======================================================
 * demo
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

package org.onap.policy.template.demo;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.UUID;

import org.junit.Test;
import org.onap.policy.mso.SOCloudConfiguration;
import org.onap.policy.mso.SOModelInfo;
import org.onap.policy.mso.SORelatedInstance;
import org.onap.policy.mso.SORelatedInstanceListElement;
import org.onap.policy.mso.SORequest;
import org.onap.policy.mso.SORequestDetails;
import org.onap.policy.mso.SORequestInfo;
import org.onap.policy.mso.SORequestParameters;
import org.onap.policy.aai.AAINQF199.AAINQF199Response;
import org.onap.policy.aai.AAINQF199.AAINQF199ResponseWrapper;
import org.onap.policy.mso.util.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class TestSO {

	private static final Logger logger = LoggerFactory.getLogger(TestSO.class);
			
	@Test
	public void test() throws FileNotFoundException {
		Gson gson = new Gson();
		JsonReader reader = new JsonReader(new FileReader("src/test/resources/aairesponse.json"));
		AAINQF199Response response = gson.fromJson(reader, AAINQF199Response.class);
		
		logger.debug(Serialization.gsonPretty.toJson(response));
		
		AAINQF199ResponseWrapper aainqf199ResponseWrapper = new AAINQF199ResponseWrapper(UUID.randomUUID(), response);
		
		//
		//
		// vnfItem
		//
		String vnfItemVnfId = aainqf199ResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).genericVNF.vnfID;
		String vnfItemVnfType = aainqf199ResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).genericVNF.vnfType;
		vnfItemVnfType = vnfItemVnfType.substring(vnfItemVnfType.lastIndexOf("/")+1);
		String vnfItemPersonaModelId = aainqf199ResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).genericVNF.personaModelId;
		String vnfItemPersonaModelVersion = aainqf199ResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).genericVNF.personaModelVersion;
		String vnfItemModelName = aainqf199ResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).extraProperties.extraProperty.get(0).propertyValue;
		String vnfItemModelNameVersionId = 	aainqf199ResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).extraProperties.extraProperty.get(4).propertyValue;
		//
		// serviceItem
		//
		String serviceItemServiceInstanceId = aainqf199ResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).serviceInstance.serviceInstanceID;
		String serviceItemPersonaModelId = aainqf199ResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).serviceInstance.personaModelId;
		String serviceItemModelName = aainqf199ResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).extraProperties.extraProperty.get(0).propertyValue;
		String serviceItemModelVersion = aainqf199ResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).serviceInstance.personaModelVersion;
		String serviceItemModelNameVersionId = aainqf199ResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).extraProperties.extraProperty.get(4).propertyValue;
		//
		// This comes from the base module
		//
		String vfModuleItemVfModuleName = 			aainqf199ResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).items.inventoryResponseItems.get(1).vfModule.vfModuleName;
		vfModuleItemVfModuleName = vfModuleItemVfModuleName.replace("Vfmodule", "vDNS");
		//
		// vfModuleItem - NOT the base module
		//
		String vfModuleItemPersonaModelId = 		aainqf199ResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).items.inventoryResponseItems.get(2).vfModule.personaModelId;
		String vfModuleItemPersonaModelVersion = 	aainqf199ResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).items.inventoryResponseItems.get(2).vfModule.personaModelVersion;
		String vfModuleItemModelName = 				aainqf199ResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).items.inventoryResponseItems.get(2).extraProperties.extraProperty.get(0).propertyValue;
		String vfModuleItemModelNameVersionId = 	aainqf199ResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).items.inventoryResponseItems.get(2).extraProperties.extraProperty.get(4).propertyValue;
		
		//
		// tenantItem
		//
		String tenantItemTenantId = aainqf199ResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(1).tenant.tenantId;
		//
		// cloudRegionItem
		//
		String cloudRegionItemCloudRegionId = aainqf199ResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(1).items.inventoryResponseItems.get(0).cloudRegion.cloudRegionId;

		//
		// Construct an SO request
		//
		SORequest request = new SORequest();
		request.requestDetails = new SORequestDetails();
		request.requestDetails.modelInfo = new SOModelInfo();
		request.requestDetails.cloudConfiguration = new SOCloudConfiguration();
		request.requestDetails.requestInfo = new SORequestInfo();
		request.requestDetails.requestParameters = new SORequestParameters();
		request.requestDetails.requestParameters.userParams = null;
		//
		// cloudConfiguration
		//
		request.requestDetails.cloudConfiguration.lcpCloudRegionId = cloudRegionItemCloudRegionId;
		request.requestDetails.cloudConfiguration.tenantId = tenantItemTenantId;
		//
		// modelInfo
		//
		request.requestDetails.modelInfo.modelType = "vfModule";
		request.requestDetails.modelInfo.modelInvariantId = vfModuleItemPersonaModelId;
		request.requestDetails.modelInfo.modelNameVersionId = vfModuleItemModelNameVersionId;
		request.requestDetails.modelInfo.modelName = vfModuleItemModelName;
		request.requestDetails.modelInfo.modelVersion = vfModuleItemPersonaModelVersion;
		//
		// requestInfo
		//
		request.requestDetails.requestInfo.instanceName = vfModuleItemVfModuleName;
		request.requestDetails.requestInfo.source = "POLICY";
		request.requestDetails.requestInfo.suppressRollback = false;
		//
		// relatedInstanceList
		//
		SORelatedInstanceListElement relatedInstanceListElement1 = new SORelatedInstanceListElement();
		SORelatedInstanceListElement relatedInstanceListElement2 = new SORelatedInstanceListElement();
		relatedInstanceListElement1.relatedInstance = new SORelatedInstance();
		relatedInstanceListElement2.relatedInstance = new SORelatedInstance();
		//
		relatedInstanceListElement1.relatedInstance.instanceId = serviceItemServiceInstanceId;
		relatedInstanceListElement1.relatedInstance.modelInfo = new SOModelInfo();
		relatedInstanceListElement1.relatedInstance.modelInfo.modelType = "service";
		relatedInstanceListElement1.relatedInstance.modelInfo.modelInvariantId = serviceItemPersonaModelId;
		relatedInstanceListElement1.relatedInstance.modelInfo.modelNameVersionId = serviceItemModelNameVersionId;
		relatedInstanceListElement1.relatedInstance.modelInfo.modelName = serviceItemModelName;
		relatedInstanceListElement1.relatedInstance.modelInfo.modelVersion = serviceItemModelVersion;
		//
		relatedInstanceListElement2.relatedInstance.instanceId = vnfItemVnfId;
		relatedInstanceListElement2.relatedInstance.modelInfo = new SOModelInfo();
		relatedInstanceListElement2.relatedInstance.modelInfo.modelType = "vnf";
		relatedInstanceListElement2.relatedInstance.modelInfo.modelInvariantId = vnfItemPersonaModelId;
		relatedInstanceListElement2.relatedInstance.modelInfo.modelNameVersionId = vnfItemModelNameVersionId;
		relatedInstanceListElement2.relatedInstance.modelInfo.modelName = vnfItemModelName;
		relatedInstanceListElement2.relatedInstance.modelInfo.modelVersion = vnfItemPersonaModelVersion;
		relatedInstanceListElement2.relatedInstance.modelInfo.modelCustomizationName = vnfItemVnfType;
		//	
		request.requestDetails.relatedInstanceList.add(relatedInstanceListElement1);
		request.requestDetails.relatedInstanceList.add(relatedInstanceListElement2);
		//
		// print SO request for debug
		//
		logger.debug("SO request sent:");
		logger.debug(Serialization.gsonPretty.toJson(request));
	}

}
