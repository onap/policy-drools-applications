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

package org.openecomp.policy.aai.AAINQF199;

import java.util.UUID;

import org.openecomp.policy.aai.AAINQF199.AAINQF199Response;

public class AAINQF199ResponseWrapper {

	public UUID requestID;
	public AAINQF199Response aainqf199response;
	
	public AAINQF199ResponseWrapper() {
		
	}
	
	public AAINQF199ResponseWrapper(UUID requestID, AAINQF199Response aainqf199response){
		this.requestID = requestID;
		this.aainqf199response = aainqf199response;
	}
}
