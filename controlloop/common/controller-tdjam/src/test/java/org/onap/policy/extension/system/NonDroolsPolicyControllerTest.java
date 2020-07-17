/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.extension.system;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.onap.policy.drools.properties.DroolsPropertyConstants.PROPERTY_CONTROLLER_TYPE;

import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.drools.controller.DroolsController;
import org.onap.policy.drools.features.DroolsControllerFeatureApi;
import org.onap.policy.drools.features.PolicyControllerFeatureApi;
import org.onap.policy.drools.protocol.coders.TopicCoderFilterConfiguration;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.drools.system.PolicyControllerConstants;
import org.onap.policy.drools.utils.PropertyUtil;

public class NonDroolsPolicyControllerTest {
    //public static boolean loop = true;
    private static Properties prop;

    @BeforeClass
    public static void setupClass() throws Exception {
        prop = PropertyUtil.getProperties("src/test/resources/config/frankfurt-controller.properties");
    }

    @Test
    public void testState() {
        NonDroolsPolicyController controller = buildController("tdjam");

        assertEquals("nondrools", controller.getName());
        assertEquals("NonDroolsPolicyController", controller.getGroupId());
        assertEquals("nondrools", controller.getArtifactId());
        assertEquals("1.0", controller.getVersion());
        assertTrue(controller.isBrained());

        assertFalse(controller.isAlive());
        assertFalse(controller.isLocked());

        // first 'start()'
        controller.start();
        assertTrue(controller.isAlive());
        assertFalse(controller.isLocked());

        // second 'start()'
        controller.start();
        assertTrue(controller.isAlive());
        assertFalse(controller.isLocked());

        // test a few stubbed-off methods
        assertTrue(controller.getSessionNames().isEmpty());
        assertTrue(controller.getCanonicalSessionNames().isEmpty());
        assertTrue(controller.getBaseDomainNames().isEmpty());
        assertFalse(controller.offer("topic", "event"));
        assertFalse(controller.offer("event"));
        assertEquals(0, controller.getRecentSourceEvents().length);
        assertEquals(0, controller.getRecentSinkEvents().length);
        assertNull(controller.getContainer());
        assertThatIllegalArgumentException().isThrownBy(
            () -> controller.fetchModelClass("NoSuchClass"));
        assertThatIllegalArgumentException().isThrownBy(
            () -> controller.updateToVersion(null, null, null, null, null));
        assertTrue(controller.factClassNames(null).isEmpty());
        assertEquals(0, controller.factCount(null));
        assertTrue(controller.facts(null, null, false).isEmpty());
        assertTrue(controller.facts("sessionName", String.class).isEmpty());
        assertTrue(controller.factQuery(null, null, null, false).isEmpty());
        assertFalse(controller.delete("sessionName", "fact"));
        assertFalse(controller.delete("fact"));
        assertFalse(controller.delete("sessionName", String.class));
        assertFalse(controller.delete(String.class));

        controller.lock();
        assertTrue(controller.isAlive());
        assertTrue(controller.isLocked());

        controller.stop();
        assertFalse(controller.isAlive());
        assertTrue(controller.isLocked());

        controller.unlock();
        assertFalse(controller.isAlive());
        assertFalse(controller.isLocked());

        destroy(controller);
    }

    @Test
    public void deliverTest() {
        DroolsControllerFeatureHandler.resetStats();
        final NonDroolsPolicyController controller = buildController("tdjam");

        final TopicSink topicSink = mock(TopicSink.class);
        when(topicSink.getTopic()).thenReturn("POLICY-CL-MGT");
        when(topicSink.send(any())).thenReturn(false);

        final VirtualControlLoopNotification msg = new VirtualControlLoopNotification(null);

        controller.lock();

        // invalid sink
        try {
            controller.deliver(null, null);
            fail("Expected IllegalArgumentException did not occur");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage(),
                       ex.getMessage().endsWith(" invalid sink"));
        }

