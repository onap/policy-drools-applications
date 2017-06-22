/*-
 * ============LICENSE_START=======================================================
 * policy-yaml
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

package org.onap.policy.controlloop.policy;

import java.util.LinkedList;

import org.onap.policy.aai.PNF;
import org.onap.policy.sdc.Resource;
import org.onap.policy.sdc.Service;

public class ControlLoop {
	
	public static String VERSION = "2.0.0";

	public String controlLoopName;
	public final String version = VERSION;
	public LinkedList<Service> services;
	public LinkedList<Resource> resources;
	public PNF	pnf;
	public String trigger_policy = FinalResult.FINAL_OPENLOOP.toString();
	public Integer timeout;
	public Boolean abatement = false;
	
	public ControlLoop() {
		
	}
	
	public ControlLoop(ControlLoop controlLoop) {
		this.controlLoopName = controlLoop.controlLoopName;
		this.services = new LinkedList<Service>();
		if (controlLoop.services != null) {
			for (Service service : controlLoop.services) {
				this.services.add(service);
			}
		}
		this.resources = new LinkedList<Resource>();
		if (controlLoop.resources != null) {
			for (Resource resource: controlLoop.resources) {
				this.resources.add(resource);
			}
		}
		if (controlLoop.pnf != null) {
			this.pnf = new PNF(controlLoop.pnf);
		}
		this.trigger_policy = controlLoop.trigger_policy;
		this.timeout = controlLoop.timeout;
		this.abatement = controlLoop.abatement;
	}
	@Override
	public String toString() {
		return "ControlLoop [controlLoopName=" + controlLoopName + ", version=" + version + ", services=" + services
				+ ", resources=" + resources + ", pnf=" + pnf + ", trigger_policy=" + trigger_policy + ", timeout="
				+ timeout + ", abatement=" + abatement + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((controlLoopName == null) ? 0 : controlLoopName.hashCode());
		result = prime * result + ((pnf == null) ? 0 : pnf.hashCode());
		result = prime * result + ((resources == null) ? 0 : resources.hashCode());
		result = prime * result + ((services == null) ? 0 : services.hashCode());
		result = prime * result + ((timeout == null) ? 0 : timeout.hashCode());
		result = prime * result + ((trigger_policy == null) ? 0 : trigger_policy.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		result = prime * result + ((abatement == null) ? 0 : abatement.hashCode());
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
		ControlLoop other = (ControlLoop) obj;
		if (controlLoopName == null) {
			if (other.controlLoopName != null)
				return false;
		} else if (!controlLoopName.equals(other.controlLoopName))
			return false;
		if (pnf == null) {
			if (other.pnf != null)
				return false;
		} else if (!pnf.equals(other.pnf))
			return false;
		if (resources == null) {
			if (other.resources != null)
				return false;
		} else if (!resources.equals(other.resources))
			return false;
		if (services == null) {
			if (other.services != null)
				return false;
		} else if (!services.equals(other.services))
			return false;
		if (timeout == null) {
			if (other.timeout != null)
				return false;
		} else if (!timeout.equals(other.timeout))
			return false;
		if (trigger_policy == null) {
			if (other.trigger_policy != null)
				return false;
		} else if (!trigger_policy.equals(other.trigger_policy))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		if (abatement == null) {
			if (other.abatement != null)
				return false;
		} else if (!abatement.equals(other.abatement))
			return false;
		return true;
	}
	
}
