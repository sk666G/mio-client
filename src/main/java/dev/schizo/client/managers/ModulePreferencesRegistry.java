/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.config.ModuleQuickAccessState;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.ModuleToggleEvent;
import dev.schizo.client.modules.Module;
import dev.schizo.client.settings.JsonConfigurable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/** Stores user module favorites and the most recently enabled modules. */
public final class ModulePreferencesRegistry implements ClientAccess, JsonConfigurable {
   public static final int CURRENT_SCHEMA_VERSION = 1;
   public static final int MAX_FAVORITES = ModuleQuickAccessState.MAX_FAVORITES;
   public static final int MAX_RECENT = ModuleQuickAccessState.MAX_RECENT;
   private final ModuleQuickAccessState state = new ModuleQuickAccessState();

   public ModulePreferencesRegistry() {
      ClientAccess.eventBus.subscribe(this);
   }

   @Override
   public String getConfigName() {
      return "module-preferences.json";
   }

   public boolean isFavorite(Module module) {
      return module != null && this.state.isFavorite(module.getConfigName());
   }

   public boolean isFavorite(String moduleName) {
      return this.state.isFavorite(moduleName);
   }

   public boolean addFavorite(Module module) {
      return module != null && this.state.addFavorite(module.getConfigName());
   }

   public boolean removeFavorite(Module module) {
      return module != null && this.state.removeFavorite(module.getConfigName());
   }

   /** Returns the new favorite state. */
   public boolean toggleFavorite(Module module) {
      return module != null && this.state.toggleFavorite(module.getConfigName());
   }

   public List<String> getFavorites() {
      return this.state.getFavorites();
   }

   public int getFavoriteIndex(String moduleName) {
      return this.state.getFavoriteIndex(moduleName);
   }

   /** Most recently enabled module first. */
   public List<String> getRecentlyUsed() {
      return this.state.getRecentlyUsed();
   }

   public int getRecentIndex(String moduleName) {
      return this.state.getRecentIndex(moduleName);
   }

   public void clearFavorites() {
      this.state.clearFavorites();
   }

   public void clearRecentlyUsed() {
      this.state.clearRecentlyUsed();
   }

   public void clear() {
      this.state.clear();
   }

   @EventHandler
   private void onModuleToggle(ModuleToggleEvent event) {
      Module module = event.getModule();
      if (module == null || !module.isToggled()) {
         return;
      }
      this.state.recordRecentlyUsed(module.getConfigName());
   }

   @Override
   public JsonElement toJson() {
      JsonObject root = new JsonObject();
      root.addProperty("version", CURRENT_SCHEMA_VERSION);
      JsonArray favoriteArray = new JsonArray();
      for (String name : this.state.getFavorites()) {
         favoriteArray.add(name);
      }
      root.add("favorites", favoriteArray);

      JsonArray recentArray = new JsonArray();
      for (String name : this.state.getRecentlyUsed()) {
         recentArray.add(name);
      }
      root.add("recent", recentArray);
      return root;
   }

   @Override
   public void fromJson(JsonElement json) {
      if (json == null || !json.isJsonObject()) {
         throw new IllegalArgumentException("Module preferences configuration must be a JSON object");
      }
      JsonObject root = json.getAsJsonObject();
      JsonElement version = root.get("version");
      if (version == null || !version.isJsonPrimitive()
         || !version.getAsJsonPrimitive().isNumber()
         || version.getAsDouble() != CURRENT_SCHEMA_VERSION) {
         throw new IllegalArgumentException("Unsupported module preferences schema version");
      }

      List<String> favorites = this.loadKnownNames(root.get("favorites"), MAX_FAVORITES, false);
      List<String> recent = this.loadKnownNames(root.get("recent"), MAX_RECENT, true);
      this.state.replace(favorites, recent);
   }

   private List<String> loadKnownNames(JsonElement value, int limit, boolean allowDuplicates) {
      List<String> names = new ArrayList<>();
      if (value == null || !value.isJsonArray() || ClientContext.moduleRegistry == null) {
         return names;
      }
      for (JsonElement element : value.getAsJsonArray()) {
         if (names.size() >= limit) {
            break;
         }
         if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            continue;
         }
         String name = element.getAsString().strip();
         if (name.isEmpty() || name.length() > 128 || name.chars().anyMatch(Character::isISOControl)) {
            continue;
         }

         boolean known = false;
         for (Object moduleElement : ClientContext.moduleRegistry.getRegistry()) {
            if (moduleElement instanceof Module module && module.getConfigName().equals(name)) {
               known = true;
               break;
            }
         }
         if (known && (allowDuplicates || !names.contains(name))) {
            names.add(name);
         }
      }
      return names;
   }
}
