/*-
 * ============LICENSE_START=======================================================
 * controlloop
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

package org.onap.policy.controlloop;

public enum ControlLoopTargetType {
	VM("VM"),
	VF("VF"),
	VFC("VFC"),
	VNF("VNF")
	;
	
	private String type;
	
	private ControlLoopTargetType(String type) {
		this.type = type;
	}
	
	public String toString() {
		return this.type;
	}
	
	public static ControlLoopTargetType toType(String type) {
		if (VM.toString().equals(type)) {
			return VM;
		}
		if (VF.toString().equals(type)) {
			return VF;
		}
		if (VFC.toString().equals(type)) {
			return VFC;
		}
		if (ENODEB.toString().equals(type)) {
			return ENODEB;
		}	
		if (VNF.toString().equals(type)) {
			return VNF;
		}		

		return null;
	}
}
