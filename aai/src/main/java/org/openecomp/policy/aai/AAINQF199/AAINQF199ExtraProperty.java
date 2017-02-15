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

import java.io.Serializable;

import com.google.gson.annotations.SerializedName;

public class AAINQF199ExtraProperty implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3861240617222397736L;
	
	@SerializedName("property-name")
	public String propertyName;
	
	@SerializedName("property-value")
	public String propertyValue;
	
	public AAINQF199ExtraProperty() {
	}
	
	public AAINQF199ExtraProperty(String propertyName, String propertyValue) {
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
	}

}
