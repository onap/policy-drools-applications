/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.controlloop.common.rules.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Tests NamedRunner. The tests don't do much, because all we really want to check is
 * which tests are executed based on the {@link TestNames} annotation.
 */
@ExtendWith(NamedRunner.class)
@TestNames(names = {"testAbc", "testDef", "testIgnore"}, prefixes = {"testGhi", "testJkl"})
class NamedRunnerTest {

    private static int testCount = 0;

    @AfterAll
    static void tearDownAfterClass() {
        assertEquals(5, testCount);
    }

    @Test
    void testAbc() {
        checkTest();
    }

    @Test
    void testAbc2() {
        fail("should not run");
    }

    @Test
    void testDef() {
        checkTest();
    }

    /*
     * Note: this test is purposely marked with the "Ignore" annotation to verify that the
     * NamedRunner skips over it, hence the sonar issue is being suppressed.
     */
    @Test
    @Disabled
    public void testIgnore() {      // NOSONAR
        fail("should not run");
    }

    @Test
    void testGhi1() {
        checkTest();
    }

    @Test
    void testGhi2() {
        checkTest();
    }

    @Test
    void testJkl() {
        checkTest();
    }


    private static void checkTest() {
        ++testCount;
    }
}
