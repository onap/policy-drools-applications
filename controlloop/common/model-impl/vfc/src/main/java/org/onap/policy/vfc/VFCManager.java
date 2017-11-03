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

import org.drools.core.WorkingMemory;
import org.onap.policy.vfc.util.Serialization;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.rest.RESTManager;
import org.onap.policy.rest.RESTManager.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;

public final class VFCManager implements Runnable {

    private String vfcUrlBase;
    private String username;
    private String password;
    private VFCRequest vfcRequest;
    WorkingMemory workingMem;
    private static final Logger logger = LoggerFactory.getLogger(VFCManager.class);
    private static final Logger netLogger = LoggerFactory.getLogger(org.onap.policy.drools.event.comm.Topic.NETWORK_LOGGER);

    public VFCManager(WorkingMemory wm, VFCRequest request) {
        workingMem = wm;
        vfcRequest = request;

        /*
         * TODO: What if these are null?
         */
        String url = PolicyEngine.manager.getEnvironmentProperty("vfc.url");
        String username = PolicyEngine.manager.getEnvironmentProperty("vfc.username");
        String password = PolicyEngine.manager.getEnvironmentProperty("vfc.password");

        setVFCParams(url, username, password);

    }

    public void setVFCParams(String baseUrl, String name, String pwd) {
        vfcUrlBase = baseUrl + "/api/nslcm/v1";
        username = name;
        password = pwd;
    }

    @Override
    public void run() {

        Map<String, String> headers = new HashMap<String, String>();
        Pair<Integer, String> httpDetails;

        VFCResponse responseError = new VFCResponse();
        responseError.responseDescriptor = new VFCResponseDescriptor();
        responseError.responseDescriptor.status = "error";

        headers.put("Accept", "application/json");
        String vfcUrl = vfcUrlBase + "/ns/" + vfcRequest.nsInstanceId + "/heal";
        try {
            String vfcRequestJson = Serialization.gsonPretty.toJson(vfcRequest);
            netLogger.info("[OUT|{}|{}|]{}{}", "VFC", vfcUrl, System.lineSeparator(), vfcRequestJson);

            httpDetails = RESTManager.post(vfcUrl, username, password, headers,
                    "application/json", vfcRequestJson);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            workingMem.insert(responseError);
            return;
        }

        if (httpDetails == null) {
            workingMem.insert(responseError);
            return;
        }

        if (httpDetails.a == 202) {
            try {
                VFCResponse response = Serialization.gsonPretty.fromJson(httpDetails.b, VFCResponse.class);
                netLogger.info("[IN|{}|{}|]{}{}", "VFC", vfcUrl, System.lineSeparator(), httpDetails.b);
                String body = Serialization.gsonPretty.toJson(response);
                logger.debug("Response to VFC Heal post:");
                logger.debug(body);

                String jobId = response.jobId;
                int attemptsLeft = 20;

                String urlGet = vfcUrlBase + "/jobs/" + jobId;
                VFCResponse responseGet = null;

                while (attemptsLeft-- > 0) {

                    netLogger.info("[OUT|{}|{}|]", "VFC", urlGet);
                    Pair<Integer, String> httpDetailsGet = RESTManager.get(urlGet, username, password, headers);
                    responseGet = Serialization.gsonPretty.fromJson(httpDetailsGet.b, VFCResponse.class);
                    netLogger.info("[IN|{}|{}|]{}{}", "VFC", urlGet, System.lineSeparator(), httpDetailsGet.b);
                    responseGet.requestId = vfcRequest.requestId.toString();
                    body = Serialization.gsonPretty.toJson(responseGet);
                    logger.debug("Response to VFC Heal get:");
                    logger.debug(body);

                    if (httpDetailsGet.a == 200) {
                        if (responseGet.responseDescriptor.status.equalsIgnoreCase("finished") ||
                                responseGet.responseDescriptor.status.equalsIgnoreCase("error")) {
                            logger.debug("VFC Heal Status {}", responseGet.responseDescriptor.status);
                            workingMem.insert(responseGet);
                            break;
                        }
                    }
                    Thread.sleep(20000);
                }
                if ((attemptsLeft <= 0)
                        && (responseGet != null)
                        && (responseGet.responseDescriptor != null)
                        && (responseGet.responseDescriptor.status != null)
                        && (!responseGet.responseDescriptor.status.isEmpty())) {
                    logger.debug("VFC timeout. Status: ({})", responseGet.responseDescriptor.status);
                    workingMem.insert(responseGet);
                }
            } catch (JsonSyntaxException e) {
                logger.error("Failed to deserialize into VFCResponse {}", e.getLocalizedMessage(), e);
            } catch (InterruptedException e) {
                logger.error("Interrupted exception: {}", e.getLocalizedMessage(), e);
                Thread.currentThread().interrupt();
            }
        } else {
            logger.warn("VFC Heal Restcall failed");
        }
    }
}
