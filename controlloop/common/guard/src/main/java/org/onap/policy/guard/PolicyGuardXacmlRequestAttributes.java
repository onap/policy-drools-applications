/*-
 * ============LICENSE_START=======================================================
 * guard
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

package org.onap.policy.guard;


import com.att.research.xacml.std.annotations.XACMLAction;
import com.att.research.xacml.std.annotations.XACMLRequest;
import com.att.research.xacml.std.annotations.XACMLResource;
import com.att.research.xacml.std.annotations.XACMLSubject;



@XACMLRequest(ReturnPolicyIdList=true,CombinedDecision=true)
public class PolicyGuardXacmlRequestAttributes {

		
		

		public PolicyGuardXacmlRequestAttributes(String clname_id, String actor_id, String operation_id, String target_id,
				String request_id) {
			super();
			this.clname_id = clname_id;
			this.actor_id = actor_id;
			this.operation_id = operation_id;
			this.target_id = target_id;
			this.request_id = request_id;
		}



		@Override
		public String toString() {
			return "PolicyGuardXacmlRequestAttributes [actor_id=" + actor_id + ", operation_id=" + operation_id
					+ ", target_id=" + target_id + ", request_id=" + request_id + "]";
		}



		//@XACMLSubject(includeInResults=true, attributeId="urn:oasis:names:tc:xacml:1.0:subject:subject-id")
		//String	userID;
		
		//@XACMLAction()
		//String	action;
		
		@XACMLSubject(includeInResults=true, attributeId="urn:oasis:names:tc:xacml:1.0:clname:clname-id")
		String	clname_id;
		
		@XACMLSubject(includeInResults=true, attributeId="urn:oasis:names:tc:xacml:1.0:actor:actor-id")
		String	actor_id;
		
		@XACMLAction(includeInResults=true, attributeId="urn:oasis:names:tc:xacml:1.0:operation:operation-id")
		String	operation_id;
		
		//@XACMLResource(includeInResults=true, attributeId="urn:oasis:names:tc:xacml:1.0:resource:resource-id123")
		//String	resource;
		
		@XACMLResource(includeInResults=true, attributeId="urn:oasis:names:tc:xacml:1.0:target:target-id")
		String	target_id;
		
		@XACMLResource(includeInResults=true, attributeId="urn:oasis:names:tc:xacml:1.0:request:request-id")
		String	request_id;

		public String getActor_id() {
			return actor_id;
		}



		public void setActor_id(String actor_id) {
			this.actor_id = actor_id;
		}



		public String getOperation_id() {
			return operation_id;
		}



		public void setOperation_id(String operation_id) {
			this.operation_id = operation_id;
		}



		public String getTarget_id() {
			return target_id;
		}



		public void setTarget_id(String target_id) {
			this.target_id = target_id;
		}



		public String getRequest_id() {
			return request_id;
		}



		public void setRequest_id(String request_id) {
			this.request_id = request_id;
		}



		public String getClname_id() {
			return clname_id;
		}



		public void setClname_id(String clname_id) {
			this.clname_id = clname_id;
		}
		
		
		
		
	};

