/*-
 * ============LICENSE_START=======================================================
 * guard
 * ================================================================================
 * Copyright (C) 2018 Ericsson. All rights reserved.
 * Modifications Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import org.junit.Test;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.controlloop.policy.guard.ControlLoopGuard;
import org.onap.policy.guard.Util.Pair;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class GuardUtilTest {
    @Test
    public void testLoadYamlOk() throws IOException {
        File tempYamlFile = File.createTempFile("ONAPPF", "yaml");
        tempYamlFile.deleteOnExit();

        ControlLoopPolicy clPolicy = new ControlLoopPolicy();

        Yaml clYaml = new Yaml(new Constructor(ControlLoopPolicy.class));
        String clYamlString = clYaml.dump(clPolicy);

        SupportTextFileUtils.putStringAsFile(clYamlString, tempYamlFile);

        Pair<ControlLoopPolicy, String> result = Util.loadYaml(tempYamlFile.getCanonicalPath());

        assertEquals(clPolicy, result.parameterA);
        assertEquals(clYamlString, result.parameterB);
    }

    @Test
    public void testLoadYamlError() throws IOException {
        File tempDir = Files.createTempDir();
        tempDir.deleteOnExit();

        // Read from a directory forces an IO exception
        assertNull(Util.loadYaml(tempDir.getCanonicalPath()));
    }

    @Test
    public void testLoadGuardYamlOk() throws IOException {
        File tempYamlFile = File.createTempFile("ONAPPF", "yaml");
        tempYamlFile.deleteOnExit();

        ControlLoopGuard clGuardPolicy = new ControlLoopGuard();

        Yaml clYaml = new Yaml(new Constructor(ControlLoopPolicy.class));
        String clYamlString = clYaml.dump(clGuardPolicy);

        SupportTextFileUtils.putStringAsFile(clYamlString, tempYamlFile);

        ControlLoopGuard result = Util.loadYamlGuard(tempYamlFile.getCanonicalPath());

        assertEquals(clGuardPolicy, result);
    }

    @Test
    public void testLoadGuardYamlError() throws IOException {
        File tempDir = Files.createTempDir();
        tempDir.deleteOnExit();

        // Read from a directory forces an IO exception
        assertNull(Util.loadYamlGuard(tempDir.getCanonicalPath()));
    }

    @Test
    public void testMisc() {
        Util.setGuardEnvProp("Actor", "Judy Garland");
        assertEquals("Judy Garland", Util.getGuardProp("Actor"));

        Util.setGuardEnvProps("http://somewhere.over.the.rainbow", "Dorothy", "Toto");

        assertEquals("http://somewhere.over.the.rainbow", Util.getGuardProp(Util.PROP_GUARD_URL));
        assertEquals("Dorothy", Util.getGuardProp(Util.PROP_GUARD_USER));
        assertEquals("Toto", Util.getGuardProp(Util.PROP_GUARD_PASS));
    }
}
