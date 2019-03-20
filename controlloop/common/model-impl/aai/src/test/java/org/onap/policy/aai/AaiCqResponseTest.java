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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.JAXBContextProperties;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.onap.aai.domain.yang.v15.CloudRegion;
import org.onap.aai.domain.yang.v15.GenericVnf;
import org.onap.aai.domain.yang.v15.ServiceInstance;
import org.onap.aai.domain.yang.v15.Tenant;
import org.onap.aai.domain.yang.v15.VfModule;
import org.onap.aai.domain.yang.v15.Vserver;
import org.onap.policy.aai.util.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AaiCqResponseTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AaiCqResponseTest.class);

    @Test
    public void test() {


        ArrayList<AaiCqInventoryResponseItem> responseList = new ArrayList<>();

        /*
         * JABX initial stuff
         */
        Map<String, Object> properties = new HashMap<>();
        properties.put(JAXBContextProperties.MEDIA_TYPE, "application/json");
        properties.put(JAXBContextProperties.JSON_INCLUDE_ROOT, false);

        JAXBContext jaxbContext = null;
        Unmarshaller unmarshaller = null;


        // Create a Context using the properties
        try {
            jaxbContext = JAXBContextFactory.createContext(new Class[] {Vserver.class, GenericVnf.class, VfModule.class,
                CloudRegion.class, ServiceInstance.class, Tenant.class}, properties);
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            LOGGER.info("Exception in creating JAXB Context and Unmarshaller", e);
        }

        /*
         * Read JSON String and add all AaiObjects
         */
        String responseString = "";
        try {
            responseString = new String(
                    Files.readAllBytes(new File("src/test/resources/org/onap/policy/aai/AaiCqResponse.json").toPath()));
        } catch (IOException e) {
            LOGGER.info("Exception in reading json string", e);
        }
        JSONObject responseObj = new JSONObject(responseString);
        JSONArray resultsArray = new JSONArray();
        if (responseObj.has("results")) {
            resultsArray = (JSONArray) responseObj.get("results");
        }
        for (int i = 0; i < resultsArray.length(); i++) {
            // Object is a vserver
            if (resultsArray.getJSONObject(i).has("vserver")) {
                AaiCqInventoryResponseItem vserverItem = new AaiCqInventoryResponseItem();
                // Create the StreamSource by creating StringReader using the
                // JSON input
                StreamSource json = new StreamSource(
                        new StringReader(resultsArray.getJSONObject(i).getJSONObject("vserver").toString()));

                // Getting the employee pojo again from the json
                Vserver vserver = null;
                try {
                    vserver = unmarshaller.unmarshal(json, Vserver.class).getValue();
                } catch (JAXBException e) {
                    LOGGER.info("JAXB exception in unmarshalling vserver", e);
                }

                vserverItem.setVserver(vserver);
                responseList.add(vserverItem);
            }

            // Object is a Generic VNF
            if (resultsArray.getJSONObject(i).has("generic-vnf")) {
                AaiCqInventoryResponseItem genericVnfItem = new AaiCqInventoryResponseItem();
                // Create the StreamSource by creating StringReader using the
                // JSON input
                StreamSource json = new StreamSource(
                        new StringReader(resultsArray.getJSONObject(i).getJSONObject("generic-vnf").toString()));

                // Getting the employee pojo again from the json
                GenericVnf genericVnf = null;
                try {
                    genericVnf = unmarshaller.unmarshal(json, GenericVnf.class).getValue();
                } catch (JAXBException e) {
                    LOGGER.info("JAXBException while unmarshalling generic vnf", e);
                }

                genericVnfItem.setGenericVnf(genericVnf);
                responseList.add(genericVnfItem);
            }

            // Object is a Service Instance
            if (resultsArray.getJSONObject(i).has("service-instance")) {
                AaiCqInventoryResponseItem serviceInstanceItem = new AaiCqInventoryResponseItem();
                // Create the StreamSource by creating StringReader using the
                // JSON input
                StreamSource json = new StreamSource(
                        new StringReader(resultsArray.getJSONObject(i).getJSONObject("service-instance").toString()));

                // Getting the employee pojo again from the json
                ServiceInstance serviceInstance = null;
                try {
                    serviceInstance = unmarshaller.unmarshal(json, ServiceInstance.class).getValue();
                } catch (JAXBException e) {
                    LOGGER.info("JAXB exception while unmarshalling service instance", e);
                }

                serviceInstanceItem.setServiceInstance(serviceInstance);
                responseList.add(serviceInstanceItem);
            }

            // Object is a VF Module
            if (resultsArray.getJSONObject(i).has("vf-module")) {
                AaiCqInventoryResponseItem vfModuleItem = new AaiCqInventoryResponseItem();
                // Create the StreamSource by creating StringReader using the
                // JSON input
                StreamSource json = new StreamSource(
                        new StringReader(resultsArray.getJSONObject(i).getJSONObject("vf-module").toString()));

                // Getting the employee pojo again from the json
                VfModule vfModule = null;
                try {
                    vfModule = unmarshaller.unmarshal(json, VfModule.class).getValue();
                } catch (JAXBException e) {
                    LOGGER.info("JAXB Exception while unmarshalling vf module", e);
                }

                vfModuleItem.setVfModule(vfModule);
                responseList.add(vfModuleItem);
            }

            // Object is a CloudRegion
            if (resultsArray.getJSONObject(i).has("cloud-region")) {
                AaiCqInventoryResponseItem cloudRegionItem = new AaiCqInventoryResponseItem();
                // Create the StreamSource by creating StringReader using the
                // JSON input
                StreamSource json = new StreamSource(
                        new StringReader(resultsArray.getJSONObject(i).getJSONObject("cloud-region").toString()));

                // Getting the employee pojo again from the json
                CloudRegion cloudRegion = null;
                try {
                    cloudRegion = unmarshaller.unmarshal(json, CloudRegion.class).getValue();
                } catch (JAXBException e) {
                    LOGGER.info("JAXB Exception while unmarshalling cloud region", e);
                }

                cloudRegionItem.setCloudRegion(cloudRegion);
                responseList.add(cloudRegionItem);
            }

            // Object is a Tenant
            if (resultsArray.getJSONObject(i).has("tenant")) {
                AaiCqInventoryResponseItem tenantItem = new AaiCqInventoryResponseItem();
                // Create the StreamSource by creating StringReader using the
                // JSON input
                StreamSource json = new StreamSource(
                        new StringReader(resultsArray.getJSONObject(i).getJSONObject("tenant").toString()));

                // Getting the employee pojo again from the json
                Tenant tenant = null;
                try {
                    tenant = unmarshaller.unmarshal(json, Tenant.class).getValue();
                } catch (JAXBException e) {
                    LOGGER.info("JAXB Exception while unmarshalling tenant", e);
                }

                tenantItem.setTenant(tenant);
                responseList.add(tenantItem);
            }

        }
        AaiCqResponse aaiCqResponse = new AaiCqResponse();
        aaiCqResponse.setInventoryResponseItems(responseList);
        assertNotNull(aaiCqResponse);
        assertNotNull(aaiCqResponse.getInventoryResponseItems());
        LOGGER.info(Serialization.gsonPretty.toJson(aaiCqResponse));
    }

    @Test
    public void testConstructor() {
        /*
         * Read JSON String and add all AaiObjects
         */

        try {
            String responseString = "";
            responseString = new String(
                    Files.readAllBytes(new File("src/test/resources/org/onap/policy/aai/AaiCqResponse.json").toPath()));

            AaiCqResponse aaiCqResponse;
            aaiCqResponse = new AaiCqResponse(responseString);
            assertNotNull(aaiCqResponse);
            assertNotNull(aaiCqResponse.getInventoryResponseItems());
            LOGGER.info(Serialization.gsonPretty.toJson(aaiCqResponse));
        } catch (JAXBException e) {
            LOGGER.info("JAXB Exception while unmarshalling response JSON string", e);
        } catch (IOException e) {
            LOGGER.info("IO exception in reading response string", e);
        }

    }

}
