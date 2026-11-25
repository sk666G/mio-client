package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import org.junit.jupiter.api.Test;

class ModulePreferencesRegistryTest {
   @Test
   void invalidRootPreservesExistingPreferences() {
      ModulePreferencesRegistry registry = registryWithFavorite("Alpha");

      assertThrows(IllegalArgumentException.class, () -> registry.fromJson(new JsonArray()));

      assertTrue(registry.isFavorite("Alpha"));
      assertEquals(1, registry.getFavorites().size());
   }

   @Test
   void unsupportedSchemaPreservesExistingPreferences() {
      ModulePreferencesRegistry registry = registryWithFavorite("Alpha");
      JsonObject root = emptyPreferences(99);

      assertThrows(IllegalArgumentException.class, () -> registry.fromJson(root));

      assertTrue(registry.isFavorite("Alpha"));
      assertEquals(1, registry.getFavorites().size());
   }

   @Test
   void validEmptyConfigurationReplacesExistingPreferences() {
      ModulePreferencesRegistry registry = registryWithFavorite("Alpha");

      registry.fromJson(emptyPreferences(ModulePreferencesRegistry.CURRENT_SCHEMA_VERSION));

      assertFalse(registry.isFavorite("Alpha"));
      assertTrue(registry.getFavorites().isEmpty());
      assertTrue(registry.getRecentlyUsed().isEmpty());
   }

   private static ModulePreferencesRegistry registryWithFavorite(String name) {
      ModulePreferencesRegistry registry = new ModulePreferencesRegistry();
      assertTrue(registry.addFavorite(new Module(name, ModuleCategory.CLIENT)));
      return registry;
   }

   private static JsonObject emptyPreferences(int version) {
      JsonObject root = new JsonObject();
      root.addProperty("version", version);
      root.add("favorites", new JsonArray());
      root.add("recent", new JsonArray());
      return root;
   }
}
