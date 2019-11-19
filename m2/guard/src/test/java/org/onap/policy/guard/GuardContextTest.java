/*-
 * ============LICENSE_START=======================================================
 * guard
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.guard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.drools.core.WorkingMemory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.onap.policy.drools.system.PolicyEngineConstants;

public class GuardContextTest {

    private static Properties prop;
    private static GuardContext guardContext;
    private static WorkingMemory workingMemory;
    private static LinkedBlockingQueue<Object> queue = new LinkedBlockingQueue<>();

    /**
     * Class-level initialization.
     */
    @BeforeClass
    public static void setup() throws IOException {
        PolicyEngineConstants.getManager().configure(new Properties());
        PolicyEngineConstants.getManager().start();

        prop = new Properties();
        prop.setProperty("guard.pdp.rest.url", "http://www.google.com/");
        prop.setProperty("guard.pdp.rest.client.user", "testuser");
        prop.setProperty("guard.pdp.rest.client.password", "testpassword");
        prop.setProperty("guard.pdp.rest.timeout", "1000");
        prop.setProperty("guard.pdp.rest.environment", "dev");

        workingMemory = mock(WorkingMemory.class);
        when(workingMemory.insert(isNotNull())).thenAnswer(
            invocation -> {
                queue.add(invocation.getArgument(0));
                return null;
            });
    }

    @AfterClass
    public static void stop() {
        PolicyEngineConstants.getManager().stop();
    }

    @Test
    public void guardDbResponseTest() throws InterruptedException {
        Properties props = new Properties(prop);
        props.setProperty("guard.disabled", "false");
        props.setProperty("guard.javax.persistence.jdbc.user", "user");
        props.setProperty("guard.javax.persistence.jdbc.password", "secret");
        props.setProperty("guard.javax.persistence.jdbc.driver", "org.h2.Driver");
        props.setProperty("guard.javax.persistence.jdbc.url", "jdbc:h2:file:./H2DB");

        guardContext = new GuardContext(props);
        assertNotNull(guardContext);

        guardContext.asyncCreateDbEntry(Instant.now().minusSeconds(1), Instant.now(),
            "testCLName", "testActor", "testRecipe", "testTarget",
            UUID.randomUUID().toString(), "1", "testMessage", "testOutcome");

        queue.clear();
        guardContext.asyncQuery(workingMemory, "testActor", "testRecipe",
            "testTarget", UUID.randomUUID().toString(), "testCLName");
        Object response = queue.poll(10, TimeUnit.SECONDS);
        assertNotNull(response);
    }

    @Test
    public void badValuesTest() throws InterruptedException {
        Properties props = new Properties(prop);
        props.setProperty("guard.disabled", "true");
        props.setProperty("guard.pdp.rest.client.user", "");
        props.setProperty("guard.pdp.rest.client.password", "");
        props.setProperty("guard.pdp.rest.url", "bad,testuser,testpassword");

        guardContext = new GuardContext(props);

        guardContext.asyncCreateDbEntry(Instant.now().minusSeconds(1), Instant.now(),
            "testCLName", "testActor", "testRecipe", "testTarget",
            UUID.randomUUID().toString(), "1", "testMessage", "testOutcome");

        queue.clear();
        guardContext.asyncQuery(workingMemory, "testActor", "testRecipe",
            "testTarget", UUID.randomUUID().toString());
        Object response = queue.poll(10, TimeUnit.SECONDS);
        assertNotNull(response);
    }

    @Test
    public void policyGuardResponseTest() {
        UUID requestId = UUID.randomUUID();
        PolicyGuardResponse emptyResponse1 = new PolicyGuardResponse(null, null, null);

        assertNotNull(emptyResponse1);

        PolicyGuardResponse response = new PolicyGuardResponse("Some Result", requestId, "Some Details");

        response.setRequestId(requestId);
        assertEquals(requestId, response.getRequestId());

        response.setResult("Some Result");
        assertEquals("Some Result", response.getResult());

        assertEquals("PolicyGuardResponse [requestId=", response.toString().substring(0, 31));
    }
}
