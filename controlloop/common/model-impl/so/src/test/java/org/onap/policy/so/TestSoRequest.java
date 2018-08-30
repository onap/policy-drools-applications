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

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.Test;

public class TestSoRequest {

    @Test
    public void testConstructor() {
        SORequest obj = new SORequest();

        assertTrue(obj.getFinishTime() == null);
        assertTrue(obj.getRequestDetails() == null);
        assertTrue(obj.getRequestId() == null);
        assertTrue(obj.getRequestScope() == null);
        assertTrue(obj.getRequestStatus() == null);
        assertTrue(obj.getRequestType() == null);
        assertTrue(obj.getStartTime() == null);
    }

    @Test
    public void testSetGet() {
        SORequest obj = new SORequest();

        LocalDateTime finishTime = LocalDateTime.now();
        obj.setFinishTime(finishTime);
        assertEquals(finishTime, obj.getFinishTime());

        UUID uuid = UUID.randomUUID();
        obj.setRequestId(uuid);
        assertEquals(uuid, obj.getRequestId());

        obj.setRequestScope("requestScope");
        assertEquals("requestScope", obj.getRequestScope());

        SORequestStatus requestStatus = new SORequestStatus();
        obj.setRequestStatus(requestStatus);
        assertEquals(requestStatus, obj.getRequestStatus());

        obj.setRequestType("requestType");
        assertEquals("requestType", obj.getRequestType());

        obj.setOperationType(SOOperationType.DELETE_VF_MODULE);
        assertEquals(SOOperationType.DELETE_VF_MODULE, obj.getOperationType());

        LocalDateTime startTime = LocalDateTime.now();
        obj.setStartTime(startTime.toString());
        assertEquals(startTime.toString(), obj.getStartTime());
    }
}
