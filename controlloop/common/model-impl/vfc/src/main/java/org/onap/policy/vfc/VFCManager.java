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

import java.util.HashMap;
import java.util.Map;

import org.onap.policy.vfc.util.Serialization;
import org.onap.policy.rest.RESTManager;
import org.onap.policy.rest.RESTManager.Pair;

import com.google.gson.JsonSyntaxException;

public final class VFCManager implements Runnable{

    String vfcUrlBase;
    String username;
    String password;
    VFCRequest vfcRequest;

    public VFCManager(VFCRequest request) {
        vfcRequest = request;
        // TODO: Get base URL, username and password from MSB?
        // TODO: Following code is a placeholder, needs to be updated
        setVFCParams("https://", "vfc", "vfc");

    }

    public void setVFCParams(String baseUrl, String name, String pwd) {
        vfcUrlBase = baseUrl + "/api/nslcm/v1";
        username = name;
        password = pwd;
    }

    @Override
    public void run() {

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "application/json");

        String vfcUrl = vfcUrlBase + "/ns/" + vfcRequest.nsInstanceId + "/heal";
        Pair<Integer, String> httpDetails = RESTManager.post(vfcUrl, username, password, headers,
                "application/json", Serialization.gsonPretty.toJson(vfcRequest));

        if (httpDetails == null) {
            return;
        }

        if (httpDetails.a == 202) {
            try {
                VFCResponse response = Serialization.gsonPretty.fromJson(httpDetails.b, VFCResponse.class);

                String body = Serialization.gsonPretty.toJson(response);
                System.out.println("Response to VFC Heal post:");
                System.out.println(body);

                String jobId = response.jobId;
                int attemptsLeft = 20;

                String urlGet = vfcUrlBase + "/jobs/" + jobId;
                VFCResponse responseGet = null;

                while (attemptsLeft-- > 0) {

                    Pair<Integer, String> httpDetailsGet = RESTManager.get(urlGet, username, password, headers);
                    responseGet = Serialization.gsonPretty.fromJson(httpDetailsGet.b, VFCResponse.class);
                    body = Serialization.gsonPretty.toJson(responseGet);
                    System.out.println("Response to VFC Heal get:");
                    System.out.println(body);

                    if (httpDetailsGet.a == 200) {
                        if (responseGet.responseDescriptor.status.equalsIgnoreCase("finished") ||
                                responseGet.responseDescriptor.status.equalsIgnoreCase("error")) {
                            System.out.println("VFC Heal Status " + responseGet.responseDescriptor.status);
                            break;
                        }
                    }
                    Thread.sleep(20000);
                }
                if (attemptsLeft <= 0)
                    System.out.println("VFC timeout. Status: (" + responseGet.responseDescriptor.status + ")");
            } catch (JsonSyntaxException e) {
                System.err.println("Failed to deserialize into VFCResponse" + e.getLocalizedMessage());
            } catch (InterruptedException e) {
                System.err.println("Interrupted exception: " + e.getLocalizedMessage());
            }
        } else {
            System.out.println("VFC Heal Restcall failed");
        }
    }
}
