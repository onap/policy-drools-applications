/*-
 * ============LICENSE_START=======================================================
 * lcm
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

package model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.onap.policy.m2.lcm.model.LcmResponseCode;


public class LcmResponseCodeTest {

    @Test
    public void test() {
        assertEquals(LcmResponseCode.ACCEPTED, LcmResponseCode.toResponseValue(100));
        assertEquals(LcmResponseCode.ERROR, LcmResponseCode.toResponseValue(200));
        assertEquals(LcmResponseCode.REJECT, LcmResponseCode.toResponseValue(300));
        assertEquals(LcmResponseCode.SUCCESS, LcmResponseCode.toResponseValue(400));
        assertEquals(LcmResponseCode.FAILURE, LcmResponseCode.toResponseValue(450));
        assertEquals(LcmResponseCode.PARTIAL_SUCCESS, LcmResponseCode.toResponseValue(500));
        assertEquals(LcmResponseCode.PARTIAL_FAILURE, LcmResponseCode.toResponseValue(501));
        assertNull(LcmResponseCode.toResponseValue(600));
    }
}
