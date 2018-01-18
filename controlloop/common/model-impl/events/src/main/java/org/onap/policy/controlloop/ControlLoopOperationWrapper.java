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

import java.util.UUID;

public class ControlLoopOperationWrapper {

	private UUID requestID;
	private ControlLoopOperation operation;
	
	public ControlLoopOperationWrapper() {
		
	}
	
	public ControlLoopOperationWrapper(UUID requestID, ControlLoopOperation operation) {
		this.requestID = requestID;
		this.operation = operation;
	}

	public UUID getRequestID() {
		return requestID;
	}

	public void setRequestID(UUID requestID) {
		this.requestID = requestID;
	}

	public ControlLoopOperation getOperation() {
		return operation;
	}

	public void setOperation(ControlLoopOperation operation) {
		this.operation = operation;
	}
}
