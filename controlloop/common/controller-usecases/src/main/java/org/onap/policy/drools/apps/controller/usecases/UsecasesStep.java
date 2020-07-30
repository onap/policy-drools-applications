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

package org.onap.policy.drools.apps.controller.usecases;

import org.onap.policy.controlloop.eventmanager.Step;
import org.onap.policy.controlloop.eventmanager.StepContext;
import org.onap.policy.controlloop.policy.Policy;

public class UsecasesStep extends Step {

    public UsecasesStep(StepContext context, Policy policy) {
        super(context, policy);
    }

    public UsecasesStep(String actor, String operation, Step otherStep) {
        super(actor, operation, otherStep);
    }

    /**
     * Determines if this step can now be skipped, because another step already
     * accomplished the work.
     *
     * @return {@code true} if this step can be skipped, {@code false} otherwise
     */
    public boolean canSkip() {
        return false;
    }
}
