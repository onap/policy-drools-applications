/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2017-2021 AT&T Intellectual Property. All rights reserved.
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

import java.io.Serializable;
import lombok.Getter;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.actorserviceprovider.OperationFinalResult;
import org.onap.policy.controlloop.actorserviceprovider.OperationResult;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.drools.domain.models.operational.Operation;
import org.onap.policy.drools.domain.models.operational.OperationalPolicy;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

public class ControlLoopProcessor implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final StandardYamlCoder coder = new StandardYamlCoder();

    @Getter
    private final OperationalPolicy policy;
    private String currentNestedPolicyId;

    // not serializable, thus must be transient
    @Getter
    private transient ToscaPolicy toscaOpPolicy;

    /**
     * Construct an instance from yaml.
     *
     * @param yaml the yaml
     * @throws ControlLoopException if an error occurs
     */
    public ControlLoopProcessor(String yaml) throws ControlLoopException {
        this(decodeTosca(yaml));
    }

    /**
     * Create an instance from a Tosca Policy.
     */
    public ControlLoopProcessor(ToscaPolicy toscaPolicy) throws ControlLoopException {
        try {
            this.policy =
                    PolicyEngineConstants.getManager().getDomainMaker().convertTo(toscaPolicy, OperationalPolicy.class);
            this.currentNestedPolicyId = this.policy.getProperties().getTrigger();
            this.toscaOpPolicy = toscaPolicy;
        } catch (RuntimeException | CoderException e) {
            throw new ControlLoopException(e);
        }
    }

    private static ToscaPolicy decodeTosca(String yaml) throws ControlLoopException {
        try {
            ToscaServiceTemplate template = coder.decode(yaml, ToscaServiceTemplate.class);
            if (template == null || template.getToscaTopologyTemplate() == null) {
                throw new IllegalArgumentException("Cannot decode yaml into ToscaServiceTemplate");
            }

            return template.getToscaTopologyTemplate().getPolicies().get(0).values().iterator().next();

        } catch (final Exception e) {
            //
            // Most likely this is a YAML Exception
            //
            throw new ControlLoopException(e);
        }
    }

    /**
     * Get ControlLoopParams.
     */
    public ControlLoopParams getControlLoopParams() {
        var controlLoopParams = new ControlLoopParams();

        controlLoopParams.setClosedLoopControlName(this.policy.getProperties().getId());
        controlLoopParams.setPolicyScope(policy.getType() + ":" + policy.getTypeVersion());
        controlLoopParams.setPolicyName(policy.getName());
        controlLoopParams.setPolicyVersion(policy.getVersion());
        controlLoopParams.setToscaPolicy(toscaOpPolicy);

        return controlLoopParams;
    }

    public OperationFinalResult checkIsCurrentPolicyFinal() {
        return OperationFinalResult.toResult(this.currentNestedPolicyId);
    }

    /**
     * Get the current policy.
     *
     * @return the current policy
     * @throws ControlLoopException if an error occurs
     */
    public Operation getCurrentPolicy() throws ControlLoopException {
        if (this.policy == null || this.policy.getProperties() == null
                        || this.policy.getProperties().getOperations() == null
                        || this.policy.getProperties().getOperations().isEmpty()) {
            throw new ControlLoopException("There are no policies defined.");
        }

        for (final Operation nestedPolicy : this.policy.getProperties().getOperations()) {
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
    public void nextPolicyForResult(OperationResult result) throws ControlLoopException {
        final Operation currentPolicy = this.getCurrentPolicy();
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
                    this.currentNestedPolicyId = currentPolicy.getFailureTimeout();
                    break;
                case FAILURE_RETRIES:
                    this.currentNestedPolicyId = currentPolicy.getFailureRetries();
                    break;
                case FAILURE_EXCEPTION:
                    this.currentNestedPolicyId = currentPolicy.getFailureException();
                    break;
                case FAILURE_GUARD:
                default:
                    this.currentNestedPolicyId = currentPolicy.getFailureGuard();
                    break;
            }
        } catch (final ControlLoopException e) {
            this.currentNestedPolicyId = OperationFinalResult.FINAL_FAILURE_EXCEPTION.toString();
            throw e;
        }
    }
}
