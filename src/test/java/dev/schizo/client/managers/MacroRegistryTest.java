package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.schizo.client.modules.Keybind;
import dev.schizo.client.modules.misc.Macro;
import dev.schizo.client.settings.options.MacroType;
import org.junit.jupiter.api.Test;

class MacroRegistryTest {
   @Test
   void registrationRejectsCaseOnlyDuplicateNames() {
      MacroRegistry registry = new MacroRegistry();

      assertTrue(registry.registerEntry(macro("Home", 1)));
      assertFalse(registry.registerEntry(macro("home", 2)));
      assertEquals(1, registry.getRegistry().size());
      assertEquals(1, registry.findByName("HOME").orElseThrow().getKeybind().code());
   }

   @Test
   void reloadReplacesOldStateAndCollapsesCaseOnlyDuplicates() {
      MacroRegistry registry = new MacroRegistry();
      assertTrue(registry.registerEntry(macro("old", 1)));

      JsonObject root = new JsonObject();
      JsonArray macros = new JsonArray();
      macros.add(serializedMacro("Build", 7, "say first"));
      macros.add(serializedMacro("build", 8, "say duplicate"));
      root.add("macros", macros);

      registry.fromJson(root);

      assertTrue(registry.findByName("old").isEmpty());
      assertEquals(1, registry.getRegistry().size());
      Macro loaded = registry.findByName("BUILD").orElseThrow();
      assertEquals(7, loaded.getKeybind().code());
      assertEquals("say first", loaded.getCommands().getFirst());
   }

   @Test
   void malformedRootDoesNotDiscardExistingState() {
      MacroRegistry registry = new MacroRegistry();
      assertTrue(registry.registerEntry(macro("keep", 1)));

      assertThrows(IllegalArgumentException.class, () -> registry.fromJson(new JsonObject()));

      assertTrue(registry.findByName("keep").isPresent());
      assertEquals(1, registry.getRegistry().size());
   }

   @Test
   void replacementRejectsNameCollisionWithoutRemovingOriginal() {
      MacroRegistry registry = new MacroRegistry();
      Macro first = macro("first", 1);
      Macro second = macro("second", 2);
      assertTrue(registry.registerEntry(first));
      assertTrue(registry.registerEntry(second));

      Macro collidingReplacement = macro("SECOND", 3);
      assertFalse(registry.replaceMacro(first, collidingReplacement));

      assertSame(first, registry.findByName("first").orElseThrow());
      assertSame(second, registry.findByName("second").orElseThrow());
      assertEquals(2, registry.getRegistry().size());
   }

   @Test
   void upsertReplacesExistingMacroCaseInsensitively() {
      MacroRegistry registry = new MacroRegistry();
      Macro original = macro("utility", 1);
      Macro replacement = macro("UTILITY", 9);
      assertTrue(registry.registerEntry(original));

      assertTrue(registry.upsertMacro(replacement));

      assertEquals(1, registry.getRegistry().size());
      assertSame(replacement, registry.findByName("utility").orElseThrow());
      assertEquals(9, registry.findByName("utility").orElseThrow().getKeybind().code());
   }

   @Test
   void holdMacroLoadKeepsAtMostPressAndReleaseCommands() {
      JsonObject root = new JsonObject();
      JsonArray macros = new JsonArray();
      JsonObject hold = serializedMacro("hold", 4, "press");
      hold.addProperty("type", "hold");
      hold.getAsJsonArray("commands").add("release");
      hold.getAsJsonArray("commands").add("ignored");
      macros.add(hold);
      root.add("macros", macros);

      MacroRegistry registry = new MacroRegistry();
      registry.fromJson(root);

      Macro loaded = registry.findByName("hold").orElseThrow();
      assertEquals(2, loaded.getCommands().size());
      assertEquals("press", loaded.getCommands().getFirst());
      assertEquals("release", loaded.getCommands().getLast());
   }

