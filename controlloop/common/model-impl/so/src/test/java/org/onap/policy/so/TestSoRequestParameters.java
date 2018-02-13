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

public class TestSoRequestParameters {

    @Test
    public void testConstructor() {
        SORequestParameters obj = new SORequestParameters();

        assertTrue(obj.getSubscriptionServiceType() == null);
        assertTrue(obj.getUserParams() != null);
        assertEquals(0, obj.getUserParams().size());
    }

    @Test
    public void testSetGet() {
        SORequestParameters obj = new SORequestParameters();

        obj.setSubscriptionServiceType("subscriptionServiceType");
        assertEquals("subscriptionServiceType", obj.getSubscriptionServiceType());
    }
}
