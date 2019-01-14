/*-
 * ============LICENSE_START=======================================================
 * guard
 * ================================================================================
 * Copyright (C) 2017-2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.guard;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;
import org.drools.core.WorkingMemory;
import org.junit.Test;

public class CallGuardTaskTest {

    private static final String REQ_ID = "1-2-3-4-5";
    private static final String REQ_MATCHER = "0+1-0+2-0+3-0+4-0+5";
    private static final String VF_COUNT_ACTOR = "SO";
    private static final String INCR_VF_COUNT_RECIPE = "VF Module Create";

    /**
     * Tests that "run" works, and inserts guard response into working memory.
     */
    @Test
    public void testRun() {
        // plain - doesn't need VF module count
        doTestRun(Util.INDETERMINATE, "act", "rec", () -> null);

        // SO actor, but plain recipe - doesn't need VF module count
        doTestRun(Util.INDETERMINATE, VF_COUNT_ACTOR, "rec", () -> null);

        // plain actor, but scale-out recipe - doesn't need VF module count
        doTestRun(Util.INDETERMINATE, "act", "VF Module Create", () -> null);

        // needs VF count
        doTestRun(Util.INDETERMINATE, VF_COUNT_ACTOR, INCR_VF_COUNT_RECIPE, () -> 22);

        // needs VF count, but it's missing ==> DENY
        doTestRun(Util.DENY, VF_COUNT_ACTOR, INCR_VF_COUNT_RECIPE, () -> null);
    }

    private void doTestRun(String status, String actor, String recipe, Supplier<Integer> vfCount) {
        WorkingMemory mockWorkingSession = mock(WorkingMemory.class);
        when(mockWorkingSession.insert(isNotNull())).thenReturn(null);
        // Create CallGuardTask and run
        CallGuardTask cgt = new CallGuardTask(mockWorkingSession, "cl", actor, recipe, "tar", REQ_ID, vfCount);
        cgt.run();
        verify(mockWorkingSession).insert(argThat((Object obj) -> {
            if (!(obj instanceof PolicyGuardResponse)) {
                return false;
            }
            // Check if the inserted response is PolicyGuardResponse, is Indeterminate,
            // and has same reqID
            PolicyGuardResponse response = (PolicyGuardResponse) obj;
            // req ID has form 00000001-0002-0003-0004-000000000005
            return status.equals(response.getResult()) && response.getRequestId().toString().matches(REQ_MATCHER);
        }));
    }
}
