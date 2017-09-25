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

public class RelationshipTest {
	private static final Logger logger = LoggerFactory.getLogger(AAINQResponseWrapperTest.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		Relationship relationship = new Relationship();
		relationship.relatedLink = "related-link"; 
		relationship.relatedTo   = "related-to"; 
		RelatedToProperty relatedToProperty = new RelatedToProperty();
		RelatedToPropertyItem relatedToPropertyItem = new RelatedToPropertyItem(); 
		relatedToPropertyItem.propertyKey = "model.model-name"; 
		relatedToPropertyItem.propertyValue = "service-instance";
		relatedToProperty.relatedTo.add(relatedToPropertyItem); 
		RelatedToPropertyItem relatedToPropertyItem2 = new RelatedToPropertyItem(); 
		relatedToPropertyItem2.propertyKey = "model.model-name2"; 
		relatedToPropertyItem2.propertyValue = "service-instance2";
		relatedToProperty.relatedTo.add(relatedToPropertyItem2);		
		relationship.relatedToProperty = relatedToProperty; 
		RelationshipDataItem relationshipDataItem = new RelationshipDataItem(); 
		relationshipDataItem.relationshipKey = "relationship-key";
		relationshipDataItem.relationshipValue = "relationship-value";  
		RelationshipData relationshipData = new RelationshipData(); 
		relationshipData.relationshipData.add(relationshipDataItem); 
		relationship.relationshipData = relationshipData; 
	    assertNotNull(relationship); 
	    logger.info(Serialization.gsonPretty.toJson(relationship));
	}

}
