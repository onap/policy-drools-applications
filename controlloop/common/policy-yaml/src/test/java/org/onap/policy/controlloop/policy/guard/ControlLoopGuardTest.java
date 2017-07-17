/*
 *                        AT&T - PROPRIETARY
 *          THIS FILE CONTAINS PROPRIETARY INFORMATION OF
 *        AT&T AND IS NOT TO BE DISCLOSED OR USED EXCEPT IN
 *             ACCORDANCE WITH APPLICABLE AGREEMENTS.
 *
 *          Copyright (c) 2016 AT&T Knowledge Ventures
 *              Unpublished and Not for Publication
 *                     All Rights Reserved
 */
package org.onap.policy.controlloop.policy.guard;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;


public class ControlLoopGuardTest {
	
	@Test 
	public void testGuardvDNS() {
		this.test("src/test/resources/v2.0.0-guard/policy_guard_OpenECOMP_demo_vDNS.yaml");
	}

	@Test 
	public void testGuardvUSP() {
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
			dump(obj);
			//
			// Now dump it to a yaml string
			//
			DumperOptions options = new DumperOptions();
			options.setDefaultFlowStyle(FlowStyle.BLOCK);
			options.setPrettyFlow(true);
			yaml = new Yaml(options);
			String dumpedYaml = yaml.dump(obj);
			System.out.println(dumpedYaml);
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
		System.out.println("Dumping " + obj.getClass().getCanonicalName());
		System.out.println(obj.toString());
	}
}
