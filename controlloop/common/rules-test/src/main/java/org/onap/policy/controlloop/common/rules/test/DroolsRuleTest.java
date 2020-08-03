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

import java.util.function.Function;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;

/**
 * Superclass used for rule tests.
 */
public abstract class DroolsRuleTest extends BaseTest {

    // these may be overridden by junit tests
    private static Function<String, Rules> ruleMaker = Rules::new;

    protected static Rules rules;

    protected PolicyController controller;

    /**
     * Initializes {@link #rules}, {@link #httpClients}, and {@link #simulators}.
     *
     * @param controllerName the rule controller name
     */
    public static void initStatics(String controllerName) {
        rules = ruleMaker.apply(controllerName);
        BaseTest.initStatics();
    }

    /**
     * Destroys {@link #httpClients}, {@link #simulators}, and {@link #rules}.
     */
    public static void finishStatics() {
        BaseTest.finishStatics();
        rules.destroy();
    }

    /**
     * Initializes {@link #topics} and {@link #controller}.
     */
    @Override
    public void init() {
        topics = topicMaker.get();
        controller = rules.getController();
    }

    /**
     * Destroys {@link #topics} and resets the rule facts.
     */
    @Override
    public void finish() {
        topics.destroy();
        rules.resetFacts();
    }

    /**
     * Waits for notifications for LOCK acquisition and GUARD Permit so that event
     * processing may proceed.
     */
    protected abstract void waitForLockAndPermit(ToscaPolicy policy,
                    Listener<VirtualControlLoopNotification> policyClMgt);

    /**
     * Waits for a FINAL transaction notification.
     *
     * @param finalType FINAL_xxx type for which to wait
     *
     * @return the FINAL notification
     */
    protected abstract VirtualControlLoopNotification waitForFinal(ToscaPolicy policy,
                    Listener<VirtualControlLoopNotification> policyClMgt, ControlLoopNotificationType finalType);

    /**
     * Returns ToscaPolicy from File.
     *
     * @param fileName a path name
     * @return ToscaPolicy
     */
    @Override
    protected ToscaPolicy checkPolicy(String fileName)  {
        return rules.setupPolicyFromFile(fileName);
    }

    /**
     * Returns Listener from createListner based on Coder.
     * @return the Listener
     */
    @Override
    protected Listener<VirtualControlLoopNotification> createTopicFromListenser() {
        return topics.createListener(POLICY_CL_MGT_TOPIC,
            VirtualControlLoopNotification.class, controller);
    }
}
