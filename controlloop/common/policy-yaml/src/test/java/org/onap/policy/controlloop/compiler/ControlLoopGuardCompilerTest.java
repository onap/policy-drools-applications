package org.onap.policy.controlloop.compiler;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import org.onap.policy.controlloop.guard.compiler.ControlLoopGuardCompiler;

public class ControlLoopGuardCompilerTest {

	@Test 
	public void testTest1() {
		try {
			this.test("src/test/resources/v2.0.0-guard/policy_guard_OpenECOMP_demo_vDNS.yaml");
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test 
	public void testTest2() {
		try {
			this.test("src/test/resources/v2.0.0-guard/policy_guard_vUSP_1707_appc.yaml");
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test 
	public void testBad1() {
		try {
			this.test("src/test/resources/v2.0.0-guard/no_guard_policy.yaml");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test 
	public void testBad2() {
		try {
			this.test("src/test/resources/v2.0.0-guard/duplicate_guard_policy.yaml");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test 
	public void testBad3() {
		try {
			this.test("src/test/resources/v2.0.0-guard/no_guard_constraint.yaml");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test 
	public void testBad4() {
		try {
			this.test("src/test/resources/v2.0.0-guard/duplicate_guard_constraint.yaml");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void test(String testFile) throws Exception {
		try (InputStream is = new FileInputStream(new File(testFile))) {
			ControlLoopGuardCompiler.compile(is, null);
		} catch (FileNotFoundException e) {
			fail(e.getMessage());
		} catch (IOException e) {
			fail(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
}
