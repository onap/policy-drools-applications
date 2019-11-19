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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

public class GuardContextTest {

    private static Properties prop;
    private static GuardContext guardContext;

    /**
     * Class-level initialization.
     */
    @BeforeClass
    public static void setup() throws IOException {

        prop = new Properties();
        prop.setProperty("guard.pdp.rest.url", "http://www.google.com/");
        prop.setProperty("guard.pdp.rest.client.user", "testuser");
        prop.setProperty("guard.pdp.rest.client.password", "testpassword");
        prop.setProperty("guard.pdp.rest.timeout", "1000");
        prop.setProperty("guard.pdp.rest.environment", "dev");

    }

    @Test
    public void guardDbResponseTest() {
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

        PolicyGuardResponse response = guardContext.query("testActor", "testRecipe",
            "testTarget", UUID.randomUUID().toString(), "testCLName");
        assertNotNull(response);
    }

    @Test
    public void badValuesTest() {
        Properties props = new Properties(prop);
        props.setProperty("guard.disabled", "true");
        props.setProperty("guard.pdp.rest.client.user", "");
        props.setProperty("guard.pdp.rest.client.password", "");
        props.setProperty("guard.pdp.rest.url", "bad,testuser,testpassword");

        guardContext = new GuardContext(props);

        guardContext.asyncCreateDbEntry(Instant.now().minusSeconds(1), Instant.now(),
            "testCLName", "testActor", "testRecipe", "testTarget",
            UUID.randomUUID().toString(), "1", "testMessage", "testOutcome");

        PolicyGuardResponse response = guardContext.query("testActor", "testRecipe",
            "testTarget", UUID.randomUUID().toString());
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
