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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.onap.policy.controlloop.processor.ControlLoopProcessor;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Control Loop Utils.
 */
public class ControlLoopUtils {

    public static final Logger logger = LoggerFactory.getLogger(ControlLoopUtils.class);
    private static final Pattern NAME_PAT = Pattern.compile("(.*)\\[(\\d{1,3})\\]");

    private ControlLoopUtils() {
        super();
    }

    /**
     * Get a Control Loop Parameters object from a Tosca Policy.
     */
    public static ControlLoopParams toControlLoopParams(ToscaPolicy policy) {

        /* No exceptions are thrown to keep the DRL simpler */

        try {
            return new ControlLoopProcessor(policy).getControlLoopParams();
        } catch (ControlLoopException | RuntimeException e) {
            logger.error("Invalid Policy because of {}: {}", e.getMessage(), policy, e);
            return null;
        }
    }

    // TODO move this to policy-common/utils

    /**
     * Converts a set of properties to a Map. Supports json-path style property names with
     * "." separating components, where components may have an optional subscript.
     *
     * @param properties properties to be converted
     * @param prefix properties whose names begin with this prefix are included. The
     *        prefix is stripped from the name before adding the value to the map
     * @return a hierarchical map representing the properties
     */
    public static Map<String, Object> toObject(Properties properties, String prefix) {
        String dottedPrefix = prefix + (prefix.isEmpty() || prefix.endsWith(".") ? "" : ".");
        int pfxlen = dottedPrefix.length();

        Map<String, Object> map = new LinkedHashMap<>();

        for (String name : properties.stringPropertyNames()) {
            if (name.startsWith(dottedPrefix)) {
                String[] components = name.substring(pfxlen).split("[.]");
                setProperty(map, components, properties.getProperty(name));
            }
        }

        return map;
    }

    /**
     * Sets a property within a hierarchical map.
     *
     * @param map map into which the value should be placed
     * @param names property name components
     * @param value value to be placed into the map
     */
    private static void setProperty(Map<String, Object> map, String[] names, String value) {
        Map<String, Object> node = map;

        final int lastComp = names.length - 1;

        // process all but the final component
        for (int comp = 0; comp < lastComp; ++comp) {
            node = getNode(node, names[comp]);
        }

        // process the final component
        String name = names[lastComp];
        Matcher matcher = NAME_PAT.matcher(name);

        if (!matcher.matches()) {
            // no subscript
            node.put(name, value);
            return;
        }

        // subscripted
        List<Object> array = getArray(node, matcher.group(1));
        int index = Integer.parseInt(matcher.group(2));
        expand(array, index);
        array.set(index, value);
    }

    /**
     * Gets a node.
     *
     * @param map map from which to get the object
     * @param name name of the element to get from the map, with an optional subscript
     * @return a Map
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> getNode(Map<String, Object> map, String name) {
        Matcher matcher = NAME_PAT.matcher(name);

        if (!matcher.matches()) {
            // no subscript
            return getObject(map, name);
        }

        // subscripted
        List<Object> array = getArray(map, matcher.group(1));
        int index = Integer.parseInt(matcher.group(2));
        expand(array, index);

        Object item = array.get(index);
        if (item instanceof Map) {
            return (Map<String, Object>) item;

        } else {
            LinkedHashMap<String, Object> result = new LinkedHashMap<>();
            array.set(index, result);
            return result;
        }
    }

    /**
     * Ensures that an array's size is large enough to hold the specified element.
     *
     * @param array array to be expanded
     * @param index index of the desired element
     */
    private static void expand(List<Object> array, int index) {
        while (array.size() <= index) {
            array.add(null);
        }
    }

    /**
     * Gets an object (i.e., Map) from a map. If the particular element is not a Map, then
     * it is replaced with an empty Map.
     *
     * @param map map from which to get the object
     * @param name name of the element to get from the map, without any subscript
     * @return a Map
     */
    private static Map<String, Object> getObject(Map<String, Object> map, String name) {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) map.compute(name, (key, value) -> {
            if (value instanceof Map) {
                return value;
            } else {
                return new LinkedHashMap<>();
            }
        });

        return result;
    }

    /**
     * Gets an array from a map. If the particular element is not an array, then it is
     * replaced with an empty array.
     *
     * @param map map from which to get the array
     * @param name name of the element to get from the map, without any subscript
     * @return an array
     */
    private static List<Object> getArray(Map<String, Object> map, String name) {
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) map.compute(name, (key, value) -> {
            if (value instanceof List) {
                return value;
            } else {
                return new ArrayList<>();
            }
        });

        return result;
    }
}
