/*-
 * ============LICENSE_START=======================================================
 * m2/test
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

package org.onap.policy.m2.test;

import com.google.gson.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * minimal Guard Simulator -- just enough to support the current tests.
 */
@Path("/")
public class SimGuard {
    private static Logger logger = LoggerFactory.getLogger(SimGuard.class);

    // used for JSON <-> String conversion
    private static StandardCoder coder = new StandardCoder();

    /**
     * Process an HTTP POST to /pdp/.
     */
    @POST
    @Path("/pdp/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String query(String data) throws CoderException {

        JsonObject msg = coder.decode(data, JsonObject.class);
        logger.info("SimGuard query:\n{}", Util.prettyPrint(msg));

        JsonObject response = new JsonObject();
        response.addProperty("status", "Permit");
        logger.info("Returning:\n{}", Util.prettyPrint(response));

        return response.toString();
    }
}
