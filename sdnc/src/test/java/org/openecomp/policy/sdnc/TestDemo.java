/*-
 * ============LICENSE_START=======================================================
 * sdnc
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

package org.openecomp.policy.sdnc;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openecomp.policy.sdnc.SDNCRequest;
import org.openecomp.policy.sdnc.util.Serialization;

public class TestDemo {

	@Test
	public void test() {
		
		SDNCRequest request = new SDNCRequest();
		request.input = new SDNCRequestInput();
		request.input.requestHeader = new SDNCRequestHeader();
		request.input.vnfTopolgyInformation = new SDNCVnfTopologyInformation();
		request.input.vnfTopolgyInformation.vnfTopologyIdentifier = new SDNCVnfTopologyIdentifier();
		request.input.vnfTopolgyInformation.vnfAssignments = new SDNCVnfAssignments();
		request.input.requestInformation = new SDNCRequestInformation();
		
		request.input.vnfTopolgyInformation.vnfTopologyIdentifier.serviceType = "my_serviceType";
		request.input.vnfTopolgyInformation.vnfTopologyIdentifier.vnfName = "my_vnfName";
		request.input.vnfTopolgyInformation.vnfTopologyIdentifier.vnfType = "my_vnfType";
		request.input.vnfTopolgyInformation.vnfTopologyIdentifier.genericVnfName = "my_genericVnfName";
		request.input.vnfTopolgyInformation.vnfTopologyIdentifier.genericVnfType = "my_genericVnfType";
		
		request.input.vnfTopolgyInformation.vnfAssignments.availabilityZones.add("zone1");
		request.input.vnfTopolgyInformation.vnfAssignments.availabilityZones.add("zone2");
		request.input.vnfTopolgyInformation.vnfAssignments.vnfNetworks.add("network1");
		request.input.vnfTopolgyInformation.vnfAssignments.vnfNetworks.add("network2");
		request.input.vnfTopolgyInformation.vnfAssignments.vnfVms.add("vnfVm1");
		request.input.vnfTopolgyInformation.vnfAssignments.vnfVms.add("vnfVm2");
		
		Map<String, String> vnfParams1 = new HashMap<String, String>();
		vnfParams1.put("name1", "value1");
		request.input.vnfTopolgyInformation.vnfParameters.add(vnfParams1);

		Map<String, String> vnfParams2 = new HashMap<String, String>();
		vnfParams2.put("name2", "value2");
		request.input.vnfTopolgyInformation.vnfParameters.add(vnfParams2);
		
		
		request.input.requestInformation.requestId = "ff5256d1-5a33-55df-13ab-12abad84e7ff";
		request.input.requestInformation.orderNumber = "1";
		request.input.requestInformation.orderVersion = "1";
		request.input.requestInformation.notificationUrl = "sdnc.myDomain.com";
		request.input.requestInformation.requestAction = "PreloadVNFRequest";
		
		request.input.requestHeader.svcRequestId = "ff5256d1-5a33-55df-13ab-12abad84e7ff";
		request.input.requestHeader.svcNotificationUrl = "some_url.myDomain.com:8080";
		request.input.requestHeader.svcAction = "reserve";
		
		String body = Serialization.gsonPretty.toJson(request);
		System.out.println(body);
		
		
		
	}

}
