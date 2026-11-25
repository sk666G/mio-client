package dev.schizo.client.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FailureLogGateTest {
   @Test
   void suppressesRepeatedFailuresUntilIntervalElapses() {
      FailureLogGate gate = new FailureLogGate(100L);

      FailureLogGate.Decision first = gate.acquire(1_000L);
      assertTrue(first.shouldLog());
      assertEquals(0L, first.suppressedSinceLastLog());

      assertFalse(gate.acquire(1_050L).shouldLog());
      assertFalse(gate.acquire(1_099L).shouldLog());

      FailureLogGate.Decision next = gate.acquire(1_100L);
      assertTrue(next.shouldLog());
      assertEquals(2L, next.suppressedSinceLastLog());
   }

   @Test
   void requiresPositiveInterval() {
      assertThrows(IllegalArgumentException.class, () -> new FailureLogGate(0L));
   }
}
