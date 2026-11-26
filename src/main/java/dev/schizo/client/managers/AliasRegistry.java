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
import dev.schizo.client.config.LegacyBranding;
import dev.schizo.client.modules.Module;
import dev.schizo.client.settings.JsonConfigurable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;

public final class AliasRegistry implements ClientAccess, JsonConfigurable {
   private final Map<Module, String> moduleAliases = new HashMap<>();
   private final Map<String, String> playerAliases = new HashMap<>();

   public static String getModuleAlias(AliasRegistry registry, Module module) {
      Map<Module, String> aliases = registry.moduleAliases;
      return aliases.getOrDefault(module, module.getName());
   }

   @Override
   public void fromJson(JsonElement input) {
      if (!(input instanceof JsonObject json)) {
         throw new IllegalArgumentException("Alias configuration must be a JSON object");
      }

      Map<Module, String> loadedModuleAliases = new HashMap<>();
      Map<String, String> loadedPlayerAliases = new HashMap<>();

      if (json.get("module") instanceof JsonObject moduleObject && ClientContext.moduleRegistry != null) {
         moduleObject.asMap().forEach((configuredModuleName, aliasElement) -> {
            Optional<Module> moduleMatch = ClientContext.moduleRegistry.findFirst((Predicate<Module>)module -> {
               return module.getName().equalsIgnoreCase(LegacyBranding.canonicalModuleName(configuredModuleName));
            });
            moduleMatch.ifPresent(module -> loadedModuleAliases.put(module, aliasElement.getAsString()));
         });
      }

      if (json.get("players") instanceof JsonObject playerObject) {
         playerObject.asMap().forEach((playerName, aliasElement) -> {
            loadedPlayerAliases.keySet().removeIf(existing -> existing.equalsIgnoreCase(playerName));
            loadedPlayerAliases.put(playerName, aliasElement.getAsString());
         });
      }

      this.moduleAliases.clear();
      this.moduleAliases.putAll(loadedModuleAliases);
      this.playerAliases.clear();
      this.playerAliases.putAll(loadedPlayerAliases);
   }

   @Override
   public JsonElement toJson() {
      JsonObject json = new JsonObject();
      JsonObject moduleObject = new JsonObject();
      JsonObject playerObject = new JsonObject();
      this.moduleAliases.forEach((module, alias) -> {
         String moduleName = module.getName();
         moduleObject.addProperty(moduleName, alias);
      });
      this.playerAliases.forEach(playerObject::addProperty);
      json.add("module", moduleObject);
      json.add("players", playerObject);
      return json;
   }

   @Override
   public String getConfigName() {
      return "aliases.json";
   }

   public void setModuleAlias(Module module, String alias) {
      this.moduleAliases.compute(module, (moduleKey, previousAlias) -> alias);
   }

   public void removePlayerAlias(String playerName) {
      if (playerName == null) {
         return;
      }
      this.playerAliases.keySet().removeIf(existing -> existing.equalsIgnoreCase(playerName));
   }

   public String getPlayerAlias(Player player) {
      GameProfile profile = player.getGameProfile();
      String playerName = profile.name();
      return this.getPlayerAlias(playerName);
   }

   public String getPlayerAlias(String playerName) {
      if (playerName == null) {
         return null;
      }
      for (Map.Entry<String, String> entry : this.playerAliases.entrySet()) {
         if (entry.getKey().equalsIgnoreCase(playerName)) {
            return entry.getValue();
         }
      }
      return playerName;
   }

   public void setPlayerAlias(String playerName, String alias) {
      if (playerName == null) {
         return;
      }
      this.playerAliases.keySet().removeIf(existing -> existing.equalsIgnoreCase(playerName));
      this.playerAliases.put(playerName, alias);
   }

   public void removeModuleAlias(Module module) {
      this.moduleAliases.remove(module);
   }
}
