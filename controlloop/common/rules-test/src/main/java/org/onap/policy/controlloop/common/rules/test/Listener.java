/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Predicate;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.TopicEndpoint;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.event.comm.TopicListener;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for messages published on a topic SINK.
 *
 * @param <T> message type
 */
public class Listener<T> implements TopicListener {
    private static final Logger logger = LoggerFactory.getLogger(Listener.class);
    private static final long DEFAULT_WAIT_SEC = 5L;

    private final TopicSink sink;
    private final Function<String, T> decoder;
    private final BlockingQueue<T> messages = new LinkedBlockingQueue<>();

    /**
     * Constructs the object.
     *
     * @param topicName name of the NOOP topic SINK on which to listen
     * @param decoder function that takes a topic name and a message and decodes it into
     *        the desired type
     */
    public Listener(String topicName, Function<String, T> decoder) {
        this.sink = getTopicManager().getNoopTopicSink(topicName);
        this.decoder = decoder;
        this.sink.register(this);
    }

    /**
     * Determines if there are any messages waiting.
     *
     * @return {@code true} if there are no messages waiting, {@code false} otherwise
     */
    public boolean isEmpty() {
        return messages.isEmpty();
    }

    /**
     * Waits, for the default amount of time, for a message to be published to the topic.
     *
     * @return the message that was published
     * @throws TopicException if interrupted or no message is received within the
     *         specified time
     */
    public T await() {
        return await(DEFAULT_WAIT_SEC, TimeUnit.SECONDS);
    }

    /**
     * Waits, for the specified period of time, for a message to be published to the
     * topic.
     *
     * @param twait maximum time to wait
     * @param unit time unit
     * @return the message that was published
     * @throws TopicException if interrupted or no message is received within the
     *         specified time
     */
    public T await(long twait, TimeUnit unit) {
        return await(twait, unit, msg -> true);
    }

    /**
     * Waits, for the default amount of time, for a message to be published to the topic.
     *
     * @param filter filter used to select the message of interest; preceding messages
     *        that do not pass the filter are discarded
     * @return the message that was published
     * @throws TopicException if interrupted or no message is received within the
     *         specified time
     */
    public T await(Predicate<T> filter) {
        return await(DEFAULT_WAIT_SEC, TimeUnit.SECONDS, filter);
    }

    /**
     * Waits, for the specified period of time, for a message to be published to the
     * topic.
     *
     * @param twait maximum time to wait
     * @param unit time unit
     * @param filter filter used to select the message of interest; preceding messages
     *        that do not pass the filter are discarded
     * @return the message that was published
     * @throws TopicException if interrupted or no message is received within the
     *         specified time
     */
    public T await(long twait, TimeUnit unit, Predicate<T> filter) {
        long endMs = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(twait, unit);

        for (;;) {
            try {
                long remainingMs = endMs - System.currentTimeMillis();
                if (remainingMs < 0) {
                    throw new TimeoutException();
                }

                T msg = pollMessage(remainingMs);
                if (msg == null) {
                    throw new TimeoutException();
                }

                if (filter.test(msg)) {
                    return msg;
                }

                logger.info("message discarded by the filter on topic {}", sink.getTopic());

            } catch (InterruptedException e) {
                logger.warn("'await' interrupted on topic {}", sink.getTopic());
                Thread.currentThread().interrupt();
                throw new TopicException(e);

            } catch (TimeoutException e) {
                logger.warn("'await' timed out on topic {}", sink.getTopic());
                throw new TopicException(e);
            }
        }
    }

    /**
     * Unregisters the listener.
     */
    public void unregister() {
        sink.unregister(this);
    }

    @Override
    public void onTopicEvent(CommInfrastructure commType, String topic, String event) {
        try {
            messages.add(decoder.apply(event));
        } catch (RuntimeException e) {
            logger.warn("cannot decode message on topic {} for event {}", topic, event, e);
        }
    }

    // these methods may be overridden by junit tests

    protected TopicEndpoint getTopicManager() {
        return TopicEndpointManager.getManager();
    }

    protected T pollMessage(long remainingMs) throws InterruptedException {
        return messages.poll(remainingMs, TimeUnit.MILLISECONDS);
    }
}
