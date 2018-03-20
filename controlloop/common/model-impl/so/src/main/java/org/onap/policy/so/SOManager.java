/*-
 * ============LICENSE_START=======================================================
 * so
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.onap.policy.so.util.Serialization;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.rest.RESTManager;
import org.onap.policy.rest.RESTManager.Pair;
import org.drools.core.WorkingMemory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public final class SOManager {

	private static final Logger logger = LoggerFactory.getLogger(SOManager.class);
	private static final Logger netLogger = LoggerFactory.getLogger(org.onap.policy.drools.event.comm.Topic.NETWORK_LOGGER);
	private static ExecutorService executors = Executors.newCachedThreadPool();

	static final String MEDIA_TYPE = "application/json";

	static final String LINE_SEPARATOR = System.lineSeparator();

	// REST get timeout value in milliseconds
	private static final long DEFAULT_GET_REQUEST_TIMEOUT = 20000;

	// The REST manager used for processing REST calls for this VFC manager
	private RESTManager restManager;

	private long restGetTimeout = DEFAULT_GET_REQUEST_TIMEOUT;

	public SOManager() {
		restManager = new RESTManager();
	}

	public SOResponse createModuleInstance(String url, String urlBase, String username, String password, SORequest request) {

		//
		// Call REST
		//
		Map<String, String> headers = new HashMap<>();
		headers.put("Accept", MEDIA_TYPE);

		//
		// 201 - CREATED - you are done just return
		//
		String requestJson = Serialization.gsonPretty.toJson(request);
		netLogger.info("[OUT|{}|{}|{}|{}|{}|{}|]{}{}", "SO", url, username, password, headers, MEDIA_TYPE, LINE_SEPARATOR, requestJson);
		Pair<Integer, String> httpDetails =	restManager.post(url, username, password, headers, MEDIA_TYPE, requestJson);

		if (httpDetails == null) {
			return null;
		}

		if (httpDetails.a != 202) {
			return null;
		}

		try {
			SOResponse response = Serialization.gsonPretty.fromJson(httpDetails.b, SOResponse.class);

			String body = Serialization.gsonPretty.toJson(response);
			logger.debug("***** Response to post:");
			logger.debug(body);

			String requestId = response.getRequestReferences().getRequestId();

			String urlGet = urlBase + "/orchestrationRequests/v2/" + requestId;
			SOResponse responseGet = null;

			for (int attemptsLeft = 20; attemptsLeft > 0; attemptsLeft--) {
				Pair<Integer, String> httpDetailsGet = restManager.get(urlGet, username, password, headers);
				if (!httpResultIsNullFree(httpDetailsGet)) {
					return null;
				}

				responseGet = Serialization.gsonPretty.fromJson(httpDetailsGet.b, SOResponse.class);
				netLogger.info("[IN|{}|{}|]{}{}", "SO", urlGet, LINE_SEPARATOR, httpDetailsGet.b);

				body = Serialization.gsonPretty.toJson(responseGet);
				logger.debug("***** Response to get:");
				logger.debug(body);

				if (httpDetailsGet.a == 200  &&
						(responseGet.getRequest().getRequestStatus().getRequestState().equalsIgnoreCase("COMPLETE")
								|| responseGet.getRequest().getRequestStatus().getRequestState().equalsIgnoreCase("FAILED"))) {
					logger.debug("***** ########  VF Module Creation {}",
							responseGet.getRequest().getRequestStatus().getRequestState());
					return responseGet;
				}
				Thread.sleep(restGetTimeout);
			}

			if (responseGet != null && responseGet.getRequest() != null
					&& responseGet.getRequest().getRequestStatus() != null
					&& responseGet.getRequest().getRequestStatus().getRequestState() != null) {
				logger.warn("***** ########  VF Module Creation timeout. Status: ( {})",
						responseGet.getRequest().getRequestStatus().getRequestState());
			}

			return responseGet;
		}
		catch (JsonSyntaxException e) {
			logger.error("Failed to deserialize into SOResponse: ", e);
		}
		catch (InterruptedException e) {
			logger.error("Interrupted exception: ", e);
			Thread.currentThread().interrupt();
		}

		return null;
	}

	/**
	 * 
	 * @param wm
	 * @param url
	 * @param urlBase
	 * @param username
	 * @param password
	 * @param request
	 * 
	 *        This method makes an asynchronous Rest call to MSO and inserts the response into the
	 *        Drools working memory
	 * @return 
	 */
	public Future<?> asyncSORestCall(String requestID, WorkingMemory wm, String serviceInstanceId,	String vnfInstanceId, SORequest request) {
		return executors.submit(new AsyncSORestCallThread(requestID, wm, serviceInstanceId, vnfInstanceId, request));
	}

	private class AsyncSORestCallThread implements Runnable {
		final String requestID;
		final WorkingMemory wm;
		final String serviceInstanceId;
		final String vnfInstanceId;
		final SORequest request;

		private AsyncSORestCallThread(final String requestID, final WorkingMemory wm, final String serviceInstanceId,	final String vnfInstanceId, final SORequest request) {
			this.requestID = requestID;
			this.wm = wm;
			this.serviceInstanceId = serviceInstanceId;
			this.vnfInstanceId = vnfInstanceId;
			this.request = request;
		}

		@Override
		public void run() {
			try {
				String serverRoot = PolicyEngine.manager.getEnvironmentProperty("so.url");
				String username = PolicyEngine.manager.getEnvironmentProperty("so.username");
				String password = PolicyEngine.manager.getEnvironmentProperty("so.password");

				String url = serverRoot + "/serviceInstances/v5/" + serviceInstanceId + "/vnfs/"
						+ vnfInstanceId + "/vfModules";

				String auth = username + ":" + password;

				Map<String, String> headers = new HashMap<>();
				byte[] encodedBytes = Base64.getEncoder().encode(auth.getBytes());
				headers.put("Accept", MEDIA_TYPE);
				headers.put("Authorization", "Basic " + new String(encodedBytes));

				Gson gsonPretty = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

				String soJson = gsonPretty.toJson(request);
				SOResponse so = new SOResponse();
				
				int attemptsLeft = 20;
				while (attemptsLeft > 0) {
					netLogger.info("[OUT|{}|{}|]{}{}", "SO", url, LINE_SEPARATOR, soJson);
					Pair<Integer, String> httpResponse = restManager.post(url, "policy",	"policy", headers, MEDIA_TYPE, soJson);

					if (!httpResultIsNullFree(httpResponse)) {
						logger.error("SO Response or Response status/code is null.");
						so.setHttpResponseCode(999);
						break;
					}	
					
					netLogger.info("[IN|{}|{}|]{}{}", url, "SO", LINE_SEPARATOR,	httpResponse.b);
					so = new Gson().fromJson(httpResponse.b, SOResponse.class);

					if (httpResponse.a == 200  &&
							(so.getRequest().getRequestStatus().getRequestState().equalsIgnoreCase("COMPLETE")
									|| so.getRequest().getRequestStatus().getRequestState().equalsIgnoreCase("FAILED"))) {
						so.setHttpResponseCode(httpResponse.a);
						attemptsLeft = 0;
					}
					else {
						Thread.sleep(restGetTimeout);
						attemptsLeft--;
					}
				}

				SOResponseWrapper soWrapper = new SOResponseWrapper(so, requestID);
				wm.insert(soWrapper);
				if (logger.isInfoEnabled()) {
					logger.info("SOResponse inserted " + gsonPretty.toJson(soWrapper));
				}
			}
			catch (Exception e) {
				logger.error("Error while performing asyncSORestCall: " + e.getMessage(), e);

				// create dummy SO object to trigger cleanup
				SOResponse so = new SOResponse();
				so.setHttpResponseCode(999);
				wm.insert(so);
			}
		}
	}

	/**
	 * method to allow tuning of REST get timeout 
	 * @param restGetTimeout the timeout value
	 */
	protected void setRestGetTimeout(final long restGetTimeout) {
		this.restGetTimeout = restGetTimeout;
	}

	/**
	 * Protected setter for rest manager to allow mocked rest manager to be used for testing 
	 * @param restManager the test REST manager
	 */
	protected void setRestManager(final RESTManager restManager) {
		this.restManager = restManager;
	}

	/**
	 * Check that a HTTP operation result has no nulls
	 * @param httpOperationResult the result to check
	 * @return true if no nulls are found
	 */
	private boolean httpResultIsNullFree(Pair<Integer, String> httpOperationResult) {
		return httpOperationResult != null && httpOperationResult.a != null && httpOperationResult.b != null;
	}
}
