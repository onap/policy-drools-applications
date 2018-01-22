/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2017 Intel Corp. All rights reserved.
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

package org.onap.policy.controlloop.actor.vfc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.vfc.VFCRequest;
import org.onap.policy.vfc.VFCHealRequest;
import org.onap.policy.vfc.VFCHealAdditionalParams;
import org.onap.policy.vfc.VFCHealActionVmInfo;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.rest.RESTManager;
import org.onap.policy.controlloop.actorServiceProvider.spi.Actor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.onap.policy.aai.AAIManager;
import org.onap.policy.aai.AAIGETVnfResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VFCActorServiceProvider implements Actor {
	private static final Logger logger = LoggerFactory.getLogger(VFCActorServiceProvider.class);

	// Strings for VFC Actor
	private static final String VFC_ACTOR  = "VFC";

	// Strings for targets
	private static final String TARGET_VM  = "VM";

	// Strings for recipes
	private static final String RECIPE_RESTART = "Restart";

	private static final ImmutableList<String> recipes = ImmutableList.of(RECIPE_RESTART);
	private static final ImmutableMap<String, List<String>> targets = new ImmutableMap.Builder<String, List<String>>()
			.put(RECIPE_RESTART, ImmutableList.of(TARGET_VM)).build();

	@Override
	public String actor() {
		return VFC_ACTOR;
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
		return Collections.emptyList();
	}

	public static VFCRequest constructRequest(VirtualControlLoopEvent onset, ControlLoopOperation operation,
			Policy policy, AAIGETVnfResponse vnfResponse) {
		
		// Construct an VFC request
		VFCRequest request = new VFCRequest();
		String serviceInstance = onset.getAAI().get("service-instance.service-instance-id");
		if (serviceInstance == null || "".equals(serviceInstance))
		{
			AAIGETVnfResponse tempVnfResp = vnfResponse;
			if(tempVnfResp == null) //if the response is null, we haven't queried
			{
				tempVnfResp = getAAIServiceInstance(onset); //This does the AAI query since we haven't already
				if (tempVnfResp == null)
					return null;
			}
			serviceInstance = tempVnfResp.getServiceId();
		}
		request.setNSInstanceId(serviceInstance);
		request.setRequestId(onset.getRequestID());
		request.setHealRequest(new VFCHealRequest());
		request.getHealRequest().setVnfInstanceId(onset.getAAI().get("generic-vnf.vnf-id"));
		request.getHealRequest().setCause(operation.getMessage());
		request.getHealRequest().setAdditionalParams(new VFCHealAdditionalParams());

		if (policy.getRecipe().toLowerCase().equalsIgnoreCase(RECIPE_RESTART)) {
			request.getHealRequest().getAdditionalParams().setAction("restartvm");
			request.getHealRequest().getAdditionalParams().setActionInfo(new VFCHealActionVmInfo());
			request.getHealRequest().getAdditionalParams().getActionInfo().setVmid(onset.getAAI().get("vserver.vserver-id"));
			request.getHealRequest().getAdditionalParams().getActionInfo().setVmname(onset.getAAI().get("vserver.vserver-name"));
		}
		else {
			return null;
		}
		return request;
	}

	private static AAIGETVnfResponse getAAIServiceInstance(VirtualControlLoopEvent event) {
		AAIGETVnfResponse response = null;
		UUID requestID = event.getRequestID();
		String vnfName = event.getAAI().get("generic-vnf.vnf-name");
		String vnfID = event.getAAI().get("generic-vnf.vnf-id");
		String aaiUrl = PolicyEngine.manager.getEnvironmentProperty("aai.url");
		String aaiUsername = PolicyEngine.manager.getEnvironmentProperty("aai.username");
		String aaiPassword = PolicyEngine.manager.getEnvironmentProperty("aai.password");
		try {
			if (vnfName != null) {
				String url = aaiUrl + "/aai/v11/network/generic-vnfs/generic-vnf?vnf-name=";
				response = new AAIManager(new RESTManager()).getQueryByVnfName(url, aaiUsername, aaiPassword, requestID, vnfName);
			} else if (vnfID != null) {
				String url = aaiUrl + "/aai/v11/network/generic-vnfs/generic-vnf/";
				response = new AAIManager(new RESTManager()).getQueryByVnfID(url, aaiUsername, aaiPassword, requestID, vnfID);
			} else {
				logger.error("getAAIServiceInstance failed");
			}
		} catch (Exception e) {
			logger.error("getAAIServiceInstance exception: ", e);
		}
		return response;
	}
}
