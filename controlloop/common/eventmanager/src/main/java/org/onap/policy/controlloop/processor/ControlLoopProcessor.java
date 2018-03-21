/*-
 * ============LICENSE_START=======================================================
 * controlloop processor
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

package org.onap.policy.controlloop.processor;

import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.policy.ControlLoop;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.controlloop.policy.FinalResult;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

public class ControlLoopProcessor {

    private final String yaml;
    private final ControlLoopPolicy policy;
    private String currentNestedPolicyId = null;

    /**
     * Construct an instance from yaml.
     * 
     * @param yaml the yaml
     * @throws ControlLoopException if an error occurs
     */
    public ControlLoopProcessor(String yaml) throws ControlLoopException {
        this.yaml = yaml;
        try {
            final Yaml y = new Yaml(new CustomClassLoaderConstructor(ControlLoopPolicy.class,
                    ControlLoopPolicy.class.getClassLoader()));
            final Object obj = y.load(this.yaml);

            this.policy = (ControlLoopPolicy) obj;
            this.currentNestedPolicyId = this.policy.getControlLoop().getTrigger_policy();
        } catch (final Exception e) {
            //
            // Most likely this is a YAML Exception
            //
            throw new ControlLoopException(e);
        }
    }

    public ControlLoop getControlLoop() {
        return this.policy.getControlLoop();
    }

    public FinalResult checkIsCurrentPolicyFinal() {
        return FinalResult.toResult(this.currentNestedPolicyId);
    }

    /**
     * Get the current policy.
     * 
     * @return the current policy
     * @throws ControlLoopException if an error occurs
     */
    public Policy getCurrentPolicy() throws ControlLoopException {
        if (this.policy == null || this.policy.getPolicies() == null) {
            throw new ControlLoopException("There are no policies defined.");
        }

        for (final Policy nestedPolicy : this.policy.getPolicies()) {
            if (nestedPolicy.getId().equals(this.currentNestedPolicyId)) {
                return nestedPolicy;
            }
        }
        return null;
    }

    /**
     * Get the next policy given a result of the current policy.
     * 
     * @param result the result of the current policy
     * @throws ControlLoopException if an error occurs
     */
    public void nextPolicyForResult(PolicyResult result) throws ControlLoopException {
        final Policy currentPolicy = this.getCurrentPolicy();
        try {
            if (currentPolicy == null) {
                throw new ControlLoopException("There is no current policy to determine where to go to.");
            }
            switch (result) {
                case SUCCESS:
                    this.currentNestedPolicyId = currentPolicy.getSuccess();
                    break;
                case FAILURE:
                    this.currentNestedPolicyId = currentPolicy.getFailure();
                    break;
                case FAILURE_TIMEOUT:
                    this.currentNestedPolicyId = currentPolicy.getFailure_timeout();
                    break;
                case FAILURE_RETRIES:
                    this.currentNestedPolicyId = currentPolicy.getFailure_retries();
                    break;
                case FAILURE_EXCEPTION:
                    this.currentNestedPolicyId = currentPolicy.getFailure_exception();
                    break;
                case FAILURE_GUARD:
                default:
                    this.currentNestedPolicyId = currentPolicy.getFailure_guard();
                    break;
            }
        } catch (final ControlLoopException e) {
            this.currentNestedPolicyId = FinalResult.FINAL_FAILURE_EXCEPTION.toString();
            throw e;
        }
    }
}
