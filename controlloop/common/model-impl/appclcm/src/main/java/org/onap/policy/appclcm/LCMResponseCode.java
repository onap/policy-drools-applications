/*-
 * ============LICENSE_START=======================================================
 * appclcm
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.appclcm;

public class LCMResponseCode {   
    
    /* These fields define the key to the response code value. */
    public static final String ACCEPTED = "ACCEPTED";
    public static final String ERROR = "ERROR";
    public static final String REJECT = "REJECT";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILURE = "FAILURE";
    public static final String PARTIAL_SUCCESS = "PARTIAL SUCCESS";
    public static final String PARTIAL_FAILURE = "PARTIAL FAILURE";
    
    private Integer code;

    private LCMResponseCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

    @Override
    public String toString() {
        return Integer.toString(this.code);
    }

    /**
     * Translates the code to a string value that represents the meaning of the code.
     * 
     * @param code
     *            the numeric value that is returned by APPC based on success,
     *            failure, etc. of the action requested
     * @return the string value equivalent of the APPC response code
     */
    public static String toResponseValue(int code) {
        if (code == 100) {
            return ACCEPTED;
        }
        else if (code == 200) {
            return ERROR;
        }
        else if (code >= 300 && code <= 313) {
            return REJECT;
        }
        else if (code == 400) {
            return SUCCESS;
        }
        else if (code == 450 || (code >= 401 && code <= 406)) {
            return FAILURE;
        }
        else if (code == 500) {
            return PARTIAL_SUCCESS;
        }
        else if (code >= 501 && code <= 599) {
            return PARTIAL_FAILURE;
        }
        return null;
    }
}
