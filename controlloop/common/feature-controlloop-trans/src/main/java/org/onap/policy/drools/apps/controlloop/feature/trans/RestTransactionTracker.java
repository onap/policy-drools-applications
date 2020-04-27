/*
 * ============LICENSE_START=======================================================
 * ONAP
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

package org.onap.policy.drools.apps.controlloop.feature.trans;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.onap.policy.common.endpoints.http.server.YamlMessageBodyHandler;
import org.onap.policy.controlloop.VirtualControlLoopNotification;

/**
 * REST Transaction Tracker.
 */

@Path("/policy/pdp/engine/controllers")
@Produces({MediaType.APPLICATION_JSON, YamlMessageBodyHandler.APPLICATION_YAML})
@Consumes({MediaType.APPLICATION_JSON, YamlMessageBodyHandler.APPLICATION_YAML})
@Api
public class RestTransactionTracker {

    /**
     * GET transactions.
     */

    @GET
    @Path("transactions")
    @ApiOperation(value = "Retrieve in-progress transactions", responseContainer = "List")
    public Response transactions() {
        return Response.status(Response.Status.OK)
                       .entity(ControlLoopMetricsManager.getManager().getTransactions()).build();
    }

    /**
     * GET one transaction.
     */

    @GET
    @Path("transactions/{transactionId}")
    @ApiOperation(value = "Retrieve an in-progress transaction", response = VirtualControlLoopNotification.class)
    public Response transactionId(
          @ApiParam(value = "UUID", required = true) @PathParam("transactionId") String transactionId) {
        VirtualControlLoopNotification notification =
                ControlLoopMetricsManager.getManager().getTransaction(UUID.fromString(transactionId));
        return Response.status((notification != null) ? Response.Status.OK : Response.Status.NOT_FOUND)
                       .entity(notification).build();
    }

    /**
     * Resets the cache with a new cache size.
     */

    @PUT
    @Path("transactions/cacheSize/{cacheSize}")
    @ApiOperation(value = "Sets the cache size", response = Integer.class)
    public Response cacheSize(
            @ApiParam(value = "cache size", required = true) @PathParam("cacheSize") int cacheSize) {
        ControlLoopMetricsManager.getManager().resetCache(cacheSize,
                ControlLoopMetricsManager.getManager().getTransactionTimeout());
        return Response.status(Response.Status.OK)
                       .entity(ControlLoopMetricsManager.getManager().getCacheSize()).build();
    }

    /**
     * GET the cache size.
     */

    @GET
    @Path("transactions/cacheSize")
    @ApiOperation(value = "Gets the cache size", response = Integer.class)
    public Response cacheSize() {
        return Response.status(Response.Status.OK)
                       .entity(ControlLoopMetricsManager.getManager().getCacheSize()).build();
    }

    /**
     * Resets the cache with a new transaction timeout in seconds.
     */

    @PUT
    @Path("transactions/timeout/{timeoutSecs}")
    @ApiOperation(value = "Sets the timeout in seconds", response = Integer.class)
    public Response timeout(
            @ApiParam(value = "timeout", required = true) @PathParam("timeoutSecs") long timeoutSecs) {
        ControlLoopMetricsManager.getManager().resetCache(
                ControlLoopMetricsManager.getManager().getCacheSize(), timeoutSecs);
        return Response.status(Response.Status.OK)
                       .entity(ControlLoopMetricsManager.getManager().getTransactionTimeout()).build();
    }

    /**
     * GET the cache timeout.
     */

    @GET
    @Path("transactions/timeout")
    @ApiOperation(value = "Gets the cache timeout", response = Long.class)
    public Response timeout() {
        return Response.status(Response.Status.OK)
                       .entity(ControlLoopMetricsManager.getManager().getTransactionTimeout()).build();
    }
}
