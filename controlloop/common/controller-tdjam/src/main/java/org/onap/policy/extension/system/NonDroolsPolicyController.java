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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.onap.policy.common.endpoints.event.comm.Topic;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.event.comm.TopicSource;
import org.onap.policy.common.endpoints.properties.PolicyEndPointProperties;
import org.onap.policy.common.utils.services.OrderedServiceImpl;
import org.onap.policy.drools.controller.DroolsController;
import org.onap.policy.drools.controller.DroolsControllerConstants;
import org.onap.policy.drools.core.PolicyContainer;
import org.onap.policy.drools.features.DroolsControllerFeatureApi;
import org.onap.policy.drools.features.DroolsControllerFeatureApiConstants;
import org.onap.policy.drools.protocol.coders.EventProtocolCoder;
import org.onap.policy.drools.protocol.coders.EventProtocolCoderConstants;
import org.onap.policy.drools.protocol.coders.EventProtocolParams;
import org.onap.policy.drools.protocol.coders.JsonProtocolFilter;
import org.onap.policy.drools.protocol.coders.TopicCoderFilterConfiguration;
import org.onap.policy.drools.protocol.coders.TopicCoderFilterConfiguration.CustomGsonCoder;
import org.onap.policy.drools.protocol.coders.TopicCoderFilterConfiguration.PotentialCoderFilter;
import org.onap.policy.drools.system.internal.AggregatedPolicyController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class combines the 'PolicyController' and 'DroolsController'
 * interfaces, and provides a controller that does not have Drools running
 * underneath. It also contains some code copied from 'MavenDroolsController'
 * and 'NullDroolsController'. The goal is to have it look like other
 * controllers, use the same style property file, and provide access to
 * UEB/DMAAP message streams associated with the controller.
 */
public class NonDroolsPolicyController extends AggregatedPolicyController implements DroolsController {
    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(NonDroolsPolicyController.class);

    /**
     * The PolicyController and DroolsController factories assume that the
     * controllers are separate objects, but in this case, the same object
     * is used for both. We want the DroolsController 'build' method to
     * return the same object; however, at the point the DroolsController
     * build is taking place, the PolicyController hasn't yet been placed
     * in any tables. The following variable is used to pass this information
     * from one stack frame to another within the same thread.
     */
    private static ThreadLocal<NonDroolsPolicyController> buildInProgress = new ThreadLocal<>();

    /**
     * alive status of this drools controller,
     * reflects invocation of start()/stop() only.
     */
    protected volatile boolean alive = false;

    /**
     * locked status of this drools controller,
     * reflects if i/o drools related operations are permitted,
     * more specifically: offer() and deliver().
     * It does not affect the ability to start and stop
     * underlying drools infrastructure
     */
    protected volatile boolean locked = false;

    /**
     * list of topics, each with associated decoder classes, each
     * with a list of associated filters.
     */
    protected List<TopicCoderFilterConfiguration> decoderConfigurations;

    /**
     * list of topics, each with associated encoder classes, each
     * with a list of associated filters.
     */
    protected List<TopicCoderFilterConfiguration> encoderConfigurations;

    /**
     * recent sink events processed.
     */
    protected final CircularFifoQueue<String> recentSinkEvents = new CircularFifoQueue<>(10);

    // this is used to avoid infinite recursion in a shutdown or halt operation
    private boolean shutdownInProgress = false;

    private static Properties convert(String name, Properties properties) {

        Properties newProperties = new Properties();
        for (String pname : properties.stringPropertyNames()) {
            newProperties.setProperty(pname, properties.getProperty(pname));
        }

        newProperties.setProperty("rules.groupId", "NonDroolsPolicyController");
        newProperties.setProperty("rules.artifactId", name);
        newProperties.setProperty("rules.version", "1.0");
        return newProperties;
    }

    /**
     * constructor -- pass parameters to superclass.
     * @param name controller name
     * @param properties contents of controller properties file
     */
    public NonDroolsPolicyController(String name, Properties properties) {
        super(name, convert(name, properties));
    }

    /**
     * This is used to pass the 'NonDroolsPolicyController' object to the
     * 'DroolsPolicyBuilder' object, as the same object is used for both
     * 'PolicyController' and 'DroolsController'.
     *
     * @return the NonDroolsPolicyController object ('null' if not available)
     */
    public static NonDroolsPolicyController getBuildInProgress() {
        return buildInProgress.get();
    }

