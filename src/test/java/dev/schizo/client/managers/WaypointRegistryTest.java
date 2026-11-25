package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.utils.world.Waypoint;
import org.junit.jupiter.api.Test;

class WaypointRegistryTest {
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
   void identityRequiresBothNameAndServer() {
      Waypoint stored = new Waypoint("base", 1.0, 64.0, 2.0, "minecraft:overworld", "server.example");
      Waypoint differentName = new Waypoint("mine", 1.0, 64.0, 2.0, "minecraft:overworld", "server.example");

      assertFalse(WaypointRegistry.hasSameIdentity(stored, differentName));
      assertFalse(WaypointRegistry.hasSameIdentity(null, stored));
      assertFalse(WaypointRegistry.hasSameIdentity(stored, null));
   }
}
