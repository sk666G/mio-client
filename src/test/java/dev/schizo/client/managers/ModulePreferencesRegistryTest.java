package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.config.ModuleQuickAccessState;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class ModulePreferencesRegistryTest {
   @Test
   void malformedVersionedRootPreservesExistingPreferences() throws Exception {
      ModulePreferencesRegistry registry = new ModulePreferencesRegistry();
      try {
         ModuleQuickAccessState state = stateOf(registry);
         state.addFavorite("KeepFavorite");
         state.recordRecentlyUsed("KeepRecent");

         JsonObject truncated = new JsonObject();
         truncated.addProperty("version", ModulePreferencesRegistry.CURRENT_SCHEMA_VERSION);

         registry.fromJson(truncated);

         assertEquals(java.util.List.of("KeepFavorite"), registry.getFavorites());
         assertEquals(java.util.List.of("KeepRecent"), registry.getRecentlyUsed());
      } finally {
         ClientAccess.eventBus.unsubscribe(registry);
      }
   }

   @Test
   void earlyLoadWithoutModuleRegistryPreservesExistingPreferences() throws Exception {
      ModulePreferencesRegistry registry = new ModulePreferencesRegistry();
      ModuleRegistry previousRegistry = ClientContext.moduleRegistry;
      try {
         ModuleQuickAccessState state = stateOf(registry);
         state.addFavorite("KeepFavorite");
         state.recordRecentlyUsed("KeepRecent");

         JsonObject root = new JsonObject();
         root.addProperty("version", ModulePreferencesRegistry.CURRENT_SCHEMA_VERSION);
         root.add("favorites", new JsonArray());
         root.add("recent", new JsonArray());

         ClientContext.moduleRegistry = null;
         registry.fromJson(root);

         assertEquals(java.util.List.of("KeepFavorite"), registry.getFavorites());
         assertEquals(java.util.List.of("KeepRecent"), registry.getRecentlyUsed());
      } finally {
         ClientContext.moduleRegistry = previousRegistry;
         ClientAccess.eventBus.unsubscribe(registry);
      }
   }

   private static ModuleQuickAccessState stateOf(ModulePreferencesRegistry registry) throws Exception {
      Field field = ModulePreferencesRegistry.class.getDeclaredField("state");
      field.setAccessible(true);
      return (ModuleQuickAccessState)field.get(registry);
   }
}
