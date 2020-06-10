/*-
 * ============LICENSE_START=======================================================
 * m2/appclcm
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.m2.appclcm;

import java.util.HashMap;
import java.util.Map;

import org.onap.policy.appclcm.AppcLcmDmaapWrapper;
import org.onap.policy.appclcm.AppcLcmOutput;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.controlloop.ControlLoopEvent;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.guard.PolicyGuardResponse;
import org.onap.policy.m2.appclcm.model.AppcLcmResponseCode;
import org.onap.policy.m2.base.Transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppcLcmHealthCheckOperation extends AppcLcmOperation {
    public static final String DCAE_IPV4_ADDR =
        "vserver.l-interface.l3-interface-ipv4-address-list.l3-inteface-ipv4-address";

    private static Logger logger = LoggerFactory.getLogger(AppcLcmHealthCheckOperation.class);

    private static final long serialVersionUID = 4969322301462776173L;

    public AppcLcmHealthCheckOperation(Transaction transaction, Policy policy,
                                   ControlLoopEvent onset, int attempt) {
        super(transaction, policy, onset, attempt);
    }

    /**
     * This method will attempt to deserialize the json payload from appc and
     * then parse the response to determine if the vnf is healthy or unhealthy.
     * The "state" field in the payload will contain "healthy" or "unhealthy"
     * based on the condition of the vnf.
     *
     * @param jsonPayload
     *            the appc lcm response json payload
     * @return the string that contains the state of the vnf
     */
    private String getVnfHealthState(String jsonPayload) {
        HashMap<String, Object> healthCheckPayloadMap;
        try {
            healthCheckPayloadMap = coder.decode(jsonPayload, HashMap.class);
        } catch (CoderException e) {
            return null;
        }

        String stateOfHealth = null;
        if (healthCheckPayloadMap.containsKey("state")) {
            stateOfHealth = healthCheckPayloadMap.get("state").toString();
        } else {
            return null;
        }
        return stateOfHealth;
    }

    /**
     * An incoming message is being delivered to the operation.
     *
     * {@inheritDoc}
     */
    @Override
    public void incomingMessage(Object object) {
        if (!(object instanceof AppcLcmDmaapWrapper)) {
            // ignore this message (not sure why we even got it)
            return;
        }

        // If we reach this point, we have a 'AppcLcmDmaapWrapper' instance.
        // The rest of this method is mostly copied from
        // 'ControlLoopOperationManager.onResponse'.

        AppcLcmOutput response = ((AppcLcmDmaapWrapper)object).getBody().getOutput();

        //
        // Determine which subrequestID (ie. attempt)
        //
        int operationAttempt;
        try {
            operationAttempt = Integer
                               .parseInt(response.getCommonHeader().getSubRequestId());
        } catch (NumberFormatException e) {
            //
            // We cannot tell what happened if this doesn't exist
            //
            this.completeOperation(
                this.getAttempt(),
                "Policy was unable to parse APP-C SubRequestID (it was null).",
                PolicyResult.FAILURE_EXCEPTION);
            return;
        }
        //
        // Sanity check the response message
        //
        if (response.getStatus() == null) {
            //
            // We cannot tell what happened if this doesn't exist
            //
            this.completeOperation(
                operationAttempt,
                "Policy was unable to parse APP-C response status field (it was null).",
                PolicyResult.FAILURE_EXCEPTION);
            return;
        }
        //
        // Get the Response Code
        //
        AppcLcmResponseCode responseValue = AppcLcmResponseCode
                                        .toResponseValue(response.getStatus().getCode());
        if (responseValue == null) {
            //
            // We are unaware of this code
            //
            this.completeOperation(
                operationAttempt,
                "Policy was unable to parse APP-C response status code field.",
                PolicyResult.FAILURE_EXCEPTION);
            return;
        }
        //
        // Ok, let's figure out what APP-C's response is
        //
        switch (responseValue) {
            case ACCEPTED:
                //
                // This is good, they got our original message and
                // acknowledged it.
                //
                // Is there any need to track this?
                //
                return;
            case ERROR:
            case REJECT:
                //
                // We'll consider these two codes as exceptions
                //
                this.completeOperation(operationAttempt,
                                       response.getStatus().getMessage(),
                                       PolicyResult.FAILURE_EXCEPTION);
                return;
            case FAILURE:
                //
                // APPC could not do a healthcheck
                //
                this.completeOperation(operationAttempt,
                                       response.getStatus().getMessage(),
                                       PolicyResult.FAILURE);
                return;
            case SUCCESS:
                //
                // This means APPC was able to execute the health check.
                // The payload has to be parsed to see if the VNF is
                // healthy or unhealthy
                //

                //
                // sanity check the payload
                //
                if (response.getPayload() == null || response.getPayload().isEmpty()) {
                    //
                    // We are cannot parse the payload
                    //
                    this.completeOperation(
                        operationAttempt,
                        "Policy was unable to parse APP-C response payload because it was null.",
                        PolicyResult.FAILURE_EXCEPTION);
                    return;
                }

                //
                // parse the payload to see if the VNF is healthy/unhealthy
                //
                String vnfHealthState = getVnfHealthState(response.getPayload());
                if ("healthy".equalsIgnoreCase(vnfHealthState)) {
                    this.completeOperation(operationAttempt, "VNF is healthy",
                                           PolicyResult.SUCCESS);
                } else if ("unhealthy".equalsIgnoreCase(vnfHealthState)) {
                    this.completeOperation(operationAttempt, "VNF is unhealthy",
                                           PolicyResult.FAILURE);
                } else {
                    this.completeOperation(
                        operationAttempt,
                        "Error: Could not determine the state of the VNF."
                        + " The state field in the APPC response payload was unrecognized or null.",
                        PolicyResult.FAILURE_EXCEPTION);
                }
                return;
            default:
                return;
        }
    }

    /**
       * This method will construct a payload for a health check.
       * The payload must be an escaped json string so gson is used
       * to convert the payload hashmap into json
       *
       * @return an escaped json string representation of the payload
       * @throws ControlLoopException if it occurs
       */
    @Override
    protected String setPayload(Map<String, String> aai, String recipe) throws ControlLoopException {
        Map<String, String> payload = new HashMap<>();

        // Extract oam ip address from the onset
        String ipAddr = aai.get(DCAE_IPV4_ADDR);
        if (ipAddr != null) {
            payload.put("host-ip-address", ipAddr);
        } else {
            logger.error("Error - IPv4 Address not found in the onset");
            setErrorStatus("Error - IPv4 Address not found in the onset");
        }

        try {
            return coder.encode(payload);
        } catch (CoderException e) {
            return null;
        }
    }
}
