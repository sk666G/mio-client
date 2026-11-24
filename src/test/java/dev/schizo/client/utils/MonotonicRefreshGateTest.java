package dev.schizo.client.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MonotonicRefreshGateTest {
   @Test
   void allowsTheFirstRefreshRegardlessOfNanoTimeOrigin() {
      MonotonicRefreshGate gate = new MonotonicRefreshGate(250L);

      assertTrue(gate.shouldRefresh(Long.MIN_VALUE, false));
   }

   @Test
   void enforcesTheIntervalAndAllowsTheExactBoundary() {
      MonotonicRefreshGate gate = new MonotonicRefreshGate(250L);

      assertTrue(gate.shouldRefresh(100L, false));
      assertFalse(gate.shouldRefresh(349L, false));
      assertTrue(gate.shouldRefresh(350L, false));
   }

   @Test
   void forcedRefreshRestartsTheCadence() {
      MonotonicRefreshGate gate = new MonotonicRefreshGate(250L);

      assertTrue(gate.shouldRefresh(1_000L, false));
      assertTrue(gate.shouldRefresh(1_050L, true));
      assertFalse(gate.shouldRefresh(1_299L, false));
      assertTrue(gate.shouldRefresh(1_300L, false));
   }

   @Test
   void elapsedTimeCheckSurvivesSignedLongWraparound() {
      MonotonicRefreshGate gate = new MonotonicRefreshGate(50L);

      assertTrue(gate.shouldRefresh(Long.MAX_VALUE - 100L, false));
      assertFalse(gate.shouldRefresh(Long.MIN_VALUE - 51L, false));
      assertTrue(gate.shouldRefresh(Long.MIN_VALUE + 10L, false));
   }

   @Test
   void rejectsNonPositiveIntervals() {
      assertThrows(IllegalArgumentException.class, () -> new MonotonicRefreshGate(0L));
      assertThrows(IllegalArgumentException.class, () -> new MonotonicRefreshGate(-1L));
   }
}
