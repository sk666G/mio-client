package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import dev.schizo.client.settings.options.PresetCategory;
import dev.schizo.client.utils.network.AutoPresetServerEntry;
import java.util.List;
import java.util.Map;
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
   }   @Test
   void allPresetAppliesBeforeSpecificCategoriesRegardlessOfHashMapOrder() {
      AutoPresetServerEntry settings = new AutoPresetServerEntry("example.org");
      settings.getPresets().put(PresetCategory.VISUALS, "visual");
      settings.getPresets().put(PresetCategory.ALL, "base");
      settings.getPresets().put(PresetCategory.MODULES, "modules");

      List<Map.Entry<PresetCategory, String>> ordered = AutoPresetRegistry.orderedPresetEntries(settings);

      assertEquals(
         List.of(PresetCategory.ALL, PresetCategory.MODULES, PresetCategory.VISUALS),
         ordered.stream().map(Map.Entry::getKey).toList()
      );
      assertEquals(List.of("base", "modules", "visual"), ordered.stream().map(Map.Entry::getValue).toList());
   }

   @Test
   @SuppressWarnings({"rawtypes", "unchecked"})
   void malformedRawPresetEntriesAreIgnored() {
      AutoPresetServerEntry settings = new AutoPresetServerEntry("example.org");
      Map raw = settings.getPresets();
      raw.put("not-a-category", "bad");
      raw.put(PresetCategory.ALL, 123);
      raw.put(PresetCategory.HUD, "hud");

      List<Map.Entry<PresetCategory, String>> ordered = AutoPresetRegistry.orderedPresetEntries(settings);

      assertEquals(1, ordered.size());
      assertEquals(PresetCategory.HUD, ordered.getFirst().getKey());
      assertEquals("hud", ordered.getFirst().getValue());
   }


}
