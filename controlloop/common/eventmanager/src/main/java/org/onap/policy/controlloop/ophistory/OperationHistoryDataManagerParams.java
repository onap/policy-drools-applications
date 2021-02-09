/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.controlloop.ophistory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.onap.policy.common.parameters.BeanValidator;
import org.onap.policy.common.parameters.ValidationResult;
import org.onap.policy.common.parameters.annotations.Min;
import org.onap.policy.common.parameters.annotations.NotBlank;
import org.onap.policy.common.parameters.annotations.NotNull;

/**
 * Parameters for a Data Manager.
 */
@NotNull
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationHistoryDataManagerParams {
    public static final String DEFAULT_PU = "OperationsHistoryPU";
    public static final String DEFAULT_DRIVER = "org.mariadb.jdbc.Driver";
    public static final String DEFAULT_TYPE = "MySQL";

    @NotBlank
    private String url;
    @NotBlank
    private String userName;

    // may be blank
    private String password;

    @Builder.Default
    private String persistenceUnit = DEFAULT_PU;

    @Builder.Default
    private String driver = DEFAULT_DRIVER;

    @Builder.Default
    private String dbType = DEFAULT_TYPE;

    /**
     * Maximum number of records that can be waiting to be inserted into the DB. When the
     * limit is reached, the oldest records are discarded.
     */
    @Min(1)
    @Builder.Default
    private int maxQueueLength = 10000;

    /**
     * Number of records to add the DB in one transaction.
     */
    @Min(1)
    @Builder.Default
    private int batchSize = 100;

    /**
     * Validates the parameters.
     *
     * @param resultName name of the result
     *
     * @return the validation result
     */
    public ValidationResult validate(String resultName) {
        return new BeanValidator().validateTop(resultName, this);
    }
}
