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

public class TestSoRequestInfo {

    @Test
    public void testConstructor() {
        SORequestInfo obj = new SORequestInfo();

        assertTrue(obj.getBillingAccountNumber() == null);
        assertTrue(obj.getCallbackUrl() == null);
        assertTrue(obj.getCorrelator() == null);
        assertTrue(obj.getInstanceName() == null);
        assertTrue(obj.getOrderNumber() == null);
        assertTrue(obj.getOrderVersion() == null);
        assertTrue(obj.getProductFamilyId() == null);
        assertTrue(obj.getRequestorId() == null);
        assertTrue(obj.getSource() == null);
        assertTrue(obj.isSuppressRollback() == false);
    }

    @Test
    public void testSetGet() {
        SORequestInfo obj = new SORequestInfo();

        obj.setBillingAccountNumber("billingAccountNumber");
        assertEquals("billingAccountNumber", obj.getBillingAccountNumber());

        obj.setCallbackUrl("callbackUrl");
        assertEquals("callbackUrl", obj.getCallbackUrl());

        obj.setCorrelator("correlator");
        assertEquals("correlator", obj.getCorrelator());

        obj.setInstanceName("instanceName");
        assertEquals("instanceName", obj.getInstanceName());

        obj.setOrderNumber("orderNumber");
        assertEquals("orderNumber", obj.getOrderNumber());

        int orderVersion = 2008;
        obj.setOrderVersion(orderVersion);
        assertEquals((Integer) orderVersion, obj.getOrderVersion());

        obj.setProductFamilyId("productFamilyId");
        assertEquals("productFamilyId", obj.getProductFamilyId());

        obj.setRequestorId("requestorId");
        assertEquals("requestorId", obj.getRequestorId());

        obj.setSource("source");
        assertEquals("source", obj.getSource());

        obj.setSuppressRollback(true);
        assertEquals(true, obj.isSuppressRollback());
    }
}
