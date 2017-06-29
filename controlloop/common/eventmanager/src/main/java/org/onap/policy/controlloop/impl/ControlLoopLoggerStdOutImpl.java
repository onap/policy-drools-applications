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


package org.onap.policy.controlloop.impl;

import org.onap.policy.controlloop.ControlLoopLogger;

public class ControlLoopLoggerStdOutImpl implements ControlLoopLogger {

	public ControlLoopLoggerStdOutImpl() {
	}

	@Override
	public void info(String... parameters) {
		StringBuilder builder = new StringBuilder();
		for (String param : parameters) {
			builder.append(param);
			builder.append(" " );
		}
		System.out.println(builder.toString().trim());
	}

	@Override
	public void metrics(String... msgs) {
		this.info(msgs);
	}

	@Override
	public void metrics(Object obj) {
		this.info(obj.toString());
	}

}
