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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import org.onap.policy.common.endpoints.event.comm.TopicEndpoint;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.drools.protocol.coders.EventProtocolCoder;
import org.onap.policy.drools.protocol.coders.EventProtocolCoderConstants;
import org.onap.policy.drools.system.PolicyController;

/**
 * Mechanism by which junit tests can manage topic messages.
 */
public class Topics {
    /**
     * Wherever this string appears within an input file, it is replaced by a value passed
     * as a parameter to the {@link #inject(String, String, String)} method.
     */
    private static final String REPLACE_ME = "${replaceMe}";

    /**
     * Listeners that have been created and registered by "this" object.
     */
    private final List<Listener<?>> listeners = new LinkedList<>();


    /**
     * Constructs the object.
     */
    public Topics() {
        super();
    }

    /**
     * Unregisters all of the listeners.
     */
    public void destroy() {
        listeners.forEach(Listener::unregister);
    }

    /**
     * Injects the content of the given file onto a NOOP topic SOURCE.
     *
     * @param topicName topic on which to inject
     * @param file file whose content is to be injected
     */
    public void inject(String topicName, String file) {
        inject(topicName, file, REPLACE_ME);
    }

    /**
     * Injects the content of the given file onto a NOOP topic SOURCE, with the given
     * substitution.
     *
     * @param topicName topic on which to inject
     * @param file file whose content is to be injected
     * @param newText text to be substituted for occurrences of "${replaceMe}" in the
     *        source file
     */
    public void inject(String topicName, String file, String newText) {
        try {
            var text = ResourceUtils.getResourceAsString(file);
            if (text == null) {
                throw new FileNotFoundException(file);
            }
            text = text.replace(REPLACE_ME, newText);
            getTopicManager().getNoopTopicSource(topicName).offer(text);
        } catch (IOException e) {
            throw new TopicException(e);
        }
    }

    /**
     * Creates a listener for messages published on a NOOP topic SINK. Messages are
     * decoded using the coder associated with the controller.
     *
     * @param <T> message type
     * @param topicName name of the topic on which to listen
     * @param expectedClass type of message expected
     * @param controller controller whose decoders are to be used
     * @return a new listener
     */
    public <T> Listener<T> createListener(String topicName, Class<T> expectedClass, PolicyController controller) {
        EventProtocolCoder mgr = getProtocolCoder();
        String groupId = controller.getDrools().getGroupId();
        String artifactId = controller.getDrools().getArtifactId();

        // @formatter:off
        return createListener(topicName,
            event -> expectedClass.cast(mgr.decode(groupId, artifactId, topicName, event)));
        // @formatter:on
    }

    /**
     * Creates a listener for messages published on a NOOP topic SINK. Messages are
     * decoded using the specified coder.
     *
     * @param <T> message type
     * @param topicName name of the topic on which to listen
     * @param expectedClass type of message expected
     * @param coder coder to decode the messages
     * @return a new listener
     */
    public <T> Listener<T> createListener(String topicName, Class<T> expectedClass, Coder coder) {
        Function<String, T> decoder = event -> {
            try {
                return coder.decode(event, expectedClass);
            } catch (CoderException e) {
                throw new IllegalArgumentException("cannot decode message", e);
            }
        };

        return createListener(topicName, decoder);
    }

    /**
     * Creates a listener for messages published on a NOOP topic SINK. Messages are
     * decoded using the specified decoder.
     *
     * @param <T> message type
     * @param topicName name of the topic on which to listen
     * @param decoder function that takes a message and decodes it into the desired type
     * @return a new listener
     */
    public <T> Listener<T> createListener(String topicName, Function<String, T> decoder) {
        Listener<T> listener = makeListener(topicName, decoder);
        listeners.add(listener);

        return listener;
    }

    // these methods may be overridden by junit tests

    protected TopicEndpoint getTopicManager() {
        return TopicEndpointManager.getManager();
    }

    protected EventProtocolCoder getProtocolCoder() {
        return EventProtocolCoderConstants.getManager();
    }

    protected <T> Listener<T> makeListener(String topicName, Function<String, T> decoder) {
        return new Listener<>(topicName, decoder) {
            @Override
            protected TopicEndpoint getTopicManager() {
                return Topics.this.getTopicManager();
            }
        };
    }
}
