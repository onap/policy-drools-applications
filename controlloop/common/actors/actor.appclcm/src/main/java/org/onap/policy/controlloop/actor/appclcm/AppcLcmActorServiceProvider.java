/*-
 * ============LICENSE_START=======================================================
 * AppcLcmActorServiceProvider
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

package org.onap.policy.controlloop.actor.appclcm;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.onap.policy.aai.AAINQF199.AAINQF199InventoryResponseItem;
import org.onap.policy.aai.AAINQF199.AAINQF199Manager;
import org.onap.policy.aai.AAINQF199.AAINQF199Request;
import org.onap.policy.aai.AAINQF199.AAINQF199Response;
import org.onap.policy.appclcm.LCMCommonHeader;
import org.onap.policy.appclcm.LCMRequest;
import org.onap.policy.appclcm.LCMRequestWrapper;
import org.onap.policy.appclcm.LCMResponse;
import org.onap.policy.appclcm.LCMResponseCode;
import org.onap.policy.appclcm.LCMResponseWrapper;
import org.onap.policy.controlloop.ControlLoopOperation;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.actorServiceProvider.spi.Actor;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppcLcmActorServiceProvider implements Actor {
    
    private static final Logger logger = LoggerFactory.getLogger(AppcLcmActorServiceProvider.class);
    
    /* The source vnf-id provided from the DCAE onset */
    private static final String DCAE_VNF_ID = "generic-vnf.vnf-id";

    /* To be used in future releases to restart a single vm */
    private static final String APPC_VM_ID = "vm-id";
    
    /* To be used in future releases when LCM ConfigModify is used */
    private static final String APPC_REQUEST_PARAMS = "request-parameters";
    private static final String APPC_CONFIG_PARAMS = "configuration-parameters";

    private static final ImmutableList<String> recipes = ImmutableList.of("Restart", "Rebuild", "Migrate",
            "ConfigModify");
    private static final ImmutableMap<String, List<String>> targets = new ImmutableMap.Builder<String, List<String>>()
            .put("Restart", ImmutableList.of("VM")).put("Rebuild", ImmutableList.of("VM"))
            .put("Migrate", ImmutableList.of("VM")).put("ConfigModify", ImmutableList.of("VNF")).build();
    private static final ImmutableMap<String, List<String>> payloads = new ImmutableMap.Builder<String, List<String>>()
            .put("Restart", ImmutableList.of(APPC_VM_ID))
            .put("ConfigModify", ImmutableList.of(APPC_REQUEST_PARAMS, APPC_CONFIG_PARAMS)).build();

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
     * Constructs an A&AI Named Query using a source vnf-id to determine 
     * the vnf-id of the target entity specified in the policy to act upon.
     * 
     * @param resourceId
     *            the id of the target from the sdc catalog
     *            
     * @param sourceVnfId
     *            the vnf id of the source entity reporting the alert
     *            
     * @return the target entities vnf id to act upon
     */
    public static String vnfNamedQuery(String resourceId, String sourceVnfId) {
        String targetVnfId = "";
        AAINQF199Request aaiRequest = new AAINQF199Request();
        UUID requestId = UUID.randomUUID();
        
        aaiRequest.queryParameters.namedQuery.namedQueryUUID = requestId;
        
        Map<String, Map<String, String>> filter = new HashMap<String, Map<String, String>>();
        
        Map<String, String> filterItem = new HashMap<String, String>();
        filterItem.put("vnf-id", sourceVnfId);
        
        filter.put("generic-vnf", filterItem);
        
        aaiRequest.instanceFilters.instanceFilter.add(filter);
        
        //TODO: What is the url to use?
        AAINQF199Response aaiResponse = AAINQF199Manager.postQuery("http://localhost:6666", "policy", "policy", aaiRequest, requestId);
        
        //TODO: What if the resourceId never matches?
        for (AAINQF199InventoryResponseItem item: aaiResponse.inventoryResponseItems) {
            if (item.genericVNF.modelInvariantId.equals(resourceId)) {
                targetVnfId = item.genericVNF.vnfID;
            }
        }
        
        return targetVnfId;
    }
    
    /**
     * Constructs an APPC request conforming to the lcm API.
     * The actual request is constructed and then placed in a 
     * wrapper object used to send through DMAAP.
     * 
     * @param onset
     *            the event that is reporting the alert for policy
     *            to perform an action        
     * @param operation
     *            the control loop operation specifying the actor,
     *            operation, target, etc.  
     * @param policy
     *            the policy the was specified from the yaml generated
     *            by CLAMP or through the Policy GUI/API                        
     * @return an APPC request conforming to the lcm API using the DMAAP wrapper
     */
    public static LCMRequestWrapper constructRequest(VirtualControlLoopEvent onset, ControlLoopOperation operation,
            Policy policy) {
        
        /* Construct an APPC request using LCM Model */
        
        /*
         * The actual LCM request is placed in a wrapper used to send
         * through dmaap. The current version is 2.0 as of R1.
         */
        LCMRequestWrapper dmaapRequest = new LCMRequestWrapper();
        dmaapRequest.setVersion("2.0");
        dmaapRequest.setCorrelationId(onset.requestID + "-" + operation.subRequestId);
        dmaapRequest.setRpcName(policy.getRecipe().toLowerCase());
        dmaapRequest.setType("request");
        
        /* This is the actual request that is placed in the dmaap wrapper. */
        LCMRequest appcRequest = new LCMRequest();
        
        /* The common header is a required field for all APPC requests. */
        LCMCommonHeader requestCommonHeader = new LCMCommonHeader();
        requestCommonHeader.setOriginatorId(onset.requestID.toString());
        requestCommonHeader.setRequestId(onset.requestID);
        requestCommonHeader.setSubRequestId(operation.subRequestId);
        
        appcRequest.setCommonHeader(requestCommonHeader);

        /* 
         * Action Identifiers are required for APPC LCM requests.
         * For R1, the recipes supported by Policy only require
         * a vnf-id.
         */
        HashMap<String, String> requestActionIdentifiers = new HashMap<>();
        requestActionIdentifiers.put("vnf-id", onset.AAI.get(DCAE_VNF_ID));
        appcRequest.setActionIdentifiers(requestActionIdentifiers);
        
        /* 
         * An action is required for all APPC requests, this will 
         * be the recipe specified in the policy.
         */
        appcRequest.setAction(policy.getRecipe().toLowerCase());

        /*
         * For R1, the payloads will not be required for the Restart, 
         * Rebuild, or Migrate recipes. APPC will populate the payload
         * based on A&AI look up of the vnd-id provided in the action
         * identifiers.
         */
        if ("Restart".equalsIgnoreCase(policy.getRecipe()) || "Rebuild".equalsIgnoreCase(policy.getRecipe())
                || "Migrate".equalsIgnoreCase(policy.getRecipe())) {
            appcRequest.setPayload(null);
        }
        
        /* 
         * Once the LCM request is constructed, add it into the 
         * body of the dmaap wrapper.
         */
        dmaapRequest.setBody(appcRequest);
        
        /* Return the request to be sent through dmaap. */
        return dmaapRequest;
    }
    
    /**
     * Parses the operation attempt using the subRequestId
     * of APPC response.
     * 
     * @param subRequestId
     *            the sub id used to send to APPC, Policy sets
     *            this using the operation attempt
     *            
     * @return the current operation attempt
     */
    public static Integer parseOperationAttempt(String subRequestId) {
        Integer operationAttempt;
        try {
            operationAttempt = Integer.parseInt(subRequestId);
        } catch (NumberFormatException e) {
            logger.debug("A NumberFormatException was thrown due to error in parsing the operation attempt");
            return null;
        }
        return operationAttempt;
    }
    
    /**
     * Processes the APPC LCM response sent from APPC. Determines
     * if the APPC operation was successful/unsuccessful and maps
     * this to the corresponding Policy result.
     * 
     * @param dmaapResponse
     *            the dmaap wrapper message that contains the
     *            actual APPC reponse inside the body field
     *                       
     * @return an key-value pair that contains the Policy result
     * and APPC response message
     */
    public static SimpleEntry<PolicyResult, String> processResponse(LCMResponseWrapper dmaapResponse) {
        /* The actual APPC response is inside the wrapper's body field. */
        LCMResponse appcResponse = dmaapResponse.getBody();
        
        /* The message returned in the APPC response. */
        String message;
        
        /* The Policy result determined from the APPC Response. */
        PolicyResult result;
        
        /* If there is no status, Policy cannot determine if the request was successful. */
        if (appcResponse.getStatus() == null) {
            message = "Policy was unable to parse APP-C response status field (it was null).";
            return new AbstractMap.SimpleEntry<>(PolicyResult.FAILURE_EXCEPTION, message);
        }
        
        /* If there is no code, Policy cannot determine if the request was successful. */
        String responseValue = LCMResponseCode.toResponseValue(appcResponse.getStatus().getCode());
        if (responseValue == null) {
            message = "Policy was unable to parse APP-C response status code field.";
            return new AbstractMap.SimpleEntry<>(PolicyResult.FAILURE_EXCEPTION, message);
        }
        
        /* Save the APPC response's message for Policy noticiation message. */
        message = appcResponse.getStatus().getMessage();
        
        /* Maps the APPC response result to a Policy result. */
        switch (responseValue) {
            case LCMResponseCode.ACCEPTED:
                /* Nothing to do if code is accept, continue processing */
                result = null;
                break;
            case LCMResponseCode.SUCCESS:
                result = PolicyResult.SUCCESS;
                break;
            case LCMResponseCode.FAILURE:
                result = PolicyResult.FAILURE;
                break;
            case LCMResponseCode.REJECT:
            case LCMResponseCode.ERROR:
            default:
                result = PolicyResult.FAILURE_EXCEPTION;
        }
        return new AbstractMap.SimpleEntry<>(result, message);
    }

}
