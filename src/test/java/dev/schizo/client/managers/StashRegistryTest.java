package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import dev.schizo.client.modules.misc.StashEntry;
import org.junit.jupiter.api.Test;

class StashRegistryTest {
   @Test
   void malformedRootThrowsAndPreservesExistingState() {
      StashRegistry registry = new StashRegistry();
      StashEntry existing = new StashEntry("stash", 100, -200, "OVERWORLD", "example.org");
      assertTrue(registry.registerEntry(existing));

      assertThrows(IllegalArgumentException.class, () -> registry.fromJson(new JsonObject()));

      assertEquals(1, registry.getRegistry().size());
      assertSame(existing, registry.getRegistry().iterator().next());
   }
}
