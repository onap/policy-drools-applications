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
import java.util.UUID;

public class GuardPolicy {

	public String id = UUID.randomUUID().toString();
	public String name;
	public String description;
	public String actor;
	public String recipe;
	public LinkedList<Constraint> limit_constraints;
	
	public GuardPolicy() {
		
	}
	
	public GuardPolicy(String id) {
		this.id = id;
	}
	
	public GuardPolicy(String name, String actor, String recipe) {
		this.name = name;
		this.actor = actor;
		this.recipe = recipe;
	}
	
	public GuardPolicy(String id, String name, String description, String actor, String recipe) {
		this(name, actor, recipe);
		this.id = id;
		this.description = description;
	}
	
	public GuardPolicy(String name, String actor, String recipe, List<Constraint> limit_constraints) {
		this(name, actor, recipe);
		if (limit_constraints != null) {
			this.limit_constraints = (LinkedList<Constraint>) Collections.unmodifiableList(limit_constraints);
		}
	}
	
	public GuardPolicy(String name, String description, String actor, String recipe, List<Constraint> limit_constraints) {
		this(name, actor, recipe, limit_constraints);
		this.description = description;
	}
	
	public GuardPolicy(String id, String name, String description, String actor, String recipe, List<Constraint> limit_constraints) {
		this(name, description, actor, recipe, limit_constraints);
		this.id = id;
	}
	
	public GuardPolicy(GuardPolicy policy) {
		this.id = policy.id;
		this.name = policy.name;
		this.description = policy.description;
		this.actor = policy.actor;
		this.recipe = policy.recipe;
		if (policy.limit_constraints != null) {
			this.limit_constraints = (LinkedList<Constraint>) Collections.unmodifiableList(policy.limit_constraints);
		}
	}
	
	public boolean isValid() {
		try {
			if (id == null) {
				throw new NullPointerException();
			}
			if (name == null) {
				throw new NullPointerException();
			}
			if (actor == null) {
				throw new NullPointerException();
			}
			if (recipe == null) {
				throw new NullPointerException();
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return "Policy [id=" + id + ", name=" + name + ", description=" + description + ", actor=" + actor + ", recipe="
				+ recipe + ", limit_constraints=" + limit_constraints + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((actor == null) ? 0 : actor.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((limit_constraints == null) ? 0 : limit_constraints.hashCode());
		result = prime * result + ((recipe == null) ? 0 : recipe.hashCode());
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
		GuardPolicy other = (GuardPolicy) obj;
		if (actor == null) {
			if (other.actor != null) 
				return false;
		} else if (!actor.equals(other.actor))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (limit_constraints == null) {
			if (other.limit_constraints != null)
				return false;
		} else if (!limit_constraints.equals(other.limit_constraints))
			return false;
		if (recipe == null) {
			if (other.recipe != null)
				return false;
		} else if (!recipe.equals(other.recipe))
			return false;
		return true;
	}
	
	
}
