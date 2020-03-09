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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.Getter;
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
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.controlloop.ControlLoopEvent;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.controlloop.eventmanager.ControlLoopEventManager;
import org.onap.policy.controlloop.eventmanager.ControlLoopEventManager2;
import org.onap.policy.drools.controller.DroolsController;
import org.onap.policy.drools.persistence.SystemPersistence;
import org.onap.policy.drools.persistence.SystemPersistenceConstants;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.drools.system.PolicyControllerConstants;
import org.onap.policy.drools.system.PolicyControllerFactory;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.drools.util.KieUtils;
import org.onap.policy.drools.utils.logging.LoggerUtil;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Rules {
    private static final Logger logger = LoggerFactory.getLogger(Rules.class);
    private static final StandardCoder coder = new StandardCoder();

    /**
     * PDP-D Engine.
     */
    @Getter(AccessLevel.PROTECTED)
    private final PolicyEngine pdpd = makeEngine();

    /**
     * PDP-D Configuration Repository.
     */
    @Getter(AccessLevel.PROTECTED)
    private final SystemPersistence pdpdRepo = makePdpdRepo();


    @Getter
    private final String controllerName;

    @Getter
    private PolicyController controller;


    /**
     * Constructs the object.
     *
     * @param controllerName name of the controller
     */
    public Rules(String controllerName) {
        this.controllerName = controllerName;
    }

    /**
     * Configures and starts various items, including the PDP-D Engine.
     *
     * @param resourceDir path to resource directory
     */
    public void start(String resourceDir) {
        pdpdRepo.setConfigurationDir("src/test/resources/config");

        try {
            File kmoduleFile = new File(resourceDir + "/META-INF/kmodule.xml");
            File pomFile = new File("src/test/resources/" + controllerName + ".pom");
            String resourceDir2 = resourceDir + "/org/onap/policy/controlloop/";
            File ruleFile = new File(resourceDir + "/" + controllerName + ".drl");
            List<File> ruleFiles = Collections.singletonList(ruleFile);

            installArtifact(kmoduleFile, pomFile, resourceDir2, ruleFiles);
        } catch (IOException e) {
            throw new IllegalArgumentException("cannot configure KIE session for " + controllerName, e);
        }

        setupLogging();

        pdpd.configure(new Properties());

        controller = pdpd.createPolicyController(controllerName, pdpdRepo.getControllerProperties(controllerName));
        pdpd.start();

        setupDroolsLogging();
    }

    /**
     * Stop PDP-D.
     */
    public void destroy() {
        getControllerFactory().shutdown(controllerName);
        pdpd.stop();
    }

    /**
     * Removes various facts from working memory, including the Policy and Params, as well
     * as any event managers and events.
     */
    public void resetFacts() {
        DroolsController drools = controller.getDrools();
        drools.delete(ToscaPolicy.class);
        drools.delete(ControlLoopParams.class);
        drools.delete(ControlLoopEventManager.class);
        drools.delete(ControlLoopEventManager2.class);
        drools.delete(ControlLoopEvent.class);
    }

    /**
     * Installs a policy from policy/models (examples) repo.
     */
    public ToscaPolicy setupPolicyFromResource(String resourcePath, String policyName) {
        try {
            return setupPolicy(getPolicyFromResource(resourcePath, policyName));

        } catch (InterruptedException | CoderException e) {
            throw new IllegalArgumentException("policy " + policyName, e);
        }
    }

    private ToscaPolicy getPolicyFromResource(String resourcePath, String policyName) throws CoderException {
        String policyJson = ResourceUtils.getResourceAsString(resourcePath);
        if (policyJson == null) {
            throw new CoderException(new FileNotFoundException(resourcePath));
        }

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

    /**
     * Installs a given policy.
     */
    public ToscaPolicy setupPolicyFromFile(String policyPath) {
        try {
            return setupPolicy(getPolicyFromFile(policyPath));

        } catch (InterruptedException | IOException | CoderException e) {
            throw new IllegalArgumentException("policy " + policyPath, e);
        }
    }

    private ToscaPolicy getPolicyFromFile(String policyPath) throws IOException, CoderException {
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

        assertEquals(1, controller.getDrools().facts(controllerName, ToscaPolicy.class).stream()
                        .filter((anotherPolicy) -> anotherPolicy == policy).count());

        assertEquals(1, controller.getDrools().facts(controllerName, ControlLoopParams.class).stream()
                        .filter((params) -> params.getToscaPolicy() == policy).count());
        return policy;
    }

    /**
     * Sets up overall logging.
     */
    private void setupLogging() {
        LoggerUtil.setLevel(LoggerUtil.ROOT_LOGGER, "WARN");
        LoggerUtil.setLevel("org.eclipse.jetty", "WARN");
        LoggerUtil.setLevel("org.onap.policy.controlloop", "INFO");
        LoggerUtil.setLevel("network", "INFO");
    }

    /**
     * Sets up Drools Logging for events of interest.
     */
    private void setupDroolsLogging() {
        KieSession session = getKieSession();

        session.addEventListener(new RuleListenerLogger());
        session.addEventListener(new AgendaListenerLogger());
    }

    /**
     * Logs Modifications to Working Memory.
     */
    private static class RuleListenerLogger implements RuleRuntimeEventListener {
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
    private static class AgendaListenerLogger extends DefaultAgendaEventListener {
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
    private abstract class KieObjectExpectedCallback<T> extends DefaultRuleRuntimeEventListener {
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
            getKieSession().addEventListener(this);
            return this;
        }

        public KieObjectExpectedCallback<T> unregister() {
            getKieSession().removeEventListener(this);
            return this;
        }
    }

    /**
     * Tracks inserts in Working Memory for an object of type T.
     */
    private class KieObjectInsertedExpectedCallback<T> extends KieObjectExpectedCallback<T> {
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
     * Tracks inserts in Working Memory for any object of class T.
     */
    private class KieClassInsertedExpectedCallback<T> extends KieObjectInsertedExpectedCallback<T> {

        public KieClassInsertedExpectedCallback(T affected) {
            super(affected);
        }

        public void objectInserted(ObjectInsertedEvent event) {
            if (subject == event.getObject().getClass()) {
                callbacked();
            }
        }
    }

    // these may be overridden by junit tests


    protected PolicyEngine makeEngine() {
        return PolicyEngineConstants.getManager();
    }


    protected SystemPersistence makePdpdRepo() {
        return SystemPersistenceConstants.getManager();
    }

    protected KieSession getKieSession() {
        return getControllerFactory().get(controllerName).getDrools().getContainer().getPolicySession(controllerName)
                        .getKieSession();
    }

    protected PolicyControllerFactory getControllerFactory() {
        return PolicyControllerConstants.getFactory();
    }

    protected void installArtifact(File kmoduleFile, File pomFile, String resourceDir, List<File> ruleFiles)
                    throws IOException {

        KieUtils.installArtifact(kmoduleFile, pomFile, resourceDir, ruleFiles);
    }
}
