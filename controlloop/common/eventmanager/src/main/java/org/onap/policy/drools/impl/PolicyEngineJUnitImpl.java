/*-
 * ============LICENSE_START=======================================================
 * policy engine
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.drools.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.onap.policy.appc.Request;
import org.onap.policy.appclcm.LcmRequestWrapper;
import org.onap.policy.controlloop.ControlLoopNotification;
import org.onap.policy.controlloop.util.Serialization;
import org.onap.policy.drools.PolicyEngine;
import org.onap.policy.drools.PolicyEngineListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyEngineJUnitImpl implements PolicyEngine {

    private static final Logger logger = LoggerFactory.getLogger(PolicyEngineJUnitImpl.class);
    private Map<String, Map<String, Queue<Object>>> busMap = new HashMap<>();
    private List<PolicyEngineListener> listeners = new ArrayList<>();

    /**
     * Adds all objects that implement PolicyEngineListener to the notification list when an event
     * occurs.
     * 
     * @param listener an object that is interest in knowing about events published to the
     *        PolicyEngine
     */
    public void addListener(PolicyEngineListener listener) {
        listeners.add(listener);
    }

    /**
     * Notifies all listeners about a new event.
     * 
     * @param topic the topic in which the notification was sent to
     */
    public void notifyListeners(String topic) {
        for (PolicyEngineListener listener : listeners) {
            listener.newEventNotification(topic);
        }
    }

    @Override
    public boolean deliver(String busType, String topic, Object obj) {
        if (obj instanceof ControlLoopNotification) {
            ControlLoopNotification notification = (ControlLoopNotification) obj;
            if (logger.isDebugEnabled()) {
                logger.debug(Serialization.gsonPretty.toJson(notification));
            }
        }
        if (obj instanceof Request) {
            Request request = (Request) obj;
            logger.debug("Request: {} subrequest {}", request.getAction(), request.getCommonHeader().getSubRequestId());
        } else if (obj instanceof LcmRequestWrapper) {
            LcmRequestWrapper dmaapRequest = (LcmRequestWrapper) obj;
            logger.debug("Request: {} subrequest {}", dmaapRequest.getBody().getAction(),
                    dmaapRequest.getBody().getCommonHeader().getSubRequestId());
        }
        //
        // Does the bus exist?
        //
        if (!busMap.containsKey(busType)) {
            logger.debug("creating new bus type {}", busType);
            //
            // Create the bus
            //
            busMap.put(busType, new HashMap<>());
        }
        //
        // Get the bus
        //
        Map<String, Queue<Object>> topicMap = busMap.get(busType);
        //
        // Does the topic exist?
        //
        if (!topicMap.containsKey(topic)) {
            logger.debug("creating new topic {}", topic);
            //
            // Create the topic
            //
            topicMap.put(topic, new LinkedList<>());
        }
        //
        // Get the topic queue
        //
        logger.debug("queueing");
        boolean res = topicMap.get(topic).add(obj);
        notifyListeners(topic);
        return res;
    }

    /**
     * Subscribe to a topic on a bus.
     * 
     * @param busType the bus type
     * @param topic the topic
     * @return the head of the queue, or <code>null</code> if the queue or bus does not exist or the
     *         queue is empty
     */
    public Object subscribe(String busType, String topic) {
        //
        // Does the bus exist?
        //
        if (busMap.containsKey(busType)) {
            //
            // Get the bus
            //
            Map<String, Queue<Object>> topicMap = busMap.get(busType);
            //
            // Does the topic exist?
            //
            if (topicMap.containsKey(topic)) {
                logger.debug("The queue has {}", topicMap.get(topic).size());
                return topicMap.get(topic).poll();
            } else {
                logger.error("No topic exists {}", topic);
            }
        } else {
            logger.error("No bus exists {}", busType);
        }
        return null;
    }

}
