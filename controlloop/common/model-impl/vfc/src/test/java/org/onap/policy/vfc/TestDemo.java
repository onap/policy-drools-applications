/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2017 Intel Corp. All rights reserved.
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

package org.onap.policy.vfc;

import java.util.LinkedList;

import org.junit.Test;
import org.onap.policy.vfc.util.Serialization;

public class TestDemo {

    @Test
    public void test() {
        VFCRequest request = new VFCRequest();

        request.nsInstanceId = "100";
        request.healRequest = new VFCHealRequest();
        request.healRequest.vnfInstanceId = "1";
        request.healRequest.cause = "vm is down";

        request.healRequest.additionalParams = new VFCHealAdditionalParams();
        request.healRequest.additionalParams.action = "restartvm";

        request.healRequest.additionalParams.actionInfo = new VFCHealActionVmInfo();
        request.healRequest.additionalParams.actionInfo.vmid = "33";
        request.healRequest.additionalParams.actionInfo.vmname = "xgw-smp11";

        String body = Serialization.gsonPretty.toJson(request);
        System.out.println(body);

        VFCResponse response = new VFCResponse();
        response.jobId = "1";

        body = Serialization.gsonPretty.toJson(response);
        System.out.println(body);

        response.responseDescriptor = new VFCResponseDescriptor();
        response.responseDescriptor.progress = "40";
        response.responseDescriptor.status = "processing";
        response.responseDescriptor.statusDescription = "OMC VMs are decommissioned in VIM";
        response.responseDescriptor.errorCode = null;
        response.responseDescriptor.responseId = "42";
        body = Serialization.gsonPretty.toJson(response);
        System.out.println(body);

        VFCResponseDescriptor responseDescriptor = new VFCResponseDescriptor();
        responseDescriptor.progress = "20";
        responseDescriptor.status = "processing";
        responseDescriptor.statusDescription = "OMC VMs are decommissioned in VIM";
        responseDescriptor.errorCode = null;
        responseDescriptor.responseId = "11";

	    response.responseDescriptor.responseHistoryList = new LinkedList<>();
        response.responseDescriptor.responseHistoryList.add(responseDescriptor);

        body = Serialization.gsonPretty.toJson(response);
        System.out.println(body);

        response = Serialization.gsonPretty.fromJson(body, VFCResponse.class);
        body = Serialization.gsonPretty.toJson(response);
        System.out.println(body);

    }
}
