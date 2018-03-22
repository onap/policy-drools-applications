/*-
 * ============LICENSE_START=======================================================
 * aai
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.onap.policy.aai.util.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AaiGetVserverResponseTest {
    private static final Logger logger = LoggerFactory.getLogger(AaiGetVserverResponseTest.class);

    @Test
    public void test() {
        AaiGetVserverResponse response = new AaiGetVserverResponse();

        response.setVserverId("d0668d4f-c25e-4a1b-87c4-83845c01efd8");
        response.setVserverName("USMSO1SX7NJ0103UJZZ01-vjunos0");
        response.setVserverName2("vjunos0");
        response.setVserverSelflink(
                "https://aai-ext1.test.aaa.com:8443/aai/v7/cloud-infrastructure/cloud-regions/cloud-region/aaa-aic/AAIAIC25/tenants/tenant/USMSO1SX7NJ0103UJZZ01%3A%3AuCPE-VMS/vservers/vserver/d0668d4f-c25e-4a1b-87c4-83845c01efd8");
        response.setInMaint("false");
        response.setIsClosedLoopDisabled("false");
        response.setResourceVersion("1494001931513");

        assertEquals("d0668d4f-c25e-4a1b-87c4-83845c01efd8", response.getVserverId());
        assertEquals("USMSO1SX7NJ0103UJZZ01-vjunos0", response.getVserverName());
        assertEquals("vjunos0", response.getVserverName2());
        assertEquals("https://aai-ext1.test.aaa.com:8443/aai/v7/cloud-infrastructure/cloud-regions/cloud-region/"
                + "aaa-aic/AAIAIC25/tenants/tenant/USMSO1SX7NJ0103UJZZ01%3A%3AuCPE-VMS/vservers/vserver"
                + "/d0668d4f-c25e-4a1b-87c4-83845c01efd8", response.getVserverSelflink());
        assertEquals("false", response.getInMaint());
        assertEquals("false", response.getIsClosedLoopDisabled());
        assertEquals("1494001931513", response.getResourceVersion());

        final RelationshipList relationshipList = new RelationshipList();
        final Relationship relationship = new Relationship();
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

        relationship.setRelatedTo("service-instance");
        relationship.setRelatedLink(
                "/aai/v11/business/customers/customer/MSO_1610_ST/service-subscriptions/service-subscription/"
                        + "MSO-dev-service-type/service-instances/service-instance/"
                        + "e1e9c97c-02c0-4919-9b4c-eb5d5ef68970");
        relationship.setRelationshipData(relationshipData);
        relationship.setRelatedToProperty(relatedToProperty);

        relationshipList.getRelationshipList().add(relationship);
        response.setRelationshipList(relationshipList);
        assertEquals(response.getRelationshipList(), relationshipList);
        response.setRequestError(null);
        assertNull(response.getRequestError());

        logger.info(Serialization.gsonPretty.toJson(response));
    }

}
