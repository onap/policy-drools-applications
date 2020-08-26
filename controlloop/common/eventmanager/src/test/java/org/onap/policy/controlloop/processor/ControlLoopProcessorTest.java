/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2017-2020 AT&T Intellectual Property. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.actorserviceprovider.OperationFinalResult;
import org.onap.policy.controlloop.actorserviceprovider.OperationResult;
import org.onap.policy.drools.domain.models.operational.Operation;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlLoopProcessorTest {
    private static final Logger logger = LoggerFactory.getLogger(ControlLoopProcessorTest.class);
    private static final StandardCoder coder = new StandardCoder();

    @Test
    public void testControlLoopProcessor() throws IOException, ControlLoopException {
        String yamlString = Files.readString(new File("src/test/resources/test.yaml").toPath(), StandardCharsets.UTF_8);
        this.testSuccess(yamlString);
        this.testFailure(yamlString);
    }

    private ToscaPolicy getPolicyFromResource(String resourcePath, String policyName) throws CoderException {
        String policyJson = ResourceUtils.getResourceAsString(resourcePath);
        ToscaServiceTemplate serviceTemplate = coder.decode(policyJson, ToscaServiceTemplate.class);
        ToscaPolicy policy = serviceTemplate.getToscaTopologyTemplate().getPolicies().get(0).get(policyName);
        assertNotNull(policy);

        /*
         * name and version are used within a drl.  api component and drools core will ensure that these
         * are populated.
         */
        if (StringUtils.isBlank(policy.getName())) {
            policy.setName(policyName);
        }

        if (StringUtils.isBlank(policy.getVersion())) {
            policy.setVersion(policy.getTypeVersion());
        }

        return serviceTemplate.getToscaTopologyTemplate().getPolicies().get(0).get(policyName);
    }

    @Test
    public void testControlLoopFromToscaCompliant()
            throws CoderException, ControlLoopException {
        assertNotNull(
                new ControlLoopProcessor(
                        getPolicyFromResource(
                                "policies/vCPE.policy.operational.input.tosca.json", "operational.restart")
                ).getCurrentPolicy());


        assertNotNull(
                new ControlLoopProcessor(
                        getPolicyFromResource(
                                "policies/vFirewall.policy.operational.input.tosca.json", "operational.modifyconfig")
                ).getCurrentPolicy());

        assertNotNull(
                new ControlLoopProcessor(
                        getPolicyFromResource(
                                "policies/vDNS.policy.operational.input.tosca.json", "operational.scaleout")
                ).getCurrentPolicy());
    }

    @Test
    public void testControlLoopFromToscaCompliantBad() throws CoderException {
        ToscaPolicy toscaPolicy = getPolicyFromResource(
                "policies/vCPE.policy.operational.input.tosca.json", "operational.restart");
        toscaPolicy.setVersion(null);
        assertThatThrownBy(() -> new ControlLoopProcessor(toscaPolicy)).hasCauseInstanceOf(CoderException.class);
    }

    @Test
    public void testControlLoopProcessorBadYaml() throws IOException {
        InputStream is = new FileInputStream(new File("src/test/resources/string.yaml"));
        String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        assertThatThrownBy(() -> new ControlLoopProcessor(yamlString))
            .hasMessageEndingWith("Cannot decode yaml into ToscaServiceTemplate");
    }

    @Test
    public void testControlLoopProcessorBadTriggerYaml() throws IOException, ControlLoopException {
        String yamlString = Files.readString(new File("src/test/resources/badtriggerpolicy.yaml").toPath(),
                        StandardCharsets.UTF_8);

        ControlLoopProcessor clProcessor = new ControlLoopProcessor(yamlString);
        assertNull(clProcessor.getCurrentPolicy());

        assertThatThrownBy(() -> clProcessor.nextPolicyForResult(OperationResult.SUCCESS))
            .hasMessageStartingWith("There is no current policy to determine where to go to.");
    }

    @Test
    public void testControlLoopProcessorNoPolicyYaml() throws IOException, ControlLoopException {
        InputStream is = new FileInputStream(new File("src/test/resources/nopolicy.yaml"));
        String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        ControlLoopProcessor clProcessor = new ControlLoopProcessor(yamlString);
        assertThatThrownBy(clProcessor::getCurrentPolicy)
            .hasMessage("There are no policies defined.");
    }

    @Test
    public void testControlLoopProcessorNextPolicyForResult() throws IOException, ControlLoopException {
        InputStream is = new FileInputStream(new File("src/test/resources/test.yaml"));
        String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        for (OperationResult result : OperationResult.values()) {
            checkResult(yamlString, result);
        }
    }

    private void checkResult(String yamlString, OperationResult result) throws ControlLoopException {
        ControlLoopProcessor clProcessor = new ControlLoopProcessor(yamlString);
        clProcessor.getCurrentPolicy();
        clProcessor.nextPolicyForResult(result);
    }

    /**
     * Test policies in the given yaml following the successful path.
     *
     * @param yaml yaml containing the policies to test
     * @throws ControlLoopException if an error occurs
     */
    public void testSuccess(String yaml) throws ControlLoopException {
        ControlLoopProcessor processor = new ControlLoopProcessor(yaml);
        logger.debug("testSuccess: {}", processor.getCurrentPolicy());
        while (true) {
            OperationFinalResult result = processor.checkIsCurrentPolicyFinal();
            if (result != null) {
                logger.debug("{}", result);
                break;
            }
            Operation policy = processor.getCurrentPolicy();
            assertNotNull(policy);
            logger.debug("current policy is: {}", policy.getId());
            processor.nextPolicyForResult(OperationResult.SUCCESS);
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
        logger.debug("testFailure: {}", processor.getCurrentPolicy());
        while (true) {
            OperationFinalResult result = processor.checkIsCurrentPolicyFinal();
            if (result != null) {
                logger.debug("{}", result);
                break;
            }
            Operation policy = processor.getCurrentPolicy();
            assertNotNull(policy);
            logger.debug("current policy is: {}", policy.getId());
            processor.nextPolicyForResult(OperationResult.FAILURE);
        }
    }
}
