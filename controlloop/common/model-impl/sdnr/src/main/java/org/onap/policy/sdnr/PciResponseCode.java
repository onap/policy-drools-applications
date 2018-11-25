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

import java.util.HashMap;
import java.util.Map;

public class PciResponseCode {

    /* These fields define the key to the response code value. */
    public static final String ACCEPTED = "ACCEPTED";
    public static final String ERROR = "ERROR";
    public static final String REJECT = "REJECT";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILURE = "FAILURE";
    public static final String PARTIAL_SUCCESS = "PARTIAL SUCCESS";
    public static final String PARTIAL_FAILURE = "PARTIAL FAILURE";

    private Integer code;

    protected PciResponseCode(final int code) {
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
     * @param code the numeric value that is returned by SDNR based on success, failure, etc. of the action requested
     * @return the string value equivalent of the SDNR response code
     */
    public static String toResponseValue(int code) {
        Map<Integer, String> map = new HashMap<>();
        map.put(100, ACCEPTED);
        map.put(200, SUCCESS);
        map.put(400, ERROR);
        map.put(500, PARTIAL_SUCCESS);
        String resp;
        resp = map.get(code);
        if (resp == null && (code >= 300 && code <= 313)) {
            resp = REJECT;
        } else if (resp == null && (code == 450 || (code >= 401 && code <= 406))) {
            resp = FAILURE;
        } else if (resp == null && (code >= 501 && code <= 599)) {
            resp = PARTIAL_FAILURE;
        }
        return resp;
    }
}
