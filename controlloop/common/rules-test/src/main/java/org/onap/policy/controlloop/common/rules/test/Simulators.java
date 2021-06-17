/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
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

import java.util.LinkedList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import org.onap.policy.common.endpoints.http.server.HttpServletServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simulator container.
 */
public class Simulators {
    private static final Logger logger = LoggerFactory.getLogger(Simulators.class);

    @Getter(AccessLevel.PROTECTED)
    private final List<HttpServletServer> servers = new LinkedList<>();

    /**
     * Invokes the given functions to start the simulators. Destroys <i>all</i> of the
     * simulators if any fail to start.
     *
     * @param builders functions to invoke to build the simulators
     */
    public void start(SimulatorBuilder... builders) {
        try {
            for (SimulatorBuilder builder : builders) {
                servers.add(builder.build());
            }
        } catch (InterruptedException e) {
            logger.warn("interrupted building the simulators");
            destroy();
            Thread.currentThread().interrupt();
            throw new SimulatorException(e);
        }
    }

    /**
     * Stops all of the simulators.
     */
    public void destroy() {
        for (HttpServletServer server : servers) {
            try {
                server.shutdown();
            } catch (RuntimeException e) {
                logger.warn("error stopping simulator {}", server.getName(), e);
            }
        }

        servers.clear();
    }

    @FunctionalInterface
    public static interface SimulatorBuilder {
        public HttpServletServer build() throws InterruptedException;
    }
}
