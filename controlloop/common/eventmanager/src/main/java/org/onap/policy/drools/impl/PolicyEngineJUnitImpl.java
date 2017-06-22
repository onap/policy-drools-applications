/*-
 * ============LICENSE_START=======================================================
 * policy engine
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

package org.onap.policy.drools.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.onap.policy.appc.Request;
import org.onap.policy.controlloop.ControlLoopNotification;
import org.onap.policy.controlloop.util.Serialization;

import org.onap.policy.drools.PolicyEngine;

public class PolicyEngineJUnitImpl implements PolicyEngine {

	private Map<String, Map<String, Queue<Object>>> busMap = new HashMap<String, Map<String, Queue<Object>>>();

	@Override
	public boolean deliver(String busType, String topic, Object obj) {
		if (obj instanceof ControlLoopNotification) {
			ControlLoopNotification notification = (ControlLoopNotification) obj;
			//System.out.println("Notification: " + notification.notification + " " + (notification.message == null ? "" : notification.message) + " " + notification.history);
			System.out.println(Serialization.gsonPretty.toJson(notification));
		}
		if (obj instanceof Request) {
			Request request = (Request) obj;
			System.out.println("Request: " + request.Action + " subrequest " + request.CommonHeader.SubRequestID);
		}
		//
		// Does the bus exist?
		//
		if (busMap.containsKey(busType) == false) {
			System.out.println("creating new bus type " + busType);
			//
			// Create the bus
			//
			busMap.put(busType, new HashMap<String, Queue<Object>>());
		}
		//
		// Get the bus
		//
		Map<String, Queue<Object>> topicMap = busMap.get(busType);
		//
		// Does the topic exist?
		//
		if (topicMap.containsKey(topic) == false) {
			System.out.println("creating new topic " + topic);
			//
			// Create the topic
			//
			topicMap.put(topic, new LinkedList<Object>());
		}
		//
		// Get the topic queue
		//
		System.out.println("queueing");
		return topicMap.get(topic).add(obj);
	}
	
	public Object	subscribe(String busType, String topic) {
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
				System.out.println("The queue has " + topicMap.get(topic).size());
				return topicMap.get(topic).poll();
			} else {
				System.err.println("No topic exists " + topic);
			}
		} else {
			System.err.println("No bus exists " + busType);
		}
		return null;
	}

}
