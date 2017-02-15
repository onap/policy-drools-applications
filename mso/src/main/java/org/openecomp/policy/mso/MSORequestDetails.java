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

package org.openecomp.policy.mso;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class MSORequestDetails implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3283942659786236032L;
	
	@SerializedName("modelInfo")
	public MSOModelInfo modelInfo;
	
	@SerializedName("cloudConfiguration")
	public MSOCloudConfiguration cloudConfiguration;
	
	@SerializedName("requestInfo")
	public MSORequestInfo requestInfo;
	
	@SerializedName("subscriberInfo")
	public MSOSubscriberInfo subscriberInfo;
	
	@SerializedName("relatedInstanceList")
	public List<MSORelatedInstanceListElement> relatedInstanceList= new LinkedList<MSORelatedInstanceListElement>();	
	
	@SerializedName("requestParameters")
	public MSORequestParameters requestParameters;

	public MSORequestDetails() {
	}

}
