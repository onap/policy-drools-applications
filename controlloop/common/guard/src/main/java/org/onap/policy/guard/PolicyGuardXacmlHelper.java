/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2017-2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019 Samsung Electronics Co., Ltd.
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

package org.onap.policy.guard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.utils.NetLoggerUtil;
import org.onap.policy.common.endpoints.utils.NetLoggerUtil.EventType;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.rest.RestManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PolicyGuardXacmlHelper {
    private static final Logger logger = LoggerFactory.getLogger(PolicyGuardXacmlHelper.class);

    private String url;
    private String user;
    private String pwd;

    /**
     *  Constructor.
     */
    public PolicyGuardXacmlHelper() {
        this.url = PolicyEngineConstants.getManager().getEnvironmentProperty("guard.url");
        this.user = PolicyEngineConstants.getManager().getEnvironmentProperty("pdpx.username");
        this.pwd = PolicyEngineConstants.getManager().getEnvironmentProperty("pdpx.password");
    }

    /**
     * Call PDP.
     *
     * @param xacmlReq the XACML request
     * @return the response
     */
    public String callPdp(PolicyGuardXacmlRequestAttributes xacmlReq) {
        //
        // Create a request suitable for API
        //
        DecisionRequest decisionRequest = new DecisionRequest();
        decisionRequest.setOnapName("Policy");
        decisionRequest.setOnapComponent("Drools PDP");
        decisionRequest.setOnapInstance("usecase template");
        decisionRequest.setRequestId(UUID.randomUUID().toString());
        decisionRequest.setAction("guard");
        Map<String, String> guard = new HashMap<>();
        guard.put("actor", xacmlReq.getActorId());
        guard.put("operation", xacmlReq.getOperationId());
        guard.put("target", xacmlReq.getTargetId());
        if (xacmlReq.getClnameId() != null) {
            guard.put("clname", xacmlReq.getClnameId());
        }
        if (xacmlReq.getVfCount() != null) {
            guard.put("vfCount", Integer.toString(xacmlReq.getVfCount()));
        }
        Map<String, Object> resources = new HashMap<>();
        resources.put("guard", guard);
        decisionRequest.setResource(resources);

        try {
            //
            // Call RESTful PDP
            //
            NetLoggerUtil.log(EventType.OUT, CommInfrastructure.REST, this.url, decisionRequest.toString());
            String response = callRestfulPdp(decisionRequest);
            NetLoggerUtil.log(EventType.IN, CommInfrastructure.REST, this.url, response);

            return response;
        } catch (Exception e) {
            logger.error("Exception in sending RESTful request: ", e);
        }

        return Util.DENY;
    }

    /**
     * This makes an HTTP POST call to a running PDP RESTful servlet to get a decision.
     *
     * @param decisionRequest The Decision request
     * @return response from guard which contains "Permit" or "Deny"
     * @throws CoderException Exception when converting to/from JSON the message body
     */
    private String callRestfulPdp(DecisionRequest decisionRequest) throws CoderException {
        StandardCoder coder = new StandardCoder();

        String jsonBody = coder.encode(decisionRequest);
        RestManager restManager = new RestManager();

        Map<String, String> headers = new HashMap<>();
        headers.put("Accepts", "application/json");

        logger.info("Guard Decision Request: {}", jsonBody);

        Pair<Integer, String> httpDetails = restManager.post(url, user, pwd, headers, "application/json", jsonBody);

        if (httpDetails == null) {
            logger.error("Guard rest call returned a null pair - defaulting to DENY");
            return Util.DENY;
        }

        logger.info("Guard Decision REST Response {} {}", httpDetails.getLeft(), httpDetails.getRight());

        if (httpDetails.getLeft() == 200) {
            DecisionResponse decision = coder.decode(httpDetails.getRight(), DecisionResponse.class);
            logger.info("Guard Decision {}", decision);
            return decision.getStatus();
        }

        return Util.DENY;
    }

}
