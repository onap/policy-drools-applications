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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class CoordinationDirectiveTest {

    @Test
    public void test() {

        CoordinationDirective cd1 = new CoordinationDirective();

        assertNotNull(cd1);

        assertNull(cd1.getControlLoopOne());
        assertNull(cd1.getControlLoopTwo());
        assertNull(cd1.getCoordinationFunction());

        cd1.setControlLoopOne("cl1");
        cd1.setControlLoopTwo("cl2");
        cd1.setCoordinationFunction("firstBlocksSecond");

        assertNotNull(cd1.getControlLoopOne());
        assertNotNull(cd1.getControlLoopTwo());
        assertNotNull(cd1.getCoordinationFunction());
    }
}
