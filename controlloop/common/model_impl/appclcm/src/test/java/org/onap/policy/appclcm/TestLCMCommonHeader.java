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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

public class TestLCMCommonHeader {

	@Test
	public void testLCMLCMCommonHeader() {
		LCMCommonHeader commonHeader = new LCMCommonHeader();
		assertNotNull(commonHeader);
		assertNotNull(new LCMCommonHeader(commonHeader));
		assertNotEquals(0, commonHeader.hashCode());
		
		commonHeader.setApiVer("Kansas");
		assertEquals("Kansas", commonHeader.getApiVer());
		
		Map<String, String> flagMap = new HashMap<>();
		commonHeader.setFlags(flagMap);
		assertEquals(flagMap, commonHeader.getFlags());
		
		commonHeader.setOriginatorId("Dorothy");
		assertEquals("Dorothy", commonHeader.getOriginatorId());
		
		UUID requestId = UUID.randomUUID();
		commonHeader.setRequestId(requestId);
		assertEquals(requestId, commonHeader.getRequestId());
		
		commonHeader.setSubRequestId("Can I go home?");
		assertEquals("Can I go home?", commonHeader.getSubRequestId());
		
		Instant timestamp = Instant.now();
		commonHeader.setTimeStamp(timestamp);
		assertEquals(timestamp, commonHeader.getTimeStamp());
		
		assertNotEquals(0, commonHeader.hashCode());
		
		assertEquals("CommonHeader [timeStamp=", commonHeader.toString().substring(0,  24));
		
        LCMCommonHeader copiedLCMCommonHeader = new LCMCommonHeader();
        copiedLCMCommonHeader.setApiVer(commonHeader.getApiVer());
        copiedLCMCommonHeader.setFlags(commonHeader.getFlags());
        copiedLCMCommonHeader.setOriginatorId(commonHeader.getOriginatorId());
        copiedLCMCommonHeader.setRequestId(commonHeader.getRequestId());
        copiedLCMCommonHeader.setSubRequestId(commonHeader.getSubRequestId());
        copiedLCMCommonHeader.setTimeStamp(commonHeader.getTimeStamp());
		
        assertTrue(commonHeader.equals(commonHeader));
        assertTrue(commonHeader.equals(copiedLCMCommonHeader));
        assertFalse(commonHeader.equals(null));
        assertFalse(commonHeader.equals("Hello"));
        
        LCMCommonHeader clonedLCMCommonHeader = new LCMCommonHeader(commonHeader);
        clonedLCMCommonHeader.setApiVer(commonHeader.getApiVer());
        clonedLCMCommonHeader.setTimeStamp(commonHeader.getTimeStamp());

        assertTrue(commonHeader.equals(clonedLCMCommonHeader));

        commonHeader.setApiVer(null);
        assertFalse(commonHeader.equals(copiedLCMCommonHeader));
        copiedLCMCommonHeader.setApiVer(null);
        assertTrue(commonHeader.equals(copiedLCMCommonHeader));
        commonHeader.setApiVer("Kansas");
        assertFalse(commonHeader.equals(copiedLCMCommonHeader));
        copiedLCMCommonHeader.setApiVer("Kansas");
        assertTrue(commonHeader.equals(copiedLCMCommonHeader));
        
        commonHeader.setFlags(null);
        assertFalse(commonHeader.equals(copiedLCMCommonHeader));
        copiedLCMCommonHeader.setFlags(null);
        assertTrue(commonHeader.equals(copiedLCMCommonHeader));
        commonHeader.setFlags(flagMap);
        assertFalse(commonHeader.equals(copiedLCMCommonHeader));
        copiedLCMCommonHeader.setFlags(flagMap);
        assertTrue(commonHeader.equals(copiedLCMCommonHeader));
        
        commonHeader.setOriginatorId(null);
        assertFalse(commonHeader.equals(copiedLCMCommonHeader));
        copiedLCMCommonHeader.setOriginatorId(null);
        assertTrue(commonHeader.equals(copiedLCMCommonHeader));
        commonHeader.setOriginatorId("Dorothy");
        assertFalse(commonHeader.equals(copiedLCMCommonHeader));
        copiedLCMCommonHeader.setOriginatorId("Dorothy");
        assertTrue(commonHeader.equals(copiedLCMCommonHeader));
        
        commonHeader.setRequestId(null);
        assertFalse(commonHeader.equals(copiedLCMCommonHeader));
        copiedLCMCommonHeader.setRequestId(null);
        assertTrue(commonHeader.equals(copiedLCMCommonHeader));
        commonHeader.setRequestId(requestId);
        assertFalse(commonHeader.equals(copiedLCMCommonHeader));
        copiedLCMCommonHeader.setRequestId(requestId);
        assertTrue(commonHeader.equals(copiedLCMCommonHeader));
		
        commonHeader.setSubRequestId(null);
        assertFalse(commonHeader.equals(copiedLCMCommonHeader));
		copiedLCMCommonHeader.setSubRequestId(null);
        assertTrue(commonHeader.equals(copiedLCMCommonHeader));
        commonHeader.setSubRequestId("Can I go home?");
        assertFalse(commonHeader.equals(copiedLCMCommonHeader));
		copiedLCMCommonHeader.setSubRequestId("Can I go home?");
        assertTrue(commonHeader.equals(copiedLCMCommonHeader));
		
        commonHeader.setTimeStamp(null);
        assertFalse(commonHeader.equals(copiedLCMCommonHeader));
		copiedLCMCommonHeader.setTimeStamp(null);
        assertTrue(commonHeader.equals(copiedLCMCommonHeader));
        commonHeader.setTimeStamp(timestamp);
        assertFalse(commonHeader.equals(copiedLCMCommonHeader));
		copiedLCMCommonHeader.setTimeStamp(timestamp);
        assertTrue(commonHeader.equals(copiedLCMCommonHeader));
	}
}
