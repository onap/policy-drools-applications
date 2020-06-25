/*-
 * ============LICENSE_START=======================================================
 * m2/test
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020 Bell Canada.
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

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertNotNull;
import static org.onap.policy.guard.Util.ONAP_KEY_PASS;
import static org.onap.policy.guard.Util.ONAP_KEY_URL;
import static org.onap.policy.guard.Util.ONAP_KEY_USER;
import static org.onap.policy.guard.Util.PROP_GUARD_URL;
import static org.onap.policy.m2.test.Util.assertSubset;
import static org.onap.policy.m2.test.Util.json;

import com.google.gson.JsonObject;
import java.io.File;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.awaitility.Durations;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.drools.util.KieUtils;
import org.onap.policy.drools.utils.PropertyUtil;
import org.onap.policy.m2.test.Util.Input;
import org.onap.policy.m2.test.Util.Output;

public class AppcLcmTest {
    private static String closedLoopControlName = null;
    private static Output dcae = null;
    private static Output appcResponse = null;
    private static Input notification = null;
    private static Input appcRequest = null;
    private static Properties properties = null;
    private static PolicyController policyController = null;

    /**
     * Initialization method, which creates the following:
     * 1) VUSPLCM artifact
     * 2) The associated PolicyController and Drools session
     * 3) DMAAP/UEB topic writers and readers
     * .
     */
    @BeforeClass
    public static void init() throws Exception {
        Util.commonInit();

        String projectVersion = System.getProperty("project.version");
        assertNotNull(projectVersion);
        closedLoopControlName = "appclcm-" + UUID.randomUUID().toString();

        File kmodule = new File("src/test/resources/appclcm/kmodule.xml");

        String pom = Util.openAndReplace("src/test/resources/appclcm/pom.xml",
            "${project.version}", projectVersion);

        String yaml = Util.fileToString(new File("src/test/resources/appclcm/CLRulevUSPAPPCLCMGuardTemplate.yaml"));

        // build a '.drl' file (as a String), by replacing '${variable}' names
        String drl = Util.openAndReplace(
            "src/test/resources/appclcm/M2CLRulevUSPAPPCLCMGuardTemplate.drl",
            "${closedLoopControlName}", closedLoopControlName,
            "${controlLoopYaml}", Util.convertYaml(yaml),
            "${notificationTopic}", "NOTIFICATION-APPCLCM-TOPIC",
            "${operationTopic}", "APPC-REQUEST-APPCLCM-TOPIC",
            "${policyName}", "appclcm",
            "${policyScope}", "service=vUSP;resource=vCTS;type=operational",
            "${policyVersion}",
            "org.onap.policy.m2.test:appclcm:" + projectVersion,
            "${unique}", "2");

        // this creates the JAR file, and installs it in the local repository
        KieUtils.installArtifact(kmodule, Util.stringToFile(pom, ".xml"),
            "src/main/resources/rules/rules.drl", Util.stringToFile(drl, ".drl"));

        properties = PropertyUtil.getProperties("src/test/resources/appclcm/controller.properties");
        properties.setProperty("rules.version", projectVersion);
        //properties.setProperty("pdpx.username", "");
        //properties.setProperty("pdpx.password", "");

        // create PolicyController, which creates the Drools session
        PolicyEngineConstants.getManager().setEnvironmentProperty(PROP_GUARD_URL, "http://127.0.0.1:8443/pdp/");
        PolicyEngineConstants.getManager().setEnvironmentProperty(ONAP_KEY_URL, "jdbc:h2:file:./H2DB");
        PolicyEngineConstants.getManager().setEnvironmentProperty(ONAP_KEY_USER, "sa");
        PolicyEngineConstants.getManager().setEnvironmentProperty(ONAP_KEY_PASS, "");
        policyController =
            PolicyEngineConstants.getManager().createPolicyController("appclcm", properties);
        policyController.start();

        // create writers
        dcae = new Output("org.onap.DCAE-APPCLCM-TOPIC");
        appcResponse = new Output("APPC-RESPONSE-APPCLCM-TOPIC");

        // create readers
        notification = new Input("NOTIFICATION-APPCLCM-TOPIC");
        appcRequest = new Input("APPC-REQUEST-APPCLCM-TOPIC");
    }

    /**
     * Clean up.
     */
    @AfterClass
    public static void cleanup() {
        // close readers
        notification.close();
        appcRequest.close();

        // close writers
        dcae.close();
        appcResponse.close();

        // shut down PolicyController and Drools session
        policyController.stop();
        PolicyEngineConstants.getManager().stop();

        // clean up REST servers
        Util.commonShutdown();
    }

    /**
     * This is a sunny-day scenario.
     */
    @Test
    public void sunnyDayTest() throws Exception {
        Request req = new Request();

        // send initial ONSET message
        dcae.send(req.msg);

        // receive active notification, and restart operation
        awaitAndAssert(10, Durations.TWO_HUNDRED_MILLISECONDS, notification,
            json("notification", "ACTIVE"));

        appcOperation(req, "Restart", 400, "Restart Successful");

        // send ABATED
        req.msg.addProperty("closedLoopEventStatus", "ABATED");
        dcae.send(req.msg);

        // receive final success notification
        awaitAndAssert(10, Durations.TWO_HUNDRED_MILLISECONDS, notification,
                json("notification", "FINAL: SUCCESS"));

        // sleep to allow DB update
        Thread.sleep(1000);
    }

    /**
     * In this scenario, all requests fail until the final 'Evacuate'.
     */
    @Test
    public void initialFailure() throws Exception {
        Request req = new Request();

        // send initial ONSET message
        dcae.send(req.msg);

        // active notification, and restart 1 operation
        awaitAndAssert(10, Durations.TWO_HUNDRED_MILLISECONDS, notification,
                json("notification", "ACTIVE"));

        appcOperation(req, "Restart", 450, "Restart 1 Failed");
        appcOperation(req, "Restart", 450, "Restart 2 Failed");
        appcOperation(req, "Rebuild", 450, "Rebuild Failed");
        appcOperation(req, "Migrate", 450, "Migrate Failed");
        appcOperation(req, "Evacuate", 400, "Evacuate Successful");

        // send ABATED
        req.msg.addProperty("closedLoopEventStatus", "ABATED");
        dcae.send(req.msg);

        // receive final success notification
        awaitAndAssert(10, Durations.TWO_HUNDRED_MILLISECONDS, notification,
                json("notification", "FINAL: SUCCESS"));

        // sleep to allow DB update
        Thread.sleep(1000);
    }

    private void awaitAndAssert(int maxWaitSecond, Duration pollIntervalMilli, Input notification,
        JsonObject jsonObj) {
        AtomicReference<JsonObject> obj = new AtomicReference<>();
        await().atMost(maxWaitSecond, TimeUnit.SECONDS)
            .with().pollInterval(pollIntervalMilli)
            .until(() -> {
                obj.set(notification.poll());
                return obj.get() != null;
            });
        assertSubset(jsonObj, obj.get());
    }

    private void appcOperation(Request req, String name, int responseCode, String responseMessage)
        throws Exception {
        String lcName = name.toLowerCase();
        assertSubset(json("notification", "OPERATION",
                          "message", ".*operation=" + name + ",.*"),
                     notification.poll());

        // receive request
        JsonObject opRequest = appcRequest.poll();
        assertSubset(json("version", "2.0",
                          "rpc-name", lcName,
                          "correlation-id", ".*",
                          "type", "request",
                          "body",
                          json("input", json("common-header",
                                             json("request-id", req.requestId),
                                             "action", name
                                            ))),
                     opRequest);

        // send response
        JsonObject ch = opRequest
                        .getAsJsonObject("body")
                        .getAsJsonObject("input")
                        .getAsJsonObject("common-header");
        JsonObject opResponse =
            json("correlation-id", opRequest.get("correlation-id"),
                 "body", json("output",
                              json("common-header",
                                   json("flags", json(),
                                        "api-ver", "2.00",
                                        "originator-id", ch.get("originator-id"),
                                        "sub-request-id", ch.get("sub-request-id"),
                                        "request-id", req.requestId,
                                        "timestamp", ch.get("timestamp")
                                       ),
                                   "status",
                                   json("code", responseCode,
                                        "message", responseMessage
                                       ))),
                 "type", "response",
                 "version", "2.0",
                 "rpc-name", lcName
                );
        appcResponse.send(opResponse);

        // receive success or failure notification
        String expectedNotification =
            (responseCode == 400 ? "OPERATION: SUCCESS" : "OPERATION: FAILURE");
        assertSubset(json("notification", expectedNotification,
                          "message", ".*operation=" + name + ",.*"),
                     notification.poll());
    }

    /* ============================================================ */

    /**
     * An instance of this class is created for each Transaction. It allocates
     * any identifiers, such as 'requestId', and creates the initial ONSET
     * message.
     */
    class Request {
        String requestId;
        String triggerId;
        JsonObject msg;

        Request() {
            long time = System.currentTimeMillis();
            requestId = UUID.randomUUID().toString();
            triggerId = "trigger-" + time;

            msg = json("closedLoopEventClient", "configuration.dcae.microservice.stringmatcher.xml",
                       "policyVersion", "1610",
                       "triggerSourceName", "ctsf0002vm014",
                       "policyName", "vUSP_vCTS_CL_7.Config_MS_ClosedLoop_"
                       + "104b1445_6b30_11e7_852e_0050568c4ccf_StringMatch_1wo2qh0",
                       "policyScope", "resource=F5,service=vSCP,type=configuration,"
                       + "closedLoopControlName=vSCP_F5_Firewall_d925ed73-8231-4d02-9545-db4e101f88f8",
                       "triggerID", triggerId,
                       "target_type", "VM",
                       "AAI",
                       json("vserver.l-interface.l3-interface-ipv6-address-list.l3-inteface-ipv6-address", null,
                            "vserver.selflink", "https://compute-aic.dpa3.cci.att.com:8774/v2/d0719b845a804b368f8ac0bba39e188b/servers/7953d05b-6698-4aa6-87bd-39bed606133a",
                            "vserver.is-closed-loop-disabled", "false",
                            "vserver.l-interface.network-name", "vUSP_DPA3_OAM_3750",
                            "vserver.l-interface.l3-interface-ipv4-address-list.l3-inteface-ipv4-address", //continues
                            "135.144.3.50",
                            "vserver.vserver-id", "78fe4342-8f85-49ba-be9f-d0c1bdf1ba7b",
                            "generic-vnf.service-id", "e433710f-9217-458d-a79d-1c7aff376d89",
                            "complex.city", "AAIDefault",
                            "vserver.in-maint", "N",
                            "complex.state", "NJ",
                            "vserver.vserver-name", "ctsf0002vm025",
                            "complex.physical-location-id", "LSLEILAA",
                            "tenant.tenant-id", "d0719b845a804b368f8ac0bba39e188b",
                            "vserver.prov-status", "PROV",
                            "generic-vnf.vnf-name", "ctsf0002v",
                            "vserver.l-interface.interface-name", // continues
                            "ctsf0002v-ALU-LCP-Pair07-oziwyxlxwdyc-1-a4psuz5awjw7-ALU-LCP-ETH2-ygmny7m7rpb5",
                            "generic-vnf.vnf-type", "vUSP-vCTS",
                            "cloud-region.identity-url", "https://auth.pdk11.cci.att.com:5000/v2.0"
                           ),
                       "closedLoopAlarmStart", "1507143409107000",
                       "closedLoopEventStatus", "ONSET",
                       "closedLoopControlName", closedLoopControlName,
                       "version", "1.0.2",
                       "target", "vserver.vserver-name",
                       "resourceInstance", json("resourceName", "",
                                                "resourceInstanceName", ""
                                               ),
                       "requestID", requestId,
                       "from", "DCAE",
                       "serviceInstance", json("serviceInstanceName", "",
                                               "serviceName", ""
                                              )
                      );
        }
    }
}
