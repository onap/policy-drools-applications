/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
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
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
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
import org.onap.policy.simulators.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use Cases Tests Framework.
 */
public abstract class UsecasesBase {

    private static final Logger logger = LoggerFactory.getLogger(UsecasesBase.class);

    /**
     * PDP-D Engine.
     */
    protected static final PolicyEngine pdpD = PolicyEngineConstants.getManager();

    /**
     * PDP-D Configuration Repository.
     */
    protected static final SystemPersistence repo = SystemPersistenceConstants.getManager();

    /**
     * Usecases controller and session name.
     */
    protected static final String USECASES = "usecases";

    /**
     * Usecases controller.
     */
    protected static PolicyController usecases;

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
        KieSession session =
            PolicyControllerConstants.getFactory().get(USECASES)
                .getDrools().getContainer() .getPolicySession(USECASES).getKieSession();

        session.addEventListener(new RuleListenerLogger());
        session.addEventListener(new AgendaListenerLogger());
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
        return usecases
            .getDrools()
            .facts(USECASES, ControlLoopParams.class).stream()
            .filter((params) -> params.getToscaPolicy() == policy)
            .findFirst()
            .get();
    }


    /**
     * Installs a given policy.
     */
    protected ToscaPolicy setupPolicy(String policyPath) throws IOException, CoderException, InterruptedException {
        String rawPolicy = new String(Files.readAllBytes(Paths.get(policyPath)));
        ToscaPolicy policy = new StandardCoder().decode(rawPolicy, ToscaPolicy.class);

        final KieObjectExpectedCallback policyTracker = new KieObjectInsertedExpectedCallback<>(policy);
        final KieObjectExpectedCallback paramsTracker = new KieClassInsertedExpectedCallback<>(ControlLoopParams.class);

        usecases.getDrools().offer(policy);

        assertTrue(policyTracker.isNotified());
        assertTrue(paramsTracker.isNotified());

        assertEquals(1,
            usecases
                .getDrools()
                .facts(USECASES, ToscaPolicy.class).stream()
                .filter((anotherPolicy) -> anotherPolicy == policy)
                .count());

        assertEquals(1,
            usecases
                .getDrools()
                .facts(USECASES, ControlLoopParams.class).stream()
                .filter((params) -> params.getToscaPolicy() == policy)
                .count());

        return policy;
    }

    /**
     * Deletes a policy.
     */
    protected void deletePolicy(ToscaPolicy policy) throws InterruptedException {
        ControlLoopParams clParams = clParameters(policy);
        assertNotNull(clParams);

        final KieObjectExpectedCallback policyTracker = new KieObjectDeletedExpectedCallback<>(policy);
        final KieObjectExpectedCallback clParamsTracker = new KieObjectDeletedExpectedCallback<>(clParams);

        usecases.getDrools().delete(USECASES, policy);
        assertTrue(policyTracker.isNotified());
        assertTrue(clParamsTracker.isNotified());

        assertEquals(0,
            usecases
                .getDrools()
                .facts(USECASES, ToscaPolicy.class).stream()
                .filter((anotherPolicy) -> anotherPolicy == policy)
                .count());

        assertEquals(0,
            usecases
                .getDrools()
                .facts(USECASES, ControlLoopParams.class).stream()
                .filter((params) -> params.getPolicyName() == policy.getName())
                .count());
    }

    /**
     * Prepare a PDP-D to test the Use Cases.
     */
    protected static void preparePdpD() throws IOException {
        KieUtils.installArtifact(
            Paths.get("src/main/resources/META-INF/kmodule.xml").toFile(),
            Paths.get("src/test/resources/usecases.pom").toFile(),
            "src/main/resources/org/onap/policy/controlloop/",
            Collections.singletonList(Paths.get("src/main/resources/usecases.drl").toFile()));

        repo.setConfigurationDir("src/test/resources/config");
        PropertyUtil.setSystemProperties(repo.getSystemProperties("controlloop"));
        pdpD.setEnvironment(repo.getEnvironmentProperties("controlloop.properties"));
        pdpD.configure(new Properties());

        usecases = pdpD.createPolicyController(USECASES, repo.getControllerProperties(USECASES));
        pdpD.start();

        setupDroolsLogging();
    }

    /**
     * Stop PDP-D.
     */
    protected static void stopPdpD() {
        PolicyControllerConstants.getFactory().shutdown(USECASES);
        pdpD.stop();
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
        return new TopicCallback(TopicEndpointManager.getManager().getNoopTopicSink(topicName), clazz);
    }

    /**
     * Creates a Topic Source Callback tracker.
     */
    protected <T> TopicCallback<T> createTopicSourceCallback(String topicName, Class<T> clazz) {
        return new TopicCallback(TopicEndpointManager.getManager().getNoopTopicSource(topicName), clazz);
    }

    /**
     * Injects a message on a Topic Source.
     */
    protected void injectOnTopic(String topicName, Path onsetPath) throws IOException {
        TopicEndpointManager
            .getManager()
            .getNoopTopicSource(topicName)
            .offer(new String(Files.readAllBytes(onsetPath)));
    }

    /**
     * Waits for LOCK acquisition and getting a Permit from PDP-X to proceed.
     */
    protected void waitForLockAndPermit(ToscaPolicy policy, TopicCallback<VirtualControlLoopNotification> policyClMgt) {
        String policyName = policy.getIdentifier().getName();

        await().until(() -> !policyClMgt.getMessages().isEmpty());
        assertEquals(ControlLoopNotificationType.ACTIVE, policyClMgt.getMessages().peek().getNotification());
        assertEquals(policyName + ".EVENT", policyClMgt.getMessages().remove().getPolicyName());

        await().until(() -> !policyClMgt.getMessages().isEmpty());
        assertEquals(ControlLoopNotificationType.OPERATION,
            policyClMgt.getMessages().peek().getNotification());
        assertEquals(policyName + ".EVENT.MANAGER.OPERATION.LOCKED.GUARD_NOT_YET_QUERIED",
            policyClMgt.getMessages().remove().getPolicyName());

        await().until(() -> !policyClMgt.getMessages().isEmpty());
        assertEquals(ControlLoopNotificationType.OPERATION,
            policyClMgt.getMessages().peek().getNotification());
        assertEquals(policyName + ".GUARD.RESPONSE",
            policyClMgt.getMessages().remove().getPolicyName());

        await().until(() -> !policyClMgt.getMessages().isEmpty());
        assertEquals(ControlLoopNotificationType.OPERATION,
            policyClMgt.getMessages().peek().getNotification());
        assertEquals(policyName + ".EVENT.MANAGER.OPERATION.LOCKED.GUARD_PERMITTED",
            policyClMgt.getMessages().remove().getPolicyName());
    }

    /**
     * Waits for a FINAL SUCCESS transaction notification.
     */
    protected void waitForFinalSuccess(ToscaPolicy policy, TopicCallback<VirtualControlLoopNotification> policyClMgt) {
        await().until(() -> !policyClMgt.getMessages().isEmpty());
        assertEquals(ControlLoopNotificationType.FINAL_SUCCESS, policyClMgt.getMessages().peek().getNotification());
        assertEquals(policy.getIdentifier().getName() + ".EVENT.MANAGER",
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
            usecases.getDrools()
                .getContainer().getPolicySession(USECASES).getKieSession()
                .addEventListener(this);
            return this;
        }

        public KieObjectExpectedCallback<T> unregister() {
            usecases.getDrools()
                .getContainer().getPolicySession(USECASES).getKieSession()
                .removeEventListener(this);
            return this;
        }
    }

    /**
     * Tracks inserts in Working Memory for an object of type T.
     */
    class KieObjectInsertedExpectedCallback<T> extends KieObjectExpectedCallback {
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
    class KieObjectDeletedExpectedCallback<T> extends KieObjectExpectedCallback {
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
    class KieClassInsertedExpectedCallback<T> extends KieObjectInsertedExpectedCallback {
        public KieClassInsertedExpectedCallback(Class<T> affected) {
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

        public TopicCallback register() {
            this.topic.register(this);
            return this;
        }

        public TopicCallback unregister() {
            this.topic.unregister(this);
            return this;
        }

        @Override
        public void onTopicEvent(CommInfrastructure comm, String topic, String event) {
            try {
                messages.add((T) EventProtocolCoderConstants.getManager().decode(
                    usecases.getDrools().getGroupId(), usecases.getDrools().getArtifactId(), topic, event));
            } catch (Exception e) {
                logger.warn("invalid mapping in topic {} for event {}", topic, event, e);
            }
        }
    }
}
