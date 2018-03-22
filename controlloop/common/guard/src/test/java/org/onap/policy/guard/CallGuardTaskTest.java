/*-
 * ============LICENSE_START=======================================================
 * guard
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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

import org.drools.core.impl.StatefulKnowledgeSessionImpl;
import org.junit.Test;

public class CallGuardTaskTest {

    static final String REQ_ID = "1-2-3-4-5";
    static final String REQ_MATCHER = "0+1-0+2-0+3-0+4-0+5";

    @Test
    /**
     * Tests that the run method inserts guard response into working memory.
     */
    public void testRun() {
        // Create mock working session
        StatefulKnowledgeSessionImpl mockWorkingSession = mock(StatefulKnowledgeSessionImpl.class);
        when(mockWorkingSession.insert(isNotNull())).thenReturn(null);
        // Create CallGuardTask and run
        CallGuardTask cgt = new CallGuardTask(mockWorkingSession, "cl", "act", "rec", "tar", REQ_ID);
        cgt.run();
        verify(mockWorkingSession).insert(argThat((Object obj) -> {
            if (!(obj instanceof PolicyGuardResponse)) {
                return false;
            }
            // Check if the inserted response is PolicyGuardResponse, is Indeterminate, and has same
            // reqID
            PolicyGuardResponse response = (PolicyGuardResponse) obj;
            // req ID has form 00000001-0002-0003-0004-000000000005
            return Util.INDETERMINATE.equals(response.getResult())
                    && response.getRequestID().toString().matches(REQ_MATCHER);
        }));

    }

}
