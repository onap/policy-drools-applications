/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019 Bell Canada.
 * Modifications Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.template.demo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.ccsdk.cds.controllerblueprints.common.api.CommonHeader;
import org.onap.ccsdk.cds.controllerblueprints.common.api.EventType;
import org.onap.ccsdk.cds.controllerblueprints.common.api.Status;
import org.onap.ccsdk.cds.controllerblueprints.processing.api.BluePrintProcessingServiceGrpc;
import org.onap.ccsdk.cds.controllerblueprints.processing.api.ExecutionServiceInput;
import org.onap.ccsdk.cds.controllerblueprints.processing.api.ExecutionServiceOutput;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.TopicListener;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.ControlLoopTargetType;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.controlloop.util.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class for vfirewall use case using CDS actor.
 */
public class VfwControlLoopCdsTest extends ControlLoopBase implements TopicListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(VfwControlLoopCdsTest.class);

    private final AtomicReference<StreamObserver<ExecutionServiceOutput>> responseObserverRef = new AtomicReference<>();
    private Server server;

    /**
     * Setup the simulator.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        ControlLoopBase.setUpBeforeClass("../archetype-cl-amsterdam/src/main/resources/archetype-resources/src/"
                                                 + "main/resources/__closedLoopControlName__.drl",
                "src/test/resources/yaml/policy_ControlLoop_vFW_CDS.yaml",
                "service=ServiceDemo;resource=Res1Demo;type=operational", "CL_vFW",
                "org.onap.closed_loop.ServiceDemo:VNFS:1.0.0");
        SupportUtil.setCustomQuery("true");
    }

    @Before
    public void setUp() throws IOException {
        this.startGrpcServer();
    }

    @After
    public void tearDown() {
        this.stopGrpcServer();
    }

    private void startGrpcServer() throws IOException {

        BluePrintProcessingServiceGrpc.BluePrintProcessingServiceImplBase cdsBlueprintServerImpl =

            new BluePrintProcessingServiceGrpc.BluePrintProcessingServiceImplBase() {

                @Override
                public StreamObserver<ExecutionServiceInput> process(
                        final StreamObserver<ExecutionServiceOutput> responseObserver) {

                    responseObserverRef.set(responseObserver);

                    return new StreamObserver<ExecutionServiceInput>() {
                        @Override
                        public void onNext(final ExecutionServiceInput input) {
                            LOGGER.info("gRPC server onNext() for input: {} ...", input);
                            ExecutionServiceOutput output =
                                    ExecutionServiceOutput.newBuilder()
                                            .setCommonHeader(
                                                    CommonHeader.newBuilder().setRequestId(
                                                            input.getCommonHeader().getRequestId()).build())
                                            .setStatus(
                                                    Status.newBuilder().setEventType(
                                                            EventType.EVENT_COMPONENT_EXECUTED).build())
                                            .build();
                            responseObserver.onNext(output);
                        }

                        @Override
                        public void onError(final Throwable throwable) {
                            LOGGER.error("gRPC server onError() for throwable: {} ...", throwable);
                        }

                        @Override
                        public void onCompleted() {
                            LOGGER.info("gRPC server onCompleted() ...");
                            responseObserver.onCompleted();
                        }
                    };
                }
            };

        server = ServerBuilder.forPort(SupportUtil.GRPC_SERVER_PORT).addService(cdsBlueprintServerImpl).build().start();
        LOGGER.info("gRPC server is listening for CDS requests on port {}", SupportUtil.GRPC_SERVER_PORT);

    }

    private void stopGrpcServer() {
        if (server != null) {
            this.server.shutdown();
            LOGGER.info("gRPC server handling CDS requests has been successfully shut down.");
        }
    }

    @Test
    public void testSuccess() {

        /*
         * Allows the PolicyEngine to callback to this object to notify that there is an event ready
         * to be pulled from the queue
         */
        for (TopicSink sink : noopTopics) {
            assertTrue(sink.start());
            sink.register(this);
        }

        /*
         * Create a unique requestId
         */
        requestId = UUID.randomUUID();

        /*
         * Simulate an onset event the policy engine will receive from DCAE to kick off processing
         * through the rules
         */
        sendEvent(pair.first, requestId, ControlLoopEventStatus.ONSET);

        kieSession.fireUntilHalt();
        kieSession.fireAllRules();

        /*
         * The only fact in memory should be Params
         */
        assertEquals(1, kieSession.getFactCount());

        /*
         * Print what's left in memory
         */
        dumpFacts(kieSession);
    }


    /*
     * @see org.onap.policy.drools.PolicyEngineListener#newEventNotification(java.lang.String)
     */
    @Override
    public void onTopicEvent(CommInfrastructure commType, String topic, String event) {
        /*
         * Pull the object that was sent out to DMAAP and make sure it is a ControlLoopNotification
         * of type active
         */
        assertEquals("POLICY-CL-MGT", topic);
        VirtualControlLoopNotification notification =
                Serialization.gsonJunit.fromJson(event, VirtualControlLoopNotification.class);
        assertNotNull(notification);
        String policyName = notification.getPolicyName();
        if (policyName.endsWith("EVENT")) {
            logger.debug("Rule Fired: " + notification.getPolicyName());
            assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());
        } else if (policyName.endsWith("GUARD_NOT_YET_QUERIED")) {
            logger.debug("Rule Fired: " + notification.getPolicyName());
            assertEquals(ControlLoopNotificationType.OPERATION, notification.getNotification());
            assertNotNull(notification.getMessage());
            assertTrue(notification.getMessage().startsWith("Sending guard query"));
        } else if (policyName.endsWith("GUARD.RESPONSE")) {
            logger.debug("Rule Fired: " + notification.getPolicyName());
            assertEquals(ControlLoopNotificationType.OPERATION, notification.getNotification());
            assertNotNull(notification.getMessage());
            assertTrue(notification.getMessage().toLowerCase().endsWith("permit"));
        } else if (policyName.endsWith("GUARD_PERMITTED")) {
            logger.debug("Rule Fired: " + notification.getPolicyName());
            assertEquals(ControlLoopNotificationType.OPERATION, notification.getNotification());
            assertNotNull(notification.getMessage());
            assertTrue(notification.getMessage().startsWith("actor=CDS"));
        } else if (policyName.endsWith("OPERATION.TIMEOUT")) {
            logger.debug("Rule Fired: " + notification.getPolicyName());
            kieSession.halt();
            logger.debug("The operation timed out");
            fail("Operation Timed Out");
        } else if (policyName.endsWith("CDS.RESPONSE")) {
            logger.debug("Rule Fired: " + notification.getPolicyName());
            assertEquals(ControlLoopNotificationType.OPERATION_SUCCESS, notification.getNotification());
            assertNotNull(notification.getMessage());
            assertTrue(notification.getMessage().startsWith("actor=CDS"));
            sendEvent(pair.first, requestId, ControlLoopEventStatus.ABATED);
        } else if (policyName.endsWith("EVENT.MANAGER")) {
            logger.debug("Rule Fired: " + notification.getPolicyName());
            if ("error".equals(notification.getAai().get("generic-vnf.vnf-name"))) {
                assertEquals(ControlLoopNotificationType.FINAL_FAILURE, notification.getNotification());
                assertEquals("Target vnf-id could not be found", notification.getMessage());
            } else if ("getFail".equals(notification.getAai().get("generic-vnf.vnf-name"))) {
                assertEquals(ControlLoopNotificationType.FINAL_FAILURE, notification.getNotification());
            } else {
                assertEquals(ControlLoopNotificationType.FINAL_SUCCESS, notification.getNotification());
            }
            kieSession.halt();
        } else if (policyName.endsWith("EVENT.MANAGER.TIMEOUT")) {
            logger.debug("Rule Fired: " + notification.getPolicyName());
            kieSession.halt();
            logger.debug("The control loop timed out");
            fail("Control Loop Timed Out");
        }
    }

    /**
     * This method is used to simulate event messages from DCAE that start the control loop (onset
     * message) or end the control loop (abatement message).
     *
     * @param policy the controlLoopName comes from the policy
     * @param requestId the requestId for this event
     * @param status could be onset or abated
     */
    private void sendEvent(ControlLoopPolicy policy, UUID requestId, ControlLoopEventStatus status) {
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(policy.getControlLoop().getControlLoopName());
        event.setRequestId(requestId);
        event.setTargetType(ControlLoopTargetType.VNF);
        event.setTarget("generic-vnf.vnf-name");
        event.setClosedLoopAlarmStart(Instant.now());
        event.setAai(new HashMap<>());
        event.getAai().put("generic-vnf.vnf-name", "testGenericVnfID");
        event.getAai().put("vserver.vserver-name", "OzVServer");
        event.setClosedLoopEventStatus(status);
        Map<String, String> map = new HashMap<>();
        map.put("my-key", "my-value");
        event.setAdditionalEventParams(map);
        kieSession.insert(event);
    }

}
