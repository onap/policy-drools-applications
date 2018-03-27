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

package org.onap.policy.drools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.onap.policy.appc.CommonHeader;
import org.onap.policy.appc.Request;
import org.onap.policy.appclcm.LcmCommonHeader;
import org.onap.policy.appclcm.LcmRequest;
import org.onap.policy.appclcm.LcmRequestWrapper;
import org.onap.policy.controlloop.ControlLoopNotification;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.drools.impl.PolicyEngineJUnitImpl;

public class DroolsPolicyEngineTest {
    @Test
    public void testDroolsPolicyEngine() {
        PolicyEngineJUnitImpl pe = new PolicyEngineJUnitImpl();
        assertNotNull(pe);

        pe.addListener(new TestPolicyEngineListener());
        pe.notifyListeners("TheWizardOfOz");

        pe.subscribe("OmniBus", "TheWizardOfOz");

        pe.deliver("OmniBus", "TheWizardOfOz", "Dorothy");

        pe.subscribe("OmniBus", "TheWizardOfOz");
        pe.subscribe("OmniBus", "ThisTopicDoesNotExist");

        ControlLoopNotification notification = new VirtualControlLoopNotification();
        pe.deliver("OmniBus", "TheWizardOfOz", notification);

        Request request = new Request();
        request.setCommonHeader(new CommonHeader());
        request.getCommonHeader().setSubRequestId("12321");
        pe.deliver("OmniBus", "TheWizardOfOz", request);

        LcmRequestWrapper lcmRw = new LcmRequestWrapper();
        lcmRw.setBody(new LcmRequest());
        lcmRw.getBody().setCommonHeader(new LcmCommonHeader());
        lcmRw.getBody().getCommonHeader().setSubRequestId("54321");
        pe.deliver("OmniBus", "TheWizardOfOz", lcmRw);
    }

    private class TestPolicyEngineListener implements PolicyEngineListener {
        @Override
        public void newEventNotification(String topic) {
            assertEquals("TheWizardOfOz", topic);
        }
    }
}