    protected void initDrools(Properties properties) {
        try {
            // Register with drools factory
            buildInProgress.set(this);
            this.droolsController.set(getDroolsFactory().build(properties, sources, sinks));
            buildInProgress.set(null);
        } catch (Exception | LinkageError e) {
            logger.error("{}: cannot init-drools", this);
            throw new IllegalArgumentException(e);
        }

        decoderConfigurations = codersAndFilters(properties, sources);
        encoderConfigurations = codersAndFilters(properties, sinks);

        // add to 'EventProtocolCoderConstants.getManager()' table
        for (TopicCoderFilterConfiguration tcfc : decoderConfigurations) {
            for (PotentialCoderFilter pcf : tcfc.getCoderFilters()) {
                getCoderManager().addDecoder(
                    EventProtocolParams.builder()
                    .groupId(getGroupId())
                    .artifactId(getArtifactId())
                    .topic(tcfc.getTopic())
                    .eventClass(pcf.getCodedClass())
                    .protocolFilter(pcf.getFilter())
                    .customGsonCoder(tcfc.getCustomGsonCoder())
                    .modelClassLoaderHash(NonDroolsPolicyController.class.getClassLoader().hashCode()));
            }
        }
        for (TopicCoderFilterConfiguration tcfc : encoderConfigurations) {
            for (PotentialCoderFilter pcf : tcfc.getCoderFilters()) {
                getCoderManager().addEncoder(
                    EventProtocolParams.builder()
                    .groupId(getGroupId())
                    .artifactId(getArtifactId())
                    .topic(tcfc.getTopic())
                    .eventClass(pcf.getCodedClass())
                    .protocolFilter(pcf.getFilter())
                    .customGsonCoder(tcfc.getCustomGsonCoder())
                    .modelClassLoaderHash(NonDroolsPolicyController.class.getClassLoader().hashCode()));
            }
        }
    }

    /*==============================*/
    /* 'DroolsController' interface */
    /*==============================*/
  
    // methods copied from 'MavenDroolsController' and 'NullDroolsController'

    @Override
    public boolean start() {

        logger.info("START: {}", this);

        synchronized (this) {
            if (this.alive) {
                return true;
            }
            this.alive = true;
        }

        return true;
    }

    @Override
    public boolean stop() {

        logger.info("STOP: {}", this);

        synchronized (this) {
            if (!this.alive) {
                return true;
            }
            this.alive = false;
        }

        return true;
    }

    @Override
    public void shutdown() {
        if (shutdownInProgress) {
            // avoid infinite recursion
            return;
        }
        logger.info("{}: SHUTDOWN", this);

        try {
            this.stop();
            this.removeCoders();
            shutdownInProgress = true;

            // the following method calls 'this.shutdown' recursively
            getDroolsFactory().shutdown(this);
        } catch (Exception e) {
            logger.error("{} SHUTDOWN FAILED because of {}", this, e.getMessage(), e);
        } finally {
            shutdownInProgress = false;
        }
    }

    @Override
    public void halt() {
        if (shutdownInProgress) {
            // avoid infinite recursion
            return;
        }
        logger.info("{}: HALT", this);

        try {
            this.stop();
            this.removeCoders();
            shutdownInProgress = true;

            // the following method calls 'this.halt' recursively
            getDroolsFactory().destroy(this);
        } catch (Exception e) {
            logger.error("{} HALT FAILED because of {}", this, e.getMessage(), e);
        } finally {
            shutdownInProgress = false;
        }
    }

    @Override
    public boolean isAlive() {
        return this.alive;
    }

    @Override
    public boolean lock() {
        logger.info("LOCK: {}",  this);

        this.locked = true;
        return true;
    }

    @Override
    public boolean unlock() {
        logger.info("UNLOCK: {}",  this);

        this.locked = false;
        return true;
    }

    @Override
    public boolean isLocked() {
        return this.locked;
    }

    @Override
    public String getGroupId() {
        //return DroolsControllerConstants.NO_GROUP_ID;
        return "NonDroolsPolicyController";
    }

    @Override
    public String getArtifactId() {
        //return DroolsControllerConstants.NO_ARTIFACT_ID;
        return getName();
    }

    @Override
    public String getVersion() {
        //return DroolsControllerConstants.NO_VERSION;
        return "1.0";
    }

    @Override
    public List<String> getSessionNames() {
        return new ArrayList<>();
    }

    @Override
    public List<String> getCanonicalSessionNames() {
        return new ArrayList<>();
    }

    @Override
    public List<String> getBaseDomainNames() {
        return Collections.emptyList();
    }

    @Override
    public boolean offer(String topic, String event) {
        return false;
    }

    @Override
    public <T> boolean offer(T event) {
        return false;
    }

