/*-
 * ============LICENSE_START=======================================================
 * m2/appclcm
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

package appclcm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Properties;
import java.util.UUID;
import org.drools.core.WorkingMemory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopTargetType;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.Target;
import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.m2.appclcm.AppcLcmActor;
import org.onap.policy.m2.appclcm.AppcLcmHealthCheckOperation;
import org.onap.policy.m2.appclcm.AppcLcmOperation;
import org.onap.policy.m2.base.Transaction;

public class AppcLcmActorTest {
    public static Policy policy;
    public static VirtualControlLoopEvent event;
    public static Transaction transaction;
    public static AppcLcmHealthCheckOperation operation;
    public static AppcLcmActor actor;

    /**
     * Class-level setup.
     */
    @BeforeClass
    public static void setup() {
        PolicyEngineConstants.getManager().configure(new Properties());
        PolicyEngineConstants.getManager().start();

        policy = new Policy();
        policy.setActor("APPCLCM");
        policy.setTarget(new Target(TargetType.VM));

        event = new VirtualControlLoopEvent();
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event.setRequestId(UUID.randomUUID());
        event.setTarget("vserver.vserver-name");
        event.setTargetType(ControlLoopTargetType.VM);
        event.getAai().put("vserver.is-closed-loop-disabled", "false");
        event.getAai().put("complex.state", "NJ");
        event.getAai().put("vserver.l-interface.interface-name", "89ee9ee6-1e96-4063-b690-aa5ca9f73b32");
        event.getAai().put("vserver.l-interface.l3-interface-ipv4-address-list.l3-inteface-ipv4-address",
            "135.144.3.49");
        event.getAai().put("vserver.l-interface.l3-interface-ipv6-address-list.l3-inteface-ipv6-address", null);
        event.getAai().put("vserver.in-maint", "N");
        event.getAai().put("complex.city", "AAIDefault");
        event.getAai().put("vserver.vserver-id", "aa7a24f9-8791-491f-b31a-c8ba5ad9e2aa");
        event.getAai().put("vserver.l-interface.network-name", "vUSP_DPA3_OAM_3750");
        event.getAai().put("vserver.vserver-name", "ctsf0002vm013");
        event.getAai().put("generic-vnf.vnf-name", "ctsf0002v");
        event.getAai().put("generic-vnf.vnf-id", "0f551f1b-e4e5-4ce2-84da-eda916e06e1c");
        event.getAai().put("generic-vnf.service-id", "e433710f-9217-458d-a79d-1c7aff376d89");
        event.getAai().put("vserver.selflink", "https://compute-aic.dpa3.cci.att.com:8774/v2/d0719b845a804b368f8ac0bba39e188b/servers/aa7a24f9-8791-491f-b31a-c8ba5ad9e2aa");
        event.getAai().put("generic-vnf.vnf-type", "vUSP - vCTS");
        event.getAai().put("tenant.tenant-id", "d0719b845a804b368f8ac0bba39e188b");
        event.getAai().put("cloud-region.identity-url", "https://compute-aic.dpa3.cci.att.com:8774/");
        event.getAai().put("vserver.prov-status", "PROV");
        event.getAai().put("complex.physical-location-id", "LSLEILAA");

        WorkingMemory wm = mock(WorkingMemory.class);
        transaction = new Transaction(wm, "clvusptest", event.getRequestId(), null);

        actor = new AppcLcmActor();
    }

    @AfterClass
    public static void cleanup() {
        transaction.cleanup();
        PolicyEngineConstants.getManager().stop();
    }

    @Test
    public void testGetName() {
        assertEquals("APPCLCM", actor.getName());
    }

    @Test
    public void testCreateOperation() {
        policy.setRecipe("HEALTHCHECK");
        policy.getTarget().setType(TargetType.VNF);
        operation = (AppcLcmHealthCheckOperation) actor.createOperation(transaction, policy, event, 1);
        assertTrue(operation instanceof AppcLcmHealthCheckOperation);

        policy.setRecipe("");
        AppcLcmOperation lcmOperation = (AppcLcmOperation) actor.createOperation(transaction, policy, event, 1);
        assertTrue(lcmOperation instanceof AppcLcmOperation);
    }
}
