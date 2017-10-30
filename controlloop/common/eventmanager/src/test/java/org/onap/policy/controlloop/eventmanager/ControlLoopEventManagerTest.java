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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.aai.AAIGETVnfResponse;
import org.onap.policy.aai.AAIGETVserverResponse;
import org.onap.policy.aai.RelatedToProperty;
import org.onap.policy.aai.RelatedToPropertyItem;
import org.onap.policy.aai.Relationship;
import org.onap.policy.aai.RelationshipData;
import org.onap.policy.aai.RelationshipDataItem;
import org.onap.policy.aai.RelationshipList;
import org.onap.policy.aai.util.AAIException;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.Util;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.drools.http.server.HttpServletServer;
import org.onap.policy.drools.system.PolicyEngine; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlLoopEventManagerTest {
	private static final Logger logger = LoggerFactory.getLogger(ControlLoopEventManagerTest.class);
	
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
		onset.AAI.put("generic-vnf.vnf-id", "83f674e8-7555-44d7-9a39-bdc3770b0491");
		onset.closedLoopEventStatus = ControlLoopEventStatus.ONSET;
	}
	
	@BeforeClass
	public static void setUpSimulator() {
		try {
			org.onap.policy.simulators.Util.buildAaiSim();
		} catch (Exception e) {
			fail(e.getMessage());
		}
		PolicyEngine.manager.setEnvironmentProperty("aai.username", "AAI");
		PolicyEngine.manager.setEnvironmentProperty("aai.password", "AAI");
		PolicyEngine.manager.setEnvironmentProperty("aai.url", "http://localhost:6666");
	}

	@AfterClass
	public static void tearDownSimulator() {
		HttpServletServer.factory.destroy();
	}
	
	@Test
	public void testAAIVnfInfo() {
		final Util.Pair<ControlLoopPolicy, String> pair = Util.loadYaml("src/test/resources/test.yaml");
		onset.closedLoopControlName = pair.a.getControlLoop().getControlLoopName();
		try {			
			AAIGETVnfResponse response = getQueryByVnfID2(PolicyEngine.manager.getEnvironmentProperty("aai.url") + "/aai/v11/network/generic-vnfs/generic-vnf/", 
					PolicyEngine.manager.getEnvironmentProperty("aai.username"), 
					PolicyEngine.manager.getEnvironmentProperty("aai.password"), 
					UUID.randomUUID(), "5e49ca06-2972-4532-9ed4-6d071588d792");
			assertNotNull(response);
			logger.info("testAAIVnfInfo test result is " + (response == null ? "null" : "not null"));
		} catch (Exception e) {
			logger.error("testAAIVnfInfo Exception: ", e);
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testAAIVnfInfo2() {
		final Util.Pair<ControlLoopPolicy, String> pair = Util.loadYaml("src/test/resources/test.yaml");
		onset.closedLoopControlName = pair.a.getControlLoop().getControlLoopName();
		try {
			AAIGETVnfResponse response = getQueryByVnfName2(PolicyEngine.manager.getEnvironmentProperty("aai.url") + "/aai/v11/network/generic-vnfs/generic-vnf?vnf-name=", 
					PolicyEngine.manager.getEnvironmentProperty("aai.username"), 
					PolicyEngine.manager.getEnvironmentProperty("aai.password"), 
					UUID.randomUUID(), "lll_vnf_010317");	
			assertNotNull(response);
			logger.info("testAAIVnfInfo2 test result is " + (response == null ? "null" : "not null"));
		} catch (Exception e) {
			logger.error("testAAIVnfInfo2 Exception: ", e);
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testAAIVserver() {
		final Util.Pair<ControlLoopPolicy, String> pair = Util.loadYaml("src/test/resources/test.yaml");
		onset.closedLoopControlName = pair.a.getControlLoop().getControlLoopName();
		try {
			AAIGETVserverResponse response = getQueryByVserverName2(PolicyEngine.manager.getEnvironmentProperty("aai.url") + "/aai/v11/nodes/vservers?vserver-name=", 
					PolicyEngine.manager.getEnvironmentProperty("aai.username"), 
					PolicyEngine.manager.getEnvironmentProperty("aai.password"), 
					UUID.randomUUID(), "USMSO1SX7NJ0103UJZZ01-vjunos0");
			assertNotNull(response);
			logger.info("testAAIVserver test result is " + (response == null ? "null" : "not null"));
		} catch (Exception e) {
			logger.error("testAAIVserver Exception: ", e);
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
			logger.info("testIsClosedLoopDisabled --");
			AAIGETVnfResponse response = getQueryByVnfID2(PolicyEngine.manager.getEnvironmentProperty("aai.url") + "/aai/v11/network/generic-vnfs/generic-vnf/", 
					PolicyEngine.manager.getEnvironmentProperty("aai.username"), 
					PolicyEngine.manager.getEnvironmentProperty("aai.password"), 
					UUID.randomUUID(), "5e49ca06-2972-4532-9ed4-6d071588d792");
			assertNotNull(response);
			boolean disabled = ControlLoopEventManager.isClosedLoopDisabled(response);
			logger.info("QueryByVnfID - isClosedLoopDisabled: " + disabled); 

			response = getQueryByVnfName2(PolicyEngine.manager.getEnvironmentProperty("aai.url") + "/aai/v11/network/generic-vnfs/generic-vnf?vnf-name=", 
					PolicyEngine.manager.getEnvironmentProperty("aai.username"), 
					PolicyEngine.manager.getEnvironmentProperty("aai.password"), 
					UUID.randomUUID(), "lll_vnf_010317");			
			assertNotNull(response);
			disabled = ControlLoopEventManager.isClosedLoopDisabled(response);
			logger.info("QueryByVnfName - isClosedLoopDisabled: " + disabled); 

			AAIGETVserverResponse response2 = getQueryByVserverName2(PolicyEngine.manager.getEnvironmentProperty("aai.url") + "/aai/v11/nodes/vservers?vserver-name=", 
					PolicyEngine.manager.getEnvironmentProperty("aai.user"), 
					PolicyEngine.manager.getEnvironmentProperty("aai.password"), 
					UUID.randomUUID(), "USMSO1SX7NJ0103UJZZ01-vjunos0");
			assertNotNull(response2);
			disabled = ControlLoopEventManager.isClosedLoopDisabled(response2);
			logger.info("QueryByVserverName - isClosedLoopDisabled: " + disabled); 
		} catch (Exception e) {
			fail(e.getMessage());
		}
 	}
	
	@Test
	public void abatemetCheckEventSyntaxTest() {
		VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.closedLoopControlName = "abatementAAI";
        event.requestID = UUID.randomUUID();
        event.target = "generic-vnf.vnf-id";
        event.closedLoopAlarmStart = Instant.now();
        event.closedLoopEventStatus = ControlLoopEventStatus.ABATED;
        ControlLoopEventManager manager = new ControlLoopEventManager(event.closedLoopControlName, event.requestID);
        assertNull(manager.getVnfResponse());
        assertNull(manager.getVserverResponse());
        try {
			manager.checkEventSyntax(event);
		} catch (ControlLoopException e) {
			logger.debug("ControlLoopException in abatemetCheckEventSyntaxTest: "+e.getMessage());
			e.printStackTrace();
			fail("Exception in check event syntax");
		}
        assertNull(manager.getVnfResponse());
        assertNull(manager.getVserverResponse());
        

        event.AAI = new HashMap<>();
        event.AAI.put("generic-vnf.vnf-name", "abatementTest");
        try {
			manager.checkEventSyntax(event);
		} catch (ControlLoopException e) {
			logger.debug("ControlLoopException in abatemetCheckEventSyntaxTest: "+e.getMessage());
			e.printStackTrace();
			fail("Exception in check event syntax");
		}
        assertNull(manager.getVnfResponse());
        assertNull(manager.getVserverResponse());
	}
	
	@Test
	public void subsequentOnsetTest() {
		UUID requestId = UUID.randomUUID();
		VirtualControlLoopEvent event = new VirtualControlLoopEvent();
		event.closedLoopControlName = "TwoOnsetTest";
		event.requestID = requestId;
		event.target = "generic-vnf.vnf-id";
        event.closedLoopAlarmStart = Instant.now();
        event.closedLoopEventStatus = ControlLoopEventStatus.ONSET;
        event.AAI = new HashMap<>();
        event.AAI.put("generic-vnf.vnf-name", "onsetOne");
        
        ControlLoopEventManager manager = new ControlLoopEventManager(event.closedLoopControlName, event.requestID);
        VirtualControlLoopNotification notification = manager.activate(event);
        
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.notification);
        
        ControlLoopEventManager.NEW_EVENT_STATUS status = null;
        try {
            status = manager.onNewEvent(event);
        } catch (AAIException e) {
            logger.warn(e.toString());
            fail("A&AI Query Failed");
        }
        assertNotNull(status);
        assertEquals(ControlLoopEventManager.NEW_EVENT_STATUS.FIRST_ONSET, status);
        
        AAIGETVnfResponse response = manager.getVnfResponse();
        assertNotNull(response);
        assertNull(manager.getVserverResponse());
        
        VirtualControlLoopEvent event2 = new VirtualControlLoopEvent();
		event2.closedLoopControlName = "TwoOnsetTest";
		event2.requestID = requestId;
		event2.target = "generic-vnf.vnf-id";
        event2.closedLoopAlarmStart = Instant.now();
        event2.closedLoopEventStatus = ControlLoopEventStatus.ONSET;
        event2.AAI = new HashMap<>();
        event2.AAI.put("generic-vnf.vnf-name", "onsetTwo");
        
        
        try {
            status = manager.onNewEvent(event2);
        } catch (AAIException e) {
            logger.warn(e.toString());
            fail("A&AI Query Failed");
        }
        assertEquals(ControlLoopEventManager.NEW_EVENT_STATUS.SUBSEQUENT_ONSET, status);
        AAIGETVnfResponse response2 = manager.getVnfResponse();
        assertNotNull(response2);
        // We should not have queried AAI, so the stored response should be the same
        assertEquals(response, response2);
        assertNull(manager.getVserverResponse());
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