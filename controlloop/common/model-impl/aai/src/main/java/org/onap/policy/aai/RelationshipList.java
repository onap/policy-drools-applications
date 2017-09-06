package org.onap.policy.aai;

import com.google.gson.annotations.SerializedName;
import java.util.List; 
import java.util.LinkedList; 

public class RelationshipList {

	@SerializedName("relationship-list")
	public List<Relationship> relationshipList = new LinkedList<Relationship>();
}
