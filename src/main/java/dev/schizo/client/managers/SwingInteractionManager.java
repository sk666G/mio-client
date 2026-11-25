/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.modules.misc.SwingModule.HandChoice;

import dev.schizo.client.utils.combat.BoundingBoxUtils;
import dev.schizo.client.utils.entity.SwingAction;
import dev.schizo.client.utils.network.PacketSender;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.CrystalPlacementEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.events.bus.EventBus;
import dev.schizo.client.modules.client.AntiCheatModule;
import dev.schizo.client.modules.misc.SwingModule;
import dev.schizo.client.settings.Setting;

import dev.schizo.mixin.LivingEntityAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;

public final class SwingInteractionManager implements ClientAccess {
   private static final long PENDING_CRYSTAL_TTL_NANOS = TimeUnit.MILLISECONDS.toNanos(300L);
   private final AntiCheatModule antiCheat;
   private final SwingModule swingModule;
   private final List<PendingCrystalPlacement> pendingCrystals;
   private volatile long sessionGeneration;

   public SwingInteractionManager() {
      this.antiCheat = java.util.Objects.requireNonNull(
         ClientContext.getModule(AntiCheatModule.class),
         "Required module is not registered: AntiCheatModule"
      );
      this.swingModule = java.util.Objects.requireNonNull(
         ClientContext.getModule(SwingModule.class),
         "Required module is not registered: SwingModule"
      );
      List<PendingCrystalPlacement> pending = new ArrayList<>(initialCrystalCapacity());
      this.pendingCrystals = Collections.synchronizedList(pending);
      EventBus eventBus = ClientAccess.eventBus;
      eventBus.subscribe(this);
   }

   private static int initialCrystalCapacity() {
      return 10;
   }

   public boolean tryTrackCrystalPlacement(BlockPos position) {
      if (this.isCrystalPlacementLimited()) {
         return false;
      }

      this.pendingCrystals.add(
         new PendingCrystalPlacement(position.immutable(), System.nanoTime() + PENDING_CRYSTAL_TTL_NANOS)
      );
      ClientAccess.eventBus.post(new CrystalPlacementEvent(position));
      return true;
   }

   @EventHandler
   private void onLocalPlayerTickEvent(LocalPlayerTickEvent event) {
      this.pendingCrystals.removeIf(pending -> {
         long expiresAt = pending.expiresAtNanos();
         return expiresAt <= System.nanoTime();
      });
   }

   public void swing(
      InteractionHand hand, SwingAction action, boolean packetOnly
   ) {
      AntiCheatModule antiCheatSnapshot = this.antiCheat;
      if (!action.isEnabled(antiCheatSnapshot)) {
         if (!packetOnly) {
            this.swingClientSide(hand);
         }
      } else {
         if (packetOnly) {
            PacketSender.sendSwing(hand);
         } else {
            LocalPlayer player = ClientAccess.schizoMinecraft.player;
            player.swing(hand);
         }
      }
   }

   public void swing(InteractionHand hand, SwingAction action) {
      boolean packetOnly = false;
      this.swing(hand, action, packetOnly);
   }

   public void swingPacket(InteractionHand hand, SwingAction action) {
      boolean packetOnly = true;
      this.swing(hand, action, packetOnly);
   }

   public void swingClientSide(InteractionHand hand) {
      if (ClientAccess.schizoMinecraft.player.swinging) {
         int swingTime = ClientAccess.schizoMinecraft.player.swingTime;
         int swingDuration = ((LivingEntityAccessor)ClientAccess.schizoMinecraft.player)
            .getSwingDuration();

         if (swingTime < swingDuration / 2
            && ClientAccess.schizoMinecraft.player.swingTime >= 0) {
            return;
         }
      }

      ClientAccess.schizoMinecraft.player.swingTime = -1;
      ClientAccess.schizoMinecraft.player.swinging = true;
      SwingModule moduleSnapshot = this.swingModule;
      if (moduleSnapshot.isToggled()) {
         Setting<HandChoice> handSetting = this.swingModule.handSetting;
         SwingModule.HandChoice handChoice = handSetting.getValue();
         if (handChoice.getHand() != null) {
            LocalPlayer player = ClientAccess.schizoMinecraft.player;
            Setting<HandChoice> overrideSetting = this.swingModule.handSetting;
            SwingModule.HandChoice overrideChoice = overrideSetting.getValue();
            player.swingingArm = overrideChoice.getHand();
            return;
         }
      }

      ClientAccess.schizoMinecraft.player.swingingArm = hand;
   }

   public boolean isCrystalPlacementLimited() {
      AntiCheatModule antiCheatSnapshot = this.antiCheat;
      if (!antiCheatSnapshot.isPaperPlacement()) {
         return false;
      }

      int pendingCount = this.pendingCrystals.size();

      return pendingCount >= 9;
   }

   @EventHandler
   private void onPacketReceive(
      PacketEvent.Receive event
   ) {
      PacketEvent.Receive packetEvent = event;
      if (packetEvent.getPacket() instanceof ClientboundAddEntityPacket packet) {
         if (packet.getType() == EntityType.END_CRYSTAL) {
            EntityType spawnTypeSnapshot = packet.getType();
            double spawnXSnapshot = packet.getX();
            double spawnYSnapshot = packet.getY();
            double spawnZ = packet.getZ();
            AABB spawnBoundsSnapshot = spawnTypeSnapshot.getSpawnAABB(spawnXSnapshot, spawnYSnapshot, spawnZ);
            double inset = -BoundingBoxUtils.blockCornerInset;
            AABB crystalBounds = spawnBoundsSnapshot.inflate(inset);
            long expectedGeneration = this.sessionGeneration;
            BlockableEventLoop<?> executor = ClientAccess.schizoMinecraft;
            Runnable removePlacement = () -> {
               if (this.sessionGeneration != expectedGeneration) {
                  return;
               }
               this.pendingCrystals.removeIf(pending -> {
                  AABB placementBounds = new AABB(capturePlacementPosition(pending));
                  return crystalBounds.intersects(placementBounds);
               });
            };
            executor.execute(removePlacement);
         }
      }
   }

   @EventHandler
   private void onMinecraftDisconnect(MinecraftDisconnectEvent event) {
      this.sessionGeneration++;
      this.pendingCrystals.clear();
   }

   private static BlockPos capturePlacementPosition(PendingCrystalPlacement pending) {
      return pending.position();
   }

   private record PendingCrystalPlacement(
      BlockPos position,
      long expiresAtNanos
   ) {
   }
}
