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

import org.junit.Ignore;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.onap.policy.controlloop.policy.builder.BuilderException;
import org.onap.policy.controlloop.policy.builder.Message;
import org.onap.policy.controlloop.policy.builder.MessageLevel;
import org.onap.policy.controlloop.policy.builder.Results;
import org.onap.policy.controlloop.poligy.guard.builder.ControlLoopGuardBuilder;

public class ControlLoopGuardBuilderTest {
	
	@Ignore 
	@Test
	public void testControlLoopGuard() {
		try {
			//
			// Create a builder
			//
			ControlLoopGuardBuilder builder = ControlLoopGuardBuilder.Factory.buildControlLoopGuard(new Guard());
			//
			// Assert there is no guard policies yet
			//
			Results results = builder.buildSpecification();
			boolean no_guard_policies = false;
			for (Message m : results.getMessages()) {
				if (m.getMessage().equals("ControlLoop Guard should have at least one guard policies") && m.getLevel() == MessageLevel.ERROR) {
					no_guard_policies = true;
					break;
				}
			}
			assertTrue(no_guard_policies);
			//
			// Add a guard policy without limit constraint
			//
			GuardPolicy policy1 = new GuardPolicy("1111", "guardpolicy1", "guardpolicy1", "APPC", "restart");
			builder = builder.addGuardPolicy(policy1);
			//
			// Assert there is no limit constraint associated with the only guard policy
			//
			results = builder.buildSpecification();
			boolean no_constraint = false;
			for (Message m : results.getMessages()) {
				if (m.getMessage().equals("Guard policy guardpolicy1 does not have any limit constraint") && m.getLevel() == MessageLevel.ERROR) {
					no_constraint = true;
					break;
				}
			}
			assertTrue(no_constraint);
			//
			// Add a constraint to policy1
			//
			Map<String, String> time_in_range = new HashMap<String, String>();
			time_in_range.put("arg2", "PT5H");
			time_in_range.put("arg3", "PT24H");
			List<String> blacklist = new LinkedList<String>();
			blacklist.add("eNodeB_common_id1");
			blacklist.add("eNodeB_common_id2");
			Map<String, String> duration = new HashMap<String, String>();
			duration.put("value", "10");
			duration.put("units", "minute");
			Constraint cons = new Constraint(5, duration, time_in_range, blacklist);
			builder = builder.addLimitConstraint(policy1.id, cons);
			//
			// Add a duplicate constraint to policy1
			//
			builder = builder.addLimitConstraint(policy1.id, cons);
			//
			// Assert there are duplicate constraints associated with the only guard policy
			//
			results = builder.buildSpecification();
			boolean duplicate_constraint = false;
			for (Message m : results.getMessages()) {
				if (m.getMessage().equals("Guard policy guardpolicy1 has duplicate limit constraints") && m.getLevel() == MessageLevel.WARNING) {
					duplicate_constraint = true;
					break;
				}
			}
			assertTrue(duplicate_constraint);
			//
			// Remove the duplicate constraint
			//
			builder = builder.removeLimitConstraint(policy1.id, cons);
			//
			// Add a duplicate guard policy 
			//
			builder = builder.addGuardPolicy(policy1);
			builder = builder.addLimitConstraint(policy1.id, cons);
			//
			// Assert there are duplicate guard policies
			//
			results = builder.buildSpecification();
			boolean duplicate_guard_policy = false;
			for (Message m : results.getMessages()) {
				if (m.getMessage().equals("There are duplicate guard policies") && m.getLevel() == MessageLevel.WARNING) {
					duplicate_guard_policy = true;
					break;
				}
			}
			assertTrue(duplicate_guard_policy);
			//
			// Remove the duplicate guard policy
			//
			builder = builder.removeGuardPolicy(policy1);
			//
			// Assert there are no Error/Warning message
			//
			results = builder.buildSpecification();
			assertTrue(results.getMessages().size() == 1);
			//
		} catch (BuilderException e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void test1() {
		this.test("src/test/resources/v2.0.0-guard/policy_guard_vUSP_1707_appc.yaml");
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
			ControlLoopGuard guardTobuild = (ControlLoopGuard) obj;
			//
			// Now we're going to try to use the builder to build this.
			//
			ControlLoopGuardBuilder builder = ControlLoopGuardBuilder.Factory.buildControlLoopGuard(guardTobuild.guard);
			//
			// Add guard policy
			//
			if (guardTobuild.guards != null) {
				builder = builder.addGuardPolicy(guardTobuild.guards.toArray(new GuardPolicy[guardTobuild.guards.size()]));
			}
			//
			// Build the specification
			//
			Results results = builder.buildSpecification();
			//
			// Print out the specification
			//
			System.out.println(results.getSpecification());
			//
		} catch (FileNotFoundException e) {
			fail(e.getLocalizedMessage());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		} catch (BuilderException e) {
			fail(e.getLocalizedMessage());
		}
	}
}
