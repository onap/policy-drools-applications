/*-
 * ============LICENSE_START=======================================================
 * util
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

package org.onap.policy.util;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import org.onap.policy.drools.core.PolicySession;

/**
 * This class provides a way to serialize/deserialize objects by locating
 * an already existing object on the remote end, and using that instead. It
 * is useful for objects that may be shared by multiple transactions.
 */
public class DroolsSessionCommonSerializable implements Serializable {
    private static final long serialVersionUID = 1L;

    // identifies an object within a Drools session
    private String name;

    // the object is serialized, but is only used if the corresponding object
    // on the remote end can't be located for some reason
    private Object object;

    /**
     * Constructor - initialize instance, and also store a reference to
     * the object in a 'PolicySession' adjunct.
     *
     * @param name identifies the object within the Drools session
     * @param object the shared object
     */
    public DroolsSessionCommonSerializable(String name, Object object) {
        this.name = name;
        this.object = object;

        // store a reference to the object within the adjunct
        // (only works if we can locate the Drools session)
        Adjunct adjunct = getAdjunct();
        if (adjunct != null) {
            adjunct.put(name, object);
        }
    }

    /**
     * Return this object as a String.
     *
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "DroolsSessionCommonSerializable[" + name + "]";
    }

    /**
     * This method runs as part of deserialization. If we are able to locate
     * the 'PolicySession' and adjunct, and fetch the replacement object from
     * the adjunct, that is used. Otherwise, the deserialized object is used
     * (which is likely a duplicate).
     *
     * @return the local named object (if available), or the deserialized
     *     object
     */
    private Object readResolve() throws ObjectStreamException {
        Adjunct adjunct = getAdjunct();
        Object replacementObject;

        if (adjunct != null && (replacementObject = adjunct.get(name)) != null) {
            // we found the adjunct, as well as the replacement object -- use
            // the replacement object
            return replacementObject;
        }

        // either we couldn't find the adjunct, or couldn't locate the object
        // within the adjunct
        return object;
    }

    /**
     * This method will:
     * 1) Locate the 'PolicySession' (only works from within the Drools thread),
     * 2) Find or create the adjunct.
     *
     * @return the adjunct, or 'null' if we aren't running within a
     *     Drools session
     */
    private Adjunct getAdjunct() {
        // PolicySession - this only works from within the Drools thread
        PolicySession session = PolicySession.getCurrentSession();
        Adjunct adjunct = null;
        if (session != null) {
            // we found the 'PolicySession' -- now, look for the adjunct
            Object adj = session.getAdjunct(Adjunct.class);
            if (!(adj instanceof Adjunct)) {
                // adjunct does not exist, or has the wrong type -- create it
                adjunct = new Adjunct();
                session.setAdjunct(Adjunct.class, adjunct);
            } else {
                // found the adjunct -- return it
                adjunct = (Adjunct) adj;
            }
        }
        return adjunct;
    }

    /* ============================================================ */

    /**
     * While 'HashMap&lt;String, Object&gt;' could be used directly instead of defining
     * a subclass, you can't do run-time type checking of a parameterized type.
     * As a result, the 'getAdjunct' method (above) would get compile-time
     * warnings.
     */
    private static class Adjunct extends HashMap<String, Object> {
        private static final long serialVersionUID = 1L;
    }
}
