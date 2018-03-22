/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018 Ericsson. All rights reserved.
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

package org.onap.policy.msb.client;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NodeTest {

    @Test
    public void testSetAndGetName() {
        Node node = new Node();
        final String name = "myName";
        node.setName(name);
        assertEquals(name, node.getName());
    }

    @Test
    public void testSetAndGetIp() {
        Node node = new Node();
        final String ip = "127.0.0.1";
        node.setIp(ip);
        assertEquals(ip, node.getIp());
    }

    @Test
    public void testSetAndGetPort() {
        Node node = new Node();
        final String port = "1001";
        node.setPort(port);
        assertEquals(port, node.getPort());
    }

    @Test
    public void testToString() {
        Node node = new Node();
        final String name = "myName";
        final String ip = "127.0.0.1";
        final String port = "1001";
        node.setName(name);
        node.setIp(ip);
        node.setPort(port);
        assertEquals("Node{name='myName', ip='127.0.0.1', port='1001'}", node.toString());
    }

}
