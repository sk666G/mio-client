/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.movement;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.LocalPlayerMoveEvent;
import dev.schizo.client.utils.movement.MovementPhysics;


public final class VanillaSpeedHandler extends SpeedModeHandler {
   public VanillaSpeedHandler(SpeedModule speedModule) {
      super(speedModule);
   }

   @Override
   public void onMove(LocalPlayerMoveEvent event) {
      if (this.speedModule.shouldPauseInBlocks() || this.speedModule.shouldPauseInLiquid()
         || ClientAccess.schizoMinecraft.player.isFallFlying() || this.speedModule.shouldYieldToObstaclePasser()) {
         return;
      }
      double speed = this.speedModule.speedSetting.getValue();
      MovementPhysics.applyHorizontalMotion(event, MovementPhysics.applySpeedEffects(false, speed));
   }
}
