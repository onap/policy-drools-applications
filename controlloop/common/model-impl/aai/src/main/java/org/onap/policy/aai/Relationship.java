package org.onap.policy.aai;

import com.google.gson.annotations.SerializedName;

public class Relationship {
	
	@SerializedName("related-to")
	public String relatedTo;
	@SerializedName("related-link")
	public String relatedLink; 
	
	@SerializedName("relationship-data")
	public RelationshipData relationshipData = new RelationshipData();
	
	@SerializedName("related-to-property")
	public RelatedToProperty relatedToProperty =  new RelatedToProperty();
	
	public Relationship() {
	}
}
