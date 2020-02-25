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
import java.nio.file.Paths;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.policy.FinalResult;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlLoopProcessorTest {
    private static final Logger logger = LoggerFactory.getLogger(ControlLoopProcessorTest.class);
    private static final StandardCoder coder = new StandardCoder();

    @Test
    public void testControlLoopProcessor() throws IOException, ControlLoopException {
        InputStream is = new FileInputStream(new File("src/test/resources/test.yaml"));
        String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);
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
    public void testControlLoopFromToscaLegacy() throws IOException, CoderException, ControlLoopException {
        String policy =
                new String(Files.readAllBytes(Paths.get("src/test/resources/tosca-policy-legacy-vcpe.json")));
        assertNotNull(
                new ControlLoopProcessor(coder.decode(policy, ToscaPolicy.class)).getCurrentPolicy());

        policy =
                new String(Files.readAllBytes(Paths.get("src/test/resources/tosca-policy-legacy-vdns.json")));
        assertNotNull(
                new ControlLoopProcessor(coder.decode(policy, ToscaPolicy.class)).getCurrentPolicy());
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
        toscaPolicy.setType("onap.policies.controlloop.Operational");
        assertThatThrownBy(() -> new ControlLoopProcessor(toscaPolicy)).hasCauseInstanceOf(CoderException.class);
    }

    @Test
    public void testControlLoopProcessorBadYaml() throws IOException {
        InputStream is = new FileInputStream(new File("src/test/resources/string.yaml"));
        String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        assertThatThrownBy(() -> new ControlLoopProcessor(yamlString))
            .hasMessageStartingWith("Cannot create property=string for JavaBean=ControlLoopPolicy");
    }

    @Test
    public void testControlLoopProcessorBadTriggerYaml() throws IOException, ControlLoopException {
        InputStream is = new FileInputStream(new File("src/test/resources/badtriggerpolicy.yaml"));
        String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        ControlLoopProcessor clProcessor = new ControlLoopProcessor(yamlString);
        assertNull(clProcessor.getCurrentPolicy());

        assertThatThrownBy(() -> clProcessor.nextPolicyForResult(PolicyResult.SUCCESS))
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
     * Test policies in the given yaml following the successful path.
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
