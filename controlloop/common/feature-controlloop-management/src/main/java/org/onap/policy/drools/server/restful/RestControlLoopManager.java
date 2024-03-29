/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2018-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.drools.server.restful;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.onap.policy.aai.AaiManager;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.drools.apps.controlloop.feature.management.ControlLoopManagementFeature;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.rest.RestManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Telemetry Extensions for Control Loops in the PDP-D.
 */

@Path("/policy/pdp")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RestControlLoopManager implements PolicyApi {
    private static final Logger logger = LoggerFactory.getLogger(RestControlLoopManager.class);

    /**
     * GET control loops.
     *
     * @param controllerName controller name.
     * @param sessionName session name.
     * @return list of controller names.
     */
    @GET
    @Path("engine/controllers/{controller}/drools/facts/{session}/controlloops")
    public Response controlLoops(
        @PathParam("controller") String controllerName,
        @PathParam("session") String sessionName) {

        try {
            List<String> controlLoopNames =
                ControlLoopManagementFeature.controlLoops(controllerName, sessionName)
                    .map(ControlLoopParams::getClosedLoopControlName)
                    .collect(Collectors.toList());

            return Response.status(Response.Status.OK).entity(controlLoopNames).build();
        } catch (IllegalArgumentException e) {
            logger.error("'GET' controlloops threw an exception", e);
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    /**
     * GET control loop.
     *
     * @param controllerName controller name.
     * @param sessionName session name.
     * @param controlLoopName control loop name.
     * @return control loop.
     */
    @GET
    @Path("engine/controllers/{controller}/drools/facts/{session}/controlloops/{controlLoopName}")
    public Response controlLoop(
        @PathParam("controller") String controllerName,
        @PathParam("session") String sessionName,
        @PathParam("controlLoopName") String controlLoopName) {

        try {
            List<ControlLoopParams> controlLoopParams =
                ControlLoopManagementFeature.controlLoops(controllerName, sessionName)
                    .filter(c -> c.getClosedLoopControlName().equals(controlLoopName))
                    .collect(Collectors.toList());

            return Response.status(Response.Status.OK).entity(controlLoopParams).build();
        } catch (IllegalArgumentException e) {
            logger.error("'GET' controlloop threw an exception", e);
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    /**
     * AAI Custom Query.
     *
     * @param vserverId vServer identifier.
     * @return query results.
     */
    @GET
    @Path("engine/tools/controlloops/aai/customQuery/{vserverId}")
    public Response aaiCustomQuery(@PathParam("vserverId") String vserverId) {
        var mgr = PolicyEngineConstants.getManager();

        return Response
            .status(Status.OK)
            .entity(new AaiManager(new RestManager())
                .getCustomQueryResponse(
                    mgr.getEnvironmentProperty("aai.url"),
                    mgr.getEnvironmentProperty("aai.username"),
                    mgr.getEnvironmentProperty("aai.password"),
                    UUID.randomUUID(),
                    vserverId))
            .build();
    }

}