    @Override
    public boolean deliver(TopicSink sink, Object event) {

        // this one is from 'MavenDroolsController'

        logger.info("{} DELIVER: {} FROM {} TO {}", this, event, this, sink);

        for (DroolsControllerFeatureApi feature : getDroolsProviders().getList()) {
            try {
                if (feature.beforeDeliver(this, sink, event)) {
                    return true;
                }
            } catch (Exception e) {
                logger.error("{}: feature {} before-deliver failure because of {}", this, feature.getClass().getName(),
                        e.getMessage(), e);
            }
        }

        if (sink == null) {
            throw new IllegalArgumentException(this +  " invalid sink");
        }

        if (event == null) {
            throw new IllegalArgumentException(this +  " invalid event");
        }

        if (this.locked) {
            throw new IllegalStateException(this +  " is locked");
        }

        if (!this.alive) {
            throw new IllegalStateException(this +  " is stopped");
        }

        String json =
                getCoderManager().encode(sink.getTopic(), event, this);

        synchronized (this.recentSinkEvents) {
            this.recentSinkEvents.add(json);
        }

        boolean success = sink.send(json);

        for (DroolsControllerFeatureApi feature : getDroolsProviders().getList()) {
            try {
                if (feature.afterDeliver(this, sink, event, json, success)) {
                    return true;
                }
            } catch (Exception e) {
                logger.error("{}: feature {} after-deliver failure because of {}", this, feature.getClass().getName(),
                        e.getMessage(), e);
            }
        }

        return success;

    }

    @Override
    public Object[] getRecentSourceEvents() {
        return new String[0];
    }

    @Override
    public PolicyContainer getContainer() {
        return null;
    }

    @Override
    public String[] getRecentSinkEvents() {
        synchronized (this.recentSinkEvents) {
            String[] events = new String[recentSinkEvents.size()];
            return recentSinkEvents.toArray(events);
        }
    }

    @Override
    public boolean ownsCoder(Class<?> coderClass, int modelHash) {
        //throw new IllegalStateException(makeInvokeMsg());
        return true;
    }

