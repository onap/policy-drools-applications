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

package org.onap.policy.aai.AAINQF199;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.onap.policy.aai.AAIGETResponse;
import org.onap.policy.aai.util.Serialization;
import org.onap.policy.rest.RESTManager;
import org.onap.policy.rest.RESTManager.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;

public final class AAINQF199Manager {
	private static Logger logger = LoggerFactory.getLogger(AAINQF199Manager.class);
	
	public static AAINQF199Response	postQuery(String url, String username, String password, AAINQF199Request request, UUID requestID) {
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("X-FromAppId", "POLICY");
		headers.put("X-TransactionId", requestID.toString());
		headers.put("Accept", "application/json");
		
		url = url + "/aai/search/named-query";

		Pair<Integer, String> httpDetails = RESTManager.post(url, username, password, headers, "application/json", Serialization.gsonPretty.toJson(request));

		if (httpDetails == null) {
			System.out.println("AAI POST Null Response to " + url);
			return null;
		}
		
		System.out.println(url);
		System.out.println(httpDetails.a);
		System.out.println(httpDetails.b);
		if (httpDetails.a == 200) {
			try {
				AAINQF199Response response = Serialization.gsonPretty.fromJson(httpDetails.b, AAINQF199Response.class);
				return response;
			} catch (JsonSyntaxException e) {
				logger.error("{}: postQuery threw: ", e);
			}
		}

		return null;
	}
	
	public static AAIGETResponse getQuery(String urlGet, String username, String password, UUID requestID, String vnfId) {
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("X-FromAppId", "POLICY");
		headers.put("X-TransactionId", requestID.toString());
		headers.put("Accept", "application/json");
		
		urlGet = urlGet + "/aai/v8/network/generic-vnfs/generic-vnf/" + vnfId;
		
		int attemptsLeft = 3;
		AAIGETResponse responseGet = null;
		
		while(attemptsLeft-- > 0){
		
			Pair<Integer, String> httpDetailsGet = RESTManager.get(urlGet, username, password, headers);
			if (httpDetailsGet == null) {
				System.out.println("AAI GET Null Response to " + urlGet);
				return null;
			}
			
			System.out.println(urlGet);
			System.out.println(httpDetailsGet.a);
			System.out.println(httpDetailsGet.b);
			
			if (httpDetailsGet.a == 200) {
				try {
					responseGet = Serialization.gsonPretty.fromJson(httpDetailsGet.b, AAIGETResponse.class);
					return responseGet;
				} catch (JsonSyntaxException e) {
					logger.error("{}: getQuery threw: ", e);
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}

		}
		
		return null;
	}

}
