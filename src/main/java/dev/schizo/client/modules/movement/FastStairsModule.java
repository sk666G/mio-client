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
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.events.LocalPlayerPreTickEvent;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.utils.world.WorldUtils;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.phys.AABB;

public class FastStairsModule extends Module {
   private static final MutableBlockPos scanPosition;
   private final ElapsedTimer jumpTimer = new ElapsedTimer();
   private boolean roseSincePreviousTick;
   private float capturedSpeed;

   private boolean hasNearbyStairs(BlockPos center) {
      for (int x = -2; x <= 2; x++) {
         for (int z = -2; z < 2; z++) {
            scanPosition.set(center.getX() + x, center.getY(), center.getZ() + z);
            if (WorldUtils.getBlock(scanPosition) instanceof StairBlock) {
               return true;
            }
         }
      }
      return false;
   }

   public FastStairsModule() {
      super("FastStairs", "Makes you move faster on stair blocks.", ModuleCategory.MOVEMENT);
   }

   static {
      scanPosition = new MutableBlockPos();
   }

   @EventHandler
   public void onLocalPlayerTickEvent(LocalPlayerTickEvent event) {
      this.capturedSpeed = (float)ClientContext.movementSpeedTracker.getCurrentSpeed();
      double previousY = ClientAccess.schizoMinecraft.player.yo;
      this.roseSincePreviousTick = previousY < ClientAccess.schizoMinecraft.player.getY();
   }

   @EventHandler
   public void onLocalPlayerPreTickEvent(LocalPlayerPreTickEvent event) {
      LocalPlayer player = ClientAccess.schizoMinecraft.player;
      AABB feetBounds = player.getBoundingBox().move(0.0, -0.1D, 0.0);
      if (BlockPos.betweenClosedStream(feetBounds).noneMatch(pos -> WorldUtils.getBlock(pos) instanceof StairBlock)) {
         return;
      }
      BlockPos playerPosition = BlockPos.containing(player.getX(), player.getY(), player.getZ());
      if (!player.onGround() || !this.roseSincePreviousTick || player.isCreative() || player.isSpectator()) {
         return;
      }
      if (!this.hasNearbyStairs(playerPosition.above()) || this.capturedSpeed < 20.0F) {
         return;
      }
      if (this.jumpTimer.hasElapsedAndReset(150L)) {
         return;
      }
      ClientContext.inputOverrides.requestJump(true);
   }
}
