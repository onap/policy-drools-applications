/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.controlloop.processor.ControlLoopProcessor;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Control Loop Utils.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ControlLoopUtils {

    public static final Logger logger = LoggerFactory.getLogger(ControlLoopUtils.class);

    /**
     * Get a Control Loop Parameters object from a Tosca Policy.
     */
    public static ControlLoopParams toControlLoopParams(ToscaPolicy policy) {

        /* No exceptions are thrown to keep the DRL simpler */

        try {
            return new ControlLoopProcessor(policy).getControlLoopParams();
        } catch (ControlLoopException | RuntimeException e) {
            logger.error("Invalid Policy because of {}: {}", e.getMessage(), policy, e);
            return null;
        }
    }
}
