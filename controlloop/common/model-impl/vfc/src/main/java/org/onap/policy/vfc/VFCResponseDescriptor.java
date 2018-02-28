/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2017 Intel Corp. All rights reserved.
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

import java.io.Serializable;
import java.util.List;
import com.google.gson.annotations.SerializedName;

public class VFCResponseDescriptor implements Serializable {

    private static final long serialVersionUID = 6827782899144150158L;

    @SerializedName("progress")
    private String progress;

    @SerializedName("status")
    private String status;

    @SerializedName("statusDescription")
    private String statusDescription;

    @SerializedName("errorCode")
    private String errorCode;

    @SerializedName("responseId")
    private String responseId;

    @SerializedName("responseHistoryList") 
    private List<VFCResponseDescriptor> responseHistoryList;

    public VFCResponseDescriptor() {
		// Default constructor for VFCResponseDescriptor
    }

    public String getStatus() {
	return status;
    }

	public String getProgress() {
		return progress;
	}

	public void setProgress(String progress) {
		this.progress = progress;
	}

	public String getStatusDescription() {
		return statusDescription;
	}

	public void setStatusDescription(String statusDescription) {
		this.statusDescription = statusDescription;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public String getResponseId() {
		return responseId;
	}

	public void setResponseId(String responseId) {
		this.responseId = responseId;
	}

	public List<VFCResponseDescriptor> getResponseHistoryList() {
		return responseHistoryList;
	}

	public void setResponseHistoryList(List<VFCResponseDescriptor> responseHistoryList) {
		this.responseHistoryList = responseHistoryList;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
