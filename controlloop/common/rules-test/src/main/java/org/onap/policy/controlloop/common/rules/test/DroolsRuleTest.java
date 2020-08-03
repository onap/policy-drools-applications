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
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.Getter;
import org.onap.policy.drools.system.PolicyController;

public abstract class DroolsRuleTest extends BaseRuleTest {

    private static Function<String, Rules> ruleMaker = Rules::new;
    private static Supplier<HttpClients> httpClientMaker = HttpClients::new;
    private static Supplier<Simulators> simMaker = Simulators::new;
    private static Supplier<Topics> topicMaker = Topics::new;

    protected static Rules rules;
    protected static HttpClients httpClients;
    protected static Simulators simulators;
    protected PolicyController controller;
    
    @Getter(AccessLevel.PROTECTED)
    private Topics topics;
    
    /**
     * Initializes {@link #rules}, {@link #httpClients}, and {@link #simulators}.
     *
     * @param controllerName the rule controller name
     */
    public static void initStatics(String controllerName) {
        rules = ruleMaker.apply(controllerName);
        httpClients = httpClientMaker.get();
        simulators = simMaker.get();
    }

    /**
     * Destroys {@link #httpClients}, {@link #simulators}, and {@link #rules}.
     */
    public static void finishStatics() {
        httpClients.destroy();
        simulators.destroy();
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


}