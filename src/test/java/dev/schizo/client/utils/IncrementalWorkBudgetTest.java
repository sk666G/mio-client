package dev.schizo.client.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class IncrementalWorkBudgetTest {
   @Test
   void countOnlyBudgetStopsAtLimit() {
      assertTrue(IncrementalWorkBudget.canContinue(0, 4));
      assertTrue(IncrementalWorkBudget.canContinue(3, 4));
      assertFalse(IncrementalWorkBudget.canContinue(4, 4));
   }

   @Test
   void timedBudgetAlwaysAllowsFirstItemThenHonorsElapsedTime() {
      assertTrue(IncrementalWorkBudget.canContinue(0, 1000L, 8, 10L));
      assertTrue(IncrementalWorkBudget.canContinue(1, 9L, 8, 10L));
      assertFalse(IncrementalWorkBudget.canContinue(1, 10L, 8, 10L));
      assertFalse(IncrementalWorkBudget.canContinue(8, 0L, 8, 10L));
   }

   @Test
   void rejectsInvalidBudgets() {
      assertThrows(IllegalArgumentException.class, () -> IncrementalWorkBudget.canContinue(-1, 2));
      assertThrows(IllegalArgumentException.class, () -> IncrementalWorkBudget.canContinue(0, 0));
      assertThrows(IllegalArgumentException.class,
         () -> IncrementalWorkBudget.canContinue(0, -1L, 2, 1L));
      assertThrows(IllegalArgumentException.class,
         () -> IncrementalWorkBudget.canContinue(0, 0L, 2, 0L));
   }
}
