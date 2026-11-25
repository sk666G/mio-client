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
