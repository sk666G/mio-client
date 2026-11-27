package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

class AutoPresetRegistryTest {
   @Test
   void serverKeysTrimAndNormalizeCase() {
      assertEquals("example.org", AutoPresetRegistry.normalizeServerKey("  EXAMPLE.ORG  "));
   }

   @Test
   void blankServerKeysAreRejected() {
      assertThrows(IllegalArgumentException.class, () -> AutoPresetRegistry.normalizeServerKey("   "));
   }

   @Test
   void parsedSettingsUseCanonicalServerKeys() {
      JsonObject settings = new JsonObject();
      settings.addProperty("hud", "default");

      JsonObject root = new JsonObject();
      root.add("  EXAMPLE.ORG  ", settings);

      var parsed = AutoPresetRegistry.parseSettings(root);

      assertTrue(parsed.containsKey("example.org"));
      assertFalse(parsed.containsKey("  example.org  "));
   }
}
