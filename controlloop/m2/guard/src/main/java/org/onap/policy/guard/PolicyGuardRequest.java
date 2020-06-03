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

import java.util.UUID;

public class PolicyGuardRequest {
    private String actor;
    private String target;
    private UUID requestId;
    private String operation;

    /**
     * Construct an instance.
     * 
     * @param actor the actor
     * @param target the target
     * @param requestId the request Id
     * @param operation the operation
     */
    public PolicyGuardRequest(String actor, String target, UUID requestId, String operation) {
        super();
        this.actor = actor;
        this.target = target;
        this.requestId = requestId;
        this.operation = operation;
    }

    @Override
    public String toString() {
        return "PolicyGuardRequest [actor=" + actor + ", target=" + target + ", requestId=" + requestId + ", operation="
                + operation + "]";
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
}
