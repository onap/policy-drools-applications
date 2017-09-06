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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.onap.policy.aai.AAIGETVserverResponse;
import org.onap.policy.aai.AAIGETVnfResponse;
import org.onap.policy.aai.RelatedToPropertyItem;
import org.onap.policy.aai.RelationshipDataItem;
import org.onap.policy.aai.RelationshipData;
import org.onap.policy.aai.util.Serialization;
import org.onap.policy.aai.AAINQF199.AAINQF199Request;
import org.onap.policy.aai.AAINQF199.AAINQF199Response;
import org.onap.policy.rest.RESTManager;
import org.onap.policy.rest.RESTManager.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;

public final class AAIManager {
	private static final Logger logger = LoggerFactory.getLogger(AAIManager.class);
	
	public static AAINQF199Response	postQuery(String url, String username, String password, AAINQF199Request request, UUID requestID) {
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("X-FromAppId", "POLICY");
		headers.put("X-TransactionId", requestID.toString());
		headers.put("Accept", "application/json");
		
		url = url + "/aai/search/named-query";

		logger.debug("ESTManager.post before"); 
		Pair<Integer, String> httpDetails = RESTManager.post(url, username, password, headers, "application/json", Serialization.gsonPretty.toJson(request));
		logger.debug("ESTManager.post after"); 
		
		if (httpDetails == null) {
			System.out.println("AAI POST Null Response to " + url);
			return null;
		}
		
		System.out.println(url);
		System.out.println(httpDetails.a);
		System.out.println(httpDetails.b);
		if (httpDetails.a == 200) {
			try {
				AAINQF199Response response = Serialization.gsonPretty.fromJson(httpDetails.b, AAINQF199Response.class);
				return response;
			} catch (JsonSyntaxException e) {
				logger.error("postQuery threw: ", e);
				System.out.println("postQuery threw: " + e.toString());
			}
		}

		return null;
	}
	
