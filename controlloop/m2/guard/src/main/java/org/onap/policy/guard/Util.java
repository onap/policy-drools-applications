/*-
 * ============LICENSE_START=======================================================
 * guard
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

package org.onap.policy.guard;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.controlloop.policy.guard.ControlLoopGuard;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public final class Util {
    /*
     * Keys for guard properties
     */
    public static final String PROP_GUARD_URL = "guard.url";
    public static final String PROP_GUARD_USER = "pdpx.username";
    public static final String PROP_GUARD_PASS = "pdpx.password";
    public static final String PROP_GUARD_DISABLED = "guard.disabled";
    public static final String PROP_GUARD_PERSISTENCE_UNIT = "guard.persistenceUnit";

    /*
     * Keys for ONAP properties
     */
    public static final String ONAP_KEY_URL = "guard.jdbc.url";
    public static final String ONAP_KEY_USER = "sql.db.username";
    public static final String ONAP_KEY_PASS = "sql.db.password";

    /*
     * Guard responses
     */
    public static final String INDETERMINATE = "Indeterminate";
    public static final String PERMIT = "Permit";
    public static final String DENY = "Deny";

    /*
     * Junit props
     */
    protected static final String PU_KEY = "OperationsHistoryPU";
    protected static final String JUNITPU = "OperationsHistoryPUTest";

    private static final Logger logger = LoggerFactory.getLogger(Util.class);

    public static class Pair<A, B> {
        public final A parameterA;
        public final B parameterB;

        public Pair(A parameterA, B parameterB) {
            this.parameterA = parameterA;
            this.parameterB = parameterB;
        }
    }

    private Util() {
        // This static class cannot be instantiated
    }

    /**
     * Load a Yaml file.
     *
     * @param testFile the Yaml file
     * @return the policies
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

            return new Pair<>((ControlLoopPolicy) obj, contents);
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage(), e);
        }
        return null;
    }

    /**
     * Load a Yaml guard.
     *
     * @param testFile the Yaml file
     * @return the guard
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
            logger.error(e.getLocalizedMessage(), e);
        }
        return null;
    }

    /**
     * Sets Guard Properties.
     *
     * <p>see /guard/src/test/java/org/onap/policy/guard/UtilTest.java for setting test properties
     */
    public static void setGuardEnvProps(String url, String username, String password) {
        PolicyEngineConstants.getManager().setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_URL, url);
        PolicyEngineConstants.getManager().setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_USER, username);
        PolicyEngineConstants.getManager().setEnvironmentProperty(org.onap.policy.guard.Util.PROP_GUARD_PASS, password);
    }

    public static void setGuardEnvProp(String key, String value) {
        PolicyEngineConstants.getManager().setEnvironmentProperty(key, value);
    }

    public static String getGuardProp(String propName) {
        return PolicyEngineConstants.getManager().getEnvironmentProperty(propName);
    }
}
