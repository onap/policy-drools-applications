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

import java.util.Properties;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.controlloop.common.rules.test.BaseTest;
import org.onap.policy.controlloop.common.rules.test.Listener;
import org.onap.policy.controlloop.common.rules.test.NamedRunner;
import org.onap.policy.controlloop.common.rules.test.Rules;
import org.onap.policy.controlloop.common.rules.test.TestNames;
import org.onap.policy.controlloop.tdjam.TdjamController;
import org.onap.policy.drools.persistence.SystemPersistence;
import org.onap.policy.drools.persistence.SystemPersistenceConstants;
import org.onap.policy.drools.system.PolicyControllerConstants;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.drools.utils.logging.LoggerUtil;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.simulators.Util;

/**
 * Tests use cases using BaseTest Set.
 *
 * <p/>
 * Note: this runs ALL tests (i.e., any whose names start with "test").
 */
@RunWith(NamedRunner.class)
@TestNames(prefixes = {"test"})

public class TdjamTest extends BaseTest {
    protected static final String CONTROLLER_NAME = "tdjam";

    private static final SystemPersistence pdpdRepo = SystemPersistenceConstants.getManager();
    private static final PolicyEngine pdpd = PolicyEngineConstants.getManager();

    private static TdjamController controller;

    /**
     * Sets up statics.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        initStatics();

        pdpdRepo.setConfigurationDir("src/test/resources/config");
        pdpd.configure(new Properties());

        LoggerUtil.setLevel(LoggerUtil.ROOT_LOGGER, "WARN");
        LoggerUtil.setLevel("org.eclipse.jetty", "WARN");
        LoggerUtil.setLevel("org.onap.policy.controlloop", "INFO");
        LoggerUtil.setLevel("network", "INFO");

        controller = (TdjamController) pdpd.createPolicyController(CONTROLLER_NAME,
                        pdpdRepo.getControllerProperties(CONTROLLER_NAME));
        pdpd.start();

        httpClients.addClients("tdjam");
        simulators.start(Util::buildAaiSim, Util::buildSoSim, Util::buildVfcSim, Util::buildGuardSim,
                        Util::buildSdncSim);
    }

    /**
     * Cleans up statics.
     */
    @AfterClass
    public static void tearDownAfterClass() {
        finishStatics();
        PolicyControllerConstants.getFactory().shutdown(controller);
        pdpd.stop();
    }

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        topics = topicMaker.get();
    }

    /**
     * Tears down.
     */
    @After
    public void tearDown() {
        controller.reset();
        topics.destroy();
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

    /**
     * Returns ToscaPolicy from File.
     *
     * @param fileName a path name
     * @return ToscaPolicy
     */
    @Override
    protected ToscaPolicy checkPolicy(String fileName) {
        try {
            ToscaPolicy newPolicy = Rules.getPolicyFromFile(fileName);
            controller.getDrools().offer(newPolicy);
            return newPolicy;
        } catch (CoderException e) {
            throw new IllegalArgumentException("cannot decode " + fileName, e);
        }
    }

    /**
     * Returns Listener from createListener based on Coder.
     *
     * @return the Listener
     */
    @Override
    protected Listener<VirtualControlLoopNotification> createNoficationTopicListener() {
        return topics.createListener(POLICY_CL_MGT_TOPIC, VirtualControlLoopNotification.class, controller);
    }
}
