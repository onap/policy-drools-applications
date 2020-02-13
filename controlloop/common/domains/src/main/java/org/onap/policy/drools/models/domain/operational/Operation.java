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

package org.onap.policy.drools.models.domain.operational;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * Policy Operation.
 */

@Data
@Builder
public class Operation implements Serializable {
    private static final long serialVersionUID = 6175229119078195110L;

    /**
     * Operation Identifier.
     */
    @SerializedName("id")
    protected String id;

    /**
     * Description.
     */
    @SerializedName("description")
    protected String description;

    /**
     * Actor Operation.
     */
    @SerializedName("operation")
    protected ActorOperation actorOperation;

    /**
     * Operation Timeout in seconds.
     */
    @SerializedName("timeout")
    @Builder.Default
    protected int timeout = 10;

    /**
     * Number of Retries.
     */
    @SerializedName("retries")
    @Builder.Default
    protected int retries = 0;

    /**
     * Success Treatment.
     */
    @SerializedName("success")
    @Builder.Default
    protected String success = "final_success";

    /**
     * Failure Treatment.
     */
    @SerializedName("failure")
    @Builder.Default
    protected String failure = "final_failure";

    /**
     * Failure Timeout Treatment.
     */
    @SerializedName("failure_timeout")
    @Builder.Default
    protected String failureTimeout = "final_failure_timeout";

    /**
     * Failure Retry Treatment.
     */
    @SerializedName("failure_retries")
    @Builder.Default
    protected String failureRetries = "final_failure_retries";

    /**
     * Failure Exception Treatment.
     */
    @SerializedName("failure_exception")
    @Builder.Default
    protected String failureException = "final_failure_exception";

    /**
     * Failure Guard Treatment.
     */
    @SerializedName("failure_guard")
    @Builder.Default
    protected String failureGuard = "final_failure_guard";
}
