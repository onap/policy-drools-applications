package org.onap.policy.aai;


import java.io.Serializable;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.JAXBContextProperties;
import org.json.JSONArray;
import org.json.JSONObject;
import org.onap.aai.domain.yang.v15.CloudRegion;
import org.onap.aai.domain.yang.v15.GenericVnf;
import org.onap.aai.domain.yang.v15.ServiceInstance;
import org.onap.aai.domain.yang.v15.Tenant;
import org.onap.aai.domain.yang.v15.VfModule;
import org.onap.aai.domain.yang.v15.Vserver;

import com.google.gson.annotations.SerializedName;

public class AaiCqResponse implements Serializable {
	
	private static final long serialVersionUID = 1L;
	@SerializedName("results")
    private List<AaiCqInventoryResponseItem> inventoryResponseItems = new LinkedList<>();
    
	/*
	 * Default constructor
	 */
	public AaiCqResponse(){
		
	}
	
	/*
	 * Creates a custom query response from a valid json string
	 * @param A&AI Custom Query response JSON string
	 */
	public AaiCqResponse(String json_string) throws JAXBException {
		/*
		 * JABX initital stuff
		 */
		Map<String, Object> properties = new HashMap<>();
        properties.put(JAXBContextProperties.MEDIA_TYPE, "application/json");
        properties.put(JAXBContextProperties.JSON_INCLUDE_ROOT, false);
        
        //Create a Context using the properties
        JAXBContext jaxbContext = 
            JAXBContextFactory.createContext(new Class[]  {
               Vserver.class, GenericVnf.class, VfModule.class, CloudRegion.class, ServiceInstance.class, Tenant.class}, properties);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		
        /*
         * Read JSON String and add all AaiObjects
         */

		JSONObject response_obj = new JSONObject(json_string);
		JSONArray results_array = new JSONArray();
		if (response_obj.has("results")){
			results_array = (JSONArray) response_obj.get("results");
		}
		for (int i = 0; i < results_array.length(); i++){
			//Object is a vserver
			if (results_array.getJSONObject(i).has("vserver")){
				AaiCqInventoryResponseItem vserverItem = new AaiCqInventoryResponseItem();
				// Create the StreamSource by creating StringReader using the JSON input
		        StreamSource json = new StreamSource(new StringReader(results_array.getJSONObject(i).getJSONObject("vserver").toString()));
		 
		        // Getting the employee pojo again from the json
		        Vserver vserver = unmarshaller.unmarshal(json, Vserver.class).getValue();

		        vserverItem.setVserver(vserver);
		        this.inventoryResponseItems.add(vserverItem);
			}
			
			//Object is a Generic VNF
			if (results_array.getJSONObject(i).has("generic-vnf")){
				AaiCqInventoryResponseItem genericVnfItem = new AaiCqInventoryResponseItem();
				// Create the StreamSource by creating StringReader using the JSON input
		        StreamSource json = new StreamSource(new StringReader(results_array.getJSONObject(i).getJSONObject("generic-vnf").toString()));
		 
		        // Getting the employee pojo again from the json
		        GenericVnf genericVnf = unmarshaller.unmarshal(json, GenericVnf.class).getValue();

		        genericVnfItem.setGenericVnf(genericVnf);
		        this.inventoryResponseItems.add(genericVnfItem);
			}
			
			//Object is a Service Instance
			if (results_array.getJSONObject(i).has("service-instance")){
				AaiCqInventoryResponseItem serviceInstanceItem = new AaiCqInventoryResponseItem();
				// Create the StreamSource by creating StringReader using the JSON input
		        StreamSource json = new StreamSource(new StringReader(results_array.getJSONObject(i).getJSONObject("service-instance").toString()));
		 
		        // Getting the employee pojo again from the json
		        ServiceInstance serviceInstance = unmarshaller.unmarshal(json, ServiceInstance.class).getValue();

		        serviceInstanceItem.setServiceInstance(serviceInstance);
		        this.inventoryResponseItems.add(serviceInstanceItem);
			}
			
			//Object is a VF Module
			if (results_array.getJSONObject(i).has("vf-module")){
				AaiCqInventoryResponseItem vfModuleItem = new AaiCqInventoryResponseItem();
				// Create the StreamSource by creating StringReader using the JSON input
		        StreamSource json = new StreamSource(new StringReader(results_array.getJSONObject(i).getJSONObject("vf-module").toString()));
		 
		        // Getting the employee pojo again from the json
		        VfModule vfModule = unmarshaller.unmarshal(json, VfModule.class).getValue();

		        vfModuleItem.setVfModule(vfModule);
		        this.inventoryResponseItems.add(vfModuleItem);
			}
			
			//Object is a CloudRegion
			if (results_array.getJSONObject(i).has("cloud-region")){
				AaiCqInventoryResponseItem cloudRegionItem = new AaiCqInventoryResponseItem();
				// Create the StreamSource by creating StringReader using the JSON input
		        StreamSource json = new StreamSource(new StringReader(results_array.getJSONObject(i).getJSONObject("cloud-region").toString()));
		 
		        // Getting the employee pojo again from the json
		        CloudRegion cloudRegion = unmarshaller.unmarshal(json, CloudRegion.class).getValue();

		        cloudRegionItem.setCloudRegion(cloudRegion);
		        this.inventoryResponseItems.add(cloudRegionItem);
			}
			
			//Object is a Tenant
			if (results_array.getJSONObject(i).has("tenant")){
				AaiCqInventoryResponseItem tenantItem = new AaiCqInventoryResponseItem();
				// Create the StreamSource by creating StringReader using the JSON input
		        StreamSource json = new StreamSource(new StringReader(results_array.getJSONObject(i).getJSONObject("tenant").toString()));
		 
		        // Getting the employee pojo again from the json
		        Tenant tenant = unmarshaller.unmarshal(json, Tenant.class).getValue();

		        tenantItem.setTenant(tenant);
		        this.inventoryResponseItems.add(tenantItem);
			}
			
		}

	} 
	


    public List<AaiCqInventoryResponseItem> getInventoryResponseItems() {
        return inventoryResponseItems;
    }

    public void setInventoryResponseItems(List<AaiCqInventoryResponseItem> inventoryResponseItems) {
        this.inventoryResponseItems = inventoryResponseItems;
    }

}
