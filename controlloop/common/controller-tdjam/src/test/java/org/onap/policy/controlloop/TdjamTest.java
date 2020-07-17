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

import java.io.File;
import java.util.List;
import java.util.function.Function;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.kie.api.runtime.KieSession;
import org.onap.policy.controlloop.common.rules.test.DroolsRuleTest;
import org.onap.policy.controlloop.common.rules.test.Listener;
import org.onap.policy.controlloop.common.rules.test.NamedRunner;
import org.onap.policy.controlloop.common.rules.test.Rules;
import org.onap.policy.controlloop.common.rules.test.TestNames;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.simulators.Util;
import org.powermock.reflect.Whitebox;

/**
 * Tests use cases using Frankfurt rules.
 *
 * <p/>
 * Note: this runs ALL tests (i.e., any whose names start with "test").
 */
@RunWith(NamedRunner.class)
@TestNames(prefixes = {"test"})

public class TdjamTest extends DroolsRuleTest {
    protected static final String CONTROLLER_NAME = "frankfurt";

    /**
     * We aren't really using Drools, but we do need to simulate it to a
     * degree in order to use 'BaseTest'. This alternative version of
     * 'Rules' has the overrides needed to do this.
     */
    public static class AltRules extends Rules {
        // simulated KieSession
        AltKieSession kieSession = null;

        public AltRules(String controllerName) {
            super(controllerName);
        }

        protected KieSession getKieSession() {
            if (kieSession == null) {
                kieSession = new AltKieSession(getControllerName());
            }
            return kieSession;
        }

        protected void installArtifact(File kmoduleFile, File pomFile, String resourceDir, List<File> ruleFiles) {
            // do nothing -- the artifact isn't needed if you don't have Drools
        }
    }

    /**
     * Sets up statics.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        // use alternative version of 'Rules'
        Function<String, Rules> ruleMaker = AltRules::new;
        Whitebox.setInternalState(DroolsRuleTest.class, "ruleMaker", ruleMaker);

        initStatics(CONTROLLER_NAME);

        rules.configure("src/main/resources");
        rules.start();
        httpClients.addClients("frankfurt");
        simulators.start(Util::buildAaiSim, Util::buildSoSim, Util::buildVfcSim, Util::buildGuardSim,
                        Util::buildSdncSim);
    }

    /**
     * Cleans up statics.
     */
    @AfterClass
    public static void tearDownAfterClass() {
        finishStatics();
    }

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        init();
    }

    /**
     * Tears down.
     */
    @After
    public void tearDown() {
        finish();
    }

    @Override
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

    @Override
    protected VirtualControlLoopNotification waitForFinal(ToscaPolicy policy,
                    Listener<VirtualControlLoopNotification> policyClMgt, ControlLoopNotificationType finalType) {

        return policyClMgt.await(notif -> notif.getNotification() == finalType
                        && (policy.getIdentifier().getName() + ".EVENT.MANAGER.FINAL").equals(notif.getPolicyName()));
    }
}
