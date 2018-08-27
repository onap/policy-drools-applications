/*-
 * ============LICENSE_START=======================================================
 * simulators
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.simulators;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.onap.policy.so.SORequest;
import org.onap.policy.so.SORequestReferences;
import org.onap.policy.so.SORequestStatus;
import org.onap.policy.so.SOResponse;

import com.att.aft.dme2.internal.gson.Gson;

@Path("/serviceInstantiation")
public class SoSimulatorJaxRs {
    
	/**
     * SO post query.
     * 
     * @param serviceInstanceId the service instance Id
     * @param vnfInstanceId the VNF Id
     * @return the response
     */
    @POST
    @Path("/v7/{serviceInstanceId}/vnfs/{vnfInstanceId}/vfModules/scaleOut")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/json")
    public String soPostQuery(@PathParam("serviceInstanceId") final String serviceInstanceId, @PathParam("vnfInstanceId") final String vnfInstanceId)
    {
        final SORequest request = new SORequest();
        final SORequestStatus requestStatus = new SORequestStatus();
        requestStatus.setRequestState("COMPLETE");
        request.setRequestStatus(requestStatus);
        request.setRequestId(UUID.randomUUID());
        
        final SOResponse response = new SOResponse();
        
        final SORequestReferences requestReferences = new SORequestReferences();
        final String requestId = UUID.randomUUID().toString();
        requestReferences.setRequestId(requestId);
        response.setRequestReferences(requestReferences);
        
        response.setRequest(request);

        return new Gson().toJson(response);
    }
}
