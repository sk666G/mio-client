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
import dev.schizo.client.events.bus.EventBus;
import dev.schizo.client.managers.Registry;
import dev.schizo.client.modules.misc.StashFinderModule;
import dev.schizo.client.modules.misc.StashEntry;
import dev.schizo.client.settings.JsonConfigurable;
import dev.schizo.client.settings.Setting;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Iterator;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

public final class StashRegistry extends Registry<StashEntry> implements ClientAccess, JsonConfigurable {
   private static StashFinderModule stashFinderModule;

   public StashRegistry() {
      EventBus eventBus = ClientAccess.eventBus;
      eventBus.subscribe(this);
   }

   public static void bindModules(ModuleRegistry registry) {
      stashFinderModule = (StashFinderModule)java.util.Objects.requireNonNull(
         registry.getModule(StashFinderModule.class),
         "Required module is not registered: StashFinderModule"
      );
   }

   @Override
   public void fromJson(JsonElement element) {
      if (!element.isJsonObject()) {
         return;
      }
      JsonObject object = element.getAsJsonObject();
      if (!object.has("stashes") || !object.get("stashes").isJsonArray()) {
         return;
      }

      List<StashEntry> loaded = new ArrayList<>();
      JsonArray stashes = object.getAsJsonArray("stashes");
      for (JsonElement stashElement : stashes) {
         try {
            StashEntry stash = ClientAccess.gson.fromJson(stashElement, StashEntry.class);
            if (stash == null || !stash.isPersistable()) {
               continue;
            }
            boolean duplicate = loaded.stream().anyMatch(existing -> existing.hasSameLocation(stash));
            if (!duplicate) {
               loaded.add(stash);
            }
         } catch (Exception invalidEntry) {
            ClientAccess.logger.warn("Skipping invalid stash entry in stashfinder.json", invalidEntry);
         }
      }

      this.entries.clear();
      this.entries.addAll(loaded);
   }

   @Override
   public JsonElement toJson() {
      Setting<Boolean> logSetting = stashFinderModule.logSetting;
      Boolean loggingEnabled = logSetting.getValue();
      if (!loggingEnabled) {
         return new JsonObject();
      }

      JsonArray stashes = new JsonArray();
      JsonObject object = new JsonObject();
      Iterator<StashEntry> iterator = ClientContext.stashRegistry.getRegistry().iterator();

      while (iterator.hasNext()) {
         StashEntry stash = iterator.next();
         Gson gson = ClientAccess.gson;
         stashes.add(gson.toJsonTree(stash));
      }

      object.add("stashes", stashes);
      return object;
   }

   @Override
   public String getConfigName() {
      return "stashfinder.json";
   }

}
