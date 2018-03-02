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

public class RelationshipDataTest {
	private static final Logger logger = LoggerFactory.getLogger(RelationshipDataTest.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		RelationshipData relationshipData = new RelationshipData(); 
		RelationshipDataItem relationshipDataItem = new RelationshipDataItem(); 
		relationshipDataItem.setRelationshipKey("relationship-key");
		relationshipDataItem.setRelationshipValue("relationship-value"); 
		assertNotNull(relationshipDataItem);
		assertEquals("relationship-key", relationshipDataItem.getRelationshipKey());
		assertEquals("relationship-value", relationshipDataItem.getRelationshipValue());

		relationshipData.getRelationshipData().add(relationshipDataItem); 
		RelationshipDataItem relationshipDataItem2 = new RelationshipDataItem(); 
		relationshipDataItem2.setRelationshipKey("relationship-key2");
		relationshipDataItem2.setRelationshipValue("relationship-value2"); 
		relationshipData.getRelationshipData().add(relationshipDataItem2); 
	    assertNotNull(relationshipData);
	    
	    relationshipData.setRelationshipData(relationshipData.getRelationshipData());
	    assertNotNull(relationshipData);
	    
	    logger.info(Serialization.gsonPretty.toJson(relationshipData));
	}

}
