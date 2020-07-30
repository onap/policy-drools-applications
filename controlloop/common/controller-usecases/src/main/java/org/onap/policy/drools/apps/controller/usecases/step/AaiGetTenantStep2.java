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

package org.onap.policy.drools.apps.controller.usecases.step;

import org.apache.commons.lang3.StringUtils;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.onap.policy.controlloop.actor.aai.AaiActor;
import org.onap.policy.controlloop.actor.aai.AaiGetTenantOperation;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;

public class AaiGetTenantStep2 extends Step2 {
    private final String vserver;


    /**
     * Constructs the object using information from another step.
     *
     * @param otherStep step whose information should be used
     */
    public AaiGetTenantStep2(Step2 otherStep) {
        super(otherStep, AaiActor.NAME, AaiGetTenantOperation.NAME);

        vserver = event.getAai().get(VSERVER_VSERVER_NAME);
        if (StringUtils.isBlank(vserver)) {
            throw new IllegalArgumentException("missing " + VSERVER_VSERVER_NAME + " in enrichment data");
        }
    }

    @Override
    public boolean start(long remainingMs) {
        if (stepContext.contains(AaiGetTenantOperation.getKey(vserver))) {
            // already have the data
            return false;
        }

        return super.start(remainingMs);
    }

    /**
     * Saves the response for later use.
     */
    @Override
    public void success(OperationOutcome outcome) {
        StandardCoderObject resp = outcome.getResponse();
        stepContext.setProperty(AaiGetTenantOperation.getKey(vserver), resp);
    }
}
