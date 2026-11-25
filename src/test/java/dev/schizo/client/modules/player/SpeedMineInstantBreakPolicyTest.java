package dev.schizo.client.modules.player;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.settings.options.SpeedMineAutoSwapChoice;
import dev.schizo.client.settings.options.SpeedMineValue121Choice;
import org.junit.jupiter.api.Test;

class SpeedMineInstantBreakPolicyTest {
   @Test
   void guardAndInsufficientProgressReject() {
      assertFalse(evaluate(true, 2, 2, 1.0D, 0.8D).canInstantBreak());
      assertFalse(evaluate(false, 2, 2, 0.79D, 0.8D).canInstantBreak());
   }

   @Test
   void noSwapRequiresBestToolAlreadySelected() {
      var decision = SpeedMineInstantBreakPolicy.evaluate(
         false, 2, 1, 1.0D, 0.8D,
         SpeedMineAutoSwapChoice.NONE, SpeedMineValue121Choice.OFF
      );
      assertFalse(decision.canInstantBreak());
      assertEquals(-1, decision.toolSlot());
   }

   @Test
   void silentOneTwentyOneModeRequiresBestToolAlreadySelected() {
      var rejected = SpeedMineInstantBreakPolicy.evaluate(
         false, 2, 1, 1.0D, 0.8D,
         SpeedMineAutoSwapChoice.SILENT, SpeedMineValue121Choice.TICK
      );
      var allowed = SpeedMineInstantBreakPolicy.evaluate(
         false, 2, 1, 1.0D, 0.8D,
         SpeedMineAutoSwapChoice.SILENT, SpeedMineValue121Choice.OFF
      );

      assertFalse(rejected.canInstantBreak());
      assertTrue(allowed.canInstantBreak());
      assertEquals(2, allowed.toolSlot());
      assertTrue(allowed.breaksInOneTick());
   }

   @Test
   void acceptedDecisionPreservesToolAndOneTickThreshold() {
      var decision = evaluate(false, 4, 4, 0.9D, 0.8D);
      assertTrue(decision.canInstantBreak());
      assertEquals(4, decision.toolSlot());
      assertFalse(decision.breaksInOneTick());
   }

   private static SpeedMineInstantBreakPolicy.Decision evaluate(
      boolean guard, int best, int selected, double progress, double threshold
   ) {
      return SpeedMineInstantBreakPolicy.evaluate(
         guard, best, selected, progress, threshold,
         SpeedMineAutoSwapChoice.NONE, SpeedMineValue121Choice.OFF
      );
   }
}
