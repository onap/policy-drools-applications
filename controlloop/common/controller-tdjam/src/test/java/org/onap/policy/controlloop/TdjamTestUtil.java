/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.controlloop;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class TdjamTestUtil {
    public static final String CONFIG_DIR = "src/test/resources/config";

    private static final File configDir = new File(CONFIG_DIR);
    private static final File configFile = new File(configDir, "tdjam-controller.properties");

    public static final String CONFIG_FILE = configFile.getPath();


    private TdjamTestUtil() {
        // do nothing
    }

    /**
     * Makes a config file.
     *
     * @throws IOException if the config file cannot be created
     */
    public static void makeConfigFile() throws IOException {
        File srcFile = new File(CONFIG_DIR, "tdjam-controller-original.properties");
        Files.copy(srcFile.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        configFile.deleteOnExit();
    }
}
