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

import org.onap.msb.sdk.discovery.common.RouteException;
import org.onap.msb.sdk.discovery.entity.MicroServiceFullInfo;
import org.onap.msb.sdk.discovery.entity.NodeInfo;
import org.onap.msb.sdk.httpclient.msb.MSBServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Properties;


public class MSBServiceFactory {
    private static final Logger logger = LoggerFactory.getLogger(MSBServiceFactory.class);
    private static final String msbPropertyFile = "msb.policy.properties";
    private static final String MSB_IP = "msb.ip";
    private static final String MSB_PORT = "msb.port";
    private MSBServiceClient msbClient;
    private Properties properties;

    public MSBServiceFactory() throws Exception{
        this.intialize();
        this.msbClient = new MSBServiceClient(properties.getProperty(MSB_IP), Integer.parseInt(properties.getProperty(MSB_PORT)));
    }
    public MSBServiceFactory (String msbIp,int port) {
        this.msbClient = new MSBServiceClient(msbIp, port);
    }

    private void intialize() throws Exception {
        properties = new Properties();
        Path file = Paths.get(System.getProperty(msbPropertyFile));
        if (file == null) {
            throw new Exception("No msb.policy.properties specified.");
        }
        if (Files.notExists(file)) {
            throw new Exception("No msb.policy.properties specified.");
        }

        if (Files.isReadable(file) == false) {
            throw new Exception ("Repository is NOT readable: " + file.toAbsolutePath());
        }
        try (InputStream is = new FileInputStream(file.toFile())) {
            properties.load(is);
        }
    }


    public Node getNode(String serviceName,String version){
        return this.build(serviceName,version);
    }

    public Node getNode(String actor){
        Node node;
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
                node = new Node();
                logger.info("MSBServiceManager: policy has an unknown actor.");
        }
        return node;
    }

    private Node build(String serviceName,String version){
        Node node = new Node(serviceName);
        try {
            MicroServiceFullInfo serviceInfo = msbClient.queryMicroServiceInfo(serviceName,version);
            Iterator iterator = serviceInfo.getNodes().iterator();
            while(iterator.hasNext()) {
                NodeInfo nodeInfo = (NodeInfo)iterator.next();
                node.setIp(nodeInfo.getIp());
                node.setPort(nodeInfo.getPort());
            }
        } catch (RouteException e) {
            logger.info(e.getMessage());
        }
        return node;
    }
}
