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

package org.onap.policy.controlloop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.BeforeMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.event.rule.DefaultRuleRuntimeEventListener;
import org.kie.api.event.rule.MatchCancelledEvent;
import org.kie.api.event.rule.MatchCreatedEvent;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;
import org.kie.api.runtime.KieSession;
import org.onap.policy.common.endpoints.event.comm.Topic;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.event.comm.TopicListener;
import org.onap.policy.common.endpoints.http.client.HttpClientConfigException;
import org.onap.policy.common.endpoints.http.client.HttpClientFactoryInstance;
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.drools.persistence.SystemPersistence;
import org.onap.policy.drools.persistence.SystemPersistenceConstants;
import org.onap.policy.drools.protocol.coders.EventProtocolCoderConstants;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.drools.system.PolicyControllerConstants;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.drools.util.KieUtils;
import org.onap.policy.drools.utils.PropertyUtil;
import org.onap.policy.drools.utils.logging.LoggerUtil;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.simulators.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use Cases Tests Framework.
 */
public abstract class FrankfurtBase {

    private static final Logger logger = LoggerFactory.getLogger(FrankfurtBase.class);
    private static final StandardCoder coder = new StandardCoder();

    /**
     * PDP-D Engine.
     */
    protected static final PolicyEngine pdpD = PolicyEngineConstants.getManager();

    /**
     * PDP-D Configuration Repository.
     */
    protected static final SystemPersistence repo = SystemPersistenceConstants.getManager();

    /**
     * Frankfurt controller and session name.
     */
    protected static final String CONTROLLER_NAME = "frankfurt";

    /**
     * Frankfurt controller.
     */
    protected static PolicyController controller;

    /*
     * Canonical Topic Names.
     */
    protected static final String DCAE_TOPIC = "DCAE_TOPIC";
    protected static final String APPC_LCM_WRITE_TOPIC = "APPC-LCM-WRITE";
    protected static final String POLICY_CL_MGT_TOPIC = "POLICY-CL-MGT";
    protected static final String APPC_LCM_READ_TOPIC = "APPC-LCM-READ";
    protected static final String APPC_CL_TOPIC = "APPC-CL";

    /**
     * Sets up overall logging.
     */
    protected static void setupLogging() {
        LoggerUtil.setLevel(LoggerUtil.ROOT_LOGGER, "WARN");
        LoggerUtil.setLevel("org.eclipse.jetty", "WARN");
        LoggerUtil.setLevel("org.onap.policy.controlloop", "INFO");
        LoggerUtil.setLevel("network", "INFO");
    }

    /**
     * Sets up Drools Logging for events of interest.
     */
    protected static void setupDroolsLogging() {
        KieSession session = PolicyControllerConstants.getFactory().get(CONTROLLER_NAME).getDrools().getContainer()
                        .getPolicySession(CONTROLLER_NAME).getKieSession();

        session.addEventListener(new RuleListenerLogger());
        session.addEventListener(new AgendaListenerLogger());
    }

    /**
     * Sets up Http Clients specified in the property file.
     */
    protected static void setUpHttpClients() {
        Properties props = new Properties();
        try (InputStream inpstr = openConfigFile("config/frankfurt-controller.properties")) {
            props.load(inpstr);
            props = PropertyUtil.getInterpolatedProperties(props);
            HttpClientFactoryInstance.getClientFactory().build(props);

        } catch (IOException e) {
            throw new IllegalArgumentException("cannot read frankfurt-controller property file", e);

        } catch (HttpClientConfigException e) {
            throw new IllegalArgumentException("cannot configure HTTP clients", e);
        }
    }

    /**
     * Opens the config file.
     *
     * @param configFileName configuration file name
     * @return the file's input stream
     * @throws FileNotFoundException if the file cannot be found
     */
    private static InputStream openConfigFile(String configFileName) throws FileNotFoundException {
        InputStream inpstr = ResourceUtils.getResourceAsStream(configFileName);
        if (inpstr == null) {
            throw new FileNotFoundException(configFileName);
        }

        return inpstr;
    }

    /**
     * Sets up Simulators for use case testing.
     */
    protected static void setupSimulators() throws InterruptedException {
        Util.buildAaiSim();
        Util.buildSoSim();
        Util.buildVfcSim();
        Util.buildGuardSim();
        Util.buildSdncSim();
    }

    /**
     * Returns the runtime Control Loop Parameters associated with a Tosca Policy.
     */
    protected ControlLoopParams clParameters(ToscaPolicy policy) {
        return controller.getDrools().facts(CONTROLLER_NAME, ControlLoopParams.class).stream()
                        .filter((params) -> params.getToscaPolicy() == policy).findFirst().get();
    }