    @Override
    public Class<?> fetchModelClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(makeInvokeMsg());
        }
    }

    @Override
    public boolean isBrained() {
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("NonDroolsPolicyController []");
        return builder.toString();
    }

    @Override
    public void updateToVersion(String newGroupId, String newArtifactId, String newVersion,
            List<TopicCoderFilterConfiguration> decoderConfigurations,
            List<TopicCoderFilterConfiguration> encoderConfigurations)
                    throws LinkageError {
        throw new IllegalArgumentException(makeInvokeMsg());
    }

    @Override
    public Map<String, Integer> factClassNames(String sessionName) {
        return new HashMap<>();
    }

    @Override
    public long factCount(String sessionName) {
        return 0;
    }

    @Override
    public List<Object> facts(String sessionName, String className, boolean delete) {
        return new ArrayList<>();
    }

    @Override
    public <T> List<T> facts(@NonNull String sessionName, @NonNull Class<T> clazz) {
        return new ArrayList<>();
    }

    @Override
    public List<Object> factQuery(String sessionName, String queryName,
            String queriedEntity,
            boolean delete, Object... queryParams) {
        return new ArrayList<>();
    }

    @Override
    public <T> boolean delete(@NonNull String sessionName, @NonNull T fact) {
        return false;
    }

    @Override
    public <T> boolean delete(@NonNull T fact) {
        return false;
    }

    @Override
    public <T> boolean delete(@NonNull String sessionName, @NonNull Class<T> fact) {
        return false;
    }

    @Override
    public <T> boolean delete(@NonNull Class<T> fact) {
        return false;
    }

    private String makeInvokeMsg() {
        return this.getClass().getName() + " invoked";
    }

    /**
     * remove decoders.
     */
    protected void removeDecoders() {
        logger.info("REMOVE-DECODERS: {}", this);

        if (this.decoderConfigurations == null) {
            return;
        }


        for (TopicCoderFilterConfiguration coderConfig: decoderConfigurations) {
            String topic = coderConfig.getTopic();
            getCoderManager().removeDecoders(this.getGroupId(), this.getArtifactId(), topic);
        }
    }

    /**
     * remove decoders.
     */
    protected void removeEncoders() {

        logger.info("REMOVE-ENCODERS: {}", this);

        if (this.encoderConfigurations == null) {
            return;
        }

        for (TopicCoderFilterConfiguration coderConfig: encoderConfigurations) {
            String topic = coderConfig.getTopic();
            getCoderManager().removeEncoders(this.getGroupId(), this.getArtifactId(), topic);
        }
    }

    /**
     * removes this drools controllers and encoders and decoders from operation.
     */
    protected void removeCoders() {
        logger.info("{}: REMOVE-CODERS", this);

        try {
            this.removeDecoders();
        } catch (IllegalArgumentException e) {
            logger.error("{} REMOVE-DECODERS FAILED because of {}", this, e.getMessage(), e);
        }

        try {
            this.removeEncoders();
        } catch (IllegalArgumentException e) {
            logger.error("{} REMOVE-ENCODERS FAILED because of {}", this, e.getMessage(), e);
        }
    }

    protected List<TopicCoderFilterConfiguration> codersAndFilters(Properties properties,
            List<? extends Topic> topicEntities) {

        List<TopicCoderFilterConfiguration> topics2DecodedClasses2Filters = new ArrayList<>();

        if (topicEntities == null || topicEntities.isEmpty()) {
            return topics2DecodedClasses2Filters;
        }

        for (Topic topic : topicEntities) {

            // 1. first the topic

            String firstTopic = topic.getTopic();

            String propertyTopicEntityPrefix = getPropertyTopicPrefix(topic) + firstTopic;

            // 2. check if there is a custom decoder for this topic that the user prefers to use
            // instead of the ones provided in the platform

            CustomGsonCoder customGsonCoder = getCustomCoder(properties, propertyTopicEntityPrefix);

            // 3. second the list of classes associated with each topic

            String eventClasses = properties
                    .getProperty(propertyTopicEntityPrefix + PolicyEndPointProperties.PROPERTY_TOPIC_EVENTS_SUFFIX);

            if (eventClasses == null || eventClasses.isEmpty()) {
                logger.warn("There are no event classes for topic {}", firstTopic);
                continue;
            }

            List<PotentialCoderFilter> classes2Filters =
                            getFilterExpressions(properties, propertyTopicEntityPrefix, eventClasses);

            TopicCoderFilterConfiguration topic2Classes2Filters =
                    new TopicCoderFilterConfiguration(firstTopic, classes2Filters, customGsonCoder);
            topics2DecodedClasses2Filters.add(topic2Classes2Filters);
        }

        return topics2DecodedClasses2Filters;
    }

    private String getPropertyTopicPrefix(Topic topic) {
        boolean isSource = topic instanceof TopicSource;
        CommInfrastructure commInfra = topic.getTopicCommInfrastructure();
        if (commInfra == CommInfrastructure.UEB) {
            if (isSource) {
                return PolicyEndPointProperties.PROPERTY_UEB_SOURCE_TOPICS + ".";
            } else {
                return PolicyEndPointProperties.PROPERTY_UEB_SINK_TOPICS + ".";
            }
        } else if (commInfra == CommInfrastructure.DMAAP) {
            if (isSource) {
                return PolicyEndPointProperties.PROPERTY_DMAAP_SOURCE_TOPICS + ".";
            } else {
                return PolicyEndPointProperties.PROPERTY_DMAAP_SINK_TOPICS + ".";
            }
        } else if (commInfra == CommInfrastructure.NOOP) {
            if (isSource) {
                return PolicyEndPointProperties.PROPERTY_NOOP_SOURCE_TOPICS + ".";
            } else {
                return PolicyEndPointProperties.PROPERTY_NOOP_SINK_TOPICS + ".";
            }
        } else {
            throw new IllegalArgumentException("Invalid Communication Infrastructure: " + commInfra);
        }
    }

    private CustomGsonCoder getCustomCoder(Properties properties, String propertyPrefix) {
        String customGson = properties.getProperty(propertyPrefix
                + PolicyEndPointProperties.PROPERTY_TOPIC_EVENTS_CUSTOM_MODEL_CODER_GSON_SUFFIX);

        CustomGsonCoder customGsonCoder = null;
        if (customGson != null && !customGson.isEmpty()) {
            try {
                customGsonCoder = new CustomGsonCoder(customGson);
            } catch (IllegalArgumentException e) {
                logger.warn("{}: cannot create custom-gson-coder {} because of {}", this, customGson,
                        e.getMessage(), e);
            }
        }
        return customGsonCoder;
    }

    private List<PotentialCoderFilter> getFilterExpressions(Properties properties, String propertyPrefix,
                    String eventClasses) {

        List<PotentialCoderFilter> classes2Filters = new ArrayList<>();

        List<String> topicClasses = new ArrayList<>(Arrays.asList(eventClasses.split("\\s*,\\s*")));

        for (String theClass : topicClasses) {

            // 4. for each coder class, get the filter expression

            String filter = properties
                    .getProperty(propertyPrefix
                            + PolicyEndPointProperties.PROPERTY_TOPIC_EVENTS_SUFFIX
                            + "." + theClass + PolicyEndPointProperties.PROPERTY_TOPIC_EVENTS_FILTER_SUFFIX);

            JsonProtocolFilter protocolFilter = new JsonProtocolFilter(filter);
            PotentialCoderFilter class2Filters = new PotentialCoderFilter(theClass, protocolFilter);
            classes2Filters.add(class2Filters);
        }

        return classes2Filters;
    }

    // these may be overridden by junit tests

    protected EventProtocolCoder getCoderManager() {
        return EventProtocolCoderConstants.getManager();
    }

    protected OrderedServiceImpl<DroolsControllerFeatureApi> getDroolsProviders() {
        return DroolsControllerFeatureApiConstants.getProviders();
    }
}
