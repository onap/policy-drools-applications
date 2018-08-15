/*-
 * ============LICENSE_START=======================================================
 * unit test
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
import org.onap.policy.aai.AaiGetVnfResponse;
import org.onap.policy.aai.AaiGetVserverResponse;
import org.onap.policy.aai.AaiNqVServer;
import org.onap.policy.aai.AaiNqRequestError;
import org.onap.policy.aai.RelatedToProperty;
import org.onap.policy.aai.Relationship;
import org.onap.policy.aai.RelationshipData;
import org.onap.policy.aai.RelationshipList;
import org.onap.policy.aai.util.AaiException;
import org.onap.policy.common.endpoints.http.server.HttpServletServer;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.Util;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.eventmanager.ControlLoopEventManager.NEW_EVENT_STATUS;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.guard.GuardResult;
import org.onap.policy.guard.PolicyGuard;
import org.onap.policy.guard.PolicyGuard.LockResult;
import org.onap.policy.guard.TargetLock;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlLoopEventManagerTest {
    private static final Logger logger = LoggerFactory.getLogger(ControlLoopEventManagerTest.class);
    
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private VirtualControlLoopEvent onset;

    /**
     * Set up test class.
     */
    @BeforeClass
    public static void setUpSimulator() {
        try {
            org.onap.policy.simulators.Util.buildAaiSim();
        } catch (Exception e) {
            fail(e.getMessage());
        }
        PolicyEngine.manager.setEnvironmentProperty("aai.username", "AAI");
        PolicyEngine.manager.setEnvironmentProperty("aai.password", "AAI");
        PolicyEngine.manager.setEnvironmentProperty("aai.url", "http://localhost:6666");
    }

    @AfterClass
    public static void tearDownSimulator() {
        HttpServletServer.factory.destroy();
    }
    
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
        onset.getAai().put("generic-vnf.vnf-id", "83f674e8-7555-44d7-9a39-bdc3770b0491");
        onset.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        
        // Force AAI errors
        PolicyEngine.manager.setEnvironmentProperty("aai.url", "http://localhost:6666");
    }

    @Test
    public void testAaiVnfInfo() {
        final Util.Pair<ControlLoopPolicy, String> pair = Util.loadYaml("src/test/resources/test.yaml");
        onset.setClosedLoopControlName(pair.key.getControlLoop().getControlLoopName());
        try {
            AaiGetVnfResponse response = getQueryByVnfId2(
                    PolicyEngine.manager.getEnvironmentProperty("aai.url")
                            + "/aai/v11/network/generic-vnfs/generic-vnf/",
                    PolicyEngine.manager.getEnvironmentProperty("aai.username"),
                    PolicyEngine.manager.getEnvironmentProperty("aai.password"), UUID.randomUUID(),
                    "5e49ca06-2972-4532-9ed4-6d071588d792");
            assertNotNull(response);
            logger.info("testAAIVnfInfo test result is " + (response == null ? "null" : "not null"));
        } catch (Exception e) {
            logger.error("testAAIVnfInfo Exception: ", e);
            fail(e.getMessage());
        }
    }

    @Test
    public void testAaiVnfInfo2() {
        final Util.Pair<ControlLoopPolicy, String> pair = Util.loadYaml("src/test/resources/test.yaml");
        onset.setClosedLoopControlName(pair.key.getControlLoop().getControlLoopName());
        try {
            AaiGetVnfResponse response = getQueryByVnfName2(
                    PolicyEngine.manager.getEnvironmentProperty("aai.url")
                            + "/aai/v11/network/generic-vnfs/generic-vnf?vnf-name=",
                    PolicyEngine.manager.getEnvironmentProperty("aai.username"),
                    PolicyEngine.manager.getEnvironmentProperty("aai.password"), UUID.randomUUID(), "lll_vnf_010317");
            assertNotNull(response);
            logger.info("testAAIVnfInfo2 test result is " + (response == null ? "null" : "not null"));
        } catch (Exception e) {
            logger.error("testAAIVnfInfo2 Exception: ", e);
            fail(e.getMessage());
        }
    }

    @Test
    public void testAaiVserver() {
        final Util.Pair<ControlLoopPolicy, String> pair = Util.loadYaml("src/test/resources/test.yaml");
        onset.setClosedLoopControlName(pair.key.getControlLoop().getControlLoopName());
        try {
            AaiGetVserverResponse response = getQueryByVserverName2(
                    PolicyEngine.manager.getEnvironmentProperty("aai.url") + "/aai/v11/nodes/vservers?vserver-name=",
                    PolicyEngine.manager.getEnvironmentProperty("aai.username"),
                    PolicyEngine.manager.getEnvironmentProperty("aai.password"), UUID.randomUUID(),
                    "USMSO1SX7NJ0103UJZZ01-vjunos0");
            assertNotNull(response);
            logger.info("testAAIVserver test result is " + (response == null ? "null" : "not null"));
        } catch (Exception e) {
            logger.error("testAAIVserver Exception: ", e);
            fail(e.getMessage());
        }
    }

    @Test
    public void abatementCheckEventSyntaxTest() {
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName("abatementAAI");
        event.setRequestId(UUID.randomUUID());
        event.setTarget("generic-vnf.vnf-id");
        event.setClosedLoopAlarmStart(Instant.now());
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ABATED);
        ControlLoopEventManager manager = makeManager(event);
        assertNull(manager.getVnfResponse());
        assertNull(manager.getVserverResponse());
        try {
            manager.checkEventSyntax(event);
        } catch (ControlLoopException e) {
            logger.debug("ControlLoopException in abatemetCheckEventSyntaxTest: " + e.getMessage());
            e.printStackTrace();
            fail("Exception in check event syntax");
        }
        assertNull(manager.getVnfResponse());
        assertNull(manager.getVserverResponse());


        event.setAai(new HashMap<>());
        event.getAai().put("generic-vnf.vnf-name", "abatementTest");
        try {
            manager.checkEventSyntax(event);
        } catch (ControlLoopException e) {
            logger.debug("ControlLoopException in abatemetCheckEventSyntaxTest: " + e.getMessage());
            e.printStackTrace();
            fail("Exception in check event syntax");
        }
        assertNull(manager.getVnfResponse());
        assertNull(manager.getVserverResponse());
    }

    @Test
    public void subsequentOnsetTest() {
        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName("TwoOnsetTest");
        event.setRequestId(requestId);
        event.setTarget("generic-vnf.vnf-id");
        event.setClosedLoopAlarmStart(Instant.now());
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event.setAai(new HashMap<>());
        event.getAai().put("generic-vnf.vnf-name", "onsetOne");

        ControlLoopEventManager manager = makeManager(event);
        VirtualControlLoopNotification notification = manager.activate(event);

        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        ControlLoopEventManager.NEW_EVENT_STATUS status = null;
        try {
            status = manager.onNewEvent(event);
        } catch (AaiException e) {
            logger.warn(e.toString());
            fail("A&AI Query Failed");
        }
        assertNotNull(status);
        assertEquals(ControlLoopEventManager.NEW_EVENT_STATUS.FIRST_ONSET, status);

        AaiGetVnfResponse response = manager.getVnfResponse();
        assertNotNull(response);
        assertNull(manager.getVserverResponse());

        VirtualControlLoopEvent event2 = new VirtualControlLoopEvent();
        event2.setClosedLoopControlName("TwoOnsetTest");
        event2.setRequestId(requestId);
        event2.setTarget("generic-vnf.vnf-id");
        event2.setClosedLoopAlarmStart(Instant.now());
        event2.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event2.setAai(new HashMap<>());
        event2.getAai().put("generic-vnf.vnf-name", "onsetTwo");


        try {
            status = manager.onNewEvent(event2);
        } catch (AaiException e) {
            logger.warn(e.toString());
            fail("A&AI Query Failed");
        }
        assertEquals(ControlLoopEventManager.NEW_EVENT_STATUS.SUBSEQUENT_ONSET, status);
        AaiGetVnfResponse response2 = manager.getVnfResponse();
        assertNotNull(response2);
        // We should not have queried AAI, so the stored response should be the same
        assertEquals(response, response2);
        assertNull(manager.getVserverResponse());
    }

    /**
     * Simulate a response.
     */
    public static AaiGetVnfResponse getQueryByVnfId2(String urlGet, String username, String password, UUID requestId,
            String key) {
        AaiGetVnfResponse response = new AaiGetVnfResponse();

        response.setVnfId("83f674e8-7555-44d7-9a39-bdc3770b0491");
        response.setVnfName("lll_vnf_010317");
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

        relationshipDataItem.setRelationshipKey("customer.global-customer-id");
        relationshipDataItem.setRelationshipValue("MSO_1610_ST");
        relationship.getRelationshipData().add(relationshipDataItem);

        relationshipDataItem.setRelationshipKey("service-subscription.service-type");
        relationshipDataItem.setRelationshipValue("MSO-dev-service-type");
        relationship.getRelationshipData().add(relationshipDataItem);

        relationshipDataItem.setRelationshipKey("service-instance.service-instance-id");
        relationshipDataItem.setRelationshipValue("e1e9c97c-02c0-4919-9b4c-eb5d5ef68970");
        relationship.getRelationshipData().add(relationshipDataItem);

        RelatedToProperty item = new RelatedToProperty();
        item.setPropertyKey("service-instance.service-instance-name");
        item.setPropertyValue("lll_svc_010317");
        relationship.getRelatedToProperty().add(item);

        relationship.setRelatedTo("service-instance");
        relationship.setRelatedLink(
                "/aai/v11/business/customers/customer/MSO_1610_ST/service-subscriptions/service-subscription"
                        + "/MSO-dev-service-type/service-instances/service-instance/"
                        + "e1e9c97c-02c0-4919-9b4c-eb5d5ef68970");

        relationshipList.getRelationships().add(relationship);
        response.setRelationshipList(relationshipList);

        return response;
    }

    /**
     * Simulate a response.
     */
    public static AaiGetVnfResponse getQueryByVnfName2(String urlGet, String username, String password, UUID requestId,
            String key) {
        AaiGetVnfResponse response = new AaiGetVnfResponse();

        response.setVnfId("83f674e8-7555-44d7-9a39-bdc3770b0491");
        response.setVnfName("lll_vnf_010317");
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

        relationshipDataItem.setRelationshipKey("customer.global-customer-id");
        relationshipDataItem.setRelationshipValue("MSO_1610_ST");
        relationship.getRelationshipData().add(relationshipDataItem);

        relationshipDataItem.setRelationshipKey("service-subscription.service-type");
        relationshipDataItem.setRelationshipValue("MSO-dev-service-type");
        relationship.getRelationshipData().add(relationshipDataItem);

        relationshipDataItem.setRelationshipKey("service-instance.service-instance-id");
        relationshipDataItem.setRelationshipValue("e1e9c97c-02c0-4919-9b4c-eb5d5ef68970");
        relationship.getRelationshipData().add(relationshipDataItem);

        RelatedToProperty item = new RelatedToProperty();
        item.setPropertyKey("service-instance.service-instance-name");
        item.setPropertyValue("lll_svc_010317");
        relationship.getRelatedToProperty().add(item);

        relationship.setRelatedTo("service-instance");
        relationship.setRelatedLink(
                "/aai/v11/business/customers/customer/MSO_1610_ST/service-subscriptions/service-subscription"
                        + "/MSO-dev-service-type/service-instances/service-instance/"
                        + "e1e9c97c-02c0-4919-9b4c-eb5d5ef68970");

        relationshipList.getRelationships().add(relationship);
        response.setRelationshipList(relationshipList);

        return response;
    }

    /**
     * Simulate a response.
     */
    public static AaiGetVserverResponse getQueryByVserverName2(String urlGet, String username, String password,
            UUID requestId, String key) {
        AaiGetVserverResponse response = new AaiGetVserverResponse();
        
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

        relationshipDataItem.setRelationshipKey("customer.global-customer-id");
        relationshipDataItem.setRelationshipValue("MSO_1610_ST");
        relationship.getRelationshipData().add(relationshipDataItem);

        relationshipDataItem.setRelationshipKey("service-subscription.service-type");
        relationshipDataItem.setRelationshipValue("MSO-dev-service-type");
        relationship.getRelationshipData().add(relationshipDataItem);

        relationshipDataItem.setRelationshipKey("service-instance.service-instance-id");
        relationshipDataItem.setRelationshipValue("e1e9c97c-02c0-4919-9b4c-eb5d5ef68970");
        relationship.getRelationshipData().add(relationshipDataItem);

        RelatedToProperty item = new RelatedToProperty();
        item.setPropertyKey("service-instance.service-instance-name");
        item.setPropertyValue("lll_svc_010317");
        relationship.getRelatedToProperty().add(item);

        relationship.setRelatedTo("service-instance");
        relationship.setRelatedLink(
                "/aai/v11/business/customers/customer/MSO_1610_ST/service-subscriptions/service-subscription"
                        + "/MSO-dev-service-type/service-instances/service-instance/"
                        + "e1e9c97c-02c0-4919-9b4c-eb5d5ef68970");

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
        assertEquals(requestId, clem.getRequestID());

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
        event.setClosedLoopControlName("TwoOnsetTest");
        event.setRequestId(requestId);
        event.setTarget("generic-vnf.vnf-id");
        event.setClosedLoopAlarmStart(Instant.now());
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event.setAai(new HashMap<>());
        event.getAai().put("generic-vnf.vnf-name", "onsetOne");

        ControlLoopEventManager manager = makeManager(event);
        manager.setActivated(true);
        VirtualControlLoopNotification notification = manager.activate(event);
        assertEquals(ControlLoopNotificationType.REJECTED, notification.getNotification());
    }

    @Test
    public void testActivationYaml() throws IOException {
        InputStream is = new FileInputStream(new File("src/test/resources/test.yaml"));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        InputStream isBad = new FileInputStream(new File("src/test/resources/notutf8.yaml"));
        final String yamlStringBad = IOUtils.toString(isBad, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName("TwoOnsetTest");
        event.setRequestId(requestId);
        event.setTarget("generic-vnf.vnf-id");
        event.setClosedLoopAlarmStart(Instant.now());
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event.setAai(new HashMap<>());
        event.getAai().put("generic-vnf.vnf-name", "onsetOne");

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
    public void testControlLoopFinal() throws ControlLoopException, IOException {
        InputStream is = new FileInputStream(new File("src/test/resources/test.yaml"));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName("TwoOnsetTest");
        event.setRequestId(requestId);
        event.setTarget("generic-vnf.vnf-id");
        event.setClosedLoopAlarmStart(Instant.now());
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event.setAai(new HashMap<>());
        event.getAai().put("generic-vnf.vnf-name", "onsetOne");

        ControlLoopEventManager manager = makeManager(event);
        try {
            manager.isControlLoopFinal();
            fail("test should throw an exception here");
        } catch (ControlLoopException e) {
            assertEquals("ControlLoopEventManager MUST be activated first.", e.getMessage());
        }

        manager.setActivated(true);
        try {
            manager.isControlLoopFinal();
            fail("test should throw an exception here");
        } catch (ControlLoopException e) {
            assertEquals("No onset event for ControlLoopEventManager.", e.getMessage());
        }

        manager.setActivated(false);
        VirtualControlLoopNotification notification = manager.activate(yamlString, event);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        VirtualControlLoopNotification clfNotification = manager.isControlLoopFinal();
        assertNull(clfNotification);

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
    public void testProcessControlLoop() throws ControlLoopException, IOException, AaiException {
        InputStream is = new FileInputStream(new File("src/test/resources/test.yaml"));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName("TwoOnsetTest");
        event.setRequestId(requestId);
        event.setTarget("generic-vnf.vnf-id");
        event.setClosedLoopAlarmStart(Instant.now());
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event.setAai(new HashMap<>());
        event.getAai().put("generic-vnf.vnf-name", "onsetOne");

        ControlLoopEventManager manager = makeManager(event);
        try {
            manager.processControlLoop();
            fail("test should throw an exception here");
        } catch (Exception e) {
            assertEquals("ControlLoopEventManager MUST be activated first.", e.getMessage());
        }

        manager.setActivated(true);
        try {
            manager.processControlLoop();
            fail("test should throw an exception here");
        } catch (Exception e) {
            assertEquals("No onset event for ControlLoopEventManager.", e.getMessage());
        }

        manager.setActivated(false);
        VirtualControlLoopNotification notification = manager.activate(yamlString, event);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        ControlLoopOperationManager clom = manager.processControlLoop();
        assertNotNull(clom);
        assertNull(clom.getOperationResult());

        // Test operation in progress
        try {
            manager.processControlLoop();
            fail("test should throw an exception here");
        } catch (Exception e) {
            assertEquals("Already working an Operation, do not call this method.", e.getMessage());
        }

        manager = new ControlLoopEventManager(event.getClosedLoopControlName(), event.getRequestId());
        notification = manager.activate(yamlString, event);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        manager.getProcessor().nextPolicyForResult(PolicyResult.FAILURE_GUARD);
        VirtualControlLoopNotification clfNotification = manager.isControlLoopFinal();
        assertNotNull(clfNotification);
        assertEquals(ControlLoopNotificationType.FINAL_FAILURE, clfNotification.getNotification());

        // Test operation completed
        try {
            manager.processControlLoop();
            fail("test should throw an exception here");
        } catch (Exception e) {
            assertEquals("Control Loop is in FINAL state, do not call this method.", e.getMessage());
        }

        manager = new ControlLoopEventManager(event.getClosedLoopControlName(), event.getRequestId());
        notification = manager.activate(yamlString, event);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());
        manager.getProcessor().nextPolicyForResult(PolicyResult.FAILURE);

        // Test operation with no next policy defined
        try {
            manager.processControlLoop();
            fail("test should throw an exception here");
        } catch (Exception e) {
            assertEquals("The target type is null", e.getMessage());
        }
    }

    @Test
    public void testFinishOperation() throws IOException, ControlLoopException, AaiException {
        InputStream is = new FileInputStream(new File("src/test/resources/testSOactor.yaml"));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        InputStream isStd = new FileInputStream(new File("src/test/resources/test.yaml"));
        final String yamlStringStd = IOUtils.toString(isStd, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName("TwoOnsetTest");
        event.setRequestId(requestId);
        event.setTarget("generic-vnf.vnf-id");
        event.setClosedLoopAlarmStart(Instant.now());
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event.setAai(new HashMap<>());
        event.getAai().put("generic-vnf.vnf-id", "onsetOne");

        ControlLoopEventManager manager = makeManager(event);
        try {
            manager.finishOperation(null);
            fail("test should throw an exception here");
        } catch (Exception e) {
            assertEquals("No operation to finish.", e.getMessage());
        }

        manager.setActivated(true);
        try {
            manager.finishOperation(null);
            fail("test should throw an exception here");
        } catch (Exception e) {
            assertEquals("No operation to finish.", e.getMessage());
        }

        manager.setActivated(false);
        VirtualControlLoopNotification notification = manager.activate(yamlString, event);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        try {
            manager.lockCurrentOperation();
            fail("test should throw an exception here");
        } catch (Exception e) {
            assertEquals("Do not have a current operation.", e.getMessage());
        }

        assertNull(manager.unlockCurrentOperation());

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
    public void testOnNewEvent() throws IOException, AaiException {
        InputStream is = new FileInputStream(new File("src/test/resources/test.yaml"));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
        onsetEvent.setClosedLoopControlName("TwoOnsetTest");
        onsetEvent.setRequestId(requestId);
        onsetEvent.setTarget("generic-vnf.vnf-id");
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put("generic-vnf.vnf-name", "onsetOne");

        VirtualControlLoopEvent abatedEvent = new VirtualControlLoopEvent();
        abatedEvent.setClosedLoopControlName("TwoOnsetTest");
        abatedEvent.setRequestId(requestId);
        abatedEvent.setTarget("generic-vnf.vnf-id");
        abatedEvent.setClosedLoopAlarmStart(Instant.now());
        abatedEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ABATED);
        abatedEvent.setAai(new HashMap<>());
        abatedEvent.getAai().put("generic-vnf.vnf-name", "onsetOne");

        ControlLoopEventManager manager = makeManager(onsetEvent);
        VirtualControlLoopNotification notification = manager.activate(yamlString, onsetEvent);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        assertEquals(NEW_EVENT_STATUS.FIRST_ONSET, manager.onNewEvent(onsetEvent));
        assertEquals(NEW_EVENT_STATUS.FIRST_ABATEMENT, manager.onNewEvent(abatedEvent));
        assertEquals(NEW_EVENT_STATUS.SUBSEQUENT_ABATEMENT, manager.onNewEvent(abatedEvent));

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

        assertEquals(NEW_EVENT_STATUS.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        assertEquals(NEW_EVENT_STATUS.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setClosedLoopControlName(null);
        assertEquals(NEW_EVENT_STATUS.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setClosedLoopControlName("");
        assertEquals(NEW_EVENT_STATUS.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setClosedLoopControlName("TwoOnsetTest");
        assertEquals(NEW_EVENT_STATUS.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setRequestId(null);
        assertEquals(NEW_EVENT_STATUS.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setRequestId(requestId);
        assertEquals(NEW_EVENT_STATUS.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setAai(null);
        assertEquals(NEW_EVENT_STATUS.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setAai(new HashMap<>());
        assertEquals(NEW_EVENT_STATUS.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setTarget("");
        assertEquals(NEW_EVENT_STATUS.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setTarget(null);
        assertEquals(NEW_EVENT_STATUS.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setTarget("");
        assertEquals(NEW_EVENT_STATUS.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setTarget("OZ");
        assertEquals(NEW_EVENT_STATUS.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setTarget("VM_NAME");
        assertEquals(NEW_EVENT_STATUS.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setTarget("VNF_NAME");
        assertEquals(NEW_EVENT_STATUS.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setTarget("vserver.vserver-name");
        assertEquals(NEW_EVENT_STATUS.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setTarget("generic-vnf.vnf-id");
        assertEquals(NEW_EVENT_STATUS.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setTarget("generic-vnf.vnf-name");
        assertEquals(NEW_EVENT_STATUS.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setAai(null);
        assertEquals(NEW_EVENT_STATUS.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.setAai(new HashMap<>());
        assertEquals(NEW_EVENT_STATUS.SYNTAX_ERROR, manager.onNewEvent(checkSyntaxEvent));

        checkSyntaxEvent.getAai().put("generic-vnf.vnf-name", "onsetOne");
        assertEquals(NEW_EVENT_STATUS.SUBSEQUENT_ABATEMENT, manager.onNewEvent(abatedEvent));

        checkSyntaxEvent.getAai().put("vserver.vserver-name", "onsetOne");
        assertEquals(NEW_EVENT_STATUS.SUBSEQUENT_ABATEMENT, manager.onNewEvent(abatedEvent));

        checkSyntaxEvent.getAai().put("generic-vnf.vnf-id", "onsetOne");
        assertEquals(NEW_EVENT_STATUS.SUBSEQUENT_ABATEMENT, manager.onNewEvent(abatedEvent));
    }

    @Test
    public void testControlLoopTimeout() throws IOException {
        InputStream is = new FileInputStream(new File("src/test/resources/test.yaml"));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
        onsetEvent.setClosedLoopControlName("TwoOnsetTest");
        onsetEvent.setRequestId(requestId);
        onsetEvent.setTarget("generic-vnf.vnf-id");
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put("generic-vnf.vnf-name", "onsetOne");

        ControlLoopEventManager manager = makeManager(onsetEvent);
        assertTrue(0 == manager.getControlLoopTimeout(null));
        assertTrue(120 == manager.getControlLoopTimeout(120));

        VirtualControlLoopNotification notification = manager.activate(yamlString, onsetEvent);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        assertTrue(60 == manager.getControlLoopTimeout(null));
    }

    @Test
    public void testQueryAai_AlreadyDisabled() throws AaiException {
        ControlLoopEventManager mgr = null;
        
        try {
            onset.getAai().put(ControlLoopEventManager.GENERIC_VNF_IS_CLOSED_LOOP_DISABLED, Boolean.TRUE.toString());
            onset.getAai().put(ControlLoopEventManager.GENERIC_VNF_PROV_STATUS,
                            ControlLoopEventManager.PROV_STATUS_ACTIVE);

            mgr = makeManager(onset);
            mgr.queryAai(onset);

            fail("missing exception");

        } catch (AaiException expected) {
            assertEquals("is-closed-loop-disabled is set to true on VServer or VNF", expected.getMessage());
            assertNull(mgr.getVnfResponse());
            assertNull(mgr.getVserverResponse());
        }
    }

    @Test
    public void testQueryAai_AlreadyInactive() throws AaiException {
        ControlLoopEventManager mgr = null;
        
        try {
            onset.getAai().put(ControlLoopEventManager.GENERIC_VNF_IS_CLOSED_LOOP_DISABLED, Boolean.FALSE.toString());
            onset.getAai().put(ControlLoopEventManager.GENERIC_VNF_PROV_STATUS, "not-active2");

            mgr = makeManager(onset);
            mgr.queryAai(onset);

            fail("missing exception");

        } catch (AaiException expected) {
            assertEquals("prov-status is not ACTIVE on VServer or VNF", expected.getMessage());
            assertNull(mgr.getVnfResponse());
            assertNull(mgr.getVserverResponse());
        }
    }

    @Test
    public void testQueryAai_QueryVnfById() throws AaiException {
        ControlLoopEventManager mgr = null;

        mgr = makeManager(onset);
        mgr.queryAai(onset);
        
        AaiGetVnfResponse vnfresp = mgr.getVnfResponse();
        assertNull(mgr.getVserverResponse());
        
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
    }

    @Test
    public void testQueryAai_QueryVnfById_Disabled() throws AaiException {
        ControlLoopEventManager mgr = null;
        
        try {
            onset.getAai().put(ControlLoopEventManager.GENERIC_VNF_VNF_ID, "disableClosedLoop");
            
            mgr = makeManager(onset);
            mgr.queryAai(onset);

            fail("missing exception");

        } catch (AaiException expected) {
            assertEquals("is-closed-loop-disabled is set to true (query by vnf-id)", expected.getMessage());
            
            assertNotNull(mgr.getVnfResponse());
            assertNull(mgr.getVserverResponse());
        }
    }

    @Test
    public void testQueryAai_QueryVserver() throws AaiException {
        ControlLoopEventManager mgr = null;

        onset.getAai().remove(ControlLoopEventManager.GENERIC_VNF_VNF_ID);
        onset.getAai().put(ControlLoopEventManager.VSERVER_VSERVER_NAME, "AVserver");

        mgr = makeManager(onset);
        mgr.queryAai(onset);
        
        assertNull(mgr.getVnfResponse());
        AaiGetVserverResponse vsvresp = mgr.getVserverResponse();

        // should not re-query
        mgr.queryAai(onset);
        
        assertNull(mgr.getVnfResponse());
        assertEquals(vsvresp, mgr.getVserverResponse());
    }

    @Test
    public void testQueryAai_QueryVserver_Disabled() throws AaiException {
        ControlLoopEventManager mgr = null;
        
        try {
            onset.getAai().remove(ControlLoopEventManager.GENERIC_VNF_VNF_ID);
            onset.getAai().put(ControlLoopEventManager.VSERVER_VSERVER_NAME, "disableClosedLoop");
            
            mgr = makeManager(onset);
            mgr.queryAai(onset);

            fail("missing exception");

        } catch (AaiException expected) {
            assertEquals("is-closed-loop-disabled is set to true (query by vserver-name)", expected.getMessage());
            
            assertNull(mgr.getVnfResponse());
            assertNotNull(mgr.getVserverResponse());
        }
    }

    @Test(expected = AaiException.class)
    public void testQueryAai_QueryException() throws AaiException {
        // Force AAI errors
        PolicyEngine.manager.setEnvironmentProperty("aai.url", "http://localhost:9999");
        
        makeManager(onset).queryAai(onset);
    }

    @Test
    public void testProcessVNFResponse_Success() throws Exception {
        AaiGetVnfResponse resp = new AaiGetVnfResponse();
        resp.setIsClosedLoopDisabled(false);
        resp.setProvStatus(ControlLoopEventManager.PROV_STATUS_ACTIVE);
        Whitebox.invokeMethod(ControlLoopEventManager.class, "processVNFResponse", resp, true);
    }

    @Test
    public void testProcessVNFResponse_NullResponse() throws Exception {
        thrown.expect(AaiException.class);
        thrown.expectMessage("AAI Response is null (query by vnf-id)");

        AaiGetVnfResponse resp = null;
        Whitebox.invokeMethod(ControlLoopEventManager.class, "processVNFResponse", resp, true);
    }

    @Test
    public void testProcessVNFResponse_Error() throws Exception {
        thrown.expect(AaiException.class);
        thrown.expectMessage("AAI Responded with a request error (query by vnf-name)");
        
        AaiGetVnfResponse resp = new AaiGetVnfResponse();
        
        resp.setRequestError(new AaiNqRequestError());
        
        resp.setIsClosedLoopDisabled(false);
        resp.setProvStatus(ControlLoopEventManager.PROV_STATUS_ACTIVE);
        Whitebox.invokeMethod(ControlLoopEventManager.class, "processVNFResponse", resp, false);
    }

    @Test
    public void testProcessVNFResponse_Disabled() throws Exception {
        thrown.expect(AaiException.class);
        thrown.expectMessage("is-closed-loop-disabled is set to true (query by vnf-id)");
        
        AaiGetVnfResponse resp = new AaiGetVnfResponse();
        resp.setIsClosedLoopDisabled(true);
        resp.setProvStatus(ControlLoopEventManager.PROV_STATUS_ACTIVE);
        Whitebox.invokeMethod(ControlLoopEventManager.class, "processVNFResponse", resp, true);
    }

    @Test
    public void testProcessVNFResponse_Inactive() throws Exception {
        thrown.expect(AaiException.class);
        thrown.expectMessage("prov-status is not ACTIVE (query by vnf-name)");

        AaiGetVnfResponse resp = new AaiGetVnfResponse();
        resp.setIsClosedLoopDisabled(false);
        resp.setProvStatus("inactive1");
        Whitebox.invokeMethod(ControlLoopEventManager.class, "processVNFResponse", resp, false);
    }

    @Test
    public void testProcessVserverResponse_Success() throws Exception {
        AaiGetVserverResponse resp = new AaiGetVserverResponse();
        
        AaiNqVServer svr = new AaiNqVServer();
        resp.getVserver().add(svr);
        
        svr.setIsClosedLoopDisabled(false);
        svr.setProvStatus(ControlLoopEventManager.PROV_STATUS_ACTIVE);
        Whitebox.invokeMethod(ControlLoopEventManager.class, "processVServerResponse", resp);
    }

    @Test
    public void testProcessVserverResponse_NullResponse() throws Exception {
        thrown.expect(AaiException.class);
        thrown.expectMessage("AAI Response is null (query by vserver-name)");

        AaiGetVserverResponse resp = null;
        Whitebox.invokeMethod(ControlLoopEventManager.class, "processVServerResponse", resp);
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
        
        Whitebox.invokeMethod(ControlLoopEventManager.class, "processVServerResponse", resp);
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
        Whitebox.invokeMethod(ControlLoopEventManager.class, "processVServerResponse", resp);
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
        Whitebox.invokeMethod(ControlLoopEventManager.class, "processVServerResponse", resp);
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

    private ControlLoopEventManager makeManager(VirtualControlLoopEvent event) {
        return new ControlLoopEventManager(event.getClosedLoopControlName(), event.getRequestId());
    }
}
