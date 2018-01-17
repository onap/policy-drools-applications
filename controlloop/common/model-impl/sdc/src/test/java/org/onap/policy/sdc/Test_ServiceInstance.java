package org.onap.policy.sdc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.UUID;
import org.junit.Test;

public class Test_ServiceInstance {

  @Test
  public void testConstructors() {
    ServiceInstance si = new ServiceInstance();
    assertEquals(null, si.getServiceInstanceName());
    assertEquals(null, si.getServiceUUID());
    assertEquals(null, si.getServiceInstanceUUID());
    assertEquals(null, si.getServiceName());
    assertEquals(null, si.getPersonaModelUUID());
    assertEquals(null, si.getWidgetModelUUID());
    assertEquals(null, si.getWidgetModelVersion());

    ServiceInstance si2 = new ServiceInstance((ServiceInstance)null);
    assertEquals(null, si2.getServiceInstanceName());
    assertEquals(null, si2.getServiceUUID());
    assertEquals(null, si2.getServiceInstanceUUID());
    assertEquals(null, si2.getServiceName());
    assertEquals(null, si2.getPersonaModelUUID());
    assertEquals(null, si2.getWidgetModelUUID());
    assertEquals(null, si2.getWidgetModelVersion());

    si2 = new ServiceInstance(si);
    assertEquals(si2.getServiceInstanceName(), si.getServiceInstanceName());
    assertEquals(si2.getServiceUUID(), si.getServiceUUID());
    assertEquals(si2.getServiceInstanceUUID(), si.getServiceInstanceUUID());
    assertEquals(si2.getServiceName(), si.getServiceName());
    assertEquals(si2.getPersonaModelUUID(), si.getPersonaModelUUID());
    assertEquals(si2.getWidgetModelUUID(), si.getWidgetModelUUID());
    assertEquals(si2.getWidgetModelVersion(), si.getWidgetModelVersion());
  }

  @Test
  public void testInstanceName() {
    ServiceInstance si = new ServiceInstance();
    String name = "nameTestInstance";
    si.setServiceInstanceName(name);;
    assertEquals(name, si.getServiceInstanceName());
  }
 
  @Test
  public void testUUID() {
    ServiceInstance si = new ServiceInstance();
    UUID uuid = UUID.randomUUID();
    si.setServiceUUID(uuid);
    assertEquals(uuid, si.getServiceUUID());
  }

  @Test
  public void testInstanceUUID() {
    ServiceInstance si = new ServiceInstance();
    UUID uuid = UUID.randomUUID();
    si.setServiceInstanceUUID(uuid);
    assertEquals(uuid, si.getServiceInstanceUUID());
  }

  @Test
  public void testName() {
    ServiceInstance si = new ServiceInstance();
    String name = "nameTest";
    si.setServiceName(name);
    assertEquals(name, si.getServiceName());
  }

  @Test
  public void testPersonaModelUUID() {
    ServiceInstance si = new ServiceInstance();
    UUID uuid = UUID.randomUUID();
    si.setPersonaModelUUID(uuid);
    assertEquals(uuid, si.getPersonaModelUUID());
  }

  @Test
  public void testWidgetModelUUID() {
    ServiceInstance si = new ServiceInstance();
    UUID uuid = UUID.randomUUID();
    si.setWidgetModelUUID(uuid);
    assertEquals(uuid, si.getWidgetModelUUID());
  }

  @Test
  public void testWidgetModelVersion() {
    ServiceInstance si = new ServiceInstance();
    String version = "2.2.2";
    si.setWidgetModelVersion(version);;
    assertEquals(version, si.getWidgetModelVersion());
  }

  @Test
  public void testEquals() {
    ServiceInstance si1 = new ServiceInstance();
    ServiceInstance si2 = new ServiceInstance(si1);
    assertTrue(si1.equals(si2));
    assertTrue(si2.equals(si1));

    si1.setServiceInstanceName("instance");
    si1.setServiceName("service");
    si1.setServiceInstanceUUID(UUID.randomUUID());
    si1.setServiceUUID(UUID.randomUUID());
    si1.setPersonaModelUUID(UUID.randomUUID());
    si1.setWidgetModelUUID(UUID.randomUUID());
    si1.setWidgetModelVersion("3.3.3");
    si2 = new ServiceInstance(si1);
    assertTrue(si1.equals(si2));
    assertTrue(si2.equals(si1));
  }

  @Test
  public void testToString() {
    ServiceInstance si1 = new ServiceInstance();
    ServiceInstance si2 = new ServiceInstance(si1);
    assertEquals(si1.toString(), si2.toString());

    si1.setServiceInstanceName("instance");
    si1.setServiceName("service");
    si1.setServiceInstanceUUID(UUID.randomUUID());
    si1.setServiceUUID(UUID.randomUUID());
    si1.setPersonaModelUUID(UUID.randomUUID());
    si1.setWidgetModelUUID(UUID.randomUUID());
    si1.setWidgetModelVersion("3.3.3");
    si2 = new ServiceInstance(si1);
    assertEquals(si1.toString(), si2.toString());
  }

  @Test
  public void testHashCode() {
    ServiceInstance si1 = new ServiceInstance();
    ServiceInstance si2 = new ServiceInstance(si1);
    assertEquals(si1.hashCode(), si2.hashCode());

    si1.setServiceInstanceName("instance");
    si1.setServiceName("service");
    si1.setServiceInstanceUUID(UUID.randomUUID());
    si1.setServiceUUID(UUID.randomUUID());
    si1.setPersonaModelUUID(UUID.randomUUID());
    si1.setWidgetModelUUID(UUID.randomUUID());
    si1.setWidgetModelVersion("3.3.3");
    si2 = new ServiceInstance(si1);
    assertEquals(si1.hashCode(), si2.hashCode());
  }
}
