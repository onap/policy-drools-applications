/*-
 * ============LICENSE_START=======================================================
 * controlloop
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

package org.onap.policy.coordination;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class CoordinationDirectiveTest {

    @Test
    public void test() {

        CoordinationDirective cd1 = new CoordinationDirective();

        assertNotNull(cd1);

        assertNull(cd1.getControlLoop());
        assertNull(cd1.getCoordinationFunction());

        cd1.setControlLoop(Arrays.asList("cl1", "cl2"));
        cd1.setCoordinationFunction("firstBlocksSecond");

        assertNotNull(cd1.getControlLoop());
        assertNotNull(cd1.getControlLoop(0));
        assertNotNull(cd1.getControlLoop(1));
        assertThatThrownBy(() -> {
            cd1.getControlLoop(2);
        }).isInstanceOf(IndexOutOfBoundsException.class);
        assertNotNull(cd1.getCoordinationFunction());
    }
}
