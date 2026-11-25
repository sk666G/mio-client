package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.modules.Keybind;
import dev.schizo.client.modules.misc.Macro;
import dev.schizo.client.settings.NamedEntry;
import dev.schizo.client.settings.options.MacroType;
import dev.schizo.client.utils.inventory.Kit;
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
   void kitRegistryRejectsCaseOnlyDuplicateNames() {
      KitRegistry registry = new KitRegistry();
      Kit first = new Kit("Crystal");
      Kit duplicate = new Kit("crystal");

      assertTrue(registry.registerEntry(first));
      assertFalse(registry.registerEntry(duplicate));
      assertSame(first, registry.findByName("CRYSTAL").orElseThrow());
   }

   @Test
   void kitRenamePreservesSelectionAndRejectsConflicts() {
      KitRegistry registry = new KitRegistry();
      Kit first = new Kit("First");
      Kit second = new Kit("Second");
      first.getItemsBySlot().put(0, new Kit.ItemDescriptor(1, null));
      assertTrue(registry.registerEntry(first));
      assertTrue(registry.registerEntry(second));
      registry.setCurrentKitName("FIRST");

      assertFalse(registry.renameKit(first, "second"));
      assertSame(first, KitRegistry.getCurrentKit(registry));

      assertTrue(registry.renameKit(first, "Renamed"));
      Kit current = KitRegistry.getCurrentKit(registry);
      assertNotNull(current);
      assertEquals("Renamed", current.getName());
      assertEquals(new Kit.ItemDescriptor(1, null), current.getItemsBySlot().get(0));
   }

   @Test
   void unregisteringSelectedKitClearsSelection() {
      KitRegistry registry = new KitRegistry();
      Kit kit = new Kit("Selected");
      assertTrue(registry.registerEntry(kit));
      registry.setCurrentKitName("selected");

      assertTrue(registry.unregisterKit(kit));
      assertNull(KitRegistry.getCurrentKit(registry));
      assertTrue(registry.getRegistry().isEmpty());
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