        // invalid event
        try {
            controller.deliver(topicSink, null);
            fail("Expected IllegalArgumentException did not occur");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage(),
                       ex.getMessage().endsWith(" invalid event"));
        }

        // is locked
        try {
            controller.deliver(topicSink, "event");
            fail("Expected IllegalStateException did not occur");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage(),
                       ex.getMessage().endsWith(" is locked"));
        }
        controller.unlock();

        // is stopped
        try {
            controller.deliver(topicSink, "event");
            fail("Expected IllegalStateException did not occur");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage(),
                       ex.getMessage().endsWith(" is stopped"));
        }

        // there should have been 4 'beforeDeliver' calls up to this point
        assertEquals(4, DroolsControllerFeatureHandler.beforeDeliverFalse);

        Function<String, Boolean> signal = (sig) -> {
            msg.getAai().put("signal", sig);
            return controller.deliver(topicSink, msg);
        };

        controller.start();

        // 'beforeDeliver' intercepts
        DroolsControllerFeatureHandler.resetStats();

        assertTrue(signal.apply("beforeDeliverTrue"));
        assertEquals(1, DroolsControllerFeatureHandler.beforeDeliverTrue);
        assertEquals(0, DroolsControllerFeatureHandler.afterDeliverFalse);
        
        assertFalse(signal.apply("beforeDeliverException"));
        assertEquals(1, DroolsControllerFeatureHandler.beforeDeliverException);
        assertEquals(1, DroolsControllerFeatureHandler.afterDeliverFalse);
        // it would be nice to check the log message at this point

        // 'afterDeliver' intercepts
        DroolsControllerFeatureHandler.resetStats();

        assertTrue(signal.apply("afterDeliverTrue"));
        assertEquals(1, DroolsControllerFeatureHandler.afterDeliverTrue);

        assertFalse(signal.apply("afterDeliverException"));
        assertEquals(1, DroolsControllerFeatureHandler.afterDeliverException);

        assertFalse(signal.apply("nothing in particular"));
        assertEquals(1, DroolsControllerFeatureHandler.afterDeliverFalse);

        destroy(controller);
    }

    private NonDroolsPolicyController buildController(String type) {
        prop.setProperty(PROPERTY_CONTROLLER_TYPE, type);
        PolicyController controller =
            PolicyControllerConstants.getFactory().build("nondrools", prop);
        assertTrue(controller instanceof NonDroolsPolicyController);
        return (NonDroolsPolicyController) controller;
    }

    private void destroy(PolicyController controller) {
        String name = controller.getName();
        assertSame(controller, PolicyControllerConstants.getFactory().get(name));
        PolicyControllerConstants.getFactory().destroy(controller);
        assertThatIllegalArgumentException().isThrownBy(
            () -> PolicyControllerConstants.getFactory().get(name));
    }

    /* ============================================================ */

    /**
     * An instance of this class is called by 'IndexedPolicyControllerFactory'.
     * It does the build operation when the value of the 'controller.type'
     * property matches the value of TDJAM_CONTROLLER_BUILDER_TAG.
     */
    public static class PolicyBuilder implements PolicyControllerFeatureApi {
        @Override
        public int getSequenceNumber() {
            return 1;
        }

        @Override
        public PolicyController beforeInstance(String name, Properties properties) {
            if ("nondrools".equals(properties.getProperty(PROPERTY_CONTROLLER_TYPE))) {
                return new NonDroolsPolicyController(name, properties);
            }
            return null;
        }
    }

    /* ============================================================ */

    /**
     * An instance of this class is called by 'IndexedDroolsControllerFactory'.
     * It does the build operation when the value of the 'controller.type'
     * property matches the value of TDJAM_CONTROLLER_BUILDER_TAG.
     */
    public static class DroolsBuilder implements DroolsControllerFeatureApi {
        @Override
        public int getSequenceNumber() {
            return 1;
        }

        @Override
        public DroolsController beforeInstance(Properties properties,
                                      String groupId, String artifactId, String version,
                                      List<TopicCoderFilterConfiguration> decoderConfigurations,
                                      List<TopicCoderFilterConfiguration> encoderConfigurations) throws LinkageError {

            if ("nondrools".equals(properties.getProperty(PROPERTY_CONTROLLER_TYPE))) {
                return NonDroolsPolicyController.getBuildInProgress();
            }
            return null;
        }
    }

    /* ============================================================ */

    public static class DroolsControllerFeatureHandler implements DroolsControllerFeatureApi {
        static int beforeDeliverFalse = 0;
        static int beforeDeliverTrue = 0;
        static int beforeDeliverException = 0;
        static int afterDeliverFalse = 0;
        static int afterDeliverTrue = 0;
        static int afterDeliverException = 0;

        private static void resetStats() {
            beforeDeliverFalse = 0;
            beforeDeliverTrue = 0;
            beforeDeliverException = 0;
            afterDeliverFalse = 0;
            afterDeliverTrue = 0;
            afterDeliverException = 0;
        }

        @Override
        public int getSequenceNumber() {
            return 1;
        }

        @Override
        public boolean beforeDeliver(DroolsController controller, TopicSink sink, Object fact) {
            if (fact instanceof VirtualControlLoopNotification) {
                String factString = ((VirtualControlLoopNotification) fact).getAai().get("signal");
                if (factString == null) {
                    // this hook is run during 'FrankfurtTest' as well
                    return false;
                }
                if (factString.contains("beforeDeliverTrue")) {
                    beforeDeliverTrue += 1;
                    return true;
                }
                if (factString.contains("beforeDeliverException")) {
                    beforeDeliverException += 1;
                    RuntimeException ex = new RuntimeException("beforeDeliver");
                    ex.printStackTrace();
                    throw ex;
                }
            }
            beforeDeliverFalse += 1;
            return false;
        }


        @Override
            public boolean afterDeliver(DroolsController controller, TopicSink sink, Object fact,
                                        String json, boolean success) {

            if (fact instanceof VirtualControlLoopNotification) {
                String factString = ((VirtualControlLoopNotification) fact).getAai().get("signal");
                if (factString == null) {
                    // this hook is run during 'FrankfurtTest' as well
                    return false;
                }
                if (factString.contains("afterDeliverTrue")) {
                    afterDeliverTrue += 1;
                    return true;
                }
                if (factString.contains("afterDeliverException")) {
                    afterDeliverException += 1;
                    throw new RuntimeException("afterDeliver");
                }
            }
            afterDeliverFalse += 1;
            return false;
        }
    }
}
