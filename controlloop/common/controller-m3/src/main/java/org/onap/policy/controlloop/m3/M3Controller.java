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

package org.onap.policy.controlloop.m3;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.onap.policy.common.endpoints.event.comm.Topic;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.event.comm.TopicListener;
import org.onap.policy.common.endpoints.event.comm.TopicSource;
import org.onap.policy.controlloop.CanonicalOnset;
import org.onap.policy.controlloop.ControlLoopEvent;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.ControlLoopResponse;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.controlloop.eventmanager.ControlLoopEventManager2;
import org.onap.policy.controlloop.utils.ControlLoopUtils;
import org.onap.policy.drools.core.PolicyContainer;
import org.onap.policy.drools.core.PolicySession;
import org.onap.policy.drools.protocol.coders.EventProtocolCoder.CoderFilters;
import org.onap.policy.drools.protocol.coders.EventProtocolCoderConstants;
import org.onap.policy.drools.protocol.coders.ProtocolCoderToolset;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Each instance of this class corresponds to a 'PolicySession' instance.
 * The actual use of Drools is minimized; instead, all objects placed in
 * Drools WorkingMemory are forwarded here, with most of them being retracted,
 * because they are already being received and processed outside of Drools.
 */
public class M3Controller {
    private static Logger logger = LoggerFactory.getLogger(M3Controller.class);

    static Map<PolicySession, M3Controller> controllers =
        new IdentityHashMap<>();

    // associated 'PolicySession'
    PolicySession policySession;

    // additional data associated with session
    String groupId;
    String artifactId;

    // top-level tosca policy table (first key = name, second key = version)
    Map<String, Map<String, ToscaPolicy>> toscaPolicies = new HashMap<>();

    // maps 'controlLoopControlName' to 'ControlLoopParams'
    Map<String, ControlLoopParams> controlLoopParams = new HashMap<>();

    // maps 'event' to 'ControlLoopEventManager'
    Map<ControlLoopEvent, ControlLoopEventManager> eventManagers = new ConcurrentHashMap<>();

    // maps 'topic' to 'TopicData'
    Map<String, TopicData> topicDataTable = new ConcurrentHashMap<>();

    /**
     * Process an incoming message received via Drools WorkingMemory. Note that
     * this method is always run within a Drools thread
     *
     * @param object this is the incoming message
     * @return 'true' if the object should be retracted, 'false' if not
     */
    public static boolean incoming(Object object) {
        logger.debug("Received object of {}", object.getClass());
        if (object instanceof ToscaPolicy) {
            // this needs to remain in Drools memory, because there are some
            // asserts that check for it
            getController().addToscaPolicy((ToscaPolicy)object);
            return false;
        } else if (object instanceof ControlLoopParams) {
            // this needs to remain in Drools memory, because there are some
            // asserts that check for it
            return false;
        }
        return true;
    }

    /**
     * Locate the 'M3Controller' instance associated with a particular
     * 'PolicySession'. Note that this method must be called from a Drools
     * thread, so the 'PolicySession' object can be determed. If the controller
     * doesn't exist, it is created.
     *
     * @return the M3Controller (new or existing) associated with the current
     *     Drools thread.
     */
    private static synchronized M3Controller getController() {
        PolicySession session = PolicySession.getCurrentSession();
        return controllers.computeIfAbsent(session, key -> new M3Controller(session));
    }

    /* ============================================================ */

