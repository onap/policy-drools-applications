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
	private static final Logger logger = LoggerFactory.getLogger(AAIManager.class);
	private static final Logger netLogger = LoggerFactory.getLogger(org.onap.policy.drools.event.comm.Topic.NETWORK_LOGGER);
	
	public static AAINQResponse	postQuery(String url, String username, String password, AAINQRequest request, UUID requestID) {
		
		Map<String, String> headers = new HashMap<>();
		headers.put("X-FromAppId", "POLICY");
		headers.put("X-TransactionId", requestID.toString());
		headers.put("Accept", "application/json");
		
		url = url + "/aai/search/named-query";

		logger.debug("RESTManager.post before");
		String requestJson = Serialization.gsonPretty.toJson(request);
		netLogger.info("[OUT|{}|{}|]{}{}", "AAI", url, System.lineSeparator(), requestJson);
		Pair<Integer, String> httpDetails = RESTManager.post(url, username, password, headers, "application/json", requestJson);
		logger.debug("RESTManager.post after");
		
		if (httpDetails == null) {
			logger.info("AAI POST Null Response to " + url);
			return null;
		}
		
		logger.info(url);
		logger.info(httpDetails.a.toString());
		logger.info(httpDetails.b);
		if (httpDetails.b != null) {
			try {
				AAINQResponse response = Serialization.gsonPretty.fromJson(httpDetails.b, AAINQResponse.class);
				netLogger.info("[IN|{}|{}|]{}{}", "AAI", url, System.lineSeparator(), httpDetails.b);
				return response;
			} catch (JsonSyntaxException e) {
				logger.error("postQuery threw: ", e);
			}
		}
		return null;
	}
	
	public static AAIGETVserverResponse getQueryByVserverName(String urlGet, String username, String password, UUID requestID, String key) {
		
		Map<String, String> headers = new HashMap<>();
		headers.put("X-FromAppId", "POLICY");
		headers.put("X-TransactionId", requestID.toString());
		headers.put("Accept", "application/json");
		
		urlGet = urlGet + key; 
		
		int attemptsLeft = 3;
		AAIGETVserverResponse responseGet = null;
		
		while(attemptsLeft-- > 0){

			netLogger.info("[OUT|{}|{}|]", "AAI", urlGet);
			Pair<Integer, String> httpDetailsGet = RESTManager.get(urlGet, username, password, headers);
			if (httpDetailsGet == null) {
				logger.info("AAI GET Null Response to " + urlGet);
				return null;
			}
			
			logger.info(urlGet);
			logger.info(httpDetailsGet.a.toString());
			logger.info(httpDetailsGet.b);
			
			if (httpDetailsGet.a == 200) {
				try {
					responseGet = Serialization.gsonPretty.fromJson(httpDetailsGet.b, AAIGETVserverResponse.class);
					netLogger.info("[IN|{}|{}|]{}{}", "AAI", urlGet, System.lineSeparator(), httpDetailsGet.b);
					return responseGet;
				} catch (JsonSyntaxException e) {
					logger.error("postQuery threw: ", e);
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) { Thread.currentThread().interrupt(); }
 		}
		
		return null;
	}
	
	public static AAIGETVnfResponse getQueryByVnfID(String urlGet, String username, String password, UUID requestID, String key) {
		
		Map<String, String> headers = new HashMap<>();
		headers.put("X-FromAppId", "POLICY");
		headers.put("X-TransactionId", requestID.toString());
		headers.put("Accept", "application/json");
		
		urlGet = urlGet + key; 
		
		int attemptsLeft = 3;
		AAIGETVnfResponse responseGet = null;
		
		while(attemptsLeft-- > 0){
			netLogger.info("[OUT|{}|{}|]", "AAI", urlGet);
			Pair<Integer, String> httpDetailsGet = RESTManager.get(urlGet, username, password, headers);
			if (httpDetailsGet == null) {
				logger.info("AAI GET Null Response to " + urlGet);
				return null;
			}
			
			logger.info(urlGet);
			logger.info(httpDetailsGet.a.toString());
			logger.info(httpDetailsGet.b);
			
			if (httpDetailsGet.a == 200) {
				try {
					responseGet = Serialization.gsonPretty.fromJson(httpDetailsGet.b, AAIGETVnfResponse.class);
					netLogger.info("[IN|{}|{}|]{}{}", "AAI", urlGet, System.lineSeparator(), httpDetailsGet.b);
					return responseGet;
				} catch (JsonSyntaxException e) {
					logger.error("postQuery threw: ", e);
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) { Thread.currentThread().interrupt(); }

		}
		
		return null;
	}
	
	public static AAIGETVnfResponse getQueryByVnfName(String urlGet, String username, String password, UUID requestID, String key) {
		
		Map<String, String> headers = new HashMap<>();
		headers.put("X-FromAppId", "POLICY");
		headers.put("X-TransactionId", requestID.toString());
		headers.put("Accept", "application/json");
		
		urlGet = urlGet + key; 
		
		int attemptsLeft = 3;
		AAIGETVnfResponse responseGet = null;
		
		while(attemptsLeft-- > 0){
			netLogger.info("[OUT|{}|{}|]", "AAI", urlGet);
			Pair<Integer, String> httpDetailsGet = RESTManager.get(urlGet, username, password, headers);
			if (httpDetailsGet == null) {
				logger.info("AAI GET Null Response to " + urlGet);
				return null;
			}
			
			logger.info(urlGet);
			logger.info(httpDetailsGet.a.toString());
			logger.info(httpDetailsGet.b);
			
			if (httpDetailsGet.a == 200) {
				try {
					responseGet = Serialization.gsonPretty.fromJson(httpDetailsGet.b, AAIGETVnfResponse.class);
					netLogger.info("[IN|{}|{}|]{}{}", "AAI", urlGet, System.lineSeparator(), httpDetailsGet.b);
					return responseGet;
				} catch (JsonSyntaxException e) {
					logger.error("postQuery threw: ", e);
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) { Thread.currentThread().interrupt(); }

		}
		
		return null;
	}
}
