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

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Properties;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.policy.common.endpoints.http.client.HttpClient;
import org.onap.policy.common.endpoints.http.client.HttpClientConfigException;
import org.onap.policy.common.endpoints.http.client.HttpClientFactory;
import org.onap.policy.common.endpoints.http.client.HttpClientFactoryInstance;
import org.onap.policy.drools.persistence.SystemPersistenceConstants;

@RunWith(MockitoJUnitRunner.class)
public class HttpClientsTest {
    private static final String CLIENT_NAME = "MY-CLIENT";

    @Mock
    private HttpClientFactory factory;


    @BeforeClass
    public static void setUpBeforeClass() {
        SystemPersistenceConstants.getManager().setConfigurationDir("src/test/resources");
    }

    @After
    public void tearDown() {
        HttpClientFactoryInstance.getClientFactory().destroy();
    }

    @Test
    public void test() throws HttpClientConfigException {
        HttpClientFactoryInstance.getClientFactory().destroy();

        HttpClients clients = new HttpClients();

        clients.addClients("my");

        // should find the client now
        HttpClient client = HttpClientFactoryInstance.getClientFactory().get(CLIENT_NAME);
        assertNotNull(client);

        clients.destroy();

        // destroyed - should NOT find the client
        assertThatIllegalArgumentException()
                        .isThrownBy(() -> HttpClientFactoryInstance.getClientFactory().get(CLIENT_NAME));

        // unknown property file
        assertThatIllegalArgumentException().isThrownBy(() -> clients.addClients("unknown"));

        // force exception from builder
        HttpClients clients2 = new HttpClients() {
            @Override
            protected HttpClientFactory getClientFactory() {
                return factory;
            }
        };

        when(factory.build(any(Properties.class))).thenThrow(new HttpClientConfigException("expected exception"));

        assertThatIllegalArgumentException().isThrownBy(() -> clients2.addClients("my"))
                        .withMessage("cannot initialize HTTP clients");
    }
}
