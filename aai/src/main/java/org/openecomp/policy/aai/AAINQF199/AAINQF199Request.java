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


/*
 * 
 * {
           "query-parameters": {
               "named-query": {
                 "named-query-uuid": "f199cb88-5e69-4b1f-93e0-6f257877d066"
               }
           },
           "instance-filters": {
                "instance-filter": [ 
             		{
                		"vserver": {
                                "vserver-name": "dfw1lb01lb01"
               			}
             		}
            	]
           }
}
 
 * 
 */

public class AAINQF199Request implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3283942659786236032L;
	
	@SerializedName("query-parameters")
	public AAINQF199QueryParameters queryParameters;
	@SerializedName("instance-filters")
	public AAINQF199InstanceFilters instanceFilters;

	public AAINQF199Request() {
	}

}
