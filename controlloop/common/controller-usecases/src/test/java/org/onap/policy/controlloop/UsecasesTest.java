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

import lombok.Getter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.onap.policy.controlloop.common.rules.test.BaseRuleTest;
import org.onap.policy.controlloop.common.rules.test.Listener;
import org.onap.policy.controlloop.common.rules.test.NamedRunner;
import org.onap.policy.controlloop.common.rules.test.TestNames;
import org.onap.policy.drools.utils.PropertyUtil;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.simulators.Util;

/**
 * Tests use case rules. Only a subset of available tests will work with these rules, thus
 * a "FilterRunner" is used to filter out the other test cases.
 *
 * <p/>
 * Note: this only runs tests whose names start with "testV" (e.g., testVcpe(),
 * testVdns()).
 */
@RunWith(NamedRunner.class)
@TestNames(prefixes = {"testV"})

public class UsecasesTest extends BaseRuleTest {

    @Getter()
    protected static final String CONTROLLER_NAME = "usecases";

    /**
     * Sets up statics.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        initStatics(CONTROLLER_NAME);

        rules.configure("src/main/resources");
        PropertyUtil.setSystemProperties(rules.getPdpdRepo().getSystemProperties("controlloop"));
        rules.getPdpd().setEnvironment(rules.getPdpdRepo().getEnvironmentProperties("controlloop.properties"));

        rules.start();
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
                        && (policyName + ".EVENT.MANAGER.OPERATION.LOCKED.GUARD_NOT_YET_QUERIED")
                                        .equals(notif.getPolicyName()));

        policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.OPERATION
                        && (policyName + ".GUARD.RESPONSE").equals(notif.getPolicyName())
                        && notif.getMessage().startsWith("Guard result") && notif.getMessage().endsWith("Permit"));

        policyClMgt.await(notif -> notif.getNotification() == ControlLoopNotificationType.OPERATION
                        && (policyName + ".EVENT.MANAGER.OPERATION.LOCKED.GUARD_PERMITTED")
                                        .equals(notif.getPolicyName()));
    }

    @Override
    protected VirtualControlLoopNotification waitForFinal(ToscaPolicy policy,
                    Listener<VirtualControlLoopNotification> policyClMgt, ControlLoopNotificationType finalType) {

        return policyClMgt.await(notif -> notif.getNotification() == finalType
                        && (policy.getIdentifier().getName() + ".EVENT.MANAGER").equals(notif.getPolicyName()));
    }

    /**
     * Only runs tests starting with "testV" (e.g., testVcpeSunnyDay, testVdns).
     */
    public static class FilterRunner extends BlockJUnit4ClassRunner {

        public FilterRunner(Class<?> klass) throws InitializationError {
            super(klass);
        }

        @Override
        protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
            Description description = describeChild(method);
            if (method.getAnnotation(Ignore.class) != null || !method.getName().startsWith("testV")) {
                notifier.fireTestIgnored(description);
            } else {
                runLeaf(methodBlock(method), description, notifier);
            }
        }
    }
}
