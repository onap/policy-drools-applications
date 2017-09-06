package org.onap.policy.aai;

import java.util.LinkedList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class RelatedToProperty {
  
	@SerializedName("related-to-property")
	public List<RelatedToPropertyItem> relatedTo = new LinkedList<RelatedToPropertyItem>();

	public RelatedToProperty() {
	}
}

 