    /**
     * Initialize a new 'M3Controller'.
     *
     * @param policySession this is the session associated with this controller
     */
    public M3Controller(PolicySession policySession) {
        this.policySession = policySession;
        PolicyContainer pc = policySession.getPolicyContainer();
        this.groupId = pc.getGroupId();
        this.artifactId = pc.getArtifactId();

        // go through all of the incoming message decoders associated
        // with this controller
        for (ProtocolCoderToolset pct :
                EventProtocolCoderConstants.getManager()
                .getDecoders(groupId, artifactId)) {
            // go through the 'CoderFilters' instances, and see if there are
            // any that we are interested in
            for (CoderFilters cf : pct.getCoders()) {
                try {
                    Class<?> clazz = Class.forName(cf.getCodedClass());
                    if (ControlLoopEvent.class.isAssignableFrom(clazz)) {
                        // this one is of interest
                        logger.debug("M3Controller using CoderFilters: {}", cf);
                        getTopicData(pct.getTopic());
                    }
                } catch (ClassNotFoundException e) {
                    logger.error("CoderFilter refers to unknown class: {}",
                                 cf.getCodedClass(), e);
                }
            }
        }

        // start all 'TopicData' instances
        for (TopicData topicData : topicDataTable.values()) {
            topicData.start();
        }
    }

    /**
     * Add or replace a ToscaPolicy instance. The policy is keyed by name and
     * version.
     *
     * @param toscaPolicy the ToscaPolicy being added
     * @return if a ToscaPolicy with this name/version previously existed within
     *     this M3Controller, it is returned; otherwise, 'null' is returned.
     */
    public synchronized ToscaPolicy addToscaPolicy(ToscaPolicy toscaPolicy) {
        Map<String, ToscaPolicy> level2 =
            toscaPolicies.computeIfAbsent(toscaPolicy.getName(),
                key -> new HashMap<String, ToscaPolicy>());
        ToscaPolicy prev = level2.put(toscaPolicy.getVersion(), toscaPolicy);
        if (prev != null) {
            // update 'ControlLoopParams' entries
            for (ControlLoopParams clp : controlLoopParams.values()) {
                if (clp.getToscaPolicy() == prev) {
                    clp.setToscaPolicy(toscaPolicy);
                }
            }
        }
        logger.debug("ToscaPolicy name={}, version={}, count={}, prev={}",
            toscaPolicy.getName(), toscaPolicy.getVersion(), toscaPolicies.size(), (prev != null));
        dumpTables();

        // attempt to create a 'ControlLoopParams' instance from this object
        ControlLoopParams params =
            ControlLoopUtils.toControlLoopParams(toscaPolicy);
        if (params != null) {
            addControlLoopParams(params);
            // for now, this needs to be in Drools memory, because there are some
            // asserts that check for it
            insert(params);
        }
        return prev;
    }

    /**
     * Remove a ToscaPolicy instance associated with the specified name and
     * version.
     *
     * @param name the name of the ToscaPolicy to remove
     * @param version the version of the ToscaPolicy to remove
     * @return the ToscaPolicy that was removed, or 'null' if not found
     */
    public synchronized ToscaPolicy removeToscaPolicy(String name, String version) {
        ToscaPolicy prev = null;
        Map<String, ToscaPolicy> level2 = toscaPolicies.get(name);

        if (level2 != null && (prev = level2.remove(version)) != null) {
            // remove all 'ControlLoopParams' entries referencing this policy
            for (ControlLoopParams clp :
                    new ArrayList<>(controlLoopParams.values())) {
                if (clp.getToscaPolicy() == prev) {
                    controlLoopParams.remove(clp.getClosedLoopControlName());
                }
            }
        }
        return prev;
    }

    /**
     * Fetch a ToscaPolicy instance associated with the specified name and
     * version.
     *
     * @param name the name of the ToscaPolicy
     * @param version the version of the ToscaPolicy
     * @return the ToscaPolicy, or 'null' if not found
     */
    public synchronized ToscaPolicy getToscaPolicy(String name, String version) {
        Map<String, ToscaPolicy> level2 = toscaPolicies.get(name);
        return (level2 == null ? null : level2.get(version));
    }

