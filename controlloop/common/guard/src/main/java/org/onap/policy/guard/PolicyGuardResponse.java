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

import java.util.UUID;

public class PolicyGuardResponse {
    private UUID requestId;
    private String operation;
    private String result;

    /**
     * Create an instance.
     * 
     * @param result the result
     * @param req the request Id
     * @param op the operation
     */
    public PolicyGuardResponse(String result, UUID req, String op) {
        this.result = result;
        this.requestId = req;
        this.operation = op;
    }

    @Override
    public String toString() {
        return "PolicyGuardResponse [requestID=" + requestId + ", operation=" + operation + ", result=" + result + "]";
    }

    public UUID getRequestID() {
        return requestId;
    }

    public void setRequestID(UUID requestId) {
        this.requestId = requestId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
}
