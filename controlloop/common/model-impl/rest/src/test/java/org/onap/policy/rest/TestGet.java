/*-
 * ============LICENSE_START=======================================================
 * rest
 * ================================================================================
 * 
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

package org.onap.policy.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.onap.policy.rest.RESTManager.Pair;

public class TestGet {

    @Test(expected = NullPointerException.class)
    public void testUrlNull() {
        RESTManager mgr = new RESTManager();
        mgr.get(null, "user", null, null);
    }

    @Test
    public void testUsernameNull() {
        RESTManager mgr = new RESTManager();

        Pair<Integer, String> result = mgr.get("http://www.example.org", null, null, null);
        assertEquals((Integer)200, result.a);
        assertTrue(result.b != null);
        assertTrue(result.b.length() > 0);
    }

    @Test
    public void testUrlExampleOrg() {
        RESTManager mgr = new RESTManager();

        Pair<Integer, String> result = mgr.get("http://www.example.org", "user", null, null);
        assertEquals((Integer)200, result.a);
        assertTrue(result.b != null);
        assertTrue(result.b.length() > 0);
    }
}