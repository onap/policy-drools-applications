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

package org.onap.policy.aai;

import java.io.Serializable;

import com.google.gson.annotations.SerializedName;

public class AAIGETVserverResponse extends AAIGETResponse implements Serializable {

	private static final long serialVersionUID = -6247505944905898870L;
 
	@SerializedName("vserver-id")
	private String vserverID;
	
	@SerializedName("vserver-name")
	private String vserverName;
	
	@SerializedName("vserver-name2")
	private String vserverName2;
	
	@SerializedName("vserver-selflink")
	private String vserverSelflink;

	public String getVserverID() {
		return vserverID;
	}

	public String getVserverName() {
		return vserverName;
	}

	public String getVserverName2() {
		return vserverName2;
	}

	public String getVserverSelflink() {
		return vserverSelflink;
	}

	public void setVserverID(String vserverID) {
		this.vserverID = vserverID;
	}

	public void setVserverName(String vserverName) {
		this.vserverName = vserverName;
	}

	public void setVserverName2(String vserverName2) {
		this.vserverName2 = vserverName2;
	}

	public void setVserverSelflink(String vserverSelflink) {
		this.vserverSelflink = vserverSelflink;
	}
}
