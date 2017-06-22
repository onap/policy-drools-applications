/*-
 * ============LICENSE_START=======================================================
 * APPCActorServiceProvider
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

package org.onap.policy.controlloop.actor.appc;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.appc.CommonHeader;
import org.onap.policy.appc.Request;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.policy.Policy;

import org.onap.policy.controlloop.actorServiceProvider.spi.Actor;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;


public class APPCActorServiceProvider implements Actor {

	private static final ImmutableList<String> recipes = ImmutableList.of("Restart", "Rebuild", "Migrate", "ModifyConfig");
	private static final ImmutableMap<String, List<String>> targets = new ImmutableMap.Builder<String, List<String>>()
										.put("Restart", ImmutableList.of("VM"))
										.put("Rebuild", ImmutableList.of("VM"))
										.put("Migrate", ImmutableList.of("VM"))
										.put("ModifyConfig", ImmutableList.of("VFC"))
										.build();
	private static final ImmutableMap<String, List<String>> payloads = new ImmutableMap.Builder<String, List<String>>()
										.put("ModifyConfig", ImmutableList.of("generic-vnf.vnf-id"))
										.build();
	
	@Override
	public String actor() {
		return "APPC";
	}

	@Override
	public List<String> recipes() {
		return ImmutableList.copyOf(recipes);
	}

	@Override
	public List<String> recipeTargets(String recipe) {
		return ImmutableList.copyOf(targets.getOrDefault(recipe, Collections.emptyList()));
	}

	@Override
	public List<String> recipePayloads(String recipe) {
		return ImmutableList.copyOf(payloads.getOrDefault(recipe, Collections.emptyList()));
	}

	
	public static Request constructRequest(VirtualControlLoopEvent onset, ControlLoopOperation operation, Policy policy) {
		//
		// Construct an APPC request
		//
		Request request = new Request();
		request.CommonHeader = new CommonHeader();
		request.CommonHeader.RequestID = onset.requestID;
		request.CommonHeader.SubRequestID = operation.subRequestId;
		request.Action = policy.recipe;
		
		//
		// TODO: do we need to take care of the target
		//
		
		//
		// Handle the payload
		//
		if (policy.payload != null && !policy.payload.isEmpty()) {
			request.Payload = new HashMap<String, Object>();	
			//
			// Add each payload entry
			//
			for (Map.Entry<String, String> entry : policy.payload.entrySet()) {
			//
			// TODO: entry key has ref$, value has {xxxx}
			//
				request.Payload.put(entry.getKey(), entry.getValue());	
			}
		}
		
		
		request.Payload.put("AICVServerSelfLink", onset.AAI.get("vserver.selflink"));//.AICVServerSelfLink);
		request.Payload.put("AICIdentity", onset.AAI.get("cloud-region.identity-url"));//AICIdentity);
		//
		// Return the request
		//
		return request;
	}
	
	
}
