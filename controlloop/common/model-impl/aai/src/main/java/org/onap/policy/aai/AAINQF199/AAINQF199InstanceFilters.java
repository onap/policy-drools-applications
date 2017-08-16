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

package org.onap.policy.aai.AAINQF199;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class AAINQF199InstanceFilters implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8993824591338121185L;
	
	@SerializedName("instance-filter")
	public transient List<Map<String, Map<String, String>>> instanceFilter = new LinkedList<Map<String, Map<String, String>>>();

	public AAINQF199InstanceFilters() {
	}

}
