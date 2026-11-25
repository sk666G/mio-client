package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AutoPresetRegistryConfigTest {
   @Test
   void parsesValidPresetsAndKitWithoutClientServices() {
      JsonObject server = new JsonObject();
      server.addProperty("hud", "hud-layout");
      server.addProperty("kit", "crystal-kit");
      JsonObject root = new JsonObject();
      root.add("example.org", server);

      Map<String, AutoPresetRegistry.ParsedServerEntry> loaded = AutoPresetRegistry.parseSettings(root);

      AutoPresetRegistry.ParsedServerEntry entry = loaded.get("example.org");
      assertNotNull(entry);
      assertEquals("hud-layout", entry.presets().get("hud"));
      assertEquals("crystal-kit", entry.kitName());
   }

   @Test
   void canonicalizesServerKeysCaseInsensitively() {
      JsonObject server = new JsonObject();
      server.addProperty("hud", "layout");
      JsonObject root = new JsonObject();
      root.add("ExAmPle.ORG:25565", server);

      Map<String, AutoPresetRegistry.ParsedServerEntry> loaded = AutoPresetRegistry.parseSettings(root);

      assertEquals(1, loaded.size());
      assertTrue(loaded.containsKey("example.org:25565"));
      assertEquals("example.org:25565", AutoPresetRegistry.normalizeServerKey("EXAMPLE.ORG:25565"));
   }

   @Test
   void collapsesCaseOnlyDuplicateServerKeys() {
      JsonObject first = new JsonObject();
      first.addProperty("hud", "first");
      JsonObject second = new JsonObject();
      second.addProperty("hud", "second");
      JsonObject root = new JsonObject();
      root.add("Example.org", first);
      root.add("example.ORG", second);

      Map<String, AutoPresetRegistry.ParsedServerEntry> loaded = AutoPresetRegistry.parseSettings(root);

      assertEquals(1, loaded.size());
      assertEquals("second", loaded.get("example.org").presets().get("hud"));
   }

   @Test
   void repeatedSameServerConnectionLookupsRemainAvailable() {
      AutoPresetRegistry registry = new AutoPresetRegistry();
      registry.setKit("Example.org", "kit");

      assertNotNull(registry.getSettingsForConnection("example.ORG"));
      assertNotNull(registry.getSettingsForConnection("EXAMPLE.org"));
      assertSame(
         registry.getSettingsForConnection("example.org"),
         registry.getSettingsForConnection("EXAMPLE.ORG")
      );
   }

   @Test
   void skipsMalformedServersAndValuesButKeepsOtherEntries() {
      JsonObject root = new JsonObject();
      root.addProperty("not-an-object", "bad");

      JsonObject valid = new JsonObject();
      valid.addProperty("modules", "combat");
      valid.addProperty("kit", 42);
      root.add("good.example", valid);

      Map<String, AutoPresetRegistry.ParsedServerEntry> loaded = AutoPresetRegistry.parseSettings(root);

      assertEquals(1, loaded.size());
      assertTrue(loaded.containsKey("good.example"));
      assertNull(loaded.get("good.example").kitName());
   }

   @Test
   void rejectsPathLikePresetNamesWithoutDiscardingValidMappings() {
      JsonObject server = new JsonObject();
      server.addProperty("hud", "../escape");
      server.addProperty("modules", "legit");
      JsonObject root = new JsonObject();
      root.add("example.org", server);

      AutoPresetRegistry.ParsedServerEntry entry = AutoPresetRegistry.parseSettings(root).get("example.org");

      assertNotNull(entry);
      assertNull(entry.presets().get("hud"));
      assertEquals("legit", entry.presets().get("modules"));
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
