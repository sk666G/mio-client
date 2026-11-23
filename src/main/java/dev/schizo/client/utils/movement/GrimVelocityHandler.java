/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.movement;

import dev.schizo.client.utils.entity.PlayerStateUtils;
import dev.schizo.client.utils.network.PacketSender;
import dev.schizo.client.utils.world.WorldUtils;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.ClientPacketListenerHandleExplosionEvent;
import dev.schizo.client.events.ClientPacketListenerHandleSetEntityMotionEvent;
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.FishingHookPullEntityEvent;
import dev.schizo.client.modules.movement.VelocityModule;

import java.util.Comparator;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.phys.AABB;

public class GrimVelocityHandler extends VelocityModeHandler {
   private boolean intersectsNonEmptyBlock;
   private boolean compensationPending;
   private float lastSentYaw;
   private float lastSentPitch;

   private float getPlayerIntersectionSize(BlockPos blockPos) {
      AABB blockBounds = new AABB(blockPos);
      return (float)ClientAccess.schizoMinecraft.player.getBoundingBox().intersect(blockBounds).getSize();
   }

   public GrimVelocityHandler(VelocityModule velocityModule) {
      super(velocityModule);
   }

   private void sendBlockCompensation() {
      BlockPos target = this.findCompensationBlock();
      if (target == null || (!this.compensationPending && this.velocityModule.reduceSetting.getValue())) {
         return;
      }
      this.compensationPending = false;
      LocalPlayer player = ClientAccess.schizoMinecraft.player;
      PacketSender.sendPositionRotation(player.getX(), player.getY(), player.getZ(), this.lastSentYaw, this.lastSentPitch, player.onGround());
      PacketSender.sendPlayerAction(Action.STOP_DESTROY_BLOCK, target, Direction.DOWN);
   }

   private BlockPos findCompensationBlock() {
      AABB bounds = ClientAccess.schizoMinecraft.player.getBoundingBox();
      return BlockPos.betweenClosedStream(bounds).map(BlockPos::immutable)
         .filter(position -> WorldUtils.isAir(position)
            || (WorldUtils.hasFluid(position) && this.velocityModule.reduceSetting.getValue()))
         .max(Comparator.comparingDouble(this::getPlayerIntersectionSize)).orElse(null);
   }

   private boolean shouldLeaveMotionUnchanged() {
      boolean noCompensationBlock = this.findCompensationBlock() == null && !this.intersectsNonEmptyBlock;
      LocalPlayer player = ClientAccess.schizoMinecraft.player;
      return player.isFallFlying() || PlayerStateUtils.isInCobweb(player) || noCompensationBlock
         || !ClientContext.serverStateManager.hasElapsedSinceSetback(100L);
   }

   @Override
   public void onEntityMotion(
      ClientPacketListenerHandleSetEntityMotionEvent event
   ) {
      int entityId = event.getEntityId();
      LocalPlayer player = ClientAccess.schizoMinecraft.player;

      if (entityId == player.getId() && !this.shouldLeaveMotionUnchanged()) {
         event.cancel();
         this.compensationPending = true;
      }
   }

   @Override
   public void onFishingPull(FishingHookPullEntityEvent event) {
      if (this.velocityModule.fishingBobberSetting.getValue() && event.getEntity() == ClientAccess.schizoMinecraft.player) {
         this.compensationPending = true;
      }
   }

   @Override
   public void onSendPacket(
      PacketEvent.Send event
   ) {
      if (event.getPacket() instanceof ServerboundMovePlayerPacket movementPacket && movementPacket.hasRotation()) {
         this.lastSentYaw = movementPacket.getYRot(0.0F);
         this.lastSentPitch = movementPacket.getXRot(0.0F);
      }
   }

   @Override
   public void onExplosion(
      ClientPacketListenerHandleExplosionEvent event
   ) {
      boolean hasMotion = event.getVelocityX() != 0.0F
         || event.getVelocityY() != 0.0F
         || event.getVelocityZ() != 0.0F;
      if (hasMotion && this.velocityModule.explosionsSetting.getValue() && !this.shouldLeaveMotionUnchanged()) {
         event.setVelocityX(0.0F);
         event.setVelocityY(0.0F);
         event.setVelocityZ(0.0F);
      }
   }

   @Override
   public void onTick() {
      AABB bounds = ClientAccess.schizoMinecraft.player.getBoundingBox();
      this.intersectsNonEmptyBlock = BlockPos.betweenClosedStream(bounds)
         .anyMatch(position -> !ClientAccess.schizoMinecraft.level.isEmptyBlock(position));
      if (ClientAccess.schizoMinecraft.player.tickCount > 20 && !this.shouldLeaveMotionUnchanged()) {
         this.sendBlockCompensation();
      }
   }

}