    /**
     * Add a new 'ControlLoopParams' instance -- they are keyed by
     * 'closedLoopControlName'.
     *
     * @param clp the 'ControlLoopParams' instance to add
     * @return the 'ControlLoopParams' instance previously associated with the
     *     'closedLoopControlName' ('null' if it didn't exist)
     */
    public synchronized ControlLoopParams addControlLoopParams(ControlLoopParams clp) {
        ToscaPolicy toscaPolicy =
            getToscaPolicy(clp.getPolicyName(), clp.getPolicyVersion());
        if (toscaPolicy == null) {
            // there needs to be a 'ToscaPolicy' instance with a matching
            // name/version
            logger.debug("Missing toscaPolicy, name={}, version={}",
                         clp.getPolicyName(), clp.getPolicyVersion());
            return clp;
        }

        clp.setToscaPolicy(toscaPolicy);
        ControlLoopParams prev =
            controlLoopParams.put(clp.getClosedLoopControlName(), clp);

        logger.debug("ControlLoopParams name={}, version={}, closedLoopControlName={}, count={}, prev={}",
                     clp.getPolicyName(), clp.getPolicyVersion(),
                     clp.getClosedLoopControlName(), controlLoopParams.size(), (prev != null));
        dumpTables();
        return prev;
    }

    /**
     * Remove a ControlLoopParams instance associated with the specified
     * 'closedLoopControlName'.
     *
     * @param closedLoopControlName the closedLoopControlName identifying the
     *     ControlLoopParams instance
     * @return the 'ControlLoopParams' instance, 'null' if not found
     */
    public synchronized ControlLoopParams removeControlLoopParams(String closedLoopControlName) {
        return controlLoopParams.remove(closedLoopControlName);
    }

