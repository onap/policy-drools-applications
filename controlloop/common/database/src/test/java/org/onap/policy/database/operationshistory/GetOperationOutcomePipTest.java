/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018-2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.database.operationshistory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.att.research.xacml.api.Attribute;
import com.att.research.xacml.api.AttributeValue;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.api.pip.PIPException;
import com.att.research.xacml.api.pip.PIPFinder;
import com.att.research.xacml.api.pip.PIPRequest;
import com.att.research.xacml.api.pip.PIPResponse;
import com.att.research.xacml.std.pip.StdPIPResponse;
import java.io.FileInputStream;
import java.sql.Date;
import java.time.Instant;
import java.util.Collection;
import java.util.Properties;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.database.ToscaDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetOperationOutcomePipTest {
    private static final String ID = "issuer";
    private static final String TEST_CL1 = "testcl1";
    private static final String TEST_TARGET1 = "testtarget1";
    private static final String TEST_TARGET2 = "testtarget2";
    private static final String ACTOR = "Controller";
    private static final String RECIPE = "operationA";
    private static final String EXPECTED_EXCEPTION = "expected exception";
    private static final String ISSUER_PREFIX = ToscaDictionary.GUARD_ISSUER_PREFIX + "-my-issuer:clname:";
    private static final String ISSUER = ISSUER_PREFIX + TEST_CL1;
    private static final Logger LOGGER = LoggerFactory.getLogger(GetOperationOutcomePipTest.class);

    private static MyPip pipEngine;
    private static Properties properties;

    private static EntityManagerFactory emf;
    private static EntityManager em;

    private PIPRequest req;

    /**
     * Create an instance of our engine and also the persistence
     * factory.
     *
     * @throws Exception connectivity issues
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        LOGGER.info("Setting up PIP Testing");
        //
        // Create instance
        //
        pipEngine = new MyPip(TEST_TARGET1);
        //
        // Load our test properties to use
        //
        properties = new Properties();
        try (FileInputStream is = new FileInputStream("src/test/resources/test.properties")) {
            properties.load(is);
        }
        //
        // Configure it using properties
        //
        pipEngine.configure(ID, properties);
        LOGGER.info("PIP configured now creating our entity manager");
        LOGGER.info("properties {}", properties);
        //
        // Connect to in-mem db
        //
        String persistenceUnit = GetOperationOutcomePip.ISSUER_NAME + ".persistenceunit";
        LOGGER.info("persistenceunit {}", persistenceUnit);
        emf = Persistence.createEntityManagerFactory(properties.getProperty(persistenceUnit), properties);
        em = emf.createEntityManager();
        //
        //
        //
        LOGGER.info("Configured own entity manager", em.toString());
    }

    @Before
    public void setUp() {
        req = mock(PIPRequest.class);
        when(req.getIssuer()).thenReturn(ISSUER);
    }

    private void insertEntry(String cl, String target, String outcome) {
        //
        // Create entry
        //
        Dbao newEntry = new Dbao();
        newEntry.setClosedLoopName(cl);
        newEntry.setTarget(target);
        newEntry.setOutcome(outcome);
        newEntry.setActor(ACTOR);
        newEntry.setOperation(RECIPE);
        newEntry.setStarttime(Date.from(Instant.now().minusMillis(20000)));
        newEntry.setEndtime(Date.from(Instant.now()));
        newEntry.setRequestId(UUID.randomUUID().toString());
        //
        // Add entry
        //
        em.getTransaction().begin();
        em.persist(newEntry);
        em.getTransaction().commit();
    }


    @Test
    public void testAttributesRequired() {
        assertEquals(1, pipEngine.attributesRequired().size());
    }

    @Test
    public void testGetAttributes_InvalidRequestInfo() throws PIPException {
        // invalid request - null issuer
        when(req.getIssuer()).thenReturn(null);
        assertEquals(StdPIPResponse.PIP_RESPONSE_EMPTY, pipEngine.getAttributes(req, null));

        /*
         * Make a valid issuer in the request, for subsequent tests.
         */
        when(req.getIssuer()).thenReturn(ISSUER);

        // null target
        MyPip pip = new MyPip(null);
        pip.configure(ID, properties);
        assertEquals(StdPIPResponse.PIP_RESPONSE_EMPTY, pip.getAttributes(req, null));
    }

    @Test
    public void testDoDatabaseQuery() throws Exception {
        //
        // Insert entry
        //
        insertEntry(TEST_CL1, TEST_TARGET1, "1");
        //
        // outcome should be "1"
        //
        assertEquals("1", getOutcome(pipEngine.getAttributes(req, null)));
        //
        // Insert more entries
        //
        insertEntry(TEST_CL1, TEST_TARGET1, "2");
        insertEntry("testcl2", TEST_TARGET2, "3");
        insertEntry(TEST_CL1, TEST_TARGET2, "4");
        //
        // Test pipEngine
        //
        assertEquals("2", getOutcome(TEST_CL1, TEST_TARGET1));

        assertEquals("3", getOutcome("testcl2", TEST_TARGET2));

        assertEquals("4", getOutcome(TEST_CL1, TEST_TARGET2));
    }

    @Test
    public void testDoDatabaseQuery_NoResult() throws Exception {
        MyPip pip = new MyPip(TEST_TARGET1) {
            @Override
            public void configure(String id, Properties properties) throws PIPException {
                em = mock(EntityManager.class);
                when(em.createQuery(anyString())).thenThrow(new NoResultException());
            }
        };
        pip.configure(ID, properties);

        assertNull(getOutcome(pip.getAttributes(req, null)));
    }

    @Test
    public void testDoDatabaseQuery_EmException() throws Exception {
        MyPip pip = new MyPip(TEST_TARGET1) {
            @Override
            public void configure(String id, Properties properties) throws PIPException {
                em = mock(EntityManager.class);
                when(em.createQuery(anyString())).thenThrow(new RuntimeException(EXPECTED_EXCEPTION));
            }
        };
        pip.configure(ID, properties);

        assertEquals(null, getOutcome(pip.getAttributes(req, null)));
    }

    private String getOutcome(String clname, String target) throws PIPException {
        req = mock(PIPRequest.class);
        when(req.getIssuer()).thenReturn(ISSUER_PREFIX + clname);

        MyPip pip = new MyPip(target);
        pip.configure(ID, properties);

        return getOutcome(pip.getAttributes(req, null));
    }

    private String getOutcome(PIPResponse resp) {
        Collection<Attribute> attrs = resp.getAttributes();
        assertEquals(1, attrs.size());

        Attribute attr = attrs.iterator().next();
        assertEquals(XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE, attr.getCategory());
        assertEquals(ToscaDictionary.ID_RESOURCE_GUARD_OPERATIONOUTCOME, attr.getAttributeId());

        Collection<AttributeValue<?>> values = attr.getValues();
        assertEquals(1, values.size());

        AttributeValue<?> value = values.iterator().next();
        return (String) value.getValue();
    }

    /**
     * Close the entity manager.
     */
    @AfterClass
    public static void cleanup() {
        if (emf != null) {
            emf.close();
        }
    }

    private static class MyPip extends GetOperationOutcomePip {
        private String target;

        public MyPip(String target) {
            this.target = target;
        }

        @Override
        protected String getActor(PIPFinder pipFinder) {
            return ACTOR;
        }

        @Override
        protected String getRecipe(PIPFinder pipFinder) {
            return RECIPE;
        }

        @Override
        protected String getTarget(PIPFinder pipFinder) {
            return target;
        }
    }
}
