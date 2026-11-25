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
import dev.schizo.client.events.ClientInitializationEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.bus.EventBus;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.HudWidgetModule;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.hud.ArmorModule;
import dev.schizo.client.modules.hud.ChatModule;
import dev.schizo.client.modules.hud.CryptoModule;
import dev.schizo.client.modules.hud.DirectionModule;
import dev.schizo.client.modules.hud.EffectsModule;
import dev.schizo.client.modules.hud.EntityListModule;
import dev.schizo.client.modules.hud.GraphModule;
import dev.schizo.client.modules.hud.IndicatorModule;
import dev.schizo.client.modules.hud.InventoryModule;
import dev.schizo.client.modules.hud.LagOMeterModule;
import dev.schizo.client.modules.hud.MapModule;
import dev.schizo.client.modules.hud.MetricsModule;
import dev.schizo.client.modules.hud.ModuleListModule;
import dev.schizo.client.modules.hud.MusicModule;
import dev.schizo.client.modules.hud.PlayerModelModule;
import dev.schizo.client.modules.hud.PositionModule;
import dev.schizo.client.modules.hud.TextRadarModule;
import dev.schizo.client.modules.hud.TotemsModule;
import dev.schizo.client.modules.hud.WatermarkModule;
import dev.schizo.client.modules.hud.WelcomerModule;
import dev.schizo.client.settings.JsonConfigurable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.function.Predicate;

public final class HudModuleRegistry implements ClientAccess, JsonConfigurable {
   public HudModuleRegistry() {
      EventBus events = ClientAccess.eventBus;
      events.subscribe(this);
      ModuleRegistry watermarkRegistry = ClientContext.moduleRegistry;
      WatermarkModule watermark = new WatermarkModule();
      watermarkRegistry.registerModule(watermark);
      ClientContext.moduleRegistry.registerModule(new ModuleListModule());
      ClientContext.moduleRegistry.registerModule(new ArmorModule());
      ClientContext.moduleRegistry.registerModule(new GraphModule());
      ClientContext.moduleRegistry.registerModule(new EffectsModule());
      ClientContext.moduleRegistry.registerModule(new WelcomerModule());
      ClientContext.moduleRegistry.registerModule(new PositionModule());
      ClientContext.moduleRegistry.registerModule(new DirectionModule());
      ClientContext.moduleRegistry.registerModule(new TextRadarModule());
      ClientContext.moduleRegistry.registerModule(new MetricsModule());
      ClientContext.moduleRegistry.registerModule(new TotemsModule());
      ClientContext.moduleRegistry.registerModule(new LagOMeterModule());
      ClientContext.moduleRegistry.registerModule(new InventoryModule());
      ClientContext.moduleRegistry.registerModule(new MusicModule());
      ClientContext.moduleRegistry.registerModule(new PlayerModelModule());
      ClientContext.moduleRegistry.registerModule(new EntityListModule());
      ClientContext.moduleRegistry.registerModule(new ChatModule());
      ClientContext.moduleRegistry.registerModule(new MapModule());
      ClientContext.moduleRegistry.registerModule(new CryptoModule());
      ClientContext.moduleRegistry.registerModule(new IndicatorModule());
   }

   @Override
   public void fromJson(JsonElement json) {
      JsonObject configuration = json.getAsJsonObject();
      configuration.entrySet().stream()
         .sorted(Comparator.comparing(entry -> {
            try {
               JsonElement moduleJson = entry.getValue();
               JsonObject moduleConfiguration = moduleJson.getAsJsonObject();
               String hudKey = "hud";
               JsonObject hudConfiguration = moduleConfiguration.getAsJsonObject(hudKey);
               String indexKey = "index";
               JsonElement indexJson = hudConfiguration.get(indexKey);
               int index = indexJson.getAsInt();
               return index;
            } catch (Throwable failure) {
               return Integer.valueOf(0);
            }
         }))
         .map(entry -> new SimpleEntry<>(findConfiguredModule(entry), entry.getValue()))
         .filter(entry -> entry.getKey() instanceof HudWidgetModule)
         .forEach(entry -> {
            try {
               Module module = (Module)entry.getKey();
               JsonElement moduleJson = (JsonElement)entry.getValue();
               module.fromJson(moduleJson);
            } catch (Exception failure) {
               failure.printStackTrace();
            }
         });
   }

   private static Module findConfiguredModule(Entry configurationEntry) {
      ModuleRegistry registry = ClientContext.moduleRegistry;
      Predicate<Module> matchesName = candidate -> {
         String configName = candidate.getConfigName();
         String entryName = (String)configurationEntry.getKey();
         return configName.equalsIgnoreCase(entryName);
      };
      Optional match = registry.findFirst(matchesName);
      return (Module)match.orElse(null);
   }

   @Override
   public JsonElement toJson() {
      JsonObject configuration = new JsonObject();
      ModuleRegistry registry = ClientContext.moduleRegistry;
      Iterator modules = ((List)registry.getRegistry()).iterator();

      while (modules.hasNext()) {
         Module module = (Module)modules.next();
         if (module instanceof HudWidgetModule) {
            try {
               String configName = module.getConfigName();
               JsonElement moduleJson = module.toJson();
               configuration.add(configName, moduleJson);
            } catch (Exception failure) {
               failure.printStackTrace();
            }
         }
      }

      return configuration;
   }

   @Override
   public String getConfigName() {
      return "hud.json";
   }

   @EventHandler
   private void refreshHudAnchors(ClientInitializationEvent event) {
      ModuleRegistry registry = ClientContext.moduleRegistry;
      Iterator modules = ((List)registry.getRegistry()).iterator();

      while (modules.hasNext()) {
         Module module = (Module)modules.next();
         if (module instanceof HudWidgetModule widget) {
            widget.setAnchor(widget.getAnchor());
         }
      }
   }

}
