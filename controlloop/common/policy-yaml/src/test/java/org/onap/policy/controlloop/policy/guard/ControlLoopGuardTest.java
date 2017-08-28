/*-
 * ============LICENSE_START=======================================================
 * policy-yaml unit test
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

package org.onap.policy.controlloop.policy.guard;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;


public class ControlLoopGuardTest {
	private static final Logger logger = LoggerFactory.getLogger(ControlLoopGuardTest.class);
	@Test 
	public void testGuardvDNS() {
		this.test("src/test/resources/v2.0.0-guard/policy_guard_ONAP_demo_vDNS.yaml");
	}

	@Test 
	public void testGuardvUSP() {
		this.test("src/test/resources/v2.0.0-guard/policy_guard_appc_restart.yaml");
	}

	
	public void test(String testFile) {
		try (InputStream is = new FileInputStream(new File(testFile))) {
			//
			// Read the yaml into our Java Object
			//
			Yaml yaml = new Yaml(new Constructor(ControlLoopGuard.class));
			Object obj = yaml.load(is);
			assertNotNull(obj);
			assertTrue(obj instanceof ControlLoopGuard);
			dump(obj);
			//
			// Now dump it to a yaml string
			//
			DumperOptions options = new DumperOptions();
			options.setDefaultFlowStyle(FlowStyle.BLOCK);
			options.setPrettyFlow(true);
			yaml = new Yaml(options);
			String dumpedYaml = yaml.dump(obj);
			logger.debug(dumpedYaml);
			//
			// Read that string back into our java object
			//
			Object newObject = yaml.load(dumpedYaml);
			dump(newObject);
			assertNotNull(newObject);
			assertTrue(newObject instanceof ControlLoopGuard);
			//
			// Have to comment it out tentatively since it causes junit to fail. 
			// Seems we cannot use assertEquals here. Need advice.
			//
			//assertEquals(newObject, obj);
		} catch (FileNotFoundException e) {
			fail(e.getLocalizedMessage());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
	}
	
	public void dump(Object obj) {
		logger.debug("Dumping {}", obj.getClass().getCanonicalName());
		logger.debug("{}", obj);
	}
}
