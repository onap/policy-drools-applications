/*-
 * ============LICENSE_START=======================================================
 * aai
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

package org.onap.policy.aai;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class AaiGetResponse implements Serializable {

    /**
     * define common fields for AAIGETVnfResponse and AAIGETVserverResponse.
     */
    private static final long serialVersionUID = 7311418432051756161L;

    @SerializedName("in-maint")
    private String inMaint;

    @SerializedName("is-closed-loop-disabled")
    private String isClosedLoopDisabled;

    @SerializedName("model-invariant-id")
    private String modelInvariantId;

    @SerializedName("resource-version")
    private String resourceVersion;

    @SerializedName("relationship-list")
    private RelationshipList relationshipList;

    @SerializedName("requestError")
    private AaiNqRequestError requestError;

    public String getInMaint() {
        return inMaint;
    }

    public String getIsClosedLoopDisabled() {
        return isClosedLoopDisabled;
    }

    public String getModelInvariantId() {
        return modelInvariantId;
    }

    public String getResourceVersion() {
        return resourceVersion;
    }

    public RelationshipList getRelationshipList() {
        return relationshipList;
    }

    public AaiNqRequestError getRequestError() {
        return requestError;
    }

    public void setInMaint(String inMaint) {
        this.inMaint = inMaint;
    }

    public void setIsClosedLoopDisabled(String isClosedLoopDisabled) {
        this.isClosedLoopDisabled = isClosedLoopDisabled;
    }

    public void setModelInvariantId(String modelInvariantId) {
        this.modelInvariantId = modelInvariantId;
    }

    public void setResourceVersion(String resourceVersion) {
        this.resourceVersion = resourceVersion;
    }

    public void setRelationshipList(RelationshipList relationshipList) {
        this.relationshipList = relationshipList;
    }

    public void setRequestError(AaiNqRequestError requestError) {
        this.requestError = requestError;
    }

}
