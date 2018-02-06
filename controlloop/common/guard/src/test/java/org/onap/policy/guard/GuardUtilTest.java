/*-
 * ============LICENSE_START=======================================================
 * guard
 * ================================================================================
 * Copyright (C) 2018 Ericsson. All rights reserved.
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

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.controlloop.policy.guard.ControlLoopGuard;
import org.onap.policy.guard.Util.Pair;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.att.aft.dme2.internal.google.common.io.Files;

public class GuardUtilTest {
	@Test
	public void testLoadYamlOK() throws IOException {
		File tempYAMLFile = File.createTempFile("ONAPPF", "yaml");

		ControlLoopPolicy clPolicy = new ControlLoopPolicy();
		
		Yaml clYaml = new Yaml(new Constructor(ControlLoopPolicy.class));
		String clYamlString = clYaml.dump(clPolicy);
		
		TextFileUtils.putStringAsFile(clYamlString, tempYAMLFile);
		
		Pair<ControlLoopPolicy, String> result = Util.loadYaml(tempYAMLFile.getCanonicalPath());
		
		assertEquals(clPolicy, result.a);
		assertEquals(clYamlString, result.b);
		
		tempYAMLFile.delete();
	}
	
	@Test
	public void testLoadYamlError() throws IOException {
		File tempDir = Files.createTempDir();
		
		// Read from a directory forces an IO exception
		assertNull(Util.loadYaml(tempDir.getCanonicalPath()));
		
		tempDir.delete();
	}
	
	@Test
	public void testLoadGuardYamlOK() throws IOException {
		File tempYAMLFile = File.createTempFile("ONAPPF", "yaml");

		ControlLoopGuard clGuardPolicy = new ControlLoopGuard();
		
		Yaml clYaml = new Yaml(new Constructor(ControlLoopPolicy.class));
		String clYamlString = clYaml.dump(clGuardPolicy);
		
		TextFileUtils.putStringAsFile(clYamlString, tempYAMLFile);
		
		ControlLoopGuard result = Util.loadYamlGuard(tempYAMLFile.getCanonicalPath());
		
		assertEquals(clGuardPolicy, result);
		
		tempYAMLFile.delete();
	}
	
	@Test
	public void testLoadGuardYamlError() throws IOException {
		File tempDir = Files.createTempDir();
		
		// Read from a directory forces an IO exception
		assertNull(Util.loadYamlGuard(tempDir.getCanonicalPath()));
		
		tempDir.delete();
	}
	
	@Test
	public void testMisc() {
		Util.setGuardEnvProp("Actor", "Judy Garland");
		assertEquals("Judy Garland", Util.getGuardProp("Actor"));
		
		Util.setGuardEnvProps("http://somewhere.over.the.rainbow", "Dorothy", "Toto", "Wizard", "Emerald", "Oz");

		assertEquals("http://somewhere.over.the.rainbow", Util.getGuardProp(Util.PROP_GUARD_URL));
		assertEquals("Dorothy", Util.getGuardProp(Util.PROP_GUARD_USER));
		assertEquals("Toto", Util.getGuardProp(Util.PROP_GUARD_PASS));
		assertEquals("Wizard", Util.getGuardProp(Util.PROP_GUARD_CLIENT_USER));
		assertEquals("Emerald", Util.getGuardProp(Util.PROP_GUARD_CLIENT_PASS));
		assertEquals("Oz", Util.getGuardProp(Util.PROP_GUARD_ENV));
	}
}
