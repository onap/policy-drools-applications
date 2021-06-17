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

package org.onap.policy.controlloop.common.rules.test;

import org.onap.policy.common.endpoints.http.client.HttpClientConfigException;
import org.onap.policy.common.endpoints.http.client.HttpClientFactory;
import org.onap.policy.common.endpoints.http.client.HttpClientFactoryInstance;
import org.onap.policy.drools.persistence.SystemPersistenceConstants;

/**
 * Mechanism by which junit tests can manage HTTP Clients.
 */
public class HttpClients {

    /**
     * Adds Http Clients specified in the property file.
     *
     * @param propFilePrefix prefix prepended to "-http-client.properties" to yield the
     *        full name of the property file containing http client properties
     */
    public void addClients(String propFilePrefix) {
        try {
            getClientFactory().build(SystemPersistenceConstants.getManager().getHttpClientProperties(propFilePrefix));
        } catch (HttpClientConfigException e) {
            throw new IllegalArgumentException("cannot initialize HTTP clients", e);
        }
    }

    /**
     * Destroys all Http Clients.
     */
    public void destroy() {
        getClientFactory().destroy();
    }

    // these methods may be overridden by junit tests

    protected HttpClientFactory getClientFactory() {
        return HttpClientFactoryInstance.getClientFactory();
    }
}
