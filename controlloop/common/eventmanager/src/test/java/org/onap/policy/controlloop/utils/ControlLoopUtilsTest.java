/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.controlloop.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

public class ControlLoopUtilsTest {

    @Test
    public void testToControlLoopParams() throws Exception {
        String policyJson =
            ResourceUtils.getResourceAsString("policies/vCPE.policy.operational.input.tosca.json");
        ToscaServiceTemplate serviceTemplate = new StandardCoder().decode(policyJson, ToscaServiceTemplate.class);
        ToscaPolicy toscaPolicy =
            serviceTemplate.getToscaTopologyTemplate().getPolicies().get(0).get("operational.restart");

        ControlLoopParams params = ControlLoopUtils.toControlLoopParams(toscaPolicy);
        assertEquals("ControlLoop-vCPE-48f0c2c3-a172-4192-9ae3-052274181b6e", params.getClosedLoopControlName());
        assertEquals(toscaPolicy.getName(), params.getPolicyName());
        assertEquals(toscaPolicy.getVersion(), params.getPolicyVersion());
        assertEquals(toscaPolicy.getType() + ":" + toscaPolicy.getVersion(), params.getPolicyScope());
        assertSame(toscaPolicy, params.getToscaPolicy());

        assertNull(ControlLoopUtils.toControlLoopParams(null));
    }
}
