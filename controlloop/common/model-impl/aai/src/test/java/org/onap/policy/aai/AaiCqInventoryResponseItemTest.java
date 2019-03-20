/*-
 * ============LICENSE_START=======================================================
 *
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.aai;


import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.JAXBContextProperties;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(AaiCqInventoryResponseItemTest.class);
    private static final String JSONAPP = "application/json";

    /*
     * Test the creation of a Vserver from a JSON string
     */
    @Test
    public void testVserver() {
        AaiCqInventoryResponseItem vserverItem = new AaiCqInventoryResponseItem();
        String fileString = "";
        try {
            fileString = new String(
                    Files.readAllBytes(new File("src/test/resources/org/onap/policy/aai/AaiCqVserver.json").toPath()));
        } catch (IOException e) {
            LOGGER.info(e.getMessage(), e);
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put(JAXBContextProperties.MEDIA_TYPE, JSONAPP);
        properties.put(JAXBContextProperties.JSON_INCLUDE_ROOT, false);

        // Create a Context using the properties
        JAXBContext jaxbContext;
        Unmarshaller unmarshaller;
        try {
            jaxbContext =
                    JAXBContextFactory.createContext(new Class[] {Vserver.class, ObjectFactory.class}, properties);
            unmarshaller = jaxbContext.createUnmarshaller();
            // Create the StreamSource by creating StringReader using the JSON
            // input
            StreamSource json = new StreamSource(new StringReader(fileString));

            // Getting the vserver POJO again from the JSON
            Vserver vserver = unmarshaller.unmarshal(json, Vserver.class).getValue();

            vserverItem.setVserver(vserver);

            assertNotNull(vserverItem.getVserver());
            assertNotNull(vserverItem);
        } catch (JAXBException e) {
            LOGGER.info(e.getMessage(), e);
        }

        LOGGER.info(Serialization.gsonPretty.toJson(vserverItem));

    }

    /*
     * Test the creation of a Generic Vnf from a JSON string
     */
    @Test
    public void testGenericVnf() {
        AaiCqInventoryResponseItem genericVnfItem = new AaiCqInventoryResponseItem();
        String fileString = "";
        try {
            fileString = new String(Files
                    .readAllBytes(new File("src/test/resources/org/onap/policy/aai/AaiCqGenericVnf.json").toPath()));
        } catch (IOException e) {
            LOGGER.info(e.getMessage(), e);
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put(JAXBContextProperties.MEDIA_TYPE, JSONAPP);
        properties.put(JAXBContextProperties.JSON_INCLUDE_ROOT, false);

        // Create a Context using the properties
        JAXBContext jaxbContext;
        Unmarshaller unmarshaller;
        try {
            jaxbContext =
                    JAXBContextFactory.createContext(new Class[] {GenericVnf.class, ObjectFactory.class}, properties);
            unmarshaller = jaxbContext.createUnmarshaller();
            // Create the StreamSource by creating StringReader using the JSON
            // input
            StreamSource json = new StreamSource(new StringReader(fileString));

            // Getting the generic vnf POJO again from the JSON
            GenericVnf genericVnf = unmarshaller.unmarshal(json, GenericVnf.class).getValue();

            genericVnfItem.setGenericVnf(genericVnf);

            assertNotNull(genericVnfItem.getGenericVnf());
            assertNotNull(genericVnfItem);
        } catch (JAXBException e) {
            LOGGER.info(e.getMessage(), e);
        }

        LOGGER.info(Serialization.gsonPretty.toJson(genericVnfItem));

    }

    /*
     * Test the creation of a service instance from a JSON string
     */
    @Test
    public void testServiceInstance() {
        AaiCqInventoryResponseItem serviceInstanceItem = new AaiCqInventoryResponseItem();
        String fileString = "";
        try {
            fileString = new String(Files.readAllBytes(
                    new File("src/test/resources/org/onap/policy/aai/AaiCqServiceInstance.json").toPath()));
        } catch (IOException e) {
            LOGGER.info(e.getMessage(), e);
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put(JAXBContextProperties.MEDIA_TYPE, JSONAPP);
        properties.put(JAXBContextProperties.JSON_INCLUDE_ROOT, false);

        // Create a Context using the properties
        JAXBContext jaxbContext;
        Unmarshaller unmarshaller;
        try {
            jaxbContext = JAXBContextFactory.createContext(new Class[] {ServiceInstance.class, ObjectFactory.class},
                    properties);
            unmarshaller = jaxbContext.createUnmarshaller();
            // Create the StreamSource by creating StringReader using the JSON
            // input
            StreamSource json = new StreamSource(new StringReader(fileString));

            // Getting the service instance POJO again from the JSON
            ServiceInstance serviceInstance = unmarshaller.unmarshal(json, ServiceInstance.class).getValue();

            serviceInstanceItem.setServiceInstance(serviceInstance);

            assertNotNull(serviceInstanceItem.getServiceInstance());
            assertNotNull(serviceInstanceItem);
        } catch (JAXBException e) {
            LOGGER.info(e.getMessage(), e);
        }

        LOGGER.info(Serialization.gsonPretty.toJson(serviceInstanceItem));

    }

    /*
     * Test the creation of a vf module from a JSON string
     */
    @Test
    public void testVfModule() {
        AaiCqInventoryResponseItem vfModuleItem = new AaiCqInventoryResponseItem();
        String fileString = "";
        try {
            fileString = new String(
                    Files.readAllBytes(new File("src/test/resources/org/onap/policy/aai/AaiCqVfModule.json").toPath()));
        } catch (IOException e) {
            LOGGER.info(e.getMessage(), e);
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put(JAXBContextProperties.MEDIA_TYPE, JSONAPP);
        properties.put(JAXBContextProperties.JSON_INCLUDE_ROOT, false);

        // Create a Context using the properties
        JAXBContext jaxbContext;
        Unmarshaller unmarshaller;
        try {
            jaxbContext =
                    JAXBContextFactory.createContext(new Class[] {VfModule.class, ObjectFactory.class}, properties);
            unmarshaller = jaxbContext.createUnmarshaller();
            // Create the StreamSource by creating StringReader using the JSON
            // input
            StreamSource json = new StreamSource(new StringReader(fileString));

            // Getting the vf module POJO again from the JSON
            VfModule vfModule = unmarshaller.unmarshal(json, VfModule.class).getValue();

            vfModuleItem.setVfModule(vfModule);

            assertNotNull(vfModuleItem.getVfModule());
            assertNotNull(vfModuleItem);
        } catch (JAXBException e) {
            LOGGER.info(e.getMessage(), e);
        }

        LOGGER.info(Serialization.gsonPretty.toJson(vfModuleItem));

    }

    /*
     * Test the creation of a tenant from a JSON string
     */
    @Test
    public void testTenant() {
        AaiCqInventoryResponseItem tenantItem = new AaiCqInventoryResponseItem();
        String fileString = "";
        try {
            fileString = new String(
                    Files.readAllBytes(new File("src/test/resources/org/onap/policy/aai/AaiCqTenant.json").toPath()));
        } catch (IOException e) {
            LOGGER.info(e.getMessage(), e);
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put(JAXBContextProperties.MEDIA_TYPE, JSONAPP);
        properties.put(JAXBContextProperties.JSON_INCLUDE_ROOT, false);

        // Create a Context using the properties
        JAXBContext jaxbContext;
        Unmarshaller unmarshaller;
        try {
            jaxbContext = JAXBContextFactory.createContext(new Class[] {Tenant.class, ObjectFactory.class}, properties);
            unmarshaller = jaxbContext.createUnmarshaller();
            // Create the StreamSource by creating StringReader using the JSON
            // input
            StreamSource json = new StreamSource(new StringReader(fileString));

            // Getting the tenant POJO again from the JSON
            Tenant tenant = unmarshaller.unmarshal(json, Tenant.class).getValue();

            tenantItem.setTenant(tenant);

            assertNotNull(tenantItem.getTenant());
            assertNotNull(tenantItem);
        } catch (JAXBException e) {
            LOGGER.info(e.getMessage(), e);
        }

        LOGGER.info(Serialization.gsonPretty.toJson(tenantItem));

    }

    /*
     * Test the creation of a cloud region from a JSON string
     */
    @Test
    public void testCloudRegion() {
        AaiCqInventoryResponseItem cloudRegionItem = new AaiCqInventoryResponseItem();
        String fileString = "";
        try {
            fileString = new String(Files
                    .readAllBytes(new File("src/test/resources/org/onap/policy/aai/AaiCqCloudRegion.json").toPath()));
        } catch (IOException e) {
            LOGGER.info(e.getMessage(), e);
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put(JAXBContextProperties.MEDIA_TYPE, JSONAPP);
        properties.put(JAXBContextProperties.JSON_INCLUDE_ROOT, false);

        // Create a Context using the properties
        JAXBContext jaxbContext;
        Unmarshaller unmarshaller;
        try {
            jaxbContext =
                    JAXBContextFactory.createContext(new Class[] {CloudRegion.class, ObjectFactory.class}, properties);
            unmarshaller = jaxbContext.createUnmarshaller();
            // Create the StreamSource by creating StringReader using the JSON
            // input
            StreamSource json = new StreamSource(new StringReader(fileString));

            // Getting the cloud region POJO again from the JSON
            CloudRegion cloudRegion = unmarshaller.unmarshal(json, CloudRegion.class).getValue();

            cloudRegionItem.setCloudRegion(cloudRegion);

            assertNotNull(cloudRegionItem.getCloudRegion());
            assertNotNull(cloudRegionItem);
        } catch (JAXBException e) {
            LOGGER.info(e.getMessage(), e);
        }

        LOGGER.info(Serialization.gsonPretty.toJson(cloudRegionItem));

    }
}
