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
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.junit.Test;
import org.onap.aai.domain.yang.v15.GenericVnf;
import org.onap.aai.domain.yang.v15.ServiceInstance;
import org.onap.aai.domain.yang.v15.VfModule;
import org.onap.aai.domain.yang.v15.Vserver;
import org.onap.policy.aai.util.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AaiCqResponseTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AaiCqResponseTest.class);
    private static final String CQ_RESPONSE_SAMPLE = "src/test/resources/org/onap/policy/aai/AaiCqResponse.json";
    private static final String JAXB_EXCEPTION = "JAXB Exception while unmarshalling response JSON string";
    private static final String IO_EXCEPTION = "IO exception in reading response string";

    @Test
    public void testConstructor() {
        /*
         * Read JSON String and add all AaiObjects
         */

        try {
            String responseString = "";
            responseString = new String(Files.readAllBytes(new File(CQ_RESPONSE_SAMPLE).toPath()));

            AaiCqResponse aaiCqResponse;
            aaiCqResponse = new AaiCqResponse(responseString);
            assertNotNull(aaiCqResponse);
            assertNotNull(aaiCqResponse.getInventoryResponseItems());
            LOGGER.info(Serialization.gsonPretty.toJson(aaiCqResponse));
        } catch (JAXBException e) {
            LOGGER.info(JAXB_EXCEPTION, e);
        } catch (IOException e) {
            LOGGER.info(IO_EXCEPTION, e);
        }

    }

    @Test
    public void testAaiMalformedCqResponse() {
        /*
         * Read JSON String and add all AaiObjects
         */

        try {
            String responseString = "";
            responseString = new String(Files.readAllBytes(
                    new File("src/test/resources/org/onap/policy/aai/AaiMalformedCqResponse.json").toPath()));

            AaiCqResponse aaiCqResponse;
            aaiCqResponse = new AaiCqResponse(responseString);
            for (Object aaiObj : aaiCqResponse.getInventoryResponseItems()) {
                assertNull(aaiObj);
            }
            LOGGER.info(Serialization.gsonPretty.toJson(aaiCqResponse));
        } catch (JAXBException e) {
            LOGGER.info(JAXB_EXCEPTION, e);
        } catch (IOException e) {
            LOGGER.info(IO_EXCEPTION, e);
        }

    }

    @Test
    public void testGetItemByList() {
        /*
         * Read JSON String and add all AaiObjects
         */

        try {
            String responseString = "";
            responseString = new String(Files.readAllBytes(new File(CQ_RESPONSE_SAMPLE).toPath()));

            AaiCqResponse aaiCqResponse;
            aaiCqResponse = new AaiCqResponse(responseString);
            ArrayList<Vserver> vs = (ArrayList<Vserver>) aaiCqResponse.getItemListByType(Vserver.class);
            assertNotNull(vs);
            LOGGER.info(vs.get(0).getVserverId());
        } catch (JAXBException e) {
            LOGGER.info(JAXB_EXCEPTION, e);
        } catch (IOException e) {
            LOGGER.info(IO_EXCEPTION, e);
        }

    }

    @Test
    public void testGetServiceInstance() {

        try {
            String responseString = "";
            responseString = new String(Files.readAllBytes(new File(CQ_RESPONSE_SAMPLE).toPath()));

            AaiCqResponse aaiCqResponse;
            aaiCqResponse = new AaiCqResponse(responseString);
            ServiceInstance si = aaiCqResponse.getServiceInstance();
            assertNotNull(si);
            LOGGER.info(si.getServiceInstanceName());
        } catch (JAXBException e) {
            LOGGER.info(JAXB_EXCEPTION, e);
        } catch (IOException e) {
            LOGGER.info(IO_EXCEPTION, e);
        }

    }



    @Test
    public void testGetGenericVnfs() {

        try {
            String responseString = "";
            responseString = new String(Files.readAllBytes(new File(CQ_RESPONSE_SAMPLE).toPath()));

            AaiCqResponse aaiCqResponse;
            aaiCqResponse = new AaiCqResponse(responseString);
            List<GenericVnf> genericVnfList = aaiCqResponse.getGenericVnfs();
            assertNotNull(genericVnfList);
            for (GenericVnf genVnf : genericVnfList) {
                LOGGER.info(genVnf.getVnfName());
            }
        } catch (JAXBException e) {
            LOGGER.info(JAXB_EXCEPTION, e);
        } catch (IOException e) {
            LOGGER.info(IO_EXCEPTION, e);
        }

    }



    @Test
    public void testGetDefaultGenericVnf() {

        try {
            String responseString = "";
            responseString = new String(Files.readAllBytes(new File(CQ_RESPONSE_SAMPLE).toPath()));

            AaiCqResponse aaiCqResponse;
            aaiCqResponse = new AaiCqResponse(responseString);
            GenericVnf genVnf = aaiCqResponse.getDefaultGenericVnf();
            assertNotNull(genVnf);
            LOGGER.info(genVnf.getVnfName());
        } catch (JAXBException e) {
            LOGGER.info(JAXB_EXCEPTION, e);
        } catch (IOException e) {
            LOGGER.info(IO_EXCEPTION, e);
        }

    }

    @Test
    public void testGetGenericVnfByName() {

        try {
            String responseString = "";
            responseString = new String(Files.readAllBytes(new File(CQ_RESPONSE_SAMPLE).toPath()));

            AaiCqResponse aaiCqResponse;
            aaiCqResponse = new AaiCqResponse(responseString);
            GenericVnf genVnf = aaiCqResponse.getGenericVnfByVnfName("TestVM-Vnf-0201-1");
            assertNotNull(genVnf);
            LOGGER.info(genVnf.getVnfName());
        } catch (JAXBException e) {
            LOGGER.info(JAXB_EXCEPTION, e);
        } catch (IOException e) {
            LOGGER.info(IO_EXCEPTION, e);
        }

    }


    @Test
    public void testGetAllVfModules() {

        try {
            String responseString = "";
            responseString = new String(Files.readAllBytes(new File(CQ_RESPONSE_SAMPLE).toPath()));

            AaiCqResponse aaiCqResponse;
            aaiCqResponse = new AaiCqResponse(responseString);
            List<VfModule> vfModuleList = aaiCqResponse.getAllVfModules();
            assertNotNull(vfModuleList);
            for (VfModule vfMod : vfModuleList) {
                LOGGER.info(vfMod.getVfModuleName());
            }
        } catch (JAXBException e) {
            LOGGER.info(JAXB_EXCEPTION, e);
        } catch (IOException e) {
            LOGGER.info(IO_EXCEPTION, e);
        }

    }


    @Test
    public void testGetVfModuleByVfModuleName() {

        try {
            String responseString = "";
            responseString = new String(Files.readAllBytes(new File(CQ_RESPONSE_SAMPLE).toPath()));

            AaiCqResponse aaiCqResponse;
            aaiCqResponse = new AaiCqResponse(responseString);
            VfModule vfModule = aaiCqResponse.getVfModuleByVfModuleName("vLoadBalancerMS-0211-1");
            assertNotNull(vfModule);
            LOGGER.info(vfModule.getVfModuleName());
        } catch (JAXBException e) {
            LOGGER.info(JAXB_EXCEPTION, e);
        } catch (IOException e) {
            LOGGER.info(IO_EXCEPTION, e);
        }

    }

    @Test
    public void testGetDefaultVfModule() {

        try {
            String responseString = "";
            responseString = new String(Files.readAllBytes(new File(CQ_RESPONSE_SAMPLE).toPath()));

            AaiCqResponse aaiCqResponse;
            aaiCqResponse = new AaiCqResponse(responseString);
            VfModule vfModule = aaiCqResponse.getDefaultVfModule();
            assertNotNull(vfModule);
            LOGGER.info(vfModule.getVfModuleName());
        } catch (JAXBException e) {
            LOGGER.info(JAXB_EXCEPTION, e);
        } catch (IOException e) {
            LOGGER.info(IO_EXCEPTION, e);
        }

    }

    @Test
    public void testGetVserver() {

        try {
            String responseString = "";
            responseString = new String(Files.readAllBytes(new File(CQ_RESPONSE_SAMPLE).toPath()));

            AaiCqResponse aaiCqResponse;
            aaiCqResponse = new AaiCqResponse(responseString);
            Vserver vserver = aaiCqResponse.getVserver();
            assertNotNull(vserver);
            LOGGER.info(vserver.getVserverName());
        } catch (JAXBException e) {
            LOGGER.info(JAXB_EXCEPTION, e);
        } catch (IOException e) {
            LOGGER.info(IO_EXCEPTION, e);
        }

    }

}
