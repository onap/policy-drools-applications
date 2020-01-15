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

package org.onap.policy.m2.base.impl;

import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.m2.base.Operation;

public class OperationImpl implements Operation {

    private static final long serialVersionUID = 1L;

    private String state = "TEST.PENDING";

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
        return state;
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
        state = "TEST.PENDING";
        return "test";
    }

    @Override
    public void incomingMessage(Object object) {
        state = "TEST.COMPLETE";
    }

    @Override
    public void timeout() {
        state = "TEST.COMPLETE";
    }

}