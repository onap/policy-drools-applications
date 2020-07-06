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

package org.onap.policy.controlloop.eventmanager;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Properties;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onap.policy.common.endpoints.http.client.HttpClientFactoryInstance;
import org.onap.policy.controlloop.actorserviceprovider.ActorService;
import org.onap.policy.controlloop.ophistory.OperationHistoryDataManagerImpl;
import org.onap.policy.controlloop.ophistory.OperationHistoryDataManagerStub;
import org.onap.policy.drools.persistence.SystemPersistenceConstants;

public class EventManagerServicesTest {
    private static final String FILEPFX = "eventService/";
    private static final IllegalArgumentException EXPECTED_EXCEPTION =
                    new IllegalArgumentException("expected exception");

    private EventManagerServices services;

    /**
     * Configures HTTP clients.
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // start with a clean slate
        HttpClientFactoryInstance.getClientFactory().destroy();

        SystemPersistenceConstants.getManager().setConfigurationDir("src/test/resources");

        Properties props = SystemPersistenceConstants.getManager().getProperties("eventService/event-svc-http-client");
        HttpClientFactoryInstance.getClientFactory().build(props);
    }

    @AfterClass
    public static void teatDownBeforeClass() {
        HttpClientFactoryInstance.getClientFactory().destroy();
    }

    @After
    public void tearDown() {
        closeDb();
    }

    @Test
    public void testEventManagerServices_testGetActorService() {
        // try with guard disabled - should use DB stub
        services = new EventManagerServices(FILEPFX + "event-svc-guard-disabled");
        assertTrue(services.getDataManager() instanceof OperationHistoryDataManagerStub);
        assertNotNull(services.getActorService());

        // try with guard enabled - should create a DB connection
        services = new EventManagerServices(FILEPFX + "event-svc-with-db");
        assertTrue(services.getDataManager() instanceof OperationHistoryDataManagerImpl);
        assertNotNull(services.getActorService());
    }

    @Test
    public void testStartActorService() {
        // config file not found
        assertThatIllegalStateException().isThrownBy(() -> new EventManagerServices("missing-config-file"));
    }

    @Test
    public void testIsGuardEnabled() {
        // cannot check guard
        services = new EventManagerServices(FILEPFX + "event-svc-no-guard-actor");
        assertTrue(services.getDataManager() instanceof OperationHistoryDataManagerStub);

        // force exception when checking for guard operator
        services = new EventManagerServices(FILEPFX + "event-svc-with-db") {
            @Override
            public ActorService getActorService() {
                ActorService svc = mock(ActorService.class);
                when(svc.getActor(any())).thenThrow(EXPECTED_EXCEPTION);
                return svc;
            }
        };
        assertTrue(services.getDataManager() instanceof OperationHistoryDataManagerStub);
    }

    @Test
    public void testMakeDataManager() {
        assertThatThrownBy(() -> new EventManagerServices(FILEPFX + "event-svc-invalid-db"))
                        .isInstanceOf(IllegalArgumentException.class);
    }


    private void closeDb() {
        if (services != null) {
            services.getDataManager().stop();
        }
    }
}
