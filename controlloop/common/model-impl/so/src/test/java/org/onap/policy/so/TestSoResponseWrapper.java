/*-
 * ============LICENSE_START=======================================================
 * so
 * ================================================================================
 * 
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
}