	public static AAIGETVserverResponse getQueryByVserverName(String urlGet, String username, String password, UUID requestID, String key) {
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("X-FromAppId", "POLICY");
		headers.put("X-TransactionId", requestID.toString());
		headers.put("Accept", "application/json");
		
		urlGet = urlGet + key; 
		
		int attemptsLeft = 3;
		AAIGETVserverResponse responseGet = null;
		
		while(attemptsLeft-- > 0){
		
			Pair<Integer, String> httpDetailsGet = RESTManager.get(urlGet, username, password, headers);
			if (httpDetailsGet == null) {
				System.out.println("AAI GET Null Response to " + urlGet);
				return null;
			}
			
			System.out.println(urlGet);
			System.out.println(httpDetailsGet.a);
			System.out.println(httpDetailsGet.b);
			
			if (httpDetailsGet.a == 200) {
				try {
					responseGet = Serialization.gsonPretty.fromJson(httpDetailsGet.b, AAIGETVserverResponse.class);
					return responseGet;
				} catch (JsonSyntaxException e) {
					System.out.println("postQuery threw: " + e.toString());
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}

		}
		
		return null;
	}
	
	public static AAIGETVnfResponse getQueryByVnfID(String urlGet, String username, String password, UUID requestID, String key) {
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("X-FromAppId", "POLICY");
		headers.put("X-TransactionId", requestID.toString());
		headers.put("Accept", "application/json");
		
		urlGet = urlGet + key; 
		
		int attemptsLeft = 3;
		AAIGETVnfResponse responseGet = null;
		
		while(attemptsLeft-- > 0){
		
			Pair<Integer, String> httpDetailsGet = RESTManager.get(urlGet, username, password, headers);
			if (httpDetailsGet == null) {
				System.out.println("AAI GET Null Response to " + urlGet);
				return null;
			}
			
			System.out.println(urlGet);
			System.out.println(httpDetailsGet.a);
			System.out.println(httpDetailsGet.b);
			
			if (httpDetailsGet.a == 200) {
				try {
					responseGet = Serialization.gsonPretty.fromJson(httpDetailsGet.b, AAIGETVnfResponse.class);
					return responseGet;
				} catch (JsonSyntaxException e) {
					System.out.println("postQuery threw: " + e.toString());
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}

		}
		
		return null;
	}
	
	public static AAIGETVnfResponse getQueryByVnfName(String urlGet, String username, String password, UUID requestID, String key) {
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("X-FromAppId", "POLICY");
		headers.put("X-TransactionId", requestID.toString());
		headers.put("Accept", "application/json");
		
		urlGet = urlGet + key; 
		
		int attemptsLeft = 3;
		AAIGETVnfResponse responseGet = null;
		
		while(attemptsLeft-- > 0){
		
			Pair<Integer, String> httpDetailsGet = RESTManager.get(urlGet, username, password, headers);
			if (httpDetailsGet == null) {
				System.out.println("AAI GET Null Response to " + urlGet);
				return null;
			}
			
			System.out.println(urlGet);
			System.out.println(httpDetailsGet.a);
			System.out.println(httpDetailsGet.b);
			
			if (httpDetailsGet.a == 200) {
				try {
					responseGet = Serialization.gsonPretty.fromJson(httpDetailsGet.b, AAIGETVnfResponse.class);
					return responseGet;
				} catch (JsonSyntaxException e) {
					System.out.println("postQuery threw: " + e.toString());
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}

		}
		
		return null;
	}

	// Simulate a response 
	public static AAIGETVnfResponse getQueryByVnfID2(String urlGet, String username, String password, UUID requestID, String key) {
		AAIGETVnfResponse response = new AAIGETVnfResponse();
		
		response.vnfID = "83f674e8-7555-44d7-9a39-bdc3770b0491"; 
		response.vnfName = "lll_vnf_010317";
		response.vnfType = "Basa-122216-Service/VidVsamp12BaseVolume 1";
		response.serviceId = "a9a77d5a-123e-4ca2-9eb9-0b015d2ee0fb";
		response.orchestrationStatus = "Created";
		response.inMaint = "false";
		response.isClosedLoopDisabled = "false";
		response.resourceVersion = "1494001988835";
		response.modelInvariantId = "f18be3cd-d446-456e-9109-121d9b62feaa";
		  
       	RelationshipList relationshipList = new RelationshipList();
       	Relationship     relationship = new Relationship();
       	RelationshipData relationshipData = new RelationshipData();
       	RelationshipDataItem relationshipDataItem = new RelationshipDataItem();
       	
       	relationshipDataItem.relationshipKey   = "customer.global-customer-id"; 
       	relationshipDataItem.relationshipValue = "MSO_1610_ST"; 
       	relationshipData.relationshipData.add(relationshipDataItem);
       	
       	relationshipDataItem.relationshipKey   = "service-subscription.service-type"; 
       	relationshipDataItem.relationshipValue = "MSO-dev-service-type"; 
       	relationshipData.relationshipData.add(relationshipDataItem);
       	
       	relationshipDataItem.relationshipKey   = "service-instance.service-instance-id"; 
       	relationshipDataItem.relationshipValue = "e1e9c97c-02c0-4919-9b4c-eb5d5ef68970"; 
       	relationshipData.relationshipData.add(relationshipDataItem);
       	
       	RelatedToProperty relatedToProperty = new RelatedToProperty();
       	RelatedToPropertyItem item = new RelatedToPropertyItem();  
       	item.propertyKey = "service-instance.service-instance-name"; 
       	item.propertyValue = "lll_svc_010317"; 
       	relatedToProperty.relatedTo.add(item);
       	
        relationship.relatedTo = "service-instance";
        relationship.relatedLink = "/aai/v11/business/customers/customer/MSO_1610_ST/service-subscriptions/service-subscription/MSO-dev-service-type/service-instances/service-instance/e1e9c97c-02c0-4919-9b4c-eb5d5ef68970";
        relationship.relationshipData = relationshipData;
        relationship.relatedToProperty = relatedToProperty;
       
        relationshipList.relationshipList.add(relationship);
        response.relationshipList = relationshipList; 

		return response;
	}

	public static AAIGETVnfResponse getQueryByVnfName2(String urlGet, String username, String password, UUID requestID, String key) {
		AAIGETVnfResponse response = new AAIGETVnfResponse();
		
		response.vnfID = "83f674e8-7555-44d7-9a39-bdc3770b0491"; 
		response.vnfName = "lll_vnf_010317";
		response.vnfType = "Basa-122216-Service/VidVsamp12BaseVolume 1";
		response.serviceId = "a9a77d5a-123e-4ca2-9eb9-0b015d2ee0fb";
		response.orchestrationStatus = "Created";
		response.inMaint = "false";
		response.isClosedLoopDisabled = "false";
		response.resourceVersion = "1494001988835";
		response.modelInvariantId = "f18be3cd-d446-456e-9109-121d9b62feaa";
		  
       	RelationshipList relationshipList = new RelationshipList();
       	Relationship     relationship = new Relationship();
       	RelationshipData relationshipData = new RelationshipData();
       	RelationshipDataItem relationshipDataItem = new RelationshipDataItem();
       	
       	relationshipDataItem.relationshipKey   = "customer.global-customer-id"; 
       	relationshipDataItem.relationshipValue = "MSO_1610_ST"; 
       	relationshipData.relationshipData.add(relationshipDataItem);
       	
       	relationshipDataItem.relationshipKey   = "service-subscription.service-type"; 
       	relationshipDataItem.relationshipValue = "MSO-dev-service-type"; 
       	relationshipData.relationshipData.add(relationshipDataItem);
       	
       	relationshipDataItem.relationshipKey   = "service-instance.service-instance-id"; 
       	relationshipDataItem.relationshipValue = "e1e9c97c-02c0-4919-9b4c-eb5d5ef68970"; 
       	relationshipData.relationshipData.add(relationshipDataItem);
       	
       	RelatedToProperty relatedToProperty = new RelatedToProperty();
       	RelatedToPropertyItem item = new RelatedToPropertyItem();  
       	item.propertyKey = "service-instance.service-instance-name"; 
       	item.propertyValue = "lll_svc_010317"; 
       	relatedToProperty.relatedTo.add(item);
       	
        relationship.relatedTo = "service-instance";
        relationship.relatedLink = "/aai/v11/business/customers/customer/MSO_1610_ST/service-subscriptions/service-subscription/MSO-dev-service-type/service-instances/service-instance/e1e9c97c-02c0-4919-9b4c-eb5d5ef68970";
        relationship.relationshipData = relationshipData;
        relationship.relatedToProperty = relatedToProperty;
       
        relationshipList.relationshipList.add(relationship);
        response.relationshipList = relationshipList; 

		return response;
	}

	public static AAIGETVserverResponse getQueryByVserverName2(String urlGet, String username, String password, UUID requestID, String key) {
		AAIGETVserverResponse response = new AAIGETVserverResponse();
		
		response.vserverID = "d0668d4f-c25e-4a1b-87c4-83845c01efd8"; 
		response.vserverName = "USMSO1SX7NJ0103UJZZ01-vjunos0"; 
		response.vserverName2 = "vjunos0"; 
		response.vserverSelflink = "https://aai-ext1.test.att.com:8443/aai/v7/cloud-infrastructure/cloud-regions/cloud-region/att-aic/AAIAIC25/tenants/tenant/USMSO1SX7NJ0103UJZZ01%3A%3AuCPE-VMS/vservers/vserver/d0668d4f-c25e-4a1b-87c4-83845c01efd8"; 
		response.inMaint = "false"; 
		response.isClosedLoopDisabled = "false"; 
		response.resourceVersion = "1494001931513"; 
		  
       	RelationshipList relationshipList = new RelationshipList();
       	Relationship     relationship = new Relationship();
       	RelationshipData relationshipData = new RelationshipData();
       	RelationshipDataItem relationshipDataItem = new RelationshipDataItem();
       	
       	relationshipDataItem.relationshipKey   = "customer.global-customer-id"; 
       	relationshipDataItem.relationshipValue = "MSO_1610_ST"; 
       	relationshipData.relationshipData.add(relationshipDataItem);
       	
       	relationshipDataItem.relationshipKey   = "service-subscription.service-type"; 
       	relationshipDataItem.relationshipValue = "MSO-dev-service-type"; 
       	relationshipData.relationshipData.add(relationshipDataItem);
       	
       	relationshipDataItem.relationshipKey   = "service-instance.service-instance-id"; 
       	relationshipDataItem.relationshipValue = "e1e9c97c-02c0-4919-9b4c-eb5d5ef68970"; 
       	relationshipData.relationshipData.add(relationshipDataItem);
       	
       	RelatedToProperty relatedToProperty = new RelatedToProperty();
       	RelatedToPropertyItem item = new RelatedToPropertyItem();  
       	item.propertyKey = "service-instance.service-instance-name"; 
       	item.propertyValue = "lll_svc_010317"; 
       	relatedToProperty.relatedTo.add(item);
       	
        relationship.relatedTo = "service-instance";
        relationship.relatedLink = "/aai/v11/business/customers/customer/MSO_1610_ST/service-subscriptions/service-subscription/MSO-dev-service-type/service-instances/service-instance/e1e9c97c-02c0-4919-9b4c-eb5d5ef68970";
        relationship.relationshipData = relationshipData;
        relationship.relatedToProperty = relatedToProperty;
       
        relationshipList.relationshipList.add(relationship);
        response.relationshipList = relationshipList; 

		return response;
	}
}
