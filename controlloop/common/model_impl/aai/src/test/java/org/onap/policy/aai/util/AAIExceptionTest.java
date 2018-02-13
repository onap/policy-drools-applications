/*-
 * ============LICENSE_START=======================================================
 * aai
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
package org.onap.policy.aai.util;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

public class AAIExceptionTest {

	@Test
	public void test() {
		assertNotNull(new AAIException());
		assertNotNull(new AAIException("message"));
		assertNotNull(new AAIException("message", new IOException()));
		assertNotNull(new AAIException("message", new IOException(), true, false));
		assertNotNull(new AAIException(new IOException()));
	}

}
