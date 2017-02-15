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

 package org.openecomp.policy.template.demo;
 
 import java.util.HashMap;
 import java.util.LinkedList;
 import java.util.Map;
 import java.util.Queue;
 
 import org.openecomp.policy.controlloop.VirtualControlLoopNotification;
 import org.openecomp.policy.appc.Request;
 import org.openecomp.policy.controlloop.util.Serialization;
 
 
 public class PolicyEngine {
 
 	private static Map<String, Map<String, Queue<Object>>> busMap = new HashMap<String, Map<String, Queue<Object>>>();
 	
 	public PolicyEngine() {}
 	
 	public boolean deliver(String busType, String topic, Object obj) {
 		if (obj instanceof VirtualControlLoopNotification) {
			VirtualControlLoopNotification notification = (VirtualControlLoopNotification) obj;
			System.out.println("Notification to be sent:");
			System.out.println(Serialization.gsonPretty.toJson(notification));
		}
		if (obj instanceof Request) {
			Request request = (Request) obj;
			System.out.println("APPC request to be sent:");
			System.out.println("Request: " + request.Action + " RequestID: " + request.CommonHeader.RequestID + " Payload: " + request.Payload);
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
 }
