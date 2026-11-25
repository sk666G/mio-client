package dev.schizo.client.managers;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WaypointConfigTest {
   @Test
   void identityIncludesServerAndIgnoresCase() {
      var array = JsonParser.parseString("""
         [{"name":"Camp","server":"one","dimension":"end"},
          {"name":"camp","server":"ONE","dimension":"end"},
          {"name":"Camp","server":"two","dimension":"end"}]
         """).getAsJsonArray();
      var loaded = WaypointConfig.decode(array, new Gson());
      assertEquals(2, loaded.size());
      assertEquals("one", loaded.get(0).getServer());
      assertEquals("two", loaded.get(1).getServer());
      array.add(com.google.gson.JsonNull.INSTANCE);
      assertThrows(IllegalArgumentException.class, () -> WaypointConfig.decode(array, new Gson()));
   }
}
