package dev.schizo.client.managers;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WaypointRegistryTest {
   @Test
   void identityIncludesServerAndIgnoresCase() {
      var array = JsonParser.parseString("""
         [{"name":"Camp","server":"one","dimension":"end"},
          {"name":"camp","server":"ONE","dimension":"end"},
          {"name":"Camp","server":"two","dimension":"end"}]
         """).getAsJsonArray();
      var loaded = WaypointRegistry.decodeWaypoints(array, new Gson());
      assertEquals(2, loaded.size());
      assertFalse(WaypointRegistry.sameIdentity(loaded.get(0), loaded.get(1)));
      assertTrue(WaypointRegistry.sameIdentity(loaded.get(0), loaded.get(0)));
      array.add(com.google.gson.JsonNull.INSTANCE);
      assertThrows(IllegalArgumentException.class, () -> WaypointRegistry.decodeWaypoints(array, new Gson()));
   }
}
