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

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.onap.policy.mso.util.Serialization;
import org.onap.policy.rest.RESTManager;
import org.onap.policy.rest.RESTManager.Pair;
import org.drools.core.WorkingMemory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public final class SOManager {

	private static final Logger logger = LoggerFactory.getLogger(SOManager.class);
	private static ExecutorService executors = Executors.newCachedThreadPool();
		
	public static SOResponse createModuleInstance(String url, String urlBase, String username, String password, SORequest request) {
		
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
				SOResponse response = Serialization.gsonPretty.fromJson(httpDetails.b, SOResponse.class);
				
				String body = Serialization.gsonPretty.toJson(response);
				logger.debug("***** Response to post:");
				logger.debug(body);
				
				String requestId = response.requestReferences.requestId;
				int attemptsLeft = 20;
				
				//String getUrl = "/orchestrationRequests/v2/"+requestId;
				String urlGet = urlBase + "/orchestrationRequests/v2/"+requestId;
				SOResponse responseGet = null;
				
				while(attemptsLeft-- > 0){
					
					Pair<Integer, String> httpDetailsGet = RESTManager.get(urlGet, username, password, headers);
					responseGet = Serialization.gsonPretty.fromJson(httpDetailsGet.b, SOResponse.class);
					body = Serialization.gsonPretty.toJson(responseGet);
					logger.debug("***** Response to get:");
					logger.debug(body);
					
					if(httpDetailsGet.a == 200){
						if(responseGet.request.requestStatus.requestState.equalsIgnoreCase("COMPLETE") || 
								responseGet.request.requestStatus.requestState.equalsIgnoreCase("FAILED")){
							logger.debug("***** ########  VF Module Creation "+responseGet.request.requestStatus.requestState);
							return responseGet;
						}
					}
					Thread.sleep(20000);
				}

				if (responseGet != null
				 && responseGet.request != null
				 &&	responseGet.request.requestStatus != null
				 && responseGet.request.requestStatus.requestState != null) {
					logger.warn("***** ########  VF Module Creation timeout. Status: ( {})", responseGet.request.requestStatus.requestState);
				}

				return responseGet;
			} catch (JsonSyntaxException e) {
				logger.error("Failed to deserialize into SOResponse: ", e);
			} catch (InterruptedException e) {
				logger.error("Interrupted exception: ", e);
			}
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
	 * This method makes an asynchronous Rest call to MSO and inserts the response into the Drools working memory
	 */
	  public void asyncMSORestCall(WorkingMemory wm, String serviceInstanceId, String vnfInstanceId, SORequest request) {
		  executors.submit(new Runnable()
		  	{
			  @Override
			  	public void run()
			  {
			  	String serverRoot = ""; // TODO
			  	String username = ""; // TODO
			  	String password = ""; // TODO
				String url = serverRoot + "/serviceInstances/v5/" + serviceInstanceId + "/vnfs/" + vnfInstanceId + "/vfModules";
				
				String auth = username + ":" + password;
				
				Map<String, String> headers = new HashMap<String, String>();
				byte[] encodedBytes = Base64.getEncoder().encode(auth.getBytes());
				headers.put("Accept", "application/json");
				headers.put("Authorization", "Basic " + new String(encodedBytes));
				
				Gson gsonPretty = new GsonBuilder().disableHtmlEscaping()
						.setPrettyPrinting()
						.create();

				String msoJson = gsonPretty.toJson(request);
				
				SOResponse mso = new SOResponse();
				Pair<Integer, String> httpResponse = RESTManager.post(url, "policy", "policy", headers, "application/json", msoJson);
				if (httpResponse != null) {
					Gson gson = new Gson();
					mso = gson.fromJson(httpResponse.b, SOResponse.class);
					mso.httpResponseCode = httpResponse.a;
				}
		
//				logger.info("MSOResponse inserted " + mso.toString());
				wm.insert(mso);
			  }
		  	});
	  }

}
