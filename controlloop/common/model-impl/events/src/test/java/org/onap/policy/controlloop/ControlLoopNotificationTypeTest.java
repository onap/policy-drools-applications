/*-
 * ============LICENSE_START=======================================================
 * controlloop
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

package org.onap.policy.controlloop;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ControlLoopNotificationTypeTest {

    @Test
    public void test() {

        assertTrue(ControlLoopNotificationType.toType("ACTIVE").equals(ControlLoopNotificationType.ACTIVE));
        assertTrue(ControlLoopNotificationType.toType("REJECTED").equals(ControlLoopNotificationType.REJECTED));
        assertTrue(ControlLoopNotificationType.toType("OPERATION").equals(ControlLoopNotificationType.OPERATION));
        assertTrue(ControlLoopNotificationType.toType("OPERATION: SUCCESS")
                .equals(ControlLoopNotificationType.OPERATION_SUCCESS));
        assertTrue(ControlLoopNotificationType.toType("OPERATION: FAILURE")
                .equals(ControlLoopNotificationType.OPERATION_FAILURE));
        assertTrue(ControlLoopNotificationType.toType("FINAL: FAILURE")
                .equals(ControlLoopNotificationType.FINAL_FAILURE));
        assertTrue(ControlLoopNotificationType.toType("FINAL: SUCCESS")
                .equals(ControlLoopNotificationType.FINAL_SUCCESS));
        assertTrue(ControlLoopNotificationType.toType("FINAL: OPENLOOP")
                .equals(ControlLoopNotificationType.FINAL_OPENLOOP));

        assertNull(ControlLoopNotificationType.toType("foo"));
    }
}
