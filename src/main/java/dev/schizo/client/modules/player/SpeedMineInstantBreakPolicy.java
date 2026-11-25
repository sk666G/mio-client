package dev.schizo.client.modules.player;

import dev.schizo.client.settings.options.SpeedMineAutoSwapChoice;
import dev.schizo.client.settings.options.SpeedMineValue121Choice;
import java.util.Objects;

/** Pure decision policy for whether the current SpeedMine state may complete immediately. */
final class SpeedMineInstantBreakPolicy {
   private SpeedMineInstantBreakPolicy() {
   }

   static Decision evaluate(
      boolean noGlitchBreakGuard,
      int bestToolSlot,
      int selectedSlot,
      double breakProgress,
      double damageThreshold,
      SpeedMineAutoSwapChoice swapMode,
      SpeedMineValue121Choice value121Mode
   ) {
      Objects.requireNonNull(swapMode, "swapMode");
      Objects.requireNonNull(value121Mode, "value121Mode");
      if (!Double.isFinite(breakProgress) || !Double.isFinite(damageThreshold)
         || bestToolSlot < 0 || noGlitchBreakGuard) {
         return Decision.REJECTED;
      }

      boolean holdingBestTool = bestToolSlot == selectedSlot;
      if (swapMode == SpeedMineAutoSwapChoice.NONE && !holdingBestTool) {
         return Decision.REJECTED;
      }
      if (swapMode == SpeedMineAutoSwapChoice.SILENT
         && value121Mode != SpeedMineValue121Choice.OFF
         && !holdingBestTool) {
         return Decision.REJECTED;
      }
      if (breakProgress < damageThreshold) {
         return Decision.REJECTED;
      }
      return new Decision(true, bestToolSlot, breakProgress >= 1.0D);
   }

   record Decision(boolean canInstantBreak, int toolSlot, boolean breaksInOneTick) {
      private static final Decision REJECTED = new Decision(false, -1, false);
   }
}
