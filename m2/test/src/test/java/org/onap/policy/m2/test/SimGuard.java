/*-
 * ============LICENSE_START=======================================================
 * m2/test
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

import static org.onap.policy.m2.test.Util.json;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * minimal Guard Simulator -- just enough to support the current tests.
 */
@Path("/")
public class SimGuard {
    /**
     * Process an HTTP POST to /pdp/.
     */
    @POST
    @Path("/pdp/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String query(String data) {
        JSONObject msg = (JSONObject) new JSONTokener(data).nextValue();
        System.out.println("SimGuard query:\n" + msg.toString(4));

        JSONObject response = json("decision", "Permit",
                                   "details", "");
        System.out.println("Returning:\n" + response.toString(4));
        return response.toString();
    }
}
