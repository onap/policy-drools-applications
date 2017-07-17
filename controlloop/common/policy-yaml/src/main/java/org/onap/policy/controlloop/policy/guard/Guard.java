package org.onap.policy.controlloop.policy.guard;

public class Guard {

	public static String VERSION = "2.0.0";
	
	public final String version = VERSION;
	
	public Guard() {
		
	}
	
	@Override
	public String toString() {
		return "Guard [version=" + version + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		Guard other = (Guard) obj;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}
}
