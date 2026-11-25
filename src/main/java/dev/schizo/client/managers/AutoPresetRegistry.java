/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.utils.chat.ChatUtils;
import dev.schizo.client.config.PresetNameValidator;
import dev.schizo.client.utils.inventory.Kit;
import dev.schizo.client.utils.network.AutoPresetServerEntry;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.ConnectScreenConnectEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.settings.JsonConfigurable;
import dev.schizo.client.settings.options.PresetCategory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;

public final class AutoPresetRegistry implements ClientAccess, JsonConfigurable {
   private final Map serverSettings = new HashMap();
   private String lastServer;

   public Collection getServers() {
      return this.serverSettings.keySet();
   }

   public AutoPresetRegistry() {
      ClientAccess.eventBus.subscribe(this);
   }

   @Override
   public void fromJson(JsonElement json) {
      Map<String, ParsedServerEntry> parsed;
      try {
         parsed = parseSettings(json);
      } catch (IllegalArgumentException invalidRoot) {
         ClientAccess.logger.warn("Ignoring malformed auto-preset configuration", invalidRoot);
         return;
      }

      Map<String, AutoPresetServerEntry> loaded = new LinkedHashMap<>();
      for (Entry<String, ParsedServerEntry> serverEntry : parsed.entrySet()) {
         String server = serverEntry.getKey();
         ParsedServerEntry parsedEntry = serverEntry.getValue();
         AutoPresetServerEntry resolved = new AutoPresetServerEntry(server);

         for (Entry<String, String> presetEntry : parsedEntry.presets().entrySet()) {
            PresetCategory category = PresetCategory.findByName(presetEntry.getKey());
            if (category != null) {
               resolved.getPresets().put(category, presetEntry.getValue());
            }
         }

         if (parsedEntry.kitName() != null) {
            resolved.setKitName(parsedEntry.kitName());
         }
         if (!resolved.getPresets().isEmpty() || resolved.hasKit()) {
            loaded.put(server, resolved);
         }
      }

      this.serverSettings.clear();
      this.serverSettings.putAll(loaded);
      this.lastServer = null;
   }

   static Map<String, ParsedServerEntry> parseSettings(JsonElement json) {
      if (json == null || !json.isJsonObject()) {
         throw new IllegalArgumentException("Auto-preset configuration must be a JSON object");
      }

      Map<String, ParsedServerEntry> loaded = new LinkedHashMap<>();
      for (Entry<String, JsonElement> serverEntry : json.getAsJsonObject().entrySet()) {
         String server = serverEntry.getKey();
         if (server == null || server.isBlank() || server.length() > 255) {
            continue;
         }
         server = normalizeServerKey(server);

         JsonElement serverJson = serverEntry.getValue();
         if (serverJson == null || !serverJson.isJsonObject()) {
            continue;
         }

         Map<String, String> presets = new LinkedHashMap<>();
         String kitName = null;
         JsonObject settingsJson = serverJson.getAsJsonObject();
         for (Entry<String, JsonElement> settingEntry : settingsJson.entrySet()) {
            String categoryName = settingEntry.getKey();
            JsonElement value = settingEntry.getValue();
            if ("kit".equalsIgnoreCase(categoryName)) {
               if (value != null && value.isJsonPrimitive()
                  && value.getAsJsonPrimitive().isString()) {
                  String candidateKit = value.getAsString();
                  if (!candidateKit.isBlank() && candidateKit.length() <= 255) {
                     kitName = candidateKit;
                  }
               }
               continue;
            }

            if (categoryName == null || categoryName.isBlank() || categoryName.length() > 64
               || value == null || !value.isJsonPrimitive()
               || !value.getAsJsonPrimitive().isString()) {
               continue;
            }

            try {
               String presetName = PresetNameValidator.normalize(value.getAsString());
               presets.put(categoryName, presetName);
            } catch (IllegalArgumentException invalidPresetName) {
               // Skip only the invalid mapping; other server/category entries remain loadable.
            }
         }

         if (!presets.isEmpty() || kitName != null) {
            loaded.put(server, new ParsedServerEntry(presets, kitName));
         }
      }
      return Map.copyOf(loaded);
   }

   static String normalizeServerKey(String server) {
      if (server == null) {
         throw new IllegalArgumentException("Server key must not be null");
      }
      return server.toLowerCase(Locale.ROOT);
   }

   record ParsedServerEntry(Map<String, String> presets, String kitName) {
      ParsedServerEntry {
         presets = Map.copyOf(presets);
      }
   }

   @Override
   public JsonElement toJson() {
      JsonObject root = new JsonObject();
      Iterator serverIterator = this.serverSettings.values().iterator();

      while (serverIterator.hasNext()) {
         AutoPresetServerEntry serverSettings = (AutoPresetServerEntry)serverIterator.next();
         JsonObject settingsJson = new JsonObject();

         Iterator presetIterator = serverSettings.getPresets().entrySet().iterator();

         while (presetIterator.hasNext()) {
            Entry presetEntry = (Entry)presetIterator.next();
            PresetCategory category = (PresetCategory)presetEntry.getKey();
            String categoryKey = category.getName().toLowerCase(Locale.ROOT);
            String presetName = (String)presetEntry.getValue();
            settingsJson.addProperty(categoryKey, presetName);
         }

         if (serverSettings.hasKit()) {
            String kitName = serverSettings.getKitName();
            settingsJson.addProperty("kit", kitName);
         }

         String serverName = serverSettings.getServer();
         root.add(serverName, settingsJson);
      }

      return root;
   }

