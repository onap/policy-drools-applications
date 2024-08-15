/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023-2024 Nordix Foundation.
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

package org.onap.policy.controlloop.ophistory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.controlloop.ophistory.OperationHistoryDataManagerParams.OperationHistoryDataManagerParamsBuilder;

class OperationHistoryDataManagerParamsTest {
    private static final String CONTAINER = "my-container";
    private static final int BATCH_SIZE = 10;
    private static final int MAX_QUEUE_LENGTH = 20;
    private static final String MY_PASS = "my-pass";
    private static final String MY_PU = "my-pu";
    private static final String MY_DRIVER = "my-driver";
    private static final String MY_URL = "my-url";
    private static final String MY_USER = "my-user";

    private OperationHistoryDataManagerParams params;

    @BeforeEach
    public void setUp() {
        params = makeBuilder().build();
    }

    @Test
    void test() {
        assertEquals(BATCH_SIZE, params.getBatchSize());
        assertEquals(MAX_QUEUE_LENGTH, params.getMaxQueueLength());
        assertEquals(MY_PASS, params.getPassword());
        assertEquals(OperationHistoryDataManagerParams.DEFAULT_PU, params.getPersistenceUnit());
        assertEquals(OperationHistoryDataManagerParams.DEFAULT_DRIVER, params.getDriver());
        assertEquals(MY_URL, params.getUrl());
        assertEquals(MY_USER, params.getUserName());

        // use specified PU
        assertEquals(MY_PU, makeBuilder().persistenceUnit(MY_PU).build().getPersistenceUnit());

        // use specified driver
        assertEquals(MY_DRIVER, makeBuilder().driver(MY_DRIVER).build().getDriver());
    }

    @Test
    void testValidate() {
        assertTrue(params.validate(CONTAINER).isValid());

        testValidateField("url", "null", params2 -> params2.setUrl(null));
        testValidateField("userName", "null", params2 -> params2.setUserName(null));
        testValidateField("password", "null", params2 -> params2.setPassword(null));
        testValidateField("persistenceUnit", "null", params2 -> params2.setPersistenceUnit(null));
        testValidateField("driver", "null", params2 -> params2.setDriver(null));

        // check edge cases
        params.setBatchSize(0);
        assertFalse(params.validate(CONTAINER).isValid());

        params.setBatchSize(1);
        assertTrue(params.validate(CONTAINER).isValid());

        params.setMaxQueueLength(0);
        assertFalse(params.validate(CONTAINER).isValid());

        params.setMaxQueueLength(1);
        assertTrue(params.validate(CONTAINER).isValid());

        // blank password is ok
        params.setPassword("");
        assertTrue(params.validate(CONTAINER).isValid());
    }

    private void testValidateField(String fieldName, String expected,
                    Consumer<OperationHistoryDataManagerParams> makeInvalid) {

        // original params should be valid
        var result = params.validate(CONTAINER);
        assertTrue(result.isValid());

        // make invalid params
        var params2 = makeBuilder().build();
        makeInvalid.accept(params2);
        result = params2.validate(CONTAINER);
        assertFalse(result.isValid());
        assertThat(result.getResult()).contains(CONTAINER).contains(fieldName).contains(expected);
    }

    private OperationHistoryDataManagerParamsBuilder makeBuilder() {
        // @formatter:off
        return OperationHistoryDataManagerParams.builder()
                        .batchSize(BATCH_SIZE)
                        .maxQueueLength(MAX_QUEUE_LENGTH)
                        .password(MY_PASS)
                        .url(MY_URL)
                        .userName(MY_USER);
        // @formatter:on
    }
}
