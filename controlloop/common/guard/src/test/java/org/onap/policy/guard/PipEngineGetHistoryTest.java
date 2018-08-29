/*-
 * ============LICENSE_START=======================================================
 * guard
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.att.research.xacml.api.Attribute;
import com.att.research.xacml.api.AttributeValue;
import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.Status;
import com.att.research.xacml.api.pip.PIPEngine;
import com.att.research.xacml.api.pip.PIPException;
import com.att.research.xacml.api.pip.PIPFinder;
import com.att.research.xacml.api.pip.PIPRequest;
import com.att.research.xacml.api.pip.PIPResponse;
import com.att.research.xacml.std.IdentifierImpl;
import com.att.research.xacml.std.StdAttribute;
import com.att.research.xacml.std.StdAttributeValue;
import com.att.research.xacml.std.StdStatus;
import com.att.research.xacml.std.StdStatusCode;
import com.att.research.xacml.std.pip.StdPIPRequest;
import com.att.research.xacml.std.pip.StdPIPResponse;
import com.att.research.xacml.std.pip.finders.EngineFinder;
import com.att.research.xacml.util.FactoryException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.drools.system.PolicyEngine;

public class PipEngineGetHistoryTest {
    static PIPEngineGetHistory pegh;
    private static final String ISSUER = "issuerIntw:mid:end";
    
    private static EntityManagerFactory emf;
    private static EntityManager em;

    /**
     * Set up test class.
     */
    @BeforeClass
    public static void testPipEngineGetHistory() {
        pegh = null;
        try {
            pegh = new PIPEngineGetHistory();
        } catch (Exception e) {
            fail("PIPEngineGetHistory constructor failed");
        }

        // Set PU
        System.setProperty(Util.PU_KEY, Util.JUNITPU);

        // Enter dummy props to avoid nullPointerException
        PolicyEngine.manager.setEnvironmentProperty(Util.ONAP_KEY_URL, "a");
        PolicyEngine.manager.setEnvironmentProperty(Util.ONAP_KEY_USER, "b");
        PolicyEngine.manager.setEnvironmentProperty(Util.ONAP_KEY_PASS, "c");

        // Connect to in-mem db
        emf = Persistence.createEntityManagerFactory(Util.JUNITPU);
        em = emf.createEntityManager();

        // Create necessary table
        String sql = "CREATE TABLE `operationshistory10` (" + "`CLNAME` varchar(255)," + "`requestID` varchar(100),"
                + "`actor` varchar(50) ," + "`operation` varchar(50)," + "`target` varchar(50),"
                + "`starttime` timestamp," + "`outcome` varchar(50)," + "`message` varchar(255),"
                + "`subrequestId` varchar(100)," + "`endtime` timestamp" + ")";
        Query nq = em.createNativeQuery(sql);
        em.getTransaction().begin();
        nq.executeUpdate();
        em.getTransaction().commit();
    }
    
    @AfterClass
    public static void tearDown() {
        emf.close();
    }

    /**
     * Setup method.
     */
    @Before
    public void setUp() {
        // clear the table
        String sql = "DELETE FROM `operationshistory10`";
        Query nq = em.createNativeQuery(sql);
        em.getTransaction().begin();
        nq.executeUpdate();
        em.getTransaction().commit();
    }

    @Test
    public void testAttributesRequired() {
        assertTrue(pegh.attributesRequired().isEmpty());
    }

    @Test
    public void testAttributesProvided() {
        assertTrue(pegh.attributesProvided().isEmpty());
    }

    @Test
    public void testGetAttributes() {
        StdPIPRequest mockPipRequest = mock(StdPIPRequest.class);
        EngineFinder mockPipFinder = mock(EngineFinder.class);

        // Test issuer null
        when(mockPipRequest.getIssuer()).thenReturn(null);
        try {
            assertEquals(StdPIPResponse.PIP_RESPONSE_EMPTY, pegh.getAttributes(mockPipRequest, mockPipFinder));
        } catch (Exception e) {
            fail("getAttributes failed");
        }

        // Test issuer not equal to our issuer
        pegh.setIssuer(ISSUER);
        when(mockPipRequest.getIssuer()).thenReturn("something else");
        try {
            assertEquals(StdPIPResponse.PIP_RESPONSE_EMPTY, pegh.getAttributes(mockPipRequest, mockPipFinder));
        } catch (Exception e) {
            fail("getAttributes failed");
        }

        // Test issuer equal to our issuer
        when(mockPipRequest.getIssuer()).thenReturn(ISSUER);
        try {
            assertNotNull(pegh.getAttributes(mockPipRequest, mockPipFinder));
        } catch (Exception e) {
            // Normal to catch exception
        }
    }

    @Test
    public void testGetCountFromDb() {

        // Use reflection to run getCountFromDB
        Method method = null;
        int count = -1;
        try {
            method = PIPEngineGetHistory.class.getDeclaredMethod("getCountFromDb", String.class, String.class,
                    String.class, String.class);
            method.setAccessible(true);
            count = (int) method.invoke(null, "actor", "op", "target", "1 MINUTE");
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException e) {
            fail(e.getLocalizedMessage());
        }
        // No entries yet
        assertEquals(0, count);

        // Add an entry
        String addEntry = "insert into operationshistory10 (outcome, CLNAME, actor, operation, target, endtime)"
                + "values('success','testcl', 'actor', 'op', 'target', CURRENT_TIMESTAMP())";
        Query nq2 = em.createNativeQuery(addEntry);
        em.getTransaction().begin();
        nq2.executeUpdate();
        em.getTransaction().commit();

        try {
            count = (int) method.invoke(null, "actor", "op", "target", "1 MINUTE");
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            fail(e.getLocalizedMessage());
        }
        // Should count 1 entry now
        assertEquals(1, count);
    }

    @Test
    public void testConfigure() throws PIPException {
        PIPEngineGetHistory pegh = new PIPEngineGetHistory();
        pegh.configure("Dorothy", new Properties());

        pegh.setDescription(null);
        pegh.setIssuer(null);
        pegh.configure("Dorothy", new Properties());
    }

    @Test
    public void getAttributesTest() throws URISyntaxException, PIPException, FactoryException {
        PIPEngineGetHistory pegh = new PIPEngineGetHistory();
        pegh.setIssuer("Dorothy");

        Identifier identifierCategory = new IdentifierImpl(new URI("http://somewhere.over.the.rainbow/category"));;
        Identifier identifierAttribute = new IdentifierImpl(new URI("http://somewhere.over.the.rainbow/atrtribute"));;
        Identifier identifierDataType = new IdentifierImpl(new URI("http://somewhere.over.the.rainbow/datatype"));;
        PIPRequest pipRequest = new StdPIPRequest(identifierCategory, identifierAttribute, identifierDataType,
                "Dorothy,tw:1000:SECOND");

        assertNotNull(pegh.getAttributes(pipRequest, new DummyPipFinder()));


        assertNotNull(pegh.getAttributes(pipRequest, new DummyPipFinderPipException()));
        assertNotNull(pegh.getAttributes(pipRequest, new DummyPipFinderResponseStatusNok()));
        assertNotNull(pegh.getAttributes(pipRequest, new DummyPipFinderResponseEmptyAttrs()));
    }

    @Test
    public void timeWindowTest() throws URISyntaxException, PIPException, FactoryException {
        PIPEngineGetHistory pegh = new PIPEngineGetHistory();
        pegh.setIssuer("Dorothy");

        Identifier identifierCategory = new IdentifierImpl(new URI("http://somewhere.over.the.rainbow/category"));;
        Identifier identifierAttribute = new IdentifierImpl(new URI("http://somewhere.over.the.rainbow/atrtribute"));;
        Identifier identifierDataType = new IdentifierImpl(new URI("http://somewhere.over.the.rainbow/datatype"));;

        PIPRequest pipRequest =
                new StdPIPRequest(identifierCategory, identifierAttribute, identifierDataType, "Dorothy,tw:100:SECOND");
        assertNotNull(pegh.getAttributes(pipRequest, new DummyPipFinder()));

        pipRequest =
                new StdPIPRequest(identifierCategory, identifierAttribute, identifierDataType, "Dorothy,tw:100:MINUTE");
        assertNotNull(pegh.getAttributes(pipRequest, new DummyPipFinder()));

        pipRequest =
                new StdPIPRequest(identifierCategory, identifierAttribute, identifierDataType, "Dorothy,tw:100:HOUR");
        assertNotNull(pegh.getAttributes(pipRequest, new DummyPipFinder()));

        pipRequest =
                new StdPIPRequest(identifierCategory, identifierAttribute, identifierDataType, "Dorothy,tw:100:DAY");
        assertNotNull(pegh.getAttributes(pipRequest, new DummyPipFinder()));

        pipRequest =
                new StdPIPRequest(identifierCategory, identifierAttribute, identifierDataType, "Dorothy,tw:100:WEEK");
        assertNotNull(pegh.getAttributes(pipRequest, new DummyPipFinder()));

        pipRequest =
                new StdPIPRequest(identifierCategory, identifierAttribute, identifierDataType, "Dorothy,tw:100:MONTH");
        assertNotNull(pegh.getAttributes(pipRequest, new DummyPipFinder()));

        pipRequest = new StdPIPRequest(identifierCategory, identifierAttribute, identifierDataType,
                "Dorothy,tw:100:QUARTER");
        assertNotNull(pegh.getAttributes(pipRequest, new DummyPipFinder()));

        pipRequest =
                new StdPIPRequest(identifierCategory, identifierAttribute, identifierDataType, "Dorothy,tw:100:YEAR");
        assertNotNull(pegh.getAttributes(pipRequest, new DummyPipFinder()));

        pipRequest = new StdPIPRequest(identifierCategory, identifierAttribute, identifierDataType,
                "Dorothy,tw:100:FORTNIGHT");
        assertNotNull(pegh.getAttributes(pipRequest, new DummyPipFinder()));

        pipRequest = new StdPIPRequest(identifierCategory, identifierAttribute, identifierDataType,
                "Dorothy,tw:100:FORT NIGHT");
        assertNotNull(pegh.getAttributes(pipRequest, new DummyPipFinder()));

        assertNotNull(pegh.getAttributes(pipRequest, new DummyPipFinderPipException()));
        assertNotNull(pegh.getAttributes(pipRequest, new DummyPipFinderResponseStatusNok()));
        assertNotNull(pegh.getAttributes(pipRequest, new DummyPipFinderResponseEmptyAttrs()));
    }

    private class DummyPipFinder implements PIPFinder {
        @Override
        public PIPResponse getAttributes(PIPRequest pipRequest, PIPEngine exclude) throws PIPException {
            return null;
        }

        @Override
        public PIPResponse getAttributes(PIPRequest pipRequest, PIPEngine exclude, PIPFinder pipFinderParent)
                throws PIPException {
            return null;
        }

        @Override
        public PIPResponse getMatchingAttributes(PIPRequest pipRequest, PIPEngine exclude) throws PIPException {
            try {
                List<Attribute> attributeList = new ArrayList<>();
                Identifier categoryIdIn = new IdentifierImpl(new URI("http://somewhere.over.the.rainbow/category"));
                Identifier dataTypeIdIn = new IdentifierImpl(new URI("http://www.w3.org/2001/XMLSchema#string"));

                Identifier attributeIdIn0 = new IdentifierImpl(new URI(UUID.randomUUID().toString()));
                AttributeValue<String> valueIn0 = new StdAttributeValue<String>(dataTypeIdIn, "ActorDorothy");
                Attribute attribute0 = new StdAttribute(categoryIdIn, attributeIdIn0, valueIn0);
                attributeList.add(attribute0);

                Identifier attributeIdIn1 = new IdentifierImpl(new URI(UUID.randomUUID().toString()));
                AttributeValue<String> valueIn1 = new StdAttributeValue<String>(dataTypeIdIn, "OperationHomeFromOZ");
                Attribute attribute1 = new StdAttribute(categoryIdIn, attributeIdIn1, valueIn1);
                attributeList.add(attribute1);

                Identifier attributeIdIn2 = new IdentifierImpl(new URI(UUID.randomUUID().toString()));
                AttributeValue<String> valueIn2 = new StdAttributeValue<String>(dataTypeIdIn, "TargetWickedWitch");
                Attribute attribute2 = new StdAttribute(categoryIdIn, attributeIdIn2, valueIn2);
                attributeList.add(attribute2);

                return new StdPIPResponse(attributeList);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public PIPResponse getMatchingAttributes(PIPRequest pipRequest, PIPEngine exclude, PIPFinder pipFinderParent)
                throws PIPException {
            return null;
        }

        @Override
        public Collection<PIPEngine> getPIPEngines() {
            return null;
        }
    }

    private class DummyPipFinderPipException implements PIPFinder {
        @Override
        public PIPResponse getAttributes(PIPRequest pipRequest, PIPEngine exclude) throws PIPException {
            return null;
        }

        @Override
        public PIPResponse getAttributes(PIPRequest pipRequest, PIPEngine exclude, PIPFinder pipFinderParent)
                throws PIPException {
            return null;
        }

        @Override
        public PIPResponse getMatchingAttributes(PIPRequest pipRequest, PIPEngine exclude) throws PIPException {
            throw new PIPException();
        }

        @Override
        public PIPResponse getMatchingAttributes(PIPRequest pipRequest, PIPEngine exclude, PIPFinder pipFinderParent)
                throws PIPException {
            return null;
        }

        @Override
        public Collection<PIPEngine> getPIPEngines() {
            return null;
        }
    }

    private class DummyPipFinderResponseStatusNok implements PIPFinder {
        @Override
        public PIPResponse getAttributes(PIPRequest pipRequest, PIPEngine exclude) throws PIPException {
            return null;
        }

        @Override
        public PIPResponse getAttributes(PIPRequest pipRequest, PIPEngine exclude, PIPFinder pipFinderParent)
                throws PIPException {
            return null;
        }

        @Override
        public PIPResponse getMatchingAttributes(PIPRequest pipRequest, PIPEngine exclude) throws PIPException {
            Status status = new StdStatus(StdStatusCode.STATUS_CODE_PROCESSING_ERROR, "Processing Error");
            return new StdPIPResponse(status);
        }

        @Override
        public PIPResponse getMatchingAttributes(PIPRequest pipRequest, PIPEngine exclude, PIPFinder pipFinderParent)
                throws PIPException {
            return null;
        }

        @Override
        public Collection<PIPEngine> getPIPEngines() {
            return null;
        }
    }

    private class DummyPipFinderResponseEmptyAttrs implements PIPFinder {
        @Override
        public PIPResponse getAttributes(PIPRequest pipRequest, PIPEngine exclude) throws PIPException {
            return null;
        }

        @Override
        public PIPResponse getAttributes(PIPRequest pipRequest, PIPEngine exclude, PIPFinder pipFinderParent)
                throws PIPException {
            return null;
        }

        @Override
        public PIPResponse getMatchingAttributes(PIPRequest pipRequest, PIPEngine exclude) throws PIPException {
            List<Attribute> attributeList = new ArrayList<>();
            return new StdPIPResponse(attributeList);
        }

        @Override
        public PIPResponse getMatchingAttributes(PIPRequest pipRequest, PIPEngine exclude, PIPFinder pipFinderParent)
                throws PIPException {
            return null;
        }

        @Override
        public Collection<PIPEngine> getPIPEngines() {
            return null;
        }
    }
}
