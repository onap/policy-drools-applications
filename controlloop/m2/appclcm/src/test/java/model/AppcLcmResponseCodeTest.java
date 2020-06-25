/*-
 * ============LICENSE_START=======================================================
 * appclcm
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

package model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.onap.policy.m2.appclcm.model.AppcLcmResponseCode;


public class AppcLcmResponseCodeTest {

    @Test
    public void test() {
        assertEquals(AppcLcmResponseCode.ACCEPTED, AppcLcmResponseCode.toResponseValue(100));
        assertEquals(AppcLcmResponseCode.ERROR, AppcLcmResponseCode.toResponseValue(200));
        assertEquals(AppcLcmResponseCode.REJECT, AppcLcmResponseCode.toResponseValue(300));
        assertEquals(AppcLcmResponseCode.SUCCESS, AppcLcmResponseCode.toResponseValue(400));
        assertEquals(AppcLcmResponseCode.FAILURE, AppcLcmResponseCode.toResponseValue(450));
        assertEquals(AppcLcmResponseCode.PARTIAL_SUCCESS, AppcLcmResponseCode.toResponseValue(500));
        assertEquals(AppcLcmResponseCode.PARTIAL_FAILURE, AppcLcmResponseCode.toResponseValue(501));
        assertNull(AppcLcmResponseCode.toResponseValue(600));
    }
}
