/*-
 * ============LICENSE_START=======================================================
 * m2/appclcm
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

package org.onap.policy.m2.appclcm;

import java.io.Serializable;
import org.onap.policy.controlloop.ControlLoopEvent;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.m2.adapters.VirtualOnsetAdapter;
import org.onap.policy.m2.base.Actor;
import org.onap.policy.m2.base.Operation;
import org.onap.policy.m2.base.Transaction;

/**
 * A single instance of this class is created, and resides within the
 * 'nameToActor' table within class 'Transaction', under the key 'APPC'.
 */
public class AppcLcmActor implements Actor, Serializable {
    /* *******************/
    /* 'Actor' interface */
    /* *******************/

    private static final long serialVersionUID = -593438898257647144L;


    static {
        // ensures that 'VirtualOnsetAdapter' has an entry in the
        // 'OnsetAdapter' table
        VirtualOnsetAdapter.register();
    }

    /**
     * Return the name associated with this 'Actor'.
     *
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "APPCLCM";
    }

    /**
     * Create an 'Operation' for this 'Actor'.
     *
     * {@inheritDoc}
     */
    @Override
    public Operation createOperation(
        Transaction transaction, Policy policy, ControlLoopEvent onset,
        int attempt) {

        if ("healthcheck".equalsIgnoreCase(policy.getRecipe())) {
            return new AppcLcmHealthCheckOperation(transaction, policy, onset, attempt);
        }
        return new AppcLcmOperation(transaction, policy, onset, attempt);
    }
}
