/*-
 * ============LICENSE_START=======================================================
 * demo
 * ================================================================================
 * Copyright (C) 2018-2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.template.demo.clc;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
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
import org.onap.policy.controlloop.policy.guard.ControlLoopGuard;
import org.onap.policy.coordination.CoordinationDirective;
import org.onap.policy.drools.system.PolicyEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;


public final class SupportUtil {

    private static final String OPSHISTPUPROP = "OperationsHistoryPU";
    private static final Logger logger = LoggerFactory.getLogger(SupportUtil.class);

    // values from the last call to buildContainer()

    private static KieServices kieServices;
    private static KieContainer keyContainer;

    public static class Pair<A, B> {
        public final A first;
        public final B second;

        public Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }
    }

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
        } catch (IOException e) {
            logger.error("Error while loading YAML", e);
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
        } catch (IOException e) {
            logger.error("Error while loading YAML guard", e);
            fail(e.getLocalizedMessage());
        }
        return null;
    }

    /**
     * Insert the Xacml policy into the PDP.
     * Achieved by configuring the properties file to load the Xacml policy and required PIP(s).
     *
     * @param xacmlFile the Xacml policy file's path
     * @param propProtoDir the directory containing Xacml implementation prototypes
     * @param propDir the directory to which the Xacml rule should be output
     */
    public static void insertXacmlPolicy(String xacmlFile,
                                         String propProtoDir,
                                         String propDir) {
        String propName = "xacml_guard_clc";
        String propProtoFile = propProtoDir + File.separator + propName + ".properties";
        String propFilename = propDir + File.separator + propName + ".properties";

        String addXacmlFileToRoot = "# Policies to load\n"
            + "xacml.rootPolicies=p1\n"
            + "p1.file=" + xacmlFile + "\n";

        File propFile = new File(propFilename);
        try (Stream<String> stream = Files.lines(Paths.get(propProtoFile));
             PrintWriter output = new PrintWriter(propFile)) {
            /*
             * Remove file after test
             */
            propFile.deleteOnExit();
            /*
             * Copy the property prototype
             */
            stream.forEach(output::println);
            /*
             * Add the Xacml policy to the set of root policies
             */
            output.println(addXacmlFileToRoot);
            /*
             * Obtain PIP Engine definitions from Xacml policy
             * and insert into property file.
             */
            try (BufferedReader br = new BufferedReader(new FileReader(xacmlFile))) {
                boolean select = false;
                for (String line; (line = br.readLine()) != null; ) {
                    if (line.contains("PIP Engine Definition")) {
                        select = true;
                    }
                    if (line.contains("-->")) {
                        select = false;
                    }
                    if (select) {
                        output.println(line);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error when trying to create test propery file", e);
            fail(e.getMessage());
        }
    }

    public static HttpServletServer buildAaiSim() throws InterruptedException, IOException {
        return org.onap.policy.simulators.Util.buildAaiSim();
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

        return keyContainer.newKieSession();
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

    /**
     *  Set the A&AI properties.
     */
    public static void setAaiProps() {
        PolicyEngine.manager.setEnvironmentProperty("aai.url", "http://localhost:6666");
        PolicyEngine.manager.setEnvironmentProperty("aai.username", "AAI");
        PolicyEngine.manager.setEnvironmentProperty("aai.password", "AAI");
    }

    /**
     *  Set the Guard properties to use embedded XACML PDPEngine.
     */
    public static void setGuardPropsEmbedded() {
        /*
         * Guard PDP-x connection Properties. No URL specified -> use embedded PDPEngine.
         */
        PolicyEngine.manager.setEnvironmentProperty("prop.guard.propfile",
                                                    "src/test/resources/properties/xacml_guard_clc.properties");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_USER,        "python");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_PASS,        "test");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_CLIENT_USER, "python");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_CLIENT_PASS, "test");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_ENV,         "TEST");
        PolicyEngine.manager.setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_DISABLED,    "false");
    }

    /**
     *  Set the operation history properties.
     */
    public static void setPuProp() {
        System.setProperty(OPSHISTPUPROP, "TestOperationsHistoryPU");
    }

    /**
     * Dump the contents of the History database.
     *
     * @return a list of the database entries
     */
    public static List<?> dumpDb() {
        //
        // Connect to in-mem db
        //
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("TestOperationsHistoryPU");
        EntityManager em = emf.createEntityManager();
        //
        // Create query
        //
        String sql = "select * from operationshistory10";
        Query nq = em.createNativeQuery(sql);
        List<?> results = null;
        //
        // Execute query
        //
        try {
            results = nq.getResultList();
        } catch (Exception ex) {
            logger.error("getStatusFromDB threw: ", ex);
            //
            // Clean up and return null
            //
            em.close();
            emf.close();
            return null;
        }
        //
        // Clean up and return results
        //
        em.close();
        emf.close();
        return results;
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
