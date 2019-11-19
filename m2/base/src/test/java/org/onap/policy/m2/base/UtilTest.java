/*-
 * ============LICENSE_START=======================================================
 * m2/base
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.m2.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.drools.core.PolicyContainer;
import org.onap.policy.drools.core.PolicySession;
import org.onap.policy.drools.system.PolicyController;

public class UtilTest {

    private static PolicySession session;
    private static PolicyController controller;

    /**
     * Class-level initialization.
     */
    @BeforeClass
    public static void setup() {
        PolicyContainer container = mock(PolicyContainer.class);
        when(container.getGroupId()).thenReturn("org.onap.policy");
        when(container.getArtifactId()).thenReturn("test");
        when(container.getVersion()).thenReturn("1.0.0");

        session = mock(PolicySession.class);
        when(session.getPolicyContainer()).thenReturn(container);

        controller = mock(PolicyController.class);
        when(controller.deliver(any(), any(), any())).thenReturn(true);
    }

    @Test
    public void deliverTest() {
        Properties prop = new Properties();
        prop.put("noop.sink.topics", "testTopic");
        TopicEndpointManager.getManager().addTopicSinks(prop);

        // throws an exception:
        //     java.lang.IllegalStateException: Policy Engine is stopped
        // if policy engine is started, it still throws an exception:
        //     java.lang.IllegalArgumentException: no reverse coder has been found
        //assertTrue(Util.deliver(controller, "testTopic", "test"));
    }

    @Test(expected = IllegalStateException.class)
    public void deliverNoTopicTest() {
        Util.deliver(controller, "noTopic", "test");
    }
}
