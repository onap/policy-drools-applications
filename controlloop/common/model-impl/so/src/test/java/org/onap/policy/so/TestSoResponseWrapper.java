/*-
 * ============LICENSE_START=======================================================
 * so
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.so;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.junit.Test;

public class TestSoResponseWrapper {

    @Test
    public void testConstructor() {
        SOResponse response = new SOResponse();
        SOResponseWrapper obj = new SOResponseWrapper(response, "reqID");

        assertEquals(response, obj.getSoResponse());
        assertEquals("reqID", obj.getRequestID());
    }

    @Test
    public void testSetGet() {
        SOResponse response = new SOResponse();
        SOResponseWrapper obj = new SOResponseWrapper(response, "reqID");

        SOResponse response2 = new SOResponse();
        response2.setHttpResponseCode(2008);
        obj.setSoResponse(response2);
        assertEquals(response2, obj.getSoResponse());

        obj.setRequestID("id2");
        assertEquals("id2", obj.getRequestID());
    }

    @Test
    public void testSoResponseWrapperMethods() {
        String requestId = UUID.randomUUID().toString();
        SOResponse response = new SOResponse();

        SOResponseWrapper responseWrapper = new SOResponseWrapper(response, requestId);
        assertNotNull(responseWrapper);
        assertNotEquals(0, responseWrapper.hashCode());

        assertEquals(response, responseWrapper.getSoResponse());

        assertNotEquals(0, responseWrapper.hashCode());

        assertEquals("SOResponseWrapper [SOResponse=org.onap.policy.", responseWrapper.toString().substring(0,  46));

        SOResponseWrapper identicalResponseWrapper = new SOResponseWrapper(response, requestId);

        assertEquals(responseWrapper,  responseWrapper);
        assertEquals(responseWrapper,  identicalResponseWrapper);
        assertNotEquals(null, responseWrapper);
        assertNotEquals("Hello", responseWrapper);
        assertFalse(responseWrapper.equals(null));
        assertFalse(responseWrapper.equals("AString"));
        
        assertEquals(new SOResponseWrapper(null, null), new SOResponseWrapper(null, null));
        assertNotEquals(new SOResponseWrapper(null, null), identicalResponseWrapper);
        
        assertNotEquals(0, new SOResponseWrapper(null, null).hashCode());

        identicalResponseWrapper.setSoResponse(new SOResponse());
        assertNotEquals(responseWrapper,  identicalResponseWrapper);
        identicalResponseWrapper.setSoResponse(response);
        assertEquals(responseWrapper,  identicalResponseWrapper);
        
        identicalResponseWrapper.setRequestID(UUID.randomUUID().toString());
        assertNotEquals(responseWrapper,  identicalResponseWrapper);
        identicalResponseWrapper.setRequestID(requestId);
        assertEquals(responseWrapper,  identicalResponseWrapper);
        
        responseWrapper.setRequestID(null);
        assertNotEquals(responseWrapper,  identicalResponseWrapper);
        identicalResponseWrapper.setRequestID(null);
        assertEquals(responseWrapper,  identicalResponseWrapper);
        responseWrapper.setRequestID(requestId);
        assertNotEquals(responseWrapper,  identicalResponseWrapper);
        identicalResponseWrapper.setRequestID(requestId);
        assertEquals(responseWrapper,  identicalResponseWrapper);
    }
}
