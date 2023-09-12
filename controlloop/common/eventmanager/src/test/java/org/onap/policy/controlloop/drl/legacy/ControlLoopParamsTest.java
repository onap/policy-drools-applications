/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.controlloop.drl.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.openpojo.reflection.impl.PojoClassFactory;
import com.openpojo.validation.ValidatorBuilder;
import com.openpojo.validation.test.impl.GetterTester;
import com.openpojo.validation.test.impl.SetterTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class ControlLoopParamsTest {
    private static final String CONTROL_LOOP_NAME = "c";
    private static final String POLICY_NAME = "m";
    private static final String POLICY_SCOPE = "s";
    private static final String POLICY_VERSION = "v";

    private final ControlLoopParams  clp = new ControlLoopParams();

    /**
     * Prepare tests.
     */
    @BeforeEach
    public void setUp() {
        clp.setClosedLoopControlName(CONTROL_LOOP_NAME);
        clp.setPolicyName(POLICY_NAME);
        clp.setPolicyScope(POLICY_SCOPE);
        clp.setPolicyVersion(POLICY_VERSION);
    }

    @Test
    void testPojo() {
        var controlLoopParams = PojoClassFactory.getPojoClass(ControlLoopParams.class);
        var validator = ValidatorBuilder.create()
                                      .with(new SetterTester(), new GetterTester()).build();
        validator.validate(controlLoopParams);
    }

    @Test
    void getClosedLoopControlName() {
        assertEquals(CONTROL_LOOP_NAME, clp.getClosedLoopControlName());
    }

    @Test
    void getPolicyName() {
        assertEquals(POLICY_NAME, clp.getPolicyName());
    }

    @Test
    void getPolicyScope() {
        assertEquals(POLICY_SCOPE, clp.getPolicyScope());
    }

    @Test
    void getPolicyVersion() {
        assertEquals(POLICY_VERSION, clp.getPolicyVersion());
    }

    @Test
    void setClosedLoopControlName() {
        clp.setClosedLoopControlName(CONTROL_LOOP_NAME.toUpperCase());
        assertEquals(CONTROL_LOOP_NAME.toUpperCase(), clp.getClosedLoopControlName());
    }

    @Test
    void setPolicyName() {
        clp.setPolicyName(POLICY_NAME.toUpperCase());
        assertEquals(POLICY_NAME.toUpperCase(), clp.getPolicyName());
    }

    @Test
    void setPolicyScope() {
        clp.setPolicyScope(POLICY_SCOPE.toUpperCase());
        assertEquals(POLICY_SCOPE.toUpperCase(), clp.getPolicyScope());
    }

    @Test
    void setPolicyVersion() {
        clp.setPolicyVersion(POLICY_VERSION.toUpperCase());
        assertEquals(POLICY_VERSION.toUpperCase(), clp.getPolicyVersion());
    }

    @Test
    void testTwo() {
        var other = new ControlLoopParams();
        other.setClosedLoopControlName(CONTROL_LOOP_NAME);
        other.setPolicyName(POLICY_NAME);
        other.setPolicyScope(POLICY_SCOPE);
        other.setPolicyVersion(POLICY_VERSION);

        assertEquals(clp, other);
        assertEquals(clp.hashCode(), other.hashCode());
        assertEquals(clp.toString(), other.toString());
    }
}