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

package org.onap.policy.controlloop.policy.guard;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Constraint {

	public Integer num;
	//public String duration;
	public Map<String, String> duration;
	public Map<String, String> time_in_range;
	
	public LinkedList<String> blacklist;
	
	public Constraint() {
		
	}
	
	public Constraint(Integer num, Map<String, String> duration) {
		this.num = num;
		this.duration = duration;
	}
	
	public Constraint(List<String> blacklist) {
		this.blacklist = new LinkedList<String>(blacklist);
		
	}
	
	public Constraint(Integer num, Map<String, String> duration, List<String> blacklist) {
		this.num = num;
		this.duration = Collections.unmodifiableMap(duration);
		this.blacklist = new LinkedList<String>(blacklist);
	}
	
	public Constraint(Integer num, Map<String, String> duration, Map<String, String> time_in_range, List<String> blacklist) {
		//this(num, duration);
		if (duration != null) {
			this.duration = Collections.unmodifiableMap(duration);
		}
		if (time_in_range != null) {
			this.time_in_range = Collections.unmodifiableMap(time_in_range);
		}
		this.blacklist = new LinkedList<String>(blacklist);
	}
	
	public Constraint(Constraint constraint) {
		this.num = constraint.num;
		this.duration = constraint.duration;
		if (constraint.time_in_range != null) {
			this.time_in_range = Collections.unmodifiableMap(constraint.time_in_range);
		}
		this.blacklist = new LinkedList<String>(constraint.blacklist);
	}
	
	public boolean isValid() {
		try {
			if (num == null && duration != null) {
				throw new NullPointerException();
			}
			if (duration == null && num != null) {
				throw new NullPointerException();
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return "Constraint [num=" + num + ", duration=" + duration + ", time_in_range=" + time_in_range + ", blacklist=" + blacklist + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((num == null) ? 0 : num.hashCode());
		result = prime * result + ((duration == null) ? 0 : duration.hashCode());
		result = prime * result + ((time_in_range == null) ? 0 : time_in_range.hashCode());
		result = prime * result + ((blacklist == null) ? 0 : blacklist.hashCode());
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
		Constraint other = (Constraint) obj;
		if (num == null) {
			if (other.num != null) 
				return false;
		} else if (!num.equals(other.num))
			return false;
		if (duration == null) {
			if (other.duration != null)
				return false;
		} else if (!duration.equals(other.duration))
			return false;
		if (time_in_range == null) {
			if (other.time_in_range != null)
				return false;
		} else if (!time_in_range.equals(other.time_in_range))
			return false;
		if (blacklist == null) {
			if (other.blacklist != null)
				return false;
		} else if (!blacklist.equals(other.blacklist))
			return false;
		return true;
	}
}
