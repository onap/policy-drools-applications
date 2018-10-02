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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.UUID;
import org.junit.Test;
import org.onap.policy.controlloop.util.Serialization;

public class ControlLoopNotificationTest {
    
    private class TestControlLoopNotification extends ControlLoopNotification {
        private static final long serialVersionUID = 1L;
    
        public TestControlLoopNotification() {
            super();
        }

        public TestControlLoopNotification(ControlLoopEvent event) {
            super(event);
        }

        @Override
        public String toString() {
            return "TestControlLoopNotification [getClosedLoopControlName()="
                    + getClosedLoopControlName()
                    + ", getVersion()="
                    + getVersion()
                    + ", getRequestId()="
                    + getRequestId()
                    + ", getClosedLoopEventClient()="
                    + getClosedLoopEventClient()
                    + ", getTargetType()="
                    + getTargetType()
                    + ", getTarget()="
                    + getTarget()
                    + ", getFrom()="
                    + getFrom()
                    + ", getPolicyScope()="
                    + getPolicyScope()
                    + ", getPolicyName()="
                    + getPolicyName()
                    + ", getPolicyVersion()="
                    + getPolicyVersion()
                    + ", getNotification()="
                    + getNotification()
                    + ", getMessage()="
                    + getMessage()
                    + ", getNotificationTime()="
                    + getNotificationTime()
                    + ", getOpsClTimer()="
                    + getOpsClTimer()
                    + ", getHistory()="
                    + getHistory()
                    + "]";
        }
        
    }

    @Test
    public void test() {
        ControlLoopNotification notification = new TestControlLoopNotification();
        assertNotNull(notification);
        assertTrue(notification.getVersion().equals("1.0.2"));
        
        notification.setClosedLoopControlName("name");
        assertTrue(notification.getClosedLoopControlName().equals("name"));
        
        notification.setClosedLoopEventClient("client");
        assertTrue(notification.getClosedLoopEventClient().equals("client"));
        
        notification.setFrom("from");
        assertTrue(notification.getFrom().equals("from"));
        
        notification.setHistory(Collections.emptyList());
        assertTrue(notification.getHistory().size() == 0);
        
        notification.setMessage("message");
        assertTrue(notification.getMessage().equals("message"));
        
        notification.setNotification(ControlLoopNotificationType.ACTIVE);
        assertTrue(notification.getNotification().equals(ControlLoopNotificationType.ACTIVE));
        
        ZonedDateTime time = ZonedDateTime.now(ZoneOffset.UTC);
        notification.setNotificationTime(time);
        assertTrue(notification.getNotificationTime().equals(time));
        
        notification.setOpsClTimer(1000);
        assertTrue(notification.getOpsClTimer().equals(1000));
        
        notification.setPolicyName("name");
        assertTrue(notification.getPolicyName().equals("name"));
        
        notification.setPolicyScope("scope");
        assertTrue(notification.getPolicyScope().equals("scope"));
        
        notification.setPolicyVersion("1");
        assertTrue(notification.getPolicyVersion().equals("1"));
        
        UUID id = UUID.randomUUID();
        notification.setRequestId(id);
        assertTrue(notification.getRequestId().equals(id));
        
        notification.setTarget("target");
        assertTrue(notification.getTarget().equals("target"));
        
        notification.setTargetType(ControlLoopTargetType.VFC);
        assertTrue(notification.getTargetType().equals(ControlLoopTargetType.VFC));

        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName("controlloop");
        
        TestControlLoopNotification notification2 = new TestControlLoopNotification(event);
        assertTrue(notification2.getClosedLoopControlName().equals("controlloop"));
        
        notification2.setVersion("1");
        assertTrue(notification2.getVersion().equals("1"));
        
        String json = Serialization.gsonPretty.toJson(notification);
        
        TestControlLoopNotification notification3 = Serialization.gson.fromJson(json, 
                TestControlLoopNotification.class);

        //
        // There is no equals for the class - chose not to create one
        //
        assertEquals(notification.getRequestId(), notification3.getRequestId());
        
    }
}
