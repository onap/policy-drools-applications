/*-
 * ============LICENSE_START=======================================================
 * simulators
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

package org.onap.policy.simulators;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.onap.policy.so.SORequest;
import org.onap.policy.so.util.Serialization;

@Path("/serviceInstances")
public class SoSimulatorJaxRs {
	
	@POST
	@Path("/v5/{serviceInstanceId}/vnfs/{vnfInstanceId}/vfModules")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces("application/json")
	public String SoPostQuery(@PathParam("serviceInstanceId") String serviceInstanceId, @PathParam("vnfInstanceId") String vnfInstanceId, String req)
	{
		SORequest request = Serialization.gsonPretty.fromJson(req, SORequest.class);
		return "{\"requestReferences\": {\"instanceId\": \"ff305d54-75b4-ff1b-bdb2-eb6b9e5460ff\", \"requestId\": " + request.requestId + "}}";
	}
	
}
