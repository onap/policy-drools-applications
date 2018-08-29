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

public class SOResponse implements Serializable {

    private static final long serialVersionUID = -3283942659786236032L;

    @SerializedName("requestReferences")
    private SORequestReferences requestReferences;

    @SerializedName("requestError")
    private SORequestError requestError;

    @SerializedName("request")
    private SORequest request;

    private int httpResponseCode;

    public SOResponse() {
        // required by author
    }

    public int getHttpResponseCode() {
        return httpResponseCode;
    }

    public SORequest getRequest() {
        return request;
    }

    public SORequestError getRequestError() {
        return requestError;
    }

    public SORequestReferences getRequestReferences() {
        return requestReferences;
    }

    public void setHttpResponseCode(int httpResponseCode) {
        this.httpResponseCode = httpResponseCode;
    }

    public void setRequest(SORequest request) {
        this.request = request;
    }

    public void setRequestError(SORequestError requestError) {
        this.requestError = requestError;
    }

    public void setRequestReferences(SORequestReferences requestReferences) {
        this.requestReferences = requestReferences;
    }

}
