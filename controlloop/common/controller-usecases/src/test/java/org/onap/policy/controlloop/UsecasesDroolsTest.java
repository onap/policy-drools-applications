/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019 Bell Canada.
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
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.onap.policy.controlloop.params.ControlLoopParams;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit test class for drools rules.
 */
public class UsecasesDroolsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsecasesDroolsTest.class);

    // Drools engine session
    private KieSession kieSession;

    /**
     * Setup drools engine session.
     */
    @Before
    public void setup() throws IOException {

        // Get a builder
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);

        // Add the rule file
        try (InputStream rulesInputStream = UsecasesDroolsTest.class.getResourceAsStream("/usecases.drl")) {
            assertNotNull("Can't open stream for rule file.", rulesInputStream);
            kieFileSystem.write(
                    kieServices.getResources().newInputStreamResource(rulesInputStream).setSourcePath("usecases.drl"));
        }

        // Build and check for errors
        kieBuilder.buildAll();

        StringBuilder message = new StringBuilder();
        List<Message> messages = kieBuilder.getResults().getMessages();
        for (Message msg : messages) {
            message.append(msg.toString()).append("\n");
        }
        assertEquals(
                "Errors shouldn't have occurred but were found: " + message.toString(),
                0, messages.size());

        // Get container which finally produces the session we want
        KieContainer kieContainer = kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());

        this.kieSession = kieContainer.getKieBase().newKieSession();

    }

    /**
     * Test rules processing.
     */
    @Test
    public void testRules() {

        // Prepare

        final String closedLoopControlName = "my-closed-loop-control-name";
        final String policyName = "my-policy-name";
        final String policyVersion = "my-policy-version";

        final ToscaPolicy toscaPolicy = new ToscaPolicy();
        toscaPolicy.setName(policyName);
        toscaPolicy.setVersion(policyVersion);
        this.kieSession.insert(toscaPolicy);

        ControlLoopParams params = new ControlLoopParams();
        params.setClosedLoopControlName(closedLoopControlName);
        params.setPolicyName(policyName);
        params.setPolicyVersion(policyVersion);
        this.kieSession.insert(params);

        final VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(closedLoopControlName);
        event.setRequestId(UUID.randomUUID());
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        // TODO bsa - Insert event in session to test
        //this.kieSession.insert(event);

        // Run
        LOGGER.debug("Fire rules ...");
        int result = kieSession.fireAllRules();
        LOGGER.debug("{} rules has been fired.", result);

        // Verify
        assertTrue(result > 0);

    }

    /**
     * Dispose drools engine session.
     */
    @After
    public void tearDown() {
        if (this.kieSession != null) {
            this.kieSession.dispose();
        }
        LOGGER.debug("Drools engine session disposed.");
    }

}
