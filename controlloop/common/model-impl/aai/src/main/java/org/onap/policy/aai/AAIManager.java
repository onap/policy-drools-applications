/*-
 * ============LICENSE_START=======================================================
 * aai
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

package org.onap.policy.aai;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.onap.policy.aai.util.Serialization;
import org.onap.policy.rest.RESTManager;
import org.onap.policy.rest.RESTManager.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;

public final class AAIManager {
	private static final String LINE_SEPARATOR = System.lineSeparator();

	/**
	 * Private constructor added to avoid instantiation of static class
	 */
	private AAIManager() {
	}

	private static final Logger logger = LoggerFactory.getLogger(AAIManager.class);
	private static final Logger netLogger = LoggerFactory.getLogger(org.onap.policy.drools.event.comm.Topic.NETWORK_LOGGER);

	public static AAINQResponse	postQuery(String url, String username, String password, AAINQRequest request, UUID requestID) {

		Map<String, String> headers = createHeaders(requestID);

		url = url + "/aai/search/named-query";

		logger.debug("RESTManager.post before");
		String requestJson = Serialization.gsonPretty.toJson(request);
		netLogger.info("[OUT|{}|{}|]{}{}", "AAI", url, LINE_SEPARATOR, requestJson);
		Pair<Integer, String> httpDetails = RESTManager.post(url, username, password, headers, "application/json", requestJson);
		logger.debug("RESTManager.post after");

		if (httpDetails == null) {
			logger.info("AAI POST Null Response to {}", url);
			return null;
		}

		int httpResponseCode = httpDetails.a;
		
		logger.info(url);
		logger.info("{}", httpResponseCode);
		logger.info(httpDetails.b);
		
		if (httpDetails.b != null) {
			return composeResponse(httpDetails, url, AAINQResponse.class);
		}
		return null;
	}

	public static AAIGETVserverResponse getQueryByVserverName(String urlGet, String username, String password, UUID requestID, String key) {

		Map<String, String> headers = createHeaders(requestID);

		urlGet = urlGet + key; 

		int attemptsLeft = 3;

		while(attemptsLeft-- > 0){

			netLogger.info("[OUT|{}|{}|]", "AAI", urlGet);
			Pair<Integer, String> httpDetailsGet = RESTManager.get(urlGet, username, password, headers);
			if (httpDetailsGet == null) {
				logger.info("AAI GET Null Response to {}", urlGet);
				return null;
			}

			int httpResponseCode = httpDetailsGet.a;
			
			logger.info(urlGet);
			logger.info("{}", httpResponseCode);
			logger.info(httpDetailsGet.b);

			if (httpResponseCode == 200) {
				AAIGETVserverResponse responseGet = composeResponse(httpDetailsGet, urlGet, AAIGETVserverResponse.class);
				if (responseGet != null) {
					return responseGet;
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		}

		return null;
	}

	public static AAIGETVnfResponse getQueryByVnfID(String urlGet, String username, String password, UUID requestID, String key) {

		Map<String, String> headers = createHeaders(requestID); 

		urlGet = urlGet + key; 

		int attemptsLeft = 3;

		while(attemptsLeft-- > 0){
			netLogger.info("[OUT|{}|{}|]", "AAI", urlGet);
			Pair<Integer, String> httpDetailsGet = RESTManager.get(urlGet, username, password, headers);
			if (httpDetailsGet == null) {
				logger.info("AAI GET Null Response to {}", urlGet);
				return null;
			}

			int httpResponseCode = httpDetailsGet.a;
			
			logger.info(urlGet);
			logger.info("{}", httpResponseCode);
			logger.info(httpDetailsGet.b);

			if (httpResponseCode == 200) {
				AAIGETVnfResponse responseGet = composeResponse(httpDetailsGet, urlGet, AAIGETVnfResponse.class);
				if (responseGet != null) {
					return responseGet;
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) { Thread.currentThread().interrupt(); }

		}

		return null;
	}

	public static AAIGETVnfResponse getQueryByVnfName(String urlGet, String username, String password, UUID requestID, String key) {
		return getQueryByVnfID(urlGet, username, password, requestID, key);
	}

	/**
	 * Create the headers for the HTTP request
	 * @param requestID the request ID to insert in the headers
	 * @return the HTTP headers
	 */
	private static Map<String, String> createHeaders(final UUID requestID) {
		Map<String, String> headers = new HashMap<>();

		headers.put("X-FromAppId", "POLICY");
		headers.put("X-TransactionId", requestID.toString());
		headers.put("Accept", "application/json");

		return headers;
	}

	/**
	 * This method uses Google's GSON to create a response object from a JSON string
	 * @param httpDetails the HTTP response
	 * @param url the URL from which the response came
	 * @param classOfT The response class
	 * @return an instance of the response class
	 * @throws JsonSyntaxException on GSON errors instantiating the response
	 */
	private static <T> T composeResponse(final Pair<Integer, String> httpDetails, final String url, final Class<T> classOfT) {
		try {
			T response = Serialization.gsonPretty.fromJson(httpDetails.b, classOfT);
			netLogger.info("[IN|{}|{}|]{}{}", "AAI", url, LINE_SEPARATOR, httpDetails.b);
			return response;
		} catch (JsonSyntaxException e) {
			logger.error("postQuery threw: ", e);
			return null;
		}
	}
}
