/*-
 * ============LICENSE_START=======================================================
 * demo
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

package org.onap.policy.template.demo;

import static org.junit.Assert.fail;

import com.att.research.xacml.util.XACMLProperties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.Results;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.onap.policy.common.endpoints.http.server.HttpServletServer;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.controlloop.policy.guard.ControlLoopGuard;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.guard.PolicyGuardYamlToXacml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;


public final class Util {

    private static final String OPSHISTPUPROP = "OperationsHistoryPU";
    private static final Logger logger = LoggerFactory.getLogger(Util.class);

    public static class Pair<A, B> {
        public final A first;
        public final B second;

        public Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }
    }

    // values from the last call to buildConter()

    private static KieServices kieServices;
    private static KieContainer keyContainer;

    /**
     * Load YAML.
     *
     * @param testFile test file to load
     * @return the Pair of a policy and the yaml contents
     */
    public static Pair<ControlLoopPolicy, String> loadYaml(String testFile) {
        try (InputStream is = new FileInputStream(new File(testFile))) {
            String contents = IOUtils.toString(is, StandardCharsets.UTF_8);
            //
            // Read the yaml into our Java Object
            //
            Yaml yaml = new Yaml(new Constructor(ControlLoopPolicy.class));
            Object obj = yaml.load(contents);

            logger.debug(contents);

            return new Pair<ControlLoopPolicy, String>((ControlLoopPolicy) obj, contents);
        } catch (FileNotFoundException e) {
            fail(e.getLocalizedMessage());
        } catch (IOException e) {
            fail(e.getLocalizedMessage());
        }
        return null;
    }

    /**
     * Load the YAML guard policy.
     *
     * @param testFile the test file to load
     * @return return the guard object
     */
    public static ControlLoopGuard loadYamlGuard(String testFile) {
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

    public static HttpServletServer buildAaiSim() throws InterruptedException, IOException {
        return org.onap.policy.simulators.Util.buildAaiSim();
    }

    public static HttpServletServer buildSoSim() throws InterruptedException, IOException {
        return org.onap.policy.simulators.Util.buildSoSim();
    }

    public static HttpServletServer buildVfcSim() throws InterruptedException, IOException {
        return org.onap.policy.simulators.Util.buildVfcSim();
    }

    public static HttpServletServer buildGuardSim() throws InterruptedException, IOException {
        return org.onap.policy.simulators.Util.buildGuardSim();
    }

    public static HttpServletServer buildSdncSim() throws InterruptedException, IOException {
        return org.onap.policy.simulators.Util.buildSdncSim();
    }

    /**
     * Build a container containing a single set of rules.
     *
     * @param droolsTemplate template
     * @param closedLoopControlName control loop id
     * @param policyScope policy scope
     * @param policyName policy name
     * @param policyVersion policy version
     * @param yamlSpecification incoming yaml specification
     * @return the Kie session
     * @throws IOException if the container cannot be built
     */
    public static KieSession buildContainer(String droolsTemplate, String closedLoopControlName, String policyScope,
                    String policyName, String policyVersion, String yamlSpecification) throws IOException {

        RuleSpec spec = new RuleSpec(droolsTemplate, closedLoopControlName, policyScope, policyName, policyVersion,
                        yamlSpecification);

        return buildContainer(policyVersion, new RuleSpec[] {spec});
    }

    /**
     * Build a container containing all of the specified rules.
     *
     * @param policyVersion policy version
     * @param specifications rule specifications
     * @return the Kie session
     * @throws IOException if the container cannot be built
     */
    public static KieSession buildContainer(String policyVersion, RuleSpec[] specifications) throws IOException {
        //
        // Get our Drools Kie factory
        //
        kieServices = KieServices.Factory.get();

        ReleaseId releaseId = buildPolicy(policyVersion, specifications);
        logger.debug(releaseId.toString());

        //
        // Create our kie Session and container
        //
        keyContainer = kieServices.newKieContainer(releaseId);

        return setupSession(keyContainer.newKieSession());
    }

    /**
     * Update the container with new rules.
     *
     * @param policyVersion new policy version
     * @param specifications new rule specifications
     * @throws IOException if the container cannot be built
     */
    public static void updateContainer(String policyVersion, RuleSpec[] specifications) throws IOException {
        ReleaseId releaseId = buildPolicy(policyVersion, specifications);
        logger.debug(releaseId.toString());

        keyContainer.updateToVersion(releaseId);
    }

    /**
     * Build the Policy so it can be loaded into a KIE container.
     *
     * @param policyVersion policy version
     * @param specifications rule specifications
     * @return the release
     * @throws IOException if the container cannot be built
     */
    private static ReleaseId buildPolicy(String policyVersion, RuleSpec[] specifications) throws IOException {
        //
        // Generate our drools rule from our template
        //
        KieFileSystem kfs = kieServices.newKieFileSystem();
        ReleaseId releaseId = kieServices.getRepository().getDefaultReleaseId();
        releaseId = kieServices.newReleaseId(releaseId.getGroupId(), releaseId.getArtifactId(), policyVersion);

        kfs.generateAndWritePomXML(releaseId);

        for (RuleSpec spec : specifications) {
            String drlContents = spec.generateRules();
            kfs.write("src/main/resources/" + spec.policyName + ".drl",
                            kieServices.getResources().newByteArrayResource(drlContents.getBytes()));
        }

        //
        // Compile the rule
        //
        KieBuilder builder = kieServices.newKieBuilder(kfs).buildAll();
        Results results = builder.getResults();
        if (results.hasMessages(Message.Level.ERROR)) {
            for (Message msg : results.getMessages()) {
                logger.error(msg.toString());
            }
            throw new RuntimeException("Drools Rule has Errors");
        }
        for (Message msg : results.getMessages()) {
            logger.debug(msg.toString());
        }

        return releaseId;
    }

    private static KieSession setupSession(KieSession kieSession) {


        //
        // Create XACML Guard policy from YAML
        // We prepare 4 Guards. Notice that Rebuilds recipe has two Guards (for checking policy combining algorithm)
        //
        PolicyGuardYamlToXacml.fromYamlToXacml("src/test/resources/yaml/policy_guard_appc_restart.yaml",
                "src/main/resources/frequency_limiter_template.xml",
                "src/test/resources/xacml/autogenerated_frequency_limiter_restart.xml");

        PolicyGuardYamlToXacml.fromYamlToXacml("src/test/resources/yaml/policy_guard_appc_rebuild.yaml",
                "src/main/resources/frequency_limiter_template.xml",
                "src/test/resources/xacml/autogenerated_frequency_limiter_rebuild.xml");

        PolicyGuardYamlToXacml.fromYamlToXacml("src/test/resources/yaml/policy_guard_appc_rebuild_1.yaml",
                "src/main/resources/frequency_limiter_template.xml",
                "src/test/resources/xacml/autogenerated_frequency_limiter_rebuild_1.xml");

        PolicyGuardYamlToXacml.fromYamlToXacml("src/test/resources/yaml/policy_guard_appc_migrate.yaml",
                "src/main/resources/frequency_limiter_template.xml",
                "src/test/resources/xacml/autogenerated_frequency_limiter_migrate.xml");

        PolicyGuardYamlToXacml.fromYamlToXacml("src/test/resources/yaml/policy_guard_appc_modifyconfig.yaml",
                "src/main/resources/frequency_limiter_template.xml",
                "src/test/resources/xacml/autogenerated_frequency_limiter_modifyconfig.xml");

        PolicyGuardYamlToXacml.fromYamlToXacmlBlacklist(
                "src/test/resources/yaml/policy_guard_appc_restart_blacklist.yaml",
                "src/main/resources/blacklist_template.xml",
                "src/test/resources/xacml/autogenerated_blacklist.xml");

        //
        // Creating an embedded XACML PDP
        //
        System.setProperty(XACMLProperties.XACML_PROPERTIES_NAME, "src/test/resources/xacml/xacml_guard.properties");

        return kieSession;
    }

    /**
     *  Set the A&AI properties.
     */
    public static void setAaiProps() {
        PolicyEngine.manager.setEnvironmentProperty("aai.url", "http://localhost:6666");
        PolicyEngine.manager.setEnvironmentProperty("aai.username", "AAI");
        PolicyEngine.manager.setEnvironmentProperty("aai.password", "AAI");
    }

    /**
     *  Set the SO properties.
     */
    public static void setSoProps() {
        PolicyEngine.manager.setEnvironmentProperty("so.url", "http://localhost:6667");
        PolicyEngine.manager.setEnvironmentProperty("so.username", "SO");
        PolicyEngine.manager.setEnvironmentProperty("so.password", "SO");
    }

    /**
     *  Set the SDNC properties.
     */
    public static void setSdncProps() {
        PolicyEngine.manager.setEnvironmentProperty("sdnc.url", "http://localhost:6670/restconf/operations");
        PolicyEngine.manager.setEnvironmentProperty("sdnc.username", "sdnc");
        PolicyEngine.manager.setEnvironmentProperty("sdnc.password", "sdnc");
    }

    /**
     *  Set the Guard properties.
     */
    public static void setGuardProps() {
        /*
         * Guard PDP-x connection Properties
         */
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_URL,         "http://localhost:6669/pdp/api/getDecision");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_USER,        "python");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_PASS,        "test");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_CLIENT_USER, "python");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_CLIENT_PASS, "test");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_ENV,         "TEST");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_DISABLED,    "false");
    }

    /**
     *  Set the VFC properties.
     */
    public static void setVfcProps() {
        PolicyEngine.manager.setEnvironmentProperty("vfc.url", "http://localhost:6668");
        PolicyEngine.manager.setEnvironmentProperty("vfc.username", "VFC");
        PolicyEngine.manager.setEnvironmentProperty("vfc.password", "VFC");
    }

    /**
     *  Set the operation history properties.
     */
    public static void setPuProp() {
        System.setProperty(OPSHISTPUPROP, "TestOperationsHistoryPU");
    }

    /**
     * Rule specification.
     */
    public static class RuleSpec {
        private String droolsTemplate;
        private String closedLoopControlName;
        private String policyScope;
        private String policyName;
        private String policyVersion;
        private String yamlSpecification;

        /**
         * Constructs the object.
         *
         * @param droolsTemplate template
         * @param closedLoopControlName control loop id
         * @param policyScope policy scope
         * @param policyName policy name
         * @param policyVersion policy version
         * @param yamlSpecification incoming yaml specification
         */
        public RuleSpec(String droolsTemplate, String closedLoopControlName, String policyScope, String policyName,
                        String policyVersion, String yamlSpecification) {

            this.droolsTemplate = droolsTemplate;
            this.closedLoopControlName = closedLoopControlName;
            this.policyScope = policyScope;
            this.policyName = policyName;
            this.policyVersion = policyVersion;
            this.yamlSpecification = yamlSpecification;
        }

        /**
         * Generates the rules by reading the template and making variable substitutions.
         *
         * @return the rules
         * @throws IOException if an error occurs
         */
        private String generateRules() throws IOException {
            Path rule = Paths.get(droolsTemplate);
            String ruleTemplate = new String(Files.readAllBytes(rule));

            Pattern pattern = Pattern.compile("\\$\\{closedLoopControlName\\}");
            Matcher matcher = pattern.matcher(ruleTemplate);
            ruleTemplate = matcher.replaceAll(closedLoopControlName);

            pattern = Pattern.compile("\\$\\{policyScope\\}");
            matcher = pattern.matcher(ruleTemplate);
            ruleTemplate = matcher.replaceAll(policyScope);

            pattern = Pattern.compile("\\$\\{policyName\\}");
            matcher = pattern.matcher(ruleTemplate);
            ruleTemplate = matcher.replaceAll(policyName);

            pattern = Pattern.compile("\\$\\{policyVersion\\}");
            matcher = pattern.matcher(ruleTemplate);
            ruleTemplate = matcher.replaceAll(policyVersion);

            pattern = Pattern.compile("\\$\\{controlLoopYaml\\}");
            matcher = pattern.matcher(ruleTemplate);
            ruleTemplate = matcher.replaceAll(yamlSpecification);

            return ruleTemplate;
        }
    }
}
