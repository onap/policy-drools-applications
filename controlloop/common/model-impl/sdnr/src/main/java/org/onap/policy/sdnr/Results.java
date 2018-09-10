/*-
 * ============LICENSE_START=======================================================
 * sdnr
 * ================================================================================
 * Copyright (C) 2018 Wipro Limited Intellectual Property. All rights reserved.
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

package org.onap.policy.sdnr;

import com.google.gson.annotations.SerializedName;

public class Results {
    private Status status;

    @SerializedName(value = "generic-pnf.pnf-id")
    private String pnfId;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getPnfId() {
        return pnfId;
    }

    public void setPnfId(String pnfId) {
        this.pnfId = pnfId;
    }

    @Override
    public String toString() {
        return "Results [status = " + status + ", pnfId = " + pnfId + "]";
    }

    public Results(Status status, String pnfId) {
        super();
        this.status = status;
        this.pnfId = pnfId;
    }
}
