/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.controlloop.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.junit.Test;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;

public class ControlLoopUtilsTest {

    @Test
    public void testToControlLoopParams() throws Exception {
        String policy = new String(Files.readAllBytes(Paths.get("src/test/resources/tosca-policy-legacy-vcpe.json")));
        ToscaPolicy toscaPolicy = new StandardCoder().decode(policy, ToscaPolicy.class);

        ControlLoopParams params = ControlLoopUtils.toControlLoopParams(toscaPolicy);
        assertEquals("ControlLoop-vCPE-48f0c2c3-a172-4192-9ae3-052274181b6e", params.getClosedLoopControlName());
        assertEquals(toscaPolicy.getName(), params.getPolicyName());
        assertEquals(toscaPolicy.getVersion(), params.getPolicyVersion());
        assertEquals(toscaPolicy.getType() + ":" + toscaPolicy.getVersion(), params.getPolicyScope());
        assertSame(toscaPolicy, params.getToscaPolicy());

        assertNull(ControlLoopUtils.toControlLoopParams(null));
    }

    @Test
    public void testToObject() {
        Map<String, String> map = Map.of("abc", "def", "ghi", "jkl");
        Properties props = new Properties();
        props.putAll(map);

        // with empty prefix
        Map<String, Object> result = ControlLoopUtils.toObject(props, "");
        assertEquals(map, result);

        // with dotted prefix - other items skipped
        map = Map.of("pfx.abc", "def", "ghi", "jkl", "pfx.mno", "pqr", "differentpfx.stu", "vwx");
        props.clear();
        props.putAll(Map.of("pfx.abc", "def", "ghi", "jkl", "pfx.mno", "pqr", "differentpfx.stu", "vwx"));
        result = ControlLoopUtils.toObject(props, "pfx.");
        map = Map.of("abc", "def", "mno", "pqr");
        assertEquals(map, result);

        // undotted prefix - still skips other items
        result = ControlLoopUtils.toObject(props, "pfx");
        assertEquals(map, result);
    }

    @Test
    public void testSetProperty() {
        // one, two, and three components in the name, the last two with subscripts
        Map<String, Object> map = Map.of("one", "one.abc", "two.def", "two.ghi", "three.jkl.mno[0]", "three.pqr",
                        "three.jkl.mno[1]", "three.stu");
        Properties props = new Properties();
        props.putAll(map);

        Map<String, Object> result = ControlLoopUtils.toObject(props, "");
        // @formatter:off
        map = Map.of(
                "one", "one.abc",
                "two", Map.of("def", "two.ghi"),
                "three", Map.of("jkl",
                            Map.of("mno",
                                List.of("three.pqr", "three.stu"))));
        // @formatter:on
        assertEquals(map, result);
    }

    @Test
    public void testGetNode() {
        Map<String, Object> map = Map.of("abc[0].def", "node.ghi", "abc[0].jkl", "node.mno", "abc[1].def", "node.pqr");
        Properties props = new Properties();
        props.putAll(map);

        Map<String, Object> result = ControlLoopUtils.toObject(props, "");
        // @formatter:off
        map = Map.of(
                "abc",
                    List.of(
                        Map.of("def", "node.ghi", "jkl", "node.mno"),
                        Map.of("def", "node.pqr")
                    ));
        // @formatter:on
        assertEquals(map, result);

    }

    @Test
    public void testExpand() {
        // add subscripts out of order
        Properties props = makeProperties("abc[2]", "expand.def", "abc[1]", "expand.ghi");

        Map<String, Object> result = ControlLoopUtils.toObject(props, "");
        // @formatter:off
        Map<String,Object> map =
            Map.of("abc",
                Arrays.asList(null, "expand.ghi", "expand.def"));
        // @formatter:on
        assertEquals(map, result);

    }

    @Test
    public void testGetObject() {
        // first value is primitive, while second is a map
        Properties props = makeProperties("object.abc", "object.def", "object.abc.ghi", "object.jkl");

        Map<String, Object> result = ControlLoopUtils.toObject(props, "");
        // @formatter:off
        Map<String,Object> map =
            Map.of("object",
                Map.of("abc",
                    Map.of("ghi", "object.jkl")));
        // @formatter:on
        assertEquals(map, result);
    }

    @Test
    public void testGetArray() {
        // first value is primitive, while second is an array
        Properties props = makeProperties("array.abc", "array.def", "array.abc[0].ghi", "array.jkl");

        Map<String, Object> result = ControlLoopUtils.toObject(props, "");
        // @formatter:off
        Map<String,Object> map =
            Map.of("array",
                Map.of("abc",
                    List.of(
                        Map.of("ghi", "array.jkl"))));
        // @formatter:on
        assertEquals(map, result);

    }

    /**
     * Makes properties containing the specified key/value pairs. The property set returns
     * names in the order listed.
     *
     * @return a new properties containing the specified key/value pairs
     */
    private Properties makeProperties(String key1, String value1, String key2, String value2) {
        // control the order in which the names are returned
        List<String> keyList = List.of(key1, key2);

        Set<String> keySet = new AbstractSet<>() {
            @Override
            public Iterator<String> iterator() {
                return keyList.iterator();
            }

            @Override
            public int size() {
                return 2;
            }
        };

        Properties props = new Properties() {
            private static final long serialVersionUID = 1L;

            @Override
            public Set<String> stringPropertyNames() {
                return keySet;
            }
        };

        props.putAll(Map.of(key1, value1, key2, value2));

        return props;
    }
}
