package org.onap.policy.aai;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.JAXBContextProperties;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.aai.domain.yang.v15.CloudRegion;
import org.onap.aai.domain.yang.v15.GenericVnf;
import org.onap.aai.domain.yang.v15.ObjectFactory;
import org.onap.aai.domain.yang.v15.ServiceInstance;
import org.onap.aai.domain.yang.v15.Tenant;
import org.onap.aai.domain.yang.v15.VfModule;
import org.onap.aai.domain.yang.v15.Vserver;
import org.onap.policy.aai.util.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AaiCqInventoryResponseItemTest {
	
    private static final Logger logger = LoggerFactory.getLogger(AaiCqInventoryResponseItemTest.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {}

    @AfterClass
    public static void tearDownAfterClass() throws Exception {}
    
    /*
     * Test the creation of a Vserver from a JSON string
     */
	@Test
	public void testVserver() throws Exception {
        AaiCqInventoryResponseItem vserverItem = new AaiCqInventoryResponseItem();
        String vserver_string = new String(Files
                .readAllBytes(new File("src/test/resources/org/onap/policy/aai/AaiCqVServer.json").toPath()));

        
        Map<String, Object> properties = new HashMap<>();
        properties.put(JAXBContextProperties.MEDIA_TYPE, "application/json");
        properties.put(JAXBContextProperties.JSON_INCLUDE_ROOT, false);
        
      //Create a Context using the properties
        JAXBContext jaxbContext = 
            JAXBContextFactory.createContext(new Class[]  {
               Vserver.class,    ObjectFactory.class}, properties);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
 

 
        // Create the StreamSource by creating StringReader using the JSON input
        StreamSource json = new StreamSource(new StringReader(vserver_string));
 
        // Getting the employee pojo again from the json
        Vserver vserver = unmarshaller.unmarshal(json, Vserver.class).getValue();

        vserverItem.setVserver(vserver);
        
        assertNotNull(vserverItem.getVserver());
        assertNotNull(vserverItem);

        logger.info(Serialization.gsonPretty.toJson(vserverItem));

	}
	
	/*
	 * Test the creation of GenericVnf from a JSON string
	 */
	@Test
	public void testGenericVnf() throws Exception {
		
        AaiCqInventoryResponseItem genericVnfItem = new AaiCqInventoryResponseItem();
        String file_string = new String(Files
                .readAllBytes(new File("src/test/resources/org/onap/policy/aai/AaiCqGenericVnf.json").toPath()));
        
        Map<String, Object> properties = new HashMap<>();
        properties.put(JAXBContextProperties.MEDIA_TYPE, "application/json");
        properties.put(JAXBContextProperties.JSON_INCLUDE_ROOT, false);
        
      //Create a Context using the properties
        JAXBContext jaxbContext = 
            JAXBContextFactory.createContext(new Class[]  {
               GenericVnf.class,    ObjectFactory.class}, properties);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
 

 
        // Create the StreamSource by creating StringReader using the JSON input
        StreamSource json = new StreamSource(new StringReader(file_string));
 
        // Getting the pojo again from the json
        GenericVnf generic_vnf = unmarshaller.unmarshal(json, GenericVnf.class).getValue();

        genericVnfItem.setGenericVnf(generic_vnf);
        
        assertNotNull(genericVnfItem.getGenericVnf());
        assertNotNull(genericVnfItem);

        logger.info(Serialization.gsonPretty.toJson(genericVnfItem));

	}
	
	/*
	 * Test the creation of a ServiceInstance from a JSON string
	 */
	@Test
	public void testServiceInstance() throws Exception {

        AaiCqInventoryResponseItem serviceInstanceItem = new AaiCqInventoryResponseItem();
        String file_string = new String(Files
                .readAllBytes(new File("src/test/resources/org/onap/policy/aai/AaiCqServiceInstance.json").toPath()));

        
        Map<String, Object> properties = new HashMap<>();
        properties.put(JAXBContextProperties.MEDIA_TYPE, "application/json");
        properties.put(JAXBContextProperties.JSON_INCLUDE_ROOT, false);
        
      //Create a Context using the properties
        JAXBContext jaxbContext = 
            JAXBContextFactory.createContext(new Class[]  {
               ServiceInstance.class,    ObjectFactory.class}, properties);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

 
        // Create the StreamSource by creating StringReader using the JSON input
        StreamSource json = new StreamSource(new StringReader(file_string));
 
        // Getting the pojo again from the json
        ServiceInstance service_instance = unmarshaller.unmarshal(json, ServiceInstance.class).getValue();

        serviceInstanceItem.setServiceInstance(service_instance);
        
        assertNotNull(serviceInstanceItem.getServiceInstance());
        assertNotNull(serviceInstanceItem);

        logger.info(Serialization.gsonPretty.toJson(serviceInstanceItem));

	}

	/*
	 * Test the creation of a VFModule from a JSON string
	 */
	@Test
	public void testVfModule() throws Exception {

        AaiCqInventoryResponseItem vfModuleItem = new AaiCqInventoryResponseItem();
        String file_string = new String(Files
                .readAllBytes(new File("src/test/resources/org/onap/policy/aai/AaiCqVfModule.json").toPath()));
        
        Map<String, Object> properties = new HashMap<>();
        properties.put(JAXBContextProperties.MEDIA_TYPE, "application/json");
        properties.put(JAXBContextProperties.JSON_INCLUDE_ROOT, false);
        
      //Create a Context using the properties
        JAXBContext jaxbContext = 
            JAXBContextFactory.createContext(new Class[]  {
               VfModule.class,    ObjectFactory.class}, properties);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
 

 
        // Create the StreamSource by creating StringReader using the JSON input
        StreamSource json = new StreamSource(new StringReader(file_string));
 
        // Getting the pojo again from the json
        VfModule vf_module = unmarshaller.unmarshal(json, VfModule.class).getValue();

        vfModuleItem.setVfModule(vf_module);
        
        assertNotNull(vfModuleItem.getVfModule());
        assertNotNull(vfModuleItem);

        logger.info(Serialization.gsonPretty.toJson(vfModuleItem));

	}
	
	/*
	 * Test the creation of a Tenant from JSON string
	 */
	@Test
	public void testTenant() throws Exception {

        AaiCqInventoryResponseItem tenantItem = new AaiCqInventoryResponseItem();
        String file_string = new String(Files
                .readAllBytes(new File("src/test/resources/org/onap/policy/aai/AaiCqTenant.json").toPath()));
        
        Map<String, Object> properties = new HashMap<>();
        properties.put(JAXBContextProperties.MEDIA_TYPE, "application/json");
        properties.put(JAXBContextProperties.JSON_INCLUDE_ROOT, false);
        
      //Create a Context using the properties
        JAXBContext jaxbContext = 
            JAXBContextFactory.createContext(new Class[]  {
               Tenant.class,    ObjectFactory.class}, properties);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
 

 
        // Create the StreamSource by creating StringReader using the JSON input
        StreamSource json = new StreamSource(new StringReader(file_string));
 
        // Getting the pojo again from the json
        Tenant tenant = unmarshaller.unmarshal(json, Tenant.class).getValue();

        tenantItem.setTenant(tenant);
        
        assertNotNull(tenantItem.getTenant());
        assertNotNull(tenantItem);

        logger.info(Serialization.gsonPretty.toJson(tenantItem));

	}
	
	/*
	 * Test the creation of CloudRegion from a json string
	 */
	@Test
	public void testCloudRegion() throws Exception {

        AaiCqInventoryResponseItem cloudRegionItem = new AaiCqInventoryResponseItem();
        String file_string = new String(Files
                .readAllBytes(new File("src/test/resources/org/onap/policy/aai/AaiCqCloudRegion.json").toPath()));

        
        Map<String, Object> properties = new HashMap<>();
        properties.put(JAXBContextProperties.MEDIA_TYPE, "application/json");
        properties.put(JAXBContextProperties.JSON_INCLUDE_ROOT, false);
        
      //Create a Context using the properties
        JAXBContext jaxbContext = 
            JAXBContextFactory.createContext(new Class[]  {
               CloudRegion.class,    ObjectFactory.class}, properties);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

 
        // Create the StreamSource by creating StringReader using the JSON input
        StreamSource json = new StreamSource(new StringReader(file_string));
 
        // Getting the pojo again from the json
        CloudRegion cloud_region = unmarshaller.unmarshal(json, CloudRegion.class).getValue();

        cloudRegionItem.setCloudRegion(cloud_region);
        
        assertNotNull(cloudRegionItem.getCloudRegion());
        assertNotNull(cloudRegionItem);

        logger.info(Serialization.gsonPretty.toJson(cloudRegionItem));

	}
	
}
