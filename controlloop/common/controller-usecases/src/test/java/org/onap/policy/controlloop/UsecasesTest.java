/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2022 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.controlloop;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.controlloop.common.rules.test.DroolsRuleTest;
import org.onap.policy.controlloop.common.rules.test.Listener;
import org.onap.policy.controlloop.common.rules.test.NamedRunner;
import org.onap.policy.controlloop.common.rules.test.TestNames;
import org.onap.policy.drools.apps.controller.usecases.UsecasesEventManager;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.simulators.Util;

/**
 * Tests use cases using Usecases rules.
 *
 * <p/>
 * Note: this runs ALL tests (i.e., any whose names start with "test").
 */
@ExtendWith(NamedRunner.class)
@TestNames(prefixes = {"test"})
class UsecasesTest extends DroolsRuleTest {
    protected static final String CONTROLLER_NAME = "usecases";


    /**
     * Sets up statics.
     */
    @BeforeAll
    public static void setUpBeforeClass() {
        initStatics(CONTROLLER_NAME);

        rules.configure("src/main/resources");
        httpClients.addClients("usecases");
        simulators.start(Util::buildAaiSim, Util::buildSoSim, Util::buildVfcSim, Util::buildXacmlSim,
                        Util::buildSdncSim);

        rules.start();
    }

    /**
     * Cleans up statics.
     */
    @AfterAll
    public static void tearDownAfterClass() {
        finishStatics();
    }

    /**
     * Sets up.
     */
    @BeforeEach
    public void setUp() {
        init();
    }

    /**
     * Tears down.
     */
    @AfterEach
    public void tearDown() {
        finish();
    }

    @Override
    protected void waitForLockAndPermit(ToscaPolicy policy, Listener<VirtualControlLoopNotification> policyClMgt) {
        String policyName = policy.getIdentifier().getName();

        policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.ACTIVE
                        && "EVENT.MANAGER.ACCEPT".equals(notif.getPolicyScope())
                        && policyName.equals(notif.getPolicyName()));

        policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.OPERATION
                        && "EVENT.MANAGER.PROCESS.GUARD.OUTCOME".equals(notif.getPolicyScope())
                        && policyName.equals(notif.getPolicyName())
                        && notif.getMessage().startsWith("Sending guard query"));

        policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.OPERATION
                        && "EVENT.MANAGER.PROCESS.GUARD.OUTCOME".equals(notif.getPolicyScope())
                        && policyName.equals(notif.getPolicyName())
                        && notif.getMessage().startsWith("Guard result")
                        && notif.getMessage().endsWith("Permit"));

        policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.OPERATION
                        && "EVENT.MANAGER.PROCESS.POLICY.STARTED".equals(notif.getPolicyScope())
                        && policyName.equals(notif.getPolicyName())
                        && notif.getMessage().startsWith("actor="));
    }

    @Override
    protected VirtualControlLoopNotification waitForFinal(ToscaPolicy policy,
                    Listener<VirtualControlLoopNotification> policyClMgt, ControlLoopNotificationType finalType) {

        return policyClMgt.await(notif -> notif.getNotification() == finalType
                        && "EVENT.MANAGER.FINAL".equals(notif.getPolicyScope())
                        && (policy.getIdentifier().getName()).equals(notif.getPolicyName()));
    }

    @Override
    protected long getCreateCount() {
        return UsecasesEventManager.getCreateCount();
    }
}
