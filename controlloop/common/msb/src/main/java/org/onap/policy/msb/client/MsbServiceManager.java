/*-
 * ============LICENSE_START=======================================================
 * Copyright 2017-2018 ZTE, Inc. and others.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.msb.client;

import java.io.IOException;
import java.io.Serializable;

import org.onap.msb.sdk.httpclient.msb.MSBServiceClient;

public class MsbServiceManager implements Serializable {
    private static final long serialVersionUID = -2517971308551895215L;
    private MsbServiceFactory factory;

    public MsbServiceManager() throws MsbServiceException, IOException {
        this.factory = new MsbServiceFactory();
    }

    public MsbServiceManager(MSBServiceClient msbClient) {

        this.factory = new MsbServiceFactory(msbClient);
    }

    /**
     * Get the IP and port of the components registered in the MSB.
     * 
     * @param actor AAI or SO or VFC or SNDC
     * @return the node
     */
    public Node getNode(String actor) {

        return factory.getNode(actor);
    }

    /**
     * Get the IP and port of the components registered in the MSB.
     * 
     * @param serviceName the service name registered in the MSB
     * @param version the service version registered in the MSB
     * @return the node
     */
    public Node getNode(String serviceName, String version) {

        return factory.getNode(serviceName, version);
    }

}
