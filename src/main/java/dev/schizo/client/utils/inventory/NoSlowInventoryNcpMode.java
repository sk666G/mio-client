/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.inventory;

import dev.schizo.client.events.EventStage;
import dev.schizo.client.utils.entity.PlayerStateTracker;
import dev.schizo.client.utils.movement.MovementPhysics;
import dev.schizo.client.utils.network.PacketSender;

import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.InventoryActionEvent;
import dev.schizo.client.events.LocalPlayerPreTickEvent;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.exploit.MultiTaskModule;
import dev.schizo.client.modules.movement.NoSlowModule;
import dev.schizo.client.settings.options.MultiTaskModeChoice;
import dev.schizo.client.settings.options.NoSlowModeChoice;

import dev.schizo.mixin.ServerboundMovePlayerPacketAccessor;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;

public class NoSlowInventoryNcpMode extends NoSlowInventoryMode {
   private static MultiTaskModule multiTask;
   private final ElapsedTimer airborneTimer = new ElapsedTimer();
   private boolean groundSpoofPending;
   private boolean positionOffsetSent;

   public NoSlowInventoryNcpMode(NoSlowModule noSlow) {
      super(noSlow);
   }

   

   @Override
   public void onInventoryAction(InventoryActionEvent event) {
      Boolean guiMoveEnabled = this.noSlow.guiMoveSetting.getValue();
      if (!guiMoveEnabled) return;
      NoSlowModule guiModule = this.noSlow;
      if (!guiModule.isGuiMoveAllowed()) return;
      if (event.getStage() == EventStage.Pre) {
         boolean releaseItem = true;
         if (multiTask.getMode() == MultiTaskModeChoice.ALL) {
            releaseItem = this.noSlow.modeSetting.getValue() == NoSlowModeChoice.CONSTANTIAM;
         }
         if (releaseItem && !this.noSlow.releasedUsingItemThisTick) {
            MultiPlayerGameMode gameMode = ClientAccess.schizoMinecraft.gameMode;
            LocalPlayer releasePlayer = ClientAccess.schizoMinecraft.player;
            gameMode.releaseUsingItem(releasePlayer);
            this.noSlow.releasedUsingItemThisTick = true;
         }
         if (!MovementPhysics.isLocalPlayerMoving()) return;
         if (ClientContext.playerStateTracker.getGroundedTicks() > 1 && !this.positionOffsetSent) {
            double x = ClientAccess.schizoMinecraft.player.getX();
            double offsetY = ClientAccess.schizoMinecraft.player.getY() + 0.0626D;
            double z = ClientAccess.schizoMinecraft.player.getZ();
            PacketSender.sendPosition(x, offsetY, z, false);
            this.positionOffsetSent = true;
            this.groundSpoofPending = true;
         }
         LocalPlayer sprintPlayer = ClientAccess.schizoMinecraft.player;
         if (sprintPlayer.isSprinting()) {
            ServerboundPlayerCommandPacket stopSprinting = new ServerboundPlayerCommandPacket(ClientAccess.schizoMinecraft.player, Action.STOP_SPRINTING);
            PacketSender.send(stopSprinting);
         }
         LocalPlayer shiftPlayer = ClientAccess.schizoMinecraft.player;
         if (shiftPlayer.isShiftKeyDown()) {
            PlayerStateTracker shiftState = ClientContext.playerStateTracker;
            shiftState.setSneaking(false);
         }
      } else if (event.getStage() == EventStage.Post && MovementPhysics.isLocalPlayerMoving()) {
         LocalPlayer sprintPlayer = ClientAccess.schizoMinecraft.player;
         if (sprintPlayer.isSprinting()) {
            ServerboundPlayerCommandPacket startSprinting = new ServerboundPlayerCommandPacket(ClientAccess.schizoMinecraft.player, Action.START_SPRINTING);
            PacketSender.send(startSprinting);
         }
         LocalPlayer shiftPlayer = ClientAccess.schizoMinecraft.player;
         if (shiftPlayer.isShiftKeyDown()) {
            PlayerStateTracker shiftState = ClientContext.playerStateTracker;
            shiftState.setSneaking(true);
         }
      }
   }

   @Override
   public void onTickStart(LocalPlayerPreTickEvent event) {
      LocalPlayer groundPlayer = ClientAccess.schizoMinecraft.player;
      if (!groundPlayer.onGround()) {
         this.airborneTimer.reset();
      }
      this.positionOffsetSent = false;
   }

   @Override
   public void onPacketSend(PacketEvent.Send event) {
      if (!(event.getPacket() instanceof ServerboundMovePlayerPacket packet)) return;
      ServerboundMovePlayerPacketAccessor accessor = (ServerboundMovePlayerPacketAccessor)packet;
      Boolean guiMoveEnabled = this.noSlow.guiMoveSetting.getValue();
      if (!guiMoveEnabled || !this.groundSpoofPending) return;
      if (ClientAccess.schizoMinecraft.player.isHandsBusy()) return;
      LocalPlayer groundPlayer = ClientAccess.schizoMinecraft.player;
      if (!groundPlayer.onGround()) return;
      ElapsedTimer timer = this.airborneTimer;
      if (!timer.hasElapsed(200L)) return;
      if (ClientAccess.schizoMinecraft.player.isFallFlying()) return;
      accessor.setOnGround(false);
      this.groundSpoofPending = false;
   }

   public static void bindReferencedModules(ModuleRegistry registry) {
      multiTask = (MultiTaskModule)registry.getModule(
         MultiTaskModule.class
      );
   }
}
