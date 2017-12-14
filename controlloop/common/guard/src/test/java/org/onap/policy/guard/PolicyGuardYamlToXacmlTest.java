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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class PolicyGuardYamlToXacmlTest {

	@Test
	public void testFromYamlToXacml() {
		//PolicyGuardYamlToXacml.fromYamlToXacml(yamlFile, xacmlTemplate, xacmlPolicyOutput);
		//fail("Not yet implemented");
	}

	@Test
	public void testGenerateXacmlGuard() {
		String dummyFileContent = "${clname}, ${actor}, ${recipe}, ${targets}, ${limit}, ${twValue}, ${twUnits}, ${guardActiveStart}, ${guardActiveEnd}";
		List<String> targets = new ArrayList();
		targets.add("target1");
		targets.add("target2");
		Map<String, String> tw = new HashMap();
		tw.put("value", "10");
		tw.put("units", "hours");
		String res = PolicyGuardYamlToXacml.generateXacmlGuard(dummyFileContent,
				"cl", "actor", "recipe", targets, 5, tw, "start", "end");

		// Assert no mote "${}" are left
		assertFalse(res.contains("${"));
		assertFalse(res.contains("}"));
		// Assert all substitutions are made
		assertTrue(res.contains("cl"));
		assertTrue(res.contains("actor"));
		assertTrue(res.contains("recipe"));
		assertTrue(res.contains("target1"));
		assertTrue(res.contains("target2"));
		assertTrue(res.contains("10"));
		assertTrue(res.contains("hours"));
		assertTrue(res.contains("start"));
		assertTrue(res.contains("end"));
	}

	@Test
	public void testIsNullOrEmpty() {
		assertTrue(PolicyGuardYamlToXacml.isNullOrEmpty(""));
		assertTrue(PolicyGuardYamlToXacml.isNullOrEmpty(null));
		assertFalse(PolicyGuardYamlToXacml.isNullOrEmpty("hello"));
	}

	@Test
	public void testIsNullOrEmptyList() {
		List<String> l = new ArrayList();
		assertTrue(PolicyGuardYamlToXacml.isNullOrEmptyList(null));
		assertTrue(PolicyGuardYamlToXacml.isNullOrEmptyList(l));

		l.add("hello");
		assertFalse(PolicyGuardYamlToXacml.isNullOrEmptyList(l));
	}

	@Test
	public void testFromYamlToXacmlBlacklist() {
		//fail("Not yet implemented");
	}

	@Test
	public void testGenerateXacmlGuardBlacklist() {
		String dummyFileContent = "${clname}, ${actor}, ${recipe}, ${blackListElement}, ${guardActiveStart}, ${guardActiveEnd}";
		List<String> blacklist = new ArrayList();
		blacklist.add("target1");
		blacklist.add("target2");
		String res = PolicyGuardYamlToXacml.generateXacmlGuardBlacklist(dummyFileContent,
				"cl", "actor", "recipe", blacklist, "start", "end");

		// Assert no mote "${}" are left
		assertFalse(res.contains("${"));
		assertFalse(res.contains("}"));
		// Assert all substitutions are made
		assertTrue(res.contains("cl"));
		assertTrue(res.contains("actor"));
		assertTrue(res.contains("recipe"));
		assertTrue(res.contains("target1"));
		assertTrue(res.contains("target2"));
		assertTrue(res.contains("start"));
		assertTrue(res.contains("end"));
	}

}
