package dev.schizo.client.diagnostics;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MixinHealthRegistryTest {
   @AfterEach
   void clear() {
      MixinHealthRegistry.clearObserved();
   }

   @Test
   void unknownHooksDoNotPolluteCriticalHealth() {
      MixinHealthRegistry.Snapshot before = MixinHealthRegistry.snapshot();
      MixinHealthRegistry.markSeen("not-a-critical-hook");
      assertEquals(before, MixinHealthRegistry.snapshot());
   }

   @Test
   void recordsCriticalHooksIdempotently() {
      int expected = MixinHealthRegistry.snapshot().expected();
      MixinHealthRegistry.markSeen("minecraft.tick");
      MixinHealthRegistry.markSeen("minecraft.tick");

      MixinHealthRegistry.Snapshot snapshot = MixinHealthRegistry.snapshot();
      assertEquals(expected, snapshot.expected());
      assertEquals(1, snapshot.seen());
      assertFalse(snapshot.unseen().contains("minecraft.tick"));
   }
}
