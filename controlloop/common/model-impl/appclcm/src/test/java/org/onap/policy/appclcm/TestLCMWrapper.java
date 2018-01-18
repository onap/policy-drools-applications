/*-
 * ============LICENSE_START=======================================================
 * appc
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

package org.onap.policy.appclcm;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestLCMWrapper {

	@Test
	public void testLCMWrapper() {
		LCMWrapper wrapper = new LCMWrapper();
		assertNotNull(wrapper);
		assertNotEquals(0, wrapper.hashCode());
		
		wrapper.setVersion("19.3.9");
		assertEquals("19.3.9", wrapper.getVersion());
		
		wrapper.setCambriaPartition("The Emerald City");
		assertEquals("The Emerald City", wrapper.getCambriaPartition());
		
		wrapper.setRpcName("Tornado");
		assertEquals("Tornado", wrapper.getRpcName());
		
		wrapper.setCorrelationId("YellowBrickRoad");
		assertEquals("YellowBrickRoad", wrapper.getCorrelationId());
		
		wrapper.setType("Munchkin");
		assertEquals("Munchkin", wrapper.getType());
		
		assertNotEquals(0, wrapper.hashCode());
		
		assertEquals("Wrapper [version=19.3.9, cambriaPartition=The ", wrapper.toString().substring(0,  46));
		
        LCMWrapper copiedLCMWrapper = new LCMWrapper();
        copiedLCMWrapper.setVersion(wrapper.getVersion());
        copiedLCMWrapper.setCambriaPartition(wrapper.getCambriaPartition());
        copiedLCMWrapper.setRpcName(wrapper.getRpcName());
        copiedLCMWrapper.setCorrelationId(wrapper.getCorrelationId());
        copiedLCMWrapper.setType(wrapper.getType());

        assertTrue(wrapper.equals(wrapper));
        assertTrue(wrapper.equals(copiedLCMWrapper));
        assertFalse(wrapper.equals(null));
        assertFalse(wrapper.equals("Hello"));
        
        wrapper.setVersion(null);
        assertFalse(wrapper.equals(copiedLCMWrapper));
        copiedLCMWrapper.setVersion(null);
        assertTrue(wrapper.equals(copiedLCMWrapper));
        wrapper.setVersion("19.3.9");
        assertFalse(wrapper.equals(copiedLCMWrapper));
        copiedLCMWrapper.setVersion("19.3.9");
        assertTrue(wrapper.equals(copiedLCMWrapper));
        
        wrapper.setCambriaPartition(null);
        assertFalse(wrapper.equals(copiedLCMWrapper));
        copiedLCMWrapper.setCambriaPartition(null);
        assertTrue(wrapper.equals(copiedLCMWrapper));
        wrapper.setCambriaPartition("The Emerald City");
        assertFalse(wrapper.equals(copiedLCMWrapper));
        copiedLCMWrapper.setCambriaPartition("The Emerald City");
        assertTrue(wrapper.equals(copiedLCMWrapper));
        
        wrapper.setRpcName(null);
        assertFalse(wrapper.equals(copiedLCMWrapper));
        copiedLCMWrapper.setRpcName(null);
        assertTrue(wrapper.equals(copiedLCMWrapper));
        wrapper.setRpcName("Tornado");
        assertFalse(wrapper.equals(copiedLCMWrapper));
        copiedLCMWrapper.setRpcName("Tornado");
        assertTrue(wrapper.equals(copiedLCMWrapper));
        
        wrapper.setCorrelationId(null);
        assertFalse(wrapper.equals(copiedLCMWrapper));
        copiedLCMWrapper.setCorrelationId(null);
        assertTrue(wrapper.equals(copiedLCMWrapper));
        wrapper.setCorrelationId("YellowBrickRoad");
        assertFalse(wrapper.equals(copiedLCMWrapper));
        copiedLCMWrapper.setCorrelationId("YellowBrickRoad");
        assertTrue(wrapper.equals(copiedLCMWrapper));
        
        wrapper.setType(null);
        assertFalse(wrapper.equals(copiedLCMWrapper));
        copiedLCMWrapper.setType(null);
        assertTrue(wrapper.equals(copiedLCMWrapper));
        wrapper.setType("Munchkin");
        assertFalse(wrapper.equals(copiedLCMWrapper));
        copiedLCMWrapper.setType("Munchkin");
        assertTrue(wrapper.equals(copiedLCMWrapper));
	}
}
