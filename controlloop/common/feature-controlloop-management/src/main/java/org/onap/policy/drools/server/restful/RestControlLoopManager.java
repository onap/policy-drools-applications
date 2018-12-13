/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights reserved.
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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.params.ControlLoopParams;
import org.onap.policy.controlloop.processor.ControlLoopProcessor;
import org.onap.policy.drools.apps.controlloop.feature.management.ControlLoopManagementFeature;
import org.onap.policy.drools.controller.DroolsController;
import org.onap.policy.drools.system.PolicyController;

/**
 * Telemetry Extensions for Control Loops in the PDP-D.
 */

@Path("/policy/pdp")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api
public class RestControlLoopManager {

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
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    /**
     * GET operational policy.
     *
     * @param controllerName controller name.
     * @param sessionName session name.
     * @param controlLoopName control loop name.
     * @return operational policy.
     */
    @GET
    @Path("engine/controllers/{controller}/drools/facts/{session}/controlloops/{controlLoopName}/policy")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation( value = "Operational Policy", notes = "The policy is in yaml format")
    @ApiResponses(value = {@ApiResponse(code = 404, message = "The Control Loop cannot be found"),
        @ApiResponse(code = 500, message = "The Control Loop has invalid data")})
    public Response policy(
        @ApiParam(value = "Policy Controller Name", required = true) @PathParam("controller") String controllerName,
        @ApiParam(value = "Drools Session Name", required = true) @PathParam("session") String sessionName,
        @ApiParam(value = "Control Loop Name", required = true) @PathParam("controlLoopName") String controlLoopName) {

        try {
            ControlLoopParams controlLoopParams =
                ControlLoopManagementFeature.controlLoops(controllerName, sessionName)
                    .filter(c -> c.getClosedLoopControlName().equals(controlLoopName))
                    .findFirst()
                    .orElse(null);

            if (controlLoopParams == null || controlLoopParams.getControlLoopYaml() == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Policy not found").build();
            }

            return Response.status(Status.OK)
                .entity(URLDecoder.decode(controlLoopParams.getControlLoopYaml(), "UTF-8")).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (UnsupportedEncodingException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unreadable Policy").build();
        }
    }

    /**
     * PUT an Operational Policy.
     *
     * @param controllerName controller name.
     * @param sessionName session name.
     * @param controlLoopName control loop name.
     * @param policy operational policy.
     *
     * @return operational policy.
     */

    @PUT
    @Path("engine/controllers/{controller}/drools/facts/{session}/controlloops/{controlLoopName}/policy")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation( value = "Add Operational Policy", notes = "The Operational Policy should be syntactically correct")
    @ApiResponses(value = {@ApiResponse(code = 404, message = "The Control Loop cannot be found"),
        @ApiResponse(code = 409, message = "The Control Loop exists"),
        @ApiResponse(code = 412, message = "The Control Loop Name must be matched in the URL"),
        @ApiResponse(code = 406, message = "The Operational Policy is invalid")})
    public Response opOffer(
        @ApiParam(value = "Policy Controller Name", required = true) @PathParam("controller") String controllerName,
        @ApiParam(value = "Drools Session Name", required = true) @PathParam("session") String sessionName,
        @ApiParam(value = "Control Loop Name", required = true) @PathParam("controlLoopName") String controlLoopName,
        @ApiParam(value = "Operational Policy", required = true) String policy) {

        try {
            ControlLoopParams controlLoop =
                ControlLoopManagementFeature.controlLoop(controllerName, sessionName, controlLoopName);

            if (controlLoop != null) {
                return Response.status(Status.CONFLICT).entity(controlLoop).build();
            }

            /* validation */

            ControlLoopProcessor controlLoopProcessor = new ControlLoopProcessor(policy);

            if (!controlLoopName.equals(controlLoopProcessor.getControlLoop().getControlLoopName())) {
                return Response.status(Status.PRECONDITION_FAILED)
                    .entity("Control Loop Name in URL does not match the Operational Policy")
                    .build();
            }

            DroolsController controller = PolicyController.factory.get(controllerName).getDrools();

            controlLoop = new ControlLoopParams();
            controlLoop.setPolicyScope(controller.getGroupId());
            controlLoop.setPolicyName(controller.getArtifactId());
            controlLoop.setPolicyVersion(controller.getVersion());
            controlLoop.setClosedLoopControlName(controlLoopName);
            controlLoop.setControlLoopYaml(URLEncoder.encode(policy, "UTF-8"));

            controller.getContainer().insertAll(controlLoop);
            return Response.status(Status.OK).entity(controlLoop).build();

        } catch (IllegalArgumentException i) {
            return Response.status(Response.Status.NOT_FOUND).entity(i).build();
        } catch (ControlLoopException | UnsupportedEncodingException e) {
            return Response.status(Status.NOT_ACCEPTABLE).entity(e).build();
        }
    }
}
