/*-
 * ============LICENSE_START=======================================================
 * appc
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

package org.openecomp.policy.appc;

public enum ResponseValue {
	ACCEPT("ACCEPT"),
	ERROR("ERROR"),
	REJECT("REJECT"),
	SUCCESS("SUCCESS"),
	FAILURE("FAILURE")
	;
	
	private String value;
	
	private ResponseValue(String value) {
		this.value = value;
	}
	
	public String toString() {
		return this.value;
	}

	public static ResponseValue toResponseValue(String value) {
		if (value.toString().equals(ACCEPT.toString())) {
			return ACCEPT;
		}
		if (value.toString().equals(ERROR.toString())) {
			return ERROR;
		}
		if (value.toString().equals(REJECT.toString())) {
			return REJECT;
		}
		if (value.toString().equals(SUCCESS.toString())) {
			return SUCCESS;
		}
		if (value.toString().equals(FAILURE.toString())) {
			return FAILURE;
		}
		
		return null;
	}
	
}
