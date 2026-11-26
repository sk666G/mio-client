package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.utils.world.Waypoint;
import org.junit.jupiter.api.Test;

class WaypointRegistryTest {
   @Test
   void discoveriesWithTheSameDescriptionKeepDistinctLocationsAndReuseAnExistingLocation() {
      WaypointRegistry registry = new WaypointRegistry();
      Waypoint first = registry.registerDiscoveryWaypoint(new Waypoint("Storage", 10, 64, 20, "overworld", "example.org"));
      Waypoint second = registry.registerDiscoveryWaypoint(new Waypoint("storage", 30, 64, 40, "overworld", "EXAMPLE.ORG"));
      assertEquals("Storage", first.getName());
      assertEquals("storage (2)", second.getName());
      assertEquals(30, second.getX());
      assertSame(second, registry.registerDiscoveryWaypoint(new Waypoint("Storage", 30, 64, 40, "minecraft:overworld", "example.org")));
      assertEquals(2, registry.getRegistry().size());
      Waypoint otherServer = registry.registerDiscoveryWaypoint(new Waypoint("Storage", 30, 64, 40, "overworld", "other.example"));
      assertEquals("Storage", otherServer.getName());
   }

   @Test
   void discoveryNamesSkipExistingSuffixesAndDoNotMergeDimensions() {
      WaypointRegistry registry = new WaypointRegistry();
      registry.registerWaypoint(new Waypoint("Storage", 10, 64, 20, "overworld", "example.org"));
      registry.registerWaypoint(new Waypoint("Storage (2)", 30, 64, 40, "overworld", "example.org"));
      Waypoint nether = registry.registerDiscoveryWaypoint(new Waypoint("Storage", 10, 64, 20, "nether", "example.org"));
      assertEquals("Storage (3)", nether.getName());
      assertEquals(3, registry.getRegistry().size());
   }

   @Test
   void malformedStoredDimensionsDoNotPreventCreatingDiscoveries() {
      WaypointRegistry registry = new WaypointRegistry();
      registry.registerWaypoint(new Waypoint("Storage", 10, 64, 20, null, "example.org"));
      assertNotNull(registry.registerDiscoveryWaypoint(new Waypoint("Storage", 10, 64, 20, "overworld", "example.org")));
      assertNull(registry.registerDiscoveryWaypoint(null));
   }

   @Test
   void identityMatchesNameAndServerCaseInsensitively() {
      Waypoint first = new Waypoint("Home", 1.0, 64.0, 2.0, "minecraft:overworld", "example.org");
      Waypoint sameIdentity = new Waypoint("home", 50.0, 70.0, 50.0, "minecraft:the_nether", "EXAMPLE.ORG");

      assertTrue(WaypointRegistry.hasSameIdentity(first, sameIdentity));
   }

   @Test
   void identityDoesNotCompareServerAgainstWaypointName() {
      Waypoint stored = new Waypoint("base", 1.0, 64.0, 2.0, "minecraft:overworld", "server.example");
      Waypoint requested = new Waypoint("base", 1.0, 64.0, 2.0, "minecraft:overworld", "different.example");

      assertFalse(WaypointRegistry.hasSameIdentity(stored, requested));
   }

   @Test
   void waypointLookupMatchesNameAndServerCaseInsensitively() {
      Waypoint waypoint = new Waypoint("Home", 1.0, 64.0, 2.0, "minecraft:overworld", "example.org");

      assertTrue(waypoint.matches("home", "EXAMPLE.ORG"));
      assertFalse(waypoint.matches("mine", "example.org"));
      assertFalse(waypoint.matches("home", "other.example"));
   }

   @Test
   void reloadReplacesRemovedWaypointsAndCollapsesDuplicates() {
      WaypointRegistry registry = new WaypointRegistry();
      assertTrue(registry.registerWaypoint(new Waypoint(
         "Old", 1.0, 64.0, 1.0, "minecraft:overworld", "example.org"
      )));

      com.google.gson.JsonObject root = new com.google.gson.JsonObject();
      com.google.gson.JsonArray waypoints = new com.google.gson.JsonArray();
      waypoints.add(dev.schizo.client.ClientAccess.gson.toJsonTree(new Waypoint(
         "Home", 10.0, 64.0, 10.0, "minecraft:overworld", "example.org"
      )));
      waypoints.add(dev.schizo.client.ClientAccess.gson.toJsonTree(new Waypoint(
         "home", 20.0, 70.0, 20.0, "minecraft:the_nether", "EXAMPLE.ORG"
      )));
      root.add("waypoints", waypoints);

      registry.fromJson(root);

      assertEquals(1, registry.getRegistry().size());
      assertTrue(((java.util.List<Waypoint>)registry.getRegistry()).stream()
         .anyMatch(waypoint -> waypoint.matches("HOME", "example.org")));
      assertFalse(((java.util.List<Waypoint>)registry.getRegistry()).stream()
         .anyMatch(waypoint -> waypoint.getName().equalsIgnoreCase("Old")));
   }

   @Test
   void malformedWaypointRootPreservesExistingState() {
      WaypointRegistry registry = new WaypointRegistry();
      Waypoint existing = new Waypoint(
         "Keep", 1.0, 64.0, 1.0, "minecraft:overworld", "example.org"
      );
      assertTrue(registry.registerWaypoint(existing));

      assertThrows(IllegalArgumentException.class, () -> registry.fromJson(new com.google.gson.JsonObject()));

      assertEquals(1, registry.getRegistry().size());
      assertSame(existing, ((java.util.List<Waypoint>)registry.getRegistry()).getFirst());
   }

   @Test
   void identityRequiresBothNameAndServer() {
      Waypoint stored = new Waypoint("base", 1.0, 64.0, 2.0, "minecraft:overworld", "server.example");
      Waypoint differentName = new Waypoint("mine", 1.0, 64.0, 2.0, "minecraft:overworld", "server.example");

      assertFalse(WaypointRegistry.hasSameIdentity(stored, differentName));
      assertFalse(WaypointRegistry.hasSameIdentity(null, stored));
      assertFalse(WaypointRegistry.hasSameIdentity(stored, null));
   }
}
