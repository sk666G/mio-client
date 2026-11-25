/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.events.EventStage;
import dev.schizo.client.utils.network.PacketSender;
import dev.schizo.client.utils.world.WorldUtils;

import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.InventoryActionEvent;
import dev.schizo.client.events.SelectedSlotChangeEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerSendPositionEvent;
import dev.schizo.client.events.LocalPlayerPreTickEvent;
import dev.schizo.client.events.MultiPlayerGameModeHandleInventoryMouseClickEvent;
import dev.schizo.client.events.MultiPlayerGameModeUseItemOnEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.events.bus.EventBus;
import dev.schizo.client.modules.client.AntiCheatModule;
import dev.schizo.client.modules.exploit.XCarryModule;
import dev.schizo.client.settings.Setting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public final class InventoryStateManager implements ClientAccess {
   private static final long CLICK_WINDOW_NANOS = java.util.concurrent.TimeUnit.SECONDS.toNanos(4L);
   private static final AntiCheatModule antiCheat;
   private static final XCarryModule xCarry;
   private final Map<Integer, DeferredSlotUpdate> deferredSlotUpdates = Collections.synchronizedMap(new HashMap<>());
   private final Queue<Long> clickTimestamps = new ArrayDeque<>();
   private final ElapsedTimer lastClickTimer = new ElapsedTimer();
   private final ElapsedTimer enderChestUseTimer = new ElapsedTimer();
   private volatile boolean containerOpen;
   private volatile boolean clickBatching;
   private volatile boolean clickBatchCompleted;
   private boolean localEntityEventPending;
   private int serverSelectedSlot = -1;
   private boolean slotResyncArmed;

   public void setClickBatching(boolean batching) {
      if (this.clickBatching != batching) {
         this.clickBatching = batching;

         if (!(ClientAccess.schizoMinecraft.screen instanceof AbstractContainerScreen)) {
            if (RotationManager.isMovementSyncEnabled()) {
               XCarryModule xCarryModule = xCarry;
               if (!xCarryModule.isToggled()
                  && !this.clickBatching
                  && this.clickBatchCompleted) {
                  this.close();
               }
            }
         }
         this.clickBatchCompleted = false;
      }
   }

   public InventoryStateManager() {
      EventBus eventBus = ClientAccess.eventBus;
      eventBus.subscribe(this);
   }

   static {
      antiCheat = (AntiCheatModule)ClientContext.getModule(AntiCheatModule.class);
      xCarry = (XCarryModule)ClientContext.getModule(XCarryModule.class);
   }

   public void close() {
      Packet packet = new ServerboundContainerClosePacket(ClientAccess.schizoMinecraft.player.containerMenu.containerId);
      PacketSender.send(packet);
      this.containerOpen = false;
   }

   public static boolean wouldReachClickLimit(InventoryStateManager manager, int additionalClicks) {
      java.util.Objects.requireNonNull(manager);
      return antiCheat.is2b2tEnabled() && manager.clickTimestamps.size() + additionalClicks >= 71;
   }

   private void resendHeldSlot(int slot) {
      if (RotationManager.isMovementSyncEnabled() && this.slotResyncArmed) {
         PacketSender.sendSelectedSlot((slot + 1) % 9);
         PacketSender.sendSelectedSlot(slot);
         this.slotResyncArmed = true;
      }
   }

   @EventHandler
   private void onConnectionDoSendPacketEvent(
      PacketEvent.PostSend event
   ) {
      if (event.getPacket() instanceof ServerboundContainerClickPacket
         && (
            !this.clickBatching
               || !this.clickBatchCompleted
         )) {
         EventBus capturedEventBus = ClientAccess.eventBus;
         InventoryActionEvent postEvent = new InventoryActionEvent(EventStage.Post);

         capturedEventBus.post(postEvent);
         this.clickBatchCompleted = true;
      }
   }

   @EventHandler
   private void onClientPacketListenerHandleSetHeldSlotEvent(
      SelectedSlotChangeEvent.ClientPacketListenerHandleSetHeldSlotEvent event
   ) {
      this.serverSelectedSlot = event.getSlot();
      int selectedSlot = ClientAccess.schizoMinecraft.player.getInventory().getSelectedSlot();
      Minecraft minecraft = ClientAccess.schizoMinecraft;
      if (!minecraft.isLocalServer()) {
         Setting<Boolean> noServerSlotSetting = antiCheat.noServerSlotSetting;
         if (noServerSlotSetting.getValue()) {
            int slotWord = event.getSlot();
            this.resendHeldSlot(slotWord);

            if (ClientAccess.schizoMinecraft.player.tickCount < (5)) {
               return;
            }

            event.cancel();

            if (RotationManager.isMovementSyncEnabled()) {
               TaskScheduler scheduler = ClientContext.taskScheduler;
               Runnable restoreHeldSlot = () -> {
                  PacketSender.sendSelectedSlot(selectedSlot);
                  if (ClientAccess.schizoMinecraft.player.isUsingItem()) {
                     MultiPlayerGameMode releaseGameMode = ClientAccess.schizoMinecraft.gameMode;
                     LocalPlayer releasePlayer = ClientAccess.schizoMinecraft.player;
                     releaseGameMode.releaseUsingItem(releasePlayer);
                  }
               };
               scheduler.scheduleRotationTask(restoreHeldSlot, 0);
            } else {
               PacketSender.sendSelectedSlot(selectedSlot);
            }

            return;
         }
      }
   }

   private boolean matchesOtherHeldStack(ItemStack stack, int menuSlot) {
      ItemStack offhandStack = ClientAccess.schizoMinecraft.player.getOffhandItem();
      if (ItemStack.matches(offhandStack, stack)) {
         return true;
      }

      for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
         if (menuSlot - (36) != hotbarSlot) {
            ItemStack hotbarStack = ClientAccess.schizoMinecraft.player.getInventory().getItem(hotbarSlot);
            if (ItemStack.matches(hotbarStack, stack)) {
               return true;
            }
         }
      }
      return false;
   }

   @EventHandler(priority = -100)
   private void onMultiPlayerGameModeHandleInventoryMouseClickEvent(
      MultiPlayerGameModeHandleInventoryMouseClickEvent event
   ) {
      if (event.isCancelled()) {
         return;
      }

      Minecraft minecraft = ClientAccess.schizoMinecraft;
      Runnable recordClick = this::recordClick;
      minecraft.execute(recordClick);
      this.containerOpen = true;

      this.lastClickTimer.reset();
      if (!this.clickBatching
         || !this.clickBatchCompleted) {
         EventBus capturedEventBus = ClientAccess.eventBus;
         InventoryActionEvent preEvent = new InventoryActionEvent(EventStage.Pre);

         capturedEventBus.post(preEvent);
      }
   }

   private void recordClick() {
      this.clickTimestamps.add(System.nanoTime());
   }

   @EventHandler(priority = -100)
   private void onInventoryClickAfter(
      MultiPlayerGameModeHandleInventoryMouseClickEvent.After event
   ) {
      if (!this.clickBatching
         || !this.clickBatchCompleted) {
         EventBus capturedEventBus = ClientAccess.eventBus;
         InventoryActionEvent postEvent = new InventoryActionEvent(EventStage.Post);

         capturedEventBus.post(postEvent);
         this.clickBatchCompleted = true;
      }
   }

   @EventHandler
   private void onMultiPlayerGameModeUseItemOnEvent(MultiPlayerGameModeUseItemOnEvent event) {
      LocalPlayer sneakPlayer = ClientAccess.schizoMinecraft.player;
      if (!sneakPlayer.isShiftKeyDown()) {
         BlockPos capturedPosition = event.getHitResult().getBlockPos();

         if (WorldUtils.getBlock(capturedPosition)
            == Blocks.ENDER_CHEST) {
            this.enderChestUseTimer.reset();
         }
      }
   }

   @EventHandler(priority = 200)
   private void onLocalPlayerPreTick(LocalPlayerPreTickEvent event) {
      long nowNanos = System.nanoTime();
      this.clickTimestamps.removeIf(timestamp -> nowNanos - timestamp >= CLICK_WINDOW_NANOS);
   }

   @EventHandler(priority = -100)
   private void onConnectionSendPacketEvent(
      PacketEvent.Send event
   ) {
      if (event.isCancelled()) {
         return;
      }

      if (event.getPacket() instanceof ServerboundContainerClickPacket) {
         this.containerOpen = true;

         this.lastClickTimer.reset();

         if (!this.clickBatching
            || !this.clickBatchCompleted) {
            EventBus capturedPreEventBus = ClientAccess.eventBus;
            InventoryActionEvent preEvent = new InventoryActionEvent(EventStage.Pre);

            capturedPreEventBus.post(preEvent);
         }
      }

      if (event.getPacket() instanceof ServerboundContainerClosePacket) {
         this.containerOpen = false;
      }

      if (event.getPacket() instanceof ServerboundSetCarriedItemPacket carriedItemPacket) {
         if (carriedItemPacket.getSlot() != this.serverSelectedSlot) {
            EventBus capturedSlotEventBus = ClientAccess.eventBus;
            SelectedSlotChangeEvent slotChangeEvent = new SelectedSlotChangeEvent(carriedItemPacket.getSlot());

            capturedSlotEventBus.post(slotChangeEvent);
            this.serverSelectedSlot = carriedItemPacket.getSlot();
         }
      }

   }

   @EventHandler(priority = -200)
   private void onLocalPlayerSendPositionEvent(LocalPlayerSendPositionEvent event) {
      if (event.getStage() == EventStage.Post) {
         return;
      }

      synchronized (this.deferredSlotUpdates) {
         ArrayList<Integer> readySlots = new ArrayList<>();
         this.deferredSlotUpdates.forEach(
            (slot, update) -> {
               if (deadlineReached(System.nanoTime(), update.resumeAtNanos())) {
                  readySlots.add(slot);
               }
            }
         );
         Iterator<Integer> slotsIterator = readySlots.iterator();

         while (slotsIterator.hasNext()) {
            Integer readySlot = slotsIterator.next();
            InventoryStateManager.DeferredSlotUpdate removedUpdate = this.deferredSlotUpdates.remove(readySlot);
            ClientPacketListener capturedConnection = ClientAccess.schizoMinecraft.player.connection;

            ClientboundContainerSetSlotPacket packet = removedUpdate.packet();

            capturedConnection.handleContainerSetSlot(packet);
         }
      }

      this.localEntityEventPending = false;
      if (xCarry.isToggled()
         || ClientAccess.schizoMinecraft.screen instanceof AbstractContainerScreen) {
         return;
      }
      ElapsedTimer capturedTimer = this.lastClickTimer;

      if (!capturedTimer.hasElapsed(50L) || !this.containerOpen) {
         return;
      }
      ServerStateManager capturedTickTracker = ClientContext.serverStateManager;

      if (capturedTickTracker.getPacketIdleMillis(ServerStateManager.PacketActivity.ANY) > 2500L) {
         return;
      }
      if (RotationManager.is2b2tMode()) {
         capturedTimer = this.enderChestUseTimer;
         if (!capturedTimer.hasElapsed(200L)) {
            return;
         }
      }

      this.close();
   }

   @EventHandler(priority = 100)
   private void onPacketReceive(
      PacketEvent.Receive event
   ) {
      if (event.getPacket() instanceof ClientboundLoginPacket) {
         this.slotResyncArmed = true;
      }

      if (event.getPacket() instanceof ClientboundContainerClosePacket closePacket) {
         this.containerOpen = false;
      }

      if (event.getPacket() instanceof ClientboundContainerSetContentPacket) {
         this.deferredSlotUpdates.clear();
      }

      if (event.getPacket() instanceof ClientboundContainerSetSlotPacket slotPacket) {
         if (RotationManager.isMovementSyncEnabled()) {
            int slotWord = slotPacket.getSlot();
            Inventory inventory = ClientAccess.schizoMinecraft.player.getInventory();
            if (slotWord == (36) + inventory.getSelectedSlot()) {
               return;
            }

            slotWord = slotPacket.getSlot();

            if (slotWord < (36)) {
               return;
            }

            slotWord = slotPacket.getSlot();

            if (slotWord > (44)) {
               return;
            }

            ItemStack capturedItem = slotPacket.getItem();
            int itemSlot = slotPacket.getSlot();

            if (!this.matchesOtherHeldStack(capturedItem, itemSlot)) {
               Map<Integer, DeferredSlotUpdate> removeMap = this.deferredSlotUpdates;
               int removeSlot = slotPacket.getSlot();
               removeMap.remove(removeSlot);
               return;
            }

            Map<Integer, DeferredSlotUpdate> deferredMap = this.deferredSlotUpdates;
            int deferredSlot = slotPacket.getSlot();
            deferredMap.put(
               deferredSlot,
               new InventoryStateManager.DeferredSlotUpdate(
                  slotPacket,
                  System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(500L)
               )
            );

            event.cancel();
         }
      }

      if (event.getPacket() instanceof ClientboundEntityEventPacket entityPacket) {
         if (entityPacket.getEventId() == 35) {
            ClientLevel level = ClientAccess.schizoMinecraft.level;
            if (entityPacket.getEntity(level) == ClientAccess.schizoMinecraft.player) {
               this.localEntityEventPending = true;
            }
         }
      }
   }

   public boolean hasPendingLocalEntityEvent() {
      return this.localEntityEventPending;
   }

   @EventHandler
   private void onMinecraftDisconnect(MinecraftDisconnectEvent event) {
      synchronized (this.deferredSlotUpdates) {
         this.deferredSlotUpdates.clear();
      }
      this.clickTimestamps.clear();
      this.lastClickTimer.clear();
      this.enderChestUseTimer.clear();
      this.containerOpen = false;
      this.clickBatching = false;
      this.clickBatchCompleted = false;
      this.localEntityEventPending = false;
      this.serverSelectedSlot = -1;
      this.slotResyncArmed = false;
   }

   static boolean deadlineReached(long nowNanos, long deadlineNanos) {
      return nowNanos - deadlineNanos >= 0L;
   }

   public static boolean isClickRateLimited(InventoryStateManager manager) {
      return wouldReachClickLimit(manager, 0);
   }

   private record DeferredSlotUpdate(ClientboundContainerSetSlotPacket packet, long resumeAtNanos) {
   }
}
