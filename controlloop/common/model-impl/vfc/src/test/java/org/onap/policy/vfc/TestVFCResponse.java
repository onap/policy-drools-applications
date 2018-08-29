/*-
 * ============LICENSE_START=======================================================
 * vfc
 * ================================================================================
 * Copyright (C) 2018 Ericsson. All rights reserved.
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

package org.onap.policy.vfc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class TestVFCResponse {

    @Test
    public void testVFCResponse() {
        VFCResponse response = new VFCResponse();
        assertNotNull(response);
        assertNotEquals(0, response.hashCode());

        String jobId = "GetToOz";
        response.setJobId(jobId);
        assertEquals(jobId, response.getJobId());

        String requestId = "Get Home";
        response.setRequestId(requestId);
        assertEquals(requestId, response.getRequestId());

        VFCResponseDescriptor responseDescriptor = new VFCResponseDescriptor();
        response.setResponseDescriptor(responseDescriptor);
        assertEquals(responseDescriptor, response.getResponseDescriptor());

        assertNotEquals(0, response.hashCode());
    }
}
