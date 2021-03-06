/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests NamedRunner. The tests don't do much, because all we really want to check is
 * which tests are executed based on the {@link TestNames} annotation.
 */
@RunWith(NamedRunner.class)
@TestNames(names = {"testAbc", "testDef", "testIgnore"}, prefixes = {"testGhi", "testJkl"})
public class NamedRunnerTest {

    private static int testCount = 0;

    @AfterClass
    public static void tearDownAfterClass() {
        assertEquals(5, testCount);
    }

    @Test
    public void testAbc() {
        checkTest();
    }

    @Test
    public void testAbc2() {
        fail("should not run");
    }

    @Test
    public void testDef() {
        checkTest();
    }

    /*
     * Note: this test is purposely marked with the "Ignore" annotation to verify that the
     * NamedRunner skips over it, hence the sonar issue is being suppressed.
     */
    @Test
    @Ignore
    public void testIgnore() {      // NOSONAR
        fail("should not run");
    }

    @Test
    public void testGhi1() {
        checkTest();
    }

    @Test
    public void testGhi2() {
        checkTest();
    }

    @Test
    public void testJkl() {
        checkTest();
    }


    private static void checkTest() {
        ++testCount;
    }
}
