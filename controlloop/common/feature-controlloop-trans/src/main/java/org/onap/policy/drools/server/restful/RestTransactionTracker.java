/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.onap.policy.common.endpoints.http.server.YamlMessageBodyHandler;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.drools.apps.controlloop.feature.trans.ControlLoopMetricsManager;

/**
 * REST Transaction Tracker.
 */

@Path("/policy/pdp/engine/controllers/transactions")
@Produces({MediaType.APPLICATION_JSON, YamlMessageBodyHandler.APPLICATION_YAML})
@Consumes({MediaType.APPLICATION_JSON, YamlMessageBodyHandler.APPLICATION_YAML})
public class RestTransactionTracker implements PolicyApi {

    /**
     * GET transactions.
     */

    @Override
    @GET
    @Path("inprogress")
    public Response transactions() {
        return Response.status(Response.Status.OK)
                       .entity(ControlLoopMetricsManager.getManager().getTransactions()).build();
    }

    /**
     * GET one transaction.
     */

    @Override
    @GET
    @Path("inprogress/{transactionId}")
    public Response transactionId(@PathParam("transactionId") String transactionId) {
        VirtualControlLoopNotification notification =
                ControlLoopMetricsManager.getManager().getTransaction(UUID.fromString(transactionId));
        return Response.status((notification != null) ? Response.Status.OK : Response.Status.NOT_FOUND)
                       .entity(notification).build();
    }

    /**
     * Resets the cache with a new cache size.
     */

    @Override
    @PUT
    @Path("cacheSize/{cacheSize}")
    public Response cacheSize1(@PathParam("cacheSize") int cacheSize) {
        ControlLoopMetricsManager.getManager().resetCache(cacheSize,
                ControlLoopMetricsManager.getManager().getTransactionTimeout());
        return Response.status(Response.Status.OK)
                       .entity(ControlLoopMetricsManager.getManager().getCacheSize()).build();
    }

    /**
     * GET the cache size.
     */

    @Override
    @GET
    @Path("cacheSize")
    public Response cacheSize() {
        return Response.status(Response.Status.OK)
                       .entity(ControlLoopMetricsManager.getManager().getCacheSize()).build();
    }

    /**
     * Resets the cache with a new transaction timeout in seconds.
     */
    @Override
    @PUT
    @Path("timeout/{timeoutSecs}")
    public Response timeout(@PathParam("timeoutSecs") long timeoutSecs) {
        ControlLoopMetricsManager.getManager().resetCache(
                ControlLoopMetricsManager.getManager().getCacheSize(), timeoutSecs);
        return Response.status(Response.Status.OK)
                       .entity(ControlLoopMetricsManager.getManager().getTransactionTimeout()).build();
    }

    /**
     * GET the cache timeout.
     */
    @Override
    @GET
    @Path("timeout")
    public Response timeout1() {
        return Response.status(Response.Status.OK)
                       .entity(ControlLoopMetricsManager.getManager().getTransactionTimeout()).build();
    }

}
