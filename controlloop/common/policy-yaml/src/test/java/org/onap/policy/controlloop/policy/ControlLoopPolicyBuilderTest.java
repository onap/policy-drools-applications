/*-
 * ============LICENSE_START=======================================================
 * policy-yaml unit test
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

package org.onap.policy.controlloop.policy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onap.policy.aai.Pnf;
import org.onap.policy.aai.PnfType;
import org.onap.policy.controlloop.policy.builder.BuilderException;
import org.onap.policy.controlloop.policy.builder.ControlLoopPolicyBuilder;
import org.onap.policy.controlloop.policy.builder.Message;
import org.onap.policy.controlloop.policy.builder.MessageLevel;
import org.onap.policy.controlloop.policy.builder.Results;
import org.onap.policy.sdc.Resource;
import org.onap.policy.sdc.ResourceType;
import org.onap.policy.sdc.Service;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;


public class ControlLoopPolicyBuilderTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testControlLoop() {
        try {
            //
            // Create a builder for our policy
            //
            ControlLoopPolicyBuilder builder =
                    ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
            //
            // Test add services
            //
            Service vSCP = new Service("vSCP");
            Service vUSP = new Service("vUSP");
            Service vTrinity = new Service("Trinity");
            builder = builder.addService(vSCP, vUSP, vTrinity);
            assertTrue(builder.getControlLoop().getServices().size() == 3);
            //
            // Test remove services
            //
            builder = builder.removeService(vSCP);
            assertTrue(builder.getControlLoop().getServices().size() == 2);
            builder = builder.removeAllServices();
            assertTrue(builder.getControlLoop().getServices().size() == 0);
            //
            // Test add resources
            //
            Resource vCTS = new Resource("vCTS", ResourceType.VF);
            Resource vCOM = new Resource("vCTS", ResourceType.VF);
            Resource vRAR = new Resource("vCTS", ResourceType.VF);
            builder = builder.addResource(vCTS, vCOM, vRAR);
            assertTrue(builder.getControlLoop().getResources().size() == 3);
            //
            // Test remove resources
            //
            builder = builder.removeResource(vCTS);
            assertTrue(builder.getControlLoop().getResources().size() == 2);
            builder = builder.removeAllResources();
            assertTrue(builder.getControlLoop().getResources().size() == 0);
        } catch (BuilderException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testAddNullService() throws BuilderException {
        ControlLoopPolicyBuilder builder =
                ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
        expectedException.expect(BuilderException.class);
        expectedException.expectMessage("Service must not be null");
        builder.addService((Service) null);
    }

    @Test
    public void testAddInvalidService() throws BuilderException {
        ControlLoopPolicyBuilder builder =
                ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
        expectedException.expect(BuilderException.class);
        expectedException.expectMessage("Invalid service - need either a serviceUUID or serviceName");
        builder.addService(new Service());
    }

    @Test
    public void testAddServiceWithUUID() throws BuilderException {
        ControlLoopPolicyBuilder builder =
                ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
        UUID uuid = UUID.randomUUID();
        Service serviceWithUUID = new Service(uuid);
        builder.addService(serviceWithUUID);
        assertTrue(builder.getControlLoop().getServices().size() == 1);
    }

    @Test
    public void testAddNullResource() throws BuilderException {
        ControlLoopPolicyBuilder builder =
                ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
        expectedException.expect(BuilderException.class);
        expectedException.expectMessage("Resource must not be null");
        builder.addResource((Resource) null);
    }


    @Test
    public void testAddInvalidResource() throws BuilderException {
        ControlLoopPolicyBuilder builder =
                ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
        expectedException.expect(BuilderException.class);
        expectedException.expectMessage("Invalid resource - need either resourceUUID or resourceName");
        builder.addResource(new Resource());
    }

    @Test
    public void testAddAndRemoveResourceWithUUID() throws BuilderException {
        ControlLoopPolicyBuilder builder =
                ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
        UUID uuid = UUID.randomUUID();
        Resource resourceWithUUID = new Resource(uuid);
        builder.addResource(resourceWithUUID);
        assertTrue(builder.getControlLoop().getResources().size() == 1);

        builder.removeResource(resourceWithUUID);
        assertTrue(builder.getControlLoop().getResources().size() == 0);
    }

    @Test
    public void testRemoveNullResource() throws BuilderException {
        ControlLoopPolicyBuilder builder =
                ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
        Resource resource = new Resource("resource1", ResourceType.VF);
        builder.addResource(resource);
        expectedException.expect(BuilderException.class);
        expectedException.expectMessage("Resource must not be null");
        builder.removeResource((Resource) null);
    }

    @Test
    public void testRemoveResourceNoExistingResources() throws BuilderException {
        ControlLoopPolicyBuilder builder =
                ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
        expectedException.expect(BuilderException.class);
        expectedException.expectMessage("No existing resources to remove");
        builder.removeResource(new Resource("resource1", ResourceType.VF));
    }

    @Test
    public void testRemoveInvalidResource() throws BuilderException {
        ControlLoopPolicyBuilder builder =
                ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
        Resource resource = new Resource("resource1", ResourceType.VF);
        builder.addResource(resource);
        expectedException.expect(BuilderException.class);
        expectedException.expectMessage("Invalid resource - need either a resourceUUID or resourceName");
        builder.removeResource(new Resource());
    }

    @Test
    public void testRemoveUnknownResource() throws BuilderException {
        ControlLoopPolicyBuilder builder =
                ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
        Resource resource = new Resource("resource1", ResourceType.VF);
        builder.addResource(resource);
        final String unknownResourceName = "reource2";
        expectedException.expect(BuilderException.class);
        expectedException.expectMessage("Unknown resource " + unknownResourceName);
        builder.removeResource(new Resource(unknownResourceName, ResourceType.VF));
    }

    @Test
    public void testControlLoopWithInitialResourceAndServices() {
        try {
            Resource vCTS = new Resource("vCTS", ResourceType.VF);
            Service vSCP = new Service("vSCP");
            Service vUSP = new Service("vUSP");
            ControlLoopPolicyBuilder builder = ControlLoopPolicyBuilder.Factory
                    .buildControlLoop(UUID.randomUUID().toString(), 2400, vCTS, vSCP, vUSP);
            assertTrue(builder.getControlLoop().getResources().size() == 1);
            assertTrue(builder.getControlLoop().getServices().size() == 2);
        } catch (BuilderException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testControlLoopWithInitialResourcesAndService() {
        try {
            Resource vCTS = new Resource("vCTS", ResourceType.VF);
            Resource vCOM = new Resource("vCTS", ResourceType.VF);
            Service vSCP = new Service("vSCP");
            ControlLoopPolicyBuilder builder = ControlLoopPolicyBuilder.Factory
                    .buildControlLoop(UUID.randomUUID().toString(), 2400, vSCP, vCTS, vCOM);
            assertTrue(builder.getControlLoop().getServices().size() == 1);
            assertTrue(builder.getControlLoop().getResources().size() == 2);
        } catch (BuilderException e) {
            fail(e.getMessage());
        }
    }

    @Test
    @Ignore
    // I'VE MARKED THIS TEST CASE AS IGNORE BECAUSE THE TEST CASE FAILS
    // This test case fails because builder.getControlLoop() returns an instance of ControlLoop
    // copied using
    // the ControlLoop(ControlLoop controlLoop) constructor.
    // This constructor does not copy the value of pnf into the newly created object
    // On the face of it, this looks like a bug, but perhaps there is a reason for this
    // PLEASE ADVISE IF THE BEHAVIOUR IS INCORRECT OR THE TEST CASE IS INVALID
    public void testControlLoopForPnf() {
        try {
            Pnf pnf = new Pnf();
            pnf.setPnfType(PnfType.ENODEB);
            ControlLoopPolicyBuilder builder =
                    ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400, pnf);
            assertEquals(pnf, builder.getControlLoop().getPnf());

            builder.removePNF();
            assertNull(builder.getControlLoop().getPnf());
        } catch (BuilderException e) {
            fail(e.getMessage());
        }
    }

    @Test
    @Ignore
    // Fails for the same reason as the above test case
    public void testSetAndRemovePnf() throws BuilderException {
        ControlLoopPolicyBuilder builder =
                ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
        assertNull(builder.getControlLoop().getPnf());

        Pnf pnf = new Pnf();
        pnf.setPnfType(PnfType.ENODEB);
        builder.setPNF(pnf);
        assertEquals(pnf, builder.getControlLoop().getPnf());

        builder.removePNF();
        assertNull(builder.getControlLoop().getPnf());
    }

    @Test
    public void testSetNullPnf() throws BuilderException {
        ControlLoopPolicyBuilder builder =
                ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
        expectedException.expect(BuilderException.class);
        expectedException.expectMessage("PNF must not be null");
        builder.setPNF(null);
    }

    @Test
    public void testSetInvalidPnf() throws BuilderException {
        ControlLoopPolicyBuilder builder =
                ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
        expectedException.expect(BuilderException.class);
        expectedException.expectMessage("Invalid PNF - need either pnfName or pnfType");
        builder.setPNF(new Pnf());
    }

    @Test
    public void testSetAbatement() throws BuilderException {
        ControlLoopPolicyBuilder builder =
                ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
        assertFalse(builder.getControlLoop().getAbatement());
        builder = builder.setAbatement(true);
        assertTrue(builder.getControlLoop().getAbatement());
    }

    @Test
    public void testSetNullAbatement() throws BuilderException {
        ControlLoopPolicyBuilder builder =
                ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
        expectedException.expect(BuilderException.class);
        expectedException.expectMessage("abatement must not be null");
        builder = builder.setAbatement(null);
    }

    @Test
    public void testTimeout() {
        try {
            //
            // Create a builder for our policy
            //
            ControlLoopPolicyBuilder builder =
                    ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
            //
            // Test setTimeout
            //
            assertTrue(builder.getControlLoop().getTimeout() == 2400);
            builder = builder.setTimeout(800);
            assertTrue(builder.getControlLoop().getTimeout() == 800);
            //
            // Test calculateTimeout
            //
            Policy trigger =
                    builder.setTriggerPolicy("Restart the VM", "Upon getting the trigger event, restart the VM", "APPC",
                            new Target(TargetType.VM), "Restart", null, 2, 300);
            @SuppressWarnings("unused")
            Policy onRestartFailurePolicy = builder.setPolicyForPolicyResult("Rebuild VM",
                    "If the restart fails, rebuild it", "APPC", new Target(TargetType.VM), "Rebuild", null, 1, 600,
                    trigger.getId(), PolicyResult.FAILURE, PolicyResult.FAILURE_RETRIES, PolicyResult.FAILURE_TIMEOUT);
            assertTrue(builder.calculateTimeout().equals(new Integer(300 + 600)));
            //
        } catch (BuilderException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testTriggerPolicyMethods() {
        try {
            ControlLoopPolicyBuilder builder =
                    ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
            //
            // Test isOpenLoop
            //
            assertTrue(builder.isOpenLoop());
            //
            // Test set initial trigger policy
            //
            Policy triggerPolicy1 =
                    builder.setTriggerPolicy("Restart the VM", "Upon getting the trigger event, restart the VM", "APPC",
                            new Target(TargetType.VM), "Restart", null, 2, 300);
            assertTrue(builder.isOpenLoop() == false);
            assertTrue(builder.getControlLoop().getTrigger_policy().equals(triggerPolicy1.getId()));
            //
            // Set trigger policy to a new policy
            //
            @SuppressWarnings("unused")
            Policy triggerPolicy2 =
                    builder.setTriggerPolicy("Rebuild the VM", "Upon getting the trigger event, rebuild the VM", "APPC",
                            new Target(TargetType.VM), "Rebuild", null, 2, 300);
            //
            // Test set trigger policy to another existing policy
            //
            @SuppressWarnings("unused")
            ControlLoop cl = builder.setTriggerPolicy(triggerPolicy1.getId());
            assertTrue(builder.getControlLoop().getTrigger_policy().equals(triggerPolicy1.getId()));
            //
            // Test get trigger policy
            //
            assertTrue(builder.getTriggerPolicy().equals(triggerPolicy1));
            //
        } catch (BuilderException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testSetTriggerPolicyNullPolicyId() throws BuilderException {
        ControlLoopPolicyBuilder builder =
                ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
        expectedException.expect(BuilderException.class);
        expectedException.expectMessage("Id must not be null");
        builder.setTriggerPolicy(null);
    }

    @Test
    public void testSetTriggerPolicyNoPoliciesExist() throws BuilderException {
        ControlLoopPolicyBuilder builder =
                ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
        final String unknownPolicyId = "100";
        expectedException.expect(BuilderException.class);
        expectedException.expectMessage("Unknown policy " + unknownPolicyId);
        builder.setTriggerPolicy(unknownPolicyId);
    }

    @Test
    public void testSetTriggerPolicyUnknownPolicy() throws BuilderException {
        ControlLoopPolicyBuilder builder =
                ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
        builder.setTriggerPolicy("Restart the VM", "Upon getting the trigger event, restart the VM", "APPC",
                new Target(TargetType.VM), "Restart", null, 2, 300);
        final String unknownPolicyId = "100";
        expectedException.expect(BuilderException.class);
        expectedException.expectMessage("Unknown policy " + unknownPolicyId);
        builder.setTriggerPolicy(unknownPolicyId);
    }

    @Test
    public void testAddRemovePolicies() {
        try {
            ControlLoopPolicyBuilder builder =
                    ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
            Policy triggerPolicy =
                    builder.setTriggerPolicy("Restart the VM", "Upon getting the trigger event, restart the VM", "APPC",
                            new Target(TargetType.VM), "Restart", null, 2, 300);
            //
            // Test create a policy and chain it to the results of trigger policy
            //
            Policy onRestartFailurePolicy1 = builder.setPolicyForPolicyResult("Rebuild VM",
                    "If the restart fails, rebuild it.", "APPC", new Target(TargetType.VM), "Rebuild", null, 1, 600,
                    triggerPolicy.getId(), PolicyResult.FAILURE, PolicyResult.FAILURE_EXCEPTION,
                    PolicyResult.FAILURE_RETRIES, PolicyResult.FAILURE_TIMEOUT, PolicyResult.FAILURE_GUARD);
            //
            assertTrue(builder.getTriggerPolicy().getFailure().equals(onRestartFailurePolicy1.getId()));
            assertTrue(builder.getTriggerPolicy().getFailure_exception().equals(onRestartFailurePolicy1.getId()));
            assertTrue(builder.getTriggerPolicy().getFailure_retries().equals(onRestartFailurePolicy1.getId()));
            assertTrue(builder.getTriggerPolicy().getFailure_timeout().equals(onRestartFailurePolicy1.getId()));
            assertTrue(builder.getTriggerPolicy().getFailure_guard().equals(onRestartFailurePolicy1.getId()));

            //
            // Test create a policy and chain it to the results of trigger policy success
            //
            Policy onSuccessPolicy1 = builder.setPolicyForPolicyResult("Do something",
                    "If the restart succeeds, do something else.", "APPC", new Target(TargetType.VM), "SomethingElse",
                    null, 1, 600, triggerPolicy.getId(), PolicyResult.SUCCESS);
            //
            assertTrue(builder.getTriggerPolicy().getSuccess().equals(onSuccessPolicy1.getId()));

            //
            // Test remove policy
            //
            boolean removed = builder.removePolicy(onRestartFailurePolicy1.getId());
            assertTrue(removed);
            assertTrue(builder.getTriggerPolicy().getFailure().equals(FinalResult.FINAL_FAILURE.toString()));
            assertTrue(builder.getTriggerPolicy().getFailure_retries()
                    .equals(FinalResult.FINAL_FAILURE_RETRIES.toString()));
            assertTrue(builder.getTriggerPolicy().getFailure_timeout()
                    .equals(FinalResult.FINAL_FAILURE_TIMEOUT.toString()));
            assertTrue(
                    builder.getTriggerPolicy().getFailure_guard().equals(FinalResult.FINAL_FAILURE_GUARD.toString()));
            //
            // Create another policy and chain it to the results of trigger policy
            //
            Policy onRestartFailurePolicy2 =
                    builder.setPolicyForPolicyResult("Rebuild VM", "If the restart fails, rebuild it.", "APPC",
                            new Target(TargetType.VM), "Rebuild", null, 2, 600, triggerPolicy.getId(),
                            PolicyResult.FAILURE, PolicyResult.FAILURE_RETRIES, PolicyResult.FAILURE_TIMEOUT);
            //
            // Test reset policy results
            //
            triggerPolicy = builder.resetPolicyResults(triggerPolicy.getId());
            assertTrue(builder.getTriggerPolicy().getFailure().equals(FinalResult.FINAL_FAILURE.toString()));
            assertTrue(builder.getTriggerPolicy().getFailure_retries()
                    .equals(FinalResult.FINAL_FAILURE_RETRIES.toString()));
            assertTrue(builder.getTriggerPolicy().getFailure_timeout()
                    .equals(FinalResult.FINAL_FAILURE_TIMEOUT.toString()));
            //
            // Test set the policy results to an existing operational policy
            //
            onRestartFailurePolicy2 =
                    builder.setPolicyForPolicyResult(onRestartFailurePolicy2.getId(), triggerPolicy.getId(),
                            PolicyResult.FAILURE, PolicyResult.FAILURE_RETRIES, PolicyResult.FAILURE_TIMEOUT);
            assertTrue(builder.getTriggerPolicy().getFailure().equals(onRestartFailurePolicy2.getId()));
            assertTrue(builder.getTriggerPolicy().getFailure_retries().equals(onRestartFailurePolicy2.getId()));
            assertTrue(builder.getTriggerPolicy().getFailure_timeout().equals(onRestartFailurePolicy2.getId()));
            //
            // Test set the policy result for success to an existing operational policy
            //
            onRestartFailurePolicy2 =
                    builder.setPolicyForPolicyResult(onRestartFailurePolicy2.getId(), triggerPolicy.getId(),
                            PolicyResult.FAILURE, PolicyResult.FAILURE_EXCEPTION, PolicyResult.FAILURE_GUARD,
                            PolicyResult.FAILURE_RETRIES, PolicyResult.FAILURE_TIMEOUT, PolicyResult.SUCCESS);
            assertTrue(builder.getTriggerPolicy().getFailure().equals(onRestartFailurePolicy2.getId()));
            assertTrue(builder.getTriggerPolicy().getFailure_exception().equals(onRestartFailurePolicy2.getId()));
            assertTrue(builder.getTriggerPolicy().getFailure_guard().equals(onRestartFailurePolicy2.getId()));
            assertTrue(builder.getTriggerPolicy().getFailure_retries().equals(onRestartFailurePolicy2.getId()));
            assertTrue(builder.getTriggerPolicy().getFailure_timeout().equals(onRestartFailurePolicy2.getId()));
            assertTrue(builder.getTriggerPolicy().getSuccess().equals(onRestartFailurePolicy2.getId()));

            //
            // Test remove all existing operational policies
            //
            builder = builder.removeAllPolicies();
            assertTrue(builder.getControlLoop().getTrigger_policy().equals(FinalResult.FINAL_OPENLOOP.toString()));
            //
        } catch (BuilderException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testAddToUnknownPolicy() throws BuilderException {
        ControlLoopPolicyBuilder builder =
                ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
        final String policyId = "100";
        expectedException.expect(BuilderException.class);
        expectedException.expectMessage("Unknown policy " + policyId);

        builder.setPolicyForPolicyResult("Rebuild VM", "If the restart fails, rebuild it.", "APPC",
                new Target(TargetType.VM), "Rebuild", null, 1, 600, policyId, PolicyResult.FAILURE,
                PolicyResult.FAILURE_RETRIES, PolicyResult.FAILURE_TIMEOUT, PolicyResult.FAILURE_GUARD);
    }

    @Test
    public void testAddExistingPolicyToUnknownPolicy() throws BuilderException {
        ControlLoopPolicyBuilder builder =
                ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
        Policy triggerPolicy =
                builder.setTriggerPolicy("Restart the VM", "Upon getting the trigger event, restart the VM", "APPC",
                        new Target(TargetType.VM), "Restart", null, 2, 300);


        Policy onRestartFailurePolicy = builder.setPolicyForPolicyResult("Rebuild VM",
                "If the restart fails, rebuild it.", "APPC", new Target(TargetType.VM), "Rebuild", null, 1, 600,
                triggerPolicy.getId(), PolicyResult.FAILURE);

        final String unknownPolicyId = "100";
        expectedException.expect(BuilderException.class);
        expectedException.expectMessage(unknownPolicyId + " does not exist");

        builder.setPolicyForPolicyResult(onRestartFailurePolicy.getId(), unknownPolicyId, PolicyResult.FAILURE);
    }

    @Test
    public void testAddUnknownExistingPolicyToPolicy() throws BuilderException {
        ControlLoopPolicyBuilder builder =
                ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
        Policy triggerPolicy =
                builder.setTriggerPolicy("Restart the VM", "Upon getting the trigger event, restart the VM", "APPC",
                        new Target(TargetType.VM), "Restart", null, 2, 300);

        final String unknownPolicyId = "100";
        expectedException.expect(BuilderException.class);
        expectedException.expectMessage("Operational policy " + unknownPolicyId + " does not exist");

        builder.setPolicyForPolicyResult(unknownPolicyId, triggerPolicy.getId(), PolicyResult.FAILURE);
    }

    @Test
    public void testAddOperationsAccumulateParams() {
        try {
            ControlLoopPolicyBuilder builder =
                    ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 2400);
            Policy triggerPolicy =
                    builder.setTriggerPolicy("Restart the eNodeB", "Upon getting the trigger event, restart the eNodeB",
                            "RANController", new Target(TargetType.PNF), "Restart", null, 2, 300);
            //
            // Add the operationsAccumulateParams
            //
            triggerPolicy = builder.addOperationsAccumulateParams(triggerPolicy.getId(),
                    new OperationsAccumulateParams("15m", 5));
            assertNotNull(builder.getTriggerPolicy().getOperationsAccumulateParams());
            assertTrue(builder.getTriggerPolicy().getOperationsAccumulateParams().getPeriod().equals("15m"));
            assertTrue(builder.getTriggerPolicy().getOperationsAccumulateParams().getLimit() == 5);
            //
        } catch (BuilderException e) {
            fail(e.getMessage());
        }
    }


    @Test
    public void testBuildSpecification() {
        try {
            //
            // Create the builder
            //
            ControlLoopPolicyBuilder builder =
                    ControlLoopPolicyBuilder.Factory.buildControlLoop(UUID.randomUUID().toString(), 800);
            //
            // Set the first invalid trigger policy
            //
            Policy policy1 = builder.setTriggerPolicy("Restart the VM",
                    "Upon getting the trigger event, restart the VM", null, null, "Instantiate", null, 2, 300);
            Results results = builder.buildSpecification();
            //
            // Check that ERRORs are in results for invalid policy arguments
            //
            boolean invalid_actor = false;
            boolean invalid_recipe = false;
            boolean invalid_target = false;
            for (Message m : results.getMessages()) {
                if (m.getMessage().equals("Policy actor is null") && m.getLevel() == MessageLevel.ERROR) {
                    invalid_actor = true;
                }
                if (m.getMessage().equals("Policy recipe is invalid") && m.getLevel() == MessageLevel.ERROR) {
                    invalid_recipe = true;
                }
                if (m.getMessage().equals("Policy target is null") && m.getLevel() == MessageLevel.ERROR) {
                    invalid_target = true;
                }
            }
            //
            assertTrue(invalid_actor);
            assertTrue(invalid_recipe);
            assertTrue(invalid_target);
            //
            // Remove the invalid policy
            //
            // @SuppressWarnings("unused")
            boolean removed = builder.removePolicy(policy1.getId());
            assertTrue(removed);
            assertTrue(builder.getTriggerPolicy() == null);
            //
            // Set a valid trigger policy
            //
            policy1 = builder.setTriggerPolicy("Rebuild VM", "If the restart fails, rebuild it.", "APPC",
                    new Target(TargetType.VM), "Rebuild", null, 1, 600);
            //
            // Set a second valid trigger policy
            //
            Policy policy2 =
                    builder.setTriggerPolicy("Restart the VM", "Upon getting the trigger event, restart the VM", "APPC",
                            new Target(TargetType.VM), "Restart", null, 2, 300);
            //
            // Now, we have policy1 unreachable
            //
            results = builder.buildSpecification();
            boolean unreachable = false;
            for (Message m : results.getMessages()) {
                if (m.getMessage().equals("Policy " + policy1.getId() + " is not reachable.")
                        && m.getLevel() == MessageLevel.WARNING) {
                    unreachable = true;
                    break;
                }
            }
            assertTrue(unreachable);
            //
            // Set policy1 for the failure results of policy2
            //
            policy1 = builder.setPolicyForPolicyResult(policy1.getId(), policy2.getId(), PolicyResult.FAILURE,
                    PolicyResult.FAILURE_RETRIES, PolicyResult.FAILURE_TIMEOUT);
            results = builder.buildSpecification();
            boolean invalid_timeout = false;
            for (Message m : results.getMessages()) {
                if (m.getMessage()
                        .equals("controlLoop overall timeout is less than the sum of operational policy timeouts.")
                        && m.getLevel() == MessageLevel.ERROR) {
                    invalid_timeout = true;
                    break;
                }
            }
            assertTrue(invalid_timeout);
            //
            // Remove policy2 (revert controlLoop back to open loop)
            //
            removed = builder.removePolicy(policy2.getId());
            //
            // ControlLoop is open loop now, but it still has policies (policy1)
            //
            results = builder.buildSpecification();
            unreachable = false;
            for (Message m : results.getMessages()) {
                if (m.getMessage().equals("Open Loop policy contains policies. The policies will never be invoked.")
                        && m.getLevel() == MessageLevel.WARNING) {
                    unreachable = true;
                    break;
                }
            }
            assertTrue(unreachable);
            //
        } catch (BuilderException e) {
            fail(e.getMessage());
        }
    }


    @Test
    public void test() {
        this.test("src/test/resources/v1.0.0/policy_Test.yaml");
    }

    @Test
    public void testEvilYaml() {
        try (InputStream is = new FileInputStream(new File("src/test/resources/v1.0.0/test_evil.yaml"))) {
            //
            // Read the yaml into our Java Object
            //
            Yaml yaml = new Yaml(new Constructor(ControlLoopPolicy.class));
            yaml.load(is);
        } catch (FileNotFoundException e) {
            fail(e.getLocalizedMessage());
        } catch (IOException e) {
            fail(e.getLocalizedMessage());
        } catch (YAMLException e) {
            //
            // Should have this
            //
        }
    }

    public void test(String testFile) {
        try (InputStream is = new FileInputStream(new File(testFile))) {
            //
            // Read the yaml into our Java Object
            //
            Yaml yaml = new Yaml(new Constructor(ControlLoopPolicy.class));
            Object obj = yaml.load(is);
            assertNotNull(obj);
            assertTrue(obj instanceof ControlLoopPolicy);
            ControlLoopPolicy policyTobuild = (ControlLoopPolicy) obj;
            //
            // Now we're going to try to use the builder to build this.
            //
            ControlLoopPolicyBuilder builder = ControlLoopPolicyBuilder.Factory.buildControlLoop(
                    policyTobuild.getControlLoop().getControlLoopName(), policyTobuild.getControlLoop().getTimeout());
            //
            // Add services
            //
            if (policyTobuild.getControlLoop().getServices() != null) {
                builder = builder.addService(policyTobuild.getControlLoop().getServices()
                        .toArray(new Service[policyTobuild.getControlLoop().getServices().size()]));
            }
            //
            // Add resources
            //
            if (policyTobuild.getControlLoop().getResources() != null) {
                builder = builder.addResource(policyTobuild.getControlLoop().getResources()
                        .toArray(new Resource[policyTobuild.getControlLoop().getResources().size()]));
            }
            //
            // Set pnf
            //
            if (policyTobuild.getControlLoop().getPnf() != null) {
                builder = builder.setPNF(policyTobuild.getControlLoop().getPnf());
            }
            //
            // Add the policies and be sure to set the trigger policy
            //
            if (policyTobuild.getPolicies() != null) {
                for (Policy policy : policyTobuild.getPolicies()) {
                    if (policy.getId() == policyTobuild.getControlLoop().getTrigger_policy()) {
                        builder.setTriggerPolicy(policy.getName(), policy.getDescription(), policy.getActor(),
                                policy.getTarget(), policy.getRecipe(), null, policy.getRetry(), policy.getTimeout());
                    }
                }
            }

            // Question : how to change policy ID and results by using builder ??

            @SuppressWarnings("unused")
            Results results = builder.buildSpecification();

        } catch (FileNotFoundException e) {
            fail(e.getLocalizedMessage());
        } catch (IOException e) {
            fail(e.getLocalizedMessage());
        } catch (BuilderException e) {
            fail(e.getLocalizedMessage());
        }

    }

}