    protected ToscaPolicy getPolicyFromResource(String resourcePath, String policyName) throws CoderException {
        String policyJson = ResourceUtils.getResourceAsString(resourcePath);
        ToscaServiceTemplate serviceTemplate = coder.decode(policyJson, ToscaServiceTemplate.class);
        ToscaPolicy policy = serviceTemplate.getToscaTopologyTemplate().getPolicies().get(0).get(policyName);
        assertNotNull(policy);

        /*
         * name and version are used within a drl. api component and drools core will
         * ensure that these are populated.
         */
        if (StringUtils.isBlank(policy.getName())) {
            policy.setName(policyName);
        }

        if (StringUtils.isBlank(policy.getVersion())) {
            policy.setVersion(policy.getTypeVersion());
        }

        return serviceTemplate.getToscaTopologyTemplate().getPolicies().get(0).get(policyName);
    }

    protected ToscaPolicy getPolicyFromFile(String policyPath) throws IOException, CoderException {
        String rawPolicy = new String(Files.readAllBytes(Paths.get(policyPath)));
        return coder.decode(rawPolicy, ToscaPolicy.class);
    }

    private ToscaPolicy setupPolicy(ToscaPolicy policy) throws InterruptedException {
        final KieObjectExpectedCallback<?> policyTracker = new KieObjectInsertedExpectedCallback<>(policy);
        final KieObjectExpectedCallback<?> paramsTracker =
                        new KieClassInsertedExpectedCallback<>(ControlLoopParams.class);

        controller.getDrools().offer(policy);

        assertTrue(policyTracker.isNotified());
        assertTrue(paramsTracker.isNotified());

        assertEquals(1, controller.getDrools().facts(CONTROLLER_NAME, ToscaPolicy.class).stream()
                        .filter((anotherPolicy) -> anotherPolicy == policy).count());

        assertEquals(1, controller.getDrools().facts(CONTROLLER_NAME, ControlLoopParams.class).stream()
                        .filter((params) -> params.getToscaPolicy() == policy).count());
        return policy;
    }

    /**
     * Installs a policy from policy/models (examples) repo.
     */
    protected ToscaPolicy setupPolicyFromResource(String resourcePath, String policyName)
                    throws CoderException, InterruptedException {
        return setupPolicy(getPolicyFromResource(resourcePath, policyName));
    }


    /**
     * Installs a given policy.
     */
    protected ToscaPolicy setupPolicyFromFile(String policyPath)
                    throws IOException, CoderException, InterruptedException {
        return setupPolicy(getPolicyFromFile(policyPath));
    }

    /**
     * Deletes a policy.
     */
    protected void deletePolicy(ToscaPolicy policy) throws InterruptedException {
        ControlLoopParams clParams = clParameters(policy);
        assertNotNull(clParams);

        final KieObjectExpectedCallback<?> policyTracker = new KieObjectDeletedExpectedCallback<>(policy);
        final KieObjectExpectedCallback<?> clParamsTracker = new KieObjectDeletedExpectedCallback<>(clParams);

        controller.getDrools().delete(CONTROLLER_NAME, policy);
        assertTrue(policyTracker.isNotified());
        assertTrue(clParamsTracker.isNotified());

        assertEquals(0, controller.getDrools().facts(CONTROLLER_NAME, ToscaPolicy.class).stream()
                        .filter((anotherPolicy) -> anotherPolicy == policy).count());

        assertEquals(0, controller.getDrools().facts(CONTROLLER_NAME, ControlLoopParams.class).stream()
                        .filter((params) -> params.getPolicyName() == policy.getName()).count());
    }

    /**
     * Prepare a PDP-D to test the Use Cases.
     */
    protected static void preparePdpD() throws IOException {
        KieUtils.installArtifact(Paths.get("src/main/resources/META-INF/kmodule.xml").toFile(),
                        Paths.get("src/test/resources/frankfurt.pom").toFile(),
                        "src/main/resources/org/onap/policy/controlloop/",
                        Collections.singletonList(Paths.get("src/main/resources/frankfurt.drl").toFile()));

        repo.setConfigurationDir("src/test/resources/config");
        pdpD.configure(new Properties());

        controller = pdpD.createPolicyController(CONTROLLER_NAME, repo.getControllerProperties(CONTROLLER_NAME));
        pdpD.start();

        setupDroolsLogging();
    }

    /**
     * Stop PDP-D.
     */
    protected static void stopPdpD() {
        PolicyControllerConstants.getFactory().shutdown(CONTROLLER_NAME);
        pdpD.stop();
    }

