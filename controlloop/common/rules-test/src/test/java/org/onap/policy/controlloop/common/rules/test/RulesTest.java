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

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.definition.rule.Rule;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.BeforeMatchFiredEvent;
import org.kie.api.event.rule.MatchCancelledEvent;
import org.kie.api.event.rule.MatchCreatedEvent;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.Match;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.controlloop.ControlLoopEvent;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.controlloop.eventmanager.ControlLoopEventManager;
import org.onap.policy.controlloop.eventmanager.ControlLoopEventManager2;
import org.onap.policy.drools.controller.DroolsController;
import org.onap.policy.drools.persistence.SystemPersistence;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.drools.system.PolicyControllerFactory;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;

public class RulesTest {
    private static final String CONTROLLER_NAME = "rulesTest";
    private static final String POLICY_FILE = "src/test/resources/tosca-policy.json";
    private static final String MY_POLICY = "operational.restart";
    private static final String RESOURCE_DIR = "src/test/resources";
    private static final String MY_RULE_NAME = "my-rule-name";
    private static final String MY_TEXT = "my text";

    @Mock
    private PolicyEngine engine;
    @Mock
    private SystemPersistence repo;
    @Mock
    private PolicyController controller;
    @Mock
    private KieSession kieSession;
    @Mock
    private PolicyControllerFactory controllerFactory;
    @Mock
    private DroolsController drools;

    private List<RuleRuntimeEventListener> ruleListeners = new LinkedList<>();
    private List<AgendaEventListener> agendaListeners = new LinkedList<>();
    private Properties properties;
    private boolean installed;

    private Rules rules;

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        installed = false;
        properties = new Properties();

        when(engine.createPolicyController(any(), any())).thenReturn(controller);
        when(repo.getControllerProperties(CONTROLLER_NAME)).thenReturn(properties);
        when(controller.getDrools()).thenReturn(drools);

        // notify listeners when objects are added to drools
        when(drools.offer(any())).thenAnswer(args -> {
            Object object = args.getArgument(0);
            notifyInserted(object);

            if (!(object instanceof ToscaPolicy)) {
                return true;
            }

            // "insert" Params objects associated with the policy (mimic rules)
            ToscaPolicy policy = (ToscaPolicy) object;
            ControlLoopParams params = new ControlLoopParams();
            params.setToscaPolicy(policy);
            notifyInserted(params);

            return true;
        });

        // handle rule listener registration and deregistration with the kieSession
        doAnswer(args -> {
            ruleListeners.add(args.getArgument(0));
            return null;
        }).when(kieSession).addEventListener(any(RuleRuntimeEventListener.class));

        doAnswer(args -> {
            ruleListeners.remove(args.getArgument(0));
            return null;
        }).when(kieSession).removeEventListener(any(RuleRuntimeEventListener.class));

        // handle agenda listener registration and deregistration with the kieSession
        doAnswer(args -> {
            agendaListeners.add(args.getArgument(0));
            return null;
        }).when(kieSession).addEventListener(any(AgendaEventListener.class));

        doAnswer(args -> {
            agendaListeners.remove(args.getArgument(0));
            return null;
        }).when(kieSession).removeEventListener(any(AgendaEventListener.class));

        rules = new MyRules();

