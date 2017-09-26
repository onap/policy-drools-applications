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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.controlloop.policy.guard.ControlLoopGuard;
import org.onap.policy.drools.system.PolicyEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public final class Util {
	
	/*
	 * Keys for guard properties
	 */
	public static final String PROP_GUARD_URL         = "guard.url";
	public static final String PROP_GUARD_USER        = "pdpx.username";
	public static final String PROP_GUARD_PASS        = "pdpx.password";
	public static final String PROP_GUARD_CLIENT_USER = "pdpx.client.username";
	public static final String PROP_GUARD_CLIENT_PASS = "pdpx.client.password";
	public static final String PROP_GUARD_ENV         = "pdpx.environment";
	
	/*
	 * Guard responses
	 */
	public static final String INDTRMNT =  "Indeterminate";
	public static final String PERM = "Permit";
	public static final String DENY = "Deny";
	

	private static final Logger logger = LoggerFactory.getLogger(Util.class);
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
			
			logger.debug(contents);
			
			return new Pair<ControlLoopPolicy, String>((ControlLoopPolicy) obj, contents);
		} catch (IOException e) {
			logger.error(e.getLocalizedMessage(), e);
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
		} catch (IOException e) {
			logger.error(e.getLocalizedMessage(), e);
			fail(e.getLocalizedMessage());
		}
		return null;
	}
	
	/**
	 * Not implemented. Sets Guard Properties
	 * 
	 *  @see /guard/src/test/java/org/onap/policy/guard/UtilTest.java 
	 *  for setting test properties
	 */
	public static void setGuardProps(){
		// TODO: set guard properties here.
	}
	public static void setGuardUrl(String url){
		PolicyEngine.manager.setEnvironmentProperty(PROP_GUARD_URL, url);
	}
	public static String getGuardProp(String propName){
		return PolicyEngine.manager.getEnvironmentProperty(propName);
	}

}
