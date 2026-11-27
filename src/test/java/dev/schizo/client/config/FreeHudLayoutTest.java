package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import dev.schizo.client.render.HudHorizontalAlignment;
import dev.schizo.client.render.HudVerticalAlignment;
import org.junit.jupiter.api.Test;

class FreeHudLayoutTest {
   private static FreeHudLayout read(String json) {
      return FreeHudLayout.read(JsonParser.parseString(json).getAsJsonObject());
   }

   @Test
   void retainsLegacyDefaultsAndClampsFinitePositions() {
      var layout = read("{\"x\":-3,\"y\":2}");
      assertEquals(HudHorizontalAlignment.values()[0], layout.horizontal());
      assertEquals(HudVerticalAlignment.values()[0], layout.vertical());
      assertEquals(0.0F, layout.x());
      assertEquals(1.0F, layout.y());
   }

   @Test
   void rejectsInvalidAlignmentsAndIncompleteOrNonfiniteCoordinates() {
      assertThrows(IllegalArgumentException.class, () -> read("{\"h\":-1,\"x\":0,\"y\":0}"));
      assertThrows(IllegalArgumentException.class, () -> read("{\"v\":42,\"x\":0,\"y\":0}"));
      assertThrows(IllegalArgumentException.class, () -> read("{\"x\":0}"));
      assertThrows(IllegalArgumentException.class, () -> read("{\"x\":0,\"y\":\"NaN\"}"));
   }
}
