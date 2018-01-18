/*-
 * ============LICENSE_START=======================================================
 * so
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

package org.onap.policy.so;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestSoPolicyException {

    @Test
    public void testConstructor() {
        SOPolicyException obj = new SOPolicyException();

        assertTrue(obj.getMessageId() == null);
        assertTrue(obj.getText() == null);
    }

    @Test
    public void testSetGet() {
        SOPolicyException obj = new SOPolicyException();

        obj.setMessageId("messageId");
        assertEquals("messageId", obj.getMessageId());

        obj.setText("text");
        assertEquals("text", obj.getText());
    }
}
