package dev.schizo.client.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AdaptiveScanBudgetTest {
   @Test
   void keepsFullBudgetForHealthyFrames() {
      assertEquals(128, AdaptiveScanBudget.limit(128, 120, 8.0F, 0));
   }

   @Test
   void backsOffProgressivelyUnderFramePressure() {
      assertEquals(96, AdaptiveScanBudget.limit(128, 50, 19.0F, 0));
      assertEquals(64, AdaptiveScanBudget.limit(128, 30, 30.0F, 0));
      assertEquals(32, AdaptiveScanBudget.limit(128, 18, 55.0F, 0));
   }

   @Test
   void repeatedStuttersCapTheBudgetAtHalf() {
      assertEquals(64, AdaptiveScanBudget.limit(128, 120, 8.0F, 4));
   }

   @Test
   void neverProducesAnEmptyBudget() {
      assertEquals(1, AdaptiveScanBudget.limit(1, 10, 100.0F, 10));
      assertThrows(IllegalArgumentException.class, () ->
         AdaptiveScanBudget.limit(0, 120, 8.0F, 0));
   }
}
