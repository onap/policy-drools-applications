/*-
 * ============LICENSE_START=======================================================
 * guard
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.drools.system.PolicyEngine;

import com.att.research.xacml.std.pip.StdPIPRequest;
import com.att.research.xacml.std.pip.StdPIPResponse;
import com.att.research.xacml.std.pip.finders.EngineFinder;

public class PIPEngineGetHistoryTest {
	static PIPEngineGetHistory pegh;
	private static final String ISSUER = "issuerIntw:mid:end";

	@BeforeClass
	public static void testPIPEngineGetHistory(){
		pegh = null;
		try{
			pegh = new PIPEngineGetHistory();
		} catch(Exception e){
			fail("PIPEngineGetHistory constructor failed");
		}
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
		StdPIPRequest mockPIPRequest = mock(StdPIPRequest.class);
		EngineFinder mockPIPFinder = mock(EngineFinder.class);

		// Test issuer null
		when(mockPIPRequest.getIssuer()).thenReturn(null);
		try {
			assertEquals(pegh.getAttributes(mockPIPRequest, mockPIPFinder),
					StdPIPResponse.PIP_RESPONSE_EMPTY);
		} catch (Exception e) {
			fail("getAttributes failed");
		}

		// Test issuer not equal to our issuer
		pegh.setIssuer(ISSUER);
		when(mockPIPRequest.getIssuer()).thenReturn("something else");
		try {
			assertEquals(pegh.getAttributes(mockPIPRequest, mockPIPFinder),
					StdPIPResponse.PIP_RESPONSE_EMPTY);
		} catch (Exception e) {
			fail("getAttributes failed");
		}

		// Test issuer equal to our issuer
		when(mockPIPRequest.getIssuer()).thenReturn(ISSUER);
		try {
			assertNotNull(pegh.getAttributes(mockPIPRequest, mockPIPFinder));
		} catch (Exception e) {
			// Normal to catch exception
		}
	}

	@Test
	public void testGetCountFromDB(){
		// Set PU
		System.setProperty(Util.PU_KEY, Util.JUNITPU);

		//Enter dummy props to avoid nullPointerException
		PolicyEngine.manager.setEnvironmentProperty(Util.ONAP_KEY_URL,  "a");
		PolicyEngine.manager.setEnvironmentProperty(Util.ONAP_KEY_USER, "b");
		PolicyEngine.manager.setEnvironmentProperty(Util.ONAP_KEY_PASS, "c");

		// Connect to in-mem db
		EntityManager em = null;
		try{
			em = Persistence.createEntityManagerFactory(Util.JUNITPU).createEntityManager();
		} catch(Exception e){
			fail(e.getLocalizedMessage());
		}

		String sql = "CREATE TABLE `operationshistory10` (" +
				"`CLNAME` varchar(255)," +
				"`requestID` varchar(100)," +
				"`actor` varchar(50) ," +
				"`operation` varchar(50)," +
				"`target` varchar(50)," +
				"`starttime` timestamp," +
				"`outcome` varchar(50)," +
				"`message` varchar(255)," +
				"`subrequestId` varchar(100)," +
				"`endtime` timestamp"+
				")";
		// Create necessary table
		Query nq = em.createNativeQuery(sql);
		em.getTransaction().begin();
		nq.executeUpdate();
		em.getTransaction().commit();

		// Use reflection to run getCountFromDB
		Method method = null;
		int count = -1;
		try {
			method = PIPEngineGetHistory.class.getDeclaredMethod("getCountFromDB", String.class, String.class, String.class, String.class);
			method.setAccessible(true);
			count = (int) method.invoke(null, "actor", "op", "target", "1 MINUTE");
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e) {
			fail(e.getLocalizedMessage());
		}
		// No entries yet
		assertEquals(0, count);

		// Add an entry
		String addEntry = "insert into operationshistory10 (outcome, CLNAME, actor, operation, target, endtime)" +
				"values('success','testcl', 'actor', 'op', 'target', CURRENT_TIMESTAMP())";
		Query nq2 = em.createNativeQuery(addEntry);
		em.getTransaction().begin();
		nq2.executeUpdate();
		em.getTransaction().commit();
		em.close();

		try {
			count = (int) method.invoke(null, "actor", "op", "target", "1 MINUTE");
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			fail(e.getLocalizedMessage());
		}
		// Should count 1 entry now
		assertEquals(1, count);
	}

}
