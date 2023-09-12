/*-
 * ============LICENSE_START=======================================================
 * controlloop
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.coordination;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class UtilTest {

    @Test
    void test() {

        var filename = "src/test/resources/test_coordination_directive.yaml";
        var cd1 = Util.loadCoordinationDirectiveFromFile(filename);

        assertNotNull(cd1);

        assertNotNull(cd1.getControlLoop(0));
        assertNotNull(cd1.getControlLoop(1));
        assertNotNull(cd1.getCoordinationFunction());

        assertEquals("cl1", cd1.getControlLoop(0));
        assertEquals("cl2", cd1.getControlLoop(1));
        assertEquals("cf", cd1.getCoordinationFunction());

        filename = "src/test/resources/non_existent_coordination_directive.yaml";
        var cd2 = Util.loadCoordinationDirectiveFromFile(filename);

        assertNull(cd2);

        assertThatNullPointerException().isThrownBy(() -> {
            Util.generateXacmlFromCoordinationDirective(cd2, "");
        });
    }
}
