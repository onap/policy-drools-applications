/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018 Ericsson. All rights reserved.
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
package org.onap.policy.msb.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class MSBServiceExceptionTest {
	
	private static final String MESSAGE = "An error message";
	private static final Exception CAUSE = new NullPointerException();

	@Test
	public void testMSBServiceException() {
		MSBServiceException exception = new MSBServiceException();
		assertNull(exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	public void testMSBServiceException_with_message_cause_enableSuppression_writableStackTrace() {
		MSBServiceException exception = new MSBServiceException(MESSAGE, CAUSE, true, true);
		assertEquals(MESSAGE, exception.getMessage());
		assertEquals(CAUSE, exception.getCause());
	}

	@Test
	public void testMSBServiceException_with_message_cause() {
		MSBServiceException exception = new MSBServiceException(MESSAGE, CAUSE);
		assertEquals(MESSAGE, exception.getMessage());
		assertEquals(CAUSE, exception.getCause());
	}

	@Test
	public void testMSBServiceException_with_message() {
		MSBServiceException exception = new MSBServiceException(MESSAGE);
		assertEquals(MESSAGE, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	public void testMSBServiceException_with_cause() {
		MSBServiceException exception = new MSBServiceException(CAUSE);
		exception.getMessage();
		assertEquals(CAUSE.toString(), exception.getMessage());
		assertEquals(CAUSE, exception.getCause());
	}

}
