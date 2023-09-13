/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023 Nordix Foundation.
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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.TopicEndpoint;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.event.comm.bus.NoopTopicSink;
import org.onap.policy.common.endpoints.parameters.TopicParameters;

class ListenerTest {
    private static final String EXPECTED_EXCEPTION = "expected exception";
    private static final String MY_TOPIC = "my-topic";
    private static final String MESSAGE = "the-message";
    private static final String MESSAGE2 = "other-message";
    private static final String MSG_SUFFIX = "s";
    private static final String DECODED_MESSAGE = MESSAGE + MSG_SUFFIX;

    private final NoopTopicSink sink = mock(NoopTopicSink.class);

    private final TopicEndpoint mgr = mock(TopicEndpoint.class);

    private Listener<String> listener;

    /**
     * Creates topics.
     */
    @BeforeAll
    public static void setUpBeforeClass() {
        TopicEndpointManager.getManager().shutdown();

        var params = new TopicParameters();
        params.setTopic(MY_TOPIC);
        params.setManaged(true);
        params.setTopicCommInfrastructure("NOOP");

        TopicEndpointManager.getManager().addTopicSinks(List.of(params));
    }

    @AfterAll
    public static void tearDownAfterClass() {
        TopicEndpointManager.getManager().shutdown();
    }

    /**
     * Sets up.
     */
    @BeforeEach
    public void setUp() {
        when(mgr.getNoopTopicSink(MY_TOPIC)).thenReturn(sink);

        listener = new Listener<>(MY_TOPIC, msg -> msg + MSG_SUFFIX) {
            @Override
            protected TopicEndpoint getTopicManager() {
                return mgr;
            }
        };
    }

    @Test
    void testListener() {
        verify(sink).register(listener);
    }

    @Test
    void testAwait_testAwaitLongTimeUnit_testIsEmpty() {
        assertTrue(listener.isEmpty());

        listener.onTopicEvent(CommInfrastructure.NOOP, MY_TOPIC, MESSAGE);
        assertFalse(listener.isEmpty());

        assertEquals(DECODED_MESSAGE, listener.await());

        assertTrue(listener.isEmpty());
    }

    @Test
    void testAwaitPredicateOfT() {
        listener.onTopicEvent(CommInfrastructure.NOOP, MY_TOPIC, MESSAGE);
        listener.onTopicEvent(CommInfrastructure.NOOP, MY_TOPIC, MESSAGE2);
        assertEquals(MESSAGE2 + MSG_SUFFIX, listener.await(msg -> msg.startsWith("other-")));
    }

    /**
     * Tests await() when the remaining time is negative.
     */
    @Test
    void testAwaitLongTimeUnitPredicateNoTime() {
        assertThatThrownBy(() -> listener.await(-1, TimeUnit.SECONDS)).isInstanceOf(TopicException.class);
    }

    /**
     * Tests await() when the poll() returns {@code null}.
     */
    @Test
    void testAwaitLongTimeUnitPredicateNoMessage() {
        assertThatThrownBy(() -> listener.await(1, TimeUnit.MILLISECONDS)).isInstanceOf(TopicException.class);
    }

    /**
     * Tests await() when the poll() is interrupted.
     */
    @Test
    void testAwaitLongTimeUnitPredicateInterrupted() throws InterruptedException {
        listener = new Listener<String>(MY_TOPIC, msg -> msg) {
            @Override
            protected String pollMessage(long remainingMs) throws InterruptedException {
                throw new InterruptedException(EXPECTED_EXCEPTION);
            }
        };

        AtomicReference<TopicException> exref = new AtomicReference<>();
        var interrupted = new CountDownLatch(1);

        var thread = new Thread() {
            @Override
            public void run() {
                try {
                    listener.await();
                } catch (TopicException e) {
                    exref.set(e);
                }

                if (Thread.currentThread().isInterrupted()) {
                    interrupted.countDown();
                }
            }
        };

        thread.start();
        assertTrue(interrupted.await(5, TimeUnit.SECONDS));
        assertNotNull(exref.get());
    }

    @Test
    void testUnregister() {
        listener.unregister();
        verify(sink).unregister(listener);
    }

    @Test
    void testOnTopicEvent() {
        listener = new Listener<>(MY_TOPIC, msg -> {
            throw new IllegalArgumentException(EXPECTED_EXCEPTION);
        });

        // onTopicEvent() should not throw an exception
        assertThatCode(() -> listener.onTopicEvent(CommInfrastructure.NOOP, MY_TOPIC, MESSAGE))
                        .doesNotThrowAnyException();

        // should not have queued a message
        assertTrue(listener.isEmpty());
    }

    @Test
    void testGetTopicManager() {
        // use a listener with a real manager
        assertNotNull(new Listener<>(MY_TOPIC, msg -> msg).getTopicManager());
    }
}
