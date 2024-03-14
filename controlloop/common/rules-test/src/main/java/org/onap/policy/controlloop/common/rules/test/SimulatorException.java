/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation.
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

package org.onap.policy.controlloop.common.rules.test;

/**
 * Exception thrown by <i>Simulators</i>.
 */
public class SimulatorException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SimulatorException() {
        super();
    }

    public SimulatorException(String message) {
        super(message);
    }

    public SimulatorException(Throwable cause) {
        super(cause);
    }

    public SimulatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
