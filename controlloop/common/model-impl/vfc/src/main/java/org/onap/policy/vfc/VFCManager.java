/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2017-2018 Intel Corp, AT&T. All rights reserved.
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

import java.util.HashMap;
import java.util.Map;

import org.drools.core.WorkingMemory;
import org.onap.policy.vfc.util.Serialization;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.rest.RESTManager;
import org.onap.policy.rest.RESTManager.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;

public final class VFCManager implements Runnable {
	private static final String SYSTEM_LS = System.lineSeparator();
	
	private String vfcUrlBase;
	private String username;
	private String password;
	private VFCRequest vfcRequest;
	private WorkingMemory workingMem;
	private static final Logger logger = LoggerFactory.getLogger(VFCManager.class);
	private static final Logger netLogger = LoggerFactory.getLogger(org.onap.policy.drools.event.comm.Topic.NETWORK_LOGGER);

	// The REST manager used for processing REST calls for this VFC manager
	private RESTManager restManager;
	
	public VFCManager(WorkingMemory wm, VFCRequest request) {
		if (wm == null || request == null) {
			throw new IllegalArgumentException("the parameters \"wm\" and \"request\" on the VFCManager constructor may not be null");
		}
		workingMem = wm;
		vfcRequest = request;

		restManager = new RESTManager();

		// use getPEManagerEnvProperty() for required properties; others are optional
        setVFCParams(getPEManagerEnvProperty("vfc.url"), PolicyEngine.manager.getEnvironmentProperty("vfc.username"),
                        PolicyEngine.manager.getEnvironmentProperty("vfc.password"));
	}

	public void setVFCParams(String baseUrl, String name, String pwd) {
		vfcUrlBase = baseUrl + "/api/nslcm/v1";
		username = name;
		password = pwd;
	}

	@Override
	public void run() {
		Map<String, String> headers = new HashMap<>();
		Pair<Integer, String> httpDetails;

		VFCResponse responseError = new VFCResponse();
		responseError.setResponseDescriptor(new VFCResponseDescriptor());
		responseError.getResponseDescriptor().setStatus("error");

		headers.put("Accept", "application/json");
		String vfcUrl = vfcUrlBase + "/ns/" + vfcRequest.getNSInstanceId() + "/heal";
		try {
			String vfcRequestJson = Serialization.gsonPretty.toJson(vfcRequest);
			netLogger.info("[OUT|{}|{}|]{}{}", "VFC", vfcUrl, SYSTEM_LS, vfcRequestJson);

			httpDetails = restManager.post(vfcUrl, username, password, headers, "application/json", vfcRequestJson);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			workingMem.insert(responseError);
			return;
		}

		if (httpDetails == null) {
			workingMem.insert(responseError);
			return;
		}

		if (httpDetails.a != 202) {
			logger.warn("VFC Heal Restcall failed");
			return;
		}

		try {
			VFCResponse response = Serialization.gsonPretty.fromJson(httpDetails.b, VFCResponse.class);
			netLogger.info("[IN|{}|{}|]{}{}", "VFC", vfcUrl, SYSTEM_LS, httpDetails.b);
			String body = Serialization.gsonPretty.toJson(response);
			logger.debug("Response to VFC Heal post:");
			logger.debug(body);

			String jobId = response.getJobId();
			int attemptsLeft = 20;

			String urlGet = vfcUrlBase + "/jobs/" + jobId;
			VFCResponse responseGet = null;

			while (attemptsLeft-- > 0) {
				netLogger.info("[OUT|{}|{}|]", "VFC", urlGet);
				Pair<Integer, String> httpDetailsGet = restManager.get(urlGet, username, password, headers);
				responseGet = Serialization.gsonPretty.fromJson(httpDetailsGet.b, VFCResponse.class);
				netLogger.info("[IN|{}|{}|]{}{}", "VFC", urlGet, SYSTEM_LS, httpDetailsGet.b);
				responseGet.setRequestId(vfcRequest.getRequestId().toString());
				body = Serialization.gsonPretty.toJson(responseGet);
				logger.debug("Response to VFC Heal get:");
				logger.debug(body);

				String responseStatus = responseGet.getResponseDescriptor().getStatus();
				if (httpDetailsGet.a == 200 && ("finished".equalsIgnoreCase(responseStatus) || "error".equalsIgnoreCase(responseStatus))) {
					logger.debug("VFC Heal Status {}", responseGet.getResponseDescriptor().getStatus());
					workingMem.insert(responseGet);
					break;
				}
				Thread.sleep(20000);
			}
			if ((attemptsLeft <= 0)
					&& (responseGet != null)
					&& (responseGet.getResponseDescriptor() != null)
					&& (responseGet.getResponseDescriptor().getStatus() != null)
					&& (!responseGet.getResponseDescriptor().getStatus().isEmpty())) {
				logger.debug("VFC timeout. Status: ({})", responseGet.getResponseDescriptor().getStatus());
				workingMem.insert(responseGet);
			}
		} catch (JsonSyntaxException e) {
			logger.error("Failed to deserialize into VFCResponse {}", e.getLocalizedMessage(), e);
		} catch (InterruptedException e) {
			logger.error("Interrupted exception: {}", e.getLocalizedMessage(), e);
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			logger.error("Unknown error deserializing into VFCResponse {}", e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Protected setter for rest manager to allow mocked rest manager to be used for testing 
	 * @param restManager the test REST manager
	 */
	protected void setRestManager(final RESTManager restManager) {
		this.restManager = restManager;
	}
	
	/**
	 * This method reads and validates environmental properties coming from the policy engine. Null properties cause
	 * an {@link IllegalArgumentException} runtime exception to be thrown 
	 * @param string the name of the parameter to retrieve
	 * @return the property value
	 */

	private String getPEManagerEnvProperty(String enginePropertyName) {
		String enginePropertyValue = PolicyEngine.manager.getEnvironmentProperty(enginePropertyName);
		if (enginePropertyValue == null) {
			throw new IllegalArgumentException("The value of policy engine manager environment property \"" + enginePropertyName + "\" may not be null");
		}
		return enginePropertyValue;
	}
}
