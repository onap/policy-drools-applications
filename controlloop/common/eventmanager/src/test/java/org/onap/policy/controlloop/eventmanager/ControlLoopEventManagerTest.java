/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2017-2020 AT&T Intellectual Property. All rights reserved.
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.io.Serializer;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.ControlLoopTargetType;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.eventmanager.ControlLoopEventManager.NewEventStatus;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.drools.core.lock.Lock;
import org.onap.policy.drools.core.lock.LockCallback;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.drools.utils.Pair;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.powermock.reflect.Whitebox;

public class ControlLoopEventManagerTest {
    private static final String TARGET_LOCK_FIELD = "targetLock";
    private static final String ONSET_ONE = "onsetOne";
    private static final String VSERVER_NAME = "vserver.vserver-name";
    private static final String TEST_YAML = "src/test/resources/test.yaml";
    private static final String VNF_NAME = "generic-vnf.vnf-name";
    private static final String VNF_ID = "generic-vnf.vnf-id";
    private static final String AAI_USERNAME = "aai.username";
    private static final String AAI_URL = "aai.url";
    private static final String AAI_PASS = "aai.password";
    private static final String TWO_ONSET_TEST = "TwoOnsetTest";
    private static final String VNF_UUID = "83f674e8-7555-44d7-9a39-bdc3770b0491";

    private VirtualControlLoopEvent onset;
    private LockCallback callback;

    /**
     * Set up test class.
     */
    @BeforeClass
    public static void setUpSimulator() throws Exception {
        org.onap.policy.simulators.Util.buildAaiSim();

        PolicyEngineConstants.getManager().setEnvironmentProperty(AAI_USERNAME, "AAI");
        PolicyEngineConstants.getManager().setEnvironmentProperty(AAI_PASS, "AAI");
        PolicyEngineConstants.getManager().setEnvironmentProperty(AAI_URL, "http://localhost:6666");
        PolicyEngineConstants.getManager().setEnvironmentProperty("aai.customQuery", "false");
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
        callback = mock(LockCallback.class);

        onset = new VirtualControlLoopEvent();
        onset.setClosedLoopControlName("ControlLoop-vUSP");
        onset.setRequestId(UUID.randomUUID());
        onset.setTarget("VM_NAME");
        onset.setClosedLoopAlarmStart(Instant.now());
        onset.setAai(new HashMap<>());
        onset.getAai().put("cloud-region.identity-url", "foo");
        onset.getAai().put("vserver.selflink", "bar");
        onset.getAai().put(VNF_ID, VNF_UUID);
        onset.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onset.setTargetType(ControlLoopTargetType.VNF);

        PolicyEngineConstants.getManager().setEnvironmentProperty(AAI_URL, "http://localhost:6666");
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

        assertEquals(true, clem.isControlLoopTimedOut());

        assertNull(clem.unlockCurrentOperation());
    }

    @Test
    public void testAlreadyActivated() {
        VirtualControlLoopEvent event = getOnsetEvent();

        ControlLoopEventManager manager = makeManager(event);
        manager.setActivated(true);
        VirtualControlLoopNotification notification = manager.activate(event);
        assertEquals(ControlLoopNotificationType.REJECTED, notification.getNotification());
    }

    @Test
    public void testActivationYaml() throws IOException, CoderException {

        VirtualControlLoopEvent event = getOnsetEvent();
        ControlLoopEventManager manager = makeManager(event);

        // Null YAML should fail
        VirtualControlLoopNotification notificationNull = manager.activate((String) null, event);
        assertNotNull(notificationNull);
        assertEquals(ControlLoopNotificationType.REJECTED, notificationNull.getNotification());

        // Empty YAML should fail
        VirtualControlLoopNotification notificationEmpty = manager.activate("", event);
        assertNotNull(notificationEmpty);
        assertEquals(ControlLoopNotificationType.REJECTED, notificationEmpty.getNotification());

        // Bad YAML should fail
        InputStream isBad = new FileInputStream(new File("src/test/resources/notutf8.yaml"));
        final String yamlStringBad = IOUtils.toString(isBad, StandardCharsets.UTF_8);

        VirtualControlLoopNotification notificationBad = manager.activate(yamlStringBad, event);
        assertNotNull(notificationBad);
        assertEquals(ControlLoopNotificationType.REJECTED, notificationBad.getNotification());


        InputStream is = new FileInputStream(new File(TEST_YAML));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        VirtualControlLoopNotification notification = manager.activate(yamlString, event);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        // Another activate should fail
        VirtualControlLoopNotification notificationActive = manager.activate(yamlString, event);
        assertNotNull(notificationActive);
        assertEquals(ControlLoopNotificationType.REJECTED, notificationActive.getNotification());
    }

