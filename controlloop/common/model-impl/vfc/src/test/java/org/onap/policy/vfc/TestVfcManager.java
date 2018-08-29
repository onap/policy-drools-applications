/*-
 * ============LICENSE_START=======================================================
 * vfc
 * ================================================================================
 * Copyright (C) 2018 Ericsson, AT&T. All rights reserved.
 * Modifications Copyright (C) 2018 AT&T Corporation. All rights reserved.
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

package org.onap.policy.vfc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.drools.core.WorkingMemory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.rest.RESTManager;
import org.onap.policy.rest.RESTManager.Pair;
import org.onap.policy.vfc.util.Serialization;

public class TestVfcManager {
    private static WorkingMemory mockedWorkingMemory;

    private RESTManager   mockedRestManager;

    private Pair<Integer, String> httpResponsePutOk;
    private Pair<Integer, String> httpResponseGetOk;
    private Pair<Integer, String> httpResponseBadResponse;
    private Pair<Integer, String> httpResponseErr;

    private VFCRequest  request;
    private VFCResponse response;

    @BeforeClass
    public static void beforeTestVfcManager() {
        mockedWorkingMemory = mock(WorkingMemory.class);
    }

    /**
     * Set up the mocked REST manager.
     */
    @Before
    public void setupMockedRest() {
        mockedRestManager   = mock(RESTManager.class);

        httpResponsePutOk       = mockedRestManager.new Pair<>(202, Serialization.gsonPretty.toJson(response));
        httpResponseGetOk       = mockedRestManager.new Pair<>(200, Serialization.gsonPretty.toJson(response));
        httpResponseBadResponse = mockedRestManager.new Pair<>(202, Serialization.gsonPretty.toJson(null));
        httpResponseErr         = mockedRestManager.new Pair<>(200, null);
    }

    /**
     * Create the request and response before.
     */
    @Before
    public void createRequestAndResponse() {
        VFCHealActionVmInfo actionInfo = new VFCHealActionVmInfo();
        actionInfo.setVmid("TheWizard");
        actionInfo.setVmname("The Wizard of Oz");

        VFCHealAdditionalParams additionalParams = new VFCHealAdditionalParams();
        additionalParams.setAction("Go Home");
        additionalParams.setActionInfo(actionInfo);

        VFCHealRequest healRequest = new VFCHealRequest();
        healRequest.setAdditionalParams(additionalParams);
        healRequest.setCause("WestWitch");
        healRequest.setVnfInstanceId("EmeraldCity");

        final UUID requestId = UUID.randomUUID();
        request = new VFCRequest();
        request.setHealRequest(healRequest);
        request.setNSInstanceId("Dorothy");
        request.setRequestId(requestId);

        List<VFCResponseDescriptor> responseHistoryList = new ArrayList<>();;

        VFCResponseDescriptor responseDescriptor = new VFCResponseDescriptor();
        responseDescriptor.setErrorCode("1234");
        responseDescriptor.setProgress("Follow The Yellow Brick Road");
        responseDescriptor.setResponseHistoryList(responseHistoryList);
        responseDescriptor.setResponseId(UUID.randomUUID().toString());
        responseDescriptor.setStatus("finished");
        responseDescriptor.setStatusDescription("There's no place like home");

        response = new VFCResponse();
        response.setJobId("1234");
        response.setRequestId(request.getRequestId().toString());
        response.setResponseDescriptor(responseDescriptor);
    }

    /**
     * Remove the environnment.
     */
    @After
    public void tearDown() {
        PolicyEngine.manager.getEnvironment().remove("vfc.password");
        PolicyEngine.manager.getEnvironment().remove("vfc.username");
        PolicyEngine.manager.getEnvironment().remove("vfc.url");
    }

    @Test
    public void testVfcInitiation() {
        try {
            new VFCManager(null, null);
            fail("test should throw an exception here");
        }
        catch (IllegalArgumentException e) {
            assertEquals("the parameters \"wm\" and \"request\" on the VFCManager constructor may not be null", 
                    e.getMessage());
        }

        try {
            new VFCManager(mockedWorkingMemory, null);
            fail("test should throw an exception here");
        }
        catch (IllegalArgumentException e) {
            assertEquals("the parameters \"wm\" and \"request\" on the VFCManager constructor may not be null", 
                    e.getMessage());
        }

        try {
            new VFCManager(mockedWorkingMemory, request);
            fail("test should throw an exception here");
        }
        catch (IllegalArgumentException e) {
            assertEquals("The value of policy engine manager environment property \"vfc.url\" may not be null", 
                    e.getMessage());
        }

        // add url; username & password are not required
        PolicyEngine.manager.getEnvironment().put("vfc.url", "http://somewhere.over.the.rainbow");
        new VFCManager(mockedWorkingMemory, request);

        // url & username, but no password
        PolicyEngine.manager.getEnvironment().put("vfc.username", "Dorothy");

        // url, username, and password
        PolicyEngine.manager.getEnvironment().put("vfc.password", "Toto");
        new VFCManager(mockedWorkingMemory, request);
    }

    @Test
    public void testVfcExecutionException() throws InterruptedException {
        PolicyEngine.manager.getEnvironment().put("vfc.url", "http://somewhere.over.the.rainbow");
        PolicyEngine.manager.getEnvironment().put("vfc.username", "Dorothy");
        PolicyEngine.manager.getEnvironment().put("vfc.password", "Exception");

        VFCManager manager = new VFCManager(mockedWorkingMemory, request);
        manager.setRestManager(mockedRestManager);

        Thread managerThread = new Thread(manager);
        managerThread.start();

        when(mockedRestManager.post(
            startsWith("http://somewhere.over.the.rainbow"),
            eq("Dorothy"),
            eq("Exception"),
            anyMap(),
            anyString(),
            anyString()))
            .thenThrow(new RuntimeException("OzException"));

        while (managerThread.isAlive()) {
            Thread.sleep(100);
        }

        PolicyEngine.manager.getEnvironment().remove("vfc.password");
        PolicyEngine.manager.getEnvironment().remove("vfc.username");
        PolicyEngine.manager.getEnvironment().remove("vfc.url");
    }

    @Test
    public void testVfcExecutionNull() throws InterruptedException {
        PolicyEngine.manager.getEnvironment().put("vfc.url", "http://somewhere.over.the.rainbow");
        PolicyEngine.manager.getEnvironment().put("vfc.username", "Dorothy");
        PolicyEngine.manager.getEnvironment().put("vfc.password", "Null");

        VFCManager manager = new VFCManager(mockedWorkingMemory, request);
        manager.setRestManager(mockedRestManager);

        Thread managerThread = new Thread(manager);
        managerThread.start();

        when(mockedRestManager.post(startsWith("http://somewhere.over.the.rainbow"), 
                eq("Dorothy"), eq("Null"), anyMap(), anyString(), anyString()))
                .thenReturn(null);

        while (managerThread.isAlive()) {
            Thread.sleep(100);
        }

        PolicyEngine.manager.getEnvironment().remove("vfc.password");
        PolicyEngine.manager.getEnvironment().remove("vfc.username");
        PolicyEngine.manager.getEnvironment().remove("vfc.url");
    }

    @Test
    public void testVfcExecutionError0() throws InterruptedException {
        PolicyEngine.manager.getEnvironment().put("vfc.url", "http://somewhere.over.the.rainbow");
        PolicyEngine.manager.getEnvironment().put("vfc.username", "Dorothy");
        PolicyEngine.manager.getEnvironment().put("vfc.password", "Error0");

        VFCManager manager = new VFCManager(mockedWorkingMemory, request);
        manager.setRestManager(mockedRestManager);

        Thread managerThread = new Thread(manager);
        managerThread.start();

        when(mockedRestManager.post(startsWith("http://somewhere.over.the.rainbow"), 
                eq("Dorothy"), eq("Error0"), anyMap(), anyString(), anyString()))
                .thenReturn(httpResponseErr);

        while (managerThread.isAlive()) {
            Thread.sleep(100);
        }

        PolicyEngine.manager.getEnvironment().remove("vfc.password");
        PolicyEngine.manager.getEnvironment().remove("vfc.username");
        PolicyEngine.manager.getEnvironment().remove("vfc.url");
    }

    @Test
    public void testVfcExecutionBadResponse() throws InterruptedException {
        PolicyEngine.manager.getEnvironment().put("vfc.url", "http://somewhere.over.the.rainbow");
        PolicyEngine.manager.getEnvironment().put("vfc.username", "Dorothy");
        PolicyEngine.manager.getEnvironment().put("vfc.password", "BadResponse");

        VFCManager manager = new VFCManager(mockedWorkingMemory, request);
        manager.setRestManager(mockedRestManager);

        Thread managerThread = new Thread(manager);
        managerThread.start();

        when(mockedRestManager.post(startsWith("http://somewhere.over.the.rainbow"), 
                eq("Dorothy"), eq("OK"), anyMap(), anyString(), anyString()))
                .thenReturn(httpResponseBadResponse);

        while (managerThread.isAlive()) {
            Thread.sleep(100);
        }

        PolicyEngine.manager.getEnvironment().remove("vfc.password");
        PolicyEngine.manager.getEnvironment().remove("vfc.username");
        PolicyEngine.manager.getEnvironment().remove("vfc.url");
    }

    @Test
    public void testVfcExecutionOk() throws InterruptedException {
        PolicyEngine.manager.getEnvironment().put("vfc.url", "http://somewhere.over.the.rainbow");
        PolicyEngine.manager.getEnvironment().put("vfc.username", "Dorothy");
        PolicyEngine.manager.getEnvironment().put("vfc.password", "OK");

        VFCManager manager = new VFCManager(mockedWorkingMemory, request);
        manager.setRestManager(mockedRestManager);

        Thread managerThread = new Thread(manager);
        managerThread.start();

        when(mockedRestManager.post(startsWith("http://somewhere.over.the.rainbow"), 
                eq("Dorothy"), eq("OK"), anyMap(), anyString(), anyString()))
                .thenReturn(httpResponsePutOk);

        when(mockedRestManager.get(endsWith("1234"), eq("Dorothy"), eq("OK"), anyMap()))
            .thenReturn(httpResponseGetOk);

        while (managerThread.isAlive()) {
            Thread.sleep(100);
        }

        PolicyEngine.manager.getEnvironment().remove("vfc.password");
        PolicyEngine.manager.getEnvironment().remove("vfc.username");
        PolicyEngine.manager.getEnvironment().remove("vfc.url");
    }
}
