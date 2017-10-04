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

package org.onap.policy.template.demo;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.onap.policy.appclcm.LCMRequest;
import org.onap.policy.appclcm.LCMRequestWrapper;
import org.onap.policy.appclcm.LCMResponse;
import org.onap.policy.appclcm.LCMResponseWrapper;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.policy.ControlLoopPolicy;
import org.onap.policy.drools.PolicyEngineListener;
import org.onap.policy.drools.http.server.HttpServletServer;
import org.onap.policy.drools.impl.PolicyEngineJUnitImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VCPEControlLoopTest implements PolicyEngineListener {

    private static final Logger logger = LoggerFactory.getLogger(VCPEControlLoopTest.class);
    
    private KieSession kieSession;
    private Util.Pair<ControlLoopPolicy, String> pair;
    private PolicyEngineJUnitImpl engine; 
    private UUID requestID;
    
    static {
        /* Set environment properties */
        Util.setAAIProps();
        Util.setGuardProps();
        Util.setPUProp();
    }
    
    @BeforeClass
    public static void setUpSimulator() {
        try {
            Util.buildAaiSim();
            Util.buildGuardSim();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @AfterClass
    public static void tearDownSimulator() {
        HttpServletServer.factory.destroy();
    }
    
    @Test
    public void successTest() {
        /*
         * Start the kie session
         */
        try {
            kieSession = startSession("src/main/resources/ControlLoop_Template_xacml_guard.drl", 
                        "src/test/resources/yaml/policy_ControlLoop_vCPE.yaml",
                        "service=ServiceDemo;resource=Res1Demo;type=operational", 
                        "CL_vCPE", 
                        "org.onap.closed_loop.ServiceDemo:VNFS:1.0.0");
        } catch (IOException e) {
            e.printStackTrace();
            logger.debug("Could not create kieSession");
            fail("Could not create kieSession");
        }
        
        /*
         * Allows the PolicyEngine to callback to this object to
         * notify that there is an event ready to be pulled 
         * from the queue
         */
        engine.addListener(this);
        
        /*
         * Create a unique requestId
         */
        requestID = UUID.randomUUID();
        
        /* 
         * Simulate an onset event the policy engine will 
         * receive from DCAE to kick off processing through
         * the rules
         */
        sendEvent(pair.a, requestID, ControlLoopEventStatus.ONSET);
        
        kieSession.fireUntilHalt();
        
        /*
         * The only fact in memory should be Params
         */
        assertEquals(1, kieSession.getFactCount());
        
        /*
         * Print what's left in memory
         */
        dumpFacts(kieSession);
        
        /*
         * Gracefully shut down the kie session
         */
        kieSession.dispose();
    }

    /**
     * This method will start a kie session and instantiate 
     * the Policy Engine.
     * 
     * @param droolsTemplate
     *          the DRL rules file
     * @param yamlFile
     *          the yaml file containing the policies
     * @param policyScope
     *          scope for policy
     * @param policyName
     *          name of the policy
     * @param policyVersion
     *          version of the policy          
     * @return the kieSession to be used to insert facts 
     * @throws IOException
     */
    private KieSession startSession(String droolsTemplate, 
            String yamlFile, 
            String policyScope, 
            String policyName, 
            String policyVersion) throws IOException {
        
        /*
         * Load policies from yaml
         */
        pair = Util.loadYaml(yamlFile);
        assertNotNull(pair);
        assertNotNull(pair.a);
        assertNotNull(pair.a.getControlLoop());
        assertNotNull(pair.a.getControlLoop().getControlLoopName());
        assertTrue(pair.a.getControlLoop().getControlLoopName().length() > 0);
        
        /* 
         * Construct a kie session
         */
        final KieSession kieSession = Util.buildContainer(droolsTemplate, 
                pair.a.getControlLoop().getControlLoopName(), 
                policyScope, 
                policyName, 
                policyVersion, 
                URLEncoder.encode(pair.b, "UTF-8"));
        
        /*
         * Retrieve the Policy Engine
         */
        engine = (PolicyEngineJUnitImpl) kieSession.getGlobal("Engine");
        
        logger.debug("============");
        logger.debug(URLEncoder.encode(pair.b, "UTF-8"));
        logger.debug("============");
        
        return kieSession;
    }
    
    /*
     * (non-Javadoc)
     * @see org.onap.policy.drools.PolicyEngineListener#newEventNotification(java.lang.String)
     */
    public void newEventNotification(String topic) {
        /*
         * Pull the object that was sent out to DMAAP and make
         * sure it is a ControlLoopNoticiation of type active
         */
        Object obj = engine.subscribe("UEB", topic);
        assertNotNull(obj);
        if (obj instanceof VirtualControlLoopNotification) {
            VirtualControlLoopNotification notification = (VirtualControlLoopNotification) obj;
            String policyName = notification.policyName;
            if (policyName.endsWith("EVENT")) {
                logger.debug("Rule Fired: " + notification.policyName);
                assertTrue(ControlLoopNotificationType.ACTIVE.equals(notification.notification));
            }
            else if (policyName.endsWith("GUARD_NOT_YET_QUERIED")) {
                logger.debug("Rule Fired: " + notification.policyName);
                assertTrue(ControlLoopNotificationType.OPERATION.equals(notification.notification));
                assertNotNull(notification.message);
                assertTrue(notification.message.startsWith("Sending guard query"));
            }
            else if (policyName.endsWith("GUARD.RESPONSE")) {
                logger.debug("Rule Fired: " + notification.policyName);
                assertTrue(ControlLoopNotificationType.OPERATION.equals(notification.notification));
                assertNotNull(notification.message);
                assertTrue(notification.message.endsWith("PERMIT"));
            }
            else if (policyName.endsWith("GUARD_PERMITTED")) {
                logger.debug("Rule Fired: " + notification.policyName);
                assertTrue(ControlLoopNotificationType.OPERATION.equals(notification.notification));
                assertNotNull(notification.message);
                assertTrue(notification.message.startsWith("actor=APPC"));
            }
            else if (policyName.endsWith("OPERATION.TIMEOUT")) {
                logger.debug("Rule Fired: " + notification.policyName);
                kieSession.halt();
                logger.debug("The operation timed out");
                fail("Operation Timed Out");
            }
            else if (policyName.endsWith("APPC.LCM.RESPONSE")) {
                logger.debug("Rule Fired: " + notification.policyName);
                assertTrue(ControlLoopNotificationType.OPERATION_SUCCESS.equals(notification.notification));
                assertNotNull(notification.message);
                assertTrue(notification.message.startsWith("actor=APPC"));
                sendEvent(pair.a, requestID, ControlLoopEventStatus.ABATED);
            }
            else if (policyName.endsWith("EVENT.MANAGER")) {
                logger.debug("Rule Fired: " + notification.policyName);
                assertTrue(ControlLoopNotificationType.FINAL_SUCCESS.equals(notification.notification));
                kieSession.halt();
            }
            else if (policyName.endsWith("EVENT.MANAGER.TIMEOUT")) {
                logger.debug("Rule Fired: " + notification.policyName);
                kieSession.halt();
                logger.debug("The control loop timed out");
                fail("Control Loop Timed Out");
            }
        }
        else if (obj instanceof LCMRequestWrapper) {
            /*
             * The request should be of type LCMRequestWrapper
             * and the subrequestid should be 1
             */
            LCMRequestWrapper dmaapRequest = (LCMRequestWrapper) obj;
            LCMRequest appcRequest = dmaapRequest.getBody();
            assertTrue(appcRequest.getCommonHeader().getSubRequestId().equals("1"));
            
            logger.debug("\n============ APPC received the request!!! ===========\n");
            
            /*
             * Simulate a success response from APPC and insert
             * the response into the working memory
             */
            LCMResponseWrapper dmaapResponse = new LCMResponseWrapper();
            LCMResponse appcResponse = new LCMResponse(appcRequest);
            appcResponse.getStatus().setCode(400);
            appcResponse.getStatus().setMessage("AppC success");
            dmaapResponse.setBody(appcResponse);
            kieSession.insert(dmaapResponse);
        }        
    }
    
    /**
     * This method is used to simulate event messages from DCAE
     * that start the control loop (onset message) or end the
     * control loop (abatement message).
     * 
     * @param policy the controlLoopName comes from the policy 
     * @param requestID the requestId for this event
     * @param status could be onset or abated
     */
    protected void sendEvent(ControlLoopPolicy policy, UUID requestID, ControlLoopEventStatus status) {
        VirtualControlLoopEvent event = new VirtualControlLoopEvent();
        event.closedLoopControlName = policy.getControlLoop().getControlLoopName();
        event.requestID = requestID;
        event.target = "generic-vnf.vnf-id";
        event.closedLoopAlarmStart = Instant.now();
        event.AAI = new HashMap<>();
        event.AAI.put("generic-vnf.vnf-id", "testGenericVnfID");
        event.closedLoopEventStatus = status;
        kieSession.insert(event);
    }
    
    /**
     * This method will dump all the facts in the working memory.
     * 
     * @param kieSession the session containing the facts
     */
    public void dumpFacts(KieSession kieSession) {
        logger.debug("Fact Count: {}", kieSession.getFactCount());
        for (FactHandle handle : kieSession.getFactHandles()) {
            logger.debug("FACT: {}", handle);
        }
    }
}
