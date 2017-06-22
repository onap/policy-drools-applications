/*-
 * ============LICENSE_START=======================================================
 * mso
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

package org.onap.policy.mso;

import java.io.Serializable;


import com.google.gson.annotations.SerializedName;

public class MSORequestInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3283942659786236032L;
	
	@SerializedName("instanceName")
	public String instanceName;
	
	@SerializedName("source")
	public String source;
	
	@SerializedName("productFamilyId")
	public String productFamilyId;
	
	@SerializedName("suppressRollback")
	public boolean suppressRollback;
	
	@SerializedName("billingAccountNumber")
	public String billingAccountNumber;
	
	@SerializedName("callbackUrl")
	public String callbackUrl;
	
	@SerializedName("correlator")
	public String correlator;
	
	@SerializedName("orderNumber")
	public String orderNumber;
	
	@SerializedName("orderVersion")
	public Integer orderVersion;
	

	public MSORequestInfo() {
	}

}
