/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018 Huawei. All rights reserved.
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

package org.onap.policy.sdnc;

import java.util.LinkedList;

import org.junit.Test;
import org.onap.policy.sdnc.util.Serialization;

public class TestDemo {

    @Test
    public void test() {
        SdncRequest request = new SdncRequest();

        request.setNsInstanceId("100");
        request.setHealRequest(new SdncHealRequest());

        request.getHealRequest().setRequestHeaderInfo(new SdncHealRequestHeaderInfo());
        request.getHealRequest().getRequestHeaderInfo().setSvcRequestId("service-req-01");
        request.getHealRequest().getRequestHeaderInfo().setSvcAction("servive-action");

        request.getHealRequest().setRequestInfo(new SdncHealRequestInfo());
        request.getHealRequest().getRequestInfo().setRequestAction("request-action");
        
        request.getHealRequest().setServiceInfo(new SdncHealServiceInfo());
        request.getHealRequest().getServiceInfo().setServiceInstanceId("service-instance-01");
        
        request.getHealRequest().setNetworkInfo(new SdncHealNetworkInfo());
        request.getHealRequest().getNetworkInfo().setNetworkId("network-5555");
        

        String body = Serialization.gsonPretty.toJson(request);
        System.out.println(body);

        SdncResponse response = new SdncResponse();

        body = Serialization.gsonPretty.toJson(response);
        System.out.println(body);

        response.setRequestId("request-01");
        response.setResponseOutput(new SdncResponseOutput());
        response.getResponseOutput().setSvcRequestId("service-req-01");
        response.getResponseOutput().setResponseCode("200");
        response.getResponseOutput().setAckFinalIndicator("final-indicator-00");
        
        body = Serialization.gsonPretty.toJson(response);
        System.out.println(body);

        response = Serialization.gsonPretty.fromJson(body, SdncResponse.class);
        body = Serialization.gsonPretty.toJson(response);
        System.out.println(body);

    }
}
