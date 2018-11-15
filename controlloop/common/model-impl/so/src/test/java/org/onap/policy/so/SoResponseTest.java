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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SoResponseTest {

    @Test
    public void testConstructor() {
        SOResponse obj = new SOResponse();

        assertEquals(0, obj.getHttpResponseCode());
        assertTrue(obj.getRequest() == null);
        assertTrue(obj.getRequestError() == null);
        assertTrue(obj.getRequestReferences() == null);
    }

    @Test
    public void testSetGet() {
        SOResponse obj = new SOResponse();

        obj.setHttpResponseCode(2008);
        assertEquals(2008, obj.getHttpResponseCode());

        SORequest request = new SORequest();
        obj.setRequest(request);
        assertEquals(request, obj.getRequest());

        SORequestError requestError = new SORequestError();
        obj.setRequestError(requestError);
        assertEquals(requestError, obj.getRequestError());

        SORequestReferences requestReferences = new SORequestReferences();
        obj.setRequestReferences(requestReferences);
        assertEquals(requestReferences, obj.getRequestReferences());
    }
}
