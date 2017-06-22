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

public class PNF implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3535108358668248501L;

	public String	PNFName;
	public PNFType	PNFType;
	
	public PNF() {
		
	}
	
	public PNF(PNF pnf) {
		this.PNFName = pnf.PNFName;
		this.PNFType = pnf.PNFType;
	}
	
	@Override
	public String toString() {
		return "PNF [PNFName=" + PNFName + ", PNFType=" + PNFType + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((PNFName == null) ? 0 : PNFName.hashCode());
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
		PNF other = (PNF) obj;
		if (PNFName == null) {
			if (other.PNFName != null)
				return false;
		} else if (!PNFName.equals(other.PNFName))
			return false;
		if (PNFType != other.PNFType)
			return false;
		return true;
	}
	
}
