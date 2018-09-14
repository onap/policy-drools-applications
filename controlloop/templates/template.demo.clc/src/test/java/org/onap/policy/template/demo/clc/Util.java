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

package org.onap.policy.template.demo.clc;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.kie.api.builder.model.KieModuleModel;
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

    private static String generatePolicy(String ruleContents, 
            String closedLoopControlName, 
            String policyScope, 
            String policyName, 
            String policyVersion, 
            String controlLoopYaml) {

        Pattern pattern = Pattern.compile("\\$\\{closedLoopControlName\\}");
        Matcher matcher = pattern.matcher(ruleContents);
        ruleContents = matcher.replaceAll(closedLoopControlName);

        pattern = Pattern.compile("\\$\\{policyScope\\}");
        matcher = pattern.matcher(ruleContents);
        ruleContents = matcher.replaceAll(policyScope);

        pattern = Pattern.compile("\\$\\{policyName\\}");
        matcher = pattern.matcher(ruleContents);
        ruleContents = matcher.replaceAll(policyName);

        pattern = Pattern.compile("\\$\\{policyVersion\\}");
        matcher = pattern.matcher(ruleContents);
        ruleContents = matcher.replaceAll(policyVersion);

        pattern = Pattern.compile("\\$\\{controlLoopYaml\\}");
        matcher = pattern.matcher(ruleContents);
        ruleContents = matcher.replaceAll(controlLoopYaml);

        return ruleContents;
    }

    /**
     * Build the container.
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
    public static KieSession buildContainer(String droolsTemplate, String closedLoopControlName, 
            String policyScope, String policyName, String policyVersion, 
            String yamlSpecification) throws IOException {
        //
        // Get our Drools Kie factory
        //
        KieServices ks = KieServices.Factory.get();

        KieModuleModel kieModule = ks.newKieModuleModel();

        logger.debug("KMODULE:" + System.lineSeparator() + kieModule.toXML());

        //
        // Generate our drools rule from our template
        //
        KieFileSystem kfs = ks.newKieFileSystem();

        kfs.writeKModuleXML(kieModule.toXML());
        {
            Path rule = Paths.get(droolsTemplate);
            String ruleTemplate = new String(Files.readAllBytes(rule));
            String drlContents = generatePolicy(ruleTemplate,
                    closedLoopControlName,
                    policyScope,
                    policyName,
                    policyVersion,
                    yamlSpecification);

            kfs.write("src/main/resources/" + policyName + ".drl", 
                    ks.getResources().newByteArrayResource(drlContents.getBytes()));
        }
        //
        // Compile the rule
        //
        KieBuilder builder = ks.newKieBuilder(kfs).buildAll();
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
        //
        // Create our kie Session and container
        //
        ReleaseId releaseId = ks.getRepository().getDefaultReleaseId();
        logger.debug(releaseId.toString());
        KieContainer keyContainer = ks.newKieContainer(releaseId);

        return keyContainer.newKieSession();
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
                                                    "src/test/resources/xacml/xacml_guard_clc.properties");
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
    public static List dumpDb() {
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
        List results = null;
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
}
