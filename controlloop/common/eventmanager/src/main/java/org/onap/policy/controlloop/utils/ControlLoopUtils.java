/*-
 * ============LICENSE_START=======================================================
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

package org.onap.policy.controlloop.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import org.apache.commons.lang3.StringUtils;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.params.ControlLoopParams;
import org.onap.policy.controlloop.processor.ControlLoopProcessor;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Control Loop Utils.
 */
public class ControlLoopUtils {

    public static final Logger logger = LoggerFactory.getLogger(ControlLoopUtils.class);
    public static final String TOSCA_POLICY_PROPERTY_CONTENT = "content";

    private ControlLoopUtils() {
        super();
    }

    /**
     * Get a Control Loop Parameters object from a Tosca Policy.
     */
    public static ControlLoopParams toControlLoopParams(ToscaPolicy policy) {

        // TODO: ControlLoopParams class should be moved to this repo and take Tosca Policy in a constructor.

        /* No exceptions are thrown to keep the DRL simpler */

        try {
            if (policy == null || policy.getProperties() == null
                || policy.getProperties().get(TOSCA_POLICY_PROPERTY_CONTENT) == null) {
                logger.error("Invalid Policy: {}", policy);
                return null;
            }

            String encodedPolicy = policy.getProperties().get(TOSCA_POLICY_PROPERTY_CONTENT).toString();
            String decodedPolicy = URLDecoder.decode(encodedPolicy, "UTF-8");

            ControlLoopProcessor controlLoopProcessor = new ControlLoopProcessor(decodedPolicy);
            if (controlLoopProcessor.getControlLoop() == null
                || StringUtils.isEmpty(controlLoopProcessor.getControlLoop().getControlLoopName())) {
                return null;
            }

            ControlLoopParams controlLoopParams = new ControlLoopParams();
            controlLoopParams.setClosedLoopControlName(controlLoopProcessor.getControlLoop().getControlLoopName());
            controlLoopParams.setControlLoopYaml(encodedPolicy);
            controlLoopParams.setPolicyScope(policy.getType() + ":" + policy.getTypeVersion());
            controlLoopParams.setPolicyName(policy.getName());
            controlLoopParams.setPolicyVersion(policy.getVersion());

            return controlLoopParams;
        } catch (ControlLoopException | RuntimeException | UnsupportedEncodingException e) {
            logger.error("Invalid Policy because of {}: {}", e.getMessage(), policy, e);
            return null;
        }
    }

}