        rules.start(RESOURCE_DIR);
    }

    @Test
    public void testRules() {
        assertEquals(CONTROLLER_NAME, rules.getControllerName());

        assertSame(engine, rules.getPdpd());
        assertSame(repo, rules.getPdpdRepo());
        assertSame(controller, rules.getController());
    }

    @Test
    public void testStart() throws Exception {
        verify(repo).setConfigurationDir("src/test/resources/config");
        assertTrue(installed);
        verify(engine).configure(any(Properties.class));
        verify(engine).createPolicyController(CONTROLLER_NAME, properties);
        verify(engine).start();

        verify(kieSession).addEventListener(any(RuleRuntimeEventListener.class));
        verify(kieSession).addEventListener(any(AgendaEventListener.class));
    }

    @Test
    public void testDestroy() {
        rules.destroy();

        verify(controllerFactory).shutdown(CONTROLLER_NAME);
        verify(engine).stop();
    }

    @Test
    public void testResetFacts() {
        rules.resetFacts();

        verify(drools).delete(ToscaPolicy.class);
        verify(drools).delete(ControlLoopParams.class);
        verify(drools).delete(ControlLoopEventManager.class);
        verify(drools).delete(ControlLoopEventManager2.class);
        verify(drools).delete(ControlLoopEvent.class);
    }

    @Test
    public void testSetupPolicyFromResource_testGetPolicyFromResource() {
        rules.setupPolicyFromResource("tosca-template.json", MY_POLICY);

        assertThatIllegalArgumentException()
                        .isThrownBy(() -> rules.setupPolicyFromResource("missing-file.json", "a-policy"));
    }

    @Test
    public void testSetupPolicyFromFile_testGetPolicyFromFile_testSetupPolicy() {
        assertNotNull(rules.setupPolicyFromFile(POLICY_FILE));

        assertThatIllegalArgumentException().isThrownBy(() -> rules.setupPolicyFromFile("missing-file.json"));
    }

    @Test
    public void testRuleListenerLogger() {
        Rule rule = mock(Rule.class);
        when(rule.getName()).thenReturn(MY_RULE_NAME);

        // insertions - with and without rule name
        ObjectInsertedEvent insert = mock(ObjectInsertedEvent.class);
        when(insert.getObject()).thenReturn(MY_TEXT);
        ruleListeners.forEach(listener -> listener.objectInserted(insert));
        when(insert.getRule()).thenReturn(rule);
        ruleListeners.forEach(listener -> listener.objectInserted(insert));

        // updates - with and without rule name
        ObjectUpdatedEvent update = mock(ObjectUpdatedEvent.class);
        when(update.getObject()).thenReturn(MY_TEXT);
        ruleListeners.forEach(listener -> listener.objectUpdated(update));
        when(update.getRule()).thenReturn(rule);
        ruleListeners.forEach(listener -> listener.objectUpdated(update));

        // deletions - with and without rule name
        ObjectDeletedEvent delete = mock(ObjectDeletedEvent.class);
        when(delete.getOldObject()).thenReturn(MY_TEXT);
        ruleListeners.forEach(listener -> listener.objectDeleted(delete));
        when(delete.getRule()).thenReturn(rule);
        ruleListeners.forEach(listener -> listener.objectDeleted(delete));
    }

    @Test
    public void testAgendaListenerLogger() {
        Rule rule = mock(Rule.class);
        when(rule.getName()).thenReturn(MY_RULE_NAME);

        Match match = mock(Match.class);
        when(match.getRule()).thenReturn(rule);

        // create
        MatchCreatedEvent create = mock(MatchCreatedEvent.class);
        when(create.getMatch()).thenReturn(match);
        agendaListeners.forEach(listener -> listener.matchCreated(create));

        // cancel
        MatchCancelledEvent cancel = mock(MatchCancelledEvent.class);
        when(cancel.getMatch()).thenReturn(match);
        agendaListeners.forEach(listener -> listener.matchCancelled(cancel));

        // before-fire
        BeforeMatchFiredEvent before = mock(BeforeMatchFiredEvent.class);
        when(before.getMatch()).thenReturn(match);
        agendaListeners.forEach(listener -> listener.beforeMatchFired(before));

        // after-fire
        AfterMatchFiredEvent after = mock(AfterMatchFiredEvent.class);
        when(after.getMatch()).thenReturn(match);
        agendaListeners.forEach(listener -> listener.afterMatchFired(after));
    }

    @Test
    public void testMakePdpd_testMakePdpdRepo() {
        // need rules that makes real objects
        rules = new Rules(CONTROLLER_NAME);

        assertNotNull(rules.getPdpd());
        assertNotNull(rules.getPdpdRepo());
    }

    protected void notifyInserted(Object object) {
        // act like this object is now in working memory
        when(drools.facts(CONTROLLER_NAME, object.getClass())).thenAnswer(args -> {
            return List.of(object);
        });

        // increase code coverage by adding random objects
        ObjectInsertedEvent event0 = mock(ObjectInsertedEvent.class);
        when(event0.getObject()).thenReturn(new Object());
        ruleListeners.forEach(listener -> listener.objectInserted(event0));

        // increase code coverage by adding a rule name for some objects
        ObjectInsertedEvent event = mock(ObjectInsertedEvent.class);
        when(event.getObject()).thenReturn(object);

        if (object instanceof ToscaPolicy) {
            Rule rule = mock(Rule.class);
            when(rule.getName()).thenReturn(MY_RULE_NAME);
            when(event.getRule()).thenReturn(rule);
        }

        ruleListeners.forEach(listener -> listener.objectInserted(event));
    }

    private class MyRules extends Rules {
        public MyRules() {
            super(CONTROLLER_NAME);
        }

        @Override
        protected PolicyEngine makeEngine() {
            return engine;
        }

        @Override
        protected SystemPersistence makePdpdRepo() {
            return repo;
        }

        @Override
        protected KieSession getKieSession() {
            return kieSession;
        }

        @Override
        protected PolicyControllerFactory getControllerFactory() {
            return controllerFactory;
        }

        @Override
        protected void installArtifact(File kmoduleFile, File pomFile, String resourceDir, List<File> ruleFiles) {
            installed = true;
        }
    }
}
