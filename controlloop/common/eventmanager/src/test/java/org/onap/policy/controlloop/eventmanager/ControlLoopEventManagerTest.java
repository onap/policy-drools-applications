/*-
 * ============LICENSE_START=======================================================
 * unit test
 * ================================================================================
 * Copyright (C) 2017-2019 AT&T Intellectual Property. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onap.policy.aai.AaiCqResponse;
import org.onap.policy.aai.AaiGetVnfResponse;
import org.onap.policy.aai.AaiGetVserverResponse;
import org.onap.policy.aai.AaiNqRequestError;
import org.onap.policy.aai.AaiNqResponseWrapper;
import org.onap.policy.aai.AaiNqVServer;
import org.onap.policy.aai.RelatedToProperty;
import org.onap.policy.aai.Relationship;
import org.onap.policy.aai.RelationshipData;
import org.onap.policy.aai.RelationshipList;
import org.onap.policy.aai.util.AaiException;
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.common.utils.io.Serializer;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.SupportUtil;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.eventmanager.ControlLoopEventManager.NewEventStatus;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.guard.GuardResult;
import org.onap.policy.guard.PolicyGuard;
import org.onap.policy.guard.PolicyGuard.LockResult;
import org.onap.policy.guard.TargetLock;
import org.powermock.reflect.Whitebox;

public class ControlLoopEventManagerTest {
    private static final String PROCESS_VSERVER_RESPONSE = "processVServerResponse";
    private static final String ONSET_ONE = "onsetOne";
    private static final String VSERVER_NAME = "vserver.vserver-name";
    private static final String TEST_YAML = "src/test/resources/test.yaml";
    private static final String SERVICE_TYPE = "service-subscription.service-type";
    private static final String SERVICE_INSTANCE_NAME = "service-instance.service-instance-name";
    private static final String SERVICE_INSTANCE_ID = "service-instance.service-instance-id";
    private static final String SERVICE_INSTANCE = "service-instance";
    private static final String VNF_NAME_TEXT = "lll_vnf_010317";
    private static final String SERVICE_INSTANCE_NAME_TEXT = "lll_svc_010317";
    private static final String VNF_NAME = "generic-vnf.vnf-name";
    private static final String VNF_ID = "generic-vnf.vnf-id";
    private static final String SERVICE_INSTANCE_UUID = "e1e9c97c-02c0-4919-9b4c-eb5d5ef68970";
    private static final String MSO_CUSTOMER_ID = "customer.global-customer-id";
    private static final String AAI_USERNAME = "aai.username";
    private static final String AAI_URL = "aai.url";
    private static final String AAI_PASS = "aai.password";
    private static final String TWO_ONSET_TEST = "TwoOnsetTest";
    private static final String MSO_1610_ST = "MSO_1610_ST";
    private static final String MSO_DEV_SERVICE_TYPE = "MSO-dev-service-type";
    private static final String VNF_UUID = "83f674e8-7555-44d7-9a39-bdc3770b0491";
    private static final String AAI_SERVICE_SUBSCRIPTION_URI =
                    "/aai/v11/business/customers/customer/MSO_1610_ST/service-subscriptions/service-subscription";
    private static final String MSO_SERVICE_INSTANCE_URI = "/MSO-dev-service-type/service-instances/service-instance/";

    private static final String PROCESS_VNF_RESPONSE_METHOD_NAME = "processVnfResponse";

    private static final String INVALID_URL = "http://localhost:9999";


    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private VirtualControlLoopEvent onset;

    /**
     * Set up test class.
     */
    @BeforeClass
    public static void setUpSimulator() throws Exception {
        org.onap.policy.simulators.Util.buildAaiSim();

        PolicyEngineConstants.getManager().setEnvironmentProperty(AAI_USERNAME, "AAI");
        PolicyEngineConstants.getManager().setEnvironmentProperty(AAI_PASS, "AAI");
        PolicyEngineConstants.getManager().setEnvironmentProperty(AAI_URL, "http://localhost:6666");
    }

    @AfterClass
    public static void tearDownSimulator() {
        HttpServletServerFactoryInstance.getServerFactory().destroy();
    }

    /**
     * Setup.
     */
    @Before
    public void setUp() {
        onset = new VirtualControlLoopEvent();
        onset.setClosedLoopControlName("ControlLoop-vUSP");
        onset.setRequestId(UUID.randomUUID());
        onset.setTarget("VM_NAME");
        onset.setClosedLoopAlarmStart(Instant.now());
        onset.setAai(new HashMap<String, String>());
        onset.getAai().put("cloud-region.identity-url", "foo");
        onset.getAai().put("vserver.selflink", "bar");
        onset.getAai().put(VNF_ID, VNF_UUID);
        onset.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);

        PolicyEngineConstants.getManager().setEnvironmentProperty(AAI_URL, "http://localhost:6666");
    }

    @Test
    public void testAaiVnfInfo() throws IOException {
        final SupportUtil.Pair<ControlLoopPolicy, String> pair = SupportUtil.loadYaml(TEST_YAML);
        onset.setClosedLoopControlName(pair.key.getControlLoop().getControlLoopName());
        AaiGetVnfResponse response = getQueryByVnfId2();
        assertNotNull(response);
    }

    @Test
    public void testAaiVnfInfo2() throws IOException {
        final SupportUtil.Pair<ControlLoopPolicy, String> pair = SupportUtil.loadYaml(TEST_YAML);
        onset.setClosedLoopControlName(pair.key.getControlLoop().getControlLoopName());
        AaiGetVnfResponse response = getQueryByVnfName2();
        assertNotNull(response);
    }

    @Test
    public void testAaiVserver() throws IOException {
        final SupportUtil.Pair<ControlLoopPolicy, String> pair = SupportUtil.loadYaml(TEST_YAML);
        onset.setClosedLoopControlName(pair.key.getControlLoop().getControlLoopName());
        AaiGetVserverResponse response = getQueryByVserverName2();
        assertNotNull(response);
    }

    @Test
    public void abatementCheckEventSyntaxTest() throws ControlLoopException {
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName("abatementAAI");
        event.setRequestId(UUID.randomUUID());
        event.setTarget(VNF_ID);
        event.setClosedLoopAlarmStart(Instant.now());
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ABATED);
        ControlLoopEventManager manager = makeManager(event);
        assertNull(manager.getVnfResponse());
        assertNull(manager.getVserverResponse());
        manager.checkEventSyntax(event);
        assertNull(manager.getVnfResponse());
        assertNull(manager.getVserverResponse());


        event.setAai(new HashMap<>());
        event.getAai().put(VNF_NAME, "abatementTest");
        manager.checkEventSyntax(event);
        assertNull(manager.getVnfResponse());
        assertNull(manager.getVserverResponse());
    }

    @Test
    public void subsequentOnsetTest() throws Exception {
        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(TWO_ONSET_TEST);
        event.setRequestId(requestId);
        event.setTarget(VNF_ID);
        event.setClosedLoopAlarmStart(Instant.now());
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event.setAai(new HashMap<>());
        event.getAai().put(VNF_NAME, ONSET_ONE);

        ControlLoopEventManager manager = makeManager(event);
        VirtualControlLoopNotification notification = manager.activate(event);

        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        ControlLoopEventManager.NewEventStatus status = null;
        status = manager.onNewEvent(event);
        assertNotNull(status);
        assertEquals(ControlLoopEventManager.NewEventStatus.FIRST_ONSET, status);

        AaiGetVnfResponse response = manager.getVnfResponse();
        assertNotNull(response);
        assertNull(manager.getVserverResponse());

        VirtualControlLoopEvent event2 = new VirtualControlLoopEvent();
        event2.setClosedLoopControlName(TWO_ONSET_TEST);
        event2.setRequestId(requestId);
        event2.setTarget(VNF_ID);
        event2.setClosedLoopAlarmStart(Instant.now());
        event2.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event2.setAai(new HashMap<>());
        event2.getAai().put(VNF_NAME, "onsetTwo");


        status = manager.onNewEvent(event2);
        assertEquals(ControlLoopEventManager.NewEventStatus.SUBSEQUENT_ONSET, status);
        AaiGetVnfResponse response2 = manager.getVnfResponse();
        assertNotNull(response2);
        // We should not have queried AAI, so the stored response should be the same
        assertEquals(response, response2);
        assertNull(manager.getVserverResponse());
    }

    /**
     * Simulate a response.
     */
    public static AaiGetVnfResponse getQueryByVnfId2() {
        AaiGetVnfResponse response = new AaiGetVnfResponse();

        response.setVnfId(VNF_UUID);
        response.setVnfName(VNF_NAME_TEXT);
        response.setVnfType("Basa-122216-Service/VidVsamp12BaseVolume 1");
        response.setServiceId("a9a77d5a-123e-4ca2-9eb9-0b015d2ee0fb");
        response.setOrchestrationStatus("Created");
        response.setInMaint(false);
        response.setIsClosedLoopDisabled(false);
        response.setResourceVersion("1494001988835");
        response.setModelInvariantId("f18be3cd-d446-456e-9109-121d9b62feaa");

        final RelationshipList relationshipList = new RelationshipList();
        final Relationship relationship = new Relationship();
        RelationshipData relationshipDataItem = new RelationshipData();

        relationshipDataItem.setRelationshipKey(MSO_CUSTOMER_ID);
        relationshipDataItem.setRelationshipValue(MSO_1610_ST);
        relationship.getRelationshipData().add(relationshipDataItem);

        relationshipDataItem.setRelationshipKey(SERVICE_TYPE);
        relationshipDataItem.setRelationshipValue(MSO_DEV_SERVICE_TYPE);
        relationship.getRelationshipData().add(relationshipDataItem);

        relationshipDataItem.setRelationshipKey(SERVICE_INSTANCE_ID);
        relationshipDataItem.setRelationshipValue(SERVICE_INSTANCE_UUID);
        relationship.getRelationshipData().add(relationshipDataItem);

        RelatedToProperty item = new RelatedToProperty();
        item.setPropertyKey(SERVICE_INSTANCE_NAME);
        item.setPropertyValue(SERVICE_INSTANCE_NAME_TEXT);
        relationship.getRelatedToProperty().add(item);

        relationship.setRelatedTo(SERVICE_INSTANCE);
        relationship.setRelatedLink(
                AAI_SERVICE_SUBSCRIPTION_URI
                        + MSO_SERVICE_INSTANCE_URI
                        + SERVICE_INSTANCE_UUID);

        relationshipList.getRelationships().add(relationship);
        response.setRelationshipList(relationshipList);

        return response;
    }

    /**
     * Simulate a response.
     */
    public static AaiGetVnfResponse getQueryByVnfName2() {
        AaiGetVnfResponse response = new AaiGetVnfResponse();

        response.setVnfId(VNF_UUID);
        response.setVnfName(VNF_NAME_TEXT);
        response.setVnfType("Basa-122216-Service/VidVsamp12BaseVolume 1");
        response.setServiceId("a9a77d5a-123e-4ca2-9eb9-0b015d2ee0fb");
        response.setOrchestrationStatus("Created");
        response.setInMaint(false);
        response.setIsClosedLoopDisabled(false);
        response.setResourceVersion("1494001988835");
        response.setModelInvariantId("f18be3cd-d446-456e-9109-121d9b62feaa");

        final RelationshipList relationshipList = new RelationshipList();
        final Relationship relationship = new Relationship();
        RelationshipData relationshipDataItem = new RelationshipData();

        relationshipDataItem.setRelationshipKey(MSO_CUSTOMER_ID);
        relationshipDataItem.setRelationshipValue(MSO_1610_ST);
        relationship.getRelationshipData().add(relationshipDataItem);

        relationshipDataItem.setRelationshipKey(SERVICE_TYPE);
        relationshipDataItem.setRelationshipValue(MSO_DEV_SERVICE_TYPE);
        relationship.getRelationshipData().add(relationshipDataItem);

        relationshipDataItem.setRelationshipKey(SERVICE_INSTANCE_ID);
        relationshipDataItem.setRelationshipValue(SERVICE_INSTANCE_UUID);
        relationship.getRelationshipData().add(relationshipDataItem);

        RelatedToProperty item = new RelatedToProperty();
        item.setPropertyKey(SERVICE_INSTANCE_NAME);
        item.setPropertyValue(SERVICE_INSTANCE_NAME_TEXT);
        relationship.getRelatedToProperty().add(item);

        relationship.setRelatedTo(SERVICE_INSTANCE);
        relationship.setRelatedLink(
                AAI_SERVICE_SUBSCRIPTION_URI
                        + MSO_SERVICE_INSTANCE_URI
                        + SERVICE_INSTANCE_UUID);

        relationshipList.getRelationships().add(relationship);
        response.setRelationshipList(relationshipList);

        return response;
    }

    /**
     * Simulate a response.
     */
    public static AaiGetVserverResponse getQueryByVserverName2() {
        final AaiGetVserverResponse response = new AaiGetVserverResponse();

        AaiNqVServer svr = new AaiNqVServer();

        svr.setVserverId("d0668d4f-c25e-4a1b-87c4-83845c01efd8");
        svr.setVserverName("USMSO1SX7NJ0103UJZZ01-vjunos0");
        svr.setVserverName2("vjunos0");
        svr.setVserverSelflink(
                "https://aai-ext1.test.att.com:8443/aai/v7/cloud-infrastructure/cloud-regions/cloud-region/att-aic/AAIAIC25/tenants/tenant/USMSO1SX7NJ0103UJZZ01%3A%3AuCPE-VMS/vservers/vserver/d0668d4f-c25e-4a1b-87c4-83845c01efd8");
        svr.setInMaint(false);
        svr.setIsClosedLoopDisabled(false);
        svr.setResourceVersion("1494001931513");

        final RelationshipList relationshipList = new RelationshipList();
        final Relationship relationship = new Relationship();
        RelationshipData relationshipDataItem = new RelationshipData();

        relationshipDataItem.setRelationshipKey(MSO_CUSTOMER_ID);
        relationshipDataItem.setRelationshipValue(MSO_1610_ST);
        relationship.getRelationshipData().add(relationshipDataItem);

        relationshipDataItem.setRelationshipKey(SERVICE_TYPE);
        relationshipDataItem.setRelationshipValue(MSO_DEV_SERVICE_TYPE);
        relationship.getRelationshipData().add(relationshipDataItem);

        relationshipDataItem.setRelationshipKey(SERVICE_INSTANCE_ID);
        relationshipDataItem.setRelationshipValue(SERVICE_INSTANCE_UUID);
        relationship.getRelationshipData().add(relationshipDataItem);

        RelatedToProperty item = new RelatedToProperty();
        item.setPropertyKey(SERVICE_INSTANCE_NAME);
        item.setPropertyValue(SERVICE_INSTANCE_NAME_TEXT);
        relationship.getRelatedToProperty().add(item);

        relationship.setRelatedTo(SERVICE_INSTANCE);
        relationship.setRelatedLink(
                AAI_SERVICE_SUBSCRIPTION_URI
                        + MSO_SERVICE_INSTANCE_URI
                        + SERVICE_INSTANCE_UUID);

        relationshipList.getRelationships().add(relationship);
        svr.setRelationshipList(relationshipList);

        response.getVserver().add(svr);

        return response;
    }

    @Test
    public void testMethods() {
        UUID requestId = UUID.randomUUID();
        ControlLoopEventManager clem = new ControlLoopEventManager("MyClosedLoopName", requestId);

        assertEquals("MyClosedLoopName", clem.getClosedLoopControlName());
        assertEquals(requestId, clem.getRequestId());

        clem.setActivated(true);
        assertEquals(true, clem.isActivated());

        clem.setControlLoopResult("SUCCESS");
        assertEquals("SUCCESS", clem.getControlLoopResult());

        clem.setControlLoopTimedOut();
        assertEquals(true, clem.isControlLoopTimedOut());

        clem.setNumAbatements(12345);
        assertEquals(Integer.valueOf(12345), clem.getNumAbatements());

        clem.setNumOnsets(54321);
        assertEquals(Integer.valueOf(54321), clem.getNumOnsets());

        assertNull(clem.getOnsetEvent());
        assertNull(clem.getAbatementEvent());
        assertNull(clem.getProcessor());

        assertEquals(true, clem.isActive());
        assertEquals(false, clem.releaseLock());
        assertEquals(true, clem.isControlLoopTimedOut());

        assertNull(clem.unlockCurrentOperation());
    }

    @Test
    public void testAlreadyActivated() {
        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(TWO_ONSET_TEST);
        event.setRequestId(requestId);
        event.setTarget(VNF_ID);
        event.setClosedLoopAlarmStart(Instant.now());
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event.setAai(new HashMap<>());
        event.getAai().put(VNF_NAME, ONSET_ONE);

        ControlLoopEventManager manager = makeManager(event);
        manager.setActivated(true);
        VirtualControlLoopNotification notification = manager.activate(event);
        assertEquals(ControlLoopNotificationType.REJECTED, notification.getNotification());
    }

    @Test
    public void testActivationYaml() throws IOException {
        InputStream is = new FileInputStream(new File(TEST_YAML));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        InputStream isBad = new FileInputStream(new File("src/test/resources/notutf8.yaml"));
        final String yamlStringBad = IOUtils.toString(isBad, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(TWO_ONSET_TEST);
        event.setRequestId(requestId);
        event.setTarget(VNF_ID);
        event.setClosedLoopAlarmStart(Instant.now());
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event.setAai(new HashMap<>());
        event.getAai().put(VNF_NAME, ONSET_ONE);

        ControlLoopEventManager manager = makeManager(event);

        // Null YAML should fail
        VirtualControlLoopNotification notificationNull = manager.activate(null, event);
        assertNotNull(notificationNull);
        assertEquals(ControlLoopNotificationType.REJECTED, notificationNull.getNotification());

        // Empty YAML should fail
        VirtualControlLoopNotification notificationEmpty = manager.activate("", event);
        assertNotNull(notificationEmpty);
        assertEquals(ControlLoopNotificationType.REJECTED, notificationEmpty.getNotification());

        // Bad YAML should fail
        VirtualControlLoopNotification notificationBad = manager.activate(yamlStringBad, event);
        assertNotNull(notificationBad);
        assertEquals(ControlLoopNotificationType.REJECTED, notificationBad.getNotification());

        VirtualControlLoopNotification notification = manager.activate(yamlString, event);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        // Another activate should fail
        VirtualControlLoopNotification notificationActive = manager.activate(yamlString, event);
        assertNotNull(notificationActive);
        assertEquals(ControlLoopNotificationType.REJECTED, notificationActive.getNotification());
    }

    @Test
    public void testControlLoopFinal() throws Exception {
        InputStream is = new FileInputStream(new File(TEST_YAML));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(TWO_ONSET_TEST);
        event.setRequestId(requestId);
        event.setTarget(VNF_ID);
        event.setClosedLoopAlarmStart(Instant.now());
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event.setAai(new HashMap<>());
        event.getAai().put(VNF_NAME, ONSET_ONE);

        ControlLoopEventManager manager = makeManager(event);
        ControlLoopEventManager manager2 = manager;
        assertThatThrownBy(manager2::isControlLoopFinal).isInstanceOf(ControlLoopException.class)
                        .hasMessage("ControlLoopEventManager MUST be activated first.");

        manager.setActivated(true);
        assertThatThrownBy(manager2::isControlLoopFinal).isInstanceOf(ControlLoopException.class)
                        .hasMessage("No onset event for ControlLoopEventManager.");

        manager.setActivated(false);
        VirtualControlLoopNotification notification = manager.activate(yamlString, event);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        VirtualControlLoopNotification clfNotification = manager.isControlLoopFinal();
        assertNull(clfNotification);

        // serialize and de-serialize manager
        manager = Serializer.roundTrip(manager);

        manager.getProcessor().nextPolicyForResult(PolicyResult.SUCCESS);
        clfNotification = manager.isControlLoopFinal();
        assertNotNull(clfNotification);
        assertEquals(ControlLoopNotificationType.FINAL_SUCCESS, clfNotification.getNotification());

        manager = new ControlLoopEventManager(event.getClosedLoopControlName(), event.getRequestId());
        notification = manager.activate(yamlString, event);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        manager.getProcessor().nextPolicyForResult(PolicyResult.FAILURE_EXCEPTION);
        clfNotification = manager.isControlLoopFinal();
        assertNotNull(clfNotification);
        assertEquals(ControlLoopNotificationType.FINAL_FAILURE, clfNotification.getNotification());

        manager = new ControlLoopEventManager(event.getClosedLoopControlName(), event.getRequestId());
        notification = manager.activate(yamlString, event);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        manager.getProcessor().nextPolicyForResult(PolicyResult.FAILURE_GUARD);
        clfNotification = manager.isControlLoopFinal();
        assertNotNull(clfNotification);
        assertEquals(ControlLoopNotificationType.FINAL_FAILURE, clfNotification.getNotification());

        manager.setControlLoopTimedOut();
        clfNotification = manager.isControlLoopFinal();
        assertNotNull(clfNotification);
        assertEquals(ControlLoopNotificationType.FINAL_FAILURE, clfNotification.getNotification());
    }

    @Test
    public void testProcessControlLoop() throws Exception {
        InputStream is = new FileInputStream(new File(TEST_YAML));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(TWO_ONSET_TEST);
        event.setRequestId(requestId);
        event.setTarget(VNF_ID);
        event.setClosedLoopAlarmStart(Instant.now());
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event.setAai(new HashMap<>());
        event.getAai().put(VNF_NAME, ONSET_ONE);

        ControlLoopEventManager manager = makeManager(event);
        ControlLoopEventManager manager2 = manager;
        assertThatThrownBy(manager2::processControlLoop).isInstanceOf(ControlLoopException.class)
                        .hasMessage("ControlLoopEventManager MUST be activated first.");

        manager.setActivated(true);
        assertThatThrownBy(manager2::processControlLoop).isInstanceOf(ControlLoopException.class)
                        .hasMessage("No onset event for ControlLoopEventManager.");

        manager.setActivated(false);
        VirtualControlLoopNotification notification = manager.activate(yamlString, event);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        ControlLoopOperationManager clom = manager.processControlLoop();
        assertNotNull(clom);
        assertNull(clom.getOperationResult());

        // serialize and de-serialize manager
        manager = Serializer.roundTrip(manager);

        // Test operation in progress
        ControlLoopEventManager manager3 = manager;
        assertThatThrownBy(manager3::processControlLoop).isInstanceOf(ControlLoopException.class)
                        .hasMessage("Already working an Operation, do not call this method.");

        manager = new ControlLoopEventManager(event.getClosedLoopControlName(), event.getRequestId());
        notification = manager.activate(yamlString, event);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        manager.getProcessor().nextPolicyForResult(PolicyResult.FAILURE_GUARD);
        VirtualControlLoopNotification clfNotification = manager.isControlLoopFinal();
        assertNotNull(clfNotification);
        assertEquals(ControlLoopNotificationType.FINAL_FAILURE, clfNotification.getNotification());

        // Test operation completed
        ControlLoopEventManager manager4 = manager;
        assertThatThrownBy(manager4::processControlLoop).isInstanceOf(ControlLoopException.class)
                        .hasMessage("Control Loop is in FINAL state, do not call this method.");

        manager = new ControlLoopEventManager(event.getClosedLoopControlName(), event.getRequestId());
        notification = manager.activate(yamlString, event);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());
        manager.getProcessor().nextPolicyForResult(PolicyResult.FAILURE);

        // Test operation with no next policy defined
        ControlLoopEventManager manager5 = manager;
        assertThatThrownBy(manager5::processControlLoop).isInstanceOf(ControlLoopException.class)
                        .hasMessage("The target type is null");
    }

    @Test
    public void testFinishOperation() throws Exception {
        InputStream is = new FileInputStream(new File("src/test/resources/testSOactor.yaml"));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        InputStream isStd = new FileInputStream(new File(TEST_YAML));
        final String yamlStringStd = IOUtils.toString(isStd, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(TWO_ONSET_TEST);
        event.setRequestId(requestId);
        event.setTarget(VNF_ID);
        event.setClosedLoopAlarmStart(Instant.now());
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event.setAai(new HashMap<>());
        event.getAai().put(VNF_ID, ONSET_ONE);

        ControlLoopEventManager manager = makeManager(event);
        ControlLoopEventManager manager2 = manager;
        assertThatThrownBy(() -> manager2.finishOperation(null)).isInstanceOf(ControlLoopException.class)
                        .hasMessage("No operation to finish.");

        manager.setActivated(true);
        assertThatThrownBy(() -> manager2.finishOperation(null)).isInstanceOf(ControlLoopException.class)
                        .hasMessage("No operation to finish.");

        manager.setActivated(false);
        VirtualControlLoopNotification notification = manager.activate(yamlString, event);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        assertThatThrownBy(manager2::lockCurrentOperation).isInstanceOf(ControlLoopException.class)
                        .hasMessage("Do not have a current operation.");

        assertNull(manager.unlockCurrentOperation());

        // serialize and de-serialize manager
        manager = Serializer.roundTrip(manager);

        ControlLoopOperationManager clom = manager.processControlLoop();
        assertNotNull(clom);
        assertNull(clom.getOperationResult());

        LockResult<GuardResult, TargetLock> lockLock = manager.lockCurrentOperation();
        assertNotNull(lockLock);
        assertEquals(GuardResult.LOCK_ACQUIRED, lockLock.getA());

        LockResult<GuardResult, TargetLock> lockLockAgain = manager.lockCurrentOperation();
        assertNotNull(lockLockAgain);
        assertEquals(GuardResult.LOCK_ACQUIRED, lockLockAgain.getA());
        assertEquals(lockLock.getB(), lockLockAgain.getB());

        assertEquals(lockLock.getB(), manager.unlockCurrentOperation());
        assertNull(manager.unlockCurrentOperation());

        lockLock = manager.lockCurrentOperation();
        assertNotNull(lockLock);
        PolicyGuard.unlockTarget(lockLock.getB());
        assertEquals(lockLock.getB(), manager.unlockCurrentOperation());

        clom.startOperation(event);

        // This call should be exception free
        manager.finishOperation(clom);

        ControlLoopEventManager otherManager = makeManager(event);
        VirtualControlLoopNotification otherNotification = otherManager.activate(yamlStringStd, event);
        assertNotNull(otherNotification);
        assertEquals(ControlLoopNotificationType.ACTIVE, otherNotification.getNotification());

        ControlLoopOperationManager otherClom = otherManager.processControlLoop();
        assertNotNull(otherClom);
        assertNull(otherClom.getOperationResult());

        otherManager.finishOperation(clom);
    }

    @Test
    public void testOnNewEvent() throws Exception {
        InputStream is = new FileInputStream(new File(TEST_YAML));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
        onsetEvent.setClosedLoopControlName(TWO_ONSET_TEST);
        onsetEvent.setRequestId(requestId);
        onsetEvent.setTarget(VNF_ID);
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put(VNF_NAME, ONSET_ONE);

        VirtualControlLoopEvent abatedEvent = new VirtualControlLoopEvent();
        abatedEvent.setClosedLoopControlName(TWO_ONSET_TEST);
        abatedEvent.setRequestId(requestId);
        abatedEvent.setTarget(VNF_ID);
        abatedEvent.setClosedLoopAlarmStart(Instant.now());
        abatedEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ABATED);
        abatedEvent.setAai(new HashMap<>());
        abatedEvent.getAai().put(VNF_NAME, ONSET_ONE);

        ControlLoopEventManager manager = makeManager(onsetEvent);
        VirtualControlLoopNotification notification = manager.activate(yamlString, onsetEvent);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        assertEquals(NewEventStatus.FIRST_ONSET, manager.onNewEvent(onsetEvent));
        assertEquals(NewEventStatus.FIRST_ABATEMENT, manager.onNewEvent(abatedEvent));
        assertEquals(NewEventStatus.SUBSEQUENT_ABATEMENT, manager.onNewEvent(abatedEvent));

        VirtualControlLoopEvent checkSyntaxEvent = new VirtualControlLoopEvent();
        checkSyntaxEvent.setAai(null);
        checkSyntaxEvent.setClosedLoopAlarmEnd(null);
        checkSyntaxEvent.setClosedLoopAlarmStart(null);
        checkSyntaxEvent.setClosedLoopControlName(null);
        checkSyntaxEvent.setClosedLoopEventClient(null);
        checkSyntaxEvent.setClosedLoopEventStatus(null);
        checkSyntaxEvent.setFrom(null);
        checkSyntaxEvent.setPolicyName(null);
        checkSyntaxEvent.setPolicyScope(null);
        checkSyntaxEvent.setPolicyVersion(null);
        checkSyntaxEvent.setRequestId(null);
        checkSyntaxEvent.setTarget(null);
        checkSyntaxEvent.setTargetType(null);
        checkSyntaxEvent.setVersion(null);

        assertEquals(NewEventStatus.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        assertEquals(NewEventStatus.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setClosedLoopControlName(null);
        assertEquals(NewEventStatus.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setClosedLoopControlName("");
        assertEquals(NewEventStatus.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setClosedLoopControlName(TWO_ONSET_TEST);
        assertEquals(NewEventStatus.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setRequestId(null);
        assertEquals(NewEventStatus.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setRequestId(requestId);
        assertEquals(NewEventStatus.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setAai(null);
        assertEquals(NewEventStatus.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setAai(new HashMap<>());
        assertEquals(NewEventStatus.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setTarget("");
        assertEquals(NewEventStatus.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setTarget(null);
        assertEquals(NewEventStatus.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setTarget("");
        assertEquals(NewEventStatus.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setTarget("OZ");
        assertEquals(NewEventStatus.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setTarget("VM_NAME");
        assertEquals(NewEventStatus.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setTarget("VNF_NAME");
        assertEquals(NewEventStatus.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setTarget(VSERVER_NAME);
        assertEquals(NewEventStatus.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setTarget(VNF_ID);
        assertEquals(NewEventStatus.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setTarget(VNF_NAME);
        assertEquals(NewEventStatus.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setAai(null);
        assertEquals(NewEventStatus.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setAai(new HashMap<>());
        assertEquals(NewEventStatus.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.getAai().put(VNF_NAME, ONSET_ONE);
        assertEquals(NewEventStatus.SUBSEQUENT_ABATEMENT, manager.onNewEvent(abatedEvent));

        checkSyntaxEvent.getAai().put(VSERVER_NAME, ONSET_ONE);
        assertEquals(NewEventStatus.SUBSEQUENT_ABATEMENT, manager.onNewEvent(abatedEvent));

        checkSyntaxEvent.getAai().put(VNF_ID, ONSET_ONE);
        assertEquals(NewEventStatus.SUBSEQUENT_ABATEMENT, manager.onNewEvent(abatedEvent));
    }

    @Test
    public void testControlLoopTimeout() throws IOException {
        InputStream is = new FileInputStream(new File(TEST_YAML));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
        onsetEvent.setClosedLoopControlName(TWO_ONSET_TEST);
        onsetEvent.setRequestId(requestId);
        onsetEvent.setTarget(VNF_ID);
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put(VNF_NAME, ONSET_ONE);

        ControlLoopEventManager manager = makeManager(onsetEvent);
        assertTrue(0 == manager.getControlLoopTimeout(null));
        assertTrue(120 == manager.getControlLoopTimeout(120));

        VirtualControlLoopNotification notification = manager.activate(yamlString, onsetEvent);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        assertEquals(60, manager.getControlLoopTimeout(null));
    }

    @Test
    public void testControlLoopTimeout_ZeroTimeout() throws IOException {
        InputStream is = new FileInputStream(new File("src/test/resources/test-zero-timeout.yaml"));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
        onsetEvent.setClosedLoopControlName(TWO_ONSET_TEST);
        onsetEvent.setRequestId(requestId);
        onsetEvent.setTarget(VNF_ID);
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put(VNF_NAME, ONSET_ONE);

        ControlLoopEventManager manager = makeManager(onsetEvent);

        VirtualControlLoopNotification notification = manager.activate(yamlString, onsetEvent);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        assertTrue(0 == manager.getControlLoopTimeout(null));
        assertTrue(120 == manager.getControlLoopTimeout(120));
    }

    @Test
    public void testControlLoopTimeout_NullTimeout() throws IOException {
        InputStream is = new FileInputStream(new File("src/test/resources/test-null-timeout.yaml"));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
        onsetEvent.setClosedLoopControlName(TWO_ONSET_TEST);
        onsetEvent.setRequestId(requestId);
        onsetEvent.setTarget(VNF_ID);
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put(VNF_NAME, ONSET_ONE);

        ControlLoopEventManager manager = makeManager(onsetEvent);

        VirtualControlLoopNotification notification = manager.activate(yamlString, onsetEvent);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        assertTrue(0 == manager.getControlLoopTimeout(null));
        assertTrue(120 == manager.getControlLoopTimeout(120));
    }

    @Test
    public void testQueryAai_AlreadyDisabled() throws AaiException {
        onset.getAai().put(ControlLoopEventManager.GENERIC_VNF_IS_CLOSED_LOOP_DISABLED, Boolean.TRUE.toString());
        onset.getAai().put(ControlLoopEventManager.GENERIC_VNF_PROV_STATUS, ControlLoopEventManager.PROV_STATUS_ACTIVE);

        ControlLoopEventManager mgr = makeManager(onset);

        assertThatThrownBy(() -> mgr.queryAai(onset)).isInstanceOf(AaiException.class)
                        .hasMessage("is-closed-loop-disabled is set to true on VServer or VNF");
        assertNull(mgr.getVnfResponse());
        assertNull(mgr.getVserverResponse());
    }

    @Test
    public void testQueryAai_AlreadyInactive() throws AaiException {
        onset.getAai().put(ControlLoopEventManager.GENERIC_VNF_IS_CLOSED_LOOP_DISABLED, Boolean.FALSE.toString());
        onset.getAai().put(ControlLoopEventManager.GENERIC_VNF_PROV_STATUS, "not-active2");

        ControlLoopEventManager mgr = makeManager(onset);

        assertThatThrownBy(() -> mgr.queryAai(onset)).isInstanceOf(AaiException.class)
                        .hasMessage("prov-status is not ACTIVE on VServer or VNF");
        assertNull(mgr.getVnfResponse());
        assertNull(mgr.getVserverResponse());
    }

    @Test
    public void testQueryAai_QueryVnfById() throws AaiException {
        ControlLoopEventManager mgr = null;

        mgr = makeManager(onset);
        mgr.queryAai(onset);

        assertNotNull(mgr.getVnfResponse());
        assertNull(mgr.getVserverResponse());

        AaiGetVnfResponse vnfresp = mgr.getVnfResponse();

        // should not re-query
        mgr.queryAai(onset);

        assertEquals(vnfresp, mgr.getVnfResponse());
        assertNull(mgr.getVserverResponse());
    }

    @Test
    public void testQueryAai_QueryVnfByName() throws AaiException {
        ControlLoopEventManager mgr = null;

        // vnf query by name
        onset.getAai().remove(ControlLoopEventManager.GENERIC_VNF_VNF_ID);
        onset.getAai().put(ControlLoopEventManager.GENERIC_VNF_VNF_NAME, "AVNFName");

        mgr = makeManager(onset);
        mgr.queryAai(onset);

        assertNotNull(mgr.getVnfResponse());
        assertNull(mgr.getVserverResponse());

        AaiGetVnfResponse vnfresp = mgr.getVnfResponse();

        // should not re-query
        mgr.queryAai(onset);

        assertEquals(vnfresp, mgr.getVnfResponse());
        assertNull(mgr.getVserverResponse());
    }

    @Test
    public void testQueryAai_QueryVnfById_Disabled() {
        onset.getAai().put(ControlLoopEventManager.GENERIC_VNF_VNF_ID, "disableClosedLoop");

        ControlLoopEventManager mgr = makeManager(onset);

        assertThatThrownBy(() -> mgr.queryAai(onset)).isInstanceOf(AaiException.class)
                        .hasMessage("is-closed-loop-disabled is set to true (query by vnf-id)");

        assertNotNull(mgr.getVnfResponse());
        assertNull(mgr.getVserverResponse());
    }

    @Test
    public void testQueryAai_QueryVserver() throws AaiException {
        onset.getAai().remove(ControlLoopEventManager.GENERIC_VNF_VNF_ID);
        onset.getAai().put(ControlLoopEventManager.VSERVER_VSERVER_NAME, "AVserver");

        ControlLoopEventManager mgr = makeManager(onset);
        mgr.queryAai(onset);

        assertNull(mgr.getVnfResponse());
        assertNotNull(mgr.getVserverResponse());

        AaiGetVserverResponse vsvresp = mgr.getVserverResponse();

        // should not re-query
        mgr.queryAai(onset);

        assertNull(mgr.getVnfResponse());
        assertEquals(vsvresp, mgr.getVserverResponse());
    }

    @Test
    public void testQueryAai_QueryVserver_Disabled() {
        onset.getAai().remove(ControlLoopEventManager.GENERIC_VNF_VNF_ID);
        onset.getAai().put(ControlLoopEventManager.VSERVER_VSERVER_NAME, "disableClosedLoop");

        ControlLoopEventManager mgr = makeManager(onset);

        assertThatThrownBy(() -> mgr.queryAai(onset)).isInstanceOf(AaiException.class)
                        .hasMessage("is-closed-loop-disabled is set to true (query by vserver-name)");

        assertNull(mgr.getVnfResponse());
        assertNotNull(mgr.getVserverResponse());
    }

    @Test(expected = AaiException.class)
    public void testQueryAai_QueryException() throws AaiException {
        // Force AAI errors
        PolicyEngineConstants.getManager().setEnvironmentProperty(AAI_URL, INVALID_URL);

        makeManager(onset).queryAai(onset);
    }

    @Test
    public void testProcessVnfResponse_Success() throws Exception {
        AaiGetVnfResponse resp = new AaiGetVnfResponse();
        resp.setIsClosedLoopDisabled(false);
        resp.setProvStatus(ControlLoopEventManager.PROV_STATUS_ACTIVE);
        Whitebox.invokeMethod(ControlLoopEventManager.class, PROCESS_VNF_RESPONSE_METHOD_NAME, resp, true);
    }

    @Test
    public void testProcessVnfResponse_NullResponse() throws Exception {
        thrown.expect(AaiException.class);
        thrown.expectMessage("AAI Response is null (query by vnf-id)");

        AaiGetVnfResponse resp = null;
        Whitebox.invokeMethod(ControlLoopEventManager.class, PROCESS_VNF_RESPONSE_METHOD_NAME, resp, true);
    }

    @Test
    public void testProcessVnfResponse_Error() throws Exception {
        thrown.expect(AaiException.class);
        thrown.expectMessage("AAI Responded with a request error (query by vnf-name)");

        AaiGetVnfResponse resp = new AaiGetVnfResponse();

        resp.setRequestError(new AaiNqRequestError());

        resp.setIsClosedLoopDisabled(false);
        resp.setProvStatus(ControlLoopEventManager.PROV_STATUS_ACTIVE);
        Whitebox.invokeMethod(ControlLoopEventManager.class, PROCESS_VNF_RESPONSE_METHOD_NAME, resp, false);
    }

    @Test
    public void testProcessVnfResponse_Disabled() throws Exception {
        thrown.expect(AaiException.class);
        thrown.expectMessage("is-closed-loop-disabled is set to true (query by vnf-id)");

        AaiGetVnfResponse resp = new AaiGetVnfResponse();
        resp.setIsClosedLoopDisabled(true);
        resp.setProvStatus(ControlLoopEventManager.PROV_STATUS_ACTIVE);
        Whitebox.invokeMethod(ControlLoopEventManager.class, PROCESS_VNF_RESPONSE_METHOD_NAME, resp, true);
    }

    @Test
    public void testProcessVnfResponse_Inactive() throws Exception {
        thrown.expect(AaiException.class);
        thrown.expectMessage("prov-status is not ACTIVE (query by vnf-name)");

        AaiGetVnfResponse resp = new AaiGetVnfResponse();
        resp.setIsClosedLoopDisabled(false);
        resp.setProvStatus("inactive1");
        Whitebox.invokeMethod(ControlLoopEventManager.class, PROCESS_VNF_RESPONSE_METHOD_NAME, resp, false);
    }

    @Test
    public void testProcessVserverResponse_Success() throws Exception {
        AaiGetVserverResponse resp = new AaiGetVserverResponse();

        AaiNqVServer svr = new AaiNqVServer();
        resp.getVserver().add(svr);

        svr.setIsClosedLoopDisabled(false);
        svr.setProvStatus(ControlLoopEventManager.PROV_STATUS_ACTIVE);
        Whitebox.invokeMethod(ControlLoopEventManager.class, PROCESS_VSERVER_RESPONSE, resp);
    }

    @Test
    public void testProcessVserverResponse_NullResponse() throws Exception {
        thrown.expect(AaiException.class);
        thrown.expectMessage("AAI Response is null (query by vserver-name)");

        AaiGetVserverResponse resp = null;
        Whitebox.invokeMethod(ControlLoopEventManager.class, PROCESS_VSERVER_RESPONSE, resp);
    }

    @Test
    public void testProcessVserverResponse_Error() throws Exception {
        thrown.expect(AaiException.class);
        thrown.expectMessage("AAI Responded with a request error (query by vserver-name)");

        AaiGetVserverResponse resp = new AaiGetVserverResponse();

        resp.setRequestError(new AaiNqRequestError());

        AaiNqVServer svr = new AaiNqVServer();
        resp.getVserver().add(svr);

        svr.setIsClosedLoopDisabled(false);
        svr.setProvStatus(ControlLoopEventManager.PROV_STATUS_ACTIVE);

        Whitebox.invokeMethod(ControlLoopEventManager.class, PROCESS_VSERVER_RESPONSE, resp);
    }

    @Test
    public void testProcessVserverResponse_Disabled() throws Exception {
        thrown.expect(AaiException.class);
        thrown.expectMessage("is-closed-loop-disabled is set to true (query by vserver-name)");

        AaiGetVserverResponse resp = new AaiGetVserverResponse();
        AaiNqVServer svr = new AaiNqVServer();
        resp.getVserver().add(svr);

        svr.setIsClosedLoopDisabled(true);
        svr.setProvStatus(ControlLoopEventManager.PROV_STATUS_ACTIVE);
        Whitebox.invokeMethod(ControlLoopEventManager.class, PROCESS_VSERVER_RESPONSE, resp);
    }

    @Test
    public void testProcessVserverResponse_Inactive() throws Exception {
        thrown.expect(AaiException.class);
        thrown.expectMessage("prov-status is not ACTIVE (query by vserver-name)");

        AaiGetVserverResponse resp = new AaiGetVserverResponse();
        AaiNqVServer svr = new AaiNqVServer();
        resp.getVserver().add(svr);

        svr.setIsClosedLoopDisabled(false);
        svr.setProvStatus("inactive1");
        Whitebox.invokeMethod(ControlLoopEventManager.class, PROCESS_VSERVER_RESPONSE, resp);
    }

    @Test
    public void testIsClosedLoopDisabled() {
        Map<String, String> aai = onset.getAai();

        // null, null
        aai.remove(ControlLoopEventManager.GENERIC_VNF_IS_CLOSED_LOOP_DISABLED);
        aai.remove(ControlLoopEventManager.VSERVER_IS_CLOSED_LOOP_DISABLED);
        assertFalse(ControlLoopEventManager.isClosedLoopDisabled(onset));

        // null, false
        aai.remove(ControlLoopEventManager.GENERIC_VNF_IS_CLOSED_LOOP_DISABLED);
        aai.put(ControlLoopEventManager.VSERVER_IS_CLOSED_LOOP_DISABLED, Boolean.FALSE.toString());
        assertFalse(ControlLoopEventManager.isClosedLoopDisabled(onset));

        // false, null
        aai.put(ControlLoopEventManager.GENERIC_VNF_IS_CLOSED_LOOP_DISABLED, Boolean.FALSE.toString());
        aai.remove(ControlLoopEventManager.VSERVER_IS_CLOSED_LOOP_DISABLED);
        assertFalse(ControlLoopEventManager.isClosedLoopDisabled(onset));

        // null, true
        aai.remove(ControlLoopEventManager.GENERIC_VNF_IS_CLOSED_LOOP_DISABLED);
        aai.put(ControlLoopEventManager.VSERVER_IS_CLOSED_LOOP_DISABLED, Boolean.TRUE.toString());
        assertTrue(ControlLoopEventManager.isClosedLoopDisabled(onset));

        // true, null
        aai.put(ControlLoopEventManager.GENERIC_VNF_IS_CLOSED_LOOP_DISABLED, Boolean.TRUE.toString());
        aai.remove(ControlLoopEventManager.VSERVER_IS_CLOSED_LOOP_DISABLED);
        assertTrue(ControlLoopEventManager.isClosedLoopDisabled(onset));
    }

    @Test
    public void testIsProvStatusInactive() {
        Map<String, String> aai = onset.getAai();

        // null, null
        aai.remove(ControlLoopEventManager.GENERIC_VNF_PROV_STATUS);
        aai.remove(ControlLoopEventManager.VSERVER_PROV_STATUS);
        assertFalse(ControlLoopEventManager.isProvStatusInactive(onset));

        // null, active
        aai.remove(ControlLoopEventManager.GENERIC_VNF_PROV_STATUS);
        aai.put(ControlLoopEventManager.VSERVER_PROV_STATUS, ControlLoopEventManager.PROV_STATUS_ACTIVE);
        assertFalse(ControlLoopEventManager.isProvStatusInactive(onset));

        // active, null
        aai.put(ControlLoopEventManager.GENERIC_VNF_PROV_STATUS, ControlLoopEventManager.PROV_STATUS_ACTIVE);
        aai.remove(ControlLoopEventManager.VSERVER_PROV_STATUS);
        assertFalse(ControlLoopEventManager.isProvStatusInactive(onset));

        // null, inactive
        aai.remove(ControlLoopEventManager.GENERIC_VNF_PROV_STATUS);
        aai.put(ControlLoopEventManager.VSERVER_PROV_STATUS, "other1");
        assertTrue(ControlLoopEventManager.isProvStatusInactive(onset));

        // inactive, null
        aai.put(ControlLoopEventManager.GENERIC_VNF_PROV_STATUS, "other2");
        aai.remove(ControlLoopEventManager.VSERVER_PROV_STATUS);
        assertTrue(ControlLoopEventManager.isProvStatusInactive(onset));
    }

    @Test
    public void testIsAaiTrue() {
        assertTrue(ControlLoopEventManager.isAaiTrue("tRuE"));
        assertTrue(ControlLoopEventManager.isAaiTrue("T"));
        assertTrue(ControlLoopEventManager.isAaiTrue("t"));
        assertTrue(ControlLoopEventManager.isAaiTrue("yES"));
        assertTrue(ControlLoopEventManager.isAaiTrue("Y"));
        assertTrue(ControlLoopEventManager.isAaiTrue("y"));

        assertFalse(ControlLoopEventManager.isAaiTrue("no"));
        assertFalse(ControlLoopEventManager.isAaiTrue(null));
    }

    @Test
    public void testGetNqVserverFromAai() {

        // empty vserver name
        ControlLoopEventManager manager = makeManager(onset);
        manager.activate(onset);
        assertNull(manager.getNqVserverFromAai());


        // re-create manager with a vserver name in the onset
        onset.getAai().put(ControlLoopEventManager.VSERVER_VSERVER_NAME, "my-name");
        manager = makeManager(onset);
        manager.activate(onset);

        AaiNqResponseWrapper resp = manager.getNqVserverFromAai();
        assertNotNull(resp);
        assertEquals(onset.getRequestId(), resp.getRequestId());
        assertNotNull(resp.getAaiNqResponse());
        assertFalse(resp.getAaiNqResponse().getInventoryResponseItems().isEmpty());

        // re-query should return the same object
        assertTrue(resp == manager.getNqVserverFromAai());


        // Force AAI error
        PolicyEngineConstants.getManager().setEnvironmentProperty(AAI_URL, INVALID_URL);

        // re-create manager
        manager = makeManager(onset);
        manager.activate(onset);
        assertNull(manager.getNqVserverFromAai());
    }

    @Test
    public void testGetCqResponseEmptyVserver() throws AaiException {
        ControlLoopEventManager mgr = makeManager(onset);
        mgr.queryAai(onset);

        assertThatThrownBy(() -> mgr.getCqResponse(onset)).isInstanceOf(AaiException.class)
                        .hasMessage("Vserver name is missing");
    }

    @Test
    public void testGetCqResponse() throws AaiException {
        ControlLoopEventManager mgr = makeManager(onset);
        mgr.queryAai(onset);
        onset.getAai().put(VSERVER_NAME, "sample");

        AaiCqResponse aaiCqResponse = mgr.getCqResponse(onset);
        assertNotNull(aaiCqResponse);
    }


    private ControlLoopEventManager makeManager(VirtualControlLoopEvent event) {
        return new ControlLoopEventManager(event.getClosedLoopControlName(), event.getRequestId());
    }
}
