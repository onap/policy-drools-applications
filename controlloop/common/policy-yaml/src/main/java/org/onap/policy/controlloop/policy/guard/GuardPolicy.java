package org.onap.policy.controlloop.policy.guard;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class GuardPolicy {

	public String id = UUID.randomUUID().toString();
	public String name;
	public String description;
	public MatchParameters match_parameters;
	public LinkedList<Constraint> limit_constraints;
	
	
public GuardPolicy() {
		
	}
	
	public GuardPolicy(String id) {
		this.id = id;
	}
	
	public GuardPolicy(String name, MatchParameters match_parameters) {
		this.name = name;
		System.out.println("match_parameters: " + match_parameters);
		this.match_parameters = new MatchParameters(match_parameters);
	}
	
	public GuardPolicy(String id, String name, String description, MatchParameters match_parameters) {
		this(name, match_parameters);
		this.id = id;
		this.description = description;
	}
	
	public GuardPolicy(String name, MatchParameters match_parameters, List<Constraint> limit_constraints) {
		this(name, match_parameters);
		if (limit_constraints != null) {
			this.limit_constraints = (LinkedList<Constraint>) Collections.unmodifiableList(limit_constraints);
		}
	}
	
	public GuardPolicy(String name, String description, MatchParameters match_parameters, List<Constraint> limit_constraints) {
		this(name, match_parameters, limit_constraints);
		this.description = description;
	}
	
	public GuardPolicy(String id, String name, String description, MatchParameters match_parameters, List<Constraint> limit_constraints) {
		this(name, description, match_parameters, limit_constraints);
		this.id = id;
	}
	
	
	
	
	
	
	public GuardPolicy(GuardPolicy policy) {
		this.id = policy.id;
		this.name = policy.name;
		this.description = policy.description;
		this.match_parameters = new MatchParameters(policy.match_parameters);
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
			
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return "GuardPolicy [id=" + id + ", name=" + name + ", description=" + description + ", match_parameters="
				+ match_parameters + ", limit_constraints=" + limit_constraints + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((limit_constraints == null) ? 0 : limit_constraints.hashCode());
		result = prime * result + ((match_parameters == null) ? 0 : match_parameters.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		if (limit_constraints == null) {
			if (other.limit_constraints != null)
				return false;
		} else if (!limit_constraints.equals(other.limit_constraints))
			return false;
		if (match_parameters == null) {
			if (other.match_parameters != null)
				return false;
		} else if (!match_parameters.equals(other.match_parameters))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	
}