   public boolean removeKit(String server) {
      AutoPresetServerEntry settings = server == null
         ? null
         : (AutoPresetServerEntry)this.serverSettings.get(normalizeServerKey(server));
      if (settings == null) {
         return false;
      }

      settings.setKitName(null);
      return true;
   }

   @Override
   public String getConfigName() {
      Path presetDirectoryName = ConfigurationManager.PRESETS_DIRECTORY.getFileName();

      return presetDirectoryName.resolve("auto.json").toString();
   }

   public boolean removePreset(String server, PresetCategory category) {
      AutoPresetServerEntry settings = server == null
         ? null
         : (AutoPresetServerEntry)this.serverSettings.get(normalizeServerKey(server));
      if (settings == null) {
         return false;
      }

      return settings.getPresets().remove(category) != null;
   }

   public void setPreset(
      String server, PresetCategory category, String presetName
   ) {
      String serverKey = normalizeServerKey(server);
      AutoPresetServerEntry settings = (AutoPresetServerEntry)this.serverSettings.computeIfAbsent(
         serverKey,
         (Function<String, AutoPresetServerEntry>)ignoredServer -> new AutoPresetServerEntry(serverKey)
      );

      settings.getPresets().put(category, presetName);
   }

   boolean beginConnection(String serverAddress) {
      String serverKey = normalizeServerKey(serverAddress);
      if (serverKey.equals(this.lastServer)) {
         return false;
      }
      this.lastServer = serverKey;
      return true;
   }

   void resetConnectionState() {
      this.lastServer = null;
   }

   @EventHandler
   public void onConnectScreenConnectEvent(ConnectScreenConnectEvent event) {
      String serverAddress = event.getServerAddress();
      if (!this.beginConnection(serverAddress)) {
         return;
      }

      AutoPresetServerEntry settings = (AutoPresetServerEntry)this.serverSettings.get(this.lastServer);
      if (settings == null) {
         return;
      }

      AtomicInteger messageCounter = new AtomicInteger(-2);

      settings.getPresets().forEach((BiConsumer<PresetCategory, String>)(category, presetName) -> {
         ConfigurationManager configurationManager = ClientContext.configurationManager;
         PresetRegistry presetRegistry = configurationManager.getPresetRegistry(category);
         presetRegistry.applyPreset(presetName);
         TaskScheduler schedulerForPreset = ClientContext.taskScheduler;
         Runnable messageForPreset = () -> {
            int messageId = messageCounter.getAndDecrement();

            MessageSignature signature = ChatUtils.createSignature(messageId);

            Object[] messageArguments = new Object[]{presetName};
            String messageFormat = "Preset %s has been loaded.";
            String messageText = messageFormat.formatted(messageArguments);
            ChatUtils.printChat(Component.literal(messageText), signature);
         };
         schedulerForPreset.scheduleRotationTask(messageForPreset, 0);
      });

      if (settings.hasKit()) {
         String kitName = settings.getKitName();
         ClientContext.kitRegistry.findFirst((Predicate<Kit>)kit -> kit.getName().equalsIgnoreCase(kitName))
            .ifPresent((Consumer<Kit>)matchingKit -> {
               KitRegistry kitRegistry = ClientContext.kitRegistry;
               kitRegistry.setCurrentKitName(kitName);
               TaskScheduler schedulerForKit = ClientContext.taskScheduler;
               Runnable messageForKit = () -> {
                  int messageId = messageCounter.getAndDecrement();

                  MessageSignature signature = ChatUtils.createSignature(messageId);

                  Object[] messageArguments = new Object[]{kitName};
                  String messageFormat = "Kit %s has been loaded.";
                  String messageText = messageFormat.formatted(messageArguments);
                  ChatUtils.printChat(Component.literal(messageText), signature);
               };
               schedulerForKit.scheduleRunTickTask(messageForKit);
            });
      }
   }

   @EventHandler
   private void onMinecraftDisconnect(MinecraftDisconnectEvent event) {
      this.resetConnectionState();
   }

   public boolean removeServer(String server) {
      return server != null && this.serverSettings.remove(normalizeServerKey(server)) != null;
   }

   public void setKit(String server, String kitName) {
      String serverKey = normalizeServerKey(server);
      AutoPresetServerEntry settings = (AutoPresetServerEntry)this.serverSettings.computeIfAbsent(
         serverKey,
         (Function<String, AutoPresetServerEntry>)ignoredServer -> new AutoPresetServerEntry(serverKey)
      );

      settings.setKitName(kitName);
   }

}
