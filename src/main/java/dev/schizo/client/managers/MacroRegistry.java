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
import com.google.gson.JsonParser;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

      if (!this.entries.remove(previous)) {
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

      this.entries.clear();
      this.entries.addAll(loaded);
   }

   /**
    * Decode and validate clipboard data before touching the live registry.
    * Clipboard imports retain their historical enabled-by-default behavior.
    */
   public static Macro decodeClipboardMacro(String encoded) {
      if (encoded == null || encoded.isBlank()) {
         throw new IllegalArgumentException("Empty macro clipboard");
      }
      final String decoded;
      try {
         decoded = StandardCharsets.UTF_8.newDecoder()
            .decode(ByteBuffer.wrap(Base64.getDecoder().decode(encoded))).toString();
      } catch (CharacterCodingException invalidEncoding) {
         throw new IllegalArgumentException("Invalid macro clipboard encoding", invalidEncoding);
      }

      if (!(JsonParser.parseString(decoded) instanceof JsonObject config)) {
         throw new IllegalArgumentException("Macro clipboard must contain an object");
      }

      JsonElement key = config.get("key");
      JsonElement mouse = config.get("mouse");
      if (key == null || !key.isJsonPrimitive() || !key.getAsJsonPrimitive().isNumber()
         || mouse == null || !mouse.isJsonPrimitive() || !mouse.getAsJsonPrimitive().isBoolean()) {
         throw new IllegalArgumentException("Invalid macro key binding");
      }
      try {
         key.getAsBigDecimal().intValueExact();
      } catch (ArithmeticException | NumberFormatException invalidKey) {
         throw new IllegalArgumentException("Invalid macro key code", invalidKey);
      }

      config.remove("toggled");
      Macro macro = parseMacro(config);
      if (macro == null) {
         throw new IllegalArgumentException("Invalid macro clipboard configuration");
      }
      return macro;
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
         Iterator<Macro> macroIterator = this.entries.iterator();
         while (macroIterator.hasNext()) {
            Macro macro = macroIterator.next();
            if (macro.isToggled()) {
               Keybind keybind = macro.getKeybind();
               boolean mouse = keybind.mouse();
               if (mouse == event.isMouse()) {
                  keybind = macro.getKeybind();
                  int keyCode = keybind.code();
                  if (keyCode == event.getCode()) {
                     macro.run();
                     ClientAccess.eventBus.post(new MacroTriggerEvent(macro));
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onTick(WorldRenderEvent.Main event) {
      if (ClientAccess.schizoMinecraft.screen == null && !this.isWorldUnavailable()) {
         Iterator<Macro> macroIterator = this.entries.iterator();
         while (macroIterator.hasNext()) {
            Macro macro = macroIterator.next();
            if (macro instanceof HoldMacro holdMacro) {
               if (holdMacro.isToggled()) {
                  Keybind keybind = holdMacro.getKeybind();
                  boolean physicallyDown = KeyUtils.isPhysicallyDown(keybind);
                  holdMacro.updatePressed(physicallyDown);
               }
            }
         }
      }
   }
}
