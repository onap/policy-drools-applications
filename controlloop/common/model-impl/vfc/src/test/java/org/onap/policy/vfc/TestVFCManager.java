/*-
 * ============LICENSE_START=======================================================
 * vfc
 * ================================================================================
 * Copyright (C) 2018 Ericsson, AT&T. All rights reserved.
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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

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

public class TestVFCManager {
	private static WorkingMemory mockedWorkingMemory;

	private RESTManager   mockedRESTManager;

	private Pair<Integer, String> httpResponsePutOK;
	private Pair<Integer, String> httpResponseGetOK;
	private Pair<Integer, String> httpResponseBadResponse;
	private Pair<Integer, String> httpResponseErr;

	private VFCRequest  request;
	private VFCResponse response;

	@BeforeClass
	public static void beforeTestVFCManager() {
		mockedWorkingMemory = mock(WorkingMemory.class);
	}

	@Before
	public void setupMockedRest() {
		mockedRESTManager   = mock(RESTManager.class);

		httpResponsePutOK       = mockedRESTManager.new Pair<>(202, Serialization.gsonPretty.toJson(response));
		httpResponseGetOK       = mockedRESTManager.new Pair<>(200, Serialization.gsonPretty.toJson(response));
		httpResponseBadResponse = mockedRESTManager.new Pair<>(202, Serialization.gsonPretty.toJson(null));
		httpResponseErr         = mockedRESTManager.new Pair<>(200, null);
	}

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

		UUID requestId = UUID.randomUUID();
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

    @After
    public void tearDown() {
        PolicyEngine.manager.getEnvironment().remove("vfc.password");
        PolicyEngine.manager.getEnvironment().remove("vfc.username");
        PolicyEngine.manager.getEnvironment().remove("vfc.url");
    }

	@Test
	public void testVFCInitiation() {
		try {
			new VFCManager(null, null);
			fail("test should throw an exception here");
		}
		catch (IllegalArgumentException e) {
			assertEquals("the parameters \"wm\" and \"request\" on the VFCManager constructor may not be null", e.getMessage());
		}

		try {
			new VFCManager(mockedWorkingMemory, null);
			fail("test should throw an exception here");
		}
		catch (IllegalArgumentException e) {
			assertEquals("the parameters \"wm\" and \"request\" on the VFCManager constructor may not be null", e.getMessage());
		}

		try {
			new VFCManager(mockedWorkingMemory, request);
			fail("test should throw an exception here");
		}
		catch (IllegalArgumentException e) {
			assertEquals("The value of policy engine manager environment property \"vfc.url\" may not be null", e.getMessage());
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
	public void testVFCExecutionException() throws InterruptedException {
		PolicyEngine.manager.getEnvironment().put("vfc.url", "http://somewhere.over.the.rainbow");
		PolicyEngine.manager.getEnvironment().put("vfc.username", "Dorothy");
		PolicyEngine.manager.getEnvironment().put("vfc.password", "Exception");

		VFCManager manager = new VFCManager(mockedWorkingMemory, request);
		manager.setRestManager(mockedRESTManager);

		Thread managerThread = new Thread(manager);
		managerThread.start();

		when(mockedRESTManager.post(startsWith("http://somewhere.over.the.rainbow"), eq("Dorothy"), eq("Exception"), anyMap(), anyString(), anyString()))
		.thenThrow(new RuntimeException("OzException"));

		while (managerThread.isAlive()) {
			Thread.sleep(100);
		}

		PolicyEngine.manager.getEnvironment().remove("vfc.password");
		PolicyEngine.manager.getEnvironment().remove("vfc.username");
		PolicyEngine.manager.getEnvironment().remove("vfc.url");
	}

	@Test
	public void testVFCExecutionNull() throws InterruptedException {
		PolicyEngine.manager.getEnvironment().put("vfc.url", "http://somewhere.over.the.rainbow");
		PolicyEngine.manager.getEnvironment().put("vfc.username", "Dorothy");
		PolicyEngine.manager.getEnvironment().put("vfc.password", "Null");

		VFCManager manager = new VFCManager(mockedWorkingMemory, request);
		manager.setRestManager(mockedRESTManager);

		Thread managerThread = new Thread(manager);
		managerThread.start();

		when(mockedRESTManager.post(startsWith("http://somewhere.over.the.rainbow"), eq("Dorothy"), eq("Null"), anyMap(), anyString(), anyString()))
		.thenReturn(null);

		while (managerThread.isAlive()) {
			Thread.sleep(100);
		}

		PolicyEngine.manager.getEnvironment().remove("vfc.password");
		PolicyEngine.manager.getEnvironment().remove("vfc.username");
		PolicyEngine.manager.getEnvironment().remove("vfc.url");
	}

	@Test
	public void testVFCExecutionError0() throws InterruptedException {
		PolicyEngine.manager.getEnvironment().put("vfc.url", "http://somewhere.over.the.rainbow");
		PolicyEngine.manager.getEnvironment().put("vfc.username", "Dorothy");
		PolicyEngine.manager.getEnvironment().put("vfc.password", "Error0");

		VFCManager manager = new VFCManager(mockedWorkingMemory, request);
		manager.setRestManager(mockedRESTManager);

		Thread managerThread = new Thread(manager);
		managerThread.start();

		when(mockedRESTManager.post(startsWith("http://somewhere.over.the.rainbow"), eq("Dorothy"), eq("Error0"), anyMap(), anyString(), anyString()))
		.thenReturn(httpResponseErr);

		while (managerThread.isAlive()) {
			Thread.sleep(100);
		}

		PolicyEngine.manager.getEnvironment().remove("vfc.password");
		PolicyEngine.manager.getEnvironment().remove("vfc.username");
		PolicyEngine.manager.getEnvironment().remove("vfc.url");
	}

	@Test
	public void testVFCExecutionBadResponse() throws InterruptedException {
		PolicyEngine.manager.getEnvironment().put("vfc.url", "http://somewhere.over.the.rainbow");
		PolicyEngine.manager.getEnvironment().put("vfc.username", "Dorothy");
		PolicyEngine.manager.getEnvironment().put("vfc.password", "BadResponse");

		VFCManager manager = new VFCManager(mockedWorkingMemory, request);
		manager.setRestManager(mockedRESTManager);

		Thread managerThread = new Thread(manager);
		managerThread.start();

		when(mockedRESTManager.post(startsWith("http://somewhere.over.the.rainbow"), eq("Dorothy"), eq("OK"), anyMap(), anyString(), anyString()))
		.thenReturn(httpResponseBadResponse);

		while (managerThread.isAlive()) {
			Thread.sleep(100);
		}

		PolicyEngine.manager.getEnvironment().remove("vfc.password");
		PolicyEngine.manager.getEnvironment().remove("vfc.username");
		PolicyEngine.manager.getEnvironment().remove("vfc.url");
	}

	@Test
	public void testVFCExecutionOK() throws InterruptedException {
		PolicyEngine.manager.getEnvironment().put("vfc.url", "http://somewhere.over.the.rainbow");
		PolicyEngine.manager.getEnvironment().put("vfc.username", "Dorothy");
		PolicyEngine.manager.getEnvironment().put("vfc.password", "OK");

		VFCManager manager = new VFCManager(mockedWorkingMemory, request);
		manager.setRestManager(mockedRESTManager);

		Thread managerThread = new Thread(manager);
		managerThread.start();

		when(mockedRESTManager.post(startsWith("http://somewhere.over.the.rainbow"), eq("Dorothy"), eq("OK"), anyMap(), anyString(), anyString()))
		.thenReturn(httpResponsePutOK);

		when(mockedRESTManager.get(endsWith("1234"), eq("Dorothy"), eq("OK"), anyMap()))
		.thenReturn(httpResponseGetOK);

		while (managerThread.isAlive()) {
			Thread.sleep(100);
		}

		PolicyEngine.manager.getEnvironment().remove("vfc.password");
		PolicyEngine.manager.getEnvironment().remove("vfc.username");
		PolicyEngine.manager.getEnvironment().remove("vfc.url");
	}
}
