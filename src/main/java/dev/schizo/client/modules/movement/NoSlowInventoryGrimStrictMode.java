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
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.InventoryActionEvent;
import dev.schizo.client.events.LocalPlayerPreTickEvent;
import dev.schizo.client.events.MultiPlayerGameModeHandleInventoryMouseClickEvent;
import dev.schizo.client.managers.RotationManager;
import dev.schizo.client.utils.network.PacketSender;
import dev.schizo.client.utils.inventory.InventoryUtils;
import dev.schizo.client.utils.inventory.NoSlowInventoryMode;
import dev.schizo.client.integrations.ProtocolCompatibility;
import dev.schizo.mixin.LocalPlayerAccessor;
import java.util.ArrayDeque;
import java.util.Queue;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

public class NoSlowInventoryGrimStrictMode extends NoSlowInventoryMode {
   private final Queue<Runnable> pendingActions = new ArrayDeque<>();
   private boolean flushPending;
   private boolean replayingAction;
   private boolean inputActive;
   private boolean inventoryInputSuppressed;

   private void flushQueuedActions() {
      if (this.pendingActions.isEmpty()) return;
      long attempts = 0L;
      while (!this.pendingActions.isEmpty()) {
         if (++attempts > 50L) break;
         Runnable action = this.pendingActions.poll();
         if (action != null) this.runReplayedAction(action);
      }
      this.pendingActions.clear();
   }

   public NoSlowInventoryGrimStrictMode(NoSlowModule noSlow) {
      super(noSlow);
   }

   private boolean isDelayablePacket(Packet packet) {
      return packet instanceof ServerboundUseItemPacket
         || packet instanceof ServerboundSwingPacket
         || packet instanceof PosRot && RotationManager.isSilentRotationMode();
   }

   @Override
   public void onPacketSend(PacketEvent.Send event) {
      if (this.replayingAction) return;
      if (this.usesLegacyClickProtocol()) return;
      Packet packet = event.getPacket();
      if (this.isDelayablePacket(packet) && this.inputActive && !this.pendingActions.isEmpty()) {
         this.queueOrRunAction(() -> {
            PacketSender.send(event.getPacket());
         });
         event.cancel();
      }
      if (event.getPacket() instanceof ServerboundContainerClosePacket closePacket && this.inputActive) {
         this.queueOrRunAction(() -> PacketSender.send(closePacket));
         event.cancel();
      }
      if (event.getPacket() instanceof ServerboundPlayerCommandPacket command) {
         if (command.getAction() == Action.START_FALL_FLYING) {
            this.inputActive = false;
            this.flushQueuedActions();
         }
      }
   }

   @Override
   public void onInventoryClickIntercept(MultiPlayerGameModeHandleInventoryMouseClickEvent.AfterStackHash event) {
      if (this.usesLegacyClickProtocol()) return;
      if (this.replayingAction || !this.inputActive) return;
      int containerId = event.getContainerId();
      int slotId = event.getSlotId();
      int button = event.getButton();
      ClickType clickType = event.getClickType();
      this.queueOrRunAction(() -> {
         ClientAccess.schizoMinecraft.gameMode.handleInventoryMouseClick(containerId, slotId, button, clickType, ClientAccess.schizoMinecraft.player);
      });
      event.cancel();
   }

   @Override
   public void beforeMenuClick(MultiPlayerGameModeHandleInventoryMouseClickEvent.BeforeContainerClick event) {
      if (this.usesLegacyClickProtocol()) return;
      if (this.replayingAction) {
         event.cancel();
      }
   }

   @Override
   public void onTickStart(LocalPlayerPreTickEvent event) {
      if (this.flushPending) {
         this.flushQueuedActions();
         this.flushPending = false;
      }
      this.inputActive = this.hasPlayerInput();
      boolean keepPendingFlush = false;
      if (this.inputActive && ClientAccess.schizoMinecraft.screen instanceof AbstractContainerScreen) {
         ItemStack cursorStack = InventoryUtils.getCarriedStack();
         keepPendingFlush = !cursorStack.isEmpty();
      }
      if (!keepPendingFlush) this.flushPending = !this.pendingActions.isEmpty();
      if (this.flushPending) {
         ClientContext.inputOverrides.requestReset(this.noSlow);
         ClientAccess.schizoMinecraft.player.setSprinting(false);
      }
   }

   @Override
   public void onInventoryAction(InventoryActionEvent event) {
      if (this.usesLegacyClickProtocol() && !this.inventoryInputSuppressed) {
         Input emptyInput = new Input(false, false, false, false, false, false, false);
         PacketSender.send(new ServerboundPlayerInputPacket(emptyInput));
         PacketSender.sendPlayerCommand(Action.STOP_SPRINTING);
         ((LocalPlayerAccessor)ClientAccess.schizoMinecraft.player).schizo$setLastPlayerInput(emptyInput);
         ((LocalPlayerAccessor)ClientAccess.schizoMinecraft.player).schizo$setLastSprinting(false);
         ClientContext.inputOverrides.blockSprint();
         this.inventoryInputSuppressed = true;
      }
   }

   private boolean canQueueInCurrentScreen() {
      if (ClientAccess.schizoMinecraft.screen instanceof AbstractContainerScreen screen) {
         if (!this.noSlow.containersSetting.getValue()) {
            return screen.getMenu() == ClientAccess.schizoMinecraft.player.inventoryMenu;
         }
      }
      return true;
   }

   private boolean hasPlayerInput() {
      Input input = ClientAccess.schizoMinecraft.player.input.keyPresses;
      if (input.forward()) return true;
      if (input.backward()) return true;
      if (input.left()) return true;
      if (input.right()) return true;
      if (input.jump()) return true;
      if (input.shift()) return true;
      return input.sprint();
   }

   private synchronized void runReplayedAction(Runnable action) {
      this.replayingAction = true;
      action.run();
      this.replayingAction = false;
   }

   @Override
   public void onPlayerAiStep() {
      if (this.flushPending || this.inventoryInputSuppressed) {
         ClientAccess.schizoMinecraft.player.setSprinting(false);
      }
   }

   public boolean isReplayingAction() {
      return this.replayingAction;
   }

   @Override
   public void onTickBeforePassengerCheck() {
      this.inputActive = this.hasPlayerInput();
      this.inventoryInputSuppressed = false;
   }

   private boolean usesLegacyClickProtocol() {
      return ProtocolCompatibility.getTargetVersion().olderEq_1_21_5();
   }

   public synchronized void queueOrRunAction(Runnable action) {
      if (this.replayingAction) return;
      if (this.noSlow.guiMoveSetting.getValue()) {
         if (this.canQueueInCurrentScreen()) {
            this.pendingActions.add(action);
            return;
         }
      }
      this.runReplayedAction(action);
   }
}