    /**
     * Stops the http clients.
     */
    protected static void stopHttpClients() {
        HttpClientFactoryInstance.getClientFactory().destroy();
    }

    /**
     * Stop Simulators.
     */
    protected static void stopSimulators() {
        HttpServletServerFactoryInstance.getServerFactory().destroy();
    }

    /**
     * Creates a Topic Sink Callback tracker.
     */
    protected <T> TopicCallback<T> createTopicSinkCallback(String topicName, Class<T> clazz) {
        return new TopicCallback<>(TopicEndpointManager.getManager().getNoopTopicSink(topicName), clazz);
    }

    /**
     * Creates a Topic Sink Callback tracker.
     */
    protected <T> TopicCallback<T> createTopicSinkCallbackPlain(String topicName, Class<T> clazz, Coder coder) {
        return new TopicCallbackCoder<>(TopicEndpointManager.getManager().getNoopTopicSink(topicName), clazz, coder);
    }

    /**
     * Creates a Topic Source Callback tracker.
     */
    protected <T> TopicCallback<T> createTopicSourceCallback(String topicName, Class<T> clazz) {
        return new TopicCallback<>(TopicEndpointManager.getManager().getNoopTopicSource(topicName), clazz);
    }

    /**
     * Injects a message on a Topic Source.
     */
    protected void injectOnTopic(String topicName, Path onsetPath) throws IOException {
        TopicEndpointManager.getManager().getNoopTopicSource(topicName)
                        .offer(new String(Files.readAllBytes(onsetPath)));
    }

    /**
     * Injects a message on a Topic Source, with the given substitution..
     */
    protected void injectOnTopic(String topicName, Path path, String newText) throws IOException {
        String text = IOUtils.toString(path.toUri(), StandardCharsets.UTF_8);
        text = text.replace("${replaceMe}", newText);
        TopicEndpointManager.getManager().getNoopTopicSource(topicName).offer(text);
    }

    /**
     * Waits for LOCK acquisition and getting a Permit from PDP-X to proceed.
     */
    protected void waitForLockAndPermit(ToscaPolicy policy, TopicCallback<VirtualControlLoopNotification> policyClMgt) {
        String policyName = policy.getIdentifier().getName();

        // TODO register a topic listener instead of using await() ?

        await().until(() -> !policyClMgt.getMessages().isEmpty());
        VirtualControlLoopNotification notif = policyClMgt.getMessages().remove();
        assertEquals(ControlLoopNotificationType.ACTIVE, notif.getNotification());
        assertEquals(policyName + ".EVENT", notif.getPolicyName());

        await().until(() -> !policyClMgt.getMessages().isEmpty());
        notif = policyClMgt.getMessages().remove();
        assertEquals(ControlLoopNotificationType.OPERATION, notif.getNotification());
        assertEquals(policyName + ".EVENT.MANAGER.PROCESSING", notif.getPolicyName());
        assertThat(notif.getMessage()).startsWith("Sending guard query");

        await().until(() -> !policyClMgt.getMessages().isEmpty());
        notif = policyClMgt.getMessages().remove();
        assertEquals(ControlLoopNotificationType.OPERATION, notif.getNotification());
        assertEquals(policyName + ".EVENT.MANAGER.PROCESSING", notif.getPolicyName());
        assertThat(notif.getMessage()).startsWith("Guard result").endsWith("Permit");
    }

    /**
     * Waits for a FINAL SUCCESS transaction notification.
     */
    protected void waitForFinalSuccess(ToscaPolicy policy, TopicCallback<VirtualControlLoopNotification> policyClMgt) {
        await().until(() -> !policyClMgt.getMessages().isEmpty());
        assertEquals(ControlLoopNotificationType.FINAL_SUCCESS, policyClMgt.getMessages().peek().getNotification());
        assertEquals(policy.getIdentifier().getName() + ".EVENT.MANAGER.FINAL",
                        policyClMgt.getMessages().remove().getPolicyName());
    }

    /**
     * Logs Modifications to Working Memory.
     */
    static class RuleListenerLogger implements RuleRuntimeEventListener {
        @Override
        public void objectInserted(ObjectInsertedEvent event) {
            String ruleName = (event.getRule() != null) ? event.getRule().getName() : "null";
            logger.info("RULE {}: inserted {}", ruleName, event.getObject());
        }

        @Override
        public void objectUpdated(ObjectUpdatedEvent event) {
            String ruleName = (event.getRule() != null) ? event.getRule().getName() : "null";
            logger.info("RULE {}: updated {}", ruleName, event.getObject());

        }

        @Override
        public void objectDeleted(ObjectDeletedEvent event) {
            String ruleName = (event.getRule() != null) ? event.getRule().getName() : "null";
            logger.info("RULE {}: deleted {}", ruleName, event.getOldObject());
        }
    }

