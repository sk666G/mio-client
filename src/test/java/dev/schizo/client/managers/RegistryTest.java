package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.settings.NamedEntry;
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
}
