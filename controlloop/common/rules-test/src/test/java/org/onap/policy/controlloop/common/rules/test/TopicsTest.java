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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.ToString;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.TopicEndpoint;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.event.comm.bus.NoopTopicSink;
import org.onap.policy.common.endpoints.event.comm.bus.NoopTopicSource;
import org.onap.policy.common.endpoints.parameters.TopicParameters;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.drools.controller.DroolsController;
import org.onap.policy.drools.protocol.coders.EventProtocolCoder;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;

@RunWith(MockitoJUnitRunner.class)
public class TopicsTest {
    private static final String EXPECTED_EXCEPTION = "expected exception";
    private static final String MY_SOURCE_TOPIC = "my-source-topic";
    private static final String MY_SINK_TOPIC = "my-sink-topic";
    private static final String MY_GROUP = "my-group";
    private static final String MY_ARTIFACT = "my-artifact";
    private static final String MESSAGE = "{\"text\": \"hello\"}";
    private static final String TEXT = "hello";
    private static final String INJECT_FILE = "src/test/resources/topics.json";
    private static final String POLICY_NAME = "my-policy";

    @Mock
    private DroolsController drools;
    @Mock
    private PolicyController controller;
    @Mock
    private EventProtocolCoder protocolCoder;
    @Mock
    private NoopTopicSink sink;
    @Mock
    private NoopTopicSource source;
    @Mock
    private TopicEndpoint mgr;

    private ToscaPolicy policy;

    private Topics topics;

    /**
     * Creates topics.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        TopicEndpointManager.getManager().shutdown();

        TopicParameters params = new TopicParameters();
        params.setTopic(MY_SOURCE_TOPIC);
        params.setManaged(true);
        params.setTopicCommInfrastructure("NOOP");
        TopicEndpointManager.getManager().addTopicSources(List.of(params));
    }

    @AfterClass
    public static void tearDownAfterClass() {
        TopicEndpointManager.getManager().shutdown();
    }

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        policy = new ToscaPolicy();
        policy.setName(POLICY_NAME);
        policy.setVersion("1.0.0");

        when(drools.getGroupId()).thenReturn(MY_GROUP);
        when(drools.getArtifactId()).thenReturn(MY_ARTIFACT);

        when(controller.getDrools()).thenReturn(drools);

        when(protocolCoder.decode(MY_GROUP, MY_ARTIFACT, MY_SINK_TOPIC, MESSAGE)).thenReturn(TEXT);

        when(mgr.getNoopTopicSink(MY_SINK_TOPIC)).thenReturn(sink);
        when(mgr.getNoopTopicSource(MY_SOURCE_TOPIC)).thenReturn(source);

        topics = new Topics() {
            @Override
            protected TopicEndpoint getTopicManager() {
                return mgr;
            }

            @Override
            protected EventProtocolCoder getProtocolCoder() {
                return protocolCoder;
            }
        };
    }

    @Test
    public void testDestroy() {
        Listener<String> listener1 = topics.createListener(MY_SINK_TOPIC, msg -> msg);
        Listener<String> listener2 = topics.createListener(MY_SINK_TOPIC, msg -> msg + "a suffix");

        topics.destroy();

        verify(sink).unregister(listener1);
        verify(sink).unregister(listener2);
    }

    @Test
    public void testInjectStringFile() throws IOException {
        topics.inject(MY_SOURCE_TOPIC, INJECT_FILE);

        // nothing should have been replaced
        String expected = new String(Files.readAllBytes(Paths.get(INJECT_FILE)));
        verify(source).offer(expected);
    }

    @Test
    public void testInjectStringFileString() throws IOException {
        topics.inject(MY_SOURCE_TOPIC, INJECT_FILE, "hello");

        // text should have been replaced with "hello"
        String expected = new String(Files.readAllBytes(Paths.get("src", "test", "resources", "topicsReplaced.json")));
        verify(source).offer(expected);

        // exception reading file
        assertThatThrownBy(() -> topics.inject(MY_SOURCE_TOPIC, "missing-file.json", "some text"))
                        .isInstanceOf(TopicException.class);
    }

    @Test
    public void testCreateListenerStringClassOfTPolicyController() {
        Listener<String> listener = topics.createListener(MY_SINK_TOPIC, String.class, controller);
        listener.onTopicEvent(CommInfrastructure.NOOP, MY_SINK_TOPIC, MESSAGE);

        assertEquals(TEXT, listener.await());
    }

    @Test
    public void testCreateListenerStringClassOfTCoder() {
        Listener<Data> listener = topics.createListener(MY_SINK_TOPIC, Data.class, new StandardCoder());
        listener.onTopicEvent(CommInfrastructure.NOOP, MY_SINK_TOPIC, MESSAGE);

        Data expected = new Data();
        expected.text = TEXT;
        assertEquals(expected.toString(), listener.await().toString());
    }

    /**
     * Tests createListener() when the coder throws an exception.
     */
    @Test
    public void testCreateListenerStringClassOfTCoderException() {
        StandardCoder coder = new StandardCoder() {
            @Override
            public <T> T decode(String arg0, Class<T> arg1) throws CoderException {
                throw new CoderException(EXPECTED_EXCEPTION);
            }
        };

        Listener<Data> listener = topics.createListener(MY_SINK_TOPIC, Data.class, coder);

        // onTopicEvent() should not throw an exception
        assertThatCode(() -> listener.onTopicEvent(CommInfrastructure.NOOP, MY_SINK_TOPIC, MESSAGE))
                        .doesNotThrowAnyException();

        // should not have queued a message
        assertThatThrownBy(() -> listener.await(0, TimeUnit.MILLISECONDS)).isInstanceOf(TopicException.class);
    }

    @Test
    public void testCreateListenerStringFunctionOfStringT() {
        Listener<String> listener = topics.createListener(MY_SINK_TOPIC, msg -> msg);
        listener.onTopicEvent(CommInfrastructure.NOOP, MY_SINK_TOPIC, MESSAGE);

        assertEquals(MESSAGE, listener.await());
    }

    @Test
    public void testGetTopicManager_testGetProtocolCoder() {
        // use a topic with a real manager
        topics = new Topics();

        assertNotNull(topics.getTopicManager());
        assertNotNull(topics.getProtocolCoder());
    }

    @ToString
    private static class Data {
        private String text;
    }
}
