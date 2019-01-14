/*-
 * ============LICENSE_START=======================================================
 * guard
 * ================================================================================
 * Copyright (C) 2017-2019 AT&T Intellectual Property. All rights reserved.
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.att.research.xacml.api.Advice;
import com.att.research.xacml.api.Attribute;
import com.att.research.xacml.api.AttributeCategory;
import com.att.research.xacml.api.AttributeValue;
import com.att.research.xacml.api.Decision;
import com.att.research.xacml.api.IdReference;
import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.Obligation;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.api.Result;
import com.att.research.xacml.api.Status;
import com.att.research.xacml.std.IdentifierImpl;
import com.att.research.xacml.std.StdAttribute;
import com.att.research.xacml.std.StdAttributeCategory;
import com.att.research.xacml.std.StdAttributeValue;
import com.att.research.xacml.std.StdResponse;
import com.att.research.xacml.std.StdResult;
import com.att.research.xacml.std.StdStatus;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.common.endpoints.http.server.HttpServletServer;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.drools.utils.logging.LoggerUtil;

public class PolicyGuardXacmlHelperTest {
    
    private static final Integer VF_COUNT = 100;

    /**
     * Set up test class.
     */
    @BeforeClass
    public static void setupSimulator() {
        LoggerUtil.setLevel("ROOT", "INFO");
        LoggerUtil.setLevel("org.eclipse.jetty", "WARN");
        try {
            org.onap.policy.simulators.Util.buildGuardSim();
        } catch (Exception e) {
            fail(e.getMessage());
        }
        //
        // Set guard properties
        //
        org.onap.policy.guard.Util.setGuardEnvProps("http://localhost:6669/pdp/api/getDecision", "python", "test",
                "python", "test", "DEVL");
    }

    /**
     * Shuts down simulator and performs 1 more test for the case where the connection fails.
     */
    @AfterClass
    public static void tearDownSimulator() {
        HttpServletServer.factory.destroy();

        // Null/ Bad Connection Case
        PolicyGuardXacmlRequestAttributes xacmlReq = new PolicyGuardXacmlRequestAttributes(
                        org.onap.policy.simulators.GuardSimulatorJaxRs.DENY_CLNAME, "actor", "recipe", "target",
                        "requestId", VF_COUNT);
        String rawDecision = new PolicyGuardXacmlHelper().callPdp(xacmlReq);
        assertNotNull(rawDecision);
        assertEquals(0, Util.INDETERMINATE.compareToIgnoreCase(rawDecision));
    }

    @Test
    public void testSimulator() {
        PolicyGuardXacmlRequestAttributes request = new PolicyGuardXacmlRequestAttributes("clname_id", "actor_id",
                        "operation_id", "target_id", "request_id", VF_COUNT);
        String xacmlResponse = new PolicyGuardXacmlHelper().callPdp(request);
        assertNotNull(xacmlResponse);
    }

    @Test
    /**
     * Tests PolicyGuardXacmlHelper.callPdp method to determine if it returns DENY, PERMIT, or
     * INDETERMINATE as expected.
     */
    public void testCallPdp() {
        // Deny Case
        PolicyGuardXacmlRequestAttributes xacmlReq = new PolicyGuardXacmlRequestAttributes(
                        org.onap.policy.simulators.GuardSimulatorJaxRs.DENY_CLNAME, "actor", "recipe", "target",
                        "requestId", VF_COUNT);
        String rawDecision = new PolicyGuardXacmlHelper().callPdp(xacmlReq);
        assertNotNull(rawDecision);
        assertTrue(0 == Util.DENY.compareToIgnoreCase(rawDecision));

        // Permit Case
        xacmlReq = new PolicyGuardXacmlRequestAttributes("clname", "actor", "recipe", "target", "requestId", VF_COUNT);
        rawDecision = new PolicyGuardXacmlHelper().callPdp(xacmlReq);
        assertNotNull(rawDecision);
        assertEquals(0, Util.PERMIT.compareToIgnoreCase(rawDecision));

        // Indeterminate case is in tearDown for efficiency
    }

    @Test
    /**
     * Tests PolicyGuardXacmlHelper.callPdp method to exercise all branches
     */
    public void testCallPdpExtra() {
        PolicyGuardXacmlRequestAttributes xacmlReq = new PolicyGuardXacmlRequestAttributes(
                        org.onap.policy.simulators.GuardSimulatorJaxRs.DENY_CLNAME, "actor", "recipe", "target",
                        "requestId", VF_COUNT);

        xacmlReq.setClnameId(null);
        String rawDecision = new PolicyGuardXacmlHelper().callPdp(xacmlReq);
        assertNotNull(rawDecision);
        assertEquals(-5, Util.DENY.compareToIgnoreCase(rawDecision));

        org.onap.policy.guard.Util.setGuardEnvProps("http://localhost:6669/pdp/api/getDecision", "", "", "", "", "");

        rawDecision = new PolicyGuardXacmlHelper().callPdp(xacmlReq);
        assertNotNull(rawDecision);

        org.onap.policy.guard.Util.setGuardEnvProps("http://localhost:6669/pdp/api/getDecision", "python", "test",
                "python", "test", "DEVL");

    }

    @Test
    public void testParseXacmlPdpResponse() throws URISyntaxException {
        PolicyGuardResponse pgResponse = PolicyGuardXacmlHelper.parseXacmlPdpResponse(null);
        assertEquals("Indeterminate", pgResponse.getResult());

        Decision decision = Decision.PERMIT;
        Status status = new StdStatus(StdStatus.STATUS_OK);
        Result result = new StdResult(decision, status);
        Response xacmlResponse = new StdResponse(result);
        pgResponse = PolicyGuardXacmlHelper.parseXacmlPdpResponse(xacmlResponse);
        assertEquals("Permit", pgResponse.getResult());


        final Collection<Obligation> obligationsIn = null;
        final Collection<Advice> adviceIn = null;
        final Collection<IdReference> policyIdentifiersIn = null;
        final Collection<IdReference> policySetIdentifiersIn = null;

        Collection<AttributeCategory> attributesIn = new ArrayList<>();
        Identifier identifierCategory = new IdentifierImpl(new URI("http://somewhere.over.the.rainbow"));
        Collection<Attribute> listAttributes = new ArrayList<>();
        Identifier categoryIdIn = new IdentifierImpl(new URI("http://somewhere.over.the.rainbow/category"));
        Identifier attributeIdIn0 = new IdentifierImpl(new URI("urn:oasis:names:tc:xacml:1.0:request:request-id"));
        Identifier dataTypeIdIn = new IdentifierImpl(new URI("http://somewhere.over.the.rainbow.dataType"));
        AttributeValue<String> valueIn = new StdAttributeValue<String>(dataTypeIdIn, UUID.randomUUID().toString());
        Attribute attribute0 = new StdAttribute(categoryIdIn, attributeIdIn0, valueIn);
        listAttributes.add(attribute0);

        Identifier attributeIdIn1 = new IdentifierImpl(new URI("urn:oasis:names:tc:xacml:1.0:operation:operation-id"));
        Attribute attribute1 = new StdAttribute(categoryIdIn, attributeIdIn1, valueIn);
        listAttributes.add(attribute1);
        attributesIn.add(new StdAttributeCategory(identifierCategory, listAttributes));

        Identifier attributeIdIn2 = new IdentifierImpl(new URI("Http://somewhere.over.the.rainbow/attributeId"));
        Attribute attribute2 = new StdAttribute(categoryIdIn, attributeIdIn2, valueIn);
        listAttributes.add(attribute2);
        attributesIn.add(new StdAttributeCategory(identifierCategory, listAttributes));

        Result fullResult = new StdResult(Decision.DENY, obligationsIn, adviceIn, attributesIn, policyIdentifiersIn,
                policySetIdentifiersIn);
        Response fullXacmlResponse = new StdResponse(fullResult);
        PolicyGuardResponse fullPgResponse = PolicyGuardXacmlHelper.parseXacmlPdpResponse(fullXacmlResponse);
        assertEquals("Deny", fullPgResponse.getResult());
    }

    @Test
    public void testInit() {
        final Properties savedEnvironment = (Properties) PolicyEngine.manager.getEnvironment().clone();

        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngine.manager.getEnvironment().setProperty("guard.url",
                "http://localhost:6669/pdp/api/getDecision,Dorothy");
        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngine.manager.getEnvironment().setProperty("guard.url",
                "http://localhost:6669/pdp/api/getDecision,Dorothy,Toto");
        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngine.manager.getEnvironment().setProperty("guard.url", "http://localhost:6669/pdp/api/getDecision");

        PolicyEngine.manager.getEnvironment().setProperty("pdpx.timeout", "thisIsNotANumber");
        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngine.manager.getEnvironment().setProperty("pdpx.timeout", "1000");
        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngine.manager.getEnvironment().remove("pdpx.password");
        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngine.manager.getEnvironment().setProperty("pdpx.username", "python");
        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngine.manager.getEnvironment().remove("pdpx.client.password");
        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngine.manager.getEnvironment().remove("pdpx.client.username");
        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngine.manager.getEnvironment().setProperty("guard.url", "///");
        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngine.manager.getEnvironment().setProperty("guard.disabled", "");
        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngine.manager.getEnvironment().setProperty("guard.disabled", "true");
        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngine.manager.getEnvironment().clear();
        assertNotNull(new PolicyGuardXacmlHelper());

        PolicyEngine.manager.setEnvironment(savedEnvironment);
    }
}
