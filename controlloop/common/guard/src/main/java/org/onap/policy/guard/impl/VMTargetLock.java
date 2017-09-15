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

package org.onap.policy.guard.impl;

import java.io.Serializable;
import java.util.UUID;

import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.guard.LockCallback;
import org.onap.policy.guard.TargetLock;

public class VMTargetLock implements TargetLock, Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8795145054334409724L;
	private final UUID		lockID;
	private final TargetType 	targetType;
	private final String 	target;
	private final UUID 		requestID;
	private final transient LockCallback callback;

	public VMTargetLock(TargetType targetType, String target, UUID requestID, LockCallback callback) {
		this.lockID = UUID.randomUUID();
		this.targetType = targetType;
		this.target = target;
		this.requestID = requestID;
		this.callback = callback;
	}

	@Override
	public UUID		getLockID() {
		return this.lockID;
	}
	
	@Override
	public TargetType getTargetType() {
		return targetType;
	}

	@Override
	public String getTargetInstance() {
		return target;
	}
	
	@Override
	public UUID getRequestID() {
		return this.requestID;
	}

	public LockCallback getCallback() {
		return this.callback;
	}

	@Override
	public String toString() {
		return "VMTargetLock [lockID=" + lockID + ", targetType=" + targetType + ", target=" + target + ", requestID="
				+ requestID + "]";
	}

}
