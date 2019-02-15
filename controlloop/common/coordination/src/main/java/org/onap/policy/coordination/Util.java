/*-
 * ============LICENSE_START=======================================================
 * controlloop
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.coordination;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;


public final class Util {

    private static final Logger logger = LoggerFactory.getLogger(Util.class);

    /**
     * Load YAML coordination directive.
     *
     * @param directiveFilename yaml directive file to load
     * @return the CoordinationDirective
     */
    public static CoordinationDirective loadCoordinationDirectiveFromFile(String directiveFilename) {
        try (InputStream is = new FileInputStream(new File(directiveFilename))) {
            String contents = IOUtils.toString(is, StandardCharsets.UTF_8);
            //
            // Read the yaml into our Java Object
            //
            Yaml yaml = new Yaml(new Constructor(CoordinationDirective.class));
            Object obj = yaml.load(contents);

            logger.debug(contents);

            return (CoordinationDirective) obj;
        } catch (IOException e) {
            logger.error("Error while loading YAML coordination directive", e);
        }
        return null;
    }

    /**
     * Generate Xacml rule implementing specified CoordinationDirective.
     *
     * @param cd the CoordinationDirective
     * @param protoDir the directory containing Xacml implementation prototypes
     * @return the generated Xacml policy
     */
    public static String generateXacmlFromCoordinationDirective(CoordinationDirective cd,
                                                                String protoDir) {
        /*
         * Determine file names
         */
        String xacmlProtoFilename = protoDir + File.separator + cd.getCoordinationFunction() + ".xml";
        logger.debug("xacmlProtoFilename={}", xacmlProtoFilename);
        /*
         * Values to be used for placeholders
         */
        final String uniqueId = UUID.randomUUID().toString();
        final String cLOne = cd.getControlLoop().get(0);
        final String cLTwo = cd.getControlLoop().get(1);
        /*
         * Replace prototype placeholders with appropriate values
         */
        String xacmlPolicy = null;
        try (Stream<String> stream = Files.lines(Paths.get(xacmlProtoFilename))) {
            xacmlPolicy = stream.map(s -> s.replaceAll("UNIQUE_ID", uniqueId))
                .map(s -> s.replaceAll("CONTROL_LOOP_ONE", cLOne))
                .map(s -> s.replaceAll("CONTROL_LOOP_TWO", cLTwo))
                .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            logger.error("Error while generating XACML policy for coordination directive", e);
        }
        return xacmlPolicy;
    }

}
