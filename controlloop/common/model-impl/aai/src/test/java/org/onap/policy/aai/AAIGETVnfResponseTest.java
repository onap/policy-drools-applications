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

import static org.junit.Assert.*;

import org.junit.Test;

import org.onap.policy.aai.util.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AAIGETVnfResponseTest {
	private static final Logger logger = LoggerFactory.getLogger(AAIGETVnfResponseTest.class);

	@Test
	public void test() {
		AAIGETVnfResponse response = new AAIGETVnfResponse(); 
		
        response.setVnfID("83f674e8-7555-44d7-9a39-bdc3770b0491"); 
        response.setVnfName("lll_vnf_010317");
        response.setVnfType("Basa-122216-Service/VidVsamp12BaseVolume 1");
        response.setServiceId("a9a77d5a-123e-4ca2-9eb9-0b015d2ee0fb");
        response.setOrchestrationStatus("Created");
        response.setInMaint("false");
        response.setIsClosedLoopDisabled("false");
		response.setResourceVersion("1494001988835");
		response.setModelInvariantId("f18be3cd-d446-456e-9109-121d9b62feaa");
		assertEquals(response.getVnfID(), "83f674e8-7555-44d7-9a39-bdc3770b0491"); 
        assertEquals(response.getVnfName(), "lll_vnf_010317");
        assertEquals(response.getVnfType(), "Basa-122216-Service/VidVsamp12BaseVolume 1");
        assertEquals(response.getServiceId(), "a9a77d5a-123e-4ca2-9eb9-0b015d2ee0fb");
        assertEquals(response.getOrchestrationStatus(), "Created");
        assertEquals(response.getInMaint(), "false");
        assertEquals(response.getIsClosedLoopDisabled(), "false");
        assertEquals(response.getResourceVersion(), "1494001988835");
		assertEquals(response.getModelInvariantId(), "f18be3cd-d446-456e-9109-121d9b62feaa");
		  
       	RelationshipList relationshipList = new RelationshipList();
       	Relationship     relationship = new Relationship();
       	RelationshipData relationshipData = new RelationshipData();
       	RelationshipDataItem relationshipDataItem = new RelationshipDataItem();
       	
       	relationshipDataItem.setRelationshipKey("customer.global-customer-id"); 
       	relationshipDataItem.setRelationshipValue("MSO_1610_ST"); 
       	relationshipData.getRelationshipData().add(relationshipDataItem);
       	
       	relationshipDataItem.setRelationshipKey("service-subscription.service-type"); 
       	relationshipDataItem.setRelationshipValue("MSO-dev-service-type"); 
       	relationshipData.getRelationshipData().add(relationshipDataItem);
       	
       	relationshipDataItem.setRelationshipKey("service-instance.service-instance-id"); 
       	relationshipDataItem.setRelationshipValue("e1e9c97c-02c0-4919-9b4c-eb5d5ef68970"); 
       	relationshipData.getRelationshipData().add(relationshipDataItem);
       	
       	RelatedToProperty relatedToProperty = new RelatedToProperty();
       	RelatedToPropertyItem item = new RelatedToPropertyItem();  
       	item.setPropertyKey("service-instance.service-instance-name"); 
       	item.setPropertyValue("lll_svc_010317"); 
       	relatedToProperty.getRelatedTo().add(item);
       	assertEquals(item.getPropertyKey(), "service-instance.service-instance-name"); 
       	assertEquals(item.getPropertyValue(), "lll_svc_010317"); 
       	
        relationship.setRelatedTo("service-instance");
        relationship.setRelatedLink("/aai/v11/business/customers/customer/MSO_1610_ST/service-subscriptions/service-subscription/MSO-dev-service-type/service-instances/service-instance/e1e9c97c-02c0-4919-9b4c-eb5d5ef68970");
        relationship.setRelationshipData(relationshipData);
        relationship.setRelatedToProperty(relatedToProperty);
       
        relationshipList.getRelationshipList().add(relationship);
        response.setRelationshipList(relationshipList); 
        
        logger.info(Serialization.gsonPretty.toJson(response));
  	}
}
