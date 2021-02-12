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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.policy.common.endpoints.http.server.HttpServletServer;
import org.onap.policy.controlloop.common.rules.test.Simulators.SimulatorBuilder;

@RunWith(MockitoJUnitRunner.class)
public class SimulatorsTest {
    private static final String EXPECTED_EXCEPTION = "expected exception";

    @Mock
    private HttpServletServer server1;
    @Mock
    private HttpServletServer server2;
    @Mock
    private HttpServletServer server3;

    private Simulators simulators;

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        simulators = new Simulators();
    }

    @Test
    public void testStart() {
        simulators.start(() -> server1, () -> server2);
        assertEquals(List.of(server1, server2), simulators.getServers());

        verify(server1, never()).shutdown();
        verify(server2, never()).shutdown();
    }

    /**
     * Tests start() when one of the builders throws an exception.
     */
    @Test
    public void testStartException() {
        SimulatorBuilder exbuilder = () -> {
            throw new InterruptedException(EXPECTED_EXCEPTION);
        };

        assertThatThrownBy(() -> simulators.start(() -> server1, () -> server2, exbuilder, () -> server3))
                        .isInstanceOf(SimulatorException.class);

        assertTrue(simulators.getServers().isEmpty());

        verify(server1).shutdown();
        verify(server2).shutdown();

        // shouldn't have reached this builder, so nothing to shut down
        verify(server3, never()).shutdown();
    }

    @Test
    public void testDestroy() {
        simulators.start(() -> server1, () -> server2, () -> server3);

        doThrow(new IllegalStateException(EXPECTED_EXCEPTION)).when(server2).shutdown();

        simulators.destroy();

        verify(server1).shutdown();
        verify(server3).shutdown();

        assertTrue(simulators.getServers().isEmpty());
    }
}
