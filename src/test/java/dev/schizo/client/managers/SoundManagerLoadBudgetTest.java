package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SoundManagerLoadBudgetTest {
   @Test
   void reservesBytesAndFileSlotsTogether() {
      SoundManager.CustomSoundLoadBudget budget = new SoundManager.CustomSoundLoadBudget(10L, 2);

      assertTrue(budget.reserve(4));
      assertEquals(6L, budget.remainingBytes());
      assertEquals(1, budget.remainingFiles());
      assertTrue(budget.reserve(6));
      assertTrue(budget.exhausted());
      assertFalse(budget.reserve(0));
   }

   @Test
   void rejectsReservationThatExceedsRemainingBytesWithoutConsumingCapacity() {
      SoundManager.CustomSoundLoadBudget budget = new SoundManager.CustomSoundLoadBudget(5L, 2);

      assertFalse(budget.reserve(6));
      assertEquals(5L, budget.remainingBytes());
      assertEquals(2, budget.remainingFiles());
   }

   @Test
   void rejectsInvalidBudgetAndNegativeSoundSize() {
      assertThrows(IllegalArgumentException.class, () -> new SoundManager.CustomSoundLoadBudget(0L, 1));
      assertThrows(IllegalArgumentException.class, () -> new SoundManager.CustomSoundLoadBudget(1L, 0));

      SoundManager.CustomSoundLoadBudget budget = new SoundManager.CustomSoundLoadBudget(1L, 1);
      assertThrows(IllegalArgumentException.class, () -> budget.reserve(-1));
   }
}
