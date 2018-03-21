/*-
 * ============LICENSE_START=======================================================
 * eventmanager
 * ================================================================================
 * Copyright (C) 2018 Ericsson. All rights reserved.
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
import static org.junit.Assert.fail;

import org.junit.Test;
import org.onap.policy.controlloop.impl.ControlLoopPublisherJUnitImpl;

public class ControlLoopPublisherTest {
    @Test
    public void testControlLoopPublisher() throws ControlLoopException {
        ControlLoopPublisher publisher =
                new ControlLoopPublisher.Factory().buildLogger(ControlLoopPublisherJUnitImpl.class.getCanonicalName());
        assertNotNull(publisher);

        try {
            publisher.publish(Double.valueOf(3));
            fail("test should throw an exception here");
        } catch (Exception e) {
            assertEquals("publish() method is not implemented on "
                    + "org.onap.policy.controlloop.impl.ControlLoopPublisherJUnitImpl", e.getMessage());
        }

        try {
            new ControlLoopPublisher.Factory().buildLogger("java.lang.String");
            fail("test should throw an exception here");
        } catch (Exception e) {
            assertEquals("Cannot load class java.lang.String as a control loop publisher", e.getMessage());
        }
    }
}
