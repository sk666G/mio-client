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
import dev.schizo.client.config.JsonUtils;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.InputPressEvent;
import dev.schizo.client.events.MacroTriggerEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.events.WorldRenderEvent;
import dev.schizo.client.modules.Keybind;
import dev.schizo.client.modules.misc.HoldMacro;
import dev.schizo.client.modules.misc.Macro;
import dev.schizo.client.settings.JsonConfigurable;
import dev.schizo.client.settings.NamedEntry;
import dev.schizo.client.settings.options.MacroType;
import dev.schizo.client.utils.KeyUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public final class MacroRegistry extends Registry<Macro> implements ClientAccess, JsonConfigurable {

   public MacroRegistry() {
      ClientAccess.eventBus.subscribe(this);
   }

   @Override
   public boolean registerEntry(NamedEntry entry) {
      if (!(entry instanceof Macro macro) || !isValidName(macro.getName())) {
         return false;
      }

      if (this.entries.stream().anyMatch(existing -> hasSameName(existing, macro))) {
         return false;
      }

      return this.entries.add(macro);
   }

   @Override
   public boolean unregisterEntry(NamedEntry entry) {
      if (!(entry instanceof Macro macro) || !this.entries.remove(macro)) {
         return false;
      }

      releaseMacroState(macro);
      return true;
   }

   private static void releaseMacroState(Macro macro) {
      if (macro instanceof HoldMacro holdMacro) {
         try {
            holdMacro.updatePressed(false);
         } catch (RuntimeException releaseFailure) {
            ClientAccess.logger.warn("Hold macro {} failed while releasing lifecycle state", macro.getName(), releaseFailure);
         }
      }
   }

   private void releaseHoldMacros() {
      for (Macro macro : List.copyOf(this.entries)) {
         releaseMacroState(macro);
      }
   }

   public Optional<Macro> findByName(String name) {
      if (!isValidName(name)) {
         return Optional.empty();
      }

      return this.entries.stream()
         .filter(macro -> macro != null && macro.getName() != null && macro.getName().equalsIgnoreCase(name))
         .findFirst();
   }

   public boolean replaceMacro(Macro previous, Macro replacement) {
      if (previous == null
         || replacement == null
         || !isValidName(replacement.getName())
         || !this.entries.contains(previous)) {
         return false;
      }

      for (Macro existing : this.entries) {
         if (existing != previous && hasSameName(existing, replacement)) {
            return false;
         }
      }

      if (!this.unregisterEntry(previous)) {
         return false;
      }
      if (this.registerEntry(replacement)) {
         return true;
      }

      this.entries.add(previous);
      return false;
   }

   public boolean upsertMacro(Macro macro) {
      if (macro == null || !isValidName(macro.getName())) {
         return false;
      }

      Optional<Macro> existing = this.findByName(macro.getName());
      return existing.isPresent()
         ? this.replaceMacro(existing.get(), macro)
         : this.registerEntry(macro);
   }

   static boolean hasSameName(Macro first, Macro second) {
      return first != null
         && second != null
         && first.getName() != null
         && second.getName() != null
         && first.getName().equalsIgnoreCase(second.getName());
   }

   static boolean isValidName(String name) {
      return name != null && !name.isBlank();
   }

   @Override
   public void fromJson(JsonElement json) {
      if (!(json instanceof JsonObject rootObject)
         || !(rootObject.get("macros") instanceof JsonArray macroArray)) {
         throw new IllegalArgumentException("Macro configuration must contain a macros array");
      }

      List<Macro> loaded = new ArrayList<>();
      for (JsonElement macroElement : macroArray) {
         Macro macro = parseMacro(macroElement);
         if (macro == null) {
            continue;
         }
         if (loaded.stream().noneMatch(existing -> hasSameName(existing, macro))) {
            loaded.add(macro);
         }
      }

      this.releaseHoldMacros();
      this.entries.clear();
      this.entries.addAll(loaded);
   }

   static Macro parseMacro(JsonElement macroElement) {
      if (!(macroElement instanceof JsonObject macroJson)) {
         return null;
      }
      String[] propertyNames = {"name", "type", "key", "mouse", "commands"};
      if (!JsonUtils.hasProperties(macroJson, propertyNames)) {
         return null;
      }

      try {
         JsonElement nameElement = macroJson.get("name");
         JsonElement typeElement = macroJson.get("type");
         JsonElement commandsElement = macroJson.get("commands");
         if (!nameElement.isJsonPrimitive()
            || !nameElement.getAsJsonPrimitive().isString()
            || !typeElement.isJsonPrimitive()
            || !typeElement.getAsJsonPrimitive().isString()
            || !(commandsElement instanceof JsonArray commandsArray)) {
            return null;
         }

         String name = nameElement.getAsString();
         if (!isValidName(name)) {
            return null;
         }
         MacroType type = MacroType.fromName(typeElement.getAsString());
         if (type == null) {
            return null;
         }

         Keybind keybind = new Keybind(
            macroJson.get("key").getAsInt(),
            Keybind.Mode.TOGGLE,
            macroJson.get("mouse").getAsBoolean()
         );
         Macro macro = type.create(name, keybind);
         if (macroJson.has("toggled")) {
            macro.setToggled(macroJson.get("toggled").getAsBoolean());
         }

         List commands = macro.getCommands();
         for (JsonElement commandElement : commandsArray) {
            if (!commandElement.isJsonPrimitive()
               || !commandElement.getAsJsonPrimitive().isString()) {
               return null;
            }
            commands.add(commandElement.getAsString());
         }

         if (type == MacroType.HOLD) {
            while (commands.size() > 2) {
               commands.removeLast();
            }
         }
         return macro;
      } catch (RuntimeException invalidMacro) {
         return null;
      }
   }

   @Override
   public JsonElement toJson() {
      JsonArray macrosArray = new JsonArray();
      JsonObject rootObject = new JsonObject();
      Iterator<Macro> macroIterator = this.entries.iterator();
      while (macroIterator.hasNext()) {
         Macro macro = macroIterator.next();
         JsonObject macroJson = new JsonObject();
         macroJson.addProperty("name", macro.getName());
         macroJson.addProperty("toggled", macro.isToggled());
         MacroType type = macro.getType();
         macroJson.addProperty("type", type.getName());
         Keybind keybind = macro.getKeybind();
         macroJson.addProperty("key", keybind.code());
         keybind = macro.getKeybind();
         macroJson.addProperty("mouse", keybind.mouse());
         JsonArray commandsArray = new JsonArray();
         Iterator commandIterator = macro.getCommands().iterator();
         while (commandIterator.hasNext()) {
            String command = (String)commandIterator.next();
            commandsArray.add(command);
         }
         macroJson.add("commands", commandsArray);
         macrosArray.add(macroJson);
      }
      rootObject.add("macros", macrosArray);
      return rootObject;
   }

   @Override
   public String getConfigName() {
      return "macros.json";
   }

   @EventHandler
   private void onInputPress(InputPressEvent event) {
      if (ClientAccess.schizoMinecraft.screen == null && !this.isWorldUnavailable()) {
         this.runMatchingMacros(event.getCode(), event.isMouse());
      }
   }

   /**
    * A macro command can register, unregister, or replace macros while it is running.
    * Dispatch only entries present at the start and still registered when visited.
    */
   void runMatchingMacros(int keyCode, boolean mouse) {
      for (Macro macro : List.copyOf(this.entries)) {
         if (!this.isRegisteredInstance(macro) || !macro.isToggled()) {
            continue;
         }

         Keybind keybind = macro.getKeybind();
         if (keybind.mouse() == mouse && keybind.code() == keyCode) {
            macro.run();
            ClientAccess.eventBus.post(new MacroTriggerEvent(macro));
         }
      }
   }

   @EventHandler
   private void onTick(WorldRenderEvent.Main event) {
      boolean canRunHoldMacros = ClientAccess.schizoMinecraft.screen == null && !this.isWorldUnavailable();
      this.updateHoldMacros(canRunHoldMacros);
   }

   /** Hold-macro press/release commands can also modify the registry. */
   void updateHoldMacros(boolean canRunHoldMacros) {
      for (Macro macro : List.copyOf(this.entries)) {
         if (!(macro instanceof HoldMacro holdMacro) || !this.isRegisteredInstance(macro)) {
            continue;
         }
         boolean physicallyDown = canRunHoldMacros
            && holdMacro.isToggled()
            && KeyUtils.isPhysicallyDown(holdMacro.getKeybind());
         holdMacro.updatePressed(physicallyDown);
      }
   }

   private boolean isRegisteredInstance(Macro expected) {
      for (Macro registered : this.entries) {
         if (registered == expected) {
            return true;
         }
      }
      return false;
   }

   @EventHandler
   private void onMinecraftDisconnect(MinecraftDisconnectEvent event) {
      this.releaseHoldMacros();
   }
}
