package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import dev.schizo.client.modules.misc.StashEntry;
import org.junit.jupiter.api.Test;

class StashRegistryTest {
   @Test
   void emptyRootRepresentsNoPersistedStashDataAndPreservesLiveState() {
      StashRegistry registry = new StashRegistry();
      StashEntry existing = new StashEntry("stash", 100, -200, "OVERWORLD", "example.org");
      assertTrue(registry.registerEntry(existing));

      assertDoesNotThrow(() -> registry.fromJson(new JsonObject()));

      assertEquals(1, registry.getRegistry().size());
      assertSame(existing, registry.getRegistry().iterator().next());
   }

   @Test
   void malformedNonEmptyRootThrowsAndPreservesExistingState() {
      StashRegistry registry = new StashRegistry();
      StashEntry existing = new StashEntry("stash", 100, -200, "OVERWORLD", "example.org");
      assertTrue(registry.registerEntry(existing));

      JsonObject malformed = new JsonObject();
      malformed.addProperty("unexpected", true);
      assertThrows(IllegalArgumentException.class, () -> registry.fromJson(malformed));

      assertEquals(1, registry.getRegistry().size());
      assertSame(existing, registry.getRegistry().iterator().next());
   }
}
