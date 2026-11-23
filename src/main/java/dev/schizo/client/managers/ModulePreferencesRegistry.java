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
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.ModuleToggleEvent;
import dev.schizo.client.modules.Module;
import dev.schizo.client.settings.JsonConfigurable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/** Stores user module favourites and the most recently enabled modules. */
public final class ModulePreferencesRegistry implements ClientAccess, JsonConfigurable {
   public static final int CURRENT_SCHEMA_VERSION = 1;
   public static final int MAX_FAVORITES = 128;
   public static final int MAX_RECENT = 20;
   private final LinkedHashSet<String> favorites = new LinkedHashSet<>();
   private final ArrayDeque<String> recentlyUsed = new ArrayDeque<>();

   public ModulePreferencesRegistry() {
      ClientAccess.eventBus.subscribe(this);
   }

   @Override
   public String getConfigName() {
      return "module-preferences.json";
   }

   public synchronized boolean isFavorite(Module module) {
      return module != null && this.favorites.contains(module.getConfigName());
   }

   public synchronized boolean isFavorite(String moduleName) {
      return moduleName != null && this.favorites.contains(moduleName);
   }

   public synchronized boolean addFavorite(Module module) {
      if (module == null) {
         return false;
      }
      String name = module.getConfigName();
      if (this.favorites.contains(name)) {
         return false;
      }
      while (this.favorites.size() >= MAX_FAVORITES) {
         String oldest = this.favorites.iterator().next();
         this.favorites.remove(oldest);
      }
      return this.favorites.add(name);
   }

   public synchronized boolean removeFavorite(Module module) {
      return module != null && this.favorites.remove(module.getConfigName());
   }

   /** Returns the new favorite state. */
   public synchronized boolean toggleFavorite(Module module) {
      if (module == null) {
         return false;
      }
      if (this.favorites.remove(module.getConfigName())) {
         return false;
      }
      while (this.favorites.size() >= MAX_FAVORITES) {
         String oldest = this.favorites.iterator().next();
         this.favorites.remove(oldest);
      }
      this.favorites.add(module.getConfigName());
      return true;
   }

   public synchronized List<String> getFavorites() {
      return List.copyOf(this.favorites);
   }

   /** Most recently enabled module first. */
   public synchronized List<String> getRecentlyUsed() {
      return List.copyOf(this.recentlyUsed);
   }

   public synchronized int getRecentIndex(String moduleName) {
      if (moduleName == null) {
         return -1;
      }
      int index = 0;
      for (String name : this.recentlyUsed) {
         if (name.equals(moduleName)) {
            return index;
         }
         index++;
      }
      return -1;
   }

   public synchronized void clearFavorites() {
      this.favorites.clear();
   }

   public synchronized void clearRecentlyUsed() {
      this.recentlyUsed.clear();
   }

   public synchronized void clear() {
      this.favorites.clear();
      this.recentlyUsed.clear();
   }

   @EventHandler
   private void onModuleToggle(ModuleToggleEvent event) {
      Module module = event.getModule();
      if (module == null || !module.isToggled()) {
         return;
      }
      this.recordRecentlyUsed(module);
   }

   private synchronized void recordRecentlyUsed(Module module) {
      String name = module.getConfigName();
      this.recentlyUsed.remove(name);
      this.recentlyUsed.addFirst(name);
      while (this.recentlyUsed.size() > MAX_RECENT) {
         this.recentlyUsed.removeLast();
      }
   }

   @Override
   public synchronized JsonElement toJson() {
      JsonObject root = new JsonObject();
      root.addProperty("version", CURRENT_SCHEMA_VERSION);
      JsonArray favoriteArray = new JsonArray();
      for (String name : this.favorites) {
         favoriteArray.add(name);
      }
      root.add("favorites", favoriteArray);

      JsonArray recentArray = new JsonArray();
      for (String name : this.recentlyUsed) {
         recentArray.add(name);
      }
      root.add("recent", recentArray);
      return root;
   }

   @Override
   public synchronized void fromJson(JsonElement json) {
      this.clear();
      if (json == null || !json.isJsonObject()) {
         return;
      }
      JsonObject root = json.getAsJsonObject();
      JsonElement version = root.get("version");
      if (version == null || !version.isJsonPrimitive()
         || !version.getAsJsonPrimitive().isNumber()
         || version.getAsDouble() != CURRENT_SCHEMA_VERSION) {
         ClientAccess.logger.warn("Ignoring unsupported module preferences configuration");
         return;
      }

      this.loadNames(root.get("favorites"), this.favorites, MAX_FAVORITES, false);
      List<String> recent = new ArrayList<>();
      this.loadNames(root.get("recent"), recent, MAX_RECENT, true);
      this.recentlyUsed.addAll(recent);
   }

   private void loadNames(JsonElement value, Collection<String> target, int limit, boolean allowDuplicates) {
      if (value == null || !value.isJsonArray()) {
         return;
      }
      for (JsonElement element : value.getAsJsonArray()) {
         if (target.size() >= limit) {
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
         if (ClientContext.moduleRegistry != null) {
            for (Object moduleElement : ClientContext.moduleRegistry.getRegistry()) {
               if (moduleElement instanceof Module module && module.getConfigName().equals(name)) {
                  known = true;
                  break;
               }
            }
         }
         if (known && (allowDuplicates || !target.contains(name))) {
            target.add(name);
         }
      }
   }
}