    /**
     * Logs Rule Matches.
     */
    static class AgendaListenerLogger extends DefaultAgendaEventListener {
        @Override
        public void matchCreated(MatchCreatedEvent event) {
            logger.info("RULE {}: match created", event.getMatch().getRule().getName());
        }

        @Override
        public void matchCancelled(MatchCancelledEvent event) {
            logger.info("RULE {}: match cancelled", event.getMatch().getRule().getName());
        }

        @Override
        public void beforeMatchFired(BeforeMatchFiredEvent event) {
            logger.info("RULE {}: before match fired", event.getMatch().getRule().getName());
        }

        @Override
        public void afterMatchFired(AfterMatchFiredEvent event) {
            logger.info("RULE {}: after match fired", event.getMatch().getRule().getName());
        }
    }

    /**
     * Base Class to track Working Memory updates for objects of type T.
     */
    abstract class KieObjectExpectedCallback<T> extends DefaultRuleRuntimeEventListener {
        protected T subject;

        protected CountDownLatch countDownLatch = new CountDownLatch(1);

        public KieObjectExpectedCallback(T affected) {
            subject = affected;
            register();
        }

        public boolean isNotified() throws InterruptedException {
            return countDownLatch.await(9L, TimeUnit.SECONDS);
        }

        protected void callbacked() {
            unregister();
            countDownLatch.countDown();
        }

        public KieObjectExpectedCallback<T> register() {
            controller.getDrools().getContainer().getPolicySession(CONTROLLER_NAME).getKieSession()
                            .addEventListener(this);
            return this;
        }

        public KieObjectExpectedCallback<T> unregister() {
            controller.getDrools().getContainer().getPolicySession(CONTROLLER_NAME).getKieSession()
                            .removeEventListener(this);
            return this;
        }
    }

    /**
     * Tracks inserts in Working Memory for an object of type T.
     */
    class KieObjectInsertedExpectedCallback<T> extends KieObjectExpectedCallback<T> {
        public KieObjectInsertedExpectedCallback(T affected) {
            super(affected);
        }

        @Override
        public void objectInserted(ObjectInsertedEvent event) {
            if (subject == event.getObject()) {
                callbacked();
            }
        }
    }

    /**
     * Tracks deletes in Working Memory of an object of type T.
     */
    class KieObjectDeletedExpectedCallback<T> extends KieObjectExpectedCallback<T> {
        public KieObjectDeletedExpectedCallback(T affected) {
            super(affected);
        }

        @Override
        public void objectDeleted(ObjectDeletedEvent event) {
            if (subject == event.getOldObject()) {
                callbacked();
            }
        }
    }

    /**
     * Tracks inserts in Working Memory for any object of class T.
     */
    class KieClassInsertedExpectedCallback<T> extends KieObjectInsertedExpectedCallback<T> {

        public KieClassInsertedExpectedCallback(T affected) {
            super(affected);
        }

        public void objectInserted(ObjectInsertedEvent event) {
            if (subject == event.getObject().getClass()) {
                callbacked();
            }
        }
    }

    /**
     * Tracks callbacks from topics.
     */
    class TopicCallback<T> implements TopicListener {
        protected final Topic topic;
        protected final Class<T> expectedClass;

        @Getter
        protected Queue<T> messages = new LinkedList<>();

        public TopicCallback(Topic topic, Class<T> expectedClass) {
            this.topic = topic;
            this.expectedClass = expectedClass;
            this.topic.register(this);
        }

        public TopicCallback<T> register() {
            this.topic.register(this);
            return this;
        }

        public TopicCallback<T> unregister() {
            this.topic.unregister(this);
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onTopicEvent(CommInfrastructure comm, String topic, String event) {
            try {
                messages.add((T) EventProtocolCoderConstants.getManager().decode(controller.getDrools().getGroupId(),
                                controller.getDrools().getArtifactId(), topic, event));
            } catch (Exception e) {
                logger.warn("invalid mapping in topic {} for event {}", topic, event, e);
            }
        }
    }

    class TopicCallbackCoder<T> extends TopicCallback<T> {
        private final Coder coder;

        public TopicCallbackCoder(Topic topic, Class<T> expectedClass, Coder coder) {
            super(topic, expectedClass);
            this.coder = coder;
        }

        @Override
        public void onTopicEvent(CommInfrastructure comm, String topic, String event) {
            try {
                messages.add((T) coder.decode(event, expectedClass));
            } catch (Exception e) {
                logger.warn("invalid mapping in topic {} for event {}", topic, event, e);
            }
        }

    }
}
