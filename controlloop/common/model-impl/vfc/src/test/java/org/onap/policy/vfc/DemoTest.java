/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2017 Intel Corp. All rights reserved.
 * Modifications Copyright (C) 2018 AT&T Corporation. All rights reserved.
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

public class DemoTest {

    @Test
    public void test() {
        VFCRequest request = new VFCRequest();

        request.setNSInstanceId("100");
        request.setHealRequest(new VFCHealRequest());
        request.getHealRequest().setVnfInstanceId("1");
        request.getHealRequest().setCause("vm is down");

        request.getHealRequest().setAdditionalParams(new VFCHealAdditionalParams());
        request.getHealRequest().getAdditionalParams().setAction("restartvm");

        request.getHealRequest().getAdditionalParams().setActionInfo(new VFCHealActionVmInfo());
        request.getHealRequest().getAdditionalParams().getActionInfo().setVmid("33");
        request.getHealRequest().getAdditionalParams().getActionInfo().setVmname("xgw-smp11");

        String body = Serialization.gsonPretty.toJson(request);
        System.out.println(body);

        VFCResponse response = new VFCResponse();
        response.setJobId("1");

        body = Serialization.gsonPretty.toJson(response);
        System.out.println(body);

        response.setResponseDescriptor(new VFCResponseDescriptor());
        response.getResponseDescriptor().setProgress("40");
        response.getResponseDescriptor().setStatus("processing");
        response.getResponseDescriptor().setStatusDescription("OMC VMs are decommissioned in VIM");
        response.getResponseDescriptor().setErrorCode(null);
        response.getResponseDescriptor().setResponseId("42");
        body = Serialization.gsonPretty.toJson(response);
        System.out.println(body);

        VFCResponseDescriptor responseDescriptor = new VFCResponseDescriptor();
        responseDescriptor.setProgress("20");
        responseDescriptor.setStatus("processing");
        responseDescriptor.setStatusDescription("OMC VMs are decommissioned in VIM");
        responseDescriptor.setErrorCode(null);
        responseDescriptor.setResponseId("11");

        response.getResponseDescriptor().setResponseHistoryList(new LinkedList<>());
        response.getResponseDescriptor().getResponseHistoryList().add(responseDescriptor);

        body = Serialization.gsonPretty.toJson(response);
        System.out.println(body);

        response = Serialization.gsonPretty.fromJson(body, VFCResponse.class);
        body = Serialization.gsonPretty.toJson(response);
        System.out.println(body);

    }
}
