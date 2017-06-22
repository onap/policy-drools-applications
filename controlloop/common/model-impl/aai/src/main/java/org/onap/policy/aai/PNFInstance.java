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

public class PNFInstance implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3694152433472165034L;
	
	public String	PNFName;
	public String	PNFInstanceName;
	public PNFType	PNFType;
	public String	PNFSerial;
	
	public PNFInstance() {
		
	}
	
	public PNFInstance(PNFInstance instance) {
		if (instance == null) {
			return;
		}
		this.PNFName = instance.PNFName;
		this.PNFInstanceName = instance.PNFInstanceName;
		this.PNFType = instance.PNFType;
		this.PNFSerial = instance.PNFSerial;
	}

	@Override
	public String toString() {
		return "PNFInstance [PNFName=" + PNFName + ", PNFInstanceName=" + PNFInstanceName + ", PNFType=" + PNFType
				+ ", PNFSerial=" + PNFSerial + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((PNFInstanceName == null) ? 0 : PNFInstanceName.hashCode());
		result = prime * result + ((PNFName == null) ? 0 : PNFName.hashCode());
		result = prime * result + ((PNFSerial == null) ? 0 : PNFSerial.hashCode());
		result = prime * result + ((PNFType == null) ? 0 : PNFType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PNFInstance other = (PNFInstance) obj;
		if (PNFInstanceName == null) {
			if (other.PNFInstanceName != null)
				return false;
		} else if (!PNFInstanceName.equals(other.PNFInstanceName))
			return false;
		if (PNFName == null) {
			if (other.PNFName != null)
				return false;
		} else if (!PNFName.equals(other.PNFName))
			return false;
		if (PNFSerial == null) {
			if (other.PNFSerial != null)
				return false;
		} else if (!PNFSerial.equals(other.PNFSerial))
			return false;
		if (PNFType != other.PNFType)
			return false;
		return true;
	}

}
