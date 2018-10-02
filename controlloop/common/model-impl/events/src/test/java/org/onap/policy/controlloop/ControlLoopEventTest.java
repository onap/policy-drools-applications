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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.Test;

public class ControlLoopEventTest {
    
    private class TestControlLoopEvent extends ControlLoopEvent {
        private static final long serialVersionUID = 1L;
        
        public TestControlLoopEvent() {
            super();
        }
        
        public TestControlLoopEvent(ControlLoopEvent event) {
            super(event);
        }
    }

    @Test
    public void test() {
        ControlLoopEvent event = new TestControlLoopEvent();
        assertNotNull(event);
        assertTrue(event.getVersion().equals("1.0.2"));
        
        event = new TestControlLoopEvent(null);
        assertTrue(event.getVersion().equals("1.0.2"));
        
        event.setClosedLoopControlName("name");
        assertTrue(event.getClosedLoopControlName().equals("name"));
        
        event.setClosedLoopEventClient("client");
        assertTrue(event.getClosedLoopEventClient().equals("client"));
        
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        assertTrue(event.getClosedLoopEventStatus().equals(ControlLoopEventStatus.ONSET));
        
        event.setFrom("from");
        assertTrue(event.getFrom().equals("from"));
        
        event.setPayload("payload");
        assertTrue(event.getPayload().equals("payload"));
        
        event.setPolicyName("policyname");
        assertTrue(event.getPolicyName().equals("policyname"));
        
        event.setPolicyScope("scope");
        assertTrue(event.getPolicyScope().equals("scope"));
        
        event.setPolicyVersion("1");
        assertTrue(event.getPolicyVersion().equals("1"));
        
        UUID id = UUID.randomUUID();
        event.setRequestId(id);
        assertTrue(event.getRequestId().equals(id));
        
        event.setTarget("target");
        assertTrue(event.getTarget().equals("target"));
        
        event.setTargetType(ControlLoopTargetType.VF);
        assertTrue(event.getTargetType().equals(ControlLoopTargetType.VF));
        
        event.setVersion("foo");
        assertTrue(event.getVersion().equals("foo"));
        
        ControlLoopEvent event2 = new TestControlLoopEvent(event);
        assertTrue(event2.isEventStatusValid());
        
        event2.setClosedLoopEventStatus(null);
        assertFalse(event2.isEventStatusValid());
    }
}
