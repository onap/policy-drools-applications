package org.onap.policy.controlloop.policy.guard;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Constraint {

	public Integer freq_limit_per_target;
	public Map<String, String> time_window;
	public Map<String, String> active_time_range;
	
	public LinkedList<String> blacklist;
	
	public Constraint() {
		
	}
	
	public Constraint(Integer freq_limit_per_target, Map<String, String> time_window) {
		this.freq_limit_per_target = freq_limit_per_target;
		if (time_window != null) {
			this.time_window = Collections.unmodifiableMap(time_window);
		}
	}
	
	public Constraint(List<String> blacklist) {
		this.blacklist = new LinkedList<String>(blacklist);
		
	}
	
	public Constraint(Integer freq_limit_per_target, Map<String, String> time_window, List<String> blacklist) {
		this.freq_limit_per_target = freq_limit_per_target;
		this.time_window = Collections.unmodifiableMap(time_window);
		this.blacklist = new LinkedList<String>(blacklist);
	}
	
	public Constraint(Integer freq_limit_per_target, Map<String, String> time_window, Map<String, String> active_time_range, List<String> blacklist) {
		this(freq_limit_per_target, time_window);
		if (active_time_range != null) {
			this.active_time_range = Collections.unmodifiableMap(active_time_range);
		}
		this.blacklist = new LinkedList<String>(blacklist);
	}
	
	public Constraint(Integer freq_limit_per_target, Map<String, String> time_window, Map<String, String> active_time_range) {
		this(freq_limit_per_target, time_window);
		if (active_time_range != null) {
			this.active_time_range = Collections.unmodifiableMap(active_time_range);
		}
	}
	
	public Constraint(Constraint constraint) {
		this.freq_limit_per_target = constraint.freq_limit_per_target;
		this.time_window = constraint.time_window;
		if (constraint.active_time_range != null) {
			this.active_time_range = Collections.unmodifiableMap(constraint.active_time_range);
		}
		this.blacklist = new LinkedList<String>(constraint.blacklist);
	}
	
	public boolean isValid() {
		//System.out.println("freq_limit_per_target: " + freq_limit_per_target + " time_window" + time_window );
		try {
			if (freq_limit_per_target == null && time_window != null) {
				throw new NullPointerException();
			}
			if (time_window == null && freq_limit_per_target != null) {
				throw new NullPointerException();
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return "Constraint [freq_limit_per_target=" + freq_limit_per_target + ", time_window=" + time_window + ", active_time_range=" + active_time_range + ", blacklist=" + blacklist + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((freq_limit_per_target == null) ? 0 : freq_limit_per_target.hashCode());
		result = prime * result + ((time_window == null) ? 0 : time_window.hashCode());
		result = prime * result + ((active_time_range == null) ? 0 : active_time_range.hashCode());
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
		if (freq_limit_per_target == null) {
			if (other.freq_limit_per_target != null) 
				return false;
		} else if (!freq_limit_per_target.equals(other.freq_limit_per_target))
			return false;
		if (time_window == null) {
			if (other.time_window != null)
				return false;
		} else if (!time_window.equals(other.time_window))
			return false;
		if (active_time_range == null) {
			if (other.active_time_range != null)
				return false;
		} else if (!active_time_range.equals(other.active_time_range))
			return false;
		if (blacklist == null) {
			if (other.blacklist != null)
				return false;
		} else if (!blacklist.equals(other.blacklist))
			return false;
		return true;
	}
}
