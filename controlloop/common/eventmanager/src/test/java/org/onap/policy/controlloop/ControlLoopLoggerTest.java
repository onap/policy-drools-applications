/*-
 * ============LICENSE_START=======================================================
 * eventmanager
 * ================================================================================
 * Copyright (C) 2018 Ericsson. All rights reserved.
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

package org.onap.policy.controlloop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.onap.policy.controlloop.impl.ControlLoopLoggerStdOutImpl;

public class ControlLoopLoggerTest {
    @Test
    public void testControlLoopLogger() throws ControlLoopException {
        ControlLoopLogger logger =
                new ControlLoopLogger.Factory().buildLogger(ControlLoopLoggerStdOutImpl.class.getCanonicalName());
        assertNotNull(logger);
        logger.info("a log message", "and another", " and another");
        logger.metrics("a metric", "and another", " and another");
        logger.metrics(Double.valueOf(3));

        try {
            new ControlLoopLogger.Factory().buildLogger("java.lang.String");
            fail("test should throw an exception here");
        } catch (Exception e) {
            assertEquals("Cannot load class java.lang.String as a control loop logger", e.getMessage());
        }
    }
}
