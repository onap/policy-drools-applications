/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2017-2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023 Nordix Foundation.
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.actorserviceprovider.OperationResult;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ControlLoopProcessorTest {
    private static final Logger logger = LoggerFactory.getLogger(ControlLoopProcessorTest.class);
    private static final StandardCoder coder = new StandardCoder();

    @Test
    void testControlLoopProcessor() throws IOException, ControlLoopException {
        var yamlString = Files.readString(new File("src/test/resources/test.yaml").toPath(), StandardCharsets.UTF_8);
        this.testSuccess(yamlString);
        this.testFailure(yamlString);
    }

    private ToscaPolicy getPolicyFromResource(String resourcePath, String policyName) throws CoderException {
        var policyJson = ResourceUtils.getResourceAsString(resourcePath);
        var serviceTemplate = coder.decode(policyJson, ToscaServiceTemplate.class);
        var policy = serviceTemplate.getToscaTopologyTemplate().getPolicies().get(0).get(policyName);
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
    void testControlLoopFromToscaCompliant()
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
    void testControlLoopFromToscaCompliantBad() throws CoderException {
        var toscaPolicy = getPolicyFromResource(
                "policies/vCPE.policy.operational.input.tosca.json", "operational.restart");
        toscaPolicy.setVersion(null);
        assertThatThrownBy(() -> new ControlLoopProcessor(toscaPolicy)).hasCauseInstanceOf(CoderException.class);
    }

    @Test
    void testControlLoopProcessorBadYaml() throws IOException {
        var is = new FileInputStream(new File("src/test/resources/string.yaml"));
        var yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        assertThatThrownBy(() -> new ControlLoopProcessor(yamlString))
            .hasMessageEndingWith("Cannot decode yaml into ToscaServiceTemplate");
    }

    @Test
    void testControlLoopProcessorBadTriggerYaml() throws IOException, ControlLoopException {
        var yamlString = Files.readString(new File("src/test/resources/badtriggerpolicy.yaml").toPath(),
                        StandardCharsets.UTF_8);

        var clProcessor = new ControlLoopProcessor(yamlString);
        assertNull(clProcessor.getCurrentPolicy());

        assertThatThrownBy(() -> clProcessor.nextPolicyForResult(OperationResult.SUCCESS))
            .hasMessageStartingWith("There is no current policy to determine where to go to.");
    }

    @Test
    void testControlLoopProcessorNoPolicyYaml() throws IOException, ControlLoopException {
        var is = new FileInputStream(new File("src/test/resources/nopolicy.yaml"));
        var yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        var clProcessor = new ControlLoopProcessor(yamlString);
        assertThatThrownBy(clProcessor::getCurrentPolicy)
            .hasMessage("There are no policies defined.");
    }

    @Test
    void testControlLoopProcessorNextPolicyForResult() throws IOException, ControlLoopException {
        var is = new FileInputStream(new File("src/test/resources/test.yaml"));
        var yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        for (var result : OperationResult.values()) {
            checkResult(yamlString, result);
        }
    }

    private void checkResult(String yamlString, OperationResult result) throws ControlLoopException {
        var clProcessor = new ControlLoopProcessor(yamlString);
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
        var processor = new ControlLoopProcessor(yaml);
        logger.debug("testSuccess: {}", processor.getCurrentPolicy());
        while (true) {
            var result = processor.checkIsCurrentPolicyFinal();
            if (result != null) {
                logger.debug("{}", result);
                break;
            }
            var policy = processor.getCurrentPolicy();
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
        var processor = new ControlLoopProcessor(yaml);
        logger.debug("testFailure: {}", processor.getCurrentPolicy());
        while (true) {
            var result = processor.checkIsCurrentPolicyFinal();
            if (result != null) {
                logger.debug("{}", result);
                break;
            }
            var policy = processor.getCurrentPolicy();
            assertNotNull(policy);
            logger.debug("current policy is: {}", policy.getId());
            processor.nextPolicyForResult(OperationResult.FAILURE);
        }
    }
}
