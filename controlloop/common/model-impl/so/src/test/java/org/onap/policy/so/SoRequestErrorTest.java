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

public class SoRequestErrorTest {

    @Test
    public void testConstructor() {
        SORequestError obj = new SORequestError();

        assertTrue(obj.getPolicyException() == null);
        assertTrue(obj.getServiceException() == null);
    }

    @Test
    public void testSetGet() {
        SORequestError obj = new SORequestError();

        SOPolicyExceptionHolder policyException = new SOPolicyExceptionHolder();
        obj.setPolicyException(policyException);
        assertEquals(policyException, obj.getPolicyException());

        SOServiceExceptionHolder serviceException = new SOServiceExceptionHolder();
        obj.setServiceException(serviceException);
        assertEquals(serviceException, obj.getServiceException());
    }
}
