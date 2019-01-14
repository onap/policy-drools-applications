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

import com.att.research.xacml.std.annotations.XACMLAction;
import com.att.research.xacml.std.annotations.XACMLRequest;
import com.att.research.xacml.std.annotations.XACMLResource;
import com.att.research.xacml.std.annotations.XACMLSubject;

@XACMLRequest(ReturnPolicyIdList = true, CombinedDecision = true)
public class PolicyGuardXacmlRequestAttributes {

    @XACMLSubject(includeInResults = true, attributeId = "urn:oasis:names:tc:xacml:1.0:clname:clname-id")
    String clnameId;

    @XACMLSubject(includeInResults = true, attributeId = "urn:oasis:names:tc:xacml:1.0:actor:actor-id")
    String actorId;

    @XACMLAction(includeInResults = true, attributeId = "urn:oasis:names:tc:xacml:1.0:operation:operation-id")
    String operationId;

    @XACMLResource(includeInResults = true, attributeId = "urn:oasis:names:tc:xacml:1.0:target:target-id")
    String targetId;

    @XACMLResource(includeInResults = true, attributeId = "urn:oasis:names:tc:xacml:1.0:request:request-id")
    String requestId;

    @XACMLResource(includeInResults = true, attributeId = "urn:oasis:names:tc:xacml:1.0:request:vf-count")
    Integer vfCount;

    /**
     * Construct an instance.
     * 
     * @param clnameId the control loop Id
     * @param actorId the actor Id
     * @param operationId the operation Id
     * @param targetId the target Id
     * @param requestId the request Id
     * @param vfCount the new number of VF Modules
     */
    public PolicyGuardXacmlRequestAttributes(String clnameId, String actorId, String operationId, String targetId,
            String requestId, Integer vfCount) {
        super();
        this.clnameId = clnameId;
        this.actorId = actorId;
        this.operationId = operationId;
        this.targetId = targetId;
        this.requestId = requestId;
        this.vfCount = vfCount;
    }

    @Override
    public String toString() {
        return "PolicyGuardXacmlRequestAttributes [actorId=" + actorId + ", operationId=" + operationId + ", targetId="
                + targetId + ", requestId=" + requestId + "]";
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getClnameId() {
        return clnameId;
    }

    public void setClnameId(String clnameId) {
        this.clnameId = clnameId;
    }

    public Integer getVfCount() {
        return vfCount;
    }

    public void setVfCount(Integer vfCount) {
        this.vfCount = vfCount;
    }
}
