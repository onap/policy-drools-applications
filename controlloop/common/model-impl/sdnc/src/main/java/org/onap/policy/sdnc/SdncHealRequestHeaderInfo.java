/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018 Huawei. All rights reserved.
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

package org.onap.policy.sdnc;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class SdncHealRequestHeaderInfo implements Serializable {

    private static final long serialVersionUID = 3208673205100673119L;

    @SerializedName("svc-request-id")
    private String svcRequestId;

    @SerializedName("svc-action")
    private String svcAction;

    public SdncHealRequestHeaderInfo() {
        // Default constructor for SdncHealActionVmInfo
    }

    public String getSvcRequestId() {
        return svcRequestId;
    }

    public void setSvcRequestId(String svcRequestId) {
        this.svcRequestId = svcRequestId;
    }

    public String getSvcAction() {
        return svcAction;
    }

    public void setSvcAction(String svcAction) {
        this.svcAction = svcAction;
    }
}
