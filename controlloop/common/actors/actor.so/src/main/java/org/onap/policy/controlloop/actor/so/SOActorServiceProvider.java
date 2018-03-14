/*-
 * ============LICENSE_START=======================================================
 * SOActorServiceProvider
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

package org.onap.policy.controlloop.actor.so;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.drools.core.WorkingMemory;
import org.onap.policy.aai.AAIManager;
import org.onap.policy.aai.AAINQExtraProperties;
import org.onap.policy.aai.AAINQExtraProperty;
import org.onap.policy.aai.AAINQInstanceFilters;
import org.onap.policy.aai.AAINQInventoryResponseItem;
import org.onap.policy.aai.AAINQNamedQuery;
import org.onap.policy.aai.AAINQQueryParameters;
import org.onap.policy.aai.AAINQRequest;
import org.onap.policy.aai.AAINQResponse;
import org.onap.policy.aai.AAINQResponseWrapper;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actorserviceprovider.spi.Actor;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.rest.RESTManager;
import org.onap.policy.so.SOCloudConfiguration;
import org.onap.policy.so.SOManager;
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class SOActorServiceProvider implements Actor {
	private static final Logger logger = LoggerFactory.getLogger(SOActorServiceProvider.class);

	// Strings for SO Actor
	private static final String SO_ACTOR  = "SO";

	// Strings for targets
	private static final String TARGET_VFC = "VFC";

	// Strings for recipes
	private static final String RECIPE_VF_MODULE_CREATE = "VF Module Create";

	private static final ImmutableList<String> recipes = ImmutableList.of(RECIPE_VF_MODULE_CREATE);
	private static final ImmutableMap<String, List<String>> targets = new ImmutableMap.Builder<String, List<String>>()
			.put(RECIPE_VF_MODULE_CREATE, ImmutableList.of(TARGET_VFC))
			.build();

	// Static variables required to hold the IDs of the last service item and VNF item. Note that in a multithreaded deployment this WILL break
	private static String lastVNFItemVnfId;
	private static String lastServiceItemServiceInstanceId;

	@Override
	public String actor() {
		return SO_ACTOR;
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
	 * Constructs a SO request conforming to the lcm API.
	 * The actual request is constructed and then placed in a 
	 * wrapper object used to send through DMAAP.
	 * 
	 * @param onset
	 *            the event that is reporting the alert for policy
	 *            to perform an action        
	 * @param operation
	 *            the control loop operation specifying the actor,
	 *            operation, target, etc.  
	 * @param policy
	 *            the policy the was specified from the yaml generated
	 *            by CLAMP or through the Policy GUI/API                        
	 * @return a SO request conforming to the lcm API using the DMAAP wrapper
	 */
	public SORequest constructRequest(VirtualControlLoopEvent onset, ControlLoopOperation operation, Policy policy) {
		String modelNamePropertyKey = "model-ver.model-name";
		String modelVersionPropertyKey = "model-ver.model-version";
		String modelVersionIdPropertyKey = "model-ver.model-version-id";
		
		
		if (!SO_ACTOR.equals(policy.getActor()) || !RECIPE_VF_MODULE_CREATE.equals(policy.getRecipe())) {
			// for future extension
			return null;
		}

		// Perform named query request and handle response
		AAINQResponseWrapper aaiResponseWrapper = performAaiNamedQueryRequest(onset);
		if (aaiResponseWrapper == null) {
			// Tracing and error handling handied in the "performAaiNamedQueryRequest()" method
			return null;
		}

		AAINQInventoryResponseItem vnfItem;
		AAINQInventoryResponseItem vnfServiceItem;
		AAINQInventoryResponseItem tenantItem;

		// Extract the items we're interested in from the response
		try 	{
			vnfItem = aaiResponseWrapper.getAainqresponse().getInventoryResponseItems().get(0).getItems().getInventoryResponseItems().get(0);
		}
		catch (Exception e) {
			logger.error("VNF Item not found in AAI response {}", Serialization.gsonPretty.toJson(aaiResponseWrapper), e);
			return null;
		}

		try 	{
			vnfServiceItem = vnfItem.getItems().getInventoryResponseItems().get(0);
		}
		catch (Exception e) {
			logger.error("VNF Service Item not found in AAI response {}", Serialization.gsonPretty.toJson(aaiResponseWrapper), e);
			return null;
		}

		try 	{
			tenantItem = aaiResponseWrapper.getAainqresponse().getInventoryResponseItems().get(0).getItems().getInventoryResponseItems().get(1);
		}
		catch (Exception e) {
			logger.error("Tenant Item not found in AAI response {}", Serialization.gsonPretty.toJson(aaiResponseWrapper), e);
			return null;
		}

		// Find the index for base vf module and non-base vf module
		int baseIndex = findIndex(vnfItem.getItems().getInventoryResponseItems(), true);
		int nonBaseIndex = findIndex(vnfItem.getItems().getInventoryResponseItems(), false);

		// Report the error if either base vf module or non-base vf module is not found
		if (baseIndex == -1 || nonBaseIndex == -1) {
			logger.error("Either base or non-base vf module is not found from AAI response.");
			return null;
		}
		
		// Grab some frequently used 

		// Construct SO Request
		SORequest request = new SORequest();
		request.setRequestId(onset.getRequestID());
		request.setRequestDetails(new SORequestDetails());
		request.getRequestDetails().setModelInfo(new SOModelInfo());
		request.getRequestDetails().setCloudConfiguration(new SOCloudConfiguration());
		request.getRequestDetails().setRequestInfo(new SORequestInfo());
		request.getRequestDetails().setRequestParameters(new SORequestParameters());
		request.getRequestDetails().getRequestParameters().setUserParams(null);

		//
		// cloudConfiguration
		//
		request.getRequestDetails().getCloudConfiguration().setTenantId(tenantItem.getTenant().getTenantId());
		request.getRequestDetails().getCloudConfiguration().setLcpCloudRegionId(tenantItem.getItems().getInventoryResponseItems().get(0).getCloudRegion().getCloudRegionId());

		//
		// modelInfo
		//
		AAINQInventoryResponseItem vfModuleItem = vnfItem.getItems().getInventoryResponseItems().get(nonBaseIndex);

		request.getRequestDetails().getModelInfo().setModelType("vfModule");
		request.getRequestDetails().getModelInfo().setModelInvariantId(vfModuleItem.getVfModule().getModelInvariantId());
		request.getRequestDetails().getModelInfo().setModelVersionId(vfModuleItem.getVfModule().getModelVersionId());

		for (AAINQExtraProperty prop : vfModuleItem.getExtraProperties().getExtraProperty()) {
			if (prop.getPropertyName().equals(modelNamePropertyKey)) {
				request.getRequestDetails().getModelInfo().setModelName(prop.getPropertyValue());
			}
			else if (prop.getPropertyName().equals(modelVersionPropertyKey)) {
				request.getRequestDetails().getModelInfo().setModelVersion(prop.getPropertyValue());
			}
			else {
				continue;
			}
		}
		
		//
		// requestInfo
		//
		String instanceName = vnfItem.getItems().getInventoryResponseItems().get(baseIndex).getVfModule()
				.getVfModuleName().replace("Vfmodule", "vDNS");
		int numberOfNonBaseModules = findNonBaseModules(vnfItem.getItems().getInventoryResponseItems());
		// Code to create unique VF Module names across the invocations.
		if (numberOfNonBaseModules == 1) {
			int instanceNumber = 1;
			instanceName = instanceName.concat("-").concat(String.valueOf(instanceNumber));
			request.getRequestDetails().getRequestInfo().setInstanceName(instanceName);
		} else if (numberOfNonBaseModules > 1) {
			int instanceNumber = numberOfNonBaseModules + 1;
			instanceName = instanceName.concat("-").concat(String.valueOf(instanceNumber));
			request.getRequestDetails().getRequestInfo().setInstanceName(instanceName);
		} else {
			request.getRequestDetails().getRequestInfo().setInstanceName(vnfItem.getItems().getInventoryResponseItems()
					.get(baseIndex).getVfModule().getVfModuleName().replace("Vfmodule", "vDNS"));
		}
		request.getRequestDetails().getRequestInfo().setSource("POLICY");
		request.getRequestDetails().getRequestInfo().setSuppressRollback(false);
		request.getRequestDetails().getRequestInfo().setRequestorId("policy");

		//
		// relatedInstanceList
		//
		SORelatedInstanceListElement relatedInstanceListElement1 = new SORelatedInstanceListElement();
		SORelatedInstanceListElement relatedInstanceListElement2 = new SORelatedInstanceListElement();
		relatedInstanceListElement1.setRelatedInstance(new SORelatedInstance());
		relatedInstanceListElement2.setRelatedInstance(new SORelatedInstance());

		// Service Item
		relatedInstanceListElement1.getRelatedInstance().setInstanceId(vnfServiceItem.getServiceInstance().getServiceInstanceID());
		relatedInstanceListElement1.getRelatedInstance().setModelInfo(new SOModelInfo());
		relatedInstanceListElement1.getRelatedInstance().getModelInfo().setModelType("service");
		relatedInstanceListElement1.getRelatedInstance().getModelInfo().setModelInvariantId(vnfServiceItem.getServiceInstance().getModelInvariantId());
		relatedInstanceListElement1.getRelatedInstance().getModelInfo().setModelVersionId(vnfServiceItem.getServiceInstance().getModelVersionId());
		for (AAINQExtraProperty prop : vnfServiceItem.getExtraProperties().getExtraProperty()) {
			if (prop.getPropertyName().equals(modelNamePropertyKey)) {
				relatedInstanceListElement1.getRelatedInstance().getModelInfo().setModelName(prop.getPropertyValue());
			}
			else if (prop.getPropertyName().equals(modelVersionPropertyKey)) {
				relatedInstanceListElement1.getRelatedInstance().getModelInfo().setModelVersion(prop.getPropertyValue());
			}
			else {
				continue;
			}
		}

		// VNF Item
		relatedInstanceListElement2.getRelatedInstance().setInstanceId(vnfItem.getGenericVNF().getVnfID());
		relatedInstanceListElement2.getRelatedInstance().setModelInfo(new SOModelInfo());
		relatedInstanceListElement2.getRelatedInstance().getModelInfo().setModelType("vnf");
		relatedInstanceListElement2.getRelatedInstance().getModelInfo().setModelInvariantId(vnfItem.getGenericVNF().getModelInvariantId());
		for (AAINQExtraProperty prop : vnfItem.getExtraProperties().getExtraProperty()) {
			if (prop.getPropertyName().equals(modelNamePropertyKey)) {
				relatedInstanceListElement2.getRelatedInstance().getModelInfo().setModelName(prop.getPropertyValue());
			}
			else if (prop.getPropertyName().equals(modelVersionPropertyKey)) {
				relatedInstanceListElement2.getRelatedInstance().getModelInfo().setModelVersion(prop.getPropertyValue());
			}
			else if (prop.getPropertyName().equals(modelVersionIdPropertyKey)) {
				relatedInstanceListElement2.getRelatedInstance().getModelInfo().setModelVersionId(prop.getPropertyValue());
			}			
			else {
				continue;
			}
		}		
		relatedInstanceListElement2.getRelatedInstance().getModelInfo().setModelCustomizationName(vnfItem.getGenericVNF().getVnfType().substring(vnfItem.getGenericVNF().getVnfType().lastIndexOf('/') + 1));

		// Insert the Service Item and VNF Item	
		request.getRequestDetails().getRelatedInstanceList().add(relatedInstanceListElement1);
		request.getRequestDetails().getRelatedInstanceList().add(relatedInstanceListElement2);

		// Save the instance IDs for the VNF and service to static fields
		preserveInstanceIDs(vnfItem.getGenericVNF().getVnfID(), vnfServiceItem.getServiceInstance().getServiceInstanceID());

		if (logger.isDebugEnabled()) {
			logger.debug("SO request sent: {}", Serialization.gsonPretty.toJson(request));
		}

		return request;
	}

	/**
	 * This method is needed to get the serviceInstanceId and vnfInstanceId which is used
	 * in the asyncSORestCall 
	 * 
	 * @param wm
	 * @param request
	 */
	public static void sendRequest(String requestID, WorkingMemory wm, Object request) {
		SOManager soManager = new SOManager();
		soManager.asyncSORestCall(requestID, wm, lastServiceItemServiceInstanceId, lastVNFItemVnfId, (SORequest)request);
	}

	/**
	 * Constructs and sends an AAI vserver Named Query
	 * 
	 * @param onset
	 * @returns the response to the AAI Named Query
	 */
	private AAINQResponseWrapper performAaiNamedQueryRequest(VirtualControlLoopEvent onset) {

		// create AAI named-query request with UUID started with ""
		AAINQRequest aainqrequest = new AAINQRequest();
		AAINQQueryParameters aainqqueryparam = new AAINQQueryParameters();
		AAINQNamedQuery aainqnamedquery = new AAINQNamedQuery();
		AAINQInstanceFilters aainqinstancefilter = new AAINQInstanceFilters();

		// queryParameters
		aainqnamedquery.setNamedQueryUUID(UUID.fromString("4ff56a54-9e3f-46b7-a337-07a1d3c6b469")); // UUID.fromString($params.getAaiNamedQueryUUID()) TO DO: AaiNamedQueryUUID 
		aainqqueryparam.setNamedQuery(aainqnamedquery);
		aainqrequest.setQueryParameters(aainqqueryparam);
		//
		// instanceFilters
		//
		Map<String, Map<String, String>> aainqinstancefiltermap = new HashMap<>();
		Map<String, String> aainqinstancefiltermapitem = new HashMap<>();
		aainqinstancefiltermapitem.put("vserver-name", onset.getAAI().get("vserver.vserver-name")); // TO DO: get vserver.vname from dcae onset.AAI.get("vserver.vserver-name")
		aainqinstancefiltermap.put("vserver", aainqinstancefiltermapitem);
		aainqinstancefilter.getInstanceFilter().add(aainqinstancefiltermap);
		aainqrequest.setInstanceFilters(aainqinstancefilter);

		if (logger.isDebugEnabled()) {
			logger.debug("AAI Request sent: {}", Serialization.gsonPretty.toJson(aainqrequest));
		}

		AAINQResponse aainqresponse = new AAIManager(new RESTManager()).postQuery(
				getPEManagerEnvProperty("aai.url"),
				getPEManagerEnvProperty("aai.username"),
				getPEManagerEnvProperty("aai.password"),
				aainqrequest, onset.getRequestID());

		// Check AAI response
		if (aainqresponse == null) {
			logger.warn("No response received from AAI for request {}", aainqrequest);
			return null;
		}

		// Create AAINQResponseWrapper
		AAINQResponseWrapper aainqResponseWrapper = new AAINQResponseWrapper(onset.getRequestID(), aainqresponse);

		if (logger.isDebugEnabled()) {
			logger.debug("AAI Named Query Response: ");
			logger.debug(Serialization.gsonPretty.toJson(aainqResponseWrapper.getAainqresponse()));
		}

		return aainqResponseWrapper;
	}

	/**
	 * Find the base index or non base index in a list of inventory response items
	 * @param inventoryResponseItems
	 * @param baseIndexFlag true if we are searching for the base index, false if we are searching for hte non base index
	 * @return the base or non base index or -1 if the index was not found
	 */
	private int findIndex(List<AAINQInventoryResponseItem> inventoryResponseItems, boolean baseIndexFlag) {
		for (AAINQInventoryResponseItem invenoryResponseItem : inventoryResponseItems) {
			if (invenoryResponseItem.getVfModule() != null && baseIndexFlag == invenoryResponseItem.getVfModule().getIsBaseVfModule()) {
				return inventoryResponseItems.indexOf(invenoryResponseItem);
			}
		}

		return -1;
	}
	
	/**
	 * Find the number of non base modules present in API response object.
	 * 
	 * @param inventoryResponseItems
	 * @return number of non base index modules
	 */

	private int findNonBaseModules(List<AAINQInventoryResponseItem> inventoryResponseItems) {
		int nonBaseModuleCount = 0;
		for (AAINQInventoryResponseItem invenoryResponseItem : inventoryResponseItems) {
			if (invenoryResponseItem.getVfModule() != null
					&& (!invenoryResponseItem.getVfModule().getIsBaseVfModule())) {
				nonBaseModuleCount++;
			}
		}
		return nonBaseModuleCount;
	}

	/**
	 * This method is called to remember the last service instance ID and VNF Item VNF ID. Note these fields are static, beware for multithreaded deployments
	 * @param vnfInstanceID update the last VNF instance ID to this value
	 * @param serviceInstanceID update the last service instance ID to this value
	 */
	private static void preserveInstanceIDs(final String vnfInstanceID, final String serviceInstanceID) {
		lastVNFItemVnfId = vnfInstanceID;
		lastServiceItemServiceInstanceId = serviceInstanceID;
	}

	/**
	 * This method reads and validates environmental properties coming from the policy engine. Null properties cause
	 * an {@link IllegalArgumentException} runtime exception to be thrown 
	 * @param string the name of the parameter to retrieve
	 * @return the property value
	 */
	private static String getPEManagerEnvProperty(String enginePropertyName) {
		String enginePropertyValue = PolicyEngine.manager.getEnvironmentProperty(enginePropertyName);
		if (enginePropertyValue == null) {
			throw new IllegalArgumentException("The value of policy engine manager environment property \"" + enginePropertyName + "\" may not be null");
		}
		return enginePropertyValue;
	}
}
