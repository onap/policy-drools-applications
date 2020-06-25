/*-
 * ============LICENSE_START=======================================================
 * m2/base
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.m2.base;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.onap.policy.controlloop.ControlLoopEvent;
import org.onap.policy.controlloop.ControlLoopNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class maps the class of an incoming ONSET message into the
 * appropriate adapter. The default adapter is included here as well.
 */
public class OnsetAdapter implements Serializable {
    private static final long serialVersionUID = 1L;
    private static Logger logger = LoggerFactory.getLogger(OnsetAdapter.class);

    // table mapping onset message class to 'OnsetAdapter' instance
    private static Map<Class<?>, OnsetAdapter> map = new ConcurrentHashMap<>();

    /**
     * This method is called to add an entry to the table.
     *
     * @param clazz the class of the ONSET message
     * @param value an instance of 'OnsetAdapter' that should be
     *     associated with 'clazz'
     */
    public static void register(Class<?> clazz, OnsetAdapter value) {
        // only create an entry if one doesn't already exist
        map.putIfAbsent(clazz, value);
    }

    /**
     * Map an incoming event's class into the appropriate 'OnsetAdapter'
     * to use.
     *
     * @param event this is the onset event
     * @return an adapter appropriate for the 'event'
     */
    public static OnsetAdapter get(ControlLoopEvent event) {
        Class<?> clazz = event.getClass();
        OnsetAdapter rval = map.get(clazz);
        if (rval != null) {
            return rval;
        }

        // This algorithm below is generic, in the sense that it can be used
        // to find a "best match" for any class out of a set of classes
        // using the class inheritance relationships. In the general case,
        // it is possible that there could be multiple best matches, but this
        // can only happen if all of the matching keys are interfaces,
        // except perhaps one. If there are multiple matches,
        // one will be chosen "at random".

        // we need to look for the best match of 'clazz'
        HashSet<Class<?>> matches = new HashSet<>();
        Class<?> chosenMatch = null;
        synchronized (map) {
            for (Class<?> possibleMatch : map.keySet()) {
                if (possibleMatch.isAssignableFrom(clazz)) {
                    // we have a match -- see if it is the best match
                    boolean add = true;
                    for (Class<?> match : new ArrayList<Class<?>>(matches)) {
                        if (match.isAssignableFrom(possibleMatch)) {
                            // 'possibleMatch' is a better match than 'match'
                            matches.remove(match);
                        } else if (possibleMatch.isAssignableFrom(match)) {
                            // we already have a better match
                            add = false;
                            break;
                        }
                    }
                    if (add) {
                        matches.add(possibleMatch);
                    }
                }
            }
            if (!matches.isEmpty()) {
                // we have at least one match
                chosenMatch = matches.iterator().next();
                rval = map.get(chosenMatch);

                // add this entry back into the table -- this means we can
                // now use this cached entry, and don't have to run through
                // the algorithm again for this class
                map.put(clazz, rval);
            }
        }

        if (matches.isEmpty()) {
            logger.error("no matches for {}", clazz);
        } else if (matches.size() != 1) {
            logger.warn("multiple matches for {}: {} -- chose {}",
                clazz, matches, chosenMatch);
        }

        return rval;
    }

    /* ============================================================ */

    // the following code creates an initial entry in the table
    private static OnsetAdapter instance = new OnsetAdapter();

    static {
        register(ControlLoopEvent.class, instance);
    }

    // the new 'ControlLoopNotification' is abstract
    public static class BaseControlLoopNotification extends ControlLoopNotification {
        private static final long serialVersionUID = 1L;

        BaseControlLoopNotification(ControlLoopEvent event) {
            super(event);
        }
    }

    /**
     * This method is what all of the fuss is about -- we want to create
     * a 'ControlLoopNotification' instance compatible with the type of the
     * 'event' argument. This is the default implementation -- subclasses of
     * 'ControlLoopEvent' may have entries in the table that are specialized
     * generate objects that are a subclass of 'ControlLoopNotification'
     * appropriate for the transaction type.
     *
     * @param event this is the event in question
     * @return a 'ControlLoopNotification' instance based upon this event
     */
    public ControlLoopNotification createNotification(ControlLoopEvent event) {
        return new BaseControlLoopNotification(event);
    }
}
