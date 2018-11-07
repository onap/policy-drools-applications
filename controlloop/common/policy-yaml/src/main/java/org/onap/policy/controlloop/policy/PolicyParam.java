/*
 * ============LICENSE_START=======================================================
 * policy-endpoints
 * ================================================================================
 * Copyright (C) 2018 Samsung Electronics Co., Ltd. All rights reserved.
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

package org.onap.policy.controlloop.policy;

import java.util.Map;

public class PolicyParam {
    private String id;
    private String name;
    private String description;
    private String actor;
    private Map<String, String> payload;
    private Target target;
    private String recipe;
    private Integer retries;
    private Integer timeout;

    public static PolicyParamBuilder builder() {
        return  new PolicyParamBuilder();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getActor() {
        return actor;
    }

    public Map<String, String> getPayload() {
        return payload;
    }

    public Target getTarget() {
        return target;
    }

    public String getRecipe() {
        return recipe;
    }

    public Integer getRetries() {
        return retries;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public static class PolicyParamBuilder {

        PolicyParam m = new PolicyParam();

        private PolicyParamBuilder() {
        }

        public PolicyParam build() {
            return m;
        }

        public PolicyParamBuilder id(String id) {
            m.id = id;
            return this;
        }

        public PolicyParamBuilder name(String name) {
            m.name = name;
            return this;
        }

        public PolicyParamBuilder description(String description) {
            m.description = description;
            return this;
        }

        public PolicyParamBuilder actor(String actor) {
            m.actor = actor;
            return this;
        }

        public PolicyParamBuilder payload(Map<String, String> payload) {
            m.payload = payload;
            return this;
        }

        public PolicyParamBuilder target(Target target) {
            m.target = target;
            return this;
        }

        public PolicyParamBuilder recipe(String recipe) {
            m.recipe = recipe;
            return this;
        }

        public PolicyParamBuilder retries(Integer retries) {
            m.retries = retries;
            return this;
        }

        public PolicyParamBuilder timeout(Integer timeout) {
            m.timeout = timeout;
            return this;
        }
    }
}