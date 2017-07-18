package org.onap.policy.controlloop.policy.guard;

import java.util.LinkedList;

public class ControlLoopGuard {
	
	public Guard guard;
	
	public LinkedList<GuardPolicy> guards;
	
	public ControlLoopGuard() {
		
	}
	
	public ControlLoopGuard(ControlLoopGuard CLGuard) {
		this.guard = new Guard();
		this.guards = new LinkedList<GuardPolicy>(CLGuard.guards);
	}
	
	@Override
	public String toString() {
		return "Guard [guard=" + guard + ", GuardPolicies=" + guards + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((guard == null) ? 0 : guard.hashCode());
		result = prime * result + ((guards == null) ? 0 : guards.hashCode());
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
		ControlLoopGuard other = (ControlLoopGuard) obj;
		if (guard == null) {
			if (other.guard != null)
				return false;
		} else if (!guard.equals(other.guard))
			return false;
		if (guards == null) {
			if (other.guards != null)
				return false;
		} else if (!guards.equals(other.guards))
			return false;
		return true;
	}

	
}
