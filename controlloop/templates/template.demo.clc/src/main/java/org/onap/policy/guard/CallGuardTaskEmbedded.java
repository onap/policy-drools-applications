/*-
 * ============LICENSE_START=======================================================
 * guard
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

package org.onap.policy.guard;

import com.att.research.xacml.api.DataTypeException;
import com.att.research.xacml.std.annotations.RequestParser;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.drools.core.WorkingMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallGuardTaskEmbedded implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(CallGuardTaskEmbedded.class);

    /**
     * Actor/recipe pairs whose guard requests need a VF Module count. Each element is of
     * the form "&ltactor/&gt:&ltrecipe/&gt".
     */
    private static final Set<String> NEEDS_VF_COUNT = new HashSet<>();

    /**
     * Actor/recipe pairs whose guard requests need the VF Module count to be incremented
     * (i.e., because a module is being added). Each element is of the form
     * "&ltactor/&gt:/&ltrecipe/&gt".
     */
    private static final Set<String> INCR_VF_COUNT = new HashSet<>();

    static {
        INCR_VF_COUNT.add("SO:VF Module Create");
        NEEDS_VF_COUNT.addAll(INCR_VF_COUNT);
    }

    private WorkingMemory workingMemory;
    private String clname;
    private String actor;
    private String recipe;
    private String target;
    private String requestId;
    private Integer vfCount;

    /**
     * Populated once the response has been determined, which may happen during the
     * constructor or later, during {@link #run()}.
     */
    private PolicyGuardResponse guardResponse;

    /**
     * Guard url is grabbed from PolicyEngine.manager properties
     */
    public CallGuardTaskEmbedded(WorkingMemory wm, String cl, String act, String rec, 
            String tar, String reqId, Supplier<Integer> vfcnt) {
        workingMemory = wm;
        clname = cl;
        actor = act;
        recipe = rec;
        requestId = reqId;
        target = tar;

        vfCount = null;

        String key = act + ":" + rec;

        if (NEEDS_VF_COUNT.contains(key)) {
            // this actor/recipe needs the count - get it
            if ((vfCount = vfcnt.get()) == null) {
                /*
                 * The count is missing - create an artificial Deny, which will be
                 * inserted into working memory when "run()" is called.
                 */
                guardResponse = new PolicyGuardResponse(Util.DENY, UUID.fromString(requestId), recipe);
                logger.error("CallEmbeddedGuardTask.run missing VF Module count; requestId={}", requestId);
                return;
            }

            if (INCR_VF_COUNT.contains(key)) {
                // this actor/recipe needs the count to be incremented
                ++vfCount;
            }
        }
    }

    @Override
    public void run() {
        if (guardResponse != null) {
            // already have a response - just insert it
            workingMemory.insert(guardResponse);
            return;
        }
        
        final long startTime = System.nanoTime();
        com.att.research.xacml.api.Request request = null;

        PolicyGuardXacmlRequestAttributes xacmlReq =
                        new PolicyGuardXacmlRequestAttributes(clname, actor, recipe, target, requestId, vfCount);

        try {
            request = RequestParser.parseRequest(xacmlReq);
        } catch (IllegalArgumentException | IllegalAccessException | DataTypeException e) {
            logger.error("CallEmbeddedGuardTask.run threw: {}", e);
        }


        logger.debug("\n********** XACML REQUEST START ********");
        logger.debug("{}", request);
        logger.debug("********** XACML REQUEST END ********\n");

        String guardDecision = null;

        //
        // Make guard request
        //
        guardDecision = new PolicyGuardXacmlHelperEmbedded().callPdp(xacmlReq);

        logger.debug("\n********** XACML RESPONSE START ********");
        logger.debug("{}", guardDecision);
        logger.debug("********** XACML RESPONSE END ********\n");

        //
        // Check if the restful call was unsuccessful or property doesn't exist
        //
        if (guardDecision == null) {
            logger.error("********** XACML FAILED TO CONNECT ********");
            guardDecision = Util.INDETERMINATE;
        }

        guardResponse = new PolicyGuardResponse(guardDecision, UUID.fromString(this.requestId), this.recipe);


        //
        // Create an artificial Guard response in case we didn't get a clear Permit or Deny
        //
        if (guardResponse.getResult().equals("Indeterminate")) {
            guardResponse.setOperation(recipe);
            guardResponse.setRequestID(UUID.fromString(requestId));
        }

        long estimatedTime = System.nanoTime() - startTime;
        logger.debug("\n\n============ Guard inserted with decision {} !!! =========== time took: {} mili sec \n\n",
                guardResponse.getResult(), (double) estimatedTime / 1000 / 1000);
        workingMemory.insert(guardResponse);

    }

}
