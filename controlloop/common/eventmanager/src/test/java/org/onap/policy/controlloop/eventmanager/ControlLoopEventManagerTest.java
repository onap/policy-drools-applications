/*-
 * ============LICENSE_START=======================================================
 * unit test
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

package org.onap.policy.controlloop.eventmanager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.onap.policy.aai.AAIGETVserverResponse;
import org.onap.policy.aai.AAIGETVnfResponse;
import org.onap.policy.aai.RelatedToPropertyItem;
import org.onap.policy.aai.Relationship;
import org.onap.policy.aai.RelationshipData;
import org.onap.policy.aai.RelationshipDataItem;
import org.onap.policy.aai.RelationshipList;
import org.onap.policy.aai.AAIManager;
import org.onap.policy.aai.RelatedToProperty;
import org.onap.policy.appc.Request;
import org.onap.policy.appc.Response;
import org.onap.policy.appc.ResponseCode;
import org.onap.policy.appc.ResponseValue;
import org.onap.policy.controlloop.ControlLoopEventStatus;

import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.Util;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.controlloop.processor.ControlLoopProcessor;

public class ControlLoopEventManagerTest {
	
	private static VirtualControlLoopEvent onset;
	static {
		onset = new VirtualControlLoopEvent();
		onset.closedLoopControlName = "ControlLoop-vUSP"; 
		onset.requestID = UUID.randomUUID();
		onset.target = "VM_NAME";
		onset.closedLoopAlarmStart = Instant.now();
		onset.AAI = new HashMap<String, String>();
		onset.AAI.put("cloud-region.identity-url", "foo");
		onset.AAI.put("vserver.selflink", "bar");
		//onset.AAI.put("vserver.is-closed-loop-disabled", "false");
		onset.AAI.put("generic-vnf.vnf-id", "83f674e8-7555-44d7-9a39-bdc3770b0491");
		onset.closedLoopEventStatus = ControlLoopEventStatus.ONSET;
	}
	
	@Test
	public void testGetAAIInfo() {
		final Util.Pair<ControlLoopPolicy, String> pair = Util.loadYaml("src/test/resources/test.yaml");
		onset.closedLoopControlName = pair.a.getControlLoop().getControlLoopName();
		
		try {
			System.out.println("1 - testGetAAIVnfInfo started");
			ControlLoopEventManager eventManager = new ControlLoopEventManager(onset.closedLoopControlName, onset.requestID);
			onset.closedLoopEventStatus = ControlLoopEventStatus.ONSET; 
			 
			AAIGETVnfResponse response = eventManager.getAAIVnfInfo(onset); // vnf-id 
			System.out.println("testGetAAIInfo end ...");
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testIsClosedLoopDisabled() {
		//
		// Load up the policy
		//
		final Util.Pair<ControlLoopPolicy, String> pair = Util.loadYaml("src/test/resources/test.yaml");
		onset.closedLoopControlName = pair.a.getControlLoop().getControlLoopName();
		
		try {
			System.out.println("2- testIsClosedLoopDisabled started");
			ControlLoopEventManager eventManager = new ControlLoopEventManager(onset.closedLoopControlName, onset.requestID);
			onset.closedLoopEventStatus = ControlLoopEventStatus.ONSET; 
			
			System.out.println("-- manager.getQuery - vnfID");
			AAIManager manager = new AAIManager(); 
			String user = "POLICY";
			String password = "POLICY";
			String vnfID = "83f674e8-7555-44d7-9a39-bdc3770b0491";
			String url = "https://aai-ext1.test.att.com:8443/aai/v11/network/generic-vnfs/generic-vnf/"; 
			//AAIGETVnfResponse response = manager.getQueryByVnfID(url, user, password, onset.requestID, vnfID); // ???
			AAIGETVnfResponse response = getQueryByVnfID2(url, user, password, onset.requestID, vnfID); // ???
			if (response != null) {
				System.out.println("aaiResponse is NOT null");
				boolean disabled = eventManager.isClosedLoopDisabled(response);
				System.out.println("isClosedLoopDisabled: " + disabled);
			} else {
				System.out.println("aaiResponse IS null");
			}	
            //
			System.out.println("-- manager.getQuery - vnfName");
			String vnfName = "lll_vnf_010317";
			url = "https://aai-ext1.test.att.com:8443/aai/v11/network/generic-vnfs/generic-vnf?vnf-name="; 
			//response = manager.getQueryByVnfName(url, user, password, onset.requestID, vnfName); // ???
			response = getQueryByVnfName2(url, user, password, onset.requestID, vnfName); // ???
			if (response != null) {
				System.out.println("aaiResponse is NOT null");
				boolean disabled = eventManager.isClosedLoopDisabled(response);
				System.out.println("isClosedLoopDisabled: " + disabled);
			} else {
				System.out.println("aaiResponse IS null");
			}
			//
			System.out.println("-- manager.getQuery - vserverName");
			String vserverName = "USMSO1SX7NJ0103UJZZ01-vjunos0";
			url = "https://aai-ext1.test.att.com:8443//aai/v11/nodes/vservers?vserver-name="; 
			//AAIGETVserverResponse response2 = manager.getQueryByVserverName(url, user, password, onset.requestID, vserverName); // ???
			AAIGETVserverResponse response2 = getQueryByVserverName2(url, user, password, onset.requestID, vserverName); // ???
			if (response2 != null) {
				System.out.println("aaiResponse is NOT null");
				boolean disabled = eventManager.isClosedLoopDisabled(response);
				System.out.println("isClosedLoopDisabled: " + disabled);
			} else {
				System.out.println("aaiResponse IS null");
			}			
			System.out.println("testIsClosedLoopDisabled end");

		} catch (Exception e) {
			fail(e.getMessage());
		}
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