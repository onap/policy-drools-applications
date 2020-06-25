/*-
 * ============LICENSE_START=======================================================
 * adapters
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.m2.adapters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.onap.policy.controlloop.ControlLoopEvent;
import org.onap.policy.controlloop.ControlLoopNotification;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.m2.base.OnsetAdapter;

public class VirtualOnsetAdapterTest {

    @Test
    public void test() {
        VirtualOnsetAdapter.register();
        VirtualControlLoopEvent virtualControlLoopEvent = new VirtualControlLoopEvent();
        VirtualOnsetAdapter virtualOnsetAdapter =
            VirtualOnsetAdapter.class.cast(OnsetAdapter.get(virtualControlLoopEvent));
        assertNotNull(virtualOnsetAdapter);

        ControlLoopNotification notification = virtualOnsetAdapter.createNotification(virtualControlLoopEvent);
        assertNotNull(notification);
        // we want an exact class match, so 'instanceOf' is not being used
        assertEquals(VirtualControlLoopNotification.class, notification.getClass());

        ControlLoopEvent controlLoopEvent = new ControlLoopEvent() {
            private static final long serialVersionUID = 1L;
        };
        notification = virtualOnsetAdapter.createNotification(controlLoopEvent);
        assertNotNull(notification);
    }
}