    @Test
    public void testActivateToscaLegacy() throws IOException, CoderException {
        String policy =
                new String(Files.readAllBytes(Paths.get("src/test/resources/tosca-policy-legacy-vcpe.json")));
        ToscaPolicy toscaPolicy = new StandardCoder().decode(policy, ToscaPolicy.class);

        VirtualControlLoopEvent event = getOnsetEvent();
        ControlLoopEventManager manager = makeManager(event);

        // trigger a reject by passing the wrong policy type
        toscaPolicy.setType("onap.policies.controlloop.operational.common.Drools");
        VirtualControlLoopNotification notification = manager.activate(toscaPolicy, event);
        assertEquals(ControlLoopNotificationType.REJECTED, notification.getNotification());

        // place back correct policy type
        toscaPolicy.setType("onap.policies.controlloop.Operational");
        notification = manager.activate(toscaPolicy, event);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        // another activate should fail
        notification = manager.activate(toscaPolicy, event);
        assertEquals(ControlLoopNotificationType.REJECTED, notification.getNotification());
    }

    @Test
    public void testControlLoopFinal() throws Exception {
        VirtualControlLoopEvent event = getOnsetEvent();

        ControlLoopEventManager manager = makeManager(event);
        ControlLoopEventManager manager2 = manager;
        assertThatThrownBy(manager2::isControlLoopFinal).isInstanceOf(ControlLoopException.class)
                        .hasMessage("ControlLoopEventManager MUST be activated first.");

        manager.setActivated(true);
        assertThatThrownBy(manager2::isControlLoopFinal).isInstanceOf(ControlLoopException.class)
                        .hasMessage("No onset event for ControlLoopEventManager.");

        manager.setActivated(false);

        InputStream is = new FileInputStream(new File(TEST_YAML));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

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

    @NotNull
    private VirtualControlLoopEvent getOnsetEvent() {
        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(TWO_ONSET_TEST);
        event.setRequestId(requestId);
        event.setTarget(VNF_ID);
        event.setClosedLoopAlarmStart(Instant.now());
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event.setAai(new HashMap<>());
        event.getAai().put(VNF_NAME, ONSET_ONE);
        event.setTargetType(ControlLoopTargetType.VNF);
        return event;
    }

    @Test
    public void testProcessControlLoop() throws Exception {
        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(TWO_ONSET_TEST);
        event.setRequestId(requestId);
        event.setTarget(VNF_ID);
        event.setClosedLoopAlarmStart(Instant.now());
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event.setAai(new HashMap<>());
        event.getAai().put(VNF_NAME, ONSET_ONE);
        event.getAai().put(VSERVER_NAME, "testVserverName");
        event.setTargetType(ControlLoopTargetType.VNF);

        ControlLoopEventManager manager = makeManager(event);
        ControlLoopEventManager manager2 = manager;
        assertThatThrownBy(manager2::processControlLoop).isInstanceOf(ControlLoopException.class)
                        .hasMessage("ControlLoopEventManager MUST be activated first.");

        manager.setActivated(true);
        assertThatThrownBy(manager2::processControlLoop).isInstanceOf(ControlLoopException.class)
                        .hasMessage("No onset event for ControlLoopEventManager.");

        manager.setActivated(false);

        InputStream is = new FileInputStream(new File(TEST_YAML));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

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
        InputStream isStd = new FileInputStream(new File(TEST_YAML));
        final String yamlStringStd = IOUtils.toString(isStd, StandardCharsets.UTF_8);

        VirtualControlLoopEvent event = makeEvent();

        ControlLoopEventManager manager = makeManager(event);
        ControlLoopEventManager manager2 = manager;
        assertThatThrownBy(() -> manager2.finishOperation(null)).isInstanceOf(ControlLoopException.class)
                        .hasMessage("No operation to finish.");

        manager.setActivated(true);
        assertThatThrownBy(() -> manager2.finishOperation(null)).isInstanceOf(ControlLoopException.class)
                        .hasMessage("No operation to finish.");

        manager.setActivated(false);

        InputStream is = new FileInputStream(new File("src/test/resources/testSOactor.yaml"));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        VirtualControlLoopNotification notification = manager.activate(yamlString, event);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        event.getAai().put(VSERVER_NAME, "testVserverName");

        // serialize and de-serialize manager
        manager = Serializer.roundTrip(manager);

        ControlLoopOperationManager clom = manager.processControlLoop();
        assertNotNull(clom);
        assertNull(clom.getOperationResult());

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
    public void testLockCurrentOperation_testUnlockCurrentOperation() throws Exception {
        VirtualControlLoopEvent event = makeEvent();

        ControlLoopEventManager manager = makeManager(event);

        manager.setActivated(false);

        InputStream is = new FileInputStream(new File("src/test/resources/testSOactor.yaml"));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        VirtualControlLoopNotification notification = manager.activate(yamlString, event);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        ControlLoopEventManager manager2 = manager;
        assertThatThrownBy(() -> manager2.lockCurrentOperation(callback)).isInstanceOf(ControlLoopException.class)
                        .hasMessage("Do not have a current operation.");

        assertNull(manager.unlockCurrentOperation());

        event.getAai().put(VSERVER_NAME, "testVserverName");

        ControlLoopOperationManager clom = manager.processControlLoop();
        assertNotNull(clom);
        assertNull(clom.getOperationResult());

        Pair<Lock, Lock> lockPair = manager.lockCurrentOperation(callback);
        assertNull(lockPair.first());
        assertNotNull(lockPair.second());

        // pseudo lock - session should NOT have been notified of the change
        verify(callback, never()).lockAvailable(any());
        verify(callback, never()).lockUnavailable(any());

        // repeat - should cause an extension
        Lock lock = lockPair.second();
        lockPair = manager.lockCurrentOperation(callback);

        /*
         * even with a pseudo lock, the session should have been notified that it was
         * extended
         */

        verify(callback).lockAvailable(lock);

        assertSame(lock, manager.unlockCurrentOperation());

        assertNull(lockPair.first());
        assertNull(lockPair.second());

        // force it to use a pseudo lock
        manager.setUseTargetLock(false);
        lockPair = manager.lockCurrentOperation(callback);
        assertNull(lockPair.first());
        assertNotNull(lockPair.second());

        lock = lockPair.second();

        lockPair = manager.lockCurrentOperation(callback);
        assertNull(lockPair.first());
        assertNull(lockPair.second());

        // first lock uses a pseudo lock, so it will only update when extended
        verify(callback).lockAvailable(lock);

        // force it to re-create the lock due to change in resource ID
        lock = mock(Lock.class);
        when(lock.getResourceId()).thenReturn("different");
        Whitebox.setInternalState(manager, TARGET_LOCK_FIELD, lock);

        lockPair = manager.lockCurrentOperation(callback);
        assertSame(lock, lockPair.first());
        assertNotNull(lockPair.second());

        lock = lockPair.second();

        lockPair = manager.lockCurrentOperation(callback);
        assertNull(lockPair.first());
        assertNull(lockPair.second());

        // first lock uses a pseudo lock, so it won't do an update
        verify(callback).lockAvailable(lock);

        assertSame(lock, manager.unlockCurrentOperation());
        assertNull(manager.unlockCurrentOperation());

        // try again - this time don't return the fact handle- no change in count
        lockPair = manager.lockCurrentOperation(callback);
        assertNull(lockPair.first());
        assertNotNull(lockPair.second());
    }

    @Test
    public void testOnNewEvent() throws Exception {
        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent onsetEvent = new VirtualControlLoopEvent();
        onsetEvent.setClosedLoopControlName(TWO_ONSET_TEST);
        onsetEvent.setRequestId(requestId);
        onsetEvent.setTarget(VNF_ID);
        onsetEvent.setClosedLoopAlarmStart(Instant.now());
        onsetEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        onsetEvent.setAai(new HashMap<>());
        onsetEvent.getAai().put(VNF_NAME, ONSET_ONE);
        onsetEvent.setTargetType(ControlLoopTargetType.VNF);

        VirtualControlLoopEvent abatedEvent = new VirtualControlLoopEvent();
        abatedEvent.setClosedLoopControlName(TWO_ONSET_TEST);
        abatedEvent.setRequestId(requestId);
        abatedEvent.setTarget(VNF_ID);
        abatedEvent.setClosedLoopAlarmStart(Instant.now());
        abatedEvent.setClosedLoopEventStatus(ControlLoopEventStatus.ABATED);
        abatedEvent.setAai(new HashMap<>());
        abatedEvent.getAai().put(VNF_NAME, ONSET_ONE);

        ControlLoopEventManager manager = makeManager(onsetEvent);

        InputStream is = new FileInputStream(new File(TEST_YAML));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

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

        checkSyntaxEvent.setTargetType("");
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
        VirtualControlLoopEvent onsetEvent = getOnsetEvent();

        ControlLoopEventManager manager = makeManager(onsetEvent);
        assertEquals(0, manager.getControlLoopTimeout(null));
        assertEquals(120, manager.getControlLoopTimeout(120));

        InputStream is = new FileInputStream(new File(TEST_YAML));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        VirtualControlLoopNotification notification = manager.activate(yamlString, onsetEvent);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        assertEquals(60, manager.getControlLoopTimeout(null));
    }

    @Test
    public void testControlLoopTimeout_ZeroTimeout() throws IOException {
        VirtualControlLoopEvent onsetEvent = getOnsetEvent();

        ControlLoopEventManager manager = makeManager(onsetEvent);

        InputStream is = new FileInputStream(new File("src/test/resources/test-zero-timeout.yaml"));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        VirtualControlLoopNotification notification = manager.activate(yamlString, onsetEvent);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        assertEquals(0, manager.getControlLoopTimeout(null));
        assertEquals(120, manager.getControlLoopTimeout(120));
    }

    @Test
    public void testControlLoopTimeout_NullTimeout() throws IOException {
        VirtualControlLoopEvent onsetEvent = getOnsetEvent();

        ControlLoopEventManager manager = makeManager(onsetEvent);

        InputStream is = new FileInputStream(new File("src/test/resources/test-null-timeout.yaml"));
        final String yamlString = IOUtils.toString(is, StandardCharsets.UTF_8);

        VirtualControlLoopNotification notification = manager.activate(yamlString, onsetEvent);
        assertNotNull(notification);
        assertEquals(ControlLoopNotificationType.ACTIVE, notification.getNotification());

        assertEquals(0, manager.getControlLoopTimeout(null));
        assertEquals(120, manager.getControlLoopTimeout(120));
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


    private VirtualControlLoopEvent makeEvent() {
        UUID requestId = UUID.randomUUID();
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.setClosedLoopControlName(TWO_ONSET_TEST);
        event.setRequestId(requestId);
        event.setTarget(VNF_ID);
        event.setClosedLoopAlarmStart(Instant.now());
        event.setClosedLoopEventStatus(ControlLoopEventStatus.ONSET);
        event.setAai(new HashMap<>());
        event.getAai().put(VNF_ID, ONSET_ONE);
        event.getAai().put(VSERVER_NAME, "test-vserver");
        event.setTargetType(ControlLoopTargetType.VNF);
        return event;
    }

    private ControlLoopEventManager makeManager(VirtualControlLoopEvent event) {
        return new MyManager(event.getClosedLoopControlName(), event.getRequestId());
    }

    private static class MyManager extends ControlLoopEventManager implements Serializable {
        private static final long serialVersionUID = 1L;

        public MyManager(String closedLoopControlName, UUID requestId) {
            super(closedLoopControlName, requestId);
        }

        @Override
        protected Lock createRealLock(String targetEntity, UUID requestId, int holdSec, LockCallback callback) {
            return createPseudoLock(targetEntity, requestId, holdSec, callback);
        }
    }
}
