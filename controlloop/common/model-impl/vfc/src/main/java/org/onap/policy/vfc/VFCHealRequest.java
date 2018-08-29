/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2017 Intel Corp. All rights reserved.
 * Modifications Copyright (C) 2018 AT&T Corporation. All rights reserved.
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

package org.onap.policy.vfc;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class VFCHealRequest implements Serializable {

    private static final long serialVersionUID = -7341931593089709247L;

    @SerializedName("vnfInstanceId")
    private String vnfInstanceId;

    @SerializedName("cause")
    private String cause;

    @SerializedName("additionalParams")
    private VFCHealAdditionalParams additionalParams;

    public VFCHealRequest() {
        // Default constructor for VFCHealRequest
    }

    public String getVnfInstanceId() {
        return vnfInstanceId;
    }

    public void setVnfInstanceId(String vnfInstanceId) {
        this.vnfInstanceId = vnfInstanceId;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

    public VFCHealAdditionalParams getAdditionalParams() {
        return additionalParams;
    }

    public void setAdditionalParams(VFCHealAdditionalParams additionalParams) {
        this.additionalParams = additionalParams;
    }
}
