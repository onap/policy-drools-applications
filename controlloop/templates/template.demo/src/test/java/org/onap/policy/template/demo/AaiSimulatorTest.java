package org.onap.policy.template.demo;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;
import org.onap.policy.aai.AAIGETResponse;
import org.onap.policy.aai.AAINQF199.AAINQF199Manager;
import org.onap.policy.aai.AAINQF199.AAINQF199Request;
import org.onap.policy.aai.AAINQF199.AAINQF199Response;
import org.onap.policy.drools.http.server.HttpServletServer;

public class AaiSimulatorTest {
	
	@Test
	public void testGet() {
		try {
			HttpServletServer testServer = Util.buildAaiSim();
			
			AAIGETResponse response = AAINQF199Manager.getQuery("http://localhost:6666", "testUser", "testPass", UUID.randomUUID(), "5e49ca06-2972-4532-9ed4-6d071588d792");
			assertNotNull(response);
			assertNotNull(response.relationshipList);
		}
		catch (InterruptedException e) {
			fail(e.getMessage());
		}
		finally {
			HttpServletServer.factory.destroy();
		}
	}

	@Test
	public void testPost() {
		try {
			HttpServletServer testServer = Util.buildAaiSim();
			
			AAINQF199Response response = AAINQF199Manager.postQuery("http://localhost:6666", "testUser", "testPass", new AAINQF199Request(), UUID.randomUUID());
			assertNotNull(response);
			assertNotNull(response.inventoryResponseItems);
		}
		catch (InterruptedException e) {
			fail(e.getMessage());
		}
		finally {
			HttpServletServer.factory.destroy();
		}
	}
}
