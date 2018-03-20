/*-
 * ============LICENSE_START=======================================================
 * TestSOManager
 * ================================================================================
 * Copyright (C) 2018 Ericsson. All rights reserved.
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
package org.onap.policy.so;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

import java.util.UUID;
import java.util.concurrent.Future;

import org.drools.core.WorkingMemory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.rest.RESTManager;
import org.onap.policy.rest.RESTManager.Pair;
import org.onap.policy.so.util.Serialization;

public class TestSOManager {
	private static WorkingMemory mockedWorkingMemory;

	private RESTManager   mockedRESTManager;

	private Pair<Integer, String> httpResponsePutOK;
	private Pair<Integer, String> httpResponseGetOK;
	private Pair<Integer, String> httpResponsePostOK;
	private Pair<Integer, String> httpResponsePostNOK;
	private Pair<Integer, String> httpResponseErr;

	private SORequest  request;
	private SOResponse response;

	@BeforeClass
	public static void beforeTestSOManager() {
		mockedWorkingMemory = mock(WorkingMemory.class);
	}

	@Before
	public void setupMockedRest() {
		mockedRESTManager   = mock(RESTManager.class);

		httpResponsePutOK       = mockedRESTManager.new Pair<>(202, Serialization.gsonPretty.toJson(response));
		httpResponseGetOK       = mockedRESTManager.new Pair<>(200, Serialization.gsonPretty.toJson(response));
		httpResponsePostOK      = mockedRESTManager.new Pair<>(200, Serialization.gsonPretty.toJson(response));
		httpResponsePostNOK     = mockedRESTManager.new Pair<>(400, Serialization.gsonPretty.toJson(response));
		httpResponseErr         = mockedRESTManager.new Pair<>(200, "{");
	}

	@Before
	public void createRequestAndResponse() {
		request = new SORequest();
		SORequestStatus requestStatus = new SORequestStatus();
		requestStatus.setRequestState("COMPLETE");
		request.setRequestStatus(requestStatus);
		request.setRequestId(UUID.randomUUID());
		
		response = new SOResponse();
		
		SORequestReferences requestReferences = new SORequestReferences();
		String requestId = UUID.randomUUID().toString();
		requestReferences.setRequestId(requestId);
		response.setRequestReferences(requestReferences);
		
		response.setRequest(request);
	}

	@Test
	public void testSOInitiation() {
		assertNotNull(new SOManager());
	}

	@Test
	public void testCreateModuleInstance() throws InterruptedException {
		SOManager manager = new SOManager();
		manager.setRestManager(mockedRESTManager);
		
		assertNull(manager.createModuleInstance("http://somewhere.over.the.rainbow", "http://somewhere.over.the.rainbow/InOz", "Dorothy", "OK", request));
		
		when(mockedRESTManager.post(startsWith("http://somewhere.over.the.rainbow"), eq("Dorothy"), eq("Null"), anyMap(), anyString(), anyString()))
		.thenReturn(null);
		assertNull(manager.createModuleInstance("http://somewhere.over.the.rainbow", "http://somewhere.over.the.rainbow/InOz", "Dorothy", "Null", request));
		
		when(mockedRESTManager.post(startsWith("http://somewhere.over.the.rainbow"), eq("Dorothy"), eq("Not202"), anyMap(), anyString(), anyString()))
		.thenReturn(httpResponseErr);
		assertNull(manager.createModuleInstance("http://somewhere.over.the.rainbow", "http://somewhere.over.the.rainbow/InOz", "Dorothy", "Not202", request));
		
		when(mockedRESTManager.post(startsWith("http://somewhere.over.the.rainbow"), eq("Dorothy"), eq("PutOKGetNull"), anyMap(), anyString(), anyString()))
		.thenReturn(httpResponsePutOK);
		when(mockedRESTManager.get(startsWith("http://somewhere.over.the.rainbow/InOz"), eq("Dorothy"), eq("PutOKGetNull"), anyMap()))
		.thenReturn(null);
		assertNull(manager.createModuleInstance("http://somewhere.over.the.rainbow", "http://somewhere.over.the.rainbow/InOz", "Dorothy", "PutOKGetNull", request));
		
		when(mockedRESTManager.post(startsWith("http://somewhere.over.the.rainbow"), eq("Dorothy"), eq("PutOKGetOK"), anyMap(), anyString(), anyString()))
		.thenReturn(httpResponsePutOK);
		when(mockedRESTManager.get(startsWith("http://somewhere.over.the.rainbow/InOz"), eq("Dorothy"), eq("PutOKGetOK"), anyMap()))
		.thenReturn(httpResponseGetOK);
		request.getRequestStatus().setRequestState("COMPLETE");
		SOResponse response = manager.createModuleInstance("http://somewhere.over.the.rainbow", "http://somewhere.over.the.rainbow/InOz", "Dorothy", "PutOKGetOK", request);
		assertNotNull(response);
		assertEquals("COMPLETE", response.getRequest().getRequestStatus().getRequestState());

		response.getRequest().getRequestStatus().setRequestState("FAILED");
		Pair<Integer, String> httpResponseGetOKRequestFailed = mockedRESTManager.new Pair<>(200, Serialization.gsonPretty.toJson(response));
		when(mockedRESTManager.post(startsWith("http://somewhere.over.the.rainbow"), eq("Dorothy"), eq("PutOKGetOKReqFailed"), anyMap(), anyString(), anyString()))
		.thenReturn(httpResponsePutOK);
		when(mockedRESTManager.get(startsWith("http://somewhere.over.the.rainbow/InOz"), eq("Dorothy"), eq("PutOKGetOKReqFailed"), anyMap()))
		.thenReturn(httpResponseGetOKRequestFailed);
		response = manager.createModuleInstance("http://somewhere.over.the.rainbow", "http://somewhere.over.the.rainbow/InOz", "Dorothy", "PutOKGetOKReqFailed", request);
		assertNotNull(response);
		assertEquals("FAILED", response.getRequest().getRequestStatus().getRequestState());

		when(mockedRESTManager.post(startsWith("http://somewhere.over.the.rainbow"), eq("Dorothy"), eq("PutOKGetBadJSON"), anyMap(), anyString(), anyString()))
		.thenReturn(httpResponsePutOK);
		when(mockedRESTManager.get(startsWith("http://somewhere.over.the.rainbow/InOz"), eq("Dorothy"), eq("PutOKGetBadJSON"), anyMap()))
		.thenReturn(httpResponseErr);
		assertNull(manager.createModuleInstance("http://somewhere.over.the.rainbow", "http://somewhere.over.the.rainbow/InOz", "Dorothy", "PutOKGetBadJSON", request));

		response.getRequest().getRequestStatus().setRequestState("IN-PROGRESS");
		Pair<Integer, String> httpResponseGetOKRequestTimeout = mockedRESTManager.new Pair<>(200, Serialization.gsonPretty.toJson(response));
		when(mockedRESTManager.post(startsWith("http://somewhere.over.the.rainbow"), eq("Dorothy"), eq("PutOKGetOKReqTimeout"), anyMap(), anyString(), anyString()))
		.thenReturn(httpResponsePutOK);
		when(mockedRESTManager.get(startsWith("http://somewhere.over.the.rainbow/InOz"), eq("Dorothy"), eq("PutOKGetOKReqTimeout"), anyMap()))
		.thenReturn(httpResponseGetOKRequestTimeout);
		
		manager.setRestGetTimeout(10);
		response = manager.createModuleInstance("http://somewhere.over.the.rainbow", "http://somewhere.over.the.rainbow/InOz", "Dorothy", "PutOKGetOKReqTimeout", request);
		assertNotNull(response);
		assertEquals("IN-PROGRESS", response.getRequest().getRequestStatus().getRequestState());
	}

	@Test
	public void testAsyncSORestCall() throws InterruptedException {
		PolicyEngine.manager.getEnvironment().put("so.url", "http://somewhere.over.the.rainbow.null");
		PolicyEngine.manager.getEnvironment().put("so.username", "Dorothy");
		PolicyEngine.manager.getEnvironment().put("so.password", "OK");

		SOManager manager = new SOManager();
		manager.setRestManager(mockedRESTManager);
		manager.setRestGetTimeout(10);
		
		String serviceInstanceId = UUID.randomUUID().toString();
		String vnfInstanceId = UUID.randomUUID().toString();
		
		when(mockedRESTManager.post(startsWith("http://somewhere.over.the.rainbow.null"), eq("policy"), eq("policy"), anyMap(), anyString(), anyString()))
		.thenReturn(null);

		Future<?> asyncRestCallFuture = manager.asyncSORestCall(request.getRequestId().toString(), mockedWorkingMemory, serviceInstanceId, vnfInstanceId, request);
		try {
			assertNull(asyncRestCallFuture.get());
		}
		catch (Exception e) {
			fail("test should not throw an exception");
		}

		PolicyEngine.manager.getEnvironment().put("so.url", "http://somewhere.over.the.rainbow.err");
		when(mockedRESTManager.post(startsWith("http://somewhere.over.the.rainbow.err"), eq("policy"), eq("policy"), anyMap(), anyString(), anyString()))
		.thenReturn(httpResponseErr);

		asyncRestCallFuture = manager.asyncSORestCall(request.getRequestId().toString(), mockedWorkingMemory, serviceInstanceId, vnfInstanceId, request);
		try {
			assertNull(asyncRestCallFuture.get());
		}
		catch (Exception e) {
			System.err.println(e);
			fail("test should not throw an exception");
		}
		
		PolicyEngine.manager.getEnvironment().put("so.url", "http://somewhere.over.the.rainbow.ok");
		when(mockedRESTManager.post(startsWith("http://somewhere.over.the.rainbow.ok"), eq("policy"), eq("policy"), anyMap(), anyString(), anyString()))
		.thenReturn(httpResponsePostOK);

		asyncRestCallFuture = manager.asyncSORestCall(request.getRequestId().toString(), mockedWorkingMemory, serviceInstanceId, vnfInstanceId, request);
		try {
			assertNull(asyncRestCallFuture.get());
		}
		catch (Exception e) {
			System.err.println(e);
			fail("test should not throw an exception");
		}

		when(mockedRESTManager.post(startsWith("http://somewhere.over.the.rainbow.ok"), eq("policy"), eq("policy"), anyMap(), anyString(), anyString()))
		.thenReturn(httpResponsePostNOK);

		response.getRequest().getRequestStatus().setRequestState("FAILED");
		Pair<Integer, String> httpResponseGetOKRequestFailed0 = mockedRESTManager.new Pair<>(200, Serialization.gsonPretty.toJson(response));
		when(mockedRESTManager.post(startsWith("http://somewhere.over.the.rainbow.nok0"), eq("policy"), eq("policy"), anyMap(), anyString(), anyString()))
		.thenReturn(httpResponseGetOKRequestFailed0);

		response.getRequest().getRequestStatus().setRequestState("SOMETHING_ELSE");
		Pair<Integer, String> httpResponseGetOKRequestFailed1 = mockedRESTManager.new Pair<>(200, Serialization.gsonPretty.toJson(response));
		when(mockedRESTManager.post(startsWith("http://somewhere.over.the.rainbow.nok1"), eq("policy"), eq("policy"), anyMap(), anyString(), anyString()))
		.thenReturn(httpResponseGetOKRequestFailed1);

		PolicyEngine.manager.getEnvironment().put("so.url", "http://somewhere.over.the.rainbow.ok");
		asyncRestCallFuture = manager.asyncSORestCall(request.getRequestId().toString(), mockedWorkingMemory, serviceInstanceId, vnfInstanceId, request);
		try {
			assertNull(asyncRestCallFuture.get());
		}
		catch (Exception e) {
			System.err.println(e);
			fail("test should not throw an exception");
		}
		
		PolicyEngine.manager.getEnvironment().put("so.url", "http://somewhere.over.the.rainbow.nok0");
		asyncRestCallFuture = manager.asyncSORestCall(request.getRequestId().toString(), mockedWorkingMemory, serviceInstanceId, vnfInstanceId, request);
		try {
			assertNull(asyncRestCallFuture.get());
		}
		catch (Exception e) {
			System.err.println(e);
			fail("test should not throw an exception");
		}
		
		PolicyEngine.manager.getEnvironment().put("so.url", "http://somewhere.over.the.rainbow.nok1");
		asyncRestCallFuture = manager.asyncSORestCall(request.getRequestId().toString(), mockedWorkingMemory, serviceInstanceId, vnfInstanceId, request);
		try {
			assertNull(asyncRestCallFuture.get());
		}
		catch (Exception e) {
			System.err.println(e);
			fail("test should not throw an exception");
		}
	}
}
