/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2018-2020 AT&T Intellectual Property. All rights reserved.
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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.onap.policy.aai.AaiManager;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.drools.apps.controlloop.feature.management.ControlLoopManagementFeature;
import org.onap.policy.drools.system.PolicyEngine;
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
@Api
public class RestControlLoopManager {
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
    @ApiOperation(value = "Control Loops", notes = "Compact list", responseContainer = "List")
    @ApiResponses(value = {@ApiResponse(code = 404, message = "Control Loops cannot be found")})
    public Response controlLoops(
        @ApiParam(value = "Policy Controller Name", required = true) @PathParam("controller") String controllerName,
        @ApiParam(value = "Drools Session Name", required = true) @PathParam("session") String sessionName) {

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
    @ApiOperation( value = "Control Loop", notes = "Control Loop Parameters", responseContainer = "List")
    @ApiResponses(value = {@ApiResponse(code = 404, message = "The Control Loop cannot be found")})
    public Response controlLoop(
        @ApiParam(value = "Policy Controller Name", required = true) @PathParam("controller") String controllerName,
        @ApiParam(value = "Drools Session Name", required = true) @PathParam("session") String sessionName,
        @ApiParam(value = "Control Loop Name", required = true) @PathParam("controlLoopName") String controlLoopName) {

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
    @ApiOperation(value = "AAI Custom Query")
    public Response aaiCustomQuery(@ApiParam(value = "vserver Identifier") String vserverId) {
        PolicyEngine mgr = PolicyEngineConstants.getManager();

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
