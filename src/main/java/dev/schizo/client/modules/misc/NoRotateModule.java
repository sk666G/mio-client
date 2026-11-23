/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.misc;


import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.utils.movement.MovementPhysics;
import dev.schizo.client.managers.RotationManager;
import dev.schizo.client.utils.entity.PlayerStateUtils;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.EnumSetting;
import dev.schizo.client.settings.NamedEntry;
import dev.schizo.client.settings.Setting;

import dev.schizo.mixin.PositionMoveRotationAccessor;
import dev.schizo.mixin.ServerboundMovePlayerPacketAccessor;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public class NoRotateModule extends Module {
   private final Setting<InBlocksChoice> inBlocksSetting;
   private float spoofYaw;
   private float spoofPitch;
   private boolean spoofPending;

   private boolean isInsideBlockingShape() {
      LocalPlayer ladderPlayer = ClientAccess.schizoMinecraft.player;
      if (ladderPlayer.isSuppressingSlidingDownLadder()) {
         return false;
      }
      LocalPlayer cobwebPlayer = ClientAccess.schizoMinecraft.player;
      if (PlayerStateUtils.isInCobweb(cobwebPlayer)) {
         return false;
      }

      ClientLevel level = ClientAccess.schizoMinecraft.level;
      LocalPlayer blockPlayer = ClientAccess.schizoMinecraft.player;
      BlockPos blockPosition = blockPlayer.blockPosition();
      BlockState blockState = level.getBlockState(blockPosition);
      if (blockState.getBlock() instanceof FallingBlock) {
         return false;
      }

      ClientLevel collisionLevel = ClientAccess.schizoMinecraft.level;
      BlockPos collisionPosition = BlockPos.ZERO;
      VoxelShape collisionShape = blockState.getCollisionShape(collisionLevel, collisionPosition);
      if (collisionShape.isEmpty()) {
         return false;
      }
      LocalPlayer heightPlayer = ClientAccess.schizoMinecraft.player;
      double playerY = heightPlayer.getY();
      LocalPlayer floorPlayer = ClientAccess.schizoMinecraft.player;
      double floorY = floorPlayer.getY();
      double heightWithinBlock = playerY - Math.floor(floorY);
      if (heightWithinBlock >= collisionShape.bounds().maxY) {
         return false;
      }
      AABB localShapeBounds = collisionShape.bounds();
      LocalPlayer positionPlayer = ClientAccess.schizoMinecraft.player;
      BlockPos shapePosition = positionPlayer.blockPosition();
      AABB worldShapeBounds = localShapeBounds.move(shapePosition);
      LocalPlayer boundsPlayer = ClientAccess.schizoMinecraft.player;
      AABB playerBounds = boundsPlayer.getBoundingBox();
      AABB inflatedPlayerBounds = playerBounds.inflate(-0.05D);
      return worldShapeBounds.intersects(inflatedPlayerBounds);
   }

   @EventHandler
   private void onPacketSend(
      PacketEvent.Send event
   ) {
      if (event.getPacket() instanceof ServerboundMovePlayerPacket movementPacket
         && this.spoofPending) {
         ServerboundMovePlayerPacketAccessor packetAccessor = (ServerboundMovePlayerPacketAccessor)movementPacket;
         packetAccessor.setYaw(this.spoofYaw);
         packetAccessor.setPitch(this.spoofPitch);
         this.spoofPending = false;
      }

      Setting<InBlocksChoice> inBlocksSettingSnapshot = this.inBlocksSetting;
      if (inBlocksSettingSnapshot.getValue() == NoRotateModule.InBlocksChoice.CANCEL
         && !MovementPhysics.isLocalPlayerMoving()
         && event.getPacket() instanceof ServerboundMovePlayerPacket
         && this.isInsideBlockingShape()) {
         event.cancel();
      }
   }

   @EventHandler
   private void onPacketReceive(
      PacketEvent.Receive event
   ) {
      if (!(event.getPacket() instanceof ClientboundPlayerPositionPacket positionPacket)) {
         return;
      }

      if (this.isWorldUnavailable()) {
         return;
      }
      int playerTicks = ClientAccess.schizoMinecraft.player.tickCount;

      if (playerTicks <= (20)) {
         return;
      }
      Minecraft minecraft = ClientAccess.schizoMinecraft;
      if (minecraft.isLocalServer()) {
         return;
      }

      LocalPlayer positionPlayer = ClientAccess.schizoMinecraft.player;
      PositionMoveRotation playerPosition = PositionMoveRotation.of(positionPlayer);
      PositionMoveRotation serverChange = positionPacket.change();
      Set<Relative> relativeFlags = positionPacket.relatives();
      PositionMoveRotation absolutePosition = PositionMoveRotation.calculateAbsolute(playerPosition, serverChange, relativeFlags);
      LocalPlayer yawPlayer = ClientAccess.schizoMinecraft.player;
      float preservedYaw = yawPlayer.getYRot();
      LocalPlayer pitchPlayer = ClientAccess.schizoMinecraft.player;
      float preservedPitch = pitchPlayer.getXRot();
      if (positionPacket.relatives().contains(Relative.Y_ROT)) {
         preservedYaw = 0.0F;
      }

      if (positionPacket.relatives().contains(Relative.X_ROT)) {
         preservedPitch = 0.0F;
      }

      ((PositionMoveRotationAccessor)(Object)positionPacket.change()).schizo$setYaw(preservedYaw);
      ((PositionMoveRotationAccessor)(Object)positionPacket.change()).schizo$setPitch(preservedPitch);
      Setting<InBlocksChoice> inBlocksSettingSnapshot = this.inBlocksSetting;
      if (inBlocksSettingSnapshot.getValue() == NoRotateModule.InBlocksChoice.SPOOF
         || RotationManager.isMovementSyncEnabled()) {
         this.spoofYaw = absolutePosition.yRot();
         this.spoofPitch = absolutePosition.xRot();
         this.spoofPending = true;
      }
   }

   public NoRotateModule() {
      super("NoRotate", "Cancels the rotations server sets for you.", ModuleCategory.MISC, new String[0]);
      NoRotateModule.InBlocksChoice inBlocksDefault = NoRotateModule.InBlocksChoice.OFF;
      this.inBlocksSetting = this.add(new EnumSetting<>("InBlocks", inBlocksDefault));
   }

   enum InBlocksChoice implements NamedEntry {
      OFF("Off"),
      CANCEL("Cancel"),
      SPOOF("Spoof");

      private final String displayName;

      @Override
      public String getName() {
         return this.displayName;
      }

      InBlocksChoice(String displayName) {
         this.displayName = displayName;
      }
   }
}
