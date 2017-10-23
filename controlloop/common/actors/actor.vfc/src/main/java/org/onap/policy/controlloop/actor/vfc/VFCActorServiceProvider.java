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
import org.onap.policy.controlloop.actorServiceProvider.spi.Actor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.onap.policy.aai.AAIManager;
import org.onap.policy.aai.AAIGETVnfResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VFCActorServiceProvider implements Actor {

    private static final Logger logger = LoggerFactory.getLogger(VFCActorServiceProvider.class);
    private static final ImmutableList<String> recipes = ImmutableList.of("Restart");
    private static final ImmutableMap<String, List<String>> targets = new ImmutableMap.Builder<String, List<String>>()
            .put("Restart", ImmutableList.of("VM")).build();

    @Override
    public String actor() {
        return "VFC";
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
        String serviceInstance = onset.AAI.get("service-instance.service-instance-id");
        if (serviceInstance == null || "".equals(serviceInstance))
        {
        	AAIGETVnfResponse tempVnfResp = vnfResponse;
        	if(tempVnfResp == null) //if the response is null, we haven't queried
        	{
        		tempVnfResp = getAAIServiceInstance(onset); //This does the AAI query since we haven't already
        		if (tempVnfResp == null)
        		    return null;
        	}
        	serviceInstance = tempVnfResp.serviceId;
        }
        request.nsInstanceId = serviceInstance;
        request.requestId = onset.requestID;
        request.healRequest = new VFCHealRequest();
        request.healRequest.vnfInstanceId = onset.AAI.get("generic-vnf.vnf-id");
        request.healRequest.cause = operation.message;
        request.healRequest.additionalParams = new VFCHealAdditionalParams();
        
        switch (policy.getRecipe().toLowerCase()) {
            case "restart":
                request.healRequest.additionalParams.action = "restartvm";
                request.healRequest.additionalParams.actionInfo = new VFCHealActionVmInfo();
                request.healRequest.additionalParams.actionInfo.vmid = onset.AAI.get("vserver.vserver-id");
                request.healRequest.additionalParams.actionInfo.vmname = onset.AAI.get("vserver.vserver-name");
                break;
            default:
                return null;
        }
        return request;
    }


    private static AAIGETVnfResponse getAAIServiceInstance(VirtualControlLoopEvent event) {
        AAIGETVnfResponse response = null;
        UUID requestID = event.requestID;
        String vnfName = event.AAI.get("generic-vnf.vnf-name");
        String vnfID = event.AAI.get("generic-vnf.vnf-id");
        String aaiUrl = PolicyEngine.manager.getEnvironmentProperty("aai.url");
        String aaiUsername = PolicyEngine.manager.getEnvironmentProperty("aai.username");
        String aaiPassword = PolicyEngine.manager.getEnvironmentProperty("aai.password");
        try {
            if (vnfName != null) {
                String url = aaiUrl + "/aai/v11/network/generic-vnfs/generic-vnf?vnf-name=";
                response = AAIManager.getQueryByVnfName(url, aaiUsername, aaiPassword, requestID, vnfName);
            } else if (vnfID != null) {
                String url = aaiUrl + "/aai/v11/network/generic-vnfs/generic-vnf/";
                response = AAIManager.getQueryByVnfID(url, aaiUsername, aaiPassword, requestID, vnfID);
            } else {
                logger.error("getAAIServiceInstance failed");
            }
        } catch (Exception e) {
            logger.error("getAAIServiceInstance exception: ", e);
        }
        return response;
    }
}
