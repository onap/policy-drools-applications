/*-
 * ============LICENSE_START=======================================================
 * unit test
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

package org.onap.policy.controlloop.processor;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.policy.FinalResult;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlLoopProcessorTest {
	private static final Logger logger = LoggerFactory.getLogger(ControlLoopProcessorTest.class);
	
	@Test
	public void test() {
		try (InputStream is = new FileInputStream(new File("src/test/resources/test.yaml"))) {
			String result = IOUtils.toString(is, StandardCharsets.UTF_8);
			this.testSuccess(result);
			this.testFailure(result);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (ControlLoopException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testSuccess(String yaml) throws ControlLoopException {
		ControlLoopProcessor processor = new ControlLoopProcessor(yaml);
		logger.debug("testSuccess: {}", processor.getControlLoop());
		while (true) {
			FinalResult result = processor.checkIsCurrentPolicyFinal();
			if (result != null) {
				logger.debug("{}", result);
				break;
			}
			Policy policy = processor.getCurrentPolicy();
			assertNotNull(policy);
			logger.debug("current policy is: {} {}", policy.getId(), policy.getName());
			processor.nextPolicyForResult(PolicyResult.SUCCESS);
		}
	}

	public void testFailure(String yaml) throws ControlLoopException {
		ControlLoopProcessor processor = new ControlLoopProcessor(yaml);
		logger.debug("testFailure: {}", processor.getControlLoop());
		while (true) {
			FinalResult result = processor.checkIsCurrentPolicyFinal();
			if (result != null) {
				logger.debug("{}", result);
				break;
			}
			Policy policy = processor.getCurrentPolicy();
			assertNotNull(policy);
			logger.debug("current policy is: {} {}", policy.getId(), policy.getName());
			processor.nextPolicyForResult(PolicyResult.FAILURE);
		}		
	}

}
