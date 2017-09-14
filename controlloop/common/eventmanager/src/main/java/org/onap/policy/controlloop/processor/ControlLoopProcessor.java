/*-
 * ============LICENSE_START=======================================================
 * controlloop processor
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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
  private String currentPolicy = null;

  public ControlLoopProcessor(String yaml) throws ControlLoopException {
    this.yaml = yaml;
    try {
      final Yaml y = new Yaml(new CustomClassLoaderConstructor(ControlLoopPolicy.class,
          ControlLoopPolicy.class.getClassLoader()));
      final Object obj = y.load(this.yaml);
      if (obj instanceof ControlLoopPolicy) {
        this.policy = (ControlLoopPolicy) obj;
        this.currentPolicy = this.policy.getControlLoop().getTrigger_policy();
      } else {
        this.policy = null;
        throw new ControlLoopException("Unable to parse yaml into ControlLoopPolicy object");
      }
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
    return FinalResult.toResult(this.currentPolicy);
  }

  public Policy getCurrentPolicy() {
    for (final Policy policy : this.policy.getPolicies()) {
      if (policy.getId().equals(this.currentPolicy)) {
        return policy;
      }
    }
    return null;
  }

  public void nextPolicyForResult(PolicyResult result) throws ControlLoopException {
    final Policy policy = this.getCurrentPolicy();
    try {
      if (this.policy == null) {
        throw new ControlLoopException("There is no current policy to determine where to go to.");
      }
      switch (result) {
        case SUCCESS:
          this.currentPolicy = policy.getSuccess();
          break;
        case FAILURE:
          this.currentPolicy = policy.getFailure();
          break;
        case FAILURE_TIMEOUT:
          this.currentPolicy = policy.getFailure_timeout();
          break;
        case FAILURE_RETRIES:
          this.currentPolicy = policy.getFailure_retries();
          break;
        case FAILURE_EXCEPTION:
          this.currentPolicy = policy.getFailure_exception();
          break;
        case FAILURE_GUARD:
          this.currentPolicy = policy.getFailure_guard();
          break;
        default:
          throw new ControlLoopException("Bad policy result given: " + result);
      }
    } catch (final ControlLoopException e) {
      this.currentPolicy = FinalResult.FINAL_FAILURE_EXCEPTION.toString();
      throw e;
    }
  }

}
