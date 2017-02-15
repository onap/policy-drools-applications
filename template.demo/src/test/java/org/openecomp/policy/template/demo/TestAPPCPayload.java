/*-
 * ============LICENSE_START=======================================================
 * demo
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

package org.openecomp.policy.template.demo;

import java.util.HashMap;
import java.util.UUID;

import org.junit.Test;
import org.openecomp.policy.appc.CommonHeader;
import org.openecomp.policy.appc.Request;
import org.openecomp.policy.appc.util.Serialization;
import org.openecomp.policy.vnf.trafficgenerator.PGRequest;
import org.openecomp.policy.vnf.trafficgenerator.PGStream;
import org.openecomp.policy.vnf.trafficgenerator.PGStreams;

public class TestAPPCPayload {

	@Test
	public void test() {
		PGRequest request = new PGRequest();
		request.pgStreams = new PGStreams();
		
		PGStream pgStream;
		for(int i = 0; i < 5; i++){
			pgStream = new PGStream();
			pgStream.streamId = "fw_udp"+(i+1);
			pgStream.isEnabled = "true";
			request.pgStreams.pgStream.add(pgStream);
		}
		
		Request appc = new Request();
		appc.CommonHeader = new CommonHeader();
		appc.CommonHeader.RequestID = UUID.randomUUID();
		appc.Action = "ModifyConfig";
		appc.Payload = new HashMap<String, Object>();
		appc.Payload.put("pg-streams", request);
		System.out.println(Serialization.gsonPretty.toJson(appc));
	}

}
