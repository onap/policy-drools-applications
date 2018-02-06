/*******************************************************************************
 * Copyright 2017 ZTE, Inc. and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.onap.policy.msb.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Properties;

import org.onap.msb.sdk.discovery.common.RouteException;
import org.onap.msb.sdk.discovery.entity.MicroServiceFullInfo;
import org.onap.msb.sdk.discovery.entity.NodeInfo;
import org.onap.msb.sdk.httpclient.msb.MSBServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MSBServiceFactory implements Serializable {
	private static final long serialVersionUID = 4638414146278012425L;
	private static final Logger logger = LoggerFactory.getLogger(MSBServiceFactory.class);
    private static final String MSB_PROPERTY_FILE = "msb.policy.properties";
    private static final String MSB_IP = "msb.ip";
    private static final String MSB_PORT = "msb.port";
    private transient MSBServiceClient msbClient;
    private Properties properties;

    public MSBServiceFactory() throws MSBServiceException,IOException{
        this.init();
        this.msbClient = new MSBServiceClient(properties.getProperty(MSB_IP), Integer.parseInt(properties.getProperty(MSB_PORT)));
    }
    public MSBServiceFactory (MSBServiceClient msbClient) {
        this.msbClient = msbClient;
    }

    private void init() throws MSBServiceException,IOException  {
        properties = new Properties();
        String propertyFilePath = System.getProperty(MSB_PROPERTY_FILE);
        if (propertyFilePath == null) {
            throw new MSBServiceException("No msb.policy.properties specified.");
        }
        Path file = Paths.get(propertyFilePath);
        if (!file.toFile().exists()) {
            throw new MSBServiceException("No msb.policy.properties specified.");
        }

        if (!Files.isReadable(file)) {
            throw new MSBServiceException ("Repository is NOT readable: " + file.toAbsolutePath());
        }
        try(InputStream is = new FileInputStream(file.toFile())){
            properties.load(is);
        }
    }


    public Node getNode(String serviceName,String version){
        return this.build(serviceName,version);
    }

    public Node getNode(String actor){
        Node node = null;
        switch (actor) {
            case "AAI":
                node = this.build("aai-search","v11");
                return node;
            case "SO":
                node = this.build("so","v2");
                return node;
            case "VFC":
                node = this.build("nfvo-nslcm","v1");
                return node;
            default:
                logger.info("MSBServiceManager: policy has an unknown actor.");
        }
        return node;
    }

    private Node build(String serviceName,String version){
        Node node = new Node();
        node.setName(serviceName);
        try {
            MicroServiceFullInfo serviceInfo = msbClient.queryMicroServiceInfo(serviceName,version);
            Iterator<NodeInfo> iterator = serviceInfo.getNodes().iterator();
            while(iterator.hasNext()) {
                NodeInfo nodeInfo = iterator.next();
                node.setIp(nodeInfo.getIp());
                node.setPort(nodeInfo.getPort());
            }
        } catch (RouteException e) {
            logger.info("MSBServiceManager:",e);
        }
        return node;
    }
}
