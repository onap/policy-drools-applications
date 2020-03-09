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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
import org.onap.policy.common.endpoints.http.client.HttpClientFactoryInstance;
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.controlloop.common.endpoints.test.Listener;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.controlloop.eventmanager.ControlLoopEventManager2;
import org.onap.policy.drools.controller.DroolsController;
import org.onap.policy.drools.persistence.SystemPersistence;
import org.onap.policy.drools.persistence.SystemPersistenceConstants;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.drools.system.PolicyControllerConstants;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.drools.util.KieUtils;
import org.onap.policy.drools.utils.logging.LoggerUtil;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
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
     * Prepare PDP-D Framework for testing.
     */
    protected static void prepareResouces() throws InterruptedException, IOException {
        initConfigDir();
        setupLogging();
        preparePdpD();
    }

    /**
     * Take down the resources used by the test framework.
     */
    protected static void takeDownResources() {
        stopPdpD();
        stopSimulators();
    }

    protected static void initConfigDir() {
        SystemPersistenceConstants.getManager().setConfigurationDir("src/test/resources/config");
    }

    protected void resetFacts() {
        DroolsController drools = controller.getDrools();
        drools.delete(ToscaPolicy.class);
        drools.delete(ControlLoopParams.class);
        drools.delete(ControlLoopEventManager2.class);
        drools.delete(VirtualControlLoopEvent.class);
    }

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
    protected ToscaPolicy setupPolicyFromFile(String policyPath) {
        try {
            return setupPolicy(getPolicyFromFile(policyPath));

        } catch (InterruptedException | IOException | CoderException e) {
            throw new IllegalArgumentException("policy " + policyPath, e);
        }
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
     * Waits for LOCK acquisition and getting a Permit from PDP-X to proceed.
     */
    protected void waitForLockAndPermit(ToscaPolicy policy, Listener<VirtualControlLoopNotification> policyClMgt) {
        String policyName = policy.getIdentifier().getName();

        policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.ACTIVE
                        && (policyName + ".EVENT").equals(notif.getPolicyName()));

        policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.OPERATION
                        && (policyName + ".EVENT.MANAGER.PROCESSING").equals(notif.getPolicyName())
                        && notif.getMessage().startsWith("Sending guard query"));

        policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.OPERATION
                        && (policyName + ".EVENT.MANAGER.PROCESSING").equals(notif.getPolicyName())
                        && notif.getMessage().startsWith("Guard result") && notif.getMessage().endsWith("Permit"));

        policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.OPERATION
                        && (policyName + ".EVENT.MANAGER.PROCESSING").equals(notif.getPolicyName())
                        && notif.getMessage().startsWith("actor="));
    }

    /**
     * Waits for a FINAL SUCCESS transaction notification.
     */
    protected void waitForFinalSuccess(ToscaPolicy policy, Listener<VirtualControlLoopNotification> policyClMgt) {
        policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.FINAL_SUCCESS
                        && (policy.getIdentifier().getName() + ".EVENT.MANAGER.FINAL").equals(notif.getPolicyName()));
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
}
