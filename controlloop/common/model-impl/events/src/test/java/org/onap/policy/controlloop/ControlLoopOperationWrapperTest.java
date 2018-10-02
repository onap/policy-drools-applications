/*-
 * ============LICENSE_START=======================================================
 * controlloop
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.controlloop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;
import org.junit.Test;

public class ControlLoopOperationWrapperTest {

    @Test
    public void test() {
        ControlLoopOperationWrapper wrapper = new ControlLoopOperationWrapper();
        
        assertNotNull(wrapper);
        
        ControlLoopOperation operation = new ControlLoopOperation();
        wrapper.setOperation(operation);
        UUID id = UUID.randomUUID();
        wrapper.setRequestId(id);

        ControlLoopOperationWrapper wrapper2 = new ControlLoopOperationWrapper(wrapper.getRequestId(), 
                wrapper.getOperation());
        
        assertEquals(operation, wrapper2.getOperation());
        assertEquals(id, wrapper2.getRequestId());
        
        
    }
}