    /**
     * Dump out the ToscaPolicy and ControlLoopParams tables in
     * human-readable form.
     */
    private void dumpTables() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bos, true);

        // name(25) version(10) closedLoopControlName(...)

        String format = "%-25s %-10s %s\n";
        out.println("ToscaPolicy Table");
        out.format(format, "Name", "Version", "");
        out.format(format, "----", "-------", "");

        for (Map<String, ToscaPolicy> level2 : toscaPolicies.values()) {
            for (ToscaPolicy tp : level2.values()) {
                out.format(format, tp.getName(), tp.getVersion(), "");
            }
        }

        out.println("\nControlLoopParams Table");
        out.format(format, "Name", "Version", "ClosedLoopControlName");
        out.format(format, "----", "-------", "---------------------");
        for (ControlLoopParams cp : controlLoopParams.values()) {
            out.format(format, cp.getPolicyName(), cp.getPolicyVersion(),
                       cp.getClosedLoopControlName());
        }

        logger.debug(new String(bos.toByteArray()));
    }

    /**
     * Find or create a 'TopicData' instance associated with the specified
     * topic name.
     *
     * @param name the topic name
     * @return the new or existing 'TopicData' instance associated with 'name'
     */
    private TopicData getTopicData(String name) {
        return topicDataTable.computeIfAbsent(name, key -> new TopicData(name));
    }

    /* ============================================================ */

    /**
     * Process an incoming 'ControlLoopEvent'.
     *
     * @param event the incoming 'ControlLoopEvent'
     */
    private void processEvent(ControlLoopEvent event) {
        String clName = event.getClosedLoopControlName();
        ControlLoopParams params = controlLoopParams.get(clName);
        if (params == null) {
            logger.debug("No ControlLoopParams for event: {}", event);
            return;
        }

        if (event instanceof CanonicalOnset && event.getRequestId() == null) {
            // the requestId should not be 'null'
            handleNullRequestId((CanonicalOnset) event, params);
            return;
        }

        // find or create a 'ControlLoopEventManager' instance for this event
        final ControlLoopEventManager manager = eventManagers.computeIfAbsent(event, key -> {

            // only create new managers for onsets
            if (!(event instanceof CanonicalOnset)) {
                return null;
            }

            CanonicalOnset coEvent = (CanonicalOnset) event;

            // a ControlLoopEventManager does not yet exist for this
            // 'event' -- create one, and send it the initial event
            try {
                ControlLoopEventManager mgr = new ControlLoopEventManager(params, coEvent);

                // we could use the 'SerialWorkQueue' here, but it isn't
                // necessary, because the ControlLoopEventManager object
                // isn't yet accessible through the 'eventManagers' table,
                // and there is a write-lock on 'event'.
                if (!mgr.initialEvent(coEvent, params)) {
                    return null;
                }
                return mgr;
            } catch (ControlLoopException e) {
                logger.error("Exception creating ControlLoopEventManager", e);
                return null;
            }
        });

        if(manager == null) {
            // this block of code originally appeared in the 'EVENT.CLEANUP'
            // Drools rule
            String ruleName = "EVENT.CLEANUP";

            logger.info("{}: {}", clName, ruleName);
            logger.debug("{}: {}: orphan event={}",
                         clName, ruleName, event);
            return;
        }

        if (manager.getContext().getEvent() != event) {
            // subsequent event - could be an onset, abatement, etc.
            manager.getSerialWorkQueue().queueAndRun(() -> manager.subsequentEvent((VirtualControlLoopEvent) event));
        }

        // else: nothing else to do as initial onset has already been handled
    }

    /**
     * Generate and send a notification message in response to a 'CanonicalOnset'
     * with a null 'requestId'.
     *
     * @param event the CanonicalOnset event
     * @param params the associated ControlLoopParams
     */
    private void handleNullRequestId(CanonicalOnset event,
                                     ControlLoopParams params) {
        // this block of code originally appeared in the 'EVENT' Drools rule
        String ruleName = "EVENT";
        String clName = event.getClosedLoopControlName();

        VirtualControlLoopNotification notification =
            new VirtualControlLoopNotification(event);
        notification.setNotification(ControlLoopNotificationType.REJECTED);
        notification.setFrom("policy");
        notification.setMessage("Missing requestId");
        notification.setPolicyName(params.getPolicyName() + "." + ruleName);
        notification.setPolicyScope(params.getPolicyScope());
        notification.setPolicyVersion(params.getPolicyVersion());

        //
        // Generate notification
        //
        try {
            PolicyEngineConstants.getManager().deliver("POLICY-CL-MGT", notification);

        } catch (RuntimeException e) {
            logger.warn("{}: {}.{}: event={} exception generating notification",
                        clName, params.getPolicyName(), ruleName,
                        event, e);
        }
    }

    /**
     * Insert an object into Drools memory.
     *
     * @param object the object to insert
     */
    private void insert(Object object) {
        logger.debug("Insert called for object of {}", object.getClass());
        policySession.getKieSession().insert(object);
    }

    /* ============================================================ */

    /**
     * This nested class corresponds to a single topic name. At present, the
     * only topics that are directly handled by this class are
     * 'ControlLoopEvent', and subclasses (hence, the call to 'processEvent').
     * If other event types later need to be directly handled, this may need to
     * become an abstract class, with subclasses for the various event types.
     */
    private class TopicData implements TopicListener {
        // topic name
        private String name;

        // set of 'TopicSource' instances associated with this topic
        // (probably only one, but the underlying APIs support a list)
        private List<TopicSource> topicSources = null;

        /**
         * Constructor -- initialize the 'TopicData' instance.
         *
         * @param name the topic name
         */
        private TopicData(String name) {
            this.name = name;
        }

        /**
         * Register all of the 'TopicSource' instances associated with this
         * topic, and start the listeners.
         */
        private void start() {
            if (topicSources == null) {
                // locate topic sources
                ArrayList<String> topics = new ArrayList<>();
                topics.add(name);
                topicSources = TopicEndpointManager.getManager().getTopicSources(topics);
            }

            for (TopicSource consumer : topicSources) {
                consumer.register(this);
                consumer.start();
            }
        }

        /**
         * Unregister all of the 'TopicSource' instances associated with this
         * topic, and stop the listeners.
         */
        private void stop() {
            if (topicSources != null) {
                for (TopicSource consumer : topicSources) {
                    consumer.unregister(this);
                    consumer.stop();
                }
            }
        }

        /*===========================*/
        /* 'TopicListener' interface */
        /*===========================*/

        @Override
        public void onTopicEvent(Topic.CommInfrastructure commType, String topic, String event) {
            logger.debug("TopicData.onTopicEvent: {}", event);
            Object decodedObject =
                EventProtocolCoderConstants.getManager().decode(groupId, artifactId, topic, event);
            if (decodedObject != null) {
                logger.debug("Decoded to object of {}", decodedObject.getClass());
                if (decodedObject instanceof ControlLoopEvent) {
                    PolicyEngineConstants.getManager().getExecutorService().execute(() ->
                        processEvent((ControlLoopEvent)decodedObject));
                }
            }
        }
    }

    /* ============================================================ */

    /**
     * This is a 'ControlLoopEventManager2' variant designed to run under
     * 'M3Controller'.
     */
    private class ControlLoopEventManager extends ControlLoopEventManager2 {
        // used to serialize method calls from multiple threads, which avoids the
        // need for additional synchronization
        private SerialWorkQueue serialWorkQueue;

        /**
         * Constructor - initialize a ControlLoopEventManager, and include an
         * initial Runnable in the SerialWorkQueue.
         *
         * @param params the 'ControlLoopParam's instance associated with the
         *     'closedLoopControlName'
         * @param event the initial ControlLoopEvent
         * @param runnable an initial Runnable to place in the SerialWorkQueue
         */
        private ControlLoopEventManager(ControlLoopParams params, VirtualControlLoopEvent event, Runnable runnable)
            throws ControlLoopException {

            super(params, event);
            serialWorkQueue = new SerialWorkQueue(runnable);
        }

        /**
         * Constructor - initialize a ControlLoopEventManager.
         *
         * @param params the 'ControlLoopParam's instance associated with the
         *     'closedLoopControlName'
         * @param event the initial ControlLoopEvent
         */
        private ControlLoopEventManager(ControlLoopParams params, VirtualControlLoopEvent event)
            throws ControlLoopException {

            super(params, event);
            serialWorkQueue = new SerialWorkQueue();
        }

        /**
         * Return the SerialWorkQueue.
         *
         * @return the SerialWorkQueue
         */
        private SerialWorkQueue getSerialWorkQueue() {
            return serialWorkQueue;
        }

        /**
         * This is a notification from the base class that a state transition
         * has occurred.
         */
        @Override
        protected void notifyUpdate() {
            update();
        }

        /**
         * Process the initial event from DCAE that caused the
         * 'ControlLoopEventManager' to be created.
         *
         * @param event the initial event
         * @param params the associated ControlLoopParams
         * @return 'true' if the manager was successfully started
         */
        private boolean initialEvent(CanonicalOnset event, ControlLoopParams params) {
            // this block of code originally appeared in the 'EVENT' Drools rule
            String ruleName = "EVENT";
            String clName = event.getClosedLoopControlName();

            VirtualControlLoopNotification notification;
            boolean success = false;

            try {
                //
                // Check the event, because we need it to not be null when
                // we create the ControlLoopEventManager. The ControlLoopEventManager
                // will do extra syntax checking as well as check if the closed loop is disabled.
                //
                start();
                notification = makeNotification();
                notification.setNotification(ControlLoopNotificationType.ACTIVE);
                notification.setPolicyName(params.getPolicyName() + "." + ruleName);
                success = true;
            } catch (Exception e) {
                logger.warn("{}: {}.{}", clName, params.getPolicyName(), ruleName, e);
                notification = new VirtualControlLoopNotification(event);
                notification.setNotification(ControlLoopNotificationType.REJECTED);
                notification.setMessage("Exception occurred: " + e.getMessage());
                notification.setPolicyName(params.getPolicyName() + "." + ruleName);
                notification.setPolicyScope(params.getPolicyScope());
                notification.setPolicyVersion(params.getPolicyVersion());
            }
            //
            // Generate notification
            //
            try {
                PolicyEngineConstants.getManager().deliver("POLICY-CL-MGT", notification);

            } catch (RuntimeException e) {
                logger.warn("{}: {}.{}: event={} exception generating notification",
                            clName, params.getPolicyName(), ruleName,
                            event, e);
            }

            return success;
        }

        /**
         * Process a subsequent event from DCAE.
         *
         * @param event the VirtualControlLoopEvent event
         */
        private void subsequentEvent(VirtualControlLoopEvent event) {
            // this block of code originally appeared in the
            // 'EVENT.MANAGER.NEW.EVENT' Drools rule
            String ruleName = "EVENT.MANAGER.NEW.EVENT";
            UUID requestId = event.getRequestId();
            String clName = event.getClosedLoopControlName();

            //
            // Check what kind of event this is
            //
            switch (onNewEvent(event)) {
                case SYNTAX_ERROR:
                    //
                    // Ignore any bad syntax events
                    //
                    logger.warn("{}: {}.{}: syntax error",
                                getClosedLoopControlName(), getPolicyName(), ruleName);
                    break;

                case FIRST_ABATEMENT:
                case SUBSEQUENT_ABATEMENT:
                    //
                    // TODO: handle the abatement.  Currently, it's just discarded.
                    //
                    break;

                case FIRST_ONSET:
                case SUBSEQUENT_ONSET:
                default:
                    //
                    // We don't care about subsequent onsets
                    //
                    logger.warn("{}: {}.{}: subsequent onset",
                                getClosedLoopControlName(), getPolicyName(), ruleName);
                    break;
            }
        }

        /**
         * Called when a state transition occurs.
         */
        private void update() {
            // handle synchronization by running it under the SerialWorkQueue
            getSerialWorkQueue().queueAndRun(() -> {
                if (isActive()) {
                    updateActive();
                } else {
                    updateInactive();
                }
            });
        }

        /**
         * Called when a state transition occurs, and we are in the active state.
         */
        private void updateActive() {
            if (!isUpdated()) {
                // no notification needed
                return;
            }

            // this block of code originally appeared in the
            // 'EVENT.MANAGER.PROCESSING' Drools rule
            String ruleName = "EVENT.MANAGER.PROCESSING";
            VirtualControlLoopNotification notification =
                getNotification();

            logger.info("{}: {}.{}: manager={}",
                        getClosedLoopControlName(), getPolicyName(), ruleName,
                        this);
            //
            // Generate notification
            //
            try {
                notification.setPolicyName(getPolicyName() + "." + ruleName);
                PolicyEngineConstants.getManager().deliver("POLICY-CL-MGT", notification);

            } catch (RuntimeException e) {
                logger.warn("{}: {}.{}: manager={} exception generating notification",
                            getClosedLoopControlName(), getPolicyName(), ruleName,
                            this, e);
            }
            //
            // Generate Response notification
            //
            try {
                ControlLoopResponse clResponse = getControlLoopResponse();
                if (clResponse != null) {
                    PolicyEngineConstants.getManager().deliver("DCAE_CL_RSP", clResponse);
                }

            } catch (RuntimeException e) {
                logger.warn("{}: {}.{}: manager={} exception generating Response notification",
                            getClosedLoopControlName(), getPolicyName(), ruleName,
                            this, e);
            }
            //
            // Discard this message and wait for the next response.
            //
            nextStep();
            update();
        }

        /**
         * Called when a state transition has occurred, and we are not in the
         * active state.
         */
        private void updateInactive() {
            // this block of code originally appeared in the 'EVENT.MANAGER.FINAL'
            // Drools rule
            String ruleName = "EVENT.MANAGER.FINAL";
            VirtualControlLoopNotification notification =
                getNotification();

            logger.info("{}: {}.{}: manager={}",
                        getClosedLoopControlName(), getPolicyName(), ruleName,
                        this);
            //
            // Generate notification
            //
            try {
                notification.setPolicyName(getPolicyName() + "." + ruleName);
                PolicyEngineConstants.getManager().deliver("POLICY-CL-MGT", notification);
            } catch (RuntimeException e) {
                logger.warn("{}: {}.{}: manager={} exception generating notification",
                            getClosedLoopControlName(), getPolicyName(), ruleName,
                            this, e);
            }
            //
            // Destroy the manager
            //
            destroy();

            // Remove the entry from the table
            eventManagers.remove(getContext().getEvent());
        }
    }
}
