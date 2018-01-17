package org.onap.policy.sdc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.UUID;
import org.junit.Test;

public class Test_Service {

  @Test
  public void testConstructors() {
    Service s = new Service();
    assertEquals(null, s.getServiceUUID());
    assertEquals(null, s.getServiceInvariantUUID());
    assertEquals(null, s.getServiceName());
    assertEquals(null, s.getServiceVersion());

    UUID uuid = UUID.randomUUID();
    s = new Service(uuid);
    assertEquals(uuid, s.getServiceUUID());
    assertEquals(null, s.getServiceInvariantUUID());
    assertEquals(null, s.getServiceName());
    assertEquals(null, s.getServiceVersion());

    String name = "constTest";
    s = new Service(name);
    assertEquals(null, s.getServiceUUID());
    assertEquals(name, s.getServiceName());
    assertEquals(null, s.getServiceInvariantUUID());
    assertEquals(null, s.getServiceVersion());

    uuid = UUID.randomUUID();
    UUID uuidInvariant = UUID.randomUUID();
    name = "constTestUUID";
    String version = "0.0.1";
    s = new Service(uuid, uuidInvariant, name, version);
    assertEquals(uuid, s.getServiceUUID());
    assertEquals(uuidInvariant, s.getServiceInvariantUUID());
    assertEquals(name, s.getServiceName());
    assertEquals(version, s.getServiceVersion());

    Service s2 = new Service(s);
    assertEquals(uuid, s2.getServiceUUID());
    assertEquals(uuidInvariant, s2.getServiceInvariantUUID());
    assertEquals(name, s2.getServiceName());
    assertEquals(version, s2.getServiceVersion());
  }

  @Test
  public void testUUID() {
    Service s = new Service();
    UUID uuid = UUID.randomUUID();
    s.setServiceUUID(uuid);
    assertEquals(uuid, s.getServiceUUID());
  }

  @Test
  public void testInvariantUUID() {
    Service s = new Service();
    UUID uuid = UUID.randomUUID();
    s.setServiceInvariantUUID(uuid);
    assertEquals(uuid, s.getServiceInvariantUUID());
  }

  @Test
  public void testName() {
    Service s = new Service();
    String name = "nameTest";
    s.setServiceName(name);
    assertEquals(name, s.getServiceName());
  }

  @Test
  public void testVersion() {
    Service s = new Service();
    String version = "versionTest";
    s.setServiceVersion(version);
    assertEquals(version, s.getServiceVersion());
  }

  @Test
  public void testEquals() {
    Service s1 = new Service();
    Service s2 = new Service(s1);
    assertTrue(s1.equals(s2));
    assertTrue(s2.equals(s1));

    s1 = new Service(UUID.randomUUID(), UUID.randomUUID(), "equalsTest", "1.1.1");
    s2 = new Service(s1);
    assertTrue(s1.equals(s2));
    assertTrue(s2.equals(s1));
  }

  @Test
  public void testToString() {
    Service s1 = new Service();
    Service s2 = new Service(s1);
    assertEquals(s1.toString(), s2.toString());

    s1 = new Service(UUID.randomUUID(), UUID.randomUUID(), "equalsTest", "1.1.1");
    s2 = new Service(s1);
    assertEquals(s1.toString(), s2.toString());
  }

  @Test
  public void testHashCode() {
    Service s1 = new Service();
    Service s2 = new Service(s1);
    assertEquals(s1.hashCode(), s2.hashCode());

    s1 = new Service(UUID.randomUUID(), UUID.randomUUID(), "equalsTest", "1.1.1");
    s2 = new Service(s1);
    assertEquals(s1.hashCode(), s2.hashCode());
  }
}
