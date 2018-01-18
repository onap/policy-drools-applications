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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class VirtualControlLoopNotification extends ControlLoopNotification {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5354756047932144017L;

	private Map<String, String> aai = new HashMap<>();
	private Instant closedLoopAlarmStart;
	private Instant closedLoopAlarmEnd;

	public VirtualControlLoopNotification() {
	}

	public VirtualControlLoopNotification(VirtualControlLoopEvent event) {
		super(event);
		if (event == null) {
			return;
		}
		if (event.getAAI() != null) {
			this.setAAI(new HashMap<>(event.getAAI()));
		}
		this.closedLoopAlarmStart = event.getClosedLoopAlarmStart();
		this.closedLoopAlarmEnd = event.getClosedLoopAlarmEnd();
	}

	public Map<String, String> getAAI() {
		return aai;
	}

	public void setAAI(Map<String, String> aAI) {
		this.aai = aAI;
	}

	public Instant getClosedLoopAlarmStart() {
		return closedLoopAlarmStart;
	}

	public void setClosedLoopAlarmStart(Instant closedLoopAlarmStart) {
		this.closedLoopAlarmStart = closedLoopAlarmStart;
	}

	public Instant getClosedLoopAlarmEnd() {
		return closedLoopAlarmEnd;
	}

	public void setClosedLoopAlarmEnd(Instant closedLoopAlarmEnd) {
		this.closedLoopAlarmEnd = closedLoopAlarmEnd;
	}
}
