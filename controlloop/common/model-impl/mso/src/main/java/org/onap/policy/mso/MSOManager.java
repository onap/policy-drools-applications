/*-
 * ============LICENSE_START=======================================================
 * mso
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

package org.onap.policy.mso;

import java.util.HashMap;
import java.util.Map;

import org.onap.policy.mso.util.Serialization;
import org.onap.policy.rest.RESTManager;
import org.onap.policy.rest.RESTManager.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;

public final class MSOManager {

	private static final Logger logger = LoggerFactory.getLogger(MSOManager.class);
	
	public static MSOResponse createModuleInstance(String url, String urlBase, String username, String password, MSORequest request) {
		
		//
		// Call REST
		//
		Map<String, String> headers = new HashMap<String, String>();
		//headers.put("X-FromAppId", "POLICY");
		//headers.put("X-TransactionId", requestID.toString());
		headers.put("Accept", "application/json");
		
		//
		// 201 - CREATED - you are done just return 
		//
		
		Pair<Integer, String> httpDetails = RESTManager.post(url, username, password, headers, "application/json", Serialization.gsonPretty.toJson(request));
		
		if (httpDetails == null) {
			return null;
		}
		
		if (httpDetails.a == 202) {
			try {
				MSOResponse response = Serialization.gsonPretty.fromJson(httpDetails.b, MSOResponse.class);
				
				String body = Serialization.gsonPretty.toJson(response);
				System.out.println("***** Response to post:");
				System.out.println(body);
				
				String requestId = response.requestReferences.requestId;
				int attemptsLeft = 20;
				
				//String getUrl = "/orchestrationRequests/v2/"+requestId;
				String urlGet = urlBase + "/orchestrationRequests/v2/"+requestId;
				MSOResponse responseGet = null;
				
				while(attemptsLeft-- > 0){
					
					Pair<Integer, String> httpDetailsGet = RESTManager.get(urlGet, username, password, headers);
					responseGet = Serialization.gsonPretty.fromJson(httpDetailsGet.b, MSOResponse.class);
					body = Serialization.gsonPretty.toJson(responseGet);
					System.out.println("***** Response to get:");
					System.out.println(body);
					
					if(httpDetailsGet.a == 200){
						if(responseGet.request.requestStatus.requestState.equalsIgnoreCase("COMPLETE") || 
								responseGet.request.requestStatus.requestState.equalsIgnoreCase("FAILED")){
							System.out.println("***** ########  VF Module Creation "+responseGet.request.requestStatus.requestState);
							return responseGet;
						}
					}
					Thread.sleep(20000);
				}
				if (responseGet.request.requestStatus.requestState != null) {
					logger.debug("***** ########  VF Module Creation timeout. Status: ("+responseGet.request.requestStatus.requestState+")");
				}
				return responseGet;
			} catch (JsonSyntaxException e) {
				logger.error("Failed to deserialize into MSOResponse: ", e);
			} catch (InterruptedException e) {
				logger.error("Interrupted exception: ", e);
			}
		}
		
		
		
		
		return null;
	}

}
