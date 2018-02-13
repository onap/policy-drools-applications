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

package org.onap.policy.appc;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

public class TestCommonHeader {

	@Test
	public void testCommonHeader() {
		CommonHeader commonHeader = new CommonHeader();
		assertNotNull(commonHeader);
		assertNotNull(new CommonHeader(commonHeader));
		assertNotEquals(0, commonHeader.hashCode());
		
		commonHeader.setApiVer("Kansas");
		assertEquals("Kansas", commonHeader.getApiVer());
		
		List<Map<String, String>> flagSet = new ArrayList<>();
		commonHeader.setFlags(flagSet);
		assertEquals(flagSet, commonHeader.getFlags());
		
		commonHeader.setOriginatorID("Dorothy");
		assertEquals("Dorothy", commonHeader.getOriginatorID());
		
		UUID requestID = UUID.randomUUID();
		commonHeader.setRequestID(requestID);
		assertEquals(requestID, commonHeader.getRequestID());
		
		List<String> requestTrackSet = new ArrayList<>();
		commonHeader.setRequestTrack(requestTrackSet);
		assertEquals(requestTrackSet, commonHeader.getRequestTrack());
		
		commonHeader.setSubRequestID("Can I go home?");
		assertEquals("Can I go home?", commonHeader.getSubRequestID());
		
		Instant timestamp = Instant.now();
		commonHeader.setTimeStamp(timestamp);
		assertEquals(timestamp, commonHeader.getTimeStamp());
		
		assertNotEquals(0, commonHeader.hashCode());
		
		assertEquals("CommonHeader [TimeStamp=", commonHeader.toString().substring(0,  24));
		
        CommonHeader copiedCommonHeader = new CommonHeader();
        copiedCommonHeader.setApiVer(commonHeader.getApiVer());
        copiedCommonHeader.setFlags(commonHeader.getFlags());
        copiedCommonHeader.setOriginatorID(commonHeader.getOriginatorID());
        copiedCommonHeader.setRequestID(commonHeader.getRequestID());
        copiedCommonHeader.setRequestTrack(commonHeader.getRequestTrack());
        copiedCommonHeader.setSubRequestID(commonHeader.getSubRequestID());
        copiedCommonHeader.setTimeStamp(commonHeader.getTimeStamp());
		
        assertTrue(commonHeader.equals(commonHeader));
        assertTrue(commonHeader.equals(copiedCommonHeader));
        assertFalse(commonHeader.equals(null));
        assertFalse(commonHeader.equals("Hello"));
        
        CommonHeader clonedCommonHeader = new CommonHeader(commonHeader);
        clonedCommonHeader.setApiVer(commonHeader.getApiVer());
        clonedCommonHeader.setTimeStamp(commonHeader.getTimeStamp());

        assertTrue(commonHeader.equals(clonedCommonHeader));

        commonHeader.setApiVer(null);
        assertFalse(commonHeader.equals(copiedCommonHeader));
        copiedCommonHeader.setApiVer(null);
        assertTrue(commonHeader.equals(copiedCommonHeader));
        commonHeader.setApiVer("Kansas");
        assertFalse(commonHeader.equals(copiedCommonHeader));
        copiedCommonHeader.setApiVer("Kansas");
        assertTrue(commonHeader.equals(copiedCommonHeader));
        
        commonHeader.setFlags(null);
        assertFalse(commonHeader.equals(copiedCommonHeader));
        copiedCommonHeader.setFlags(null);
        assertTrue(commonHeader.equals(copiedCommonHeader));
        commonHeader.setFlags(flagSet);
        assertFalse(commonHeader.equals(copiedCommonHeader));
        copiedCommonHeader.setFlags(flagSet);
        assertTrue(commonHeader.equals(copiedCommonHeader));
        
        commonHeader.setOriginatorID(null);
        assertFalse(commonHeader.equals(copiedCommonHeader));
        copiedCommonHeader.setOriginatorID(null);
        assertTrue(commonHeader.equals(copiedCommonHeader));
        commonHeader.setOriginatorID("Dorothy");
        assertFalse(commonHeader.equals(copiedCommonHeader));
        copiedCommonHeader.setOriginatorID("Dorothy");
        assertTrue(commonHeader.equals(copiedCommonHeader));
        
        commonHeader.setRequestID(null);
        assertFalse(commonHeader.equals(copiedCommonHeader));
        copiedCommonHeader.setRequestID(null);
        assertTrue(commonHeader.equals(copiedCommonHeader));
        commonHeader.setRequestID(requestID);
        assertFalse(commonHeader.equals(copiedCommonHeader));
        copiedCommonHeader.setRequestID(requestID);
        assertTrue(commonHeader.equals(copiedCommonHeader));
		
        commonHeader.setRequestTrack(null);
        assertFalse(commonHeader.equals(copiedCommonHeader));
		copiedCommonHeader.setRequestTrack(null);
        assertTrue(commonHeader.equals(copiedCommonHeader));
        commonHeader.setRequestTrack(requestTrackSet);
        assertFalse(commonHeader.equals(copiedCommonHeader));
		copiedCommonHeader.setRequestTrack(requestTrackSet);
        assertTrue(commonHeader.equals(copiedCommonHeader));
		
        commonHeader.setSubRequestID(null);
        assertFalse(commonHeader.equals(copiedCommonHeader));
		copiedCommonHeader.setSubRequestID(null);
        assertTrue(commonHeader.equals(copiedCommonHeader));
        commonHeader.setSubRequestID("Can I go home?");
        assertFalse(commonHeader.equals(copiedCommonHeader));
		copiedCommonHeader.setSubRequestID("Can I go home?");
        assertTrue(commonHeader.equals(copiedCommonHeader));
		
        commonHeader.setTimeStamp(null);
        assertFalse(commonHeader.equals(copiedCommonHeader));
		copiedCommonHeader.setTimeStamp(null);
        assertTrue(commonHeader.equals(copiedCommonHeader));
        commonHeader.setTimeStamp(timestamp);
        assertFalse(commonHeader.equals(copiedCommonHeader));
		copiedCommonHeader.setTimeStamp(timestamp);
        assertTrue(commonHeader.equals(copiedCommonHeader));
	}
}
