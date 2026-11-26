/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.combat;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.utils.combat.ExtraBreakTarget;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.player.SpeedMineModule;

import net.minecraft.core.BlockPos;

public final class AutoMineMiningTracker implements ClientAccess {
   private static SpeedMineModule speedMine;
   private final ElapsedTimer recentTimer = new ElapsedTimer();
   private BlockPos recentPosition;


   public BlockPos getCurrentPosition() {
      ExtraBreakTarget miningData = speedMine.getExtraBreakTarget();
      if (miningData == null) {
         return null;
      }

      return miningData.getPosition();
   }


   public boolean hasMiningActivity() {
      BlockPos recent = this.getRecentPosition();
      if (recent != null
         && !ClientAccess.schizoMinecraft.level.isEmptyBlock(recent)) {
         return true;
      }

      return speedMine.getExtraBreakTarget() != null;
   }


   public BlockPos getRecentPosition() {
      return this.recentTimer.hasElapsed(200L)
         ? null
         : this.recentPosition;
   }

   AutoMineMiningTracker() {
   }
   public void rememberPosition(BlockPos position) {
      this.recentPosition = position;

      this.recentTimer.reset();
   }


   public boolean hasRecentDelayElapsed(long delayMillis) {
      return this.recentTimer.hasElapsed(delayMillis);
   }

   public static void bindModules(ModuleRegistry registry) {
      speedMine = (SpeedMineModule)registry.getModule(SpeedMineModule.class);
   }
}
