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

import com.att.research.xacml.std.annotations.XACMLAction;
import com.att.research.xacml.std.annotations.XACMLRequest;
import com.att.research.xacml.std.annotations.XACMLResource;
import com.att.research.xacml.std.annotations.XACMLSubject;

@XACMLRequest(ReturnPolicyIdList = true, CombinedDecision = true)
public class PolicyGuardXacmlRequestAttributes {

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
        this.clnameID = clnameId;
        this.actorID = actorId;
        this.operationID = operationId;
        this.targetID = targetId;
        this.requestID = requestId;
        this.vfCount = vfCount;
    }

    @Override
    public String toString() {
        return "PolicyGuardXacmlRequestAttributes [actorID=" + actorID + ", operationID=" + operationID + ", targetID="
                + targetID + ", requestID=" + requestID + "]";
    }

    @XACMLSubject(includeInResults = true, attributeId = "urn:oasis:names:tc:xacml:1.0:clname:clname-id")
    String clnameID;

    @XACMLSubject(includeInResults = true, attributeId = "urn:oasis:names:tc:xacml:1.0:actor:actor-id")
    String actorID;

    @XACMLAction(includeInResults = true, attributeId = "urn:oasis:names:tc:xacml:1.0:operation:operation-id")
    String operationID;

    @XACMLResource(includeInResults = true, attributeId = "urn:oasis:names:tc:xacml:1.0:target:target-id")
    String targetID;

    @XACMLResource(includeInResults = true, attributeId = "urn:oasis:names:tc:xacml:1.0:request:request-id")
    String requestID;

    @XACMLResource(includeInResults = true, attributeId = "urn:oasis:names:tc:xacml:1.0:request:vf-count")
    Integer vfCount;

    public String getActorID() {
        return actorID;
    }

    public void setActorID(String actorID) {
        this.actorID = actorID;
    }

    public String getOperationID() {
        return operationID;
    }

    public void setOperationID(String operationID) {
        this.operationID = operationID;
    }

    public String getTargetID() {
        return targetID;
    }

    public void setTargetID(String targetID) {
        this.targetID = targetID;
    }

    public String getRequestID() {
        return requestID;
    }

    public void setRequestID(String requestID) {
        this.requestID = requestID;
    }

    public String getClnameID() {
        return clnameID;
    }

    public void setClnameID(String clnameID) {
        this.clnameID = clnameID;
    }

    public Integer getVfCount() {
        return vfCount;
    }

    public void setVfCount(Integer vfCount) {
        this.vfCount = vfCount;
    }
}
