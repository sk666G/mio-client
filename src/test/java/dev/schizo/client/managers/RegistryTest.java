package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.settings.NamedEntry;
import dev.schizo.client.modules.Keybind;
import dev.schizo.client.modules.misc.Macro;
import dev.schizo.client.settings.options.MacroType;
import org.junit.jupiter.api.Test;

class RegistryTest {
   @Test
   void unregisterReportsWhetherAnEntryWasActuallyRemoved() {
      Registry<NamedEntry> registry = new Registry<>();
      NamedEntry entry = () -> "alpha";

      assertTrue(registry.registerEntry(entry));
      assertTrue(registry.unregisterEntry(entry));
      assertFalse(registry.unregisterEntry(entry));
   }

   @Test
   void unregisterRejectsNullWithoutMutatingTheRegistry() {
      Registry<NamedEntry> registry = new Registry<>();
      NamedEntry entry = () -> "alpha";
      registry.registerEntry(entry);

      assertFalse(registry.unregisterEntry(null));
      assertEquals(1, registry.getRegistry().size());
   }

   @Test
   void duplicateNamesRemainRejectedWithoutReplacingTheOriginal() {
      Registry<NamedEntry> registry = new Registry<>();
      NamedEntry first = () -> "alpha";
      NamedEntry duplicate = () -> "alpha";

      assertTrue(registry.registerEntry(first));
      assertFalse(registry.registerEntry(duplicate));
      assertSame(first, registry.getRegistry().iterator().next());
   }

   @Test
   void macroRegistryRejectsCaseOnlyDuplicateNames() {
      MacroRegistry registry = new MacroRegistry();
      Macro first = macro("Home");
      Macro duplicate = macro("home");

      assertTrue(registry.registerEntry(first));
      assertFalse(registry.registerEntry(duplicate));
      assertSame(first, registry.findByName("HOME").orElseThrow());
   }

   @Test
   void macroReplacementRejectsConflictingNameWithoutDroppingOriginal() {
      MacroRegistry registry = new MacroRegistry();
      Macro first = macro("First");
      Macro second = macro("Second");
      assertTrue(registry.registerEntry(first));
      assertTrue(registry.registerEntry(second));

      assertFalse(registry.replaceMacro(first, macro("SECOND")));
      assertSame(first, registry.findByName("first").orElseThrow());
      assertSame(second, registry.findByName("second").orElseThrow());
      assertEquals(2, registry.getRegistry().size());
   }

   private static Macro macro(String name) {
      return MacroType.SIMPLE.create(name, new Keybind(1, Keybind.Mode.TOGGLE, false));
   }
}
