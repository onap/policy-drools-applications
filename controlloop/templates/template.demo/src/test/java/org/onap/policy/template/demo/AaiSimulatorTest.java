package org.onap.policy.template.demo;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.onap.policy.aai.AAIGETResponse;
import org.onap.policy.aai.AAINQF199.AAINQF199Manager;
import org.onap.policy.aai.AAINQF199.AAINQF199Request;
import org.onap.policy.aai.AAINQF199.AAINQF199Response;
import org.onap.policy.drools.http.server.HttpServletServer;

public class AaiSimulatorTest {
	
	@BeforeClass
	public static void setUpSimulator() {
		try {
			Util.buildAaiSim();
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}
	}
	
	@AfterClass
	public static void tearDownSimulator() {
		HttpServletServer.factory.destroy();
	}
	
	@Test
	public void testGet() {
		AAIGETResponse response = AAINQF199Manager.getQuery("http://localhost:6666", "testUser", "testPass", UUID.randomUUID(), "5e49ca06-2972-4532-9ed4-6d071588d792");
		assertNotNull(response);
		assertNotNull(response.relationshipList);
	}

	@Test
	public void testPost() {
		AAINQF199Response response = AAINQF199Manager.postQuery("http://localhost:6666", "testUser", "testPass", new AAINQF199Request(), UUID.randomUUID());
		assertNotNull(response);
		assertNotNull(response.inventoryResponseItems);
	}
}
