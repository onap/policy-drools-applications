/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2018 Samsung Electronics Co., Ltd. All rights reserved.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.sdnr.util;

import java.util.HashMap;
import java.util.Map;

public enum StatusCodeEnum {

    ACCEPTED(100, "ACCEPTED"), SUCCESS(200, "SUCCESS"), ERROR(400, "ERROR"),
    PARTIAL_SUCCESS(500, "PARTIAL SUCCESS");

    private int code;
    private String status;

    private static Map<Integer, StatusCodeEnum> map = new HashMap<Integer, StatusCodeEnum>();

    static {
        for (StatusCodeEnum statusEnum : StatusCodeEnum.values()) {
            map.put(statusEnum.code, statusEnum);
        }
    }

    StatusCodeEnum(final int val, final String status) {
        code = val;
        this.status = status;
    }

    public static StatusCodeEnum valueOf(int val) {
        return map.get(val);
    }

    public String toString() {
        return this.status;
    }
}
