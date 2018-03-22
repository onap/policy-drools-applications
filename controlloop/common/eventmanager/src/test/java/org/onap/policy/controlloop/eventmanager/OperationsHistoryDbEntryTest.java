/*-
 * ============LICENSE_START=======================================================
 * eventmanager
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

package org.onap.policy.controlloop.eventmanager;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.util.UUID;

import org.junit.Test;

public class OperationsHistoryDbEntryTest {

    @Test
    public void testOperationsHistoryDbEntry() {
        OperationsHistoryDbEntry entry = new OperationsHistoryDbEntry();

        String actor = "Dorothy";
        entry.setActor(actor);
        assertEquals(actor, entry.getActor());

        String closedLoopName = "GoToOz";
        entry.setClosedLoopName(closedLoopName);
        assertEquals(closedLoopName, entry.getClosedLoopName());

        Timestamp endtime = new Timestamp(System.currentTimeMillis());
        entry.setEndtime(endtime);
        assertEquals(endtime, entry.getEndtime());

        String message = "I Want to go Home";
        entry.setMessage(message);
        assertEquals(message, entry.getMessage());

        String operation = "Get Home";
        entry.setOperation(operation);
        assertEquals(operation, entry.getOperation());

        String outcome = "Back in Kansas";
        entry.setOutcome(outcome);
        assertEquals(outcome, entry.getOutcome());

        String requestId = UUID.randomUUID().toString();
        entry.setRequestId(requestId);
        assertEquals(requestId, entry.getRequestId());

        long rowid = 12345;
        entry.setRowid(rowid);
        assertEquals(rowid, entry.getRowid());

        Timestamp starttime = new Timestamp(endtime.getTime() - 100);
        entry.setStarttime(starttime);
        assertEquals(starttime, entry.getStarttime());

        String subrequestId = "12321";
        entry.setSubrequestId(subrequestId);
        assertEquals(subrequestId, entry.getSubrequestId());

        String target = "WizardOfOz";
        entry.setTarget(target);
        assertEquals(target, entry.getTarget());
    }
}
