/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.ClientContext;
import dev.schizo.client.settings.JsonConfigurable;
import dev.schizo.client.integrations.MultiplayerCompatibilityPolicy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public final class ConfigurableRegistry implements Iterable<JsonConfigurable> {
   private final List<JsonConfigurable> configurables = new ArrayList<>();

   ConfigurableRegistry() {
      this.register(MultiplayerCompatibilityPolicy.GLOBAL);
      this.register(ClientContext.webhookManager);
      this.register(ClientContext.moduleRegistry);
      this.register(ClientContext.modulePreferencesRegistry);
      this.register(ClientContext.socialRegistry);
      this.register(ClientContext.waypointRegistry);
      this.register(ClientContext.macroRegistry);
      this.register(ClientContext.stashRegistry);
      this.register(ClientContext.hudModuleRegistry);
      this.register(ClientContext.kitRegistry);
      this.register(ClientContext.autoPresetRegistry);
      this.register(ClientContext.contextProfileRegistry);
      this.register(ClientContext.aliasRegistry);
      this.register(ClientContext.chatFilterRegistry);
      this.register(ClientContext.glintRegistry);
      this.register(ClientContext.commandRegistry);
      this.register(ClientContext.tradeRegistry);
      this.register(ClientContext.blockColorRegistry);
   }

   @Override
   public Iterator<JsonConfigurable> iterator() {
      return this.configurables.iterator();
   }

   private void register(JsonConfigurable configurable) {
      JsonConfigurable checked = Objects.requireNonNull(configurable, "Configurable must not be null");
      String configName = requireValidConfigName(checked);
      for (JsonConfigurable existing : this.configurables) {
         if (existing.getConfigName().equalsIgnoreCase(configName)) {
            throw new IllegalStateException("Duplicate configuration name: " + configName);
         }
      }
      this.configurables.add(checked);
   }

   static String requireValidConfigName(JsonConfigurable configurable) {
      String configName = Objects.requireNonNull(configurable.getConfigName(), "Configuration name must not be null");
      String portable = configName.replace('\\', '/');
      if (portable.isBlank()
         || portable.startsWith("/")
         || portable.contains("../")
         || portable.contains("/..")
         || portable.contains(":")
         || !portable.toLowerCase(java.util.Locale.ROOT).endsWith(".json")) {
         throw new IllegalStateException("Invalid configuration name: " + configName);
      }
      return configName;
   }
}
