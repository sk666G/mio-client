package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.schizo.client.modules.misc.Macro;
import org.junit.jupiter.api.Test;

class MacroRegistryConfigTest {
   @Test
   void reloadReplacesPreviouslyLoadedMacros() {
      MacroRegistry registry = new MacroRegistry();

      JsonObject firstRoot = new JsonObject();
      JsonArray firstMacros = new JsonArray();
      firstMacros.add(macroJson("Old", "simple", "say old"));
      firstRoot.add("macros", firstMacros);
      registry.fromJson(firstRoot);
      assertTrue(registry.findByName("old").isPresent());

      JsonObject secondRoot = new JsonObject();
      JsonArray secondMacros = new JsonArray();
      secondMacros.add(macroJson("New", "simple", "say new"));
      secondRoot.add("macros", secondMacros);
      registry.fromJson(secondRoot);

      assertTrue(registry.findByName("old").isEmpty());
      assertTrue(registry.findByName("new").isPresent());
      assertEquals(1, registry.getRegistry().size());
   }

   @Test
   void loadCollapsesCaseOnlyDuplicateNames() {
      MacroRegistry registry = new MacroRegistry();

      JsonObject root = new JsonObject();
      JsonArray macros = new JsonArray();
      macros.add(macroJson("Home", "simple", "say first"));
      macros.add(macroJson("home", "simple", "say second"));
      root.add("macros", macros);

      registry.fromJson(root);

      assertEquals(1, registry.getRegistry().size());
      Macro macro = registry.findByName("HOME").orElseThrow();
      assertEquals("say first", macro.getCommands().getFirst());
   }

   @Test
   void malformedRootDoesNotDiscardExistingState() {
      MacroRegistry registry = new MacroRegistry();

      JsonObject valid = new JsonObject();
      JsonArray macros = new JsonArray();
      macros.add(macroJson("Keep", "simple", "say keep"));
      valid.add("macros", macros);
      registry.fromJson(valid);

      assertThrows(IllegalArgumentException.class, () -> registry.fromJson(new JsonObject()));

      assertTrue(registry.findByName("keep").isPresent());
      assertEquals(1, registry.getRegistry().size());
   }

   @Test
   void invalidEntriesAreSkippedWithoutPublishingPartialGarbage() {
      MacroRegistry registry = new MacroRegistry();

      JsonObject root = new JsonObject();
      JsonArray macros = new JsonArray();
      macros.add(macroJson("Valid", "simple", "say valid"));

      JsonObject invalid = macroJson("Broken", "unknown-type", "say broken");
      macros.add(invalid);
      root.add("macros", macros);

      registry.fromJson(root);

      assertTrue(registry.findByName("valid").isPresent());
      assertTrue(registry.findByName("broken").isEmpty());
      assertEquals(1, registry.getRegistry().size());
   }

   private static JsonObject macroJson(String name, String type, String command) {
      JsonObject macro = new JsonObject();
      macro.addProperty("name", name);
      macro.addProperty("type", type);
      macro.addProperty("key", 1);
      macro.addProperty("mouse", false);
      macro.addProperty("toggled", true);
      JsonArray commands = new JsonArray();
      commands.add(command);
      macro.add("commands", commands);
      return macro;
   }
}
