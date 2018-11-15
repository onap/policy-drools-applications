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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SoRequestStatusTest {

    @Test
    public void testConstructor() {
        SORequestStatus obj = new SORequestStatus();

        assertEquals(0, obj.getPercentProgress());
        assertTrue(obj.getRequestState() == null);
        assertTrue(obj.getTimestamp() == null);
        assertFalse(obj.isWasRolledBack());
    }

    @Test
    public void testSetGet() {
        SORequestStatus obj = new SORequestStatus();

        obj.setPercentProgress(2008);
        assertEquals(2008, obj.getPercentProgress());

        obj.setRequestState("requestState");
        assertEquals("requestState", obj.getRequestState());

        obj.setTimestamp("timestamp");
        assertEquals("timestamp", obj.getTimestamp());

        obj.setWasRolledBack(true);
        assertTrue(obj.isWasRolledBack());
    }
}
