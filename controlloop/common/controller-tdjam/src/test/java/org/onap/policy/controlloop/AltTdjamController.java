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

package org.onap.policy.controlloop;

import static org.onap.policy.drools.properties.DroolsPropertyConstants.PROPERTY_CONTROLLER_TYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kie.api.definition.rule.Rule;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;
import org.kie.api.runtime.KieRuntime;
import org.kie.api.runtime.rule.FactHandle;
import org.onap.policy.controlloop.tdjam.TdjamController;
import org.onap.policy.drools.controller.DroolsController;
import org.onap.policy.drools.features.DroolsControllerFeatureApi;
import org.onap.policy.drools.features.PolicyControllerFeatureApi;
import org.onap.policy.drools.protocol.coders.TopicCoderFilterConfiguration;
import org.onap.policy.drools.system.PolicyController;

/**
 * The class 'NonDroolsPolicyController' (superclass to 'TdjamController')
 * includes the 'DroolsController' interface -- it responds to Drools
 * memory queries as if the memory was always empty. The existing Junit tests
 * include queries to Drools memory, which would fail without providing a
 * more complete simulation. This class adds that more complete simulation of
 * Drools memory.
 */
public class AltTdjamController extends TdjamController {
    // list of objects explicitly deleted from simulated Drools memory
    private Set<Object> deletedObjects = Collections.newSetFromMap(new IdentityHashMap<>());

    // we need to send out 'RuleRuntimeEventListener' notifications
    private HashSet<RuleRuntimeEventListener> listeners = new HashSet<>();

    public AltTdjamController(String name, Properties properties) {
        super(name, properties);
    }

    private Set<Object> getObjects() {
        // Simulate set of objects in Drools memory -- it includes all
        // ToscaPolicies and ControlLoopParams, except for objects that
        // have been explicitly deleted.
        Set<Object> rval = Collections.newSetFromMap(new IdentityHashMap<>());
        rval.addAll(getAllToscaPolicies());
        rval.addAll(getAllControlLoopParams());
        rval.removeAll(deletedObjects);

        return rval;
    }

    @Override
    public Map<String, Integer> factClassNames(String sessionName) {
        // we don't need to implement this one -- it isn't called
        return new HashMap<>();
    }

    @Override
    public long factCount(String sessionName) {
        long rval = getObjects().size();
        return rval;
    }

    @Override
    public List<Object> facts(String sessionName, String className, boolean delete) {
        return new ArrayList<>(getObjects());
    }

    @Override
    public <T> List<T> facts(@NonNull String sessionName, @NonNull Class<T> clazz) {
        List<T> factList = new ArrayList<>();
        for (Object obj : getObjects()) {
            if (clazz.isInstance(obj)) {
                factList.add(clazz.cast(obj));
            }
        }
        return factList;
    }

    @Override
    public <T> boolean delete(@NonNull String sessionName, @NonNull T fact) {
        return delete(fact);
    }

    @Override
    public <T> boolean delete(@NonNull T fact) {
        if (getObjects().contains(fact)) {
            // simulate deletion by adding the object to the list of
            // those deleted (they aren't really removed from the tables
            // in 'TdjamController')
            deletedObjects.add(fact);
            return true;
        }
        return false;
    }

    @Override
    public <T> boolean delete(@NonNull String sessionName, @NonNull Class<T> fact) {
        return delete(fact);
    }

    @Override
    public <T> boolean delete(@NonNull Class<T> fact) {
        boolean rval = false;
        for (Object obj : getObjects()) {
            if (fact.isInstance(obj)) {
                // simulate deletion by adding the object to the list of
                // those deleted (they aren't really removed from the tables
                // in 'TdjamController')
                deletedObjects.add(obj);
                rval = true;
            }
        }
        return rval;
    }

    @Override
    public <T> boolean offer(T event) {
        Set<Object> before = getObjects();
        super.offer(event);
        Set<Object> changes = getObjects();
        changes.removeAll(before);

        // simulate 'RuleRuntimeEventListener' notification for all additions
        // (the tests don't care about deletion notifications)

        final Rule rule = new Rule() {
                public String getId() {
                    return null;
                }

                public KnowledgeType getKnowledgeType() {
                    return KnowledgeType.RULE;
                }

                public String getNamespace() {
                    return null;
                }

                public Map<String, Object> getMetaData() {
                    return new HashMap<>();
                }

                public String getName() {
                    return "Simulated KieSession -- no Rules";
                }

                public String getPackageName() {
                    return "Simulated KieSession -- no package name";
                }
            };

        for (final Object obj : changes) {
            ObjectInsertedEvent ois = new ObjectInsertedEvent() {
                    public KieRuntime getKieRuntime() {
                        return null;
                    }

                    public FactHandle getFactHandle() {
                        return null;
                    }

                    public Object getObject() {
                        return obj;
                    }

                    public Rule getRule() {
                        return rule;
                    }
                };
            for (RuleRuntimeEventListener listener : new HashSet<RuleRuntimeEventListener>(listeners)) {
                listener.objectInserted(ois);
            }
        }
        return false;
    }

    public void addEventListener(RuleRuntimeEventListener listener) {
        // we need to keep track of active 'RuleRuntimeEventListener' instances
        listeners.add(listener);
    }

    public void removeEventListener(RuleRuntimeEventListener listener) {
        // we need to keep track of active 'RuleRuntimeEventListener' instances
        listeners.remove(listener);
    }

    /* ============================================================ */

    public static class PolicyBuilder implements PolicyControllerFeatureApi {
        @Override
        public int getSequenceNumber() {
            return 1;
        }

        @Override
        public PolicyController beforeInstance(String name, Properties properties) {
            if ("alt".equals(properties.getProperty(PROPERTY_CONTROLLER_TYPE))) {
                return new AltTdjamController(name, properties);
            }
            return null;
        }
    }

    /* ============================================================ */

    public static class DroolsBuilder implements DroolsControllerFeatureApi {
        @Override
        public int getSequenceNumber() {
            return 1;
        }

        @Override
        public DroolsController beforeInstance(Properties properties,
                                      String groupId, String artifactId, String version,
                                      List<TopicCoderFilterConfiguration> decoderConfigurations,
                                      List<TopicCoderFilterConfiguration> encoderConfigurations) {

            if ("alt".equals(properties.getProperty(PROPERTY_CONTROLLER_TYPE))) {
                return AltTdjamController.getBuildInProgress();
            }
            return null;
        }
    }
}
