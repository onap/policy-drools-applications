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
package org.onap.policy.controlloop.processor;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.controlloop.policy.guard.ControlLoopGuard;

public final class Util {

	public static class Pair<A, B> {
		public final A a;
		public final B b;
		
		public Pair(A a, B b) {
			this.a = a;
			this.b = b;
		}
	}
	
	public static Pair<ControlLoopPolicy, String>	loadYaml(String testFile) {
		try (InputStream is = new FileInputStream(new File(testFile))) {
			String contents = IOUtils.toString(is, StandardCharsets.UTF_8);
			//
			// Read the yaml into our Java Object
			//
			Yaml yaml = new Yaml(new Constructor(ControlLoopPolicy.class));
			Object obj = yaml.load(contents);
			
			//String ttt = ((ControlLoopPolicy)obj).policies.getFirst().payload.get("asdas");
			System.out.println(contents);
			//for(Policy policy : ((ControlLoopPolicy)obj).policies){
			
			return new Pair<ControlLoopPolicy, String>((ControlLoopPolicy) obj, contents);
		} catch (FileNotFoundException e) {
			fail(e.getLocalizedMessage());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
		return null;
	}
	
	public static ControlLoopGuard	loadYamlGuard(String testFile) {
		try (InputStream is = new FileInputStream(new File(testFile))) {
			String contents = IOUtils.toString(is, StandardCharsets.UTF_8);
			//
			// Read the yaml into our Java Object
			//
			Yaml yaml = new Yaml(new Constructor(ControlLoopGuard.class));
			Object obj = yaml.load(contents);
			return (ControlLoopGuard) obj;
		} catch (FileNotFoundException e) {
			fail(e.getLocalizedMessage());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
		return null;
	}

}
