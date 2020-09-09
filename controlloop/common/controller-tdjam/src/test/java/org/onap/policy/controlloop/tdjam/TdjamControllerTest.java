/*-
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

package org.onap.policy.controlloop.tdjam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onap.policy.drools.properties.DroolsPropertyConstants.PROPERTY_CONTROLLER_TYPE;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.HashSet;
import java.util.Properties;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.controlloop.CanonicalOnset;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.drools.controller.DroolsControllerConstants;
import org.onap.policy.drools.system.PolicyControllerConstants;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.drools.utils.PropertyUtil;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.powermock.reflect.Whitebox;
import org.slf4j.LoggerFactory;

public class TdjamControllerTest {
    private static Properties prop;
    private static Logger logger = (Logger) LoggerFactory.getLogger(TdjamController.class);
    private static ListAppender<ILoggingEvent> appender = new ListAppender<ILoggingEvent>();

    /**
     * Setup appender, and initialize properties.
     */
    @BeforeClass
    public static void setupClass() throws Exception {
        logger.setLevel(Level.DEBUG);
        logger.addAppender(appender);

        prop = PropertyUtil.getProperties("src/test/resources/config/tdjam-controller.properties");
        prop.setProperty(PROPERTY_CONTROLLER_TYPE, "tdjam");

        PolicyEngineConstants.getManager().configure(new Properties());
        PolicyEngineConstants.getManager().start();

    }

    /**
     * Remove appender.
     */
    @AfterClass
    public static void cleanupClass() {

        PolicyEngineConstants.getManager().stop();
        PolicyEngineConstants.getManager().getExecutorService().shutdown();

        appender.stop();
        System.out.println("APPENDER:");
        for (ILoggingEvent event : appender.list) {
            System.out.println("    " + event);
        }
        logger.detachAppender(appender);
    }

    @Test
    public void toscaPolicyTests() {
        TdjamController tc = (TdjamController) PolicyControllerConstants.getFactory().build("tc", prop);
        assertTrue(PolicyControllerConstants.getFactory().inventory().contains(tc));
        assertTrue(DroolsControllerConstants.getFactory().inventory().contains(tc));

        final HashSet<ToscaPolicy> toscaPolicies = new HashSet<>();
        final HashSet<ControlLoopParams> controlLoopParams = new HashSet<>();

        ToscaPolicy a1 = buildToscaPolicy("a", "1", tc);
        ToscaPolicy a2 = buildToscaPolicy("a", "2", tc);
        ToscaPolicy b1 = buildToscaPolicy("b", "1", tc);

        toscaPolicies.add(a1);
        toscaPolicies.add(a2);
        toscaPolicies.add(b1);

        assertSame(a1, tc.getToscaPolicy("a", "1"));
        assertSame(a2, tc.getToscaPolicy("a", "2"));
        assertSame(b1, tc.getToscaPolicy("b", "1"));
        assertEquals(toscaPolicies, tc.getAllToscaPolicies());

        // create associated ControlLoopParams
        final ControlLoopParams clpa1 = buildControlLoopParams("a", "1", "clpa1", tc);
        final ControlLoopParams clpa2 = buildControlLoopParams("a", "2", "clpa2", tc);
        final ControlLoopParams clpb1 = buildControlLoopParams("b", "1", "clpb1", tc);
        final ControlLoopParams clpb3 = buildControlLoopParams("b", "3", "clpb3", null);

        // the add for 'clpb3' should fail, because there is no ToscaPolicy
        startLog();
        assertSame(clpb3, tc.addControlLoopParams(clpb3));
        stopLog();
        assertLog(".*Missing ToscaPolicy, name=b, version=3.*");
        assertNull(tc.removeControlLoopParams("clpb3"));

        controlLoopParams.add(clpa1);
        controlLoopParams.add(clpa2);
        controlLoopParams.add(clpb1);
        assertEquals(controlLoopParams, new HashSet<>(tc.getAllControlLoopParams()));

        // manually remove a ControlLoopParams
        assertSame(clpa1, tc.removeControlLoopParams("clpa1"));
        assertTrue(controlLoopParams.remove(clpa1));
        assertEquals(controlLoopParams, new HashSet<>(tc.getAllControlLoopParams()));

        // tests of nonexistent policies
        assertNull(tc.getToscaPolicy("c", "1")); // non-existent name
        assertNull(tc.removeToscaPolicy("c", "1"));
        assertNull(tc.getToscaPolicy("b", "3")); // non-existent version
        assertNull(tc.removeToscaPolicy("b", "3"));

        assertSame(a1, tc.removeToscaPolicy("a", "1"));
        assertTrue(toscaPolicies.remove(a1));
        assertEquals(toscaPolicies, tc.getAllToscaPolicies());
        assertSame(a2, tc.removeToscaPolicy("a", "2"));
        assertTrue(toscaPolicies.remove(a2));
        assertEquals(toscaPolicies, tc.getAllToscaPolicies());

        // ControlLoopParams removal should be automatic
        assertTrue(controlLoopParams.remove(clpa2));
        assertEquals(controlLoopParams, new HashSet<>(tc.getAllControlLoopParams()));

        // test reset method
        tc.reset();
        assertTrue(tc.getAllToscaPolicies().isEmpty());
        assertTrue(tc.getAllControlLoopParams().isEmpty());
        assertTrue(tc.getAllEventManagers().isEmpty());
        assertTrue(tc.getAllOnsetToEventManager().isEmpty());

        PolicyControllerConstants.getFactory().shutdown(tc);
        assertFalse(PolicyControllerConstants.getFactory().inventory().contains(tc));
        assertFalse(DroolsControllerConstants.getFactory().inventory().contains(tc));
    }

    @Test
    public void onsetErrors() throws Exception {
        TdjamController tc = (TdjamController) PolicyControllerConstants.getFactory().build("tc", prop);
        assertTrue(PolicyControllerConstants.getFactory().inventory().contains(tc));
        assertTrue(DroolsControllerConstants.getFactory().inventory().contains(tc));
        tc.start();

        buildToscaPolicy("a", "1", tc);
        final ControlLoopParams clpa1 = buildControlLoopParams("a", "1", "clpa1", tc);
        assertTrue(tc.getAllControlLoopParams().contains(clpa1));

        CanonicalOnset canonicalOnset = new CanonicalOnset();
        startLog();
        Whitebox.invokeMethod(tc, "processEvent", canonicalOnset);
        stopLog();
        assertLog(".*No ControlLoopParams for event: CanonicalOnset.*");

        // set Name with new canonicalOnset
        CanonicalOnset canonicalOnset2 = new CanonicalOnset();
        canonicalOnset2.setClosedLoopControlName("clpa1");
        // try with a non-null requestID, but missing target
        canonicalOnset2.setRequestId(UUID.randomUUID());
        startLog();
        Whitebox.invokeMethod(tc, "processEvent", canonicalOnset2);
        stopLog();
        assertLog(".*Exception creating ControlLoopEventManager.*");

        PolicyControllerConstants.getFactory().shutdown(tc);
        assertFalse(PolicyControllerConstants.getFactory().inventory().contains(tc));
    }

    private ToscaPolicy buildToscaPolicy(String name, String version, TdjamController tc) {
        ToscaPolicy tp = new ToscaPolicy();
        tp.setName(name);
        tp.setVersion(version);

        if (tc != null) {
            tc.addToscaPolicy(tp);
        }
        return tp;
    }

    private ControlLoopParams buildControlLoopParams(String name, String version,
        String closedLoopControlName, TdjamController tc) {

        ControlLoopParams clp = new ControlLoopParams();
        clp.setPolicyName(name);
        clp.setPolicyVersion(version);
        clp.setClosedLoopControlName(closedLoopControlName);

        if (tc != null) {
            assertTrue(tc.addControlLoopParams(clp) != clp);
        }

        return clp;
    }

    private void startLog() {
        appender.list.clear();
        appender.start();
    }

    private void stopLog() {
        appender.stop();
    }

    private void assertLog(String regexp) {
        for (ILoggingEvent event : appender.list) {
            if (event.toString().matches(regexp)) {
                return;
            }
        }
        fail("Missing log entry: " + regexp);
    }
}
