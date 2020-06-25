/*-
 * ============LICENSE_START=======================================================
 * m2/base
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

package org.onap.policy.m2.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.onap.policy.controlloop.ControlLoopEvent;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;

public class ActorOperationTest {

    public static final String ACTOR_NAME = "test";
    public static final String STATE = "COMPLETE";

    public static class TestOperation implements Operation {
        private static final long serialVersionUID = 1L;

        @Override
        public Object getRequest() throws ControlLoopException {
            return "request";
        }

        @Override
        public Policy getPolicy() {
            return null;
        }

        @Override
        public String getState() {
            return STATE;
        }

        @Override
        public int getAttempt() {
            return 0;
        }

        @Override
        public PolicyResult getResult() {
            return PolicyResult.SUCCESS;
        }

        @Override
        public String getMessage() {
            return "success";
        }

        @Override
        public void incomingMessage(Object object) {
            return;
        }

        @Override
        public void timeout() {
            return;
        }

    }

    public static class TestActor implements Actor {

        @Override
        public String getName() {
            return ACTOR_NAME;
        }

        @Override
        public Operation createOperation(Transaction transaction, Policy policy, ControlLoopEvent onset, int attempt) {
            return new TestOperation();
        }

    }

    @Test
    public void getNameTest() {
        Actor actor = new TestActor();
        assertEquals(ACTOR_NAME, actor.getName());
    }

    @Test
    public void testOperation() throws ControlLoopException {
        Actor actor = new TestActor();
        Operation operation = actor.createOperation(null, null, null, 0);
        assertNotNull(operation);
        assertEquals("request", operation.getRequest());
        assertNull(operation.getPolicy());
        assertEquals(STATE, operation.getState());
        assertEquals(0, operation.getAttempt());
        assertEquals(PolicyResult.SUCCESS, operation.getResult());
        assertEquals("success", operation.getMessage());
    }

}