   @Test
   void invalidEntriesAreSkippedWhileValidEntriesSurvive() {
      MacroRegistry registry = new MacroRegistry();
      JsonObject root = new JsonObject();
      JsonArray macros = new JsonArray();
      macros.add(serializedMacro("First", 7, "say first"));
      JsonObject unknownType = serializedMacro("Broken", 8, "say broken");
      unknownType.addProperty("type", "unknown-type");
      macros.add(unknownType);
      JsonObject invalidCommand = serializedMacro("Invalid", 9, "say invalid");
      invalidCommand.getAsJsonArray("commands").add(123);
      macros.add(invalidCommand);
      macros.add(serializedMacro("Last", 10, "say last"));
      root.add("macros", macros);

      registry.fromJson(root);

      assertEquals(2, registry.getRegistry().size());
      assertTrue(registry.findByName("first").isPresent());
      assertTrue(registry.findByName("last").isPresent());
      assertTrue(registry.findByName("broken").isEmpty());
      assertTrue(registry.findByName("invalid").isEmpty());
   }

   @Test
   void serializationRoundTripPreservesDisabledStateAndMouseBinding() {
      MacroRegistry registry = new MacroRegistry();
      Macro original = MacroType.SIMPLE.create("Unicode", new Keybind(2, Keybind.Mode.TOGGLE, true));
      original.setToggled(false);
      original.getCommands().add("say café");
      assertTrue(registry.registerEntry(original));

      MacroRegistry restored = new MacroRegistry();
      restored.fromJson(registry.toJson());
      Macro loaded = restored.findByName("unicode").orElseThrow();

      assertEquals("Unicode", loaded.getName());
      assertEquals(MacroType.SIMPLE, loaded.getType());
      assertFalse(loaded.isToggled());
      assertTrue(loaded.getKeybind().mouse());
      assertEquals(2, loaded.getKeybind().code());
      assertEquals(original.getCommands(), loaded.getCommands());
   }

   @Test
   void explicitEmptyArrayClearsPreviouslyLoadedMacros() {
      MacroRegistry registry = new MacroRegistry();
      assertTrue(registry.registerEntry(macro("old", 1)));
      JsonObject root = new JsonObject();
      root.add("macros", new JsonArray());

      registry.fromJson(root);

      assertTrue(registry.getRegistry().isEmpty());
   }

   @Test
   void clipboardDecodesUtf8AndPreservesLegacyEnabledDefault() {
      JsonObject config = serializedMacro("Café", 7, "say café");
      config.addProperty("toggled", false);
      Macro decoded = MacroRegistry.decodeClipboardMacro(encodeClipboard(config));
      assertEquals("Café", decoded.getName());
      assertEquals("say café", decoded.getCommands().getFirst());
      assertTrue(decoded.isToggled());
   }

   @Test
   void clipboardRejectsMalformedFieldsBeforeRegistryMutation() {
      JsonObject wrongMouse = serializedMacro("Home", 7, "say home");
      wrongMouse.addProperty("mouse", "false");
      JsonObject fractionalKey = serializedMacro("Home", 7, "say home");
      fractionalKey.addProperty("key", 7.5);
      JsonObject oversizedKey = serializedMacro("Home", 7, "say home");
      oversizedKey.addProperty("key", 2147483648L);
      JsonObject wrongCommand = serializedMacro("Home", 7, "say home");
      wrongCommand.getAsJsonArray("commands").add(12);
      for (JsonObject invalid : java.util.List.of(wrongMouse, fractionalKey, oversizedKey, wrongCommand)) {
         assertThrows(IllegalArgumentException.class,
            () -> MacroRegistry.decodeClipboardMacro(encodeClipboard(invalid)));
      }
   }

   @Test
   void clipboardRejectsInvalidBase64AndUtf8() {
      assertThrows(IllegalArgumentException.class, () -> MacroRegistry.decodeClipboardMacro("%%%"));
      String invalidUtf8 = java.util.Base64.getEncoder().encodeToString(new byte[] {(byte) 0xc3, 0x28});
      assertThrows(IllegalArgumentException.class, () -> MacroRegistry.decodeClipboardMacro(invalidUtf8));
   }

   private static String encodeClipboard(JsonObject config) {
      return java.util.Base64.getEncoder().encodeToString(
         config.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
   }

   private static Macro macro(String name, int keyCode) {
      return MacroType.SIMPLE.create(name, new Keybind(keyCode, Keybind.Mode.TOGGLE, false));
   }

   private static JsonObject serializedMacro(String name, int keyCode, String command) {
      JsonObject macro = new JsonObject();
      macro.addProperty("name", name);
      macro.addProperty("type", "simple");
      macro.addProperty("key", keyCode);
      macro.addProperty("mouse", false);
      JsonArray commands = new JsonArray();
      commands.add(command);
      macro.add("commands", commands);
      return macro;
   }
}
