/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2017-2020 AT&T Intellectual Property. All rights reserved.
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
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.beanutils.BeanUtils;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.controlloop.policy.ControlLoop;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.controlloop.policy.FinalResult;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyParam;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.controlloop.policy.Target;
import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.drools.domain.models.DroolsPolicy;
import org.onap.policy.drools.domain.models.operational.Operation;
import org.onap.policy.drools.domain.models.operational.OperationalPolicy;
import org.onap.policy.drools.domain.models.operational.OperationalTarget;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

public class ControlLoopProcessor implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ControlLoopProcessor.class);

    @Getter
    private final ControlLoopPolicy policy;
    private String currentNestedPolicyId;

    // not serializable, thus must be transient
    @Getter
    private transient ToscaPolicy toscaOpPolicy;

    // not serializable, thus must be transient
    @Getter
    private transient DroolsPolicy domainOpPolicy;

    /**
     * Construct an instance from yaml.
     *
     * @param yaml the yaml
     * @throws ControlLoopException if an error occurs
     */
    public ControlLoopProcessor(String yaml) throws ControlLoopException {
        try {
            final Yaml y = new Yaml(new CustomClassLoaderConstructor(ControlLoopPolicy.class,
                    ControlLoopPolicy.class.getClassLoader()));
            final Object obj = y.load(yaml);

            this.policy = (ControlLoopPolicy) obj;
            this.currentNestedPolicyId = this.policy.getControlLoop().getTrigger_policy();
        } catch (final Exception e) {
            //
            // Most likely this is a YAML Exception
            //
            throw new ControlLoopException(e);
        }
    }

    /**
     * Create an instance from a Tosca Policy.
     */
    public ControlLoopProcessor(ToscaPolicy toscaPolicy) throws ControlLoopException {
        try {
            this.policy = buildPolicyFromToscaCompliant(toscaPolicy);

            this.currentNestedPolicyId = this.policy.getControlLoop().getTrigger_policy();
            this.toscaOpPolicy = toscaPolicy;
        } catch (RuntimeException | CoderException e) {
            throw new ControlLoopException(e);
        }
    }

    private Target toStandardTarget(OperationalTarget opTarget) {
        Target target = new Target(TargetType.valueOf(opTarget.getTargetType()));
        try {
            BeanUtils.populate(target, opTarget.getEntityIds());
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.warn("target entityIds cannot be mapped (unexpected): {}", opTarget, e);
        }
        return target;
    }

    protected ControlLoopPolicy buildPolicyFromToscaCompliant(ToscaPolicy policy) throws CoderException {
        OperationalPolicy domainPolicy =
                PolicyEngineConstants.getManager().getDomainMaker().convertTo(policy, OperationalPolicy.class);

        ControlLoopPolicy backwardsCompatiblePolicy = new ControlLoopPolicy();

        // @formatter:off
        backwardsCompatiblePolicy.setPolicies(
            domainPolicy.getProperties().getOperations().stream().map(this::convertPolicy)
                    .collect(Collectors.toList()));
        // @formatter:on

        ControlLoop controlLoop = new ControlLoop();
        controlLoop.setAbatement(domainPolicy.getProperties().isAbatement());
        controlLoop.setControlLoopName(domainPolicy.getProperties().getId());
        controlLoop.setTimeout(domainPolicy.getProperties().getTimeout());
        controlLoop.setTrigger_policy(domainPolicy.getProperties().getTrigger());
        controlLoop.setVersion(domainPolicy.getVersion());

        backwardsCompatiblePolicy.setControlLoop(controlLoop);
        this.domainOpPolicy = domainPolicy;
        return backwardsCompatiblePolicy;
    }

    private Policy convertPolicy(Operation operation) {
        // @formatter:off
        Policy newPolicy = new Policy(PolicyParam.builder()
                .id(operation.getId())
                .name(operation.getActorOperation().getOperation())
                .description(operation.getDescription())
                .actor(operation.getActorOperation().getActor())
                .payload(operation.getActorOperation().getPayload())
                .recipe(operation.getActorOperation().getOperation())
                .retries(operation.getRetries())
                .timeout(operation.getTimeout())
                .target(toStandardTarget(operation.getActorOperation().getTarget()))
            .build());
        // @formatter:on

        newPolicy.setSuccess(operation.getSuccess());
        newPolicy.setFailure(operation.getFailure());
        newPolicy.setFailure_exception(operation.getFailureException());
        newPolicy.setFailure_guard(operation.getFailureGuard());
        newPolicy.setFailure_retries(operation.getFailureRetries());
        newPolicy.setFailure_timeout(operation.getFailureTimeout());

        return newPolicy;
    }

    /**
     * Get ControlLoopParams.
     */
    public ControlLoopParams getControlLoopParams() {
        ControlLoopParams controlLoopParams = new ControlLoopParams();

        controlLoopParams.setClosedLoopControlName(this.getControlLoop().getControlLoopName());
        controlLoopParams.setPolicyScope(domainOpPolicy.getType() + ":" + domainOpPolicy.getTypeVersion());
        controlLoopParams.setPolicyName(domainOpPolicy.getName());
        controlLoopParams.setPolicyVersion(domainOpPolicy.getVersion());
        controlLoopParams.setToscaPolicy(toscaOpPolicy);

        return controlLoopParams;
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
