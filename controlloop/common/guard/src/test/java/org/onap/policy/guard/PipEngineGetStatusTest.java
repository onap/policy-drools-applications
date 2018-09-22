/*-
 * ============LICENSE_START=======================================================
 * guard
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights reserved.
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

import com.att.research.xacml.api.pip.PIPException;
import com.att.research.xacml.std.pip.StdPIPRequest;
import com.att.research.xacml.std.pip.StdPIPResponse;
import com.att.research.xacml.std.pip.finders.EngineFinder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.drools.system.PolicyEngine;

public class PipEngineGetStatusTest {
    static PipEngineGetStatus pegs;
    private static final String ISSUER = "issuer:clname:testclname";
    
    private static EntityManagerFactory emf;
    private static EntityManager em;

    /**
     * Set up test class.
     */
    @BeforeClass
    public static void testPipEngineGetStatus() {
        pegs = null;
        try {
            pegs = new PipEngineGetStatus();
        } catch (Exception e) {
            fail("PipEngineGetStatus constructor failed");
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

    /**
     * Clean up test class.
     */
    @AfterClass
    public static void tearDown() {
        String sql = "DROP TABLE `operationshistory10`";
        Query nq = em.createNativeQuery(sql);
        em.getTransaction().begin();
        nq.executeUpdate();
        em.getTransaction().commit();
        em.close();
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
        assertTrue(pegs.attributesRequired().isEmpty());
    }

    @Test
    public void testAttributesProvided() {
        assertTrue(pegs.attributesProvided().isEmpty());
    }

    @Test
    public void testGetAttributes() {
        StdPIPRequest mockPipRequest = mock(StdPIPRequest.class);
        EngineFinder mockPipFinder = mock(EngineFinder.class);

        // Test issuer null
        when(mockPipRequest.getIssuer()).thenReturn(null);
        try {
            assertEquals(StdPIPResponse.PIP_RESPONSE_EMPTY, pegs.getAttributes(mockPipRequest, mockPipFinder));
        } catch (Exception e) {
            fail("getAttributes failed");
        }
        
        // Test issuer not equal to our issuer
        pegs.setIssuer(ISSUER);
        when(mockPipRequest.getIssuer()).thenReturn("something else");
        try {
            assertEquals(StdPIPResponse.PIP_RESPONSE_EMPTY, pegs.getAttributes(mockPipRequest, mockPipFinder));
        } catch (Exception e) {
            fail("getAttributes failed");
        }
        
        // Test issuer equal to our issuer
        when(mockPipRequest.getIssuer()).thenReturn(ISSUER);
        try {
            assertNotNull(pegs.getAttributes(mockPipRequest, mockPipFinder));
        } catch (Exception e) {
            // Normal to catch exception
        }
    }

    @Test
    public void testGetStatusFromDb() {

        // Use reflection to run getStatsFromDB
        Method method = null;
        String status = null;
        String addEntry;
        Query nq;
        
        // Add an entry
        addEntry = "insert into operationshistory10 (outcome, CLNAME, actor, operation, target, endtime)"
            + "values('1','testcl', 'actor', 'op', 'testtarget', CURRENT_TIMESTAMP())";
        nq = em.createNativeQuery(addEntry);
        em.getTransaction().begin();
        nq.executeUpdate();
        em.getTransaction().commit();

        try {
            method = PipEngineGetStatus.class.getDeclaredMethod("getStatusFromDb", String.class, String.class);
            method.setAccessible(true);
            status = (String) method.invoke(null, "testcl", "testtarget");
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException e) {
            fail(e.getLocalizedMessage());
        }

        // Status should be "success"
        assertEquals("1", status);

        // Add entries
        addEntry = "insert into operationshistory10 (outcome, CLNAME, actor, operation, target, endtime)"
            + "values('2','testcl', 'actor', 'op', 'testtarget', CURRENT_TIMESTAMP())";
        nq = em.createNativeQuery(addEntry);
        em.getTransaction().begin();
        nq.executeUpdate();
        em.getTransaction().commit();

        addEntry = "insert into operationshistory10 (outcome, CLNAME, actor, operation, target, endtime)"
            + "values('3','testcl', 'actor', 'op', 'testtarget2', CURRENT_TIMESTAMP())";
        nq = em.createNativeQuery(addEntry);
        em.getTransaction().begin();
        nq.executeUpdate();
        em.getTransaction().commit();

        addEntry = "insert into operationshistory10 (outcome, CLNAME, actor, operation, target, endtime)"
            + "values('4','testcl2', 'actor', 'op', 'testtarget2', CURRENT_TIMESTAMP())";
        nq = em.createNativeQuery(addEntry);
        em.getTransaction().begin();
        nq.executeUpdate();
        em.getTransaction().commit();
        
        try {
            method = PipEngineGetStatus.class.getDeclaredMethod("getStatusFromDb", String.class, String.class);
            method.setAccessible(true);
            status = (String) method.invoke(null, "testcl", "testtarget");
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException e) {
            fail(e.getLocalizedMessage());
        }
        assertEquals("2", status);

        try {
            method = PipEngineGetStatus.class.getDeclaredMethod("getStatusFromDb", String.class, String.class);
            method.setAccessible(true);
            status = (String) method.invoke(null, "testcl", "testtarget2");
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException e) {
            fail(e.getLocalizedMessage());
        }
        assertEquals("3", status);

        try {
            method = PipEngineGetStatus.class.getDeclaredMethod("getStatusFromDb", String.class, String.class);
            method.setAccessible(true);
            status = (String) method.invoke(null, "testcl2", "testtarget2");
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException e) {
            fail(e.getLocalizedMessage());
        }
        assertEquals("4", status);
    }

    @Test
    public void testConfigure() throws PIPException {
        PipEngineGetStatus pegs = new PipEngineGetStatus();
        pegs.configure("Dorothy", new Properties());

        pegs.setDescription(null);
        pegs.setIssuer(null);
        pegs.configure("Dorothy", new Properties());
    }

}
