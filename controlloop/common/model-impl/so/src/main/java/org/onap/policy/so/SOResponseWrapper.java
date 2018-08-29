/*-
 * ============LICENSE_START=======================================================
 * so
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

package org.onap.policy.so;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class SOResponseWrapper implements Serializable {

    private static final long serialVersionUID = 7673023687132889069L;

    @SerializedName("SoResponse")
    private SOResponse soResponse;

    private transient String requestID;

    public SOResponseWrapper(SOResponse response, String reqID) {
        this.soResponse = response;
        this.requestID = reqID;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SOResponseWrapper other = (SOResponseWrapper) obj;
        if (soResponse == null) {
            if (other.soResponse != null) {
                return false;
            }
        }
        else if (!soResponse.equals(other.soResponse)) {
            return false;
        }
        if (requestID == null) {
            if (other.requestID != null) {
                return false;
            }
        }
        else if (!requestID.equals(other.requestID)) {
            return false;
        }
        return true;
    }

    public String getRequestID() {
        return requestID;
    }

    public SOResponse getSoResponse() {
        return soResponse;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((soResponse == null) ? 0 : soResponse.hashCode());
        result = prime * result + ((requestID == null) ? 0 : requestID.hashCode());
        return result;
    }

    public void setRequestID(String requestId) {
        this.requestID = requestId;
    }

    public void setSoResponse(SOResponse response) {
        soResponse = response;
    }

    @Override
    public String toString() {
        return "SOResponseWrapper [SOResponse=" + soResponse + ", RequestID=" + requestID + "]";
    }

}
