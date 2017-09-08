/*-
 * ============LICENSE_START=======================================================
 * MSOActorServiceProvider
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

package org.onap.policy.controlloop.actor.mso;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.drools.core.WorkingMemory;
import org.onap.policy.aai.AAINQF199.AAINQF199InstanceFilters;
import org.onap.policy.aai.AAINQF199.AAINQF199InventoryResponseItem;
import org.onap.policy.aai.AAINQF199.AAINQF199Manager;
import org.onap.policy.aai.AAINQF199.AAINQF199NamedQuery;
import org.onap.policy.aai.AAINQF199.AAINQF199QueryParameters;
import org.onap.policy.aai.AAINQF199.AAINQF199Request;
import org.onap.policy.aai.AAINQF199.AAINQF199RequestWrapper;
import org.onap.policy.aai.AAINQF199.AAINQF199Response;
import org.onap.policy.aai.AAINQF199.AAINQF199ResponseWrapper;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.actorServiceProvider.spi.Actor;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.mso.SOCloudConfiguration;
import org.onap.policy.mso.SOManager;
import org.onap.policy.mso.SOModelInfo;
import org.onap.policy.mso.SORelatedInstance;
import org.onap.policy.mso.SORelatedInstanceListElement;
import org.onap.policy.mso.SORequest;
import org.onap.policy.mso.SORequestDetails;
import org.onap.policy.mso.SORequestInfo;
import org.onap.policy.mso.SORequestParameters;
import org.onap.policy.mso.util.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class MSOActorServiceProvider implements Actor {
	
	private static final Logger logger = LoggerFactory.getLogger(MSOActorServiceProvider.class);

	private static String vnfItemVnfId;

	private String vnfItemVnfType;

	private String vnfItemModelInvariantId;

	private String vnfItemModelVersionId;

	private String vnfItemModelName;

	private String vnfItemModelVersion;

	private String vnfItemModelNameVersionId;

	private static String serviceItemServiceInstanceId;

	private String serviceItemPersonaModelId;

	private String serviceItemModelName;

	private String serviceItemModelType;

	private String serviceItemModelVersion;

	private String serviceItemModelNameVersionId;

	private String vfModuleItemVfModuleName;

	private String vfModuleItemPersonaModelId;

	private String vfModuleItemPersonaModelVersion;

	private String vfModuleItemModelName;

	private String vfModuleItemModelNameVersionId;

	private String tenantItemTenantId;

	private String cloudRegionItemCloudRegionId;

	private static final ImmutableList<String> recipes = ImmutableList.of(
			"VF Module Create");
	private static final ImmutableMap<String, List<String>> targets = new ImmutableMap.Builder<String, List<String>>()
			.put("VF Module Create", ImmutableList.of("VFC"))
			.build();
	
	@Override
	public String actor() {
		return "SO";
	}

	@Override
	public List<String> recipes() {
		return ImmutableList.copyOf(recipes);
	}

	@Override
	public List<String> recipeTargets(String recipe) {
		return ImmutableList.copyOf(targets.getOrDefault(recipe, Collections.emptyList()));
	}

	@Override
	public List<String> recipePayloads(String recipe) {
		return Collections.emptyList();
	}
	
	/**
	 * MSOActorServiceProvider Constructor
	 * 
	 */
	public MSOActorServiceProvider() {
		
	}
	
	/**
	 * Constructs and sends an AAI vserver Named Query
	 * 
	 * @param eventRequestID
	 * @returns the response to the AAI Named Query
	 */
	private AAINQF199ResponseWrapper AaiNamedQueryRequest(VirtualControlLoopEvent onset) {
		
		// create AAI named-query request with UUID started with "F199"
		AAINQF199Request aainqf199request = new AAINQF199Request();
		AAINQF199QueryParameters aainqf199queryparam = new AAINQF199QueryParameters();
		AAINQF199NamedQuery aainqf199namedquery = new AAINQF199NamedQuery();
		AAINQF199InstanceFilters aainqf199instancefilter = new AAINQF199InstanceFilters();

		// queryParameters
		aainqf199namedquery.namedQueryUUID = UUID.fromString("4ff56a54-9e3f-46b7-a337-07a1d3c6b469"); // UUID.fromString($params.getAaiNamedQueryUUID()) TO DO: AaiNamedQueryUUID 
		aainqf199queryparam.namedQuery = aainqf199namedquery;
		aainqf199request.queryParameters = aainqf199queryparam;
		//
		// instanceFilters
		//
		Map aainqf199instancefiltermap = new HashMap();
		Map aainqf199instancefiltermapitem = new HashMap();
		aainqf199instancefiltermapitem.put("vserver-name", onset.AAI.get("vserver.vserver-name")); // TO DO: get vserver.vname from dcae onset.AAI.get("vserver.vserver-name")
		aainqf199instancefiltermap.put("vserver", aainqf199instancefiltermapitem);
		aainqf199instancefilter.instanceFilter.add(aainqf199instancefiltermap);
		aainqf199request.instanceFilters = aainqf199instancefilter;
		//
		// print aainqf199request for debug
		//
  		logger.debug("AAI Request sent:");
  		logger.debug(Serialization.gsonPretty.toJson(aainqf199request));
		//
		// Create AAINQF199RequestWrapper
		//
		AAINQF199RequestWrapper aainqf199RequestWrapper = new AAINQF199RequestWrapper(onset.requestID, aainqf199request);
		//
		// insert aainqf199request into memory
		//
//		insert(aainqf199RequestWrapper);
		
		String url = "http://localhost:6666";
		String username = "testUser";
		String password = "testPass";
		
		//***** send the request *****\\
		AAINQF199Response aainqf199response = AAINQF199Manager.postQuery(url, username, password, // TO DO: get AAI URL, username, and password
				aainqf199RequestWrapper.aainqf199request, onset.requestID);

		// Check AAI response
		if (aainqf199response == null) {
			System.err.println("Failed to get AAI response");
			
			// Fail and retract everything
			return null;
		} else {
			// Create AAINQF199ResponseWrapper
			AAINQF199ResponseWrapper aainqf199ResponseWrapper = new AAINQF199ResponseWrapper(onset.requestID, aainqf199response);

			// insert aainqf199ResponseWrapper to memory -- Is this needed?
//			insert(aainqf199ResponseWrapper);
			
			// 
			extractSOFieldsFromNamedQuery(aainqf199ResponseWrapper, onset);
			return aainqf199ResponseWrapper;
		}
	}

	/**
	 * Extract the required fields from the named query response
	 * @param namedQueryResponseWrapper
	 * @param onset
	 */
	private void extractSOFieldsFromNamedQuery(AAINQF199ResponseWrapper namedQueryResponseWrapper, VirtualControlLoopEvent onset) {
		
		try {
			// vnfItem
			setVnfItemVnfId(namedQueryResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).genericVNF.vnfID);
			setVnfItemVnfType(namedQueryResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).genericVNF.vnfType);
			setVnfItemVnfType(vnfItemVnfType.substring(vnfItemVnfType.lastIndexOf("/")+1));
			setVnfItemModelInvariantId(namedQueryResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).genericVNF.personaModelId);
			setVnfItemModelVersionId(namedQueryResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).genericVNF.personaModelVersion);
			setVnfItemModelName(namedQueryResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).extraProperties.extraProperty.get(0).propertyValue);
			setVnfItemModelVersion(namedQueryResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).extraProperties.extraProperty.get(2).propertyValue);
			setVnfItemModelNameVersionId(namedQueryResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).extraProperties.extraProperty.get(4).propertyValue);			

			// serviceItem
			setServiceItemServiceInstanceId(namedQueryResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).serviceInstance.serviceInstanceID);
			setServiceItemPersonaModelId(namedQueryResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).serviceInstance.personaModelId);
			setServiceItemModelName(namedQueryResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).extraProperties.extraProperty.get(0).propertyValue);
			setServiceItemModelType(namedQueryResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).extraProperties.extraProperty.get(1).propertyValue);
			setServiceItemModelVersion(namedQueryResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).serviceInstance.personaModelVersion);
			setServiceItemModelNameVersionId(namedQueryResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).extraProperties.extraProperty.get(4).propertyValue);
			
			// Find the index for base vf module and non-base vf module
			int baseIndex = -1;
			int nonBaseIndex = -1;
			List<AAINQF199InventoryResponseItem> inventoryItems = namedQueryResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).items.inventoryResponseItems;
			for (AAINQF199InventoryResponseItem m : inventoryItems) {
				if (m.vfModule != null && m.vfModule.isBaseVfModule == true) {
					baseIndex = inventoryItems.indexOf(m);
				} else if (m.vfModule != null && m.vfModule.isBaseVfModule == false && m.vfModule.orchestrationStatus == null) {
					nonBaseIndex = inventoryItems.indexOf(m);
				}
				//
				if (baseIndex != -1 && nonBaseIndex != -1) {
					break;
				}
			}
			
			// Report the error if either base vf module or non-base vf module is not found
			if (baseIndex == -1 || nonBaseIndex == -1) {
				logger.error("Either base or non-base vf module is not found from AAI response.");
				return;
			}
			
			// This comes from the base module
			setVfModuleItemVfModuleName(namedQueryResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).items.inventoryResponseItems.get(baseIndex).vfModule.vfModuleName);
			setVfModuleItemVfModuleName(vfModuleItemVfModuleName.replace("Vfmodule", "vDNS"));

			// vfModuleItem - NOT the base module
			setVfModuleItemPersonaModelId(namedQueryResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).items.inventoryResponseItems.get(nonBaseIndex).vfModule.personaModelId);
			setVfModuleItemPersonaModelVersion(namedQueryResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).items.inventoryResponseItems.get(nonBaseIndex).vfModule.personaModelVersion);
			setVfModuleItemModelName(namedQueryResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).items.inventoryResponseItems.get(nonBaseIndex).extraProperties.extraProperty.get(0).propertyValue);
			setVfModuleItemModelNameVersionId(namedQueryResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(0).items.inventoryResponseItems.get(nonBaseIndex).extraProperties.extraProperty.get(4).propertyValue);
			
			// tenantItem
			setTenantItemTenantId(namedQueryResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(1).tenant.tenantId);

			// cloudRegionItem
			setCloudRegionItemCloudRegionId(namedQueryResponseWrapper.aainqf199response.inventoryResponseItems.get(0).items.inventoryResponseItems.get(1).items.inventoryResponseItems.get(0).cloudRegion.cloudRegionId);
		
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			VirtualControlLoopNotification notification = new VirtualControlLoopNotification(onset);
			notification.notification = ControlLoopNotificationType.REJECTED;
			notification.message = "Exception occurred " + e.getMessage();
			notification.policyName = onset.policyName;
			notification.policyScope = onset.policyScope;
			notification.policyVersion = onset.policyVersion;
			//
			try {
				logger.debug(Serialization.gsonPretty.toJson(notification));
			} catch (Exception e1) {
				logger.error("Can't deliver notification: " + notification);
				logger.error(e1.getMessage(), e1);
			}
			//
			notification.notification = ControlLoopNotificationType.FINAL_FAILURE;
			notification.message = "Invalid named-query response from AAI";
            //
            try {
            	logger.debug(Serialization.gsonPretty.toJson(notification));
            } catch (Exception e1) {
            	logger.error("Can't deliver notification: " + notification);
            	logger.error(e1.getMessage(), e1);
            }	
			// Retract everything
			return;
		}

		// Extracted fields should not be null
		if (checkExtractedFields() == false) {
			System.err.println("some fields are missing from AAI response.");
			return;
		}
	}
	
	/**
	 * Checks whether extracted fields from AAI Named Query are null or not
	 * @return false if some extracted fields are missing, true otherwise
	 */
	private boolean checkExtractedFields() {
		
		if ((getVnfItemVnfId() == null) || (getVnfItemVnfType() == null) ||
			    (getVnfItemModelInvariantId() == null) || (getVnfItemModelName() == null) ||
			    (getVnfItemModelVersion() == null) || (getVnfItemModelNameVersionId() == null) ||
			    (getServiceItemServiceInstanceId() == null) || (getServiceItemModelName() == null) ||
			    (getServiceItemModelType() == null) || (getServiceItemModelVersion() == null) ||
			    (getServiceItemModelNameVersionId() == null) || (getVfModuleItemVfModuleName() == null) ||
			    (getVfModuleItemPersonaModelId() == null) || (getVfModuleItemPersonaModelVersion() == null) ||
			    (getVfModuleItemModelName() == null) || (getVfModuleItemModelNameVersionId() == null) ||
			    (getTenantItemTenantId() == null) || (getCloudRegionItemCloudRegionId() == null)) {
				return false;
			}
		return true;
	}
	
	/**
	 * Construct SO Request
	 * 
	 * @param onset
	 * @param operation
	 * @param policy
	 * @return MSORequest
	 */
	public SORequest constructRequest(VirtualControlLoopEvent onset, ControlLoopOperation operation, Policy policy) {

		if (policy.getActor().equals("SO") && policy.getRecipe().equals("VF Module Create")) {
			// perform named query request and handle response
			AaiNamedQueryRequest(onset);
		} else {
			// for future extension
			return null;
		};
          
		// check if the fields extracted from named query response are 
		// not null so we can proceed with SO request
		if (checkExtractedFields() == false) {
			
			System.err.println("AAI response is missing some required fields. Cannot proceed with SO Request construction.");
			return null;
			
		} else {

			// Construct SO Request
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
			request.requestDetails.cloudConfiguration.lcpCloudRegionId = getCloudRegionItemCloudRegionId();
			request.requestDetails.cloudConfiguration.tenantId = getTenantItemTenantId();
			//
			// modelInfo
			//
			request.requestDetails.modelInfo.modelType = "vfModule";
			request.requestDetails.modelInfo.modelInvariantId = getVfModuleItemPersonaModelId();
			request.requestDetails.modelInfo.modelNameVersionId = getVfModuleItemModelNameVersionId();
			request.requestDetails.modelInfo.modelName = getVfModuleItemModelName();
			request.requestDetails.modelInfo.modelVersion = getVfModuleItemPersonaModelVersion();
			//
			// requestInfo
			//
			request.requestDetails.requestInfo.instanceName = getVfModuleItemVfModuleName();
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
			relatedInstanceListElement1.relatedInstance.instanceId = getServiceItemServiceInstanceId();
			relatedInstanceListElement1.relatedInstance.modelInfo = new SOModelInfo();
			relatedInstanceListElement1.relatedInstance.modelInfo.modelType = "service";
			relatedInstanceListElement1.relatedInstance.modelInfo.modelInvariantId = getServiceItemPersonaModelId();
			relatedInstanceListElement1.relatedInstance.modelInfo.modelNameVersionId = getServiceItemModelNameVersionId();
			relatedInstanceListElement1.relatedInstance.modelInfo.modelName = getServiceItemModelName();
			relatedInstanceListElement1.relatedInstance.modelInfo.modelVersion = getServiceItemModelVersion();
			//
			relatedInstanceListElement2.relatedInstance.instanceId = getVnfItemVnfId();
			relatedInstanceListElement2.relatedInstance.modelInfo = new SOModelInfo();
			relatedInstanceListElement2.relatedInstance.modelInfo.modelType = "vnf";
			relatedInstanceListElement2.relatedInstance.modelInfo.modelInvariantId = getVnfItemModelInvariantId();
			relatedInstanceListElement2.relatedInstance.modelInfo.modelNameVersionId = getVnfItemModelNameVersionId();
			relatedInstanceListElement2.relatedInstance.modelInfo.modelName = getVnfItemModelName();
			relatedInstanceListElement2.relatedInstance.modelInfo.modelVersion = getVnfItemModelVersion();
			relatedInstanceListElement2.relatedInstance.modelInfo.modelCustomizationName = getVnfItemVnfType();
			//	
			request.requestDetails.relatedInstanceList.add(relatedInstanceListElement1);
			request.requestDetails.relatedInstanceList.add(relatedInstanceListElement2);
			//
			// print MSO request for debug
			//
			logger.debug("MSO request sent:");
			logger.debug(Serialization.gsonPretty.toJson(request));
	
			return request;
		}
	}
	
	/**
	 * This method is needed to get the serviceInstanceId and vnfInstanceId which is used
	 * in the asyncMSORestCall 
	 * 
	 * @param wm
	 * @param request
	 */
	public static void sendRequest(WorkingMemory wm, Object request) {
		SOManager Mm = new SOManager();
		Mm.asyncMSORestCall(wm, getServiceItemServiceInstanceId(), getVnfItemVnfId(), (SORequest)request);
	}
		
	/**
	 * @return the vnfItemVnfId
	 */
	public static String getVnfItemVnfId() {
		return vnfItemVnfId;
	}

	/**
	 * @param vnfItemVnfId the vnfItemVnfId to set
	 */
	private void setVnfItemVnfId(String vnfItemVnfId) {
		this.vnfItemVnfId = vnfItemVnfId;
	}

	/**
	 * @return the vnfItemVnfType
	 */
	public String getVnfItemVnfType() {
		return this.vnfItemVnfType;
	}

	/**
	 * @param vnfItemVnfType the vnfItemVnfType to set
	 */
	private void setVnfItemVnfType(String vnfItemVnfType) {
		this.vnfItemVnfType = vnfItemVnfType;
	}

	/**
	 * @return the vnfItemPersonaModelId
	 */
	public String getVnfItemModelInvariantId() {
		return this.vnfItemModelInvariantId;
	}

	/**
	 * @param vnfItemPersonaModelId the vnfItemPersonaModelId to set
	 */
	private void setVnfItemModelInvariantId(String vnfItemModelInvariantId) {
		this.vnfItemModelInvariantId = vnfItemModelInvariantId;
	}

	/**
	 * @return the vnfItemModelVersionId
	 */
	public String getVnfItemModelVersionId() {
		return this.vnfItemModelVersionId;
	}

	/**
	 * @param vnfItemModelVersionId the vnfItemModelVersionId to set
	 */
	private void setVnfItemModelVersionId(String vnfItemModelVersionId) {
		this.vnfItemModelVersionId = vnfItemModelVersionId;
	}

	/**
	 * @return the vnfItemModelName
	 */
	public String getVnfItemModelName() {
		return this.vnfItemModelName;
	}

	/**
	 * @param vnfItemModelName the vnfItemModelName to set
	 */
	private void setVnfItemModelName(String vnfItemModelName) {
		this.vnfItemModelName = vnfItemModelName;
	}

	/**
	 * @return the vnfItemModelVersion
	 */
	public String getVnfItemModelVersion() {
		return this.vnfItemModelVersion;
	}

	/**
	 * @param vnfItemModelVersion the vnfItemModelVersion to set
	 */
	private void setVnfItemModelVersion(String vnfItemModelVersion) {
		this.vnfItemModelVersion = vnfItemModelVersion;
	}

	/**
	 * @return the vnfItemModelNameVersionId
	 */
	public String getVnfItemModelNameVersionId() {
		return this.vnfItemModelNameVersionId;
	}

	/**
	 * @param vnfItemModelNameVersionId the vnfItemModelNameVersionId to set
	 */
	private void setVnfItemModelNameVersionId(String vnfItemModelNameVersionId) {
		this.vnfItemModelNameVersionId = vnfItemModelNameVersionId;
	}

	/**
	 * @return the serviceItemServiceInstanceId
	 */
	public static String getServiceItemServiceInstanceId() {
		return serviceItemServiceInstanceId;
	}

	/**
	 * @param serviceItemServiceInstanceId the serviceItemServiceInstanceId to set
	 */
	private void setServiceItemServiceInstanceId(
			String serviceItemServiceInstanceId) {
		this.serviceItemServiceInstanceId = serviceItemServiceInstanceId;
	}

	/**
	 * @return the serviceItemPersonaModelId
	 */
	public String getServiceItemPersonaModelId() {
		return this.serviceItemPersonaModelId;
	}

	/**
	 * @param serviceItemPersonaModelId the serviceItemPersonaModelId to set
	 */
	private void setServiceItemPersonaModelId(String serviceItemPersonaModelId) {
		this.serviceItemPersonaModelId = serviceItemPersonaModelId;
	}

	/**
	 * @return the serviceItemModelName
	 */
	public String getServiceItemModelName() {
		return this.serviceItemModelName;
	}

	/**
	 * @param serviceItemModelName the serviceItemModelName to set
	 */
	private void setServiceItemModelName(String serviceItemModelName) {
		this.serviceItemModelName = serviceItemModelName;
	}

	/**
	 * @return the serviceItemModelType
	 */
	public String getServiceItemModelType() {
		return this.serviceItemModelType;
	}

	/**
	 * @param serviceItemModelType the serviceItemModelType to set
	 */
	private void setServiceItemModelType(String serviceItemModelType) {
		this.serviceItemModelType = serviceItemModelType;
	}

	/**
	 * @return the serviceItemModelVersion
	 */
	public String getServiceItemModelVersion() {
		return this.serviceItemModelVersion;
	}

	/**
	 * @param serviceItemModelVersion the serviceItemModelVersion to set
	 */
	private void setServiceItemModelVersion(String serviceItemModelVersion) {
		this.serviceItemModelVersion = serviceItemModelVersion;
	}

	/**
	 * @return the serviceItemModelNameVersionId
	 */
	public String getServiceItemModelNameVersionId() {
		return this.serviceItemModelNameVersionId;
	}

	/**
	 * @param serviceItemModelNameVersionId the serviceItemModelNameVersionId to set
	 */
	private void setServiceItemModelNameVersionId(
			String serviceItemModelNameVersionId) {
		this.serviceItemModelNameVersionId = serviceItemModelNameVersionId;
	}

	/**
	 * @return the vfModuleItemVfModuleName
	 */
	public String getVfModuleItemVfModuleName() {
		return this.vfModuleItemVfModuleName;
	}

	/**
	 * @param vfModuleItemVfModuleName the vfModuleItemVfModuleName to set
	 */
	private void setVfModuleItemVfModuleName(String vfModuleItemVfModuleName) {
		this.vfModuleItemVfModuleName = vfModuleItemVfModuleName;
	}

	/**
	 * @return the vfModuleItemPersonaModelId
	 */
	public String getVfModuleItemPersonaModelId() {
		return this.vfModuleItemPersonaModelId;
	}

	/**
	 * @param vfModuleItemPersonaModelId the vfModuleItemPersonaModelId to set
	 */
	private void setVfModuleItemPersonaModelId(String vfModuleItemPersonaModelId) {
		this.vfModuleItemPersonaModelId = vfModuleItemPersonaModelId;
	}

	/**
	 * @return the vfModuleItemPersonaModelVersion
	 */
	public String getVfModuleItemPersonaModelVersion() {
		return this.vfModuleItemPersonaModelVersion;
	}

	/**
	 * @param vfModuleItemPersonaModelVersion the vfModuleItemPersonaModelVersion to set
	 */
	private void setVfModuleItemPersonaModelVersion(
			String vfModuleItemPersonaModelVersion) {
		this.vfModuleItemPersonaModelVersion = vfModuleItemPersonaModelVersion;
	}

	/**
	 * @return the vfModuleItemModelName
	 */
	public String getVfModuleItemModelName() {
		return this.vfModuleItemModelName;
	}

	/**
	 * @param vfModuleItemModelName the vfModuleItemModelName to set
	 */
	private void setVfModuleItemModelName(String vfModuleItemModelName) {
		this.vfModuleItemModelName = vfModuleItemModelName;
	}

	/**
	 * @return the vfModuleItemModelNameVersionId
	 */
	public String getVfModuleItemModelNameVersionId() {
		return this.vfModuleItemModelNameVersionId;
	}

	/**
	 * @param vfModuleItemModelNameVersionId the vfModuleItemModelNameVersionId to set
	 */
	private void setVfModuleItemModelNameVersionId(
			String vfModuleItemModelNameVersionId) {
		this.vfModuleItemModelNameVersionId = vfModuleItemModelNameVersionId;
	}

	/**
	 * @return the tenantItemTenantId
	 */
	public String getTenantItemTenantId() {
		return this.tenantItemTenantId;
	}

	/**
	 * @param tenantItemTenantId the tenantItemTenantId to set
	 */
	private void setTenantItemTenantId(String tenantItemTenantId) {
		this.tenantItemTenantId = tenantItemTenantId;
	}

	/**
	 * @return the cloudRegionItemCloudRegionId
	 */
	public String getCloudRegionItemCloudRegionId() {
		return this.cloudRegionItemCloudRegionId;
	}

	/**
	 * @param cloudRegionItemCloudRegionId the cloudRegionItemCloudRegionId to set
	 */
	private void setCloudRegionItemCloudRegionId(
			String cloudRegionItemCloudRegionId) {
		this.cloudRegionItemCloudRegionId = cloudRegionItemCloudRegionId;
	}

}
