package dev.schizo.client.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class LifecycleGenerationTest {
   @Test
   void invalidationMakesPreviouslyCapturedTokensStale() {
      LifecycleGeneration generation = new LifecycleGeneration();
      LifecycleGeneration.Token first = generation.capture();

      assertTrue(first.isCurrent());
      generation.invalidate();
      assertFalse(first.isCurrent());
      assertTrue(generation.capture().isCurrent());
   }

   @Test
   void guardedWorkRunsOnlyInItsCapturedGeneration() {
      LifecycleGeneration generation = new LifecycleGeneration();
      AtomicInteger runs = new AtomicInteger();

      Runnable current = generation.guard(runs::incrementAndGet);
      current.run();
      assertEquals(1, runs.get());

      Runnable stale = generation.guard(runs::incrementAndGet);
      generation.invalidate();
      stale.run();
      assertEquals(1, runs.get());
   }

   @Test
   void repeatedInvalidationNeverRevivesOldTokens() {
      LifecycleGeneration generation = new LifecycleGeneration();
      LifecycleGeneration.Token token = generation.capture();

      generation.invalidate();
      generation.invalidate();
      generation.invalidate();

      assertFalse(token.isCurrent());
   }
}
