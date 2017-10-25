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
import java.util.List;

import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.appc.CommonHeader;
import org.onap.policy.appc.Request;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.vnf.trafficgenerator.PGRequest;
import org.onap.policy.vnf.trafficgenerator.PGStream;
import org.onap.policy.vnf.trafficgenerator.PGStreams;
import org.onap.policy.controlloop.actorServiceProvider.spi.Actor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;


public class APPCActorServiceProvider implements Actor {

	private static final ImmutableList<String> recipes = ImmutableList.of("Restart", "Rebuild", "Migrate", "ModifyConfig");
	private static final ImmutableMap<String, List<String>> targets = new ImmutableMap.Builder<String, List<String>>()
										.put("Restart", ImmutableList.of("VM"))
										.put("Rebuild", ImmutableList.of("VM"))
										.put("Migrate", ImmutableList.of("VM"))
										.put("ModifyConfig", ImmutableList.of("VNF"))
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

	/**
	 * Constructs an APPC request conforming to the legacy API.
	 * The legacy API will be deprecated in future releases as
	 * all legacy functionality is moved into the LCM API.
	 * 
	 * @param onset
	 *         the event that is reporting the alert for policy
     *            to perform an action
	 * @param operation
	 *         the control loop operation specifying the actor,
     *         operation, target, etc.
	 * @param policy
	 *         the policy the was specified from the yaml generated
     *         by CLAMP or through the Policy GUI/API
	 * @return an APPC request conforming to the legacy API
	 * @throws AAIException 
	 */
	public static Request constructRequest(VirtualControlLoopEvent onset, ControlLoopOperation operation,
	                Policy policy, String targetVnf) {
		/*
		 * Construct an APPC request
		 */
		Request request = new Request();
		request.CommonHeader = new CommonHeader();
		request.CommonHeader.RequestID = onset.requestID;
		request.CommonHeader.SubRequestID = operation.subRequestId;
		request.Action = policy.getRecipe().substring(0, 1).toUpperCase() 
                        + policy.getRecipe().substring(1);
	
		/*
		 * For now Policy generates the PG Streams as a demo, in the
		 * future the payload can be provided by CLAMP
		 */
		request.Payload.put("generic-vnf.vnf-id", targetVnf);
		
		PGRequest pgRequest = new PGRequest();
		pgRequest.pgStreams = new PGStreams();
		
		PGStream pgStream;
		for (int i = 0; i < 5; i++) {
		    pgStream = new PGStream();
		    pgStream.streamId = "fw_udp"+(i+1);
            pgStream.isEnabled = "true";
            pgRequest.pgStreams.pgStream.add(pgStream);
		}
		request.Payload.put("pg-streams", pgRequest.pgStreams);
		
		/*
		 * Return the request
		 */
		
		return request;
	}
	
	
}
