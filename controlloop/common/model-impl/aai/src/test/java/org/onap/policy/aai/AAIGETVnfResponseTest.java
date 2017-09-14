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

import org.junit.Test;
import org.onap.policy.aai.util.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AAIGETVnfResponseTest {
	private static final Logger logger = LoggerFactory.getLogger(AAIGETVnfResponseTest.class);

	@Test
	public void test() {
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
        
        logger.info(Serialization.gsonPretty.toJson(response));
  	}
}
