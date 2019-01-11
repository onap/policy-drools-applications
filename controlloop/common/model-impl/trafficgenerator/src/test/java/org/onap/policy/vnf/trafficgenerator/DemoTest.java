/*-
 * ============LICENSE_START=======================================================
 * trafficgenerator
 * ================================================================================
 * Copyright (C) 2017-2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.vnf.trafficgenerator;

import org.junit.Test;

import org.onap.policy.vnf.trafficgenerator.PgRequest;
import org.onap.policy.vnf.trafficgenerator.PgStream;
import org.onap.policy.vnf.trafficgenerator.PgStreams;
import org.onap.policy.vnf.trafficgenerator.util.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemoTest {
    private static final Logger logger = LoggerFactory.getLogger(DemoTest.class);

    @Test
    public void test() {
        PgRequest request = new PgRequest();
        request.pgStreams = new PgStreams();

        PgStream pgStream;
        for (int i = 0; i < 5; i++) {
            pgStream = new PgStream();
            pgStream.streamId = "fw_udp" + (i + 1);
            pgStream.isEnabled = "true";
            request.pgStreams.pgStream.add(pgStream);
        }

        String body = Serialization.gsonPretty.toJson(request);
        logger.debug(body);

        // fail("Not yet implemented");
    }

}
