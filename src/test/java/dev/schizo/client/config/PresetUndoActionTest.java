package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.managers.MacroRegistry;
import dev.schizo.client.modules.Keybind;
import dev.schizo.client.modules.misc.Macro;
import dev.schizo.client.settings.options.MacroType;
import dev.schizo.client.settings.options.PresetCategory;
import org.junit.jupiter.api.Test;

class PresetUndoActionTest {
   @Test
   void macroLoadUndoRestoresMacroCategorySnapshot() throws Exception {
      MacroRegistry registry = new MacroRegistry();
      MacroRegistry previousRegistry = ClientContext.macroRegistry;
      ClientContext.macroRegistry = registry;
      try {
         Macro original = MacroType.SIMPLE.create("Original", Keybind.toggle(65, false));
         original.getCommands().add("help");
         assertTrue(registry.registerEntry(original));

         PresetUndoAction undo = PresetUndoAction.beforeLoad(PresetCategory.MACRO, "macro-set");

         JsonObject replacementRoot = new JsonObject();
         JsonArray replacementMacros = new JsonArray();
         JsonObject replacement = new JsonObject();
         replacement.addProperty("name", "Replacement");
         replacement.addProperty("type", "simple");
         replacement.addProperty("key", 66);
         replacement.addProperty("mouse", false);
         replacement.addProperty("toggled", true);
         JsonArray commands = new JsonArray();
         commands.add("toggle Example");
         replacement.add("commands", commands);
         replacementMacros.add(replacement);
         replacementRoot.add("macros", replacementMacros);

         registry.fromJson(replacementRoot);
         assertTrue(registry.findByName("Replacement").isPresent());
         assertTrue(registry.findByName("Original").isEmpty());

         undo.undo();

         Macro restored = registry.findByName("Original").orElseThrow();
         assertEquals(1, restored.getCommands().size());
         assertEquals("help", restored.getCommands().getFirst());
         assertTrue(registry.findByName("Replacement").isEmpty());
      } finally {
         ClientContext.macroRegistry = previousRegistry;
         ClientAccess.eventBus.unsubscribe(registry);
      }
   }

   @Test
   void failedUndoRemainsAvailableForRetry() throws Exception {
      MacroRegistry registry = new MacroRegistry();
      MacroRegistry previousRegistry = ClientContext.macroRegistry;
      ClientContext.macroRegistry = registry;
      try {
         Macro original = MacroType.SIMPLE.create("Original", Keybind.toggle(65, false));
         assertTrue(registry.registerEntry(original));

         PresetUndoAction undo = PresetUndoAction.beforeLoad(PresetCategory.MACRO, "macro-set");
         PresetUndoState state = new PresetUndoState();
         state.push(undo);

         ClientContext.macroRegistry = null;
         assertThrows(NullPointerException.class, state::undo);

         ClientContext.macroRegistry = registry;
         assertSame(undo, state.undo());
         assertNull(state.undo());
      } finally {
         ClientContext.macroRegistry = previousRegistry;
         ClientAccess.eventBus.unsubscribe(registry);
      }
   }

   @Test
   void loadUndoRequiresCategory() {
      assertThrows(
         IllegalArgumentException.class,
         () -> PresetUndoAction.beforeLoad(null, "preset")
      );
   }
}
