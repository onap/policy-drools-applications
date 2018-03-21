/*-
 * ============LICENSE_START=======================================================
 * unit test
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
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
    public void testControlLoopProcessor() throws IOException, ControlLoopException {
        InputStream is = new FileInputStream(new File("src/test/resources/test.yaml"));
        String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);
        this.testSuccess(yamlString);
        this.testFailure(yamlString);
    }

    @Test
    public void testControlLoopProcessorBadYaml() throws IOException {
        InputStream is = new FileInputStream(new File("src/test/resources/string.yaml"));
        String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        try {
            new ControlLoopProcessor(yamlString);
            fail("test should thrown an exception");
        } catch (Exception e) {
            assertEquals("Cannot create property=string for JavaBean=ControlLoopPolicy",
                    e.getMessage().substring(0, 60));
        }
    }

    @Test
    public void testControlLoopProcessorBadTriggerYaml() throws IOException, ControlLoopException {
        InputStream is = new FileInputStream(new File("src/test/resources/badtriggerpolicy.yaml"));
        String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        ControlLoopProcessor clProcessor = new ControlLoopProcessor(yamlString);
        assertNull(clProcessor.getCurrentPolicy());

        try {
            clProcessor.nextPolicyForResult(PolicyResult.SUCCESS);
            fail("test shold throw an exception here");
        } catch (ControlLoopException e) {
            assertEquals("There is no current policy to determine where to go to.", e.getMessage());
        }
    }

    @Test
    public void testControlLoopProcessorNoPolicyYaml() throws IOException, ControlLoopException {
        InputStream is = new FileInputStream(new File("src/test/resources/nopolicy.yaml"));
        String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        ControlLoopProcessor clProcessor = new ControlLoopProcessor(yamlString);

        try {
            clProcessor.getCurrentPolicy();
            fail("test shold throw an exception here");
        } catch (ControlLoopException e) {
            assertEquals("There are no policies defined.", e.getMessage());
        }
    }

    @Test
    public void testControlLoopProcessorNextPolicyForResult() throws IOException, ControlLoopException {
        InputStream is = new FileInputStream(new File("src/test/resources/test.yaml"));
        String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        ControlLoopProcessor clProcessor = new ControlLoopProcessor(yamlString);
        clProcessor.getCurrentPolicy();
        clProcessor.nextPolicyForResult(PolicyResult.SUCCESS);

        clProcessor = new ControlLoopProcessor(yamlString);
        clProcessor.getCurrentPolicy();
        clProcessor.nextPolicyForResult(PolicyResult.FAILURE);

        clProcessor = new ControlLoopProcessor(yamlString);
        clProcessor.getCurrentPolicy();
        clProcessor.nextPolicyForResult(PolicyResult.FAILURE_EXCEPTION);

        clProcessor = new ControlLoopProcessor(yamlString);
        clProcessor.getCurrentPolicy();
        clProcessor.nextPolicyForResult(PolicyResult.FAILURE_GUARD);

        clProcessor = new ControlLoopProcessor(yamlString);
        clProcessor.getCurrentPolicy();
        clProcessor.nextPolicyForResult(PolicyResult.FAILURE_RETRIES);

        clProcessor = new ControlLoopProcessor(yamlString);
        clProcessor.getCurrentPolicy();
        clProcessor.nextPolicyForResult(PolicyResult.FAILURE_TIMEOUT);
    }

    /**
     * Test policies in the given yaml following the successfull path.
     * 
     * @param yaml yaml containing the policies to test
     * @throws ControlLoopException if an error occurs
     */
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

    /**
     * Test policies in the given yaml following the failure path.
     * 
     * @param yaml yaml containing the policies to test
     * @throws ControlLoopException if an error occurs
     */
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
