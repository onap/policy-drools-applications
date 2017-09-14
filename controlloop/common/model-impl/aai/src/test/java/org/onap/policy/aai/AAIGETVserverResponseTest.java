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

public class AAIGETVserverResponseTest {
	private static final Logger logger = LoggerFactory.getLogger(AAIGETVserverResponseTest.class);

	@Test
	public void test() {
		AAIGETVserverResponse response = new AAIGETVserverResponse(); 
		
		response.vserverID = "d0668d4f-c25e-4a1b-87c4-83845c01efd8"; 
		response.vserverName = "USMSO1SX7NJ0103UJZZ01-vjunos0"; 
		response.vserverName2 = "vjunos0"; 
		response.vserverSelflink = "https://aai-ext1.test.aaa.com:8443/aai/v7/cloud-infrastructure/cloud-regions/cloud-region/aaa-aic/AAIAIC25/tenants/tenant/USMSO1SX7NJ0103UJZZ01%3A%3AuCPE-VMS/vservers/vserver/d0668d4f-c25e-4a1b-87c4-83845c01efd8"; 
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
        
        logger.info(Serialization.gsonPretty.toJson(response));
  	}

}