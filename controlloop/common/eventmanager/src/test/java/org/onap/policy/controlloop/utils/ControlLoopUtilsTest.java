/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023, 2025 OpenInfra Foundation Europe. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

class ControlLoopUtilsTest {

    @Test
    void testToControlLoopParams() throws Exception {
        var policyJson =
            ResourceUtils.getResourceAsString("policies/vCPE.policy.operational.input.tosca.json");
        var serviceTemplate = new StandardCoder().decode(policyJson, ToscaServiceTemplate.class);
        var toscaPolicy =
            serviceTemplate.getToscaTopologyTemplate().getPolicies().get(0).get("operational.restart");

        var params = ControlLoopUtils.toControlLoopParams(toscaPolicy);
        Assertions.assertNotNull(params);
        assertEquals("ControlLoop-vCPE-48f0c2c3-a172-4192-9ae3-052274181b6e", params.getClosedLoopControlName());
        assertEquals(toscaPolicy.getName(), params.getPolicyName());
        assertEquals(toscaPolicy.getVersion(), params.getPolicyVersion());
        assertEquals(toscaPolicy.getType() + ":" + toscaPolicy.getVersion(), params.getPolicyScope());
        assertSame(toscaPolicy, params.getToscaPolicy());

        assertNull(ControlLoopUtils.toControlLoopParams(null));
    }
}
