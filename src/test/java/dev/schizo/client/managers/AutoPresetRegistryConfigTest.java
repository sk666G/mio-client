package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import dev.schizo.client.settings.options.PresetCategory;
import dev.schizo.client.utils.network.AutoPresetServerEntry;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AutoPresetRegistryConfigTest {
   @Test
   void parsesValidPresetsAndKitWithoutClientServices() {
      JsonObject server = new JsonObject();
      server.addProperty(PresetCategory.HUD.getName().toLowerCase(), "hud-layout");
      server.addProperty("kit", "crystal-kit");
      JsonObject root = new JsonObject();
      root.add("example.org", server);

      Map<String, AutoPresetServerEntry> loaded = AutoPresetRegistry.parseSettings(root);

      AutoPresetServerEntry entry = loaded.get("example.org");
      assertNotNull(entry);
      assertEquals("hud-layout", entry.getPresets().get(PresetCategory.HUD));
      assertEquals("crystal-kit", entry.getKitName());
   }

   @Test
   void skipsMalformedServersAndValuesButKeepsOtherEntries() {
      JsonObject root = new JsonObject();
      root.addProperty("not-an-object", "bad");

      JsonObject valid = new JsonObject();
      valid.addProperty(PresetCategory.MODULES.getName().toLowerCase(), "combat");
      valid.addProperty("kit", 42);
      root.add("good.example", valid);

      Map<String, AutoPresetServerEntry> loaded = AutoPresetRegistry.parseSettings(root);

      assertEquals(1, loaded.size());
      assertTrue(loaded.containsKey("good.example"));
      assertFalse(loaded.get("good.example").hasKit());
   }

   @Test
   void rejectsPathLikePresetNamesWithoutDiscardingValidMappings() {
      JsonObject server = new JsonObject();
      server.addProperty(PresetCategory.HUD.getName().toLowerCase(), "../escape");
      server.addProperty(PresetCategory.MODULES.getName().toLowerCase(), "legit");
      JsonObject root = new JsonObject();
      root.add("example.org", server);

      AutoPresetServerEntry entry = AutoPresetRegistry.parseSettings(root).get("example.org");

      assertNotNull(entry);
      assertNull(entry.getPresets().get(PresetCategory.HUD));
      assertEquals("legit", entry.getPresets().get(PresetCategory.MODULES));
   }

   @Test
   void omitsEmptyOrInvalidServerEntries() {
      JsonObject root = new JsonObject();
      root.add("", new JsonObject());
      root.add("empty.example", new JsonObject());

      assertTrue(AutoPresetRegistry.parseSettings(root).isEmpty());
   }

   @Test
   void invalidRootIsRejectedAtomically() {
      assertThrows(IllegalArgumentException.class, () -> AutoPresetRegistry.parseSettings(null));
      assertThrows(IllegalArgumentException.class, () -> AutoPresetRegistry.parseSettings(new com.google.gson.JsonArray()));
   }
}
