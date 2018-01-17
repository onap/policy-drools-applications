/*-
 * ============LICENSE_START=======================================================
 * sdc
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

package org.onap.policy.sdc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.UUID;
import org.junit.Test;

public class Test_Resource {

  @Test
  public void testConstructors() {
    Resource r = new Resource();
    assertEquals(null, r.getResourceUUID());
    assertEquals(null, r.getResourceInvariantUUID());
    assertEquals(null, r.getResourceName());
    assertEquals(null, r.getResourceType());
    assertEquals(null, r.getResourceVersion());

    UUID uuid = UUID.randomUUID();
    r = new Resource(uuid);
    assertEquals(uuid, r.getResourceUUID());
    assertEquals(null, r.getResourceInvariantUUID());
    assertEquals(null, r.getResourceName());
    assertEquals(null, r.getResourceType());
    assertEquals(null, r.getResourceVersion());

    String name = "constTest";
    r = new Resource(name, ResourceType.CP);
    assertEquals(null, r.getResourceUUID());
    assertEquals(name, r.getResourceName());
    assertEquals(ResourceType.CP, r.getResourceType());
    assertEquals(null, r.getResourceInvariantUUID());
    assertEquals(null, r.getResourceVersion());

    uuid = UUID.randomUUID();
    UUID uuidInvariant = UUID.randomUUID();
    name = "constTestUUID";
    String version = "0.0.1";
    r = new Resource(uuid, uuidInvariant, name, version, ResourceType.VF);
    assertEquals(uuid, r.getResourceUUID());
    assertEquals(uuidInvariant, r.getResourceInvariantUUID());
    assertEquals(name, r.getResourceName());
    assertEquals(ResourceType.VF, r.getResourceType());
    assertEquals(version, r.getResourceVersion());

    Resource r2 = new Resource(r);
    assertEquals(uuid, r2.getResourceUUID());
    assertEquals(uuidInvariant, r2.getResourceInvariantUUID());
    assertEquals(name, r2.getResourceName());
    assertEquals(ResourceType.VF, r2.getResourceType());
    assertEquals(version, r2.getResourceVersion());
  }

  @Test
  public void testUUID() {
    Resource r = new Resource();
    UUID uuid = UUID.randomUUID();
    r.setResourceUUID(uuid);
    assertEquals(uuid, r.getResourceUUID());
  }

  @Test
  public void testInvariantUUID() {
    Resource r = new Resource();
    UUID uuid = UUID.randomUUID();
    r.setResourceInvariantUUID(uuid);
    assertEquals(uuid, r.getResourceInvariantUUID());
  }

  @Test
  public void testName() {
    Resource r = new Resource();
    String name = "nameTest";
    r.setResourceName(name);
    assertEquals(name, r.getResourceName());
  }

  @Test
  public void testVersion() {
    Resource r = new Resource();
    String version = "versionTest";
    r.setResourceVersion(version);
    assertEquals(version, r.getResourceVersion());
  }

  @Test
  public void testType() {
    Resource r = new Resource();
    r.setResourceType(ResourceType.CP);
    assertEquals(ResourceType.CP, r.getResourceType());
  }

  @Test
  public void testEquals() {
    Resource r1 = new Resource();
    Resource r2 = new Resource(r1);
    assertTrue(r1.equals(r2));
    assertTrue(r2.equals(r1));

    r1 = new Resource(UUID.randomUUID(), UUID.randomUUID(), "equalsTest", "1.1.1", ResourceType.VFC);
    r2 = new Resource(r1);
    assertTrue(r1.equals(r2));
    assertTrue(r2.equals(r1));
  }

  @Test
  public void testToString() {
    Resource r1 = new Resource();
    Resource r2 = new Resource(r1);
    assertEquals(r1.toString(), r2.toString());

    r1 = new Resource(UUID.randomUUID(), UUID.randomUUID(), "equalsTest", "1.1.1", ResourceType.VFC);
    r2 = new Resource(r1);
    assertEquals(r1.toString(), r2.toString());
  }

  @Test
  public void testHashCode() {
    Resource r1 = new Resource();
    Resource r2 = new Resource(r1);
    assertEquals(r1.hashCode(), r2.hashCode());

    r1 = new Resource(UUID.randomUUID(), UUID.randomUUID(), "equalsTest", "1.1.1", ResourceType.VFC);
    r2 = new Resource(r1);
    assertEquals(r1.hashCode(), r2.hashCode());
  }
}
