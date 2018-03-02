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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.aai.util.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RelationshipListTest {
	private static final Logger logger = LoggerFactory.getLogger(RelationshipListTest.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		RelationshipList relationshipList = new RelationshipList();
		Relationship relationship = new Relationship();
		relationship.setRelatedLink("related-link");
		relationship.setRelatedTo("related-to");
		assertEquals("related-link", relationship.getRelatedLink());
		assertEquals("related-to", relationship.getRelatedTo());

		RelatedToProperty relatedToProperty = new RelatedToProperty();
		RelatedToPropertyItem relatedToPropertyItem = new RelatedToPropertyItem();
		relatedToPropertyItem.setPropertyKey("model.model-name");
		relatedToPropertyItem.setPropertyValue("service-instance");
		relatedToProperty.getRelatedTo().add(relatedToPropertyItem);
		RelatedToPropertyItem relatedToPropertyItem2 = new RelatedToPropertyItem();
		relatedToPropertyItem2.setPropertyKey("model.model-name2");
		relatedToPropertyItem2.setPropertyValue("service-instance2");
		relatedToProperty.getRelatedTo().add(relatedToPropertyItem2);		
		relationship.setRelatedToProperty(relatedToProperty);
		RelationshipDataItem relationshipDataItem = new RelationshipDataItem();
		relationshipDataItem.setRelationshipKey("relationship-key");
		relationshipDataItem.setRelationshipValue("relationship-value"); 
		RelationshipData relationshipData = new RelationshipData();
		relationshipData.getRelationshipData().add(relationshipDataItem);
		relationship.setRelationshipData(relationshipData);
		relationshipList.getRelationshipList().add(relationship);
		
	    assertNotNull(relationshipList);
	    
	    relationshipList.setRelationshipList(relationshipList.getRelationshipList());
	    assertNotNull(relationshipList);

	    logger.info(Serialization.gsonPretty.toJson(relationshipList));		
	}

}